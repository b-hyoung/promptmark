package local.promptmark.service.llm;

import java.util.Collections;
import java.util.List;

/**
 * Chat history entry. Role-dependent — most fields are null for most roles:
 * <ul>
 *   <li>{@code system} / {@code user}: just {@code content}.</li>
 *   <li>{@code assistant}: {@code content} (may be empty) + optional {@code toolCalls}.</li>
 *   <li>{@code tool}: tool-result message — set {@code toolCallId}, {@code toolName},
 *       {@code content} (JSON string).</li>
 * </ul>
 */
public final class Message {

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_TOOL = "tool";

    private final String role;
    private final String content;
    private final String toolCallId;
    private final String toolName;
    private final List<ToolCall> toolCalls;

    private Message(String role, String content, String toolCallId,
                    String toolName, List<ToolCall> toolCalls) {
        this.role = role;
        this.content = content;
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.toolCalls = (toolCalls == null) ? Collections.emptyList() : toolCalls;
    }

    public static Message system(String content) {
        return new Message(ROLE_SYSTEM, content, null, null, null);
    }

    public static Message user(String content) {
        return new Message(ROLE_USER, content, null, null, null);
    }

    public static Message assistant(String content, List<ToolCall> toolCalls) {
        return new Message(ROLE_ASSISTANT, content, null, null, toolCalls);
    }

    public static Message tool(String toolCallId, String toolName, String content) {
        return new Message(ROLE_TOOL, content, toolCallId, toolName, null);
    }

    public String role() { return role; }
    public String content() { return content; }
    public String toolCallId() { return toolCallId; }
    public String toolName() { return toolName; }
    public List<ToolCall> toolCalls() { return toolCalls; }
}
