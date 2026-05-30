package local.promptmark.service.llm;

/**
 * Placeholder for the OpenAI chat-completions client. Real HTTP implementation
 * arrives in the next commit; this stub exists so {@link LlmConfig} can compile
 * during the incremental build-up.
 */
public final class OpenAiLlmClient implements LlmClient {

    private final String apiKey;
    private final String model;

    public OpenAiLlmClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public LlmReply chat(LlmRequest request) {
        throw new LlmException("OpenAiLlmClient not yet implemented");
    }

    @Override
    public boolean enabled() {
        return apiKey != null && !apiKey.isEmpty();
    }

    String apiKey() { return apiKey; }
    String model() { return model; }
}
