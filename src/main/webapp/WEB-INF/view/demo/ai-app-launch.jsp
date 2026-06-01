<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle">데모 — Claude API 앱 출시 셋트</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<div class="demo-shell">
  <header class="demo-hero">
    <p class="crumb"><a href="${ctx}/app/bundle/detail?id=5">← Claude API 앱 출시 셋트</a> · 데모 (실제 실행 캡처)</p>
    <h1>InterviewLab — 기술 인터뷰 질문 생성기</h1>
    <p class="scenario">"인터뷰 준비 도구" 7일 만에 출시. 모델·캐싱·UI·정리 각 단계마다 셋트가 질문 → 결정.</p>
    <div class="demo-skills">
      <span class="demo-skill">claude-api</span>
      <span class="demo-skill">frontend-design</span>
      <span class="demo-skill">simplify</span>
    </div>
  </header>

  <nav class="demo-tabs">
    <a href="#result">📄 결과물</a>
    <a href="#history">📜 작업 히스토리 (인터랙티브)</a>
  </nav>

  <section class="demo-section" id="result">
    <span class="demo-section-label">/ result</span>
    <h2>실제 동작 — 챗 인터페이스</h2>
    <p class="lead">입력란에 키워드 (React / 파이썬 / 자료구조 / 시스템 디자인) → 질문 5개 생성.</p>
    <div class="demo-canvas chat-demo">
      <div class="chat-demo-head">
        <span class="chat-title">InterviewLab · Sonnet 4.6</span>
        <span class="chat-cost">⚡ cache hit · $0.12/세션</span>
      </div>
      <div class="chat-demo-body" id="chatBody">
        <div class="b-bot">키워드를 입력하면 그 분야의 기술 인터뷰 질문 5개를 만들어드립니다.<br>(React / 파이썬 / 자료구조 / 시스템 디자인)</div>
      </div>
      <div class="chat-demo-foot">
        <input id="chatInput" type="text" placeholder="키워드 입력 후 엔터 (예: React)" autocomplete="off">
        <button onclick="sendChat()">전송</button>
      </div>
    </div>
  </section>

  <section class="demo-section" id="history">
    <span class="demo-section-label">/ history · interactive</span>
    <h2>작업 히스토리 — 7일</h2>
    <div class="timeline-wrap">
      <div class="timeline-rail"><div class="timeline-progress"></div></div>
      <ol class="history-list interactive-history">
        <li class="history-item" data-step="1">
          <span class="history-time">D1 10:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span><span class="history-msg">의뢰</span></div>
            <div class="history-msg quote">"기술 인터뷰 질문 생성기. 키워드 → 질문 5개. 토큰 비용 최소화."</div>
          </div>
        </li>
        <li class="history-item" data-step="2">
          <span class="history-time">D1 10:14</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">claude-api</span><span class="history-msg">모델 선택 질문</span></div>
            <div class="history-msg ask">어떤 모델? ① Opus 4.7 (최고 품질) ② Sonnet 4.6 (균형) ③ Haiku 4.5 (가장 저렴)</div>
          </div>
        </li>
        <li class="history-item" data-step="3">
          <span class="history-time">D1 10:14</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">② Sonnet — 인터뷰 질문 수준에 Opus는 과함</div>
          </div>
        </li>
        <li class="history-item" data-step="4">
          <span class="history-time">D1 10:30</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">claude-api</span><span class="history-msg">캐싱 전략</span></div>
            <div class="history-msg ask">cache_control 적용 범위는? ① 시스템 프롬프트만 ② 시스템 + few-shot 예제 ③ 안 함</div>
          </div>
        </li>
        <li class="history-item" data-step="5">
          <span class="history-time">D1 10:31</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">② 시스템 + few-shot — 키워드만 다르고 패턴 같으니 캐시 잘 먹음</div>
          </div>
        </li>
        <li class="history-item" data-step="6">
          <span class="history-time">D2 14:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">claude-api</span><span class="history-msg">백엔드 API 완성</span></div>
            <div class="history-out">POST /api/generate · 평균 응답 1.8s · cache hit율 94% · $0.43 → $0.12 (72% 절감)</div>
          </div>
        </li>
        <li class="history-item" data-step="7">
          <span class="history-time">D3 09:30</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">frontend-design</span><span class="history-msg">UI 형태</span></div>
            <div class="history-msg ask">UI는? ① 챗 (대화형) ② 폼 (한 번 입력 → 결과) ③ 카드 카탈로그 (저장된 질문 목록)</div>
          </div>
        </li>
        <li class="history-item" data-step="8">
          <span class="history-time">D3 09:31</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">① 챗 — 반복 사용 시 자연스러움</div>
          </div>
        </li>
        <li class="history-item" data-step="9">
          <span class="history-time">D3-D4</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">frontend-design</span><span class="history-msg">컴포넌트 + 에러/빈/로딩 상태</span></div>
            <div class="history-out">User 버블 · Bot 버블 · 입력 폼 · 스트리밍 인디케이터 · "응답 못 받음" 재시도 · 빈 상태 ("React 같은 키워드를 입력해보세요")</div>
          </div>
        </li>
        <li class="history-item" data-step="10">
          <span class="history-time">D6 16:30</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">simplify</span><span class="history-msg">출시 직전 코드 정리</span></div>
            <div class="history-out">중복 함수 4개 통합 · 사용 안 하는 import 12개 · 죽은 컴포넌트 2개</div>
          </div>
        </li>
        <li class="history-item" data-step="11">
          <span class="history-time">D7 14:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill system">SYSTEM</span><span class="history-msg">Product Hunt 출시</span></div>
            <div class="history-msg gate">베타 사용자 47명 · 첫 24시간 호출 1,200건 · 비용 $144 (캐싱 없으면 $516)</div>
          </div>
        </li>
      </ol>
    </div>
  </section>

  <%@ include file="/WEB-INF/view/demo/_signature.jspf" %>
