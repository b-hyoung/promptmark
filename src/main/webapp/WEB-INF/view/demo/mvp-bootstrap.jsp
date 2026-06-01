<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle">데모 — MVP 부트스트랩 셋트</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<div class="demo-shell">
  <header class="demo-hero">
    <p class="crumb"><a href="${ctx}/app/bundle/detail?id=6">← MVP 부트스트랩 셋트</a> · 데모 (실제 실행 캡처)</p>
    <h1>OneLine — 한 줄 회고 트래커 (7일 MVP)</h1>
    <p class="scenario">"3일 내 출시 가능한 작은 회고 도구". 범위 → UI → 카피 → 코드 품질 각 단계 셋트가 질문 → 결정.</p>
    <div class="demo-skills">
      <span class="demo-skill">superpowers</span>
      <span class="demo-skill">frontend-design</span>
      <span class="demo-skill">copywriting</span>
      <span class="demo-skill">karpathy-guidelines</span>
    </div>
  </header>

  <nav class="demo-tabs">
    <a href="#result">📄 결과물</a>
    <a href="#history">📜 작업 히스토리 (인터랙티브)</a>
  </nav>

  <section class="demo-section" id="result">
    <span class="demo-section-label">/ result</span>
    <h2>실제 동작 — 한 줄 회고 + 주간 요약</h2>
    <p class="lead">평일에 한 줄씩 입력. 일요일 밤 11시에 AI 요약 (데모는 사전 정의).</p>
    <div class="demo-canvas mvp-shell">
      <div class="mvp-head"><h3>OneLine</h3><p>매일 한 줄. 일요일에 한 통의 정리.</p></div>
      <div class="mvp-input-row">
        <input id="onelineInput" type="text" placeholder="오늘은 어땠나요? 한 줄로." autocomplete="off">
        <button onclick="addEntry()">기록</button>
      </div>
      <div class="mvp-entries" id="entries">
        <div class="mvp-entry"><span class="mvp-date">금 05/30</span><span class="mvp-text">PR 리뷰가 한 번에 통과돼서 기분이 좋았다.</span><span class="mvp-mood">😊</span></div>
        <div class="mvp-entry"><span class="mvp-date">목 05/29</span><span class="mvp-text">API 마이그레이션 일정이 또 밀렸다.</span><span class="mvp-mood">😐</span></div>
        <div class="mvp-entry"><span class="mvp-date">수 05/28</span><span class="mvp-text">디자인 시스템 토큰 정리 끝.</span><span class="mvp-mood">😊</span></div>
        <div class="mvp-entry"><span class="mvp-date">화 05/27</span><span class="mvp-text">신입 페어 프로그래밍. 내가 더 배웠다.</span><span class="mvp-mood">😊</span></div>
        <div class="mvp-entry"><span class="mvp-date">월 05/26</span><span class="mvp-text">월요일 미팅 3시간. 결정된 건 1개.</span><span class="mvp-mood">😩</span></div>
      </div>
      <div class="mvp-weekly">
        <h4>📬 일요일 밤 11시 — 이번 주 요약 (AI)</h4>
        <p>이번 주는 <strong>제작 흐름</strong>은 좋았지만 <strong>회의 운영</strong>에서 시간을 많이 쓰셨네요. PR 통과·디자인 시스템 v2·페어 프로그래밍은 모두 한 흐름의 성취예요. 다음 주는 월요일 미팅 형식을 한 번 정리해보면 어떨까요?</p>
      </div>
    </div>
  </section>

  <section class="demo-section" id="history">
    <span class="demo-section-label">/ history · interactive</span>
    <h2>작업 히스토리 — 7일 · 28시간</h2>
    <div class="timeline-wrap">
      <div class="timeline-rail"><div class="timeline-progress"></div></div>
      <ol class="history-list interactive-history">
        <li class="history-item" data-step="1">
          <span class="history-time">D1 09:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span><span class="history-msg">의뢰</span></div>
            <div class="history-msg quote">"3일 안에 출시 가능한 작은 회고 도구."</div>
          </div>
        </li>
        <li class="history-item" data-step="2">
          <span class="history-time">D1 09:08</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">superpowers · brainstorming</span><span class="history-msg">범위 후보 3안</span></div>
            <div class="history-out">
              ① 자유 입력 회고 (Notion 클론) — <span style="color:var(--danger);">너무 큼</span><br>
              ② 한 줄 회고 + 주간 AI 요약<br>
              ③ 캘린더 회고 (날짜별 스티커)
            </div>
          </div>
        </li>
        <li class="history-item" data-step="3">
          <span class="history-time">D1 09:09</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">② — 한 줄이라 부담 없고, 주간 요약이 차별점</div>
          </div>
        </li>
        <li class="history-item" data-step="4">
          <span class="history-time">D1 09:15</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">superpowers · brainstorming</span><span class="history-msg">spec 확정 게이트</span></div>
            <div class="history-msg gate">3개 컴포넌트 (입력 폼 · 리스트 · 주간 요약) · DB 1테이블 · 일요일 23:00 cron · 이메일 발송</div>
          </div>
        </li>
        <li class="history-item" data-step="5">
          <span class="history-time">D2 10:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">frontend-design</span><span class="history-msg">톤 결정</span></div>
            <div class="history-msg ask">무드 톤? ① 다크 미니멀 ② 라이트 따뜻 ③ 종이 톤 (회고 무드)</div>
          </div>
        </li>
        <li class="history-item" data-step="6">
          <span class="history-time">D2 10:01</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">① 다크 미니멀 — 야간 입력 많을 듯</div>
          </div>
        </li>
        <li class="history-item" data-step="7">
          <span class="history-time">D2-D3</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">frontend-design</span><span class="history-msg">컴포넌트 3개 + 빈 상태</span></div>
            <div class="history-out">입력 폼 (80자 제한) · 리스트 (날짜 + 텍스트 + 이모지) · 주간 요약 카드</div>
          </div>
        </li>
        <li class="history-item" data-step="8">
          <span class="history-time">D4 11:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">copywriting</span><span class="history-msg">슬로건 + 마이크로카피</span></div>
            <div class="history-out">
              슬로건: <strong>"매일 한 줄. 일요일에 한 통의 정리."</strong><br>
              placeholder: "오늘은 어땠나요? 한 줄로."<br>
              빈 상태: "이번 주는 아직 메모가 없네요. 한 줄부터 시작해볼까요?"<br>
              요약 메일 제목: "이번 주 당신은요"
            </div>
          </div>
        </li>
        <li class="history-item" data-step="9">
          <span class="history-time">D5 15:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">karpathy-guidelines</span><span class="history-msg">우선 검토 영역</span></div>
            <div class="history-msg ask">PR 3건 — 어디 가장 먼저? ① 입력 폼 ② 주간 요약 cron ③ 이메일 발송</div>
          </div>
        </li>
        <li class="history-item" data-step="10">
          <span class="history-time">D5 15:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">③ 이메일 발송 — 실패하면 사용자가 모름</div>
          </div>
        </li>
        <li class="history-item" data-step="11">
          <span class="history-time">D5-D6</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">karpathy-guidelines</span><span class="history-msg">3 PR 검토 결과</span></div>
            <div class="history-out">
              PR #3 (이메일): null 체크 1건 추가 권장 → 수정<br>
              PR #2 (cron): 가정 명시 누락 1건 (timezone) → 수정<br>
              PR #1 (폼): max length 가정 명시 → 수정
            </div>
          </div>
        </li>
        <li class="history-item" data-step="12">
          <span class="history-time">D7 14:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill system">SYSTEM</span><span class="history-msg">Product Hunt 출시</span></div>
            <div class="history-msg gate">총 코딩 28시간 (평균 MVP 120h 대비 23%) · 첫 24시간 가입 47명 · 7일 후 retention 41%</div>
          </div>
        </li>
      </ol>
    </div>
  </section>

  <%@ include file="/WEB-INF/view/demo/_signature.jspf" %>
</div>

<script>
function addEntry() {
  const input = document.getElementById('onelineInput');
  const text = input.value.trim();
  if (!text) return;
  const list = document.getElementById('entries');
  const div = document.createElement('div');
  div.className = 'mvp-entry';
  const now = new Date();
  const days = ['일','월','화','수','목','금','토'];
  const date = days[now.getDay()] + ' ' + String(now.getMonth()+1).padStart(2,'0') + '/' + String(now.getDate()).padStart(2,'0');
  div.innerHTML = '<span class="mvp-date">' + date + '</span>'
                + '<span class="mvp-text">' + text.replace(/</g,'&lt;') + '</span>'
                + '<span class="mvp-mood">📝</span>';
  list.insertBefore(div, list.firstChild);
  input.value = '';
}
document.getElementById('onelineInput').addEventListener('keypress', (e) => { if (e.key === 'Enter') addEntry(); });

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
