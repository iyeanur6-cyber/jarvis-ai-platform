package ai.jarvis.ai.prompt;

import ai.jarvis.chat.message.Message;
import ai.jarvis.chat.message.MessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles the complete prompt from all context sources.
 *
 * ASSEMBLY ORDER (top to bottom):
 * 1. System prompt   — Jarvis personality + rules
 * 2. Working memory  — date/time/user/model (fresh each call)
 * 3. Long-term memory— what Jarvis knows about this user
 * 4. Session history — recent conversation messages
 * 5. Current message — what user just sent
 *
 * WHY THIS ORDER:
 * AI reads top-to-bottom. Earlier content = stronger frame.
 * Personality and known facts frame everything that follows.
 * History gives recent context. Current message gets answered.
 *
 * PHASE 2 CHANGE:
 * Added step 3 (long-term memory injection).
 * memoryContext is optional — empty string = no injection.
 * This keeps Phase 1 behavior unchanged if memories empty.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptAssembler {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are Jarvis, an intelligent personal AI assistant.
            You are helpful, professional, and concise.
            Use markdown formatting in your responses.
            Never make up facts. If unsure, say so clearly.
            Reference previous conversation context when relevant.
            When you know facts about the user, use them naturally
            to personalize your responses.
            """;

    private static final int MAX_HISTORY_MESSAGES = 20;

    /**
     * Assemble complete prompt for AI.
     *
     * @param userMessage   what the user just typed
     * @param workingMemory fresh context (date/time/user/model)
     * @param history       recent session messages
     * @param username      user's display name
     * @param memoryContext formatted long-term memories string
     *                      Empty string = no memories to inject
     */
    public Prompt assemble(
            String userMessage,
            String workingMemory,
            List<Message> history,
            String username,
            String memoryContext) {

        List<org.springframework.ai.chat.messages.Message>
                messages = new ArrayList<>();

        // ── Step 1: System Prompt ─────────────────
        // Jarvis personality + rules
        // Include username for personalization
        String systemContent = DEFAULT_SYSTEM_PROMPT
                + "\nYou are talking to: " + username;
        messages.add(new SystemMessage(systemContent));

        // ── Step 2: Working Memory ────────────────
        // Current date/time/session/model
        // Fresh on every single request
        messages.add(new SystemMessage(workingMemory));

        // ── Step 3: Long-Term Memory (NEW) ────────
        // What Jarvis knows about this user
        // Extracted from past conversations
        // Only injected if memories exist
        if (memoryContext != null
                && !memoryContext.isBlank()) {
            messages.add(
                    new SystemMessage(memoryContext));
            log.debug(
                    "Injected memory context for user={}",
                    username);
        }

        // ── Step 4: Session History ───────────────
        // Recent messages in this conversation
        // Limited to last 20 to stay within context window
        List<Message> recentHistory =
                history.size() > MAX_HISTORY_MESSAGES
                        ? history.subList(
                        history.size() - MAX_HISTORY_MESSAGES,
                        history.size())
                        : history;

        for (Message msg : recentHistory) {
            if (msg.role() == MessageRole.USER) {
                messages.add(
                        new UserMessage(msg.content()));
            } else if (msg.role()
                    == MessageRole.ASSISTANT
                    && !msg.error()) {
                messages.add(
                        new AssistantMessage(
                                msg.content()));
            }
        }

        // ── Step 5: Current Message ───────────────
        // Always last — this is what AI responds to
        messages.add(new UserMessage(userMessage));

        log.debug(
                "Prompt assembled: {} messages "
                        + "(memories={} history={} current=1)",
                messages.size(),
                (memoryContext != null
                        && !memoryContext.isBlank()) ? 1 : 0,
                recentHistory.size()
        );

        return new Prompt(messages);
    }

    /**
     * Backward-compatible method WITHOUT memory context.
     * Used by tests and places not yet updated.
     * Delegates to full method with empty memory string.
     */
    public Prompt assemble(
            String userMessage,
            String workingMemory,
            List<Message> history,
            String username) {

        // Empty string = no memory injection
        // Keeps Phase 1 behavior exactly the same
        return assemble(
                userMessage,
                workingMemory,
                history,
                username,
                "");
    }
}