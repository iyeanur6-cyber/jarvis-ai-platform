package ai.jarvis.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gemini provider using ChatClient.Builder.
 * Only activated when GEMINI_API_KEY is set.
 *
 * ChatClient.Builder is always available from
 * Spring AI regardless of which model is active.
 * It automatically uses whichever ChatModel
 * Spring Boot auto-configured.
 */
@Slf4j
@Component
public class GeminiProvider implements AiProvider {

    private final String apiKey;
    private final String modelName;
    private ChatClient chatClient;

    public GeminiProvider(
            ChatClient.Builder builder,
            @Value("${spring.ai.google.api-key:}")
            String apiKey,
            @Value("${spring.ai.google.chat.model:"
                    + "gemini-2.0-flash}")
            String modelName) {

        this.apiKey = apiKey;
        this.modelName = modelName;

        // Only build client if key is configured
        if (apiKey != null && !apiKey.isBlank()) {
            this.chatClient = builder.build();
            log.info("GeminiProvider initialized: "
                    + "model={}", modelName);
        } else {
            this.chatClient = null;
            log.debug("GeminiProvider: "
                    + "no API key configured");
        }
    }

    @Override
    public Flux<String> streamChat(Prompt prompt) {
        if (chatClient == null) {
            return Flux.error(new RuntimeException(
                    "Gemini API key not configured. "
                            + "Add GEMINI_API_KEY to .env"));
        }
        return chatClient
                .prompt(prompt)
                .stream()
                .content()
                .filter(t -> t != null
                        && !t.isEmpty());
    }

    @Override
    public Mono<Boolean> isAvailable() {
        boolean available = apiKey != null
                && !apiKey.isBlank()
                && chatClient != null;
        return Mono.just(available);
    }

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}