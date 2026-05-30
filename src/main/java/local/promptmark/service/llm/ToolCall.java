package local.promptmark.service.llm;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A single tool invocation requested by the model. {@link #id()} is the provider
 * tool-call id (echoed back when sending the tool result).
 */
public final class ToolCall {

    private final String id;
    private final String name;
    private final JsonNode arguments;

    public ToolCall(String id, String name, JsonNode arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public String id() { return id; }
    public String name() { return name; }
    public JsonNode arguments() { return arguments; }
}
