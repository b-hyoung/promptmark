package local.promptmark.service.llm;

import local.promptmark.config.Env;

/**
 * Snapshot of LLM-related env vars. {@link #enabled()} is the single source of
 * truth for whether the AI path is active — when false, callers short-circuit
 * to the rule-mode fallback without ever instantiating a real HTTP client.
 */
public final class LlmConfig {

    public static final String PROVIDER_OPENAI = "openai";
    public static final String PROVIDER_CLAUDE = "claude";

    private static final String DEFAULT_OPENAI_CHAT_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_CLAUDE_CHAT_MODEL = "claude-haiku-4-5-20251001";
    private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small";

    private final String provider;
    private final String openaiKey;
    private final String claudeKey;
    private final String embeddingModel;
    private final String openaiChatModel;
    private final String claudeChatModel;

    public LlmConfig(String provider,
                     String openaiKey,
                     String claudeKey,
                     String embeddingModel,
                     String openaiChatModel,
                     String claudeChatModel) {
        this.provider = norm(provider);
        this.openaiKey = nullable(openaiKey);
        this.claudeKey = nullable(claudeKey);
        this.embeddingModel = (embeddingModel == null || embeddingModel.isEmpty())
            ? DEFAULT_EMBEDDING_MODEL : embeddingModel;
        this.openaiChatModel = (openaiChatModel == null || openaiChatModel.isEmpty())
            ? DEFAULT_OPENAI_CHAT_MODEL : openaiChatModel;
        this.claudeChatModel = (claudeChatModel == null || claudeChatModel.isEmpty())
            ? DEFAULT_CLAUDE_CHAT_MODEL : claudeChatModel;
    }

    public static LlmConfig fromEnv(Env env) {
        return new LlmConfig(
            env.getOrDefault("LLM_PROVIDER", ""),
            env.getOrDefault("OPENAI_API_KEY", ""),
            env.getOrDefault("CLAUDE_API_KEY", ""),
            env.getOrDefault("EMBEDDING_MODEL", ""),
            env.getOrDefault("OPENAI_CHAT_MODEL", ""),
            env.getOrDefault("CLAUDE_CHAT_MODEL", "")
        );
    }

    /** Convenience: disabled config with no credentials. */
    public static LlmConfig disabled() {
        return new LlmConfig("", "", "", "", "", "");
    }

    public String provider() { return provider; }
    public String openaiKey() { return openaiKey; }
    public String claudeKey() { return claudeKey; }
    public String embeddingModel() { return embeddingModel; }
    public String openaiChatModel() { return openaiChatModel; }
    public String claudeChatModel() { return claudeChatModel; }

    /**
     * @return true iff a provider is set AND the matching credential is non-empty.
     */
    public boolean enabled() {
        if (PROVIDER_OPENAI.equals(provider)) return notEmpty(openaiKey);
        if (PROVIDER_CLAUDE.equals(provider)) return notEmpty(claudeKey);
        return false;
    }

    /** Build a chat client for the configured provider, or a disabled stub. */
    public LlmClient newLlmClient() {
        if (!enabled()) {
            return new DisabledLlmClient();
        }
        if (PROVIDER_OPENAI.equals(provider)) {
            return new OpenAiLlmClient(openaiKey, openaiChatModel);
        }
        if (PROVIDER_CLAUDE.equals(provider)) {
            return new ClaudeLlmClient(claudeKey, claudeChatModel);
        }
        return new DisabledLlmClient();
    }

    /**
     * Build an embedding client. Currently only OpenAI offers a hosted
     * {@code text-embedding-3-small}; Claude users still get OpenAI embeddings
     * when {@code OPENAI_API_KEY} is set, otherwise a disabled stub.
     */
    public EmbeddingClient newEmbeddingClient() {
        if (notEmpty(openaiKey)) {
            return new OpenAiEmbeddingClient(openaiKey, embeddingModel);
        }
        return new DisabledEmbeddingClient();
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }

    private static String nullable(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
