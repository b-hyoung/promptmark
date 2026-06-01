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

        // ── Public catalogue ──────────────────────────────────────────
        m.put("plugin.list",     Role.ANONYMOUS);
        m.put("plugin.detail",   Role.ANONYMOUS);

        // ── Public chat / AI agent ────────────────────────────────────
        m.put("chat.page",      Role.ANONYMOUS);
        m.put("chat.recommend", Role.ANONYMOUS);

        // ── Seller CRUD ───────────────────────────────────────────────
        m.put("plugin.new",      Role.SELLER);
        m.put("plugin.edit",     Role.SELLER);
        m.put("plugin.delete",   Role.SELLER);

        // ── Logged-in interactions ────────────────────────────────────
        m.put("plugin.download", Role.USER);
        m.put("plugin.report",   Role.USER);
        m.put("cart.add",       Role.USER);
        m.put("cart.view",      Role.USER);
        m.put("cart.remove",    Role.USER);
        m.put("order.checkout", Role.USER);
        m.put("order.complete", Role.USER);
        m.put("order.history",  Role.USER);

        // ── Mypage (Phase 5) ──────────────────────────────────────────
        m.put("mypage.index",   Role.USER);

        // ── Admin (Phase 5) ───────────────────────────────────────────
        m.put("admin.reports",        Role.ADMIN);
        m.put("admin.report.resolve", Role.ADMIN);
        m.put("admin.user.ban",       Role.ADMIN);

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
