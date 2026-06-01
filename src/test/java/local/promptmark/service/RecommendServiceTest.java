package local.promptmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import local.promptmark.dao.PluginDao;
import local.promptmark.dao.TagDao;
import local.promptmark.service.llm.AgentResult;
import local.promptmark.service.llm.PluginCard;
import local.promptmark.service.llm.DisabledEmbeddingClient;
import local.promptmark.service.llm.EmbeddingClient;
import local.promptmark.service.llm.FakeEmbeddingClient;
import local.promptmark.service.llm.LlmAgent;
import local.promptmark.service.llm.LlmConfig;
import local.promptmark.service.llm.LlmException;

class RecommendServiceTest {

    private PluginDao pluginDao;
    private TagDao tagDao;
    private LlmAgent llmAgent;

    @BeforeEach
    void setUp() {
        pluginDao = Mockito.mock(PluginDao.class);
        tagDao = Mockito.mock(TagDao.class);
        llmAgent = Mockito.mock(LlmAgent.class);
    }

    @Test
    void short_message_returns_rule_fallback_immediately() {
        EmbeddingClient embed = new DisabledEmbeddingClient();
        RecommendService svc = new RecommendService(pluginDao, tagDao, embed,
            llmAgent, LlmConfig.disabled());
        AgentResult r = svc.recommend("?");
        assertThat(r.getSource()).isEqualTo("RULE_FALLBACK");
        assertThat(r.getAnswer()).contains("자세히");
        Mockito.verifyNoInteractions(llmAgent);
    }

    @Test
    void llm_disabled_uses_rule_fallback() {
        when(pluginDao.searchHybrid(any(), any(), any(), any(), Mockito.anyInt()))
            .thenReturn(Collections.emptyList());

        RecommendService svc = new RecommendService(pluginDao, tagDao,
            new DisabledEmbeddingClient(), llmAgent, LlmConfig.disabled());
        AgentResult r = svc.recommend("좋은 프롬프트 추천해줘");
        assertThat(r.getSource()).isEqualTo("RULE_FALLBACK");
        Mockito.verifyNoInteractions(llmAgent);
    }

    @Test
    void rule_fallback_with_results_uses_first_title() {
        PluginCard card = new PluginCard(1, "PROMPT", "자기소개서 첨삭", "...", 0,
            0.8, Arrays.asList("취업"), null);
        when(pluginDao.searchHybrid(any(), any(), any(), any(), Mockito.anyInt()))
            .thenReturn(Collections.singletonList(card));

        RecommendService svc = new RecommendService(pluginDao, tagDao,
            new DisabledEmbeddingClient(), llmAgent, LlmConfig.disabled());
        AgentResult r = svc.recommend("자기소개서 첨삭");
        assertThat(r.getSource()).isEqualTo("RULE_FALLBACK");
        assertThat(r.getAnswer()).contains("자기소개서 첨삭");
        assertThat(r.getItems()).hasSize(1);
    }

    @Test
    void rule_fallback_with_no_results_uses_empty_message() {
        when(pluginDao.searchHybrid(any(), any(), any(), any(), Mockito.anyInt()))
            .thenReturn(Collections.emptyList());
        RecommendService svc = new RecommendService(pluginDao, tagDao,
            new DisabledEmbeddingClient(), llmAgent, LlmConfig.disabled());
        AgentResult r = svc.recommend("정말 모를것같은 토픽");
        assertThat(r.getAnswer()).contains("없");
    }

    @Test
    void llm_enabled_and_succeeds_returns_agent_source() {
        AgentResult agentReply = new AgentResult("ok", "AGENT",
            Collections.emptyList(), Collections.emptyList());
        when(llmAgent.run(any())).thenReturn(agentReply);

        LlmConfig enabled = new LlmConfig("openai", "sk-x", "", "", "", "");
        RecommendService svc = new RecommendService(pluginDao, tagDao,
            new FakeEmbeddingClient(), llmAgent, enabled);

        AgentResult r = svc.recommend("hello world");
        assertThat(r.getSource()).isEqualTo("AGENT");
        assertThat(r.getAnswer()).isEqualTo("ok");
    }

    @Test
    void llm_throws_falls_back_to_rule_mode() {
        when(llmAgent.run(any())).thenThrow(new LlmException("boom"));
        when(pluginDao.searchHybrid(any(), any(), any(), any(), Mockito.anyInt()))
            .thenReturn(Collections.emptyList());

        LlmConfig enabled = new LlmConfig("openai", "sk-x", "", "", "", "");
        RecommendService svc = new RecommendService(pluginDao, tagDao,
            new FakeEmbeddingClient(), llmAgent, enabled);

        AgentResult r = svc.recommend("hello world");
        assertThat(r.getSource()).isEqualTo("RULE_FALLBACK");
    }

    @Test
    void long_message_is_truncated_to_1000_chars() {
        when(llmAgent.run(any())).thenAnswer(invocation -> {
            String msg = invocation.getArgument(0);
            return new AgentResult("len=" + msg.length(), "AGENT",
                Collections.emptyList(), Collections.emptyList());
        });
        LlmConfig enabled = new LlmConfig("openai", "sk-x", "", "", "", "");
        RecommendService svc = new RecommendService(pluginDao, tagDao,
            new FakeEmbeddingClient(), llmAgent, enabled);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1500; i++) sb.append('a');
        AgentResult r = svc.recommend(sb.toString());
        assertThat(r.getAnswer()).isEqualTo("len=1000");
    }
}
