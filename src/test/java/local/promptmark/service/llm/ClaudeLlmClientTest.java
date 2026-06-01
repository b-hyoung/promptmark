package local.promptmark.service.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

class ClaudeLlmClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void buildRequestBody_extracts_system_and_keeps_user_messages() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        final String canned = sampleText("ok");
        HttpInvoker invoker = (url, headers, body, timeout) -> {
            captured.set(body);
            return new HttpInvoker.Response(200, canned);
        };
        ClaudeLlmClient client = new ClaudeLlmClient(
            "ant-test", "claude-haiku-4-5-20251001", invoker);

        client.chat(new LlmRequest(
            Arrays.asList(Message.system("you are bot"), Message.user("hi")),
            Collections.emptyList(),
            Duration.ofSeconds(5)
        ));

        JsonNode body = mapper.readTree(captured.get());
        assertThat(body.path("model").asText()).isEqualTo("claude-haiku-4-5-20251001");
        assertThat(body.path("system").asText()).isEqualTo("you are bot");
        assertThat(body.path("messages").size()).isEqualTo(1);
        assertThat(body.path("messages").get(0).path("role").asText()).isEqualTo("user");
        assertThat(body.path("messages").get(0).path("content").get(0).path("text").asText())
            .isEqualTo("hi");
    }

    @Test
    void buildRequestBody_includes_tools_with_input_schema() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        final String canned = sampleText("ok");
        HttpInvoker invoker = (url, headers, body, timeout) -> {
            captured.set(body);
            return new HttpInvoker.Response(200, canned);
        };
        ToolDef td = new ToolDef("search_plugins", "search",
            mapper.readTree("{\"type\":\"object\",\"properties\":{}}"));
        ClaudeLlmClient client = new ClaudeLlmClient(
            "ant-test", "claude-haiku-4-5-20251001", invoker);

        client.chat(new LlmRequest(
            Collections.singletonList(Message.user("x")),
            Collections.singletonList(td),
            Duration.ofSeconds(5)
        ));

        JsonNode body = mapper.readTree(captured.get());
        assertThat(body.path("tools").get(0).path("name").asText()).isEqualTo("search_plugins");
        assertThat(body.path("tools").get(0).path("input_schema").path("type").asText())
            .isEqualTo("object");
    }

    @Test
    void parseReply_collects_text_blocks() {
        HttpInvoker invoker = (url, headers, body, timeout) ->
            new HttpInvoker.Response(200, sampleText("hello world"));
        ClaudeLlmClient client = new ClaudeLlmClient(
            "ant-test", "claude-haiku-4-5-20251001", invoker);

        LlmReply r = client.chat(new LlmRequest(
            Collections.singletonList(Message.user("hi")), null, Duration.ofSeconds(3)));

        assertThat(r.content()).isEqualTo("hello world");
        assertThat(r.toolCalls()).isEmpty();
    }

    @Test
    void parseReply_extracts_tool_use_blocks() throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("role", "assistant");
        ArrayNode content = root.putArray("content");
        ObjectNode use = content.addObject();
        use.put("type", "tool_use");
        use.put("id", "toolu_1");
        use.put("name", "search_plugins");
        ObjectNode input = use.putObject("input");
        input.put("query", "auto");
        final String responseBody = mapper.writeValueAsString(root);

        HttpInvoker invoker = (url, headers, body, timeout) ->
            new HttpInvoker.Response(200, responseBody);
        ClaudeLlmClient client = new ClaudeLlmClient(
            "ant-test", "claude-haiku-4-5-20251001", invoker);

        LlmReply r = client.chat(new LlmRequest(
            Collections.singletonList(Message.user("hi")), null, Duration.ofSeconds(3)));

        assertThat(r.toolCalls()).hasSize(1);
        assertThat(r.toolCalls().get(0).id()).isEqualTo("toolu_1");
        assertThat(r.toolCalls().get(0).name()).isEqualTo("search_plugins");
        assertThat(r.toolCalls().get(0).arguments().path("query").asText()).isEqualTo("auto");
    }

    @Test
    void non2xx_throws_llm_exception() {
        HttpInvoker invoker = (url, headers, body, timeout) ->
            new HttpInvoker.Response(401, "auth");
        ClaudeLlmClient client = new ClaudeLlmClient(
            "ant-test", "claude-haiku-4-5-20251001", invoker);
        assertThatExceptionOfType(LlmException.class)
            .isThrownBy(() -> client.chat(new LlmRequest(
                Collections.singletonList(Message.user("x")), null, Duration.ofSeconds(3))));
    }

    @Test
    void io_failure_wraps() {
        HttpInvoker invoker = (url, headers, body, timeout) -> { throw new IOException("x"); };
        ClaudeLlmClient client = new ClaudeLlmClient(
            "ant-test", "claude-haiku-4-5-20251001", invoker);
        assertThatExceptionOfType(LlmException.class)
            .isThrownBy(() -> client.chat(new LlmRequest(
                Collections.singletonList(Message.user("x")), null, Duration.ofSeconds(3))));
    }

    @Test
    void disabled_when_key_blank() {
        ClaudeLlmClient client = new ClaudeLlmClient("", "claude");
        assertThat(client.enabled()).isFalse();
        assertThatExceptionOfType(LlmException.class)
            .isThrownBy(() -> client.chat(new LlmRequest(
                Collections.singletonList(Message.user("x")), null, Duration.ofSeconds(3))));
    }

    @Test
    void anthropic_headers_are_sent() throws Exception {
        AtomicReference<java.util.Map<String, String>> hdrs = new AtomicReference<>();
        final String canned = sampleText("ok");
        HttpInvoker invoker = (url, headers, body, timeout) -> {
            hdrs.set(headers);
            return new HttpInvoker.Response(200, canned);
        };
        ClaudeLlmClient client = new ClaudeLlmClient(
            "ant-test", "claude-haiku-4-5-20251001", invoker);
        client.chat(new LlmRequest(
            Collections.singletonList(Message.user("x")), null, Duration.ofSeconds(3)));
        assertThat(hdrs.get()).containsEntry("x-api-key", "ant-test");
        assertThat(hdrs.get()).containsEntry("anthropic-version", "2023-06-01");
    }

    private String sampleText(String text) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("role", "assistant");
            ArrayNode content = root.putArray("content");
            ObjectNode block = content.addObject();
            block.put("type", "text");
            block.put("text", text);
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
