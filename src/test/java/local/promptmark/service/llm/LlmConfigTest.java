package local.promptmark.service.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LlmConfigTest {

    @Test
    void disabled_when_no_provider() {
        LlmConfig c = new LlmConfig("", "sk-abc", "ant-abc", "", "", "");
        assertThat(c.enabled()).isFalse();
        assertThat(c.newLlmClient()).isInstanceOf(DisabledLlmClient.class);
    }

    @Test
    void disabled_when_openai_provider_but_no_key() {
        LlmConfig c = new LlmConfig("openai", "", "ant-abc", "", "", "");
        assertThat(c.enabled()).isFalse();
        assertThat(c.newLlmClient()).isInstanceOf(DisabledLlmClient.class);
    }

    @Test
    void disabled_when_claude_provider_but_no_key() {
        LlmConfig c = new LlmConfig("claude", "sk-abc", "", "", "", "");
        assertThat(c.enabled()).isFalse();
    }

    @Test
    void enabled_with_openai_provider_and_key() {
        LlmConfig c = new LlmConfig("openai", "sk-abc", "", "", "", "");
        assertThat(c.enabled()).isTrue();
        assertThat(c.newLlmClient()).isInstanceOf(OpenAiLlmClient.class);
    }

    @Test
    void enabled_with_claude_provider_and_key() {
        LlmConfig c = new LlmConfig("claude", "", "ant-abc", "", "", "");
        assertThat(c.enabled()).isTrue();
        assertThat(c.newLlmClient()).isInstanceOf(ClaudeLlmClient.class);
    }

    @Test
    void provider_string_is_normalised_lowercase() {
        LlmConfig c = new LlmConfig("  OPENAI  ", "sk-abc", "", "", "", "");
        assertThat(c.provider()).isEqualTo("openai");
        assertThat(c.enabled()).isTrue();
    }

    @Test
    void unknown_provider_disables() {
        LlmConfig c = new LlmConfig("gemini", "sk-abc", "", "", "", "");
        assertThat(c.enabled()).isFalse();
    }

    @Test
    void embedding_client_enabled_when_openai_key_set_even_with_claude_provider() {
        LlmConfig c = new LlmConfig("claude", "sk-openai", "ant-abc", "", "", "");
        assertThat(c.newEmbeddingClient()).isInstanceOf(OpenAiEmbeddingClient.class);
    }

    @Test
    void embedding_client_disabled_without_openai_key() {
        LlmConfig c = new LlmConfig("claude", "", "ant-abc", "", "", "");
        assertThat(c.newEmbeddingClient()).isInstanceOf(DisabledEmbeddingClient.class);
    }

    @Test
    void defaults_filled_in_for_empty_models() {
        LlmConfig c = new LlmConfig("openai", "sk-abc", "", "", "", "");
        assertThat(c.embeddingModel()).isEqualTo("text-embedding-3-small");
        assertThat(c.openaiChatModel()).isEqualTo("gpt-4o-mini");
        assertThat(c.claudeChatModel()).isEqualTo("claude-haiku-4-5-20251001");
    }

    @Test
    void disabled_factory_returns_disabled_clients() {
        LlmConfig c = LlmConfig.disabled();
        assertThat(c.enabled()).isFalse();
        assertThat(c.newLlmClient()).isInstanceOf(DisabledLlmClient.class);
        assertThat(c.newEmbeddingClient()).isInstanceOf(DisabledEmbeddingClient.class);
    }
}
