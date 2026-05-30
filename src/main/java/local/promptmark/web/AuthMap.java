package local.promptmark.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Central registry mapping {@code module.action} keys to the minimum role
 * required to invoke them. Unmapped actions default to {@link Role#USER}
 * (safe default — explicit ANONYMOUS entries opt actions out of auth).
 */
public final class AuthMap {

    private static final Map<String, Role> REQUIRED;

    static {
        Map<String, Role> m = new HashMap<>();

        // ── Public auth flows ─────────────────────────────────────────
        m.put("auth.signup",   Role.ANONYMOUS);
        m.put("auth.login",    Role.ANONYMOUS);
        m.put("auth.logout",   Role.USER);

        // ── Public catalogue (placeholders until Phase 3) ─────────────
        m.put("asset.list",    Role.ANONYMOUS);
        m.put("asset.detail",  Role.ANONYMOUS);

        REQUIRED = Collections.unmodifiableMap(m);
    }

    private AuthMap() {}

    /**
     * @return the minimum role required for {@code actionKey}, or
     *         {@link Role#USER} when the key is unknown (safe default).
     */
    public static Role required(String actionKey) {
        Role r = REQUIRED.get(actionKey);
        return (r == null) ? Role.USER : r;
    }

    /** @return true when the given action allows anonymous (logged-out) access. */
    public static boolean isAnonymous(String actionKey) {
        return required(actionKey) == Role.ANONYMOUS;
    }
}
