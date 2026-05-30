package local.promptmark.service.llm;

/**
 * Placeholder for the Anthropic /v1/messages client. Real implementation lands
 * in the next commit.
 */
public final class ClaudeLlmClient implements LlmClient {

    private final String apiKey;
    private final String model;

    public ClaudeLlmClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public LlmReply chat(LlmRequest request) {
        throw new LlmException("ClaudeLlmClient not yet implemented");
    }

    @Override
    public boolean enabled() {
        return apiKey != null && !apiKey.isEmpty();
    }

    String apiKey() { return apiKey; }
    String model() { return model; }
}
