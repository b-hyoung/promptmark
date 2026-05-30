package local.promptmark.service.llm;

/**
 * Placeholder for the OpenAI /v1/embeddings client. Real HTTP implementation
 * lands in the next commit.
 */
public final class OpenAiEmbeddingClient implements EmbeddingClient {

    private final String apiKey;
    private final String model;

    public OpenAiEmbeddingClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public float[] embed(String text) {
        throw new LlmException("OpenAiEmbeddingClient not yet implemented");
    }

    @Override
    public boolean enabled() {
        return apiKey != null && !apiKey.isEmpty();
    }

    String apiKey() { return apiKey; }
    String model() { return model; }
}
