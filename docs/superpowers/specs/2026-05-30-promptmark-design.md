# promptmark — JSP 기반 프롬프트/MD 자산 마켓플레이스 설계서

- 작성일: 2026-05-30
- 참고 레퍼런스 구조: https://github.com/b-hyoung/jvision-jspWeb
- 작업 디렉토리: `C:\Users\ACE\Desktop\bobs_project\jvisin_jsp_web`

## 0. 개요

레퍼런스 레포(`jvision-jspWeb`)의 디렉토리/빌드 구조(Gradle + 임베디드 Tomcat 9 + JSP/JSTL)를 차용해, 다른 도메인의 새 웹 애플리케이션 **promptmark** 를 만든다.

promptmark는 **프롬프트와 마크다운(.md) 파일을 거래하는 마켓플레이스**이며, 핵심 차별점은 **AI 챗봇 에이전트가 RAG 기반으로 DB를 자율 검색해 자산을 추천**하는 기능이다.

### 0.1 도메인 / 사용자

- 자산(상품) 종류: ① **프롬프트** (텍스트 본문), ② **마크다운 파일**(첨부)
- 회원 역할: `USER`(구매) / `SELLER`(판매·구매) / `ADMIN`(운영)
- 결제: **목업** (실 PG 연동 없음, 결제완료 흐름만 구현)
- 무료 자산: 다운로드 가능 + 다운로드 카운트 집계
- 챗봇 추천: 사용자가 자연어로 질문 → 에이전트가 자산 검색 → 추천

### 0.2 기술 스택

| 영역 | 선택 |
|---|---|
| 빌드 / 실행 | Gradle, 임베디드 Tomcat 9 (`javax.servlet`), Java 8 호환 |
| 뷰 | JSP + JSTL 1.2 |
| 아키텍처 | **MVC2** (FrontController + Action + JSP View) |
| DB | **Supabase Postgres** (JDBC 직접 연결 + HikariCP) |
| 인증 | 자체 세션 + BCrypt + Filter 기반 권한 |
| AI 추천 | **하이브리드 RAG** (키워드 + pgvector 임베딩) + LLM **Tool Calling** (ReAct 최대 3턴) |
| 파일 업로드 | cos.jar (레퍼런스 유지, multipart) |
| 영상 | YouTube/Vimeo **임베드 URL만** (직접 업로드 안 함) |
| 로깅 | Logback |
| 테스트 | JUnit 5 + AssertJ + Mockito + Testcontainers(pgvector) |

### 0.3 의도적 제외 항목 (YAGNI)

댓글/평점, 좋아요, 환불, 정산, 카테고리 트리, 알림, 이메일 인증, 비밀번호 찾기, 소셜 로그인, 2FA, JWT, Spring/Hibernate, 멀티턴 챗봇, 스트리밍 응답, 자동 태그 추출, CI/CD 파이프라인, Flyway, Docker Compose.

---

## 1. 전체 아키텍처 & 디렉토리

### 1.1 요청 흐름

```
Browser
  → AuthFilter (/app/*)         세션/권한 사전 체크
  → CsrfFilter                  POST 토큰 검증
  → FrontController             /app/{module}/{action} 파싱
  → Action.execute()            서비스 호출, ViewResult 반환
  → Service                     트랜잭션 경계, 비즈니스 규칙
  → DAO                         SQL only
  → ViewResult                  forward(JSP) | redirect | json
```

### 1.2 디렉토리 트리

