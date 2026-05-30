package local.promptmark.web;

import javax.servlet.http.HttpServletRequest;

/**
 * Sanitises {@code ?next=...} parameters to prevent open-redirect attacks.
 * Only paths starting with {@code /app/} are accepted; everything else
 * (schemes, protocol-relative {@code //evil}, {@code javascript:}, null) maps
 * to the supplied fallback.
 */
public final class SafeNext {

    private SafeNext() {}

    /**
     * @param req       current request (read {@code next} parameter from)
     * @param fallback  URL to return when {@code next} is missing/unsafe
     * @return a safe redirect target
     */
    public static String resolve(HttpServletRequest req, String fallback) {
        String next = (req == null) ? null : req.getParameter("next");
        return resolveValue(next, fallback);
    }

    /** Package-visible for unit testing without HttpServletRequest. */
    static String resolveValue(String next, String fallback) {
        if (next == null) return fallback;
        String trimmed = next.trim();
        if (trimmed.isEmpty()) return fallback;

        // Reject protocol-relative URLs ("//evil.com/x")
        if (trimmed.startsWith("//")) return fallback;

        // Reject absolute URLs and javascript: / data: pseudo-schemes.
        // Any colon before the first slash means a scheme is present.
        int colon = trimmed.indexOf(':');
        int slash = trimmed.indexOf('/');
        if (colon >= 0 && (slash < 0 || colon < slash)) return fallback;

        // Only allow within-app paths.
        if (!trimmed.startsWith("/app/")) return fallback;

        return trimmed;
    }
}
