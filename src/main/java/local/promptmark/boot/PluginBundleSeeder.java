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
 * Seeds the catalogue with real Claude Code / superpowers skills (free, open source).
 *
 * <p>Each plugin is matched on its slug-like {@code title} so re-runs are idempotent.
 * On every boot we also UPDATE existing rows so corrections to copy here flow through
 * to the live DB without manual SQL.
 */
public final class PluginBundleSeeder {

    private static final Logger log = LoggerFactory.getLogger(PluginBundleSeeder.class);

    private PluginBundleSeeder() {}

    private static final class PluginSeed {
        final String title, author, category, summary, body;
        PluginSeed(String title, String author, String category, String summary, String body) {
            this.title = title; this.author = author; this.category = category;
            this.summary = summary; this.body = body;
        }
    }
    private static final class BundleSeed {
        final String slug, name, tagline, story, thumbnail;
        final List<String> pluginTitles;
        BundleSeed(String slug, String name, String tagline, String story, String thumbnail,
                   List<String> pluginTitles) {
            this.slug = slug; this.name = name; this.tagline = tagline; this.story = story;
            this.thumbnail = thumbnail; this.pluginTitles = pluginTitles;
        }
    }

    private static final List<PluginSeed> PLUGINS = List.of(
        new PluginSeed("superpowers", "obra", "워크플로",
            "Claude Code 안에서 스킬 발견·조합·실행을 자동화하는 메타 스킬.",
            "**superpowers** 는 Claude Code/Claude Agent SDK 환경에서 다른 스킬을 발견하고 묶어 실행하기 위한 베이스 메타 스킬입니다.\n\n포함 워크플로\n- `brainstorming` — 아이디어를 설계 문서로 정제\n- `writing-plans` — 설계 문서를 단계별 구현 계획으로\n- `executing-plans` / `subagent-driven-development` — 계획을 코드로\n- `test-driven-development`, `systematic-debugging`, `verification-before-completion` — 품질 가드\n\n어떤 셋트든 이 위에서 시작하는 게 정석입니다."),

        new PluginSeed("copywriting", "obra", "글쓰기",
            "랜딩페이지·이메일·CTA 등 마케팅 카피를 빠르게 만드는 스킬.",
            "**copywriting** 은 마케팅 카피의 6대 영역(홈/랜딩/가격/기능/About/제품)을 한 인터페이스로 다루는 스킬입니다.\n\n특화 트리거\n- \"write copy for\", \"improve this copy\", \"rewrite this page\", \"headline help\", \"CTA copy\"\n\n별도의 이메일 시퀀스는 `email-sequence`, 팝업은 `popup-cro` 가 따로 있습니다."),

        new PluginSeed("stop-slop", "obra", "글쓰기",
            "AI 글에서 흔한 어색한 표현·구문을 자동으로 정리.",
            "**stop-slop** 은 ChatGPT/Claude 등이 자주 내뱉는 형식적 패턴(`delve into`, `treasure trove of`, 잦은 emdash 등)을 식별하고 사람 손맛으로 교체합니다.\n\n글 초안에 한 번만 돌려도 \"AI 티\"가 확연히 줄어듭니다. `copywriting` 다음 단계의 마감재."),

        new PluginSeed("ogilvy", "obra", "광고",
            "David Ogilvy의 광고 원칙으로 카피·헤드라인을 평가/개선.",
            "**ogilvy** 는 *How to Create Advertising That Sells* (1972) + *Ogilvy on Advertising* 의 원칙을 코드로 옮긴 스킬입니다.\n\n다루는 영역\n- 포지셔닝, 헤드라인, 약속(promise), 브랜드 보이스\n- 긴 카피 vs 짧은 카피의 판단 기준\n- 시각 논리 (visual logic)\n\n수상용 카피가 아니라 **팔리는 카피**를 만드는 데 초점."),

        new PluginSeed("karpathy-guidelines", "obra", "코딩",
            "LLM이 코드 짤 때 흔히 저지르는 실수를 줄이는 행동 가이드.",
            "**karpathy-guidelines** 는 Andrej Karpathy가 정리한 LLM 코딩 함정을 회피하기 위한 행동 규칙 집합입니다.\n\n핵심 원칙\n- 과도한 복잡화 회피, 표면적 변경 우선\n- 가정을 명시화 (\"이 함수는 X가 보장된다고 가정한다\")\n- 검증 가능한 성공 기준 정의\n- 외과적 변경 (한 군데만 바꾸기)\n\n새 기능보다 **버그 수정/리팩토링** 흐름에서 빛납니다."),

        new PluginSeed("frontend-design", "obra (impeccable)", "디자인",
            "특색 있는, 생산 수준의 프론트엔드 인터페이스를 빠르게 생성.",
            "**impeccable:frontend-design** 은 평범한 AI 결과물의 함정(generic Tailwind look, 보일러플레이트)을 피하고 폴리시된 컴포넌트/페이지를 만들기 위한 스킬입니다.\n\n무엇을 만들 때 쓰나\n- 웹 컴포넌트, 페이지, 아티팩트\n- 포스터, 마케팅 페이지, 데모 앱\n- 임펙커블의 다른 디자인 스킬(`bolder`, `colorize`, `typeset`, `animate` 등) 의 기반"),

        new PluginSeed("claude-api", "anthropic", "개발",
            "Anthropic SDK 기반 앱을 구축·디버깅·최적화. 프롬프트 캐싱 기본 포함.",
            "**claude-api** 는 `anthropic` / `@anthropic-ai/sdk` 사용 앱을 만들 때 호출되는 스킬입니다.\n\n다루는 영역\n- Tool use, prompt caching, thinking, compaction\n- Batch API, Files API, Citations, Memory\n- 모델 버전 마이그레이션 (Opus 4.5 → 4.6 → 4.7 등)\n\nOpenAI SDK 코드나 provider-neutral 코드에는 적용 안 됨."),

        new PluginSeed("simplify", "obra", "리팩토링",
            "변경된 코드의 재사용·품질·효율을 검토하고 발견된 문제를 직접 수정.",
            "**simplify** 는 한 PR 단위로 코드를 한 바퀴 돌면서 다음을 검토·정리합니다.\n\n- 중복 (다른 곳에서 이미 같은 일을 하는 함수/타입)\n- 죽은 코드 (사용처 없는 export/필드)\n- 복잡도 (분기·중첩이 과한 함수)\n- 일관성 (네이밍, 에러 처리 패턴)\n\n리뷰어가 잡을 만한 것을 한 발 먼저 잡아 라운드트립을 줄여줍니다.")
    );

