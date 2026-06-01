package local.promptmark.dto;

/** Visibility lifecycle for an plugin row. */
public enum PluginStatus {
    PUBLIC, HIDDEN, DELETED;

    public static PluginStatus fromDb(String s) {
        if (s == null) return PUBLIC;
        try {
            return PluginStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PUBLIC;
        }
    }
}