```
jvisin_jsp_web/
├── build.gradle, settings.gradle, gradlew(.bat)
├── libs/                         # cos.jar (멀티파트 업로드)
├── .env.example                  # SUPABASE_DB_URL, USER, PWD, LLM_PROVIDER, OPENAI_API_KEY 등
├── .gitignore                    # .env, build/, logs/, .idea/, .vscode/launch.json
└── src/main/
    ├── java/local/promptmark/
    │   ├── DevServer.java                  # 임베디드 Tomcat 진입점
    │   ├── config/
    │   │   ├── Env.java                    # .env + System.getenv() 로딩
    │   │   └── DataSourceProvider.java     # HikariCP 풀
    │   ├── web/
    │   │   ├── FrontController.java        # /app/* 매핑, action 분기
    │   │   ├── AuthFilter.java             # 세션/권한 체크
    │   │   ├── CsrfFilter.java             # CSRF 토큰 검증
    │   │   ├── Action.java                 # 인터페이스
    │   │   ├── ViewResult.java             # forward | redirect | json 팩토리
    │   │   ├── AuthMap.java                # action key → 필요 권한 매핑
    │   │   └── action/
    │   │       ├── auth/      (SignupForm, Signup, LoginForm, Login, Logout)
    │   │       ├── asset/     (List, Detail, CreateForm, Create, EditForm, Edit, Delete, Download, Report)
    │   │       ├── cart/      (Add, View, Remove)
    │   │       ├── order/     (CheckoutForm, PlaceOrder, Complete, History)
    │   │       ├── chat/      (Page, Recommend)
    │   │       ├── mypage/    (Index)
    │   │       └── admin/     (Reports, ResolveReport, BanUser)
    │   ├── dao/
    │   │   ├── UserDao.java, AssetDao.java, TagDao.java,
    │   │   ├── OrderDao.java, DownloadDao.java, ReportDao.java
    │   ├── dto/
    │   │   ├── User.java, LoginUser.java, Asset.java, Tag.java,
    │   │   ├── Order.java, OrderItem.java, Report.java
    │   ├── service/
    │   │   ├── AuthService.java            # 가입/로그인/BCrypt
    │   │   ├── AssetService.java           # 등록/수정/삭제/검색, 임베딩 트리거
    │   │   ├── OrderService.java           # 결제 목업 트랜잭션
    │   │   ├── DownloadService.java        # 권한 체크 + 카운트
    │   │   ├── RecommendService.java       # 키워드 추출 + 후보 검색 + 점수
    │   │   ├── LlmAgent.java               # ReAct 루프, 도구 dispatch
    │   │   ├── LlmClient.java              # OpenAI / Claude HTTP
    │   │   ├── EmbeddingClient.java        # text-embedding-3-small
    │   │   └── Tools.java                  # search_assets, get_asset_detail 구현
    │   └── util/
    │       ├── PasswordHasher.java, Pagination.java, MarkdownRenderer.java,
    │       └── RateLimiter.java
    └── resources/
        ├── logback.xml
        ├── db/migration/V1__init.sql       # 스키마 (수동 실행)
        └── bundle/messages.properties      # i18n
    └── webapp/
        ├── META-INF/, WEB-INF/web.xml
        ├── WEB-INF/view/                   # 외부 직접 접근 불가
        │   ├── layout/(header.jsp, footer.jsp, nav.jsp, csrf.jspf)
        │   ├── auth/, asset/, cart/, order/, chat/, mypage/, admin/, error/
        ├── assets/                          # 정적 css/js/img
        └── index.jsp                        # → /app/asset/list 로 redirect
```

### 1.3 컨벤션

- 모든 JSP는 `WEB-INF/view/` 아래 (직접 URL 접근 차단)
- URL 규칙: `/app/{module}/{action}` (예: `/app/asset/list`, `/app/chat/recommend`)
- 정적 파일만 webapp 루트의 `assets/`
- DAO는 SQL만 다룸. 트랜잭션 경계는 Service 레이어
- 응답 타입: HTML(forward) / Redirect / JSON(챗봇·AJAX)

---

## 2. DB 스키마 (Supabase Postgres)

### 2.1 확장

```sql
CREATE EXTENSION IF NOT EXISTS vector;     -- Supabase dashboard 또는 SQL editor에서 1회 실행
CREATE EXTENSION IF NOT EXISTS pg_trgm;    -- 한국어 LIKE 가속용
```

### 2.2 테이블

