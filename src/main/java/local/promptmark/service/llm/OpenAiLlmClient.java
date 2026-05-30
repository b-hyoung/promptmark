package local.promptmark.service.llm;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * OpenAI Chat Completions client supporting tool calling. Adapts our shared
 * {@link Message}/{@link ToolDef}/{@link ToolCall} shapes to and from OpenAI's
 * JSON. Endpoint: {@code https://api.openai.com/v1/chat/completions}.
 */
public final class OpenAiLlmClient implements LlmClient {

    static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final HttpInvoker http;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiLlmClient(String apiKey, String model) {
        this(apiKey, model, HttpInvoker.real());
    }

    /** Test seam: caller supplies a fake HttpInvoker. */
    public OpenAiLlmClient(String apiKey, String model, HttpInvoker http) {
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
        if (!enabled()) throw new LlmException("OpenAI API key not configured");
        String body = buildRequestBody(request);
        Duration timeout = request.timeout() == null ? Duration.ofSeconds(8) : request.timeout();
        try {
            HttpInvoker.Response res = http.post(
                ENDPOINT,
                HttpInvoker.headers("Authorization", "Bearer " + apiKey),
                body,
                timeout
            );
            if (!res.isSuccess()) {
                throw new LlmException("OpenAI HTTP " + res.status() + ": "
                    + truncate(res.body()));
            }
            return parseReply(res.body());
        } catch (IOException e) {
            throw new LlmException("OpenAI request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("OpenAI request interrupted", e);
        }
    }

    String buildRequestBody(LlmRequest req) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        ArrayNode msgs = root.putArray("messages");
        for (Message m : req.messages()) {
            msgs.add(encodeMessage(m));
        }
        if (!req.tools().isEmpty()) {
            ArrayNode tools = root.putArray("tools");
            for (ToolDef td : req.tools()) {
                ObjectNode t = tools.addObject();
                t.put("type", "function");
                ObjectNode fn = t.putObject("function");
                fn.put("name", td.name());
                if (td.description() != null) fn.put("description", td.description());
                if (td.parametersSchema() != null) {
                    fn.set("parameters", td.parametersSchema());
                }
            }
        }
        try {
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new LlmException("failed to serialise OpenAI request", e);
        }
    }

    private ObjectNode encodeMessage(Message m) {
        ObjectNode node = mapper.createObjectNode();
        node.put("role", m.role());
        if (Message.ROLE_TOOL.equals(m.role())) {
            node.put("tool_call_id", m.toolCallId());
            node.put("content", m.content() == null ? "" : m.content());
            return node;
        }
        node.put("content", m.content() == null ? "" : m.content());
        if (m.toolCalls() != null && !m.toolCalls().isEmpty()) {
            ArrayNode calls = node.putArray("tool_calls");
            for (ToolCall c : m.toolCalls()) {
                ObjectNode tc = calls.addObject();
                tc.put("id", c.id());
                tc.put("type", "function");
                ObjectNode fn = tc.putObject("function");
                fn.put("name", c.name());
                try {
                    fn.put("arguments",
                        c.arguments() == null ? "{}" : mapper.writeValueAsString(c.arguments()));
                } catch (Exception e) {
                    fn.put("arguments", "{}");
                }
            }
        }
        return node;
    }

    LlmReply parseReply(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                throw new LlmException("OpenAI response missing choices");
            }
            JsonNode msg = choices.get(0).path("message");
            String role = msg.path("role").asText("assistant");
            String content = msg.path("content").isNull() ? "" : msg.path("content").asText("");
            List<ToolCall> calls = new ArrayList<>();
            JsonNode toolCalls = msg.path("tool_calls");
            if (toolCalls.isArray()) {
                for (JsonNode tc : toolCalls) {
                    String id = tc.path("id").asText("");
                    JsonNode fn = tc.path("function");
                    String name = fn.path("name").asText("");
                    String argsStr = fn.path("arguments").asText("{}");
                    JsonNode args;
                    try {
                        args = mapper.readTree(argsStr);
                    } catch (Exception parseErr) {
                        args = mapper.createObjectNode();
                    }
                    calls.add(new ToolCall(id, name, args));
                }
            }
            return new LlmReply(role, content, calls);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= 200 ? s : s.substring(0, 200) + "…";
    }
}
