package ai.jarvis.ai.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderRouter {

    private final OllamaProvider ollamaProvider;
    private final GeminiProvider geminiProvider;
    // Use concrete types, not AiProvider interface
    // This avoids Spring confusion with multiple
    // AiProvider beans

    public Mono<AiProvider> route() {
        return ollamaProvider.isAvailable()
                .flatMap(ollamaUp -> {
                    if (ollamaUp) {
                        log.debug(
                                "Routing to: ollama ({})",
                                ollamaProvider.getModelName()
                        );
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
                                                    + "gemini [FALLBACK]"
                                    );
                                    return Mono.just(
                                            (AiProvider)
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
                    Mono.just((AiProvider) geminiProvider);
            default -> Mono.error(
                    new RuntimeException(
                            "Unknown provider: " + name));
        };
    }
}