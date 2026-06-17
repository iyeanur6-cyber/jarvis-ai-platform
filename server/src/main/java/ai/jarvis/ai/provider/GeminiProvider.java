package ai.jarvis.ai.provider;

import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gemini AI provider — cloud fallback.
 *
 * ACTIVATION:
 * Only created when spring.ai.google.genai.api-key
 * is set to a non-empty value.
 *
 * WHY MANUAL CLIENT:
 * GoogleGenAiChatAutoConfiguration is excluded in
 * application.yml because it throws IllegalStateException
 * when api-key is empty/missing.
 * We build the client manually here so it only
 * initializes when the key actually exists.
 *
 * CONSTRUCTOR PATTERN (verified from Spring AI GitHub):
 * Official integration test (GoogleGenAiChatModelIT.java)
 * uses GoogleGenAiChatModel.builder() pattern.
 * Source: github.com/spring-projects/spring-ai/blob/main/
 * models/spring-ai-google-genai/src/test/java/
 * org/springframework/ai/google/genai/
 * GoogleGenAiChatModelIT.java
 *
 * Direct constructor requires 5 params including
 * RetryTemplate + ObservationRegistry (not in pom.xml).
 * Builder pattern handles these internally — zero extra deps.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "spring.ai.google.genai.api-key",
        matchIfMissing = false
)
public class GeminiProvider implements AiProvider {

    private final String apiKey;
    private final String modelName;
    private final ChatClient chatClient;

    public GeminiProvider(
            @Value("${spring.ai.google.genai.api-key:}")
            String apiKey,
            @Value("${spring.ai.google.genai.chat.model:"
                    + "gemini-2.0-flash}")
            String modelName) {

        this.apiKey = apiKey;
        this.modelName = modelName;

        if (apiKey != null && !apiKey.isBlank()) {

            // TEMPORARY DEBUG — remove after confirming key works
            log.info(
                    "Gemini API key loaded: length={} prefix={}",
                    apiKey.length(),
                    apiKey.substring(0, Math.min(8, apiKey.length()))
            );

            // Step 1: Build Google GenAI HTTP client
            Client genAiClient = Client.builder()
                    .apiKey(apiKey)
                    .build();

            // Step 2: Build GoogleGenAiChatModel
            // Using builder() pattern — verified from
            // official Spring AI integration test:
            // GoogleGenAiChatModelIT.java uses:
            // GoogleGenAiChatModel.builder()
            //   .genAiClient(genAiClient)
            //   .defaultOptions(options)
            //   .build();
            GoogleGenAiChatModel chatModel =
                    GoogleGenAiChatModel.builder()
                            .genAiClient(genAiClient)
                            .defaultOptions(
                                    GoogleGenAiChatOptions.builder()
                                            .model(modelName)
                                            .temperature(0.7)
                                            .maxOutputTokens(2048)
                                            .build()
                            )
                            .build();

            // Step 3: Wrap in ChatClient for streaming
            this.chatClient = ChatClient
                    .builder(chatModel)
                    .build();

            log.info(
                    "GeminiProvider initialized: model={}",
                    modelName);

        } else {
            this.chatClient = null;
            log.debug(
                    "GeminiProvider: no API key configured");
        }
    }

    @Override
    public Flux<String> streamChat(Prompt prompt) {
        if (chatClient == null) {
            return Flux.error(new RuntimeException(
                    "Gemini API key not configured. "
                            + "Set GEMINI_API_KEY in environment."));
        }
        return chatClient
                .prompt(prompt)
                .stream()
                .content();
    }

    @Override
    public Mono<Boolean> isAvailable() {
        boolean hasKey = apiKey != null
                && !apiKey.isBlank()
                && chatClient != null;
        return Mono.just(hasKey);
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