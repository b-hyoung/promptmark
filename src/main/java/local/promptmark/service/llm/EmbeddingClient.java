package local.promptmark.service.llm;

/**
 * Text → 1536-dim vector for hybrid RAG. Implementations should enforce their
 * own timeout (typically 5s) and throw {@link LlmException} on any failure so
 * the agent can fall back to keyword-only search.
 */
public interface EmbeddingClient {

    /**
     * @return a 1536-dim float array.
     * @throws LlmException on any failure.
     */
    float[] embed(String text);

    /** True when this implementation has credentials and is ready to embed. */
    default boolean enabled() {
        return true;
    }
}
