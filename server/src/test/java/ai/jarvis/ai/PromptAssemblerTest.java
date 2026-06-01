package ai.jarvis.ai;

import ai.jarvis.ai.prompt.PromptAssembler;
import ai.jarvis.chat.message.Message;
import ai.jarvis.chat.message.MessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PromptAssembler Tests")
class PromptAssemblerTest {

    private PromptAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new PromptAssembler();
    }

    @Test
    @DisplayName("Should include system prompt and user message")
    void shouldIncludeSystemAndUserMessage() {
        Prompt prompt = assembler.assemble(
                "Hello Jarvis",
                "Date: May 31 2026",
                List.of(),
                "dravin"
        );

        // At minimum: system + working memory + user
        assertThat(prompt.getInstructions())
                .isNotEmpty()
                .hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should include conversation history messages")
    void shouldIncludeHistory() {
        Message userMsg = new Message(
                UUID.randomUUID(),
                UUID.randomUUID(),
                MessageRole.USER,
                "What is Java?",
                null, null, null, null, null,
                null, null, false, null, Instant.now()
        );

        Message assistantMsg = new Message(
                UUID.randomUUID(),
                UUID.randomUUID(),
                MessageRole.ASSISTANT,
                "Java is a programming language.",
                null, "llama3.1:8b",
                null, 10, 10,
                500, "STOP", false, null, Instant.now()
        );

        Prompt promptWithHistory = assembler.assemble(
                "Tell me more",
                "Date: May 31 2026",
                List.of(userMsg, assistantMsg),
                "dravin"
        );

        Prompt promptWithoutHistory = assembler.assemble(
                "Tell me more",
                "Date: May 31 2026",
                List.of(),
                "dravin"
        );

        // Prompt WITH history should have more messages
        // than prompt WITHOUT history
        assertThat(promptWithHistory.getInstructions().size())
                .isGreaterThan(
                        promptWithoutHistory
                                .getInstructions().size());
    }

    @Test
    @DisplayName("Should handle empty history gracefully")
    void shouldHandleEmptyHistory() {
        Prompt prompt = assembler.assemble(
                "Hello!",
                "Date: today",
                List.of(),
                "dravin"
        );

        assertThat(prompt.getInstructions())
                .isNotNull()
                .isNotEmpty();
    }

    @Test
    @DisplayName("Should trim history when exceeding max")
    void shouldTrimLongHistory() {
        // Create 25 messages (above 20 limit)
        List<Message> longHistory =
                java.util.stream.IntStream
                        .range(0, 25)
                        .mapToObj(i -> new Message(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                i % 2 == 0
                                        ? MessageRole.USER
                                        : MessageRole.ASSISTANT,
                                "Message " + i,
                                null,
                                i % 2 != 0
                                        ? "llama3.1:8b" : null,
                                null, null, null,
                                null, null, false, null,
                                Instant.now()
                        ))
                        .toList();

        Prompt prompt = assembler.assemble(
                "Current question",
                "Date: today",
                longHistory,
                "dravin"
        );

        // Should NOT include all 25 history messages
        // Max 20 history + 2 system + 1 current = 23
        assertThat(prompt.getInstructions().size())
                .isLessThanOrEqualTo(23);
    }

    @Test
    @DisplayName("Working memory should be in prompt")
    void shouldInjectWorkingMemory() {
        String workingMemory =
                "UNIQUE_TEST_STRING_12345";

        Prompt prompt = assembler.assemble(
                "What day is it?",
                workingMemory,
                List.of(),
                "dravin"
        );

        // Check the working memory content appears
        // in at least one message
        boolean found = prompt.getInstructions()
                .stream()
                .anyMatch(m -> m.getText() != null
                        && m.getText().contains(
                        "UNIQUE_TEST_STRING_12345"));

        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("Username should appear in prompt")
    void shouldIncludeUsername() {
        Prompt prompt = assembler.assemble(
                "Hi!",
                "Date: today",
                List.of(),
                "uniqueuser99"
        );

        boolean hasUsername = prompt
                .getInstructions()
                .stream()
                .anyMatch(m -> m.getText() != null
                        && m.getText().contains(
                        "uniqueuser99"));

        assertThat(hasUsername).isTrue();
    }
}