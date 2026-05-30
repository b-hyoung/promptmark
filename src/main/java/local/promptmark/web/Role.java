package local.promptmark.web;

/**
 * Access roles, ordered by privilege (ADMIN highest).
 *
 * <p>Hierarchy: {@code ADMIN > SELLER > USER > ANONYMOUS}. A higher role
 * automatically satisfies any lower-role requirement.
 */
public enum Role {
    ANONYMOUS(0),
    USER(1),
    SELLER(2),
    ADMIN(3);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    /**
     * @return true when {@code current} is at least as privileged as this required role.
     *         {@code USER.satisfiedBy(ADMIN) == true}, but {@code ADMIN.satisfiedBy(USER) == false}.
     *         A null current is treated as ANONYMOUS.
     */
    public boolean satisfiedBy(Role current) {
        Role c = (current == null) ? ANONYMOUS : current;
        return c.level >= this.level;
    }

    /**
     * Parse a role string from the DB. Returns USER on null/empty/unknown so callers
     * never NPE on malformed rows; the auth layer will still gate access correctly.
     */
    public static Role fromDb(String dbVal) {
        if (dbVal == null) return USER;
        switch (dbVal.trim().toUpperCase()) {
            case "ADMIN":     return ADMIN;
            case "SELLER":    return SELLER;
            case "USER":      return USER;
            case "ANONYMOUS": return ANONYMOUS;
            default:          return USER;
        }
    }
}
