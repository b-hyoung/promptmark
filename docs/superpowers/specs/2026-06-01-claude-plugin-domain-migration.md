# Claude Plugin Marketplace — promptmark 도메인 마이그 설계

- 작성일: 2026-06-01
- 베이스: promptmark Phase 5 완성 코드 (commit `1e27d9a` 기준)
- 위치: `/Users/bobs/Desktop/bobs_project/claude-plugin-marketplace` (promptmark 복사본, origin 제거)

---

## 1. 목표

promptmark의 MVC2 + Filter + RAG + LLM tool-calling 아키텍처를 그대로 살리고, **도메인만** `asset (프롬프트/MD)` → `plugin (개별) + bundle (셋트 N:N)` 으로 교체.

기능은 유지: 회원/CRUD/장바구니/주문/다운로드/AI 챗봇 추천/마이페이지/관리자/i18n.

---

## 2. 도메인 매핑

| promptmark | 신규 |
|---|---|
| `asset` (PROMPT/MD 단품) | `plugin` (개별 Claude AI 플러그인) |
| (없음) | **`bundle` (큐레이션 셋트, plugin과 N:N)** |
| `asset_tag` ↔ tag | `plugin_tag` ↔ tag (그대로), `bundle_tag` 신규 |
| `order_items.asset_id` | `order_items.target_type ('PLUGIN'|'BUNDLE') + target_id` (polymorphic) |
| `downloads.asset_id` | `downloads.target_type + target_id` |
| `reports.asset_id` | `reports.target_type + target_id` |

`plugin` 컬럼: `asset` 그대로 유지 (id, seller_id, type, title, summary, body, file_key, demo_url, video_url, price, status, view_count, download_count, embedding, timestamps). `type`은 enum (`PROMPT/MD`) → 무의미하므로 nullable 둠 (안 쓰면 빈 값 가능). 또는 제거.

`bundle` 신규 컬럼:
- `id BIGSERIAL PK`
- `curator_id BIGINT REFERENCES users(id)` (관리자가 큐레이션)
- `slug VARCHAR(40) UNIQUE` (URL용)
- `name VARCHAR(100)`
- `tagline VARCHAR(200)`
- `story TEXT` (Markdown)
- `price INT >= 0` (단품 합계와 무관하게 설정)
- `thumbnail VARCHAR(255)`
- `status VARCHAR(10)` (`PUBLIC|HIDDEN|DELETED`)
- `view_count INT DEFAULT 0`
- `embedding vector(1536)` (시맨틱 검색용)
- `created_at, updated_at`

`bundle_plugin` (N:N):
- `bundle_id BIGINT REFERENCES bundles(id) ON DELETE CASCADE`
- `plugin_id BIGINT REFERENCES plugins(id) ON DELETE CASCADE`
- `display_order INT DEFAULT 0`
- PRIMARY KEY (bundle_id, plugin_id)

---

## 3. DB 마이그 (`V2__rename_to_plugin_add_bundle.sql`)

```sql
SET search_path TO promptmark, public;

-- 1. assets → plugins
ALTER TABLE assets RENAME TO plugins;
ALTER TABLE asset_tags RENAME TO plugin_tags;
ALTER TABLE plugin_tags RENAME COLUMN asset_id TO plugin_id;

-- 인덱스 이름 변경 (Postgres는 RENAME)
ALTER INDEX IF EXISTS idx_assets_status_created RENAME TO idx_plugins_status_created;
ALTER INDEX IF EXISTS idx_assets_seller RENAME TO idx_plugins_seller;
ALTER INDEX IF EXISTS idx_assets_title_trgm RENAME TO idx_plugins_title_trgm;
ALTER INDEX IF EXISTS idx_assets_embedding RENAME TO idx_plugins_embedding;

-- 2. bundles 신규
CREATE TABLE IF NOT EXISTS bundles (
    id            BIGSERIAL PRIMARY KEY,
    curator_id    BIGINT REFERENCES users(id),
    slug          VARCHAR(40) UNIQUE NOT NULL,
    name          VARCHAR(100) NOT NULL,
    tagline       VARCHAR(200),
    story         TEXT,
    price         INT NOT NULL CHECK (price >= 0),
    thumbnail     VARCHAR(255),
    status        VARCHAR(10) NOT NULL DEFAULT 'PUBLIC',
    view_count    INT NOT NULL DEFAULT 0,
    embedding     vector(1536),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_bundles_status_created ON bundles(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bundles_name_trgm ON bundles USING gin(name gin_trgm_ops);

CREATE TABLE IF NOT EXISTS bundle_plugin (
    bundle_id     BIGINT NOT NULL REFERENCES bundles(id) ON DELETE CASCADE,
    plugin_id     BIGINT NOT NULL REFERENCES plugins(id) ON DELETE CASCADE,
    display_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (bundle_id, plugin_id)
);
CREATE INDEX IF NOT EXISTS idx_bundle_plugin_plugin ON bundle_plugin(plugin_id);

-- 3. order_items / downloads / reports polymorphic
ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS target_type VARCHAR(10) NOT NULL DEFAULT 'PLUGIN',
    ADD COLUMN IF NOT EXISTS target_id   BIGINT;

UPDATE order_items SET target_id = asset_id WHERE target_id IS NULL;
ALTER TABLE order_items ALTER COLUMN target_id SET NOT NULL;
-- 기존 asset_id FK 유지 (NULL 허용으로 변경, 새 데이터는 target_*만 채움)
ALTER TABLE order_items ALTER COLUMN asset_id DROP NOT NULL;

ALTER TABLE downloads
    ADD COLUMN IF NOT EXISTS target_type VARCHAR(10) NOT NULL DEFAULT 'PLUGIN',
    ADD COLUMN IF NOT EXISTS target_id   BIGINT;
UPDATE downloads SET target_id = asset_id WHERE target_id IS NULL;
ALTER TABLE downloads ALTER COLUMN target_id SET NOT NULL;
ALTER TABLE downloads ALTER COLUMN asset_id DROP NOT NULL;

ALTER TABLE reports
    ADD COLUMN IF NOT EXISTS target_type VARCHAR(10) NOT NULL DEFAULT 'PLUGIN',
    ADD COLUMN IF NOT EXISTS target_id   BIGINT;
UPDATE reports SET target_id = asset_id WHERE target_id IS NULL;
ALTER TABLE reports ALTER COLUMN target_id SET NOT NULL;
ALTER TABLE reports ALTER COLUMN asset_id DROP NOT NULL;
```

