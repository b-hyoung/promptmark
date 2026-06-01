package local.promptmark.dto;

public enum BundleStatus {
    PUBLIC, HIDDEN, DELETED;

    public static BundleStatus fromDb(String s) {
        if (s == null) return PUBLIC;
        return valueOf(s);
    }
}
