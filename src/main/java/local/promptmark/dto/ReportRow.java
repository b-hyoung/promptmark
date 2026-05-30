package local.promptmark.dto;

import java.time.Instant;

/**
 * Joined view row used by the admin reports queue. Carries the report's basic
 * info together with the reported asset's title/type/status and the reporter's
 * nickname so the JSP can render context without further DB hits.
 */
public final class ReportRow {

    private final long reportId;
    private final String reason;
    private final Instant reportedAt;
    private final long assetId;
    private final String assetTitle;
    private final AssetType assetType;
    private final AssetStatus assetStatus;
    private final long reporterId;
    private final String reporterNickname;

    public ReportRow(long reportId,
                     String reason,
                     Instant reportedAt,
                     long assetId,
                     String assetTitle,
                     AssetType assetType,
                     AssetStatus assetStatus,
                     long reporterId,
                     String reporterNickname) {
        this.reportId = reportId;
        this.reason = reason;
        this.reportedAt = reportedAt;
        this.assetId = assetId;
        this.assetTitle = assetTitle;
        this.assetType = assetType;
        this.assetStatus = assetStatus;
        this.reporterId = reporterId;
        this.reporterNickname = reporterNickname;
    }

    public long getReportId() { return reportId; }
    public String getReason() { return reason; }
    public Instant getReportedAt() { return reportedAt; }
    public long getAssetId() { return assetId; }
    public String getAssetTitle() { return assetTitle; }
    public AssetType getAssetType() { return assetType; }
    public AssetStatus getAssetStatus() { return assetStatus; }
    public long getReporterId() { return reporterId; }
    public String getReporterNickname() { return reporterNickname; }

    /** EL-friendly view of the enum. */
    public String getAssetTypeName() { return assetType == null ? null : assetType.name(); }
    public String getAssetStatusName() { return assetStatus == null ? null : assetStatus.name(); }
}