```sql
-- 1) users
CREATE TABLE users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(120) UNIQUE NOT NULL,
  password_hash VARCHAR(120) NOT NULL,         -- BCrypt cost 12
  nickname      VARCHAR(40)  NOT NULL,
  role          VARCHAR(10)  NOT NULL CHECK (role IN ('USER','SELLER','ADMIN')),
  status        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('ACTIVE','BANNED')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2) assets
CREATE TABLE assets (
  id             BIGSERIAL PRIMARY KEY,
  seller_id      BIGINT NOT NULL REFERENCES users(id),
  type           VARCHAR(10) NOT NULL CHECK (type IN ('PROMPT','MD')),
  title          VARCHAR(120) NOT NULL,
  summary        VARCHAR(300) NOT NULL,
  body           TEXT,                          -- type=PROMPT 일 때 본문
  file_key       VARCHAR(200),                  -- type=MD 일 때 파일 경로
  demo_url       VARCHAR(500),                  -- 데모 사이트/프로그램 URL
  video_url      VARCHAR(500),                  -- YouTube/Vimeo 임베드 URL
  price          INTEGER NOT NULL DEFAULT 0 CHECK (price >= 0),
  status         VARCHAR(10) NOT NULL DEFAULT 'PUBLIC'
                 CHECK (status IN ('PUBLIC','HIDDEN','DELETED')),
  view_count     INTEGER NOT NULL DEFAULT 0,
  download_count INTEGER NOT NULL DEFAULT 0,
  embedding      vector(1536),                  -- NULL 허용 (LLM 키 없을 때 안전)
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_assets_status_created ON assets(status, created_at DESC);
CREATE INDEX idx_assets_seller         ON assets(seller_id);
CREATE INDEX idx_assets_title_trgm     ON assets USING gin (title gin_trgm_ops);
CREATE INDEX idx_assets_embedding
       ON assets USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 3) tags
CREATE TABLE tags (
  id   BIGSERIAL PRIMARY KEY,
  name VARCHAR(40) UNIQUE NOT NULL
);

-- 4) asset_tags (N:M)
CREATE TABLE asset_tags (
  asset_id BIGINT REFERENCES assets(id) ON DELETE CASCADE,
  tag_id   BIGINT REFERENCES tags(id)   ON DELETE CASCADE,
  PRIMARY KEY (asset_id, tag_id)
);

-- 5) orders
CREATE TABLE orders (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL REFERENCES users(id),
  total_amount INTEGER NOT NULL CHECK (total_amount >= 0),
  status       VARCHAR(10) NOT NULL CHECK (status IN ('PAID','CANCELED')),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 6) order_items
CREATE TABLE order_items (
  id         BIGSERIAL PRIMARY KEY,
  order_id   BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  asset_id   BIGINT NOT NULL REFERENCES assets(id),
  price_paid INTEGER NOT NULL CHECK (price_paid >= 0),
  UNIQUE (order_id, asset_id)
);

-- 7) downloads
CREATE TABLE downloads (
  id            BIGSERIAL PRIMARY KEY,
  user_id       BIGINT NOT NULL REFERENCES users(id),
  asset_id      BIGINT NOT NULL REFERENCES assets(id),
  downloaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_downloads_user ON downloads(user_id, downloaded_at DESC);

-- 8) reports
CREATE TABLE reports (
  id          BIGSERIAL PRIMARY KEY,
  asset_id    BIGINT NOT NULL REFERENCES assets(id),
  reporter_id BIGINT NOT NULL REFERENCES users(id),
  reason      VARCHAR(300) NOT NULL,
  status      VARCHAR(10) NOT NULL DEFAULT 'OPEN'
              CHECK (status IN ('OPEN','RESOLVED','REJECTED')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 2.3 비즈니스 규칙 (Service가 강제)

- **구매 가능:** `assets.status='PUBLIC'` AND 자기 자산 아님 AND 이미 구매 안 함
- **다운로드 가능:** `price=0` 이거나 본인 `order_items` 존재
- 무료 자산도 `downloads`에 row 남겨 카운트 집계
- `download_count`/`view_count`는 비정규화 → 트랜잭션으로 동시 갱신
- 관리자 신고 처리: `assets.status` → `HIDDEN`/`DELETED`
- 정지 회원: `users.status='BANNED'` → 다음 로그인 차단, 진행 중 세션 강제 로그아웃

### 2.4 시드

- ADMIN 1명: `.env`의 `ADMIN_EMAIL`/`ADMIN_PWD`로 부팅 시 upsert (`AdminSeeder`)
- 샘플 태그 10개: 코딩 / 디자인 / 마케팅 / AI / 번역 / 요약 / 이메일 / 문서 / 학습 / 기획
- 샘플 자산 5개 (개발 편의용. 운영 빌드에서는 스킵)

---

## 3. 라우팅 & Action 맵

권한: `🟢` 공개 / `🔵` 로그인 필요 / `🟡` SELLER 이상 / `🔴` ADMIN

| Method | URL | Action 클래스 | 권한 | 응답 |
|---|---|---|---|---|
| GET | `/` | (web.xml) | 🟢 | 302 → `/app/asset/list` |
| **Auth** | | | | |
| GET | `/app/auth/signup` | `auth.SignupFormAction` | 🟢 | view `auth/signup.jsp` |
| POST | `/app/auth/signup` | `auth.SignupAction` | 🟢 | 302 → `/app/auth/login` |
| GET | `/app/auth/login` | `auth.LoginFormAction` | 🟢 | view `auth/login.jsp` |
| POST | `/app/auth/login` | `auth.LoginAction` | 🟢 | 302 → `?next` 또는 `/` |
| POST | `/app/auth/logout` | `auth.LogoutAction` | 🔵 | 302 → `/` |
| **Asset** | | | | |
| GET | `/app/asset/list` | `asset.ListAction` | 🟢 | view `asset/list.jsp` (`q`,`type`,`tag`,`sort`,`page`) |
| GET | `/app/asset/detail` | `asset.DetailAction` | 🟢 | view `asset/detail.jsp` (조회수 +1) |
| GET | `/app/asset/new` | `asset.CreateFormAction` | 🟡 | view `asset/form.jsp` |
| POST | `/app/asset/new` | `asset.CreateAction` | 🟡 | 302 → detail (multipart) |
| GET | `/app/asset/edit` | `asset.EditFormAction` | 🟡 | view `asset/form.jsp` |
| POST | `/app/asset/edit` | `asset.EditAction` | 🟡 | 302 → detail |
| POST | `/app/asset/delete` | `asset.DeleteAction` | 🟡 | 302 → list (soft delete) |
| GET | `/app/asset/download` | `asset.DownloadAction` | 🔵 | file stream + 카운트 +1 |
| POST | `/app/asset/report` | `asset.ReportAction` | 🔵 | 302 → detail |
| **Cart** (세션 기반) | | | | |
| POST | `/app/cart/add` | `cart.AddAction` | 🔵 | 302 → `/app/cart/view` |
| GET | `/app/cart/view` | `cart.ViewAction` | 🔵 | view `cart/view.jsp` |
| POST | `/app/cart/remove` | `cart.RemoveAction` | 🔵 | 302 → `/app/cart/view` |
| **Order** | | | | |
| GET | `/app/order/checkout` | `order.CheckoutFormAction` | 🔵 | view `order/checkout.jsp` |
| POST | `/app/order/checkout` | `order.PlaceOrderAction` | 🔵 | 302 → complete (트랜잭션) |
| GET | `/app/order/complete` | `order.CompleteAction` | 🔵 | view `order/complete.jsp` |
| GET | `/app/order/history` | `order.HistoryAction` | 🔵 | view `order/history.jsp` |
| **Chat** | | | | |
| GET | `/app/chat` | `chat.PageAction` | 🟢 | view `chat/page.jsp` |
| POST | `/app/chat/recommend` | `chat.RecommendAction` | 🟢 | **JSON** |
| **Mypage** | | | | |
| GET | `/app/mypage` | `mypage.IndexAction` | 🔵 | view `mypage/index.jsp` |
| **Admin** | | | | |
| GET | `/app/admin/reports` | `admin.ReportsAction` | 🔴 | view `admin/reports.jsp` |
| POST | `/app/admin/report/resolve` | `admin.ResolveReportAction` | 🔴 | 302 → reports |
| POST | `/app/admin/user/ban` | `admin.BanUserAction` | 🔴 | 302 → reports |

### 3.1 Action 인터페이스

```java
public interface Action {
    ViewResult execute(HttpServletRequest req, HttpServletResponse res) throws Exception;
}
```

- `ViewResult.forward("asset/list")`, `.redirect("/app/asset/list")`, `.json(Object body)` 팩토리.
- `FrontController`가 `/app/{module}/{action}` 파싱 → `Map<String, Action>` dispatch.
- 액션 메타에 `produces=JSON` 표시 → 에러 시 JSON 응답.

---

## 4. 챗봇 / RAG 에이전트

### 4.1 핵심 결정

- **LLM이 메인, Tool calling으로 자율 검색** (ReAct, 최대 3턴)
- **하이브리드 RAG:** 키워드 1차 후보 (≤50) + 벡터 코사인 재랭킹 → top N
- **단일 턴 추천.** 멀티턴 컨텍스트는 YAGNI. 클라이언트 `sessionStorage`에 히스토리만 보관.
- **LLM 키 없으면 graceful degrade:** rule fallback (키워드 매칭만으로 정형 응답)

### 4.2 자산 등록 시 임베딩 생성

```
SELLER 등록
  └→ AssetService.create()
       1) INSERT assets (embedding NULL)
       2) if LLM 활성:
            EmbeddingClient.embed(title + " " + summary + " " + body_preview(<=500자))
            UPDATE assets SET embedding = ? WHERE id = ?
          실패해도 자산 등록은 성공 (NULL로 둠)
