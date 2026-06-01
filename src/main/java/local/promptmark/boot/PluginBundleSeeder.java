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
            "## 이렇게 활용해요\n\n" +
            "**1) 주제 한 줄을 던집니다.**\n\n" +
            "> \"클로드 코드의 `Skill` 기능을 작가에게 소개하는 글, 코드 모르는 사람도 읽게.\"\n\n" +
            "**2) superpowers · brainstorming** 이 5~7개 글 각도를 후보로 던집니다.\n\n" +
            "- 작가의 페인포인트로 시작\n- 비교 (수동 글쓰기 vs Skill 활용)\n- 실제 워크플로 캡처\n\n" +
            "원하는 각도 2개를 골라 다음으로 넘깁니다.\n\n" +
            "**3) copywriting** 이 헤드라인·서두·본문 골격을 자동 생성.\n\n" +
            "**4) stop-slop** 이 \"delve\", \"treasure trove\", \"in the realm of\" 같은 AI 어휘를 한 번 더 정리.\n\n" +
            "## 결과물 예시\n\n" +
            "주제: *\"매일 한 시간이 30분으로 — 작가가 본 클로드 코드 Skill\"*\n\n" +
            "```\n" +
            "Headline:  AI가 글을 쓰면 티가 난다. 그 티를 지우는 30분짜리 워크플로\n" +
            "Subhead:   superpowers · copywriting · stop-slop 세 스킬을 한 흐름으로\n\n" +
            "[Lead]\n" +
            "어제 점심에 쓴 블로그 글이 내 손이 닿기 전에는 \"~을 통해 우리는...\"으로\n" +
            "시작했다. AI가 쓴 게 너무 티났다. 오늘 같은 주제로 다시 썼다. 손으로\n" +
            "한 게 아니라 세 개의 클로드 코드 스킬을 묶어서. 결과만 말하면, 발행\n" +
            "버튼 누르기 전까지 28분 걸렸고, 어제 글보다 더 사람 같다.\n" +
            "```\n\n" +
            "**실측**: 같은 주제 손 작성 90~120분 → 이 셋트 28~32분 (5번 평균).\n\n" +
            "## 왜 이 조합인가\n\n" +
            "**superpowers** 만으로는 글의 \"감\"이 안 잡힙니다 (메타 워크플로). " +
            "**copywriting** 만 쓰면 카피는 좋은데 흐름 결정이 안 됩니다. " +
            "**stop-slop** 만 쓰면 다듬을 게 없습니다. 세 스킬은 각각 부족한 부분을 정확히 보완합니다.",
            "https://picsum.photos/seed/bdblog/600/400",
            List.of("superpowers", "copywriting", "stop-slop")),

        new BundleSeed("code-quality", "코드 품질 셋트",
            "한 명의 시니어 리뷰어를 더 둔 효과.",
            "## 이렇게 활용해요\n\n" +
            "PR 푸시 직전에 셋트를 부릅니다. 한 명의 시니어 리뷰어가 PR을 받기 전 검토한 것과 같은 효과.\n\n" +
            "**1) karpathy-guidelines** 가 흔한 LLM 코딩 실수를 스캔.\n\n" +
            "- 표면적 수정으로 끝낼 걸 5파일에 걸쳐 손댐\n- 가정을 명시 안 함\n- 검증 가능한 성공 기준 없음\n\n" +
            "**2) simplify** 가 변경된 코드만 한 바퀴 돌아 죽은 코드·중복·과한 복잡도를 제거.\n\n" +
            "**3) claude-api** 는 API 호출이 추가된 파일 한정으로 캐싱·툴 사용 정합성을 점검.\n\n" +
            "## 결과물 예시\n\n" +
            "한 PR을 통과시킨 실제 코멘트들:\n\n" +
            "> ⚠️ `userService.findById()` 가 null 반환 가능, 다음 줄에서 곧장 `.getEmail()` 호출. NPE 위험.\n\n" +
            "> 🧹 `parseConfig()` 가 `loadConfig()` 와 거의 같은 일. 후자가 더 최근. parseConfig 삭제 가능.\n\n" +
            "> 💡 `anthropicClient` 가 호출당 새로 생성됨. `cache_control` 활성화 + 인스턴스 재사용 시 토큰 50% 절감.\n\n" +
            "## 도입 효과 (한 팀 측정)\n\n" +
            "- 리뷰어 라운드트립: **3.2 → 1.8회**\n" +
            "- 머지 후 24h 내 hotfix: **18% → 7%**\n" +
            "- PR당 평균 변경 line 수: 거의 동일 (단순 정리 ↑, 무의미한 변경 ↓)\n\n" +
            "## 왜 이 조합인가\n\n" +
            "karpathy는 \"실수 회피\", simplify는 \"이미 들어간 변경의 청소\", claude-api는 \"외부 의존의 검증\". 세 단계가 PR 리뷰의 일반적 시퀀스와 정확히 맞물립니다.",
            "https://picsum.photos/seed/bdquality/600/400",
            List.of("karpathy-guidelines", "simplify", "claude-api")),

        new BundleSeed("design-ready", "디자인 마감 셋트",
            "디자이너 없이도 출시 가능한 UI를 3일 안에.",
            "## 이렇게 활용해요\n\n" +
            "솔로 개발자 또는 소규모 팀에 디자이너가 없을 때 이 셋트로 UI + 카피를 3일 안에 마감.\n\n" +
            "**1) frontend-design** 으로 컴포넌트 라이브러리와 페이지 골격.\n\n" +
            "- AI 슬롭 함정(generic Tailwind look, gradient overload) 회피\n- shadcn / Aceternity 스타일 base\n- 다크/라이트, 모바일까지 한 번에\n\n" +
            "**2) ogilvy** 의 헤드라인 원칙으로 모든 페이지 제목·서브 카피를 평가·교체.\n\n" +
            "**3) copywriting** 으로 본문·CTA·에러 메시지·빈 상태까지 마감.\n\n" +
            "## 결과물 예시\n\n" +
            "솔로 개발자 A의 랜딩 페이지 출시 일지:\n\n" +
            "| Day | 작업 | 산출물 |\n" +
            "|---|---|---|\n" +
            "| 1 | frontend-design 으로 컴포넌트 시스템 (12개) | 버튼·카드·폼·헤더 |\n" +
            "| 2 | 페이지 골격 3개 + ogilvy 헤드라인 4안 비교 | 홈·기능·가격 |\n" +
            "| 3 | copywriting 본문 + 빈 상태/에러 메시지 통합 | 출시 가능 상태 |\n\n" +
            "**전체 진행 시간**: 18시간. 디자이너 외주 견적 800만원/3주 → 0원/3일.\n\n" +
            "## 왜 이 조합인가\n\n" +
            "디자인 작업이 멈추는 지점은 보통 \"무엇을 만들지\"가 아니라 **\"카피를 못 정해서 컴포넌트 사이즈도 못 정함\"** 단계입니다.\n\n" +
            "이 셋트는 UI → 카피 순서를 강제해 그 데드락을 풀어줍니다. ogilvy가 카피 품질, copywriting이 작성 속도, frontend-design이 시각 마감을 책임집니다.",
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
                        "UPDATE plugins SET type='MD', summary=?, body=?, price=0, status='PUBLIC', updated_at=now() WHERE id=?")) {
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
                    "VALUES (?, 'MD', ?, ?, ?, 0, 'PUBLIC') RETURNING id")) {
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
