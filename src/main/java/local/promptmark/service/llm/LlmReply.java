package local.promptmark.service.llm;

import java.util.Collections;
import java.util.List;

/**
 * Result of one LLM call. When {@link #toolCalls()} is non-empty the model is
 * asking the orchestrator to run those tools and feed the result back. When it
 * is empty {@link #content()} is the final assistant message for this turn.
 */
public final class LlmReply {

    private final String role;
    private final String content;
    private final List<ToolCall> toolCalls;

    public LlmReply(String role, String content, List<ToolCall> toolCalls) {
        this.role = role;
        this.content = content == null ? "" : content;
        this.toolCalls = (toolCalls == null) ? Collections.emptyList() : toolCalls;
    }

    public String role() { return role; }
    public String content() { return content; }
    public List<ToolCall> toolCalls() { return toolCalls; }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
