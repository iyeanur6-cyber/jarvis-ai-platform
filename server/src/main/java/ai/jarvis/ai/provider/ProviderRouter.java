package ai.jarvis.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ProviderRouter {

    private final OllamaProvider ollamaProvider;
    private final AiProvider geminiProvider;

    /**
     * Single constructor — no @RequiredArgsConstructor.
     * geminiProvider is AiProvider interface so it accepts
     * BOTH GeminiProvider and GeminiUnavailableProvider.
     * Spring injects whichever bean named "gemini" exists.
     */
    public ProviderRouter(
            OllamaProvider ollamaProvider,
            AiProvider geminiProvider) {
        this.ollamaProvider = ollamaProvider;
        this.geminiProvider = geminiProvider;
        log.info(
                "ProviderRouter initialized: "
                        + "primary=ollama fallback={}",
                geminiProvider.getName()
                        + "(" + geminiProvider.getModelName() + ")");
    }

    public Mono<AiProvider> route() {
        return ollamaProvider.isAvailable()
                .flatMap(ollamaUp -> {
                    if (ollamaUp) {
                        log.debug(
                                "Routing to: ollama ({})",
                                ollamaProvider.getModelName());
                        return Mono.just(
                                (AiProvider) ollamaProvider);
                    }

                    log.warn(
                            "Ollama unavailable. "
                                    + "Checking Gemini...");

                    return geminiProvider
                            .isAvailable()
                            .flatMap(geminiUp -> {
                                if (geminiUp) {
                                    log.info(
                                            "Routing to: "
                                                    + "gemini [FALLBACK]");
                                    return Mono.just(
                                            geminiProvider);
                                }
                                log.error(
                                        "All providers "
                                                + "unavailable");
                                return Mono.error(
                                        new RuntimeException(
                                                "No AI provider "
                                                        + "available.\n"
                                                        + "Fix: ollama serve"
                                        ));
                            });
                });
    }

    public Mono<AiProvider> routeTo(String name) {
        return switch (name.toLowerCase()) {
            case "ollama" ->
                    Mono.just((AiProvider) ollamaProvider);
            case "gemini" ->
                    Mono.just(geminiProvider);
            default -> Mono.error(
                    new RuntimeException(
                            "Unknown provider: " + name));
        };
    }
}