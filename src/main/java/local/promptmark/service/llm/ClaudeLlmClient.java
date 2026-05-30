package local.promptmark.service.llm;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Anthropic /v1/messages client supporting Claude tool use. Maps our shared
 * {@link Message}/{@link ToolDef}/{@link ToolCall} model to Anthropic's
 * blocks-based format (system at top level, tool definitions with
 * {@code input_schema}, tool calls via {@code content} blocks of type
 * {@code tool_use}).
 */
public final class ClaudeLlmClient implements LlmClient {

    static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int DEFAULT_MAX_TOKENS = 1024;

    private final String apiKey;
    private final String model;
    private final HttpInvoker http;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClaudeLlmClient(String apiKey, String model) {
        this(apiKey, model, HttpInvoker.real());
    }

    public ClaudeLlmClient(String apiKey, String model, HttpInvoker http) {
        this.apiKey = apiKey;
        this.model = model;
        this.http = http;
    }

    @Override
    public boolean enabled() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public LlmReply chat(LlmRequest request) {
        if (!enabled()) throw new LlmException("Claude API key not configured");
        String body = buildRequestBody(request);
        Duration timeout = request.timeout() == null ? Duration.ofSeconds(8) : request.timeout();
        try {
            HttpInvoker.Response res = http.post(
                ENDPOINT,
                HttpInvoker.headers(
                    "x-api-key", apiKey,
                    "anthropic-version", ANTHROPIC_VERSION
                ),
                body,
                timeout
            );
            if (!res.isSuccess()) {
                throw new LlmException("Claude HTTP " + res.status() + ": "
                    + truncate(res.body()));
            }
            return parseReply(res.body());
        } catch (IOException e) {
            throw new LlmException("Claude request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Claude request interrupted", e);
        }
    }

    String buildRequestBody(LlmRequest req) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", DEFAULT_MAX_TOKENS);

        // Split out system messages — Claude expects them at the top level.
        StringBuilder system = new StringBuilder();
        ArrayNode messages = mapper.createArrayNode();
        for (Message m : req.messages()) {
            if (Message.ROLE_SYSTEM.equals(m.role())) {
                if (system.length() > 0) system.append("\n\n");
                if (m.content() != null) system.append(m.content());
            } else if (Message.ROLE_USER.equals(m.role())) {
                messages.add(encodeText("user", m.content()));
            } else if (Message.ROLE_ASSISTANT.equals(m.role())) {
                messages.add(encodeAssistant(m));
            } else if (Message.ROLE_TOOL.equals(m.role())) {
                messages.add(encodeToolResult(m));
            }
        }
        if (system.length() > 0) root.put("system", system.toString());
        root.set("messages", messages);

        if (!req.tools().isEmpty()) {
            ArrayNode tools = root.putArray("tools");
            for (ToolDef td : req.tools()) {
                ObjectNode t = tools.addObject();
                t.put("name", td.name());
                if (td.description() != null) t.put("description", td.description());
                if (td.parametersSchema() != null) {
                    t.set("input_schema", td.parametersSchema());
                }
            }
        }
        try {
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new LlmException("failed to serialise Claude request", e);
        }
    }

    private ObjectNode encodeText(String role, String content) {
        ObjectNode node = mapper.createObjectNode();
        node.put("role", role);
        ArrayNode contentArr = node.putArray("content");
        ObjectNode block = contentArr.addObject();
        block.put("type", "text");
        block.put("text", content == null ? "" : content);
        return node;
    }

    private ObjectNode encodeAssistant(Message m) {
        ObjectNode node = mapper.createObjectNode();
        node.put("role", "assistant");
        ArrayNode contentArr = node.putArray("content");
        if (m.content() != null && !m.content().isEmpty()) {
            ObjectNode text = contentArr.addObject();
            text.put("type", "text");
            text.put("text", m.content());
        }
        for (ToolCall c : m.toolCalls()) {
            ObjectNode use = contentArr.addObject();
            use.put("type", "tool_use");
            use.put("id", c.id());
            use.put("name", c.name());
            use.set("input",
                c.arguments() == null ? mapper.createObjectNode() : c.arguments());
        }
        return node;
    }

    private ObjectNode encodeToolResult(Message m) {
        ObjectNode node = mapper.createObjectNode();
        node.put("role", "user"); // Claude carries tool_result blocks under a user message
        ArrayNode contentArr = node.putArray("content");
        ObjectNode block = contentArr.addObject();
        block.put("type", "tool_result");
        block.put("tool_use_id", m.toolCallId());
        block.put("content", m.content() == null ? "" : m.content());
        return node;
    }

    LlmReply parseReply(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            String role = root.path("role").asText("assistant");
            JsonNode content = root.path("content");

            StringBuilder text = new StringBuilder();
            List<ToolCall> calls = new ArrayList<>();
            if (content.isArray()) {
                for (JsonNode block : content) {
                    String type = block.path("type").asText("");
                    if ("text".equals(type)) {
                        if (text.length() > 0) text.append('\n');
                        text.append(block.path("text").asText(""));
                    } else if ("tool_use".equals(type)) {
                        String id = block.path("id").asText("");
                        String name = block.path("name").asText("");
                        JsonNode input = block.path("input");
                        if (input.isMissingNode() || input.isNull()) {
                            input = mapper.createObjectNode();
                        }
                        calls.add(new ToolCall(id, name, input));
                    }
                }
            }
            return new LlmReply(role, text.toString(), calls);
        } catch (Exception e) {
            throw new LlmException("failed to parse Claude response: " + e.getMessage(), e);
        }
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= 200 ? s : s.substring(0, 200) + "…";
    }
}