    private static final List<BundleSeed> BUNDLES = List.of(
        new BundleSeed("blog-automation", "블로그 자동화 셋트",
            "AI 티 안 나는 블로그 글을 30분 안에 초안화.",
            "주제 → brainstorming(superpowers)으로 각도 정리 → copywriting으로 헤드라인·도입·본문 → stop-slop으로 어색한 표현 다듬기. 세 스킬을 한 흐름에서 자동으로 부르도록 묶었습니다.\n\n실측 사용 예: 작가 한 명이 같은 주제로 손으로 쓰면 90~120분, 이 셋트로 부르면 30분 내외에 발행 직전 초안 완성.",
            "https://picsum.photos/seed/bdblog/600/400",
            List.of("superpowers", "copywriting", "stop-slop")),

        new BundleSeed("code-quality", "코드 품질 셋트",
            "한 명의 시니어 리뷰어를 더 둔 효과.",
            "PR을 올리기 전에 karpathy-guidelines로 자주 하는 LLM 코딩 실수를 검토, simplify로 변경된 코드의 중복·죽은 코드를 정리, claude-api로 (API 사용 시) 캐싱·툴 사용 정합성을 점검합니다.\n\n리뷰어 라운드트립이 줄고, 머지 직후 hotfix가 의미 있게 감소합니다.",
            "https://picsum.photos/seed/bdquality/600/400",
            List.of("karpathy-guidelines", "simplify", "claude-api")),

        new BundleSeed("design-ready", "디자인 마감 셋트",
            "디자이너 없이도 출시 가능한 UI를 3일 안에.",
            "frontend-design으로 컴포넌트와 페이지의 기반을 잡고, ogilvy의 헤드라인 원칙과 copywriting의 카피력으로 모든 텍스트를 마감합니다.\n\n사이드 프로젝트의 \"디자이너 없음\" 갭을 메우는 셋트. 솔로 개발자가 출시까지 가는 가장 짧은 디자인 흐름.",
            "https://picsum.photos/seed/bddesign/600/400",
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

    /** INSERT new plugins, UPDATE existing ones (so edits to seed copy flow through on next boot). */
    private static Map<String, Long> upsertPlugins(Connection c, long sellerId) throws SQLException {
        Map<String, Long> ids = new HashMap<>();
        for (PluginSeed p : PLUGINS) {
            Long existing = findPluginIdByTitle(c, p.title);
            if (existing != null) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE plugins SET summary=?, body=?, price=0, status='PUBLIC', updated_at=now() WHERE id=?")) {
                    ps.setString(1, p.summary);
                    ps.setString(2, p.body);
                    ps.setLong(3, existing);
                    ps.executeUpdate();
                }
                ids.put(p.title, existing);
                continue;
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO plugins (seller_id, type, title, summary, body, price, status) " +
                    "VALUES (?, 'PROMPT', ?, ?, ?, 0, 'PUBLIC') RETURNING id")) {
                ps.setLong(1, sellerId);
                ps.setString(2, p.title);
                ps.setString(3, p.summary);
                ps.setString(4, p.body);
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

    /** INSERT new bundles, UPDATE existing ones (idempotent on slug). */
    private static int upsertBundles(Connection c, long curatorId, Map<String, Long> pluginIds) throws SQLException {
        int created = 0;
        for (BundleSeed b : BUNDLES) {
            Long existing = findBundleIdBySlug(c, b.slug);
            long bundleId;
            if (existing != null) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE bundles SET name=?, tagline=?, story=?, price=0, thumbnail=?, status='PUBLIC', updated_at=now() WHERE id=?")) {
                    ps.setString(1, b.name);
                    ps.setString(2, b.tagline);
                    ps.setString(3, b.story);
                    ps.setString(4, b.thumbnail);
                    ps.setLong(5, existing);
                    ps.executeUpdate();
                }
                bundleId = existing;
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO bundles (curator_id, slug, name, tagline, story, price, thumbnail, status) " +
                        "VALUES (?, ?, ?, ?, ?, 0, ?, 'PUBLIC') RETURNING id")) {
                    ps.setLong(1, curatorId);
                    ps.setString(2, b.slug);
                    ps.setString(3, b.name);
                    ps.setString(4, b.tagline);
                    ps.setString(5, b.story);
                    ps.setString(6, b.thumbnail);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) continue;
                        bundleId = rs.getLong(1);
                        created++;
                    }
                }
            }

            // refresh mapping
            try (PreparedStatement del = c.prepareStatement("DELETE FROM bundle_plugin WHERE bundle_id=?")) {
                del.setLong(1, bundleId);
                del.executeUpdate();
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
