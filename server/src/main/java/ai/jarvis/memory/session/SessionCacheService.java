package ai.jarvis.memory.session;

import ai.jarvis.chat.message.Message;
import ai.jarvis.chat.message.MessageRepository;
import ai.jarvis.chat.message.MessageRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Caches session message history in Redis.
 *
 * ENCODING: JSON Lines (one JSON object per line).
 * Each line: {"r":"ROLE","i":"uuid","c":"content"}
 *
 * WHY JSON Lines instead of custom escaping:
 * - Jackson handles all special chars correctly
 * - No data corruption for code, JSON, Windows paths
 * - Safe round-trip for any message content
 * - Still simple to parse (one object per line)
 */
@Slf4j
@Service
public class SessionCacheService {

    private final ReactiveRedisTemplate<String, String>
            redisTemplate;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    private static final Duration SESSION_TTL =
            Duration.ofHours(1);
    private static final String MESSAGES_PREFIX =
            "session:messages:";
    private static final int MAX_MESSAGES = 20;

    public SessionCacheService(
            @Qualifier("reactiveStringRedisTemplate")
            ReactiveRedisTemplate<String, String>
                    redisTemplate,
            MessageRepository messageRepository,
            @Qualifier("jarvisObjectMapper")
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Get session history.
     * Cache HIT → Redis (~1ms)
     * Cache MISS → PostgreSQL last 20 rows + cache
     */
    public Mono<List<Message>> getSessionHistory(
            UUID sessionId) {

        String key = MESSAGES_PREFIX + sessionId;

        return redisTemplate.opsForValue()
                .get(key)
                .flatMap(cached -> {
                    log.debug("Cache HIT: {}", sessionId);
                    return redisTemplate
                            .expire(key, SESSION_TTL)
                            .thenReturn(
                                    deserialize(cached,
                                            sessionId));
                })
                .switchIfEmpty(
                        loadAndCache(sessionId, key)
                )
                .onErrorResume(error -> {
                    log.warn(
                            "Redis error [{}], using DB: {}",
                            sessionId,
                            error.getClass().getSimpleName());
                    return loadFromDb(sessionId);
                });
    }

    /**
     * Refresh cache after message saved.
     * Reloads from DB and re-caches.
     */
    public Mono<Void> refreshCache(UUID sessionId) {
        String key = MESSAGES_PREFIX + sessionId;
        return loadFromDb(sessionId)
                .flatMap(messages ->
                        cacheMessages(key, messages))
                .doOnSuccess(v ->
                        log.debug("Cache refreshed: {}",
                                sessionId))
                .then();
    }

    /**
     * Hard invalidate — only when session deleted.
     */
    public Mono<Void> invalidate(UUID sessionId) {
        return redisTemplate
                .delete(MESSAGES_PREFIX + sessionId)
                .doOnSuccess(d ->
                        log.debug("Cache deleted: {}",
                                sessionId))
                .then();
    }

    // ── Private ───────────────────────────────────

    private Mono<List<Message>> loadAndCache(
            UUID sessionId, String key) {
        return loadFromDb(sessionId)
                .flatMap(messages ->
                        cacheMessages(key, messages)
                                .thenReturn(messages)
                );
    }

    /**
     * Load last MAX_MESSAGES from DB at query level.
     * No in-memory slicing needed.
     */
    private Mono<List<Message>> loadFromDb(
            UUID sessionId) {
        log.debug("Cache MISS [{}] — DB query",
                sessionId);
        // LIMIT pushed to DB query
        return messageRepository
                .findLastNBySessionId(
                        sessionId, MAX_MESSAGES)
                .collectList();
    }

    private Mono<Void> cacheMessages(
            String key, List<Message> messages) {
        try {
            String serialized = serialize(messages);
            return redisTemplate.opsForValue()
                    .set(key, serialized, SESSION_TTL)
                    .doOnSuccess(ok ->
                            log.debug(
                                    "Cached {} msgs [{}]",
                                    messages.size(), key))
                    .then();
        } catch (Exception e) {
            log.error(
                    "Cache write failed [{}]: {}",
                    key, e.getClass().getSimpleName());
            return Mono.empty();
        }
    }

    /**
     * Serialize as JSON Lines.
     * Each line = one MessageDto JSON object.
     * Jackson handles all special chars correctly.
     */
    private String serialize(
            List<Message> messages) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            if (msg.role() == null) continue;
            if (msg.content() == null) continue;
            MessageDto dto = new MessageDto(
                    msg.role().name(),
                    msg.id().toString(),
                    msg.content()
            );
            sb.append(objectMapper.writeValueAsString(dto))
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * Deserialize JSON Lines back to Message list.
     * Fix: logs session ID only, NOT message content.
     */
    private List<Message> deserialize(
            String data, UUID sessionId) {
        List<Message> messages = new ArrayList<>();
        if (data == null || data.isBlank()) {
            return messages;
        }

        String[] lines = data.split("\n");
        int lineNum = 0;
        for (String line : lines) {
            lineNum++;
            if (line.isBlank()) continue;
            try {
                MessageDto dto = objectMapper
                        .readValue(line,
                                MessageDto.class);
                MessageRole role = MessageRole
                        .valueOf(dto.r());
                UUID id = UUID.fromString(dto.i());

                messages.add(new Message(
                        id, sessionId, role, dto.c(),
                        null, null, null, null, null,
                        null, null, false, null,
                        Instant.now()));

            } catch (Exception e) {
                // Fix: log session ID + line number only
                // NOT the line content (may contain user text)
                log.warn(
                        "Parse failed session={} line={}: {}",
                        sessionId, lineNum,
                        e.getClass().getSimpleName());
            }
        }
        return messages;
    }

    // ── DTO for Redis serialization ────────────────

    /**
     * Minimal DTO for Redis storage.
     * Short field names to reduce Redis memory usage.
     * r = role, i = id, c = content
     */
    private record MessageDto(
            @JsonProperty("r") String r,
            @JsonProperty("i") String i,
            @JsonProperty("c") String c) {}
}