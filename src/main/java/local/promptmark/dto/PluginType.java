package local.promptmark.dto;

/** Plugin content kind. PROMPT carries an inline body; MD points at an uploaded file. */
public enum PluginType {
    PROMPT, MD;

    /** Null/unknown safe parser for DB strings. */
    public static PluginType fromDb(String s) {
        if (s == null) return PROMPT;
        try {
            return PluginType.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PROMPT;
        }
    }
}
