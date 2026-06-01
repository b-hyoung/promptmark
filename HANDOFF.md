# promptmark — 핸드오프 가이드

다른 환경에서 작업을 이어갈 때 보는 컨텍스트 문서. README는 외부용 요약, **이 문서는 작업자/Claude용 상태 스냅샷**.

- 작성: 2026-05-30 (라운드 2 종료 시점)
- 레포: https://github.com/b-hyoung/promptmark
- 현재 main HEAD: `f557d57 docs: README updates for Phase 5 completion`

---

## 1. 한 줄 요약

JSP + 임베디드 Tomcat 9 + Supabase Postgres로 만든 **프롬프트/MD 자산 마켓플레이스**. AI 챗봇(하이브리드 RAG + LLM tool calling)으로 자산을 추천한다. Phase 1~5 코드 완성, 단위 테스트 194/194 green, **실 DB·LLM·Docker 검증은 미완**.

---

## 2. 빠르게 환경 갖추기

### 2.1 필수 도구

| 도구 | 버전 | 비고 |
|---|---|---|
| JDK | **11+** | 원래 8 호환 목표였지만 `java.net.http.HttpClient` 때문에 11로 올림 (build.gradle). 25까지 검증됨 |
| Gradle | 9.0.0 (wrapper) | 시스템 Gradle 설치 불필요 — `./gradlew` 사용 |
| Docker Desktop | 선택 | 통합 테스트만 필요. 라운드 2 종료 시점 미해결 (§7 참조) |
| Git, gh CLI | 최신 | gh는 push/PR용 |

### 2.2 클론 & 빌드

```bash
git clone https://github.com/b-hyoung/promptmark.git
cd promptmark
./gradlew test           # 194 unit tests 통과해야 정상
./gradlew compileJava compileTestJava   # 빌드만 확인
```

### 2.3 `.env` 만들기 (서버 실제 실행하려면 필수)

`.env.example` 보면 모든 키 있음. **Phase 1 부팅에 최소 필요한 5개:**

```bash
DB_URL=jdbc:postgresql://db.<your-project-ref>.supabase.co:5432/postgres
DB_USER=postgres
DB_PASSWORD=<프로젝트 만들 때 정한 비번>
ADMIN_EMAIL=admin@local
ADMIN_PWD=changeme!
```

> 현재 사용 중인 Supabase 프로젝트 ref: **`sgjcycfjvimapncybfan`** (CoreCBT와 같은 프로젝트 공유, `promptmark` 스키마로 격리). 다른 PC에서 같은 DB 쓰려면 같은 ref/비번 사용.

LLM 기능 켜려면 추가:
```bash
LLM_PROVIDER=openai            # 또는 claude (비어 있으면 rule fallback)
OPENAI_API_KEY=sk-...
EMBEDDING_MODEL=text-embedding-3-small
```

`.env`는 `.gitignore`에 들어있어서 commit 안 됨.

### 2.4 Supabase 사전 셋업 (이미 했으면 skip)

1. https://supabase.com/dashboard/project/sgjcycfjvimapncybfan/database/extensions 에서
   - `vector` 토글 ON
   - `pg_trgm` 토글 ON
2. `promptmark` 스키마는 부팅 시 자동 생성됨 (V1__init.sql)

### 2.5 실행

```bash
./gradlew run          # http://localhost:8080/promptmark/
```

부팅 로그에 "promptmark started" 보이고 X-Trace-Id 헤더 나오면 정상.

---

## 3. 디렉토리 지도

