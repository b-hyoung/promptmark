package local.promptmark.service.llm;

import java.io.IOException;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * OpenAI /v1/embeddings client. Default model is {@code text-embedding-3-small}
 * (1536 dims). 5-second timeout; on any non-2xx or parse failure throws
 * {@link LlmException} so callers degrade gracefully.
 */
public final class OpenAiEmbeddingClient implements EmbeddingClient {

    static final String ENDPOINT = "https://api.openai.com/v1/embeddings";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final String apiKey;
    private final String model;
    private final HttpInvoker http;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiEmbeddingClient(String apiKey, String model) {
        this(apiKey, model, HttpInvoker.real());
    }

    public OpenAiEmbeddingClient(String apiKey, String model, HttpInvoker http) {
        this.apiKey = apiKey;
        this.model = model;
        this.http = http;
    }

    @Override
    public boolean enabled() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public float[] embed(String text) {
        if (!enabled()) throw new LlmException("OpenAI API key not configured");
        if (text == null) text = "";

        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("input", text);

        String body;
        try {
            body = mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new LlmException("failed to serialise embedding request", e);
        }

        HttpInvoker.Response res;
        try {
            res = http.post(
                ENDPOINT,
                HttpInvoker.headers("Authorization", "Bearer " + apiKey),
                body,
                TIMEOUT
            );
        } catch (IOException e) {
            throw new LlmException("embedding request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("embedding request interrupted", e);
        }
        if (!res.isSuccess()) {
            throw new LlmException("embedding HTTP " + res.status());
        }

        try {
            JsonNode top = mapper.readTree(res.body());
            JsonNode data = top.path("data");
            if (!data.isArray() || data.size() == 0) {
                throw new LlmException("embedding response missing data");
            }
            JsonNode vec = data.get(0).path("embedding");
            if (!vec.isArray()) {
                throw new LlmException("embedding response missing vector");
            }
            float[] out = new float[vec.size()];
            for (int i = 0; i < vec.size(); i++) {
                out[i] = (float) vec.get(i).asDouble();
            }
            return out;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("failed to parse embedding response: " + e.getMessage(), e);
        }
    }
}
