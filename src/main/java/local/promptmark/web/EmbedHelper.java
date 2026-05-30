package local.promptmark.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turn a user-supplied video URL into the embed src for an iframe. Supports
 * YouTube ({@code youtube.com/watch?v=}, {@code youtu.be/}, short {@code shorts/})
 * and Vimeo ({@code vimeo.com/<id>}). Returns null for anything else so the JSP
 * can skip rendering the iframe.
 */
public final class EmbedHelper {

    private static final Pattern YT_V_PARAM    = Pattern.compile("[?&]v=([A-Za-z0-9_-]{6,})");
    private static final Pattern YT_SHORT_HOST = Pattern.compile("youtu\\.be/([A-Za-z0-9_-]{6,})");
    private static final Pattern YT_EMBED      = Pattern.compile("youtube\\.com/embed/([A-Za-z0-9_-]{6,})");
    private static final Pattern YT_SHORTS     = Pattern.compile("youtube\\.com/shorts/([A-Za-z0-9_-]{6,})");
    private static final Pattern VIMEO         = Pattern.compile("vimeo\\.com/(\\d{4,})");

    private EmbedHelper() {}

    /**
     * @param url raw URL pasted by the seller
     * @return canonical embed src ({@code https://...}) or null when unrecognised
     */
    public static String toIframeSrc(String url) {
        if (url == null) return null;
        String u = url.trim();
        if (u.isEmpty()) return null;

        Matcher m;

        m = YT_EMBED.matcher(u);
        if (m.find()) return "https://www.youtube.com/embed/" + m.group(1);

        m = YT_V_PARAM.matcher(u);
        if (m.find() && u.contains("youtube.com")) {
            return "https://www.youtube.com/embed/" + m.group(1);
        }

        m = YT_SHORT_HOST.matcher(u);
        if (m.find()) return "https://www.youtube.com/embed/" + m.group(1);

        m = YT_SHORTS.matcher(u);
        if (m.find()) return "https://www.youtube.com/embed/" + m.group(1);

        m = VIMEO.matcher(u);
        if (m.find()) return "https://player.vimeo.com/video/" + m.group(1);

        return null;
    }
}