```

- 모델: `text-embedding-3-small` (1536-d, $0.02/1M tokens)
- 타임아웃 5초, 재시도 없음
- 자산 수정 시 title/summary/body 변경 감지 → 재임베딩

### 4.3 챗봇 요청 흐름

```
POST /app/chat/recommend {message}
  └→ RecommendAction → LlmAgent.run(message)
        루프 (최대 3턴):
          1) LLM 호출 (system + history + tools=[search_assets, get_asset_detail])
          2) tool_call 있으면:
                Tools.dispatch(name, args) → 결과
                history.append(tool_result)
                continue
          3) 최종 답변 → break
        반환: {answer, items[], source, trace[]}
```

### 4.4 도구 (Tools.java)

```json
[
  {
    "name": "search_assets",
    "description": "프롬프트/MD 자산 검색. 키워드+벡터 하이브리드.",
    "parameters": {
      "type":"object",
      "properties": {
        "query":     {"type":"string"},
        "type":      {"type":"string", "enum":["PROMPT","MD"]},
        "max_price": {"type":"integer"},
        "limit":     {"type":"integer","default":10,"maximum":20}
      },
      "required":["query"]
    }
  },
  {
    "name": "get_asset_detail",
    "description": "특정 자산의 본문 미리보기·태그·데모 URL 조회.",
    "parameters": {
      "type":"object",
      "properties": {"id":{"type":"integer"}},
      "required":["id"]
    }
  }
]
```

#### `search_assets` 구현

1. `EmbeddingClient.embed(query)` → `vec` (LLM 활성 시)
2. 1차 후보 (≤50):
   ```sql
   SELECT a.id, a.title, a.summary, a.type, a.price, a.download_count, a.embedding,
          array_agg(t.name) AS tags
   FROM assets a
   LEFT JOIN asset_tags at ON at.asset_id = a.id
   LEFT JOIN tags t        ON t.id = at.tag_id
   WHERE a.status = 'PUBLIC'
     AND (a.title ILIKE ANY(?)
          OR a.summary ILIKE ANY(?)
          OR t.name = ANY(?))
     AND (? IS NULL OR a.type = ?)
     AND (? IS NULL OR a.price <= ?)
   GROUP BY a.id
   LIMIT 50
   ```
3. 벡터 재랭킹 (embedding NOT NULL 후보만): `ORDER BY 1 - (embedding <=> vec)`
4. embedding NULL 후보는 키워드 점수 그대로 뒤에 append
5. top `limit` 반환

#### `get_asset_detail` 구현

- `SELECT … FROM assets WHERE id=? AND status='PUBLIC'`
- 태그 join, body는 500자 컷, embedding은 응답에서 제외

### 4.5 LLM 호출 정책

| 항목 | 값 |
|---|---|
| 모델 | OpenAI `gpt-4o-mini` 또는 Claude `claude-haiku-4-5-20251001` (env `LLM_PROVIDER` 토글) |
| System | "당신은 프롬프트/MD 자산 추천 에이전트입니다. 사용자 요청을 이해하고 search_assets·get_asset_detail 도구로 정보를 수집한 뒤 추천 답변을 생성하세요. 후보가 없으면 솔직히 없다고 답하세요." |
| 최대 턴 | 3 |
| 전체 타임아웃 | 8초 |
| 응답 형식 | 자유 텍스트 (마크다운 허용). 추천 자산은 도구 결과에서 누적 |
| 실패 | 키 없음/타임아웃/HTTP 오류 → rule fallback |

### 4.6 응답 JSON

```json
{
  "answer": "자기소개서 첨삭에 쓸만한 자산을 찾았어요. 첫 번째는…",
  "source": "AGENT" | "RULE_FALLBACK",
  "items": [
    {"id":42,"type":"PROMPT","title":"…","summary":"…","price":1500,"score":0.87,"tags":["취업","문서"]}
  ],
  "trace": [
    {"tool":"search_assets","args":{"query":"자기소개서 첨삭"},"hits":7},
    {"tool":"get_asset_detail","args":{"id":42}}
  ]
}
```

### 4.7 챗봇 화면 (`chat/page.jsp`)

- 좌측: 메시지 영역. 사용자/봇 말풍선. 봇 답변은 markdown → DOMPurify 통과 후 렌더
- 답변 아래 `items[]` 카드 그리드 (제목, 요약, 가격, 타입 뱃지)
- 카드 클릭 → `/app/asset/detail?id=…`
- "에이전트 사고 보기" 토글 → `trace[]` 펼침
- "초기화" 버튼 → `sessionStorage` 비움

### 4.8 보안 / 운영

- 메시지 길이 1000자 컷
- IP 기준 메모리 rate-limit: 분당 30회 (`RateLimiter.java`)
- LLM 응답은 항상 escape 후 렌더 (XSS)
- 도구는 `status='PUBLIC'`만 노출

---

## 5. 인증 · 세션 · 필터

### 5.1 비밀번호 / 가입

- 해시: `org.mindrot:jbcrypt:0.4`, cost 12
- 가입 입력: email, password(8~64), password_confirm, nickname(2~20), role(`USER`|`SELLER`)
- 검증: email 정규식, `UserDao.existsByEmail`, nickname 중복
- `ADMIN`은 폼에 노출 안 함. `AdminSeeder`가 부팅 시 upsert.

### 5.2 로그인 / 세션

```java
User u = userDao.findByEmail(email);
if (u == null || u.status == BANNED || !BCrypt.checkpw(pwd, u.passwordHash))
    throw new AppException(401, "이메일/비밀번호 불일치");
