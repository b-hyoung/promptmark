package local.promptmark.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Curated bundle (셋트) — admin-maintained collection of plugins sold as a unit.
 * The {@code plugins} list is only populated by repository methods that explicitly join.
 */
public class Bundle {
    private final long id;
    private final Long curatorId;
    private final String slug;
    private final String name;
    private final String tagline;
    private final String story;
    private final int price;
    private final String thumbnail;
    private final BundleStatus status;
    private final int viewCount;
    private final Instant createdAt;
    private final Instant updatedAt;

    private List<Plugin> plugins = new ArrayList<>();

    public Bundle(long id, Long curatorId, String slug, String name, String tagline,
                  String story, int price, String thumbnail, BundleStatus status,
                  int viewCount, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.curatorId = curatorId;
        this.slug = slug;
        this.name = name;
        this.tagline = tagline;
        this.story = story;
        this.price = price;
        this.thumbnail = thumbnail;
        this.status = status;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public Long getCuratorId() { return curatorId; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getTagline() { return tagline; }
    public String getStory() { return story; }
    public int getPrice() { return price; }
    public String getThumbnail() { return thumbnail; }
    public BundleStatus getStatus() { return status; }
    public int getViewCount() { return viewCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<Plugin> getPlugins() { return plugins; }
    public void setPlugins(List<Plugin> plugins) { this.plugins = plugins == null ? new ArrayList<>() : plugins; }
}
