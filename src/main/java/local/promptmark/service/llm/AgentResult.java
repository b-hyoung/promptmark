package local.promptmark.service.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the chat endpoint returns. {@link #source()} is "AGENT" when the LLM
 * produced the answer, "RULE_FALLBACK" when the rule-mode path took over.
 */
public final class AgentResult {

    public static final String SOURCE_AGENT = "AGENT";
    public static final String SOURCE_RULE_FALLBACK = "RULE_FALLBACK";

    private final String answer;
    private final String source;
    private final List<PluginCard> items;
    private final List<TraceEntry> trace;

    public AgentResult(String answer, String source,
                       List<PluginCard> items, List<TraceEntry> trace) {
        this.answer = answer;
        this.source = source;
        this.items = (items == null) ? Collections.emptyList() : items;
        this.trace = (trace == null) ? Collections.emptyList() : trace;
    }

    public String getAnswer() { return answer; }
    public String getSource() { return source; }
    public List<PluginCard> getItems() { return items; }
    public List<TraceEntry> getTrace() { return trace; }

    /** Stable-key map for JSON serialisation. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("answer", answer);
        m.put("source", source);
        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (PluginCard c : items) itemMaps.add(c.toMap());
        m.put("items", itemMaps);
        List<Map<String, Object>> traceMaps = new ArrayList<>();
        for (TraceEntry t : trace) traceMaps.add(t.toMap());
        m.put("trace", traceMaps);
        return m;
    }
}
