package local.promptmark.service.llm;

/**
 * No-op LLM client returned by {@link LlmConfig#newLlmClient()} when no
 * provider/credential combo is configured. Any call throws so the agent can
 * detect the disabled state and bail to rule mode.
 */
public final class DisabledLlmClient implements LlmClient {

    @Override
    public LlmReply chat(LlmRequest request) {
        throw new LlmException("LLM client is disabled");
    }

    @Override
    public boolean enabled() {
        return false;
    }
}
