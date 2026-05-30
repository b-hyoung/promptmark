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

## 실행 (예정)

```bash
cp .env.example .env       # 키 채우기
./gradlew run              # http://localhost:8080/promptmark/
./gradlew test
./gradlew integrationTest  # Testcontainers (Docker 필요)
```
