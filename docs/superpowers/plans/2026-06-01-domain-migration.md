# Phase B 구현 계획 — Sub-phase 분해

> 한 단계씩 컴파일 + 테스트 통과 확인하면서 진행한다. sed 일괄 변환 중심.

## Sub-phase B1: V2 마이그 SQL
- Create `src/main/resources/db/migration/V2__rename_to_plugin_add_bundle.sql`
- 부팅 시 자동 실행 (SchemaApplier가 V*__*.sql 순서대로)
- Commit: `feat(db): V2 migration — rename assets→plugins, add bundles + bundle_plugin`

## Sub-phase B2: 일괄 리네임 (Asset → Plugin)
1. Java 파일 sed:
   - `s/Asset/Plugin/g` (단, AssetType→PluginKind, AssetStatus→PluginStatus는 별도 처리)
   - `s/asset/plugin/g` (단어 경계 주의 — package, import, var 모두 변환됨)
2. 디렉토리 mv:
   - `web/action/asset/` → `web/action/plugin/`
   - `webapp/WEB-INF/view/asset/` → `webapp/WEB-INF/view/plugin/`
3. i18n properties: `asset.` → `plugin.` (양쪽 ko/en)
4. compileJava + test → green 확인
- Commit: `refactor: rename Asset domain to Plugin (sed bulk replace)`

## Sub-phase B3: Bundle 신규 도메인
1. `dto/Bundle.java`, `dto/BundleStatus.java`
2. `dao/BundleDao.java` (CRUD + `findByIdWithPlugins` + `addPluginToBundle`)
3. `service/BundleService.java` (트랜잭션 + 임베딩)
4. Actions in `web/action/bundle/`: List, Detail, FormNew/Edit, Create, Edit, Delete
5. Views in `WEB-INF/view/bundle/`: list, detail, form
6. FrontController register + AuthMap (관리자만 new/edit/delete)
7. nav.jsp에 "셋트" 링크 추가
8. compile + test
- Commit: `feat(bundle): new Bundle domain with N:N to plugin`

## Sub-phase B4: Order / Cart polymorphic
1. `OrderItem` dto에 `targetType, targetId` 필드 추가
2. `OrderDao.placeOrder` SQL에서 새 컬럼 사용
3. `CartItem`도 polymorphic (세션 객체이므로 클래스만 변경)
4. CartView에서 PLUGIN/BUNDLE 표시 분기
5. test 업데이트
- Commit: `feat(order): polymorphic target (PLUGIN | BUNDLE)`

## Sub-phase B5: 시드 + 컴파일/테스트 통과
1. `boot/PluginBundleSeeder.java` — 부팅 시 1회: 플러그인 8개 + 셋트 3개 (admin 만든 후 매핑)
2. test 추가/조정
- Commit: `feat(seed): plugin + bundle initial data`

## 검증
- `./gradlew test` — 모든 테스트 통과
- `./gradlew compileJava` — clean
- (선택) `.env` 채우고 `./gradlew run` — V2 마이그 자동 실행 확인, /app/plugin/list, /app/bundle/list 둘러보기

## 이후 Phase
- C: AI 챗봇 prompt 튜닝 + search_bundles tool 추가
- D: dark futurism 디자인 입히기
