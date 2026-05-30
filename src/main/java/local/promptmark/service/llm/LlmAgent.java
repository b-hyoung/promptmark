package local.promptmark.service.llm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ReAct-style agent loop. For each user message we let the LLM run up to
 * {@value #MAX_TURNS} turns, dispatching any tool calls it issues against
 * {@link Tools}. Total wall-clock budget is {@value #BUDGET_SECONDS} seconds.
 *
 * <p>If everything succeeds we package the assistant text + accumulated
 * {@link AssetCard} list + per-tool {@link TraceEntry} into an
 * {@link AgentResult} with {@code source = "AGENT"}. Any {@link LlmException}
 * propagates up so {@code RecommendService} can fall back to rule mode.
 */
public final class LlmAgent {

    public static final int MAX_TURNS = 3;
    public static final int BUDGET_SECONDS = 8;
    public static final String SYSTEM_PROMPT =
        "당신은 프롬프트/MD 자산 추천 에이전트입니다. 사용자 요청을 이해하고 "
        + "search_assets·get_asset_detail 도구로 정보를 수집한 뒤 추천 답변을 생성하세요. "
        + "후보가 없으면 솔직히 없다고 답하세요.";

    private static final Logger log = LoggerFactory.getLogger(LlmAgent.class);

    private final LlmClient client;
    @SuppressWarnings("unused")
    private final EmbeddingClient embeddingClient;
    private final Tools tools;
    @SuppressWarnings("unused")
    private final LlmConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmAgent(LlmClient client, EmbeddingClient embeddingClient,
                    Tools tools, LlmConfig config) {
        this.client = client;
        this.embeddingClient = embeddingClient;
        this.tools = tools;
        this.config = config;
    }

    /**
     * Run the loop. Returns an {@link AgentResult} on success. Throws
     * {@link LlmException} when the LLM fails or the budget runs out without
     * a usable answer.
     */
    public AgentResult run(String userMessage) {
        long deadlineNanos = System.nanoTime()
            + Duration.ofSeconds(BUDGET_SECONDS).toNanos();

        List<Message> messages = new ArrayList<>();
        messages.add(Message.system(SYSTEM_PROMPT));
        messages.add(Message.user(userMessage));

        List<TraceEntry> trace = new ArrayList<>();
        List<AssetCard> items = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        String answer = "";

        for (int turn = 0; turn < MAX_TURNS; turn++) {
            if (System.nanoTime() > deadlineNanos) {
                log.debug("LlmAgent budget exhausted before turn {}", turn);
                break;
            }
            Duration remaining = remaining(deadlineNanos);
            LlmRequest req = new LlmRequest(messages, tools.definitions(), remaining);

            LlmReply reply;
            try {
                reply = client.chat(req);
            } catch (LlmException e) {
                log.debug("LlmAgent client error on turn {}: {}", turn, e.getMessage());
                throw e;
            }

            if (!reply.hasToolCalls()) {
                answer = reply.content();
                return new AgentResult(answer, AgentResult.SOURCE_AGENT, items, trace);
            }

            // Append the assistant's tool-call message before processing results.
            messages.add(Message.assistant(reply.content(), reply.toolCalls()));

            for (ToolCall call : reply.toolCalls()) {
                JsonNode result;
                try {
                    result = tools.dispatch(call.name(), call.arguments());
                } catch (RuntimeException ex) {
                    // Surface tool errors back to the model so it can adjust.
                    String msg = ex.getMessage() == null ? "error" : ex.getMessage();
                    result = mapper.createObjectNode().put("error", msg);
                    trace.add(new TraceEntry(call.name(),
                        argsAsMap(call.arguments()), null));
                    messages.add(Message.tool(call.id(), call.name(), result.toString()));
                    continue;
                }

                Integer hits = null;
                if (Tools.TOOL_SEARCH_ASSETS.equals(call.name()) && result.isArray()) {
                    hits = result.size();
                    collectCards((ArrayNode) result, items, seenIds);
                }
                trace.add(new TraceEntry(call.name(), argsAsMap(call.arguments()), hits));
                messages.add(Message.tool(call.id(), call.name(), result.toString()));
            }
        }

        // Loop exhausted MAX_TURNS without a final answer — try one more
        // synchronous turn with no tools, but only if budget remains.
        if (System.nanoTime() <= deadlineNanos) {
            try {
                LlmReply finalReply = client.chat(new LlmRequest(
                    messages, java.util.Collections.emptyList(), remaining(deadlineNanos)));
                answer = finalReply.content();
            } catch (LlmException e) {
                // Use whatever we have.
                if (answer.isEmpty()) throw e;
            }
        }
        if (answer == null || answer.isEmpty()) {
            answer = items.isEmpty()
                ? "추천 결과를 생성하지 못했어요."
                : "추천 후보를 정리했어요. 아래 자산을 확인해보세요.";
        }
        return new AgentResult(answer, AgentResult.SOURCE_AGENT, items, trace);
    }

    private Duration remaining(long deadlineNanos) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) return Duration.ofMillis(100);
        return Duration.ofNanos(remainingNanos);
    }

    private Map<String, Object> argsAsMap(JsonNode args) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (args == null || args.isNull() || !args.isObject()) return out;
        args.fields().forEachRemaining(e -> {
            JsonNode v = e.getValue();
            if (v.isNumber()) out.put(e.getKey(), v.numberValue());
            else if (v.isBoolean()) out.put(e.getKey(), v.booleanValue());
            else if (v.isNull()) out.put(e.getKey(), null);
            else out.put(e.getKey(), v.asText());
        });
        return out;
    }

    private void collectCards(ArrayNode array, List<AssetCard> sink, Set<Long> seen) {
        for (JsonNode n : array) {
            long id = n.path("id").asLong();
            if (id == 0L || seen.contains(id)) continue;
            seen.add(id);
            List<String> tags = new ArrayList<>();
            for (JsonNode t : n.path("tags")) {
                if (t.isTextual()) tags.add(t.asText());
            }
            sink.add(new AssetCard(
                id,
                n.path("type").asText(""),
                n.path("title").asText(""),
                n.path("summary").asText(""),
                n.path("price").asInt(0),
                n.path("score").asDouble(0.0),
                tags,
                null
            ));
        }
    }
}
