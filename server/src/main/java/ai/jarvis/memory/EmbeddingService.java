package ai.jarvis.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Generates vector embeddings for text content.
 *
 * Uses Ollama nomic-embed-text (768 dimensions).
 * Free. Private. No API key needed.
 *
 * Spring AI auto-configures EmbeddingModel
 * from spring.ai.ollama.embedding config.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * Generate embedding for a single text.
     * Ollama call is blocking → boundedElastic thread.
     */
    public Mono<float[]> embed(String text) {
        return Mono.fromCallable(() -> {
                    log.debug("Embedding: {}...",
                            text.substring(0,
                                    Math.min(50, text.length())));

                    // null = use model default options
                    // Official Spring AI pattern from docs
                    EmbeddingRequest request =
                            new EmbeddingRequest(
                                    List.of(text), null);

                    float[] vector = embeddingModel
                            .call(request)
                            .getResults()
                            .getFirst()   // Java 21
                            .getOutput();

                    log.debug("Generated {} dimensions",
                            vector.length);

                    return vector;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(error -> {
                    log.error("Embedding failed: {}",
                            error.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Generate embeddings for multiple texts at once.
     */
    public Mono<List<float[]>> embedAll(
            List<String> texts) {
        return Mono.fromCallable(() -> {
                    EmbeddingRequest request =
                            new EmbeddingRequest(
                                    texts, null);

                    return embeddingModel
                            .call(request)
                            .getResults()
                            .stream()
                            .map(e -> e.getOutput())
                            .toList();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(error -> {
                    log.error("Batch embedding failed: {}",
                            error.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Convert float[] to pgvector string format.
     * Format: "[0.1,0.2,0.3,...]"
     */
    public String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) {
                sb.append(",");
            }
        }
        return sb.append("]").toString();
    }
}