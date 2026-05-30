package local.promptmark.dto;

/** Lifecycle for an orders row. Phase 3 only writes PAID (mock checkout). */
public enum OrderStatus {
    PAID, CANCELED;

    public static OrderStatus fromDb(String s) {
        if (s == null) return PAID;
        try {
            return OrderStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PAID;
        }
    }
}