```
promptmark/
├── build.gradle                # Gradle 9. Java 11 target. Tomcat 9 + JSTL 1.2 + Postgres + HikariCP + jBCrypt + flexmark + jackson + pgvector + Mockito + Testcontainers
├── settings.gradle
├── gradle/wrapper/...
├── libs/cos.jar                # 멀티파트 업로드 (cos library)
├── .env.example                # 채워야 할 env 목록
├── .env                        # (gitignore) 본인 환경 키
├── README.md                   # 외부용 요약
├── HANDOFF.md                  # ← 이 문서
├── docs/superpowers/
│   ├── specs/2026-05-30-promptmark-design.md     # 마스터 spec (모든 결정 근거)
│   └── plans/2026-05-30-promptmark-phase1-foundation.md  # Phase 1 plan (Phase 2~5는 plan 없이 spec 보고 직접 구현됨)
└── src/
    ├── main/java/local/promptmark/
    │   ├── DevServer.java                       # 임베디드 Tomcat 진입점
    │   ├── config/{Env, DataSourceProvider}
    │   ├── boot/{SchemaApplier, AdminSeeder}    # 부팅 시 1회 실행
    │   ├── dto/                                 # User, LoginUser, Asset, Tag, CartItem, Order, OrderItem, ReportRow, enums
    │   ├── dao/                                 # UserDao, AssetDao, TagDao, OrderDao, DownloadDao, ReportDao
    │   ├── service/
    │   │   ├── AuthService                      # BCrypt 가입/로그인
    │   │   ├── AssetService                     # 등록 시 EmbeddingClient 호출
    │   │   ├── OrderService, DownloadService, AdminService, RecommendService
    │   │   └── llm/                             # LlmConfig, LlmAgent, LlmClient(OpenAI/Claude), EmbeddingClient, Tools, Fake*
    │   └── web/
    │       ├── FrontController                  # /app/* 단일 servlet, 액션 dispatch
    │       ├── Action, ViewResult, Role, AuthMap
    │       ├── TraceIdFilter, AuthFilter, CsrfFilter, LocaleFilter
    │       ├── AppException + 5 subclasses (NotFound/Forbidden/Unauthorized/Validation/Conflict)
    │       ├── SafeNext, UploadUtil, EmbedHelper
    │       └── action/{auth, asset, cart, order, chat, mypage, admin}/
    ├── main/resources/
    │   ├── logback.xml
    │   ├── db/migration/V1__init.sql            # 스키마 (수동 또는 부팅 시 자동 적용)
    │   └── bundle/messages{.properties, _en.properties}
    ├── main/webapp/
    │   ├── WEB-INF/web.xml
    │   ├── WEB-INF/view/                        # 외부 직접 접근 차단
    │   │   ├── layout/{header,footer,nav}.jsp, csrf.jspf
    │   │   ├── auth/{signup,login}.jsp
    │   │   ├── asset/{list,detail,form}.jsp
    │   │   ├── cart/view.jsp
    │   │   ├── order/{checkout,complete,history}.jsp
    │   │   ├── chat/page.jsp                    # AJAX 챗봇 UI
    │   │   ├── mypage/index.jsp
    │   │   ├── admin/reports.jsp
    │   │   └── error/{400,401,403,404,409,500,forbidden}.jsp
    │   ├── assets/css/app.css
    │   └── index.jsp                            # → /app/asset/list 리다이렉트
    └── test/java/local/promptmark/...           # 194 unit tests (no Docker required for these)
```

---

## 4. 아키텍처 핵심 (코드 만지기 전에 읽기)

### 4.1 요청 흐름

```
Browser
  → TraceIdFilter (/*)         UUID → MDC + X-Trace-Id 응답 헤더
  → LocaleFilter (/*)          Accept-Language → fmt:setLocale
  → AuthFilter (/app/*)        session.LOGIN_USER 확인, AuthMap에서 필요 역할 비교
  → CsrfFilter (/app/*)        POST는 csrf_token 검증 (multipart는 URL 쿼리, JSON은 X-CSRF-Token 헤더)
  → FrontController            /app/{module}/{action}.{METHOD} 파싱 → Action 객체
  → Action.execute()           Service 호출, ViewResult 반환
  → ViewResult.apply()         forward(JSP) | redirect | json | binary
```

### 4.2 Action 인터페이스

```java
public interface Action {
    ViewResult execute(HttpServletRequest req, HttpServletResponse res) throws Exception;
    default boolean producesJson() { return false; }    // JSON 엔드포인트면 override
}
```

- 등록 위치: `FrontController.init()` 안의 `actionRegistry` Map. 키 형식: `module.action.METHOD` (예: `asset.list.GET`, `chat.recommend.POST`).
- 권한 매핑: `AuthMap.populate()`. 매핑 안 된 키는 default `USER`.

### 4.3 Role 계층

`ANONYMOUS < USER < SELLER < ADMIN`. `Role.satisfiedBy(current)` 메서드로 비교.

### 4.4 트랜잭션 경계

