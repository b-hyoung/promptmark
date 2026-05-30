package local.promptmark.service.llm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Audit entry for one tool dispatch within the ReAct loop. Surfaced in
 * {@link AgentResult#trace()} so the UI can show the "agent thinking" panel.
 * {@link #hits()} is the number of results when applicable (search_assets),
 * null otherwise.
 */
public final class TraceEntry {

    private final String tool;
    private final Map<String, Object> args;
    private final Integer hits;

    public TraceEntry(String tool, Map<String, Object> args, Integer hits) {
        this.tool = tool;
        this.args = (args == null) ? Collections.emptyMap() : args;
        this.hits = hits;
    }

    public String getTool() { return tool; }
    public Map<String, Object> getArgs() { return args; }
    public Integer getHits() { return hits; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tool", tool);
        m.put("args", args);
        if (hits != null) m.put("hits", hits);
        return m;
    }
}
