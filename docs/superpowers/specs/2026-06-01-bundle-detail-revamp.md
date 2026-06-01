# Bundle Detail Page Revamp

- 작성일: 2026-06-01
- 컨셉 영감: 쿠팡(정보 밀도 + 가격 비교 + 배지) × jchsangi-lesson(큰 타입 + Step 1/2/3 스토리텔링)
- 톤: dark futurism (이전 결정 유지)
- 스코프: **셋트 상세 페이지 한 곳만** (`/app/bundle/detail?id=N`)

## 1. 목적
"이 셋트로 무엇을 만드는가"의 서사가 약했음. plugin list가 그냥 텍스트 리스트로 나열돼 있어서 "조합의 가치"가 전달 안 됨. 정보 밀도 + 단계별 스토리텔링 + 가격 절약 시각화로 재구성.

## 2. 레이아웃

### 2.1 HERO (다크 그라디언트, 70vh)
- 카테고리/난이도 칩 (정적: 카테고리는 셋트별 라벨, 난이도는 일단 "⚡ 즉시 사용 가능" 같은 고정 텍스트 — 후속에서 컬럼 추가)
- H1 제목 + 태그라인 (큰 타입, 다크 + 그라디언트 액센트)
- 메타: 조회수, 마지막 업데이트
- **가격 카드** (헤로 안에 박힘):
  - 셋트 가격 (큰 숫자)
  - 단품 합산 (취소선, 작게)
  - **절약액 + 절약 %** 강조 (예: "✓2,800원 절약 (12%)") — 양수일 때만
  - 구매하기 / 장바구니 버튼 (장바구니는 추후 — 일단 비활성 또는 hidden)

### 2.2 STEP 흐름
- 각 plugin이 `display_order` 순서로 카드
- 카드: `Step N` 칩 + plugin.title + plugin.summary + 상세 페이지 링크
- 카드 hover: 다크 그라디언트 살짝 + translate

### 2.3 "왜 이 조합인가" 스토리
- 기존 `bundle.story` 표시. 좀 더 가독성 있는 본문 톤 (max-width: 720px, leading 1.7)

### 2.4 메타 푸터
- 큐레이터 닉네임 (있으면)
- updated_at (포맷)
- 관리자에게 보이는 "셋트 삭제" 버튼 (기존)

## 3. 가격 계산 로직
JSP 안에서 `<c:set>` 로:
```jsp
<c:set var="sumOfParts" value="0"/>
<c:forEach var="p" items="${bundle.plugins}">
  <c:set var="sumOfParts" value="${sumOfParts + p.price}"/>
</c:forEach>
<c:set var="savings" value="${sumOfParts - bundle.price}"/>
<c:set var="savingsPct" value="${sumOfParts > 0 ? (savings * 100 / sumOfParts) : 0}"/>
```
- `savings > 0` 일 때만 절약 라벨 노출
- `sumOfParts == 0` 가드 (0으로 나누기 방지)

## 4. 스타일 추가 (`app.css`)
- `.bundle-hero` — 그라디언트 배경, 큰 타입, 상하 여백
- `.chip-meta` — 작은 칩 (카테고리, 난이도)
- `.price-card` — 박스, 가격 비교 강조
- `.savings-badge` — accent-2 (민트) 강조 라벨
- `.step-row` — Step 카드 리스트 컨테이너
- `.step-card` — Step 1/2/3 개별 카드 (글래스 + hover)
- 모바일 break: 768px (price-card 안의 가격/버튼 세로 스택)

## 5. 비스코프
- 별점/리뷰 시스템 (모델 없음)
- plugin별 "이 셋트에서의 역할" 별도 텍스트 (DB 컬럼 추가 필요)
- 난이도/소요시간 동적값 (정적 라벨로 임시)
- 장바구니 흐름 (장바구니 자체는 plugin 기반, bundle 가입은 Phase B4 후속)

## 6. 후속 impeccable 작업
구현 끝 → `impeccable:adapt` (모바일 반응형) → `impeccable:polish` (최종 정리)
