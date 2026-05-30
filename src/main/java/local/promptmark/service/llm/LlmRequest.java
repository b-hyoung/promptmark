package local.promptmark.service.llm;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * One synchronous chat-completion call. Carries the message history, tool
 * definitions visible to the model, and an end-to-end timeout for the HTTP
 * exchange.
 */
public final class LlmRequest {

    private final List<Message> messages;
    private final List<ToolDef> tools;
    private final Duration timeout;

    public LlmRequest(List<Message> messages, List<ToolDef> tools, Duration timeout) {
        this.messages = (messages == null) ? Collections.emptyList() : messages;
        this.tools = (tools == null) ? Collections.emptyList() : tools;
        this.timeout = (timeout == null) ? Duration.ofSeconds(8) : timeout;
    }

    public List<Message> messages() { return messages; }
    public List<ToolDef> tools() { return tools; }
    public Duration timeout() { return timeout; }
}
