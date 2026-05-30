package local.promptmark.service.llm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scripted LLM client for tests. Returns the next pre-canned {@link LlmReply}
 * each time {@link #chat} is called; an optional per-call delay simulates a
 * slow provider. Used by {@code LlmAgentTest}, {@code RecommendServiceTest},
 * and any code that wants the agent's behaviour without a real provider.
 */
public final class FakeLlmClient implements LlmClient {

    private final List<LlmReply> replies;
    private int index = 0;
    private Duration perCallDelay = Duration.ZERO;
    private final List<LlmRequest> receivedRequests = new ArrayList<>();

    public FakeLlmClient(List<LlmReply> replies) {
        this.replies = (replies == null) ? new ArrayList<>() : new ArrayList<>(replies);
    }

    public FakeLlmClient(LlmReply... replies) {
        this(replies == null ? new ArrayList<>() : Arrays.asList(replies));
    }

    /** Adds an artificial delay per call. Used by timeout tests. */
    public FakeLlmClient withDelay(Duration d) {
        this.perCallDelay = (d == null) ? Duration.ZERO : d;
        return this;
    }

    public List<LlmRequest> receivedRequests() {
        return receivedRequests;
    }

    public int callCount() {
        return index;
    }

    @Override
    public LlmReply chat(LlmRequest request) {
        receivedRequests.add(request);
        if (perCallDelay != null && !perCallDelay.isZero() && !perCallDelay.isNegative()) {
            try {
                Thread.sleep(perCallDelay.toMillis());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new LlmException("interrupted", ie);
            }
        }
        if (index >= replies.size()) {
            throw new LlmException("FakeLlmClient exhausted (call " + (index + 1)
                + " has no scripted reply)");
        }
        return replies.get(index++);
    }

    @Override
    public boolean enabled() {
        return true;
    }
}
