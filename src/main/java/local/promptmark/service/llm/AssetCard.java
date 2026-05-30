package local.promptmark.service.llm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Slim view of an asset surfaced by the agent. Used for the {@code items[]}
 * array in {@link AgentResult} and the JSON returned from {@link Tools}.
 * Type is held as a string ({@code "PROMPT"} | {@code "MD"}) so JSP / JSON
 * serializers can read it directly without extra mapping.
 */
public final class AssetCard {

    private final long id;
    private final String type;
    private final String title;
    private final String summary;
    private final int price;
    private final double score;
    private final List<String> tags;
    private final Double reason;

    public AssetCard(long id, String type, String title, String summary,
                     int price, double score, List<String> tags, Double reason) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.summary = summary;
        this.price = price;
        this.score = score;
        this.tags = (tags == null) ? Collections.emptyList() : tags;
        this.reason = reason;
    }

    public long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public int getPrice() { return price; }
    public double getScore() { return score; }
    public List<String> getTags() { return tags; }
    public Double getReason() { return reason; }

    /** Map view for JSON serialisation. Keeps key ordering stable. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("type", type);
        m.put("title", title);
        m.put("summary", summary);
        m.put("price", price);
        m.put("score", score);
        m.put("tags", tags);
        if (reason != null) m.put("reason", reason);
        return m;
    }
}