DAO는 SQL만, 트랜잭션은 **Service** 레이어. 멀티 SQL이 한 트랜잭션이어야 하는 케이스:
- `AssetService.createAsset` — INSERT asset + tag upsert + asset_tag insert
- `OrderService.placeOrder` — orders insert + 각 order_items insert
- `DownloadService.recordDownload` — downloads insert + assets.download_count++
- `AdminService.resolveReport` — assets.status 변경 + reports.status 변경

패턴:
```java
try (Connection c = ds.getConnection()) {
    c.setAutoCommit(false);
    try {
        dao1.someInsert(..., c);
        dao2.otherUpdate(..., c);
        c.commit();
    } catch (Exception e) { c.rollback(); throw e; }
}
```

### 4.5 search_path

Hikari pool이 **모든 connection**에 `SET search_path TO promptmark, public, extensions`를 자동 실행 (DataSourceProvider.java의 `connectionInitSql`). 그래서 DAO SQL은 **unqualified** 테이블 이름 사용 (`SELECT FROM users`, not `FROM promptmark.users`).

### 4.6 AI 에이전트 (Phase 4)

- `RecommendService.recommend(msg)` 진입점
- LlmConfig.enabled() == false → 즉시 rule fallback (키워드 검색만)
- enabled → `LlmAgent.run()` ReAct 루프, 최대 3턴, 8s 타임아웃, tool calls 누적
- 어떤 LlmException이든 → rule fallback (서비스 멈추지 않음)
- 자산 등록 시 EmbeddingClient.embed(title+summary+body[:500])를 비동기-아닌 동기로 호출 후 `assets.embedding`에 저장. **실패해도 자산 등록은 성공**.
- 하이브리드 검색: AssetDao.searchHybrid() = 키워드 1차 50개 + Java 측 코사인 재랭킹

---

## 5. 자주 쓰는 명령

```bash
./gradlew test                              # 194 unit tests
./gradlew compileJava compileTestJava       # 컴파일 확인
./gradlew run                               # 임베디드 톰캣 시작
./gradlew run -Dport=8081                   # 포트 변경
./gradlew integrationTest                   # 통합 테스트 (Docker 필요. §7 참조)
./gradlew clean                             # build/ 삭제
./gradlew test --tests local.promptmark.service.AuthServiceTest    # 특정 테스트만
git log --oneline | head -20                # 최근 작업 보기
```

---

## 6. spec / plan 위치 + 결정 이력

- **마스터 spec:** `docs/superpowers/specs/2026-05-30-promptmark-design.md` — 모든 아키텍처 결정의 근거. 코드 변경 전에 해당 섹션 확인.
- **Phase 1 plan:** `docs/superpowers/plans/2026-05-30-promptmark-phase1-foundation.md` — TDD 사이클로 task 분해된 가장 상세한 plan.
- **Phase 2~5 plan**: 별도 plan 파일 없음. spec 보고 subagent에게 직접 dispatch했음. 다음에 큰 작업 할 때는 plan 먼저 만드는 게 좋음.

---

## 7. 미해결 / 알려진 한계

### 7.1 Testcontainers 통합 테스트 ❌
- 3개 IT 파일 작성됨 (SchemaApplierIT, AdminSeederIT, DevServerSmokeIT)
- Docker Desktop 29.x + Testcontainers 1.20.1의 docker-java가 API 1.32 사용 → Docker 29는 1.44+ 요구
- 라운드 1에서 시도한 것들 (모두 실패):
  - `DOCKER_HOST` env var (3가지 named pipe)
  - `DOCKER_API_VERSION=1.44` env var
  - `~/.testcontainers.properties`
  - Testcontainers 1.20.4 / 1.21.3 업그레이드
- **추정 해결 경로:** Docker Desktop 설정 → "Expose daemon on tcp://localhost:2375" 켜기 + `DOCKER_HOST=tcp://localhost:2375`. 또는 WSL2 내부 실행.

### 7.2 실 Supabase e2e 검증 ❌
- 사용자가 `./gradlew run` 으로 부팅 + "It works" 페이지 한 번 확인함 (라운드 2 중)
- 그 이후 회원가입/자산 등록/챗봇/구매 등 실제 흐름은 미검증
- `.env`의 `DB_PASSWORD` 채우면 즉시 검증 가능

