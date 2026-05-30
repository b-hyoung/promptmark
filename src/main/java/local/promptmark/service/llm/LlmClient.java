package local.promptmark.service.llm;

/**
 * Provider-agnostic LLM facade. One method, synchronous. Implementations adapt
 * provider-specific tool-calling JSON to the shared {@link LlmRequest} /
 * {@link LlmReply} shape used by {@code LlmAgent}.
 */
public interface LlmClient {

    /**
     * @throws LlmException on HTTP errors, timeouts, malformed responses, or
     *                      when no API key is configured.
     */
    LlmReply chat(LlmRequest request);

    /** True when this implementation has the credentials needed to make a call. */
    default boolean enabled() {
        return true;
    }
}
