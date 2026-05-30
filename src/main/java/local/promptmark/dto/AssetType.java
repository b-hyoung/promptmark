package local.promptmark.dto;

/** Asset content kind. PROMPT carries an inline body; MD points at an uploaded file. */
public enum AssetType {
    PROMPT, MD;

    /** Null/unknown safe parser for DB strings. */
    public static AssetType fromDb(String s) {
        if (s == null) return PROMPT;
        try {
            return AssetType.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PROMPT;
        }
    }
}
