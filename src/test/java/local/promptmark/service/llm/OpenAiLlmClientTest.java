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

class OpenAiLlmClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void buildRequestBody_includes_model_messages_and_tools() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        HttpInvoker invoker = (url, headers, body, timeout) -> {
            captured.set(body);
            return new HttpInvoker.Response(200, sampleAssistantText("hello"));
        };
        ToolDef tool = new ToolDef("search_assets", "search",
            mapper.readTree("{\"type\":\"object\",\"properties\":{}}"));
        OpenAiLlmClient client = new OpenAiLlmClient("sk-test", "gpt-4o-mini", invoker);

        LlmRequest req = new LlmRequest(
            Arrays.asList(Message.system("you are a bot"), Message.user("hi")),
            Collections.singletonList(tool),
            Duration.ofSeconds(5)
        );
        client.chat(req);

        JsonNode body = mapper.readTree(captured.get());
        assertThat(body.path("model").asText()).isEqualTo("gpt-4o-mini");
        assertThat(body.path("messages").size()).isEqualTo(2);
        assertThat(body.path("messages").get(0).path("role").asText()).isEqualTo("system");
        assertThat(body.path("messages").get(1).path("role").asText()).isEqualTo("user");
        assertThat(body.path("tools").size()).isEqualTo(1);
        assertThat(body.path("tools").get(0).path("type").asText()).isEqualTo("function");
        assertThat(body.path("tools").get(0).path("function").path("name").asText())
            .isEqualTo("search_assets");
    }

    @Test
    void parseReply_returns_assistant_text_when_no_tool_calls() {
        HttpInvoker invoker = (url, headers, body, timeout) ->
            new HttpInvoker.Response(200, sampleAssistantText("done"));
        OpenAiLlmClient client = new OpenAiLlmClient("sk-test", "gpt-4o-mini", invoker);

        LlmReply reply = client.chat(new LlmRequest(
            Collections.singletonList(Message.user("hi")), null, Duration.ofSeconds(3)));

        assertThat(reply.content()).isEqualTo("done");
        assertThat(reply.toolCalls()).isEmpty();
    }

    @Test
    void parseReply_extracts_tool_calls_with_parsed_arguments() throws Exception {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode choices = root.putArray("choices");
        ObjectNode choice = choices.addObject();
        ObjectNode msg = choice.putObject("message");
        msg.put("role", "assistant");
        msg.putNull("content");
        ArrayNode calls = msg.putArray("tool_calls");
        ObjectNode c0 = calls.addObject();
        c0.put("id", "call_1");
        c0.put("type", "function");
        ObjectNode fn = c0.putObject("function");
        fn.put("name", "search_assets");
        fn.put("arguments", "{\"query\":\"hello\"}");
        String responseBody = mapper.writeValueAsString(root);

        HttpInvoker invoker = (url, headers, body, timeout) ->
            new HttpInvoker.Response(200, responseBody);
        OpenAiLlmClient client = new OpenAiLlmClient("sk-test", "gpt-4o-mini", invoker);

        LlmReply reply = client.chat(new LlmRequest(
            Collections.singletonList(Message.user("hi")), null, Duration.ofSeconds(3)));

        assertThat(reply.toolCalls()).hasSize(1);
        assertThat(reply.toolCalls().get(0).id()).isEqualTo("call_1");
        assertThat(reply.toolCalls().get(0).name()).isEqualTo("search_assets");
        assertThat(reply.toolCalls().get(0).arguments().path("query").asText()).isEqualTo("hello");
    }

    @Test
    void non2xx_throws_llm_exception() {
        HttpInvoker invoker = (url, headers, body, timeout) ->
            new HttpInvoker.Response(500, "oops");
        OpenAiLlmClient client = new OpenAiLlmClient("sk-test", "gpt-4o-mini", invoker);
        assertThatExceptionOfType(LlmException.class)
            .isThrownBy(() -> client.chat(new LlmRequest(
                Collections.singletonList(Message.user("hi")), null, Duration.ofSeconds(3))));
    }

    @Test
    void io_failure_wraps_into_llm_exception() {
        HttpInvoker invoker = (url, headers, body, timeout) -> {
            throw new IOException("network down");
        };
        OpenAiLlmClient client = new OpenAiLlmClient("sk-test", "gpt-4o-mini", invoker);
        assertThatExceptionOfType(LlmException.class)
            .isThrownBy(() -> client.chat(new LlmRequest(
                Collections.singletonList(Message.user("hi")), null, Duration.ofSeconds(3))));
    }

    @Test
    void disabled_when_key_blank() {
        OpenAiLlmClient client = new OpenAiLlmClient("", "gpt-4o-mini");
        assertThat(client.enabled()).isFalse();
        assertThatExceptionOfType(LlmException.class)
            .isThrownBy(() -> client.chat(new LlmRequest(
                Collections.singletonList(Message.user("x")), null, Duration.ofSeconds(1))));
    }

    private String sampleAssistantText(String content) {
        try {
            ObjectNode root = mapper.createObjectNode();
            ArrayNode choices = root.putArray("choices");
            ObjectNode choice = choices.addObject();
            ObjectNode msg = choice.putObject("message");
            msg.put("role", "assistant");
            msg.put("content", content);
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