req.changeSessionId();                 // 세션 고정 공격 방지
req.getSession().setAttribute("LOGIN_USER",
    new LoginUser(u.id, u.email, u.nickname, u.role));
return ViewResult.redirect(safeNext(req));
```

- **세션 객체:** `LoginUser(id, email, nickname, role)` — 비번/해시 미포함
- **세션 만료:** `web.xml` `<session-timeout>60</session-timeout>` (분), "로그인 유지" 시 120분
- **쿠키:** `HttpOnly; SameSite=Lax`
- **로그아웃:** `session.invalidate()` → `/`

### 5.3 AuthFilter (`/app/*`)

```
요청 → AuthFilter
  ├─ 정적/익명 허용 액션 → pass
  ├─ /app/{module}/{action} 파싱
  ├─ AuthMap에서 필요 권한 조회
  ├─ 세션 LOGIN_USER 확인
  │     ├─ 없음 + 익명 허용 → pass
  │     ├─ 없음 + 로그인 필요 → 302 /app/auth/login?next=<현재>
  │     ├─ 있음 + 권한 충분 → pass
  │     ├─ 있음 + 권한 부족 → 403 forward error/forbidden.jsp
  │     └─ status=BANNED → 강제 logout
  └─ chain.doFilter
```

### 5.4 CSRF

- 모든 POST 폼에 hidden `csrf_token` 삽입
- `CsrfFilter` (AuthFilter 뒤):
  - GET: 세션에 `CSRF_TOKEN` 없으면 생성 (16바이트 random base64)
  - POST: `req.getParameter("csrf_token")` vs 세션 토큰 비교 → 불일치 시 403
  - JSON API (`/app/chat/recommend`): `X-CSRF-Token` 헤더로 같은 토큰 요구
- 폼 헬퍼: `/WEB-INF/view/layout/csrf.jspf`

### 5.5 JSP 권한 표현

```jsp
<c:if test="${not empty sessionScope.LOGIN_USER}">
  <c:if test="${sessionScope.LOGIN_USER.role == 'SELLER'
                or sessionScope.LOGIN_USER.role == 'ADMIN'}">
    <a href="/app/asset/new">자산 등록</a>
  </c:if>
  <c:if test="${sessionScope.LOGIN_USER.role == 'ADMIN'}">
    <a href="/app/admin/reports">신고 관리</a>
  </c:if>
  <a href="/app/mypage">마이페이지</a>
</c:if>
```

- 객체 단위 권한(내 자산 수정/삭제)은 Service에서 한 번 더 검사

### 5.6 `safeNext`

- `?next=` 파라미터는 **상대 경로 `/app/...`만 허용**, 외부 도메인·`//`·`javascript:` 차단

### 5.7 로그인 실패 제한

- 메모리 캐시 (IP + email, 5분 슬라이딩, 5회 초과 시 15분 잠금)
- 학습 프로젝트 단순화. DB 저장 안 함

---

## 6. 에러 처리 · 테스트 · 개발 환경

### 6.1 예외 계층

```
RuntimeException
 └─ AppException(int code, String userMessage)
     ├─ NotFoundException        (404)
     ├─ ForbiddenException       (403)
     ├─ UnauthorizedException    (401)
     ├─ ValidationException      (400, fieldErrors Map)
     └─ ConflictException        (409)
```

- `FrontController`가 `AppException` 잡아서 상태 코드 set + `errorMessage` request attr → `error/{code}.jsp` forward
- 그 외 `Exception` → 500, `logger.error(traceId, e)`, `error/500.jsp` (traceId만 노출)
- 모든 요청에 `X-Trace-Id` 헤더 + MDC 등록
- JSON 엔드포인트는 에러도 JSON: `{"error":{"code":400,"message":"…","traceId":"…"}}`

### 6.2 web.xml error-page

```xml
<error-page><error-code>404</error-code><location>/WEB-INF/view/error/404.jsp</location></error-page>
<error-page><error-code>403</error-code><location>/WEB-INF/view/error/403.jsp</location></error-page>
<error-page><error-code>500</error-code><location>/WEB-INF/view/error/500.jsp</location></error-page>
<error-page><exception-type>java.lang.Throwable</exception-type>
            <location>/WEB-INF/view/error/500.jsp</location></error-page>
```

### 6.3 로깅 (Logback)

- 콘솔 appender + 파일 appender (`logs/app.%d{yyyy-MM-dd}.log`, 30일 롤링)
- 포맷: `%d{HH:mm:ss.SSS} [%thread] %X{traceId} %-5level %logger{36} - %msg%n`
- 레벨: `local.promptmark=DEBUG`, `org.apache=WARN`, root=INFO
- LLM 요청/응답은 길이만 로깅, payload는 DEBUG에서만

### 6.4 테스트

| 종류 | 위치 | 대상 | 의존 |
|---|---|---|---|
| Unit | `service/*Test.java` | Service 로직, 키워드 추출, 점수 계산 | DAO mock |
| DAO 통합 | `dao/*DaoIT.java` | SQL/매핑/트랜잭션 | Testcontainers `pgvector/pgvector:pg16` |
| 액션/Filter | `web/*Test.java` | URL → Action → ViewResult, 권한 분기 | DAO mock + `MockHttpServletRequest` |

- LLM/Embedding: 인터페이스 추상화 → 테스트는 `FakeLlmClient`, `FakeEmbeddingClient` 주입. 외부 호출 절대 안 함.
- Testcontainers: 세션당 컨테이너 1개, 각 테스트 전 `TRUNCATE ... RESTART IDENTITY CASCADE`
- 마이그레이션: `V1__init.sql`을 `Statement.execute`로 적용 (Flyway 안 씀)
- 커버리지 목표: Service 80%, DAO 핵심 쿼리 100% 흐름 통과. UI는 수동 점검.
- 실행: `./gradlew test`, `./gradlew integrationTest`

### 6.5 빌드 의존성

```groovy
dependencies {
    // 기존 (Tomcat embed-core/jasper, JSTL, cos.jar) 유지

    implementation 'org.postgresql:postgresql:42.7.4'
    implementation 'com.zaxxer:HikariCP:5.1.0'
    implementation 'org.mindrot:jbcrypt:0.4'
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.2'
    implementation 'com.vladsch.flexmark:flexmark-all:0.64.8'
    implementation 'org.slf4j:slf4j-api:2.0.13'
    implementation 'ch.qos.logback:logback-classic:1.5.7'

    testImplementation platform('org.junit:junit-bom:5.10.3')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.assertj:assertj-core:3.26.3'
    testImplementation 'org.mockito:mockito-core:5.13.0'
    testImplementation 'org.testcontainers:postgresql:1.20.1'
    testImplementation 'org.testcontainers:junit-jupiter:1.20.1'
    testImplementation 'org.springframework:spring-test:6.1.13'
}
```

### 6.6 환경변수 (`.env.example`)

```
# Supabase Postgres (Transaction Pooler)
DB_URL=jdbc:postgresql://aws-0-ap-northeast-2.pooler.supabase.com:5432/postgres
DB_USER=postgres.your-project-ref
DB_PASSWORD=...

# LLM (비어 있으면 rule fallback)
LLM_PROVIDER=openai            # openai | claude | (empty)
OPENAI_API_KEY=sk-...
CLAUDE_API_KEY=
EMBEDDING_MODEL=text-embedding-3-small

# Admin 초기 시드
ADMIN_EMAIL=admin@local
ADMIN_PWD=changeme!

# 서버
PORT=8080
CONTEXT_PATH=/promptmark
```

- `.env`는 git에 안 올림. `Env.java`가 시작 시 파일 + `System.getenv()` 둘 다 로드.
- LLM 키 없으면 자동 rule fallback.

### 6.7 Supabase 초기 셋업 (수동)

1. 프로젝트 생성 → Project Settings → Database → Connection String (Transaction Pooler URI) 복사 → `.env`
2. SQL Editor에서 `CREATE EXTENSION vector; CREATE EXTENSION pg_trgm;` 실행
3. `resources/db/migration/V1__init.sql` 전체 붙여넣고 실행
4. RLS는 비활성 유지 (서비스 키 없이 직접 JDBC 접속)

### 6.8 실행

```bash
./gradlew run
./gradlew run -Dport=8081 -DcontextPath=/promptmark
./gradlew test
./gradlew integrationTest          # Docker 필요
```

- 브라우저: `http://localhost:8080/promptmark/`

---

## 7. 구현 단계 (개략)

세부 단계는 별도 implementation plan에서 다룸.

1. 빌드 스캐폴드 (Gradle, DevServer, web.xml, 디렉토리)
2. Env, DataSourceProvider, AppException, Logback
3. DB 스키마 작성·수동 적용, AdminSeeder
4. Auth (가입/로그인/로그아웃/세션/CSRF/AuthFilter)
5. Asset CRUD + 목록/상세 + 업로드(cos.jar)
6. Cart + Order (목업 결제 트랜잭션)
7. Download (권한 + 카운트)
8. EmbeddingClient + LlmClient + Tools + LlmAgent
9. Chat 페이지 + RecommendAction (JSON)
10. Mypage + Admin (Reports / Ban)
11. 에러/i18n/스타일링 마무리
12. 테스트 보강
