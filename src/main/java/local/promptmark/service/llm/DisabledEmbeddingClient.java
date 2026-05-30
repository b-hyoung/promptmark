package local.promptmark.service.llm;

/**
 * No-op embedding client used when {@code OPENAI_API_KEY} is missing. Calls
 * throw {@link LlmException} so callers degrade to keyword-only search.
 */
public final class DisabledEmbeddingClient implements EmbeddingClient {

    @Override
    public float[] embed(String text) {
        throw new LlmException("Embedding client is disabled");
    }

    @Override
    public boolean enabled() {
        return false;
    }
}
