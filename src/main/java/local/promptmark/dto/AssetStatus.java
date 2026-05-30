package local.promptmark.dto;

/** Visibility lifecycle for an asset row. */
public enum AssetStatus {
    PUBLIC, HIDDEN, DELETED;

    public static AssetStatus fromDb(String s) {
        if (s == null) return PUBLIC;
        try {
            return AssetStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PUBLIC;
        }
    }
}
