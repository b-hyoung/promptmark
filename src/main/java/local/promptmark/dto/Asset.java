package local.promptmark.dto;

import java.time.Instant;

/**
 * Mutable-via-constructor row mapping for {@code assets}. JavaBean accessors so
 * JSP EL can read it directly; extra {@code getTypeName()}/{@code getStatusName()}
 * give EL a string view of the enum without {@code .name()} syntax.
 */
public final class Asset {

    private final long id;
    private final long sellerId;
    private final AssetType type;
    private final String title;
    private final String summary;
    private final String body;
    private final String fileKey;
    private final String demoUrl;
    private final String videoUrl;
    private final int price;
    private final AssetStatus status;
    private final int viewCount;
    private final int downloadCount;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Asset(long id,
                 long sellerId,
                 AssetType type,
                 String title,
                 String summary,
                 String body,
                 String fileKey,
                 String demoUrl,
                 String videoUrl,
                 int price,
                 AssetStatus status,
                 int viewCount,
                 int downloadCount,
                 Instant createdAt,
                 Instant updatedAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.type = type;
        this.title = title;
        this.summary = summary;
        this.body = body;
        this.fileKey = fileKey;
        this.demoUrl = demoUrl;
        this.videoUrl = videoUrl;
        this.price = price;
        this.status = status;
        this.viewCount = viewCount;
        this.downloadCount = downloadCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public long getSellerId() { return sellerId; }
    public AssetType getType() { return type; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getBody() { return body; }
    public String getFileKey() { return fileKey; }
    public String getDemoUrl() { return demoUrl; }
    public String getVideoUrl() { return videoUrl; }
    public int getPrice() { return price; }
    public AssetStatus getStatus() { return status; }
    public int getViewCount() { return viewCount; }
    public int getDownloadCount() { return downloadCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** EL-friendly: {@code ${asset.typeName == 'PROMPT'}}. */
    public String getTypeName() { return type == null ? null : type.name(); }
    /** EL-friendly: {@code ${asset.statusName == 'PUBLIC'}}. */
    public String getStatusName() { return status == null ? null : status.name(); }
}
