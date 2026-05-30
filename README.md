# promptmark

프롬프트와 마크다운(.md) 파일을 거래하는 JSP 기반 마켓플레이스. AI 챗봇 에이전트가 RAG로 DB를 자율 검색해 자산을 추천한다.

> 디렉토리/빌드 구조는 [b-hyoung/jvision-jspWeb](https://github.com/b-hyoung/jvision-jspWeb) 레퍼런스에서 차용했다.

## 기술 스택

| 영역 | 선택 |
|---|---|
| 빌드 / 실행 | Gradle, 임베디드 Tomcat 9 (`javax.servlet`) |
| 뷰 | JSP + JSTL 1.2 |
| 아키텍처 | MVC2 (FrontController + Action + JSP View) |
| DB | Supabase Postgres (JDBC + HikariCP) |
| 인증 | 자체 세션 + BCrypt + Filter 권한 |
| AI 추천 | 하이브리드 RAG (키워드 + pgvector) + LLM Tool Calling (ReAct) |
| 영상 | YouTube/Vimeo 임베드 URL |
| 로깅 | Logback |
| 테스트 | JUnit 5 + AssertJ + Mockito + Testcontainers(pgvector) |

## 설계 문서

전체 설계는 [docs/superpowers/specs/2026-05-30-promptmark-design.md](docs/superpowers/specs/2026-05-30-promptmark-design.md) 참고.

## 진행 상황

Phase 1 (스캐폴드) → Phase 2 (인증) → Phase 3 (마켓플레이스 코어) → Phase 4 (AI 에이전트)
→ **Phase 5 (Admin · Mypage · 폴리시) 완료** — 가입/로그인, 자산 CRUD·검색·다운로드,
장바구니, 목업 결제, AI 추천 챗봇, 마이페이지, 관리자 신고 처리 & 계정 정지,
i18n(ko/en) 번들, 폴리시된 에러 페이지가 모두 동작한다.

## 빠른 시작 (Phase 1)

### 사전 준비

- JDK 11+
- Docker Desktop (통합 테스트용)
- Supabase 프로젝트 + Postgres 접속 정보

### Supabase 초기 설정 (한 번)

1. Supabase 프로젝트 생성 → Project Settings → Database → Connection String (Transaction Pooler) 복사
2. SQL Editor에서 다음을 한 번 실행:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   CREATE EXTENSION IF NOT EXISTS pg_trgm;
   ```
3. `.env.example`을 `.env`로 복사 후 `DB_URL`, `DB_USER`, `DB_PASSWORD` 채우기

### 실행

```bash
cp .env.example .env       # 그리고 키 채우기
./gradlew run              # http://localhost:8080/promptmark/
./gradlew test             # 단위 테스트
./gradlew integrationTest  # 통합 테스트 (Docker 필요)
```

부팅 시 자동으로:
- DB 스키마 적용 (`V1__init.sql`)
- `ADMIN_EMAIL`/`ADMIN_PWD`로 ADMIN 계정 upsert

### 트레이스 ID

모든 요청에 `X-Trace-Id` 응답 헤더가 붙고, 같은 ID가 로그의 `%X{traceId}`로 찍힙니다.