부팅 시 `SchemaApplier`가 자동 적용. 기존 데이터는 그대로 살아남고 plugin으로 인식됨.

---

## 4. 코드 변경 매트릭스

### 일괄 리네임 (sed로 처리 가능한 1:1 매핑)

| 기존 | 신규 |
|---|---|
| `Asset` (대소문자 고정 단어) | `Plugin` |
| `asset` (소문자) | `plugin` |
| `assets` (테이블/복수) | `plugins` |
| `AssetType` | `PluginKind` (단어 충돌 회피: Plugin enum 들도 있으므로) |
| `AssetStatus` | `PluginStatus` |
| `AssetDao` | `PluginDao` |
| `AssetService` | `PluginService` |
| `web/action/asset/` (디렉토리) | `web/action/plugin/` |
| `WEB-INF/view/asset/` | `WEB-INF/view/plugin/` |
| URL `/app/asset/*` | `/app/plugin/*` |
| AuthMap key `asset.*` | `plugin.*` |
| i18n `asset.*` | `plugin.*` |
| LlmAgent system prompt: "프롬프트/MD 자산 추천" | "Claude AI 플러그인/셋트 추천" |

### 신규 추가

| 파일 | 책임 |
|---|---|
| `dto/Bundle.java` | DTO |
| `dto/BundleStatus.java` | enum |
| `dao/BundleDao.java` | CRUD + N:N (bundle_plugin) |
| `service/BundleService.java` | 트랜잭션 + 임베딩 |
| `web/action/bundle/{List,Detail,Form,Create,Edit,Delete}Action.java` | CRUD |
| `WEB-INF/view/bundle/{list,detail,form}.jsp` | View |
| `service/llm/Tools.java`에 `search_bundles`, `get_bundle_detail` 추가 |

### Polymorphic 변경 (Order/Cart/Download/Report)

- `OrderItem.targetType, targetId` 필드 추가, `assetId` 호환 유지
- `CartItem`도 동일 (세션 객체)
- `DownloadDao`, `ReportDao` 같은 패턴

---

## 5. AI 챗봇 (Phase C, 별도 phase)

- System prompt: "프롬프트/MD 자산 추천" → "Claude AI 플러그인과 큐레이션 셋트를 추천하는 에이전트. 사용자가 만들고 싶은 결과물을 들으면 셋트를 우선 제안하고, 셋트가 안 맞으면 개별 플러그인을 조합 제안."
- Tools: `search_plugins`, `get_plugin_detail`, **`search_bundles`**, **`get_bundle_detail`** (4개)

---

## 6. Phase D — 디자인

claude-plugin-store의 `design-tokens.css`, `style.css`, `effects.js`를 가져와 `webapp/assets/` 에 배치. JSP 헤더에서 로드. dark futurism + Three.js + GSAP.

---

## 7. 비스코프

- 결제 실제 게이트웨이 (목업 그대로)
- Docker IT 환경 이슈 (HANDOFF §7.1)
- LLM 실 API e2e 검증 (Supabase 검증과 함께 별도)
- `assets` 테이블 컬럼 자체 rename (`type` 등) — 그대로 두고 `plugins`로 alias만