### 7.3 LLM 실 API 검증 ❌
- OpenAI/Claude 키 없으면 모든 챗봇 요청은 rule fallback (코드는 검증됨)
- 실 OpenAI 호출 / 임베딩 저장 / pgvector 코사인 검색의 end-to-end는 미검증

### 7.4 코드 deviation (라운드 2 보고 요약)

| Phase | Deviation | 근거 |
|---|---|---|
| 1 | Gradle 8.10 → **9.0.0** | Java 25 호환 |
| 1 | `junit-platform-launcher` 추가 | Gradle 9 + JUnit 5.10 |
| 1 | integrationTest 태스크에 source set 바인딩 | plan 버그 |
| 2 | Mockito **5.14.2** + `net.bytebuddy.experimental=true` | Java 25 |
| 2 | Action에 `producesJson()` default method | JSON 액션 구분 |
| 2 | 에러 페이지 400/409 추가 | AppException 코드 커버 |
| 3 | Multipart POST의 CSRF는 **URL 쿼리** | cos가 body 못 읽음 |
| 3 | UserDao.findById 추가 | DetailAction의 seller nickname |
| 4 | `sourceCompatibility 1.8 → 11` | `java.net.http.HttpClient` |
| 4 | AssetService 생성자 2종 | Phase 3 테스트 보존 |
| 4 | searchHybrid는 Java 측 코사인 재랭킹 (pgvector `<=>` 미사용) | spec이 허용 |
| 5 | parseActionKey 3-segment 허용 (`admin.report.resolve`) | spec 라우팅 표가 그렇게 됨 |
| 5 | i18n 번들 UTF-8 (escape 없음) | Java 9+ 기본 |

---

## 8. 다음에 할 일 (라운드 3 후보)

순서 우선순위 추천:

1. **Supabase 실 검증** — `.env` DB_PASSWORD 채우고 `./gradlew run` → 회원가입/로그인/자산 등록(프롬프트+md)/검색/구매/다운로드/마이페이지/관리자/챗봇(rule fallback) 한 바퀴.
2. **Final code review** — `superpowers:requesting-code-review` 스킬로 5 phase 코드 전체 일관성·deviation 종합 리뷰.
3. **LLM 실 검증** — OpenAI 키 넣고 챗봇 tool calling + 임베딩 저장/검색 e2e.
4. **Docker IT 해결** — Docker Desktop TCP 노출 또는 WSL2 안에서 실행.
5. **CI 셋업** — GitHub Actions에서 `./gradlew test` 자동 실행. integrationTest는 docker-compose service 띄워서.
6. **YAGNI 제외 항목 중 필요한 거 추가** — 좋아요/평점/댓글/알림/카테고리 등은 spec §0.3에서 의도적으로 제외. 필요하면 brainstorming부터 다시.

---

## 9. 다른 PC에서 시작할 때 체크리스트

- [ ] `git clone https://github.com/b-hyoung/promptmark.git`
- [ ] JDK 11+ 설치 (`java -version` 확인)
- [ ] `./gradlew test` — 194/194 green이면 환경 OK
- [ ] `.env.example` → `.env` 복사 후 DB_PASSWORD 채움
- [ ] (선택) Docker Desktop 실행 + TCP 노출 켜기 → `./gradlew integrationTest`
- [ ] (선택) OpenAI 키 `.env`에 추가
- [ ] `./gradlew run` → http://localhost:8080/promptmark/
- [ ] 이 문서(`HANDOFF.md`) 한 번 다시 훑기

---

## 10. Claude/AI 작업자에게

- 작업 전 반드시 `docs/superpowers/specs/2026-05-30-promptmark-design.md` 해당 섹션 읽기
- 코드 패턴 따라가기: 기존 DAO/Service/Action 파일을 reference로
- 새 기능 추가 시 `superpowers:brainstorming` → `superpowers:writing-plans` 순서. plan 없이 바로 구현은 Phase 2~5에서 사용한 단축법인데, quality 위해 큰 작업엔 plan 권장
- TDD 사이클 유지 (red → green → commit). 모든 service/util은 단위 테스트 작성
- 트랜잭션 경계는 Service 레이어 (DAO에 Connection 파라미터 받는 메서드 패턴)
- DAO SQL은 unqualified 테이블 이름 (search_path가 해결)
- Phase 1~2 코드는 후속 phase의 기반이라 함부로 수정 금지. 수정 필요하면 deviation으로 명시
