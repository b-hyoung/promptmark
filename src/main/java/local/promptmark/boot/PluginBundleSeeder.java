package local.promptmark.boot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seeds the catalogue with curated Claude AI plugins and bundles. Idempotent
 * — uses ON CONFLICT DO NOTHING on a unique-ish marker so re-runs at every
 * boot do not duplicate rows.
 *
 * <p>Each plugin is keyed off its {@code title} (no unique constraint, so we
 * pre-check), and each bundle off its {@code slug} (which is unique).
 */
public final class PluginBundleSeeder {

    private static final Logger log = LoggerFactory.getLogger(PluginBundleSeeder.class);

    private PluginBundleSeeder() {}

    private static final class PluginSeed {
        final String title, category, summary, body;
        final int price;
        PluginSeed(String title, String category, String summary, String body, int price) {
            this.title = title; this.category = category; this.summary = summary; this.body = body; this.price = price;
        }
    }
    private static final class BundleSeed {
        final String slug, name, tagline, story, thumbnail;
        final int price;
        final List<String> pluginTitles;
        BundleSeed(String slug, String name, String tagline, String story,
                   int price, String thumbnail, List<String> pluginTitles) {
            this.slug = slug; this.name = name; this.tagline = tagline; this.story = story;
            this.price = price; this.thumbnail = thumbnail; this.pluginTitles = pluginTitles;
        }
    }

    private static final List<PluginSeed> PLUGINS = List.of(
        new PluginSeed("superpowers", "생산성",
            "슈퍼파워 스킬 마켓플레이스의 기반.",
            "brainstorming / writing-plans / TDD 워크플로를 단일 인터페이스로 제공.", 9900),
        new PluginSeed("copywriting", "글쓰기",
            "랜딩페이지·이메일·CTA를 단번에.",
            "Ogilvy의 카피 원칙을 코드로 옮긴 글쓰기 가이드.", 7900),
        new PluginSeed("stop-slop", "글쓰기",
            "AI 글의 어색한 표현을 자동 정리.",
            "흔한 LLM 어휘 패턴 (`delve`, `treasure trove` 등)을 식별·교체.", 4900),
        new PluginSeed("ogilvy", "광고",
            "David Ogilvy 광고 원칙.",
            "헤드라인·바디카피 평가 기준과 패턴 라이브러리.", 5900),
        new PluginSeed("karpathy-guidelines", "코딩",
            "LLM 코딩 실수 줄이기.",
            "Karpathy가 정리한 LLM 코딩 함정 회피 행동 가이드.", 8900),
        new PluginSeed("frontend-design", "디자인",
            "특색 있는 프론트엔드를 빠르게.",
            "shadcn/Aceternity 스타일의 컴포넌트 생성 가이드.", 12900),
        new PluginSeed("claude-api", "개발",
            "Anthropic SDK 도우미.",
            "Claude API 호출, 캐싱, 스트리밍, 도구 호출 패턴.", 6900),
        new PluginSeed("simplify", "리팩토링",
            "변경 코드를 깔끔하게 정리.",
            "PR 단위로 복잡도·중복·죽은 코드를 자동 식별·정리.", 5900)
    );

    private static final List<BundleSeed> BUNDLES = List.of(
        new BundleSeed("blog-automation", "블로그 자동화 셋트",
            "1시간에 AI 티 안 나는 글 5개.",
            "superpowers의 워크플로 위에 copywriting의 카피력과 stop-slop의 다듬기를 얹어 블로그 초안을 30분 안에 완성합니다.",
            19900, "https://picsum.photos/seed/bdblog/600/400",
            List.of("superpowers", "copywriting", "stop-slop")),
        new BundleSeed("code-quality", "코드 품질 셋트",
            "리뷰어 한 명을 더 두는 효과.",
            "karpathy-guidelines로 흔한 LLM 실수를 막고, simplify로 매 PR을 정리, claude-api로 통합 테스트까지.",
            17900, "https://picsum.photos/seed/bdquality/600/400",
            List.of("karpathy-guidelines", "simplify", "claude-api")),
        new BundleSeed("design-ready", "디자인 마감 셋트",
            "디자이너 없이도 출시 가능한 UI.",
            "frontend-design으로 기반을 잡고 ogilvy와 copywriting으로 카피까지 마무리. 사이드 프로젝트를 3일 안에 출시할 수 있는 조합.",
            22900, "https://picsum.photos/seed/bddesign/600/400",
            List.of("frontend-design", "ogilvy", "copywriting"))
    );

    public static void seed(DataSource ds, String adminEmail) {
        if (adminEmail == null || adminEmail.isEmpty()) {
            log.warn("Skipping plugin/bundle seed — admin email missing");
            return;
        }
        try (Connection c = ds.getConnection()) {
            Long adminId = lookupUserId(c, adminEmail);
            if (adminId == null) {
                log.warn("Skipping plugin/bundle seed — admin not found: {}", adminEmail);
                return;
            }

            Map<String, Long> pluginIds = upsertPlugins(c, adminId);
            int newBundles = upsertBundles(c, adminId, pluginIds);
            log.info("PluginBundleSeeder: plugins ensured={}, new bundles={}", pluginIds.size(), newBundles);
        } catch (SQLException e) {
            throw new RuntimeException("PluginBundleSeeder failed: " + e.getMessage(), e);
        }
    }

    private static Long lookupUserId(Connection c, String email) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private static Map<String, Long> upsertPlugins(Connection c, long sellerId) throws SQLException {
        Map<String, Long> ids = new HashMap<>();
        for (PluginSeed p : PLUGINS) {
            Long existing = findPluginIdByTitle(c, p.title);
            if (existing != null) {
                ids.put(p.title, existing);
                continue;
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO plugins (seller_id, type, title, summary, body, price, status) " +
                    "VALUES (?, 'PROMPT', ?, ?, ?, ?, 'PUBLIC') RETURNING id")) {
                ps.setLong(1, sellerId);
                ps.setString(2, p.title);
                ps.setString(3, p.summary);
                ps.setString(4, p.body);
                ps.setInt(5, p.price);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) ids.put(p.title, rs.getLong(1));
                }
            }
        }
        return ids;
    }

    private static Long findPluginIdByTitle(Connection c, String title) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM plugins WHERE title = ? AND status <> 'DELETED' LIMIT 1")) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private static int upsertBundles(Connection c, long curatorId, Map<String, Long> pluginIds) throws SQLException {
        int created = 0;
        for (BundleSeed b : BUNDLES) {
            Long existing = findBundleIdBySlug(c, b.slug);
            if (existing != null) continue;

            long bundleId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO bundles (curator_id, slug, name, tagline, story, price, thumbnail, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'PUBLIC') RETURNING id")) {
                ps.setLong(1, curatorId);
                ps.setString(2, b.slug);
                ps.setString(3, b.name);
                ps.setString(4, b.tagline);
                ps.setString(5, b.story);
                ps.setInt(6, b.price);
                ps.setString(7, b.thumbnail);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) continue;
                    bundleId = rs.getLong(1);
                }
            }

            int order = 0;
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO bundle_plugin (bundle_id, plugin_id, display_order) VALUES (?, ?, ?)")) {
                for (String title : b.pluginTitles) {
                    Long pid = pluginIds.get(title);
                    if (pid == null) continue;
                    ins.setLong(1, bundleId);
                    ins.setLong(2, pid);
                    ins.setInt(3, order++);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            created++;
        }
        return created;
    }

    private static Long findBundleIdBySlug(Connection c, String slug) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM bundles WHERE slug = ? AND status <> 'DELETED' LIMIT 1")) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }
}
