package local.promptmark.service.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

class OpenAiEmbeddingClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void embeds_text_returns_float_array() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        final String canned = sampleResponse(new double[]{0.1, 0.2, 0.3});
        HttpInvoker invoker = (url, headers, body, timeout) -> {
            capturedBody.set(body);
            return new HttpInvoker.Response(200, canned);
        };
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(
            "sk-x", "text-embedding-3-small", invoker);

        float[] vec = client.embed("hello world");

        assertThat(vec).hasSize(3);
        assertThat(vec[0]).isEqualTo(0.1f);
        JsonNode sent = mapper.readTree(capturedBody.get());
        assertThat(sent.path("model").asText()).isEqualTo("text-embedding-3-small");
        assertThat(sent.path("input").asText()).isEqualTo("hello world");
    }

    @Test
    void non2xx_throws_llm_exception() {
        HttpInvoker invoker = (url, headers, body, timeout) ->
            new HttpInvoker.Response(429, "rate limited");
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(
            "sk-x", "text-embedding-3-small", invoker);
        assertThatExceptionOfType(LlmException.class)
            .isThrownBy(() -> client.embed("hello"));
    }

    @Test
    void io_failure_wraps_into_llm_exception() {
        HttpInvoker invoker = (url, headers, body, timeout) -> {
            throw new IOException("boom");
        };
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(
            "sk-x", "text-embedding-3-small", invoker);
        assertThatExceptionOfType(LlmException.class)
            .isThrownBy(() -> client.embed("hello"));
    }

    @Test
    void disabled_when_key_blank() {
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(
            "", "text-embedding-3-small");
        assertThat(client.enabled()).isFalse();
        assertThatExceptionOfType(LlmException.class)
            .isThrownBy(() -> client.embed("x"));
    }

    private String sampleResponse(double[] values) throws Exception {
        ObjectNode top = mapper.createObjectNode();
        ArrayNode data = top.putArray("data");
        ObjectNode first = data.addObject();
        ArrayNode arr = first.putArray("embedding");
        for (double v : values) arr.add(v);
        return mapper.writeValueAsString(top);
    }
}
