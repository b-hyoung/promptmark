package local.promptmark.dto;

import java.time.Instant;

/**
 * Joined view row used by the admin reports queue. Carries the report's basic
 * info together with the reported plugin's title/type/status and the reporter's
 * nickname so the JSP can render context without further DB hits.
 */
public final class ReportRow {

    private final long reportId;
    private final String reason;
    private final Instant reportedAt;
    private final long pluginId;
    private final String pluginTitle;
    private final PluginType pluginType;
    private final PluginStatus pluginStatus;
    private final long reporterId;
    private final String reporterNickname;

    public ReportRow(long reportId,
                     String reason,
                     Instant reportedAt,
                     long pluginId,
                     String pluginTitle,
                     PluginType pluginType,
                     PluginStatus pluginStatus,
                     long reporterId,
                     String reporterNickname) {
        this.reportId = reportId;
        this.reason = reason;
        this.reportedAt = reportedAt;
        this.pluginId = pluginId;
        this.pluginTitle = pluginTitle;
        this.pluginType = pluginType;
        this.pluginStatus = pluginStatus;
        this.reporterId = reporterId;
        this.reporterNickname = reporterNickname;
    }

    public long getReportId() { return reportId; }
    public String getReason() { return reason; }
    public Instant getReportedAt() { return reportedAt; }
    public long getPluginId() { return pluginId; }
    public String getPluginTitle() { return pluginTitle; }
    public PluginType getPluginType() { return pluginType; }
    public PluginStatus getPluginStatus() { return pluginStatus; }
    public long getReporterId() { return reporterId; }
    public String getReporterNickname() { return reporterNickname; }

    /** EL-friendly view of the enum. */
    public String getPluginTypeName() { return pluginType == null ? null : pluginType.name(); }
    public String getPluginStatusName() { return pluginStatus == null ? null : pluginStatus.name(); }
}