</div>

<script>
const RESPONSES = {
  'react': ['1. useEffect의 cleanup 함수가 호출되는 정확한 시점은 언제이고, 왜 그 시점이어야 하는지 설명해주세요.',
            '2. Context API vs Redux: 어느 시점에 Redux로 갈아타야 하는지 판단 기준 3가지.',
            '3. React 18의 concurrent rendering이 기존 코드의 어떤 가정을 깨뜨릴 수 있나요?',
            '4. memo / useMemo / useCallback — 셋이 풀어주려는 문제가 다른 점은?',
            '5. Server Components가 도입되면서 어떤 패턴이 안티패턴이 됐나요?'],
  '파이썬': ['1. GIL이 있는데도 Python에서 멀티스레딩이 의미가 있는 케이스를 설명해주세요.',
              '2. asyncio의 event loop와 그냥 threading의 차이를 한 그림으로 설명한다면?',
              '3. __slots__를 쓰면 메모리가 줄어드는 메커니즘과 안 쓰는 게 나은 케이스는?',
              '4. list comprehension vs generator expression — 언제 어느 쪽?',
              '5. dataclass vs Pydantic — 비즈니스 로직에 어느 게 적합한가요?'],
  '자료구조': ['1. HashMap의 충돌 처리 — Chaining vs Open Addressing의 트레이드오프.',
                '2. Red-Black Tree와 AVL Tree, 실무에서 어느 게 더 자주 쓰이고 그 이유는?',
                '3. Heap의 시간복잡도가 O(log n)인데도 정렬에서 QuickSort보다 느린 이유는?',
                '4. Trie를 쓸 때 메모리 폭발을 막는 일반적인 패턴은?',
                '5. Skip List가 트리를 대신할 수 있는 케이스를 설명해주세요.'],
  '시스템 디자인': ['1. URL shortener를 설계할 때 충돌을 피하는 방법 3가지와 각각의 트레이드오프.',
                     '2. 채팅 시스템에서 message ordering을 보장하려면 어디서 어떻게?',
                     '3. CDN cache invalidation을 신뢰성 있게 하는 패턴은?',
                     '4. Rate limiter — sliding window vs token bucket 어느 게 더 정확하고 왜?',
                     '5. Distributed transaction을 피하기 위한 saga pattern의 한계는?']
};
function appendBubble(cls, text) {
  const body = document.getElementById('chatBody');
  const div = document.createElement('div');
  div.className = cls;
  div.innerHTML = text.replace(/\n/g, '<br>');
  body.appendChild(div);
  body.scrollTop = body.scrollHeight;
}
function sendChat() {
  const input = document.getElementById('chatInput');
  const q = input.value.trim();
  if (!q) return;
  appendBubble('b-user', q);
  input.value = '';
  const key = Object.keys(RESPONSES).find(k => q.toLowerCase().includes(k.toLowerCase()));
  setTimeout(() => {
    if (key) {
      appendBubble('b-bot', '<strong>' + key + '</strong> 기술 인터뷰 질문 5개:<br><br>' + RESPONSES[key].join('<br><br>'));
    } else {
      appendBubble('b-bot', '죄송해요, 데모에서는 다음 키워드만 응답해요: React / 파이썬 / 자료구조 / 시스템 디자인');
    }
  }, 400);
}
document.getElementById('chatInput').addEventListener('keypress', (e) => { if (e.key === 'Enter') sendChat(); });

// timeline activation
(function() {
  const items = document.querySelectorAll('.interactive-history .history-item');
  const bar = document.querySelector('.timeline-progress');
  if (!items.length) return;
  const io = new IntersectionObserver((entries) => {
    entries.forEach(e => { if (e.isIntersecting && e.intersectionRatio > 0.4) e.target.classList.add('is-active'); });
    if (bar) bar.style.height = (document.querySelectorAll('.interactive-history .history-item.is-active').length / items.length * 100) + '%';
  }, { threshold: [0.4] });
  items.forEach(it => { io.observe(it); it.addEventListener('click', () => { items.forEach(x => x.classList.remove('is-focus')); it.classList.add('is-focus'); }); });
})();
</script>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
