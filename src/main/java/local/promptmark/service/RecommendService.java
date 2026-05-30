package local.promptmark.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import local.promptmark.dao.AssetDao;
import local.promptmark.dao.TagDao;
import local.promptmark.service.llm.AgentResult;
import local.promptmark.service.llm.AssetCard;
import local.promptmark.service.llm.EmbeddingClient;
import local.promptmark.service.llm.LlmAgent;
import local.promptmark.service.llm.LlmConfig;
import local.promptmark.service.llm.LlmException;
import local.promptmark.service.llm.Tools;

/**
 * Entry point for {@code POST /app/chat/recommend}. Wraps the agent with
 * input sanitisation, the rule-mode fallback, and the disabled-LLM short
 * circuit.
 */
public final class RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RecommendService.class);
    private static final int MAX_MESSAGE_LEN = 1000;
    private static final int MIN_MESSAGE_LEN = 2;
    private static final int RULE_FALLBACK_LIMIT = 5;

    private final AssetDao assetDao;
    @SuppressWarnings("unused")
    private final TagDao tagDao;
    private final EmbeddingClient embeddingClient;
    private final LlmAgent llmAgent;
    private final LlmConfig llmConfig;

    public RecommendService(AssetDao assetDao,
                            TagDao tagDao,
                            EmbeddingClient embeddingClient,
                            LlmAgent llmAgent,
                            LlmConfig llmConfig) {
        this.assetDao = assetDao;
        this.tagDao = tagDao;
        this.embeddingClient = embeddingClient;
        this.llmAgent = llmAgent;
        this.llmConfig = llmConfig;
    }

    /**
     * Recommend assets for {@code userMessage}. Always returns — never throws —
     * so the chat endpoint can render the response uniformly.
     */
    public AgentResult recommend(String userMessage) {
        String msg = sanitize(userMessage);
        if (msg.length() < MIN_MESSAGE_LEN) {
            return new AgentResult(
                "질문을 좀 더 자세히 입력해주세요.",
                AgentResult.SOURCE_RULE_FALLBACK,
                Collections.emptyList(),
                Collections.emptyList()
            );
        }

        if (llmConfig != null && llmConfig.enabled() && llmAgent != null) {
            try {
                return llmAgent.run(msg);
            } catch (LlmException e) {
                log.warn("LLM agent failed, falling back to rule mode: {}", e.getMessage());
            } catch (RuntimeException e) {
                log.warn("LLM agent threw unexpected error, falling back: {}", e.getMessage(), e);
            }
        }

        return ruleFallback(msg);
    }

    private AgentResult ruleFallback(String msg) {
        String[] tokens = Tools.tokenize(msg);
        List<AssetCard> items;
        try {
            items = assetDao.searchHybrid(tokens, null, null, null, RULE_FALLBACK_LIMIT);
        } catch (RuntimeException e) {
            log.warn("rule fallback search failed: {}", e.getMessage());
            items = new ArrayList<>();
        }

        String answer;
        if (items.isEmpty()) {
            answer = "조건에 맞는 자산이 없네요. 다른 키워드로 시도해보세요.";
        } else {
            AssetCard first = items.get(0);
            answer = "추천 자산을 찾았어요. 첫 번째는 '" + first.getTitle() + "'입니다.";
        }
        return new AgentResult(answer, AgentResult.SOURCE_RULE_FALLBACK,
            items, Collections.emptyList());
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() > MAX_MESSAGE_LEN) t = t.substring(0, MAX_MESSAGE_LEN);
        return t;
    }

    /** Test seam — exposes the configured embedding client so callers can verify wiring. */
    public boolean embeddingEnabled() {
        return embeddingClient != null && embeddingClient.enabled();
    }
}
