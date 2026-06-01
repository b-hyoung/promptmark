<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle">데모 — 디자인 마감 셋트</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<div class="demo-shell">
  <header class="demo-hero">
    <p class="crumb"><a href="${ctx}/app/bundle/detail?id=3">← 디자인 마감 셋트</a> · 데모 (실제 실행 캡처)</p>
    <h1>weekend.notes — 노트앱 랜딩 3일 작업</h1>
    <p class="scenario">"주말 한정 메모앱 랜딩 페이지 필요" 의뢰. 셋트가 무드·헤드라인·본문까지 각 단계마다 질문 → 답에 따라 마감.</p>
    <div class="demo-skills">
      <span class="demo-skill">frontend-design</span>
      <span class="demo-skill">ogilvy</span>
      <span class="demo-skill">copywriting</span>
    </div>
  </header>

  <nav class="demo-tabs">
    <a href="#result">📄 결과물</a>
    <a href="#history">📜 작업 히스토리 (인터랙티브)</a>
  </nav>

  <section class="demo-section" id="result">
    <span class="demo-section-label">/ result</span>
    <h2>최종 산출물 — 랜딩 페이지</h2>
    <p class="lead">컴포넌트 12개로 만든 1페이지 랜딩. 그대로 출시 가능.</p>
    <div class="demo-canvas landing-shell">
      <nav class="landing-nav">
        <span class="brand">weekend.notes</span>
        <div class="links"><span>기능</span><span>가격</span><span>로그인</span></div>
      </nav>
      <section class="landing-hero-section">
        <h1>지난 주말, 무슨 생각을 했는지 기억나세요?</h1>
        <p>매주 일요일 밤 11시, 한 주의 메모를 묶어 보내드립니다. 평일엔 메모만, 정리는 우리가.</p>
        <a class="btn" href="#">14일 무료로 시작</a>
      </section>
      <div class="landing-feat">
        <div class="feat-cell"><h3>📌 한 줄 메모</h3><p>아이폰 단축어 한 번으로 메모. 30초도 안 걸립니다.</p></div>
        <div class="feat-cell"><h3>🔍 주간 요약</h3><p>매주 일요일, AI가 7일치를 한 문단으로.</p></div>
        <div class="feat-cell"><h3>📬 이메일로</h3><p>요약을 일요일 밤 11시에 메일로. 앱 안 열어도 됩니다.</p></div>
      </div>
    </div>
  </section>

  <section class="demo-section" id="history">
    <span class="demo-section-label">/ history · interactive</span>
    <h2>작업 히스토리 — 3일 · 18시간</h2>
    <p class="lead">의뢰 → 무드 결정 → 페이지 골격 → 헤드라인 4안 → 본문 마감.</p>

    <div class="timeline-wrap">
      <div class="timeline-rail"><div class="timeline-progress"></div></div>
      <ol class="history-list interactive-history">
        <li class="history-item" data-step="1">
          <span class="history-time">D1 10:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span><span class="history-msg">의뢰</span></div>
            <div class="history-msg quote">"주말 한정 메모앱 랜딩 페이지. 핵심: 일요일 밤 11시 자동 요약."</div>
          </div>
        </li>
        <li class="history-item" data-step="2">
          <span class="history-time">D1 10:08</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">frontend-design</span><span class="history-msg">무드 후보</span></div>
            <div class="history-msg ask">전체 무드는? ① 다크 미니멀 (Linear 결) ② 라이트 따뜻 (Notion 결) ③ 컬러풀 펀 (Duolingo 결)</div>
          </div>
        </li>
        <li class="history-item" data-step="3">
          <span class="history-time">D1 10:09</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">① 다크 미니멀</div>
          </div>
        </li>
        <li class="history-item" data-step="4">
          <span class="history-time">D1 11:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">frontend-design</span><span class="history-msg">컴포넌트 시스템 12개</span></div>
            <div class="history-out">Btn(primary/ghost) · Card · Form · Nav · Hero · Feature · Footer · Badge · Modal · Tab · Input · Toast</div>
          </div>
        </li>
        <li class="history-item" data-step="5">
          <span class="history-time">D2 09:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">frontend-design</span><span class="history-msg">페이지 골격 — 1페이지 vs 멀티페이지?</span></div>
            <div class="history-msg ask">랜딩 형식은? ① 1페이지 (Hero + 기능 + CTA) ② 멀티 (Hero / 기능 / 가격 / About)</div>
          </div>
        </li>
        <li class="history-item" data-step="6">
          <span class="history-time">D2 09:01</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">① 1페이지 — 첫 출시라 가볍게</div>
          </div>
        </li>
        <li class="history-item" data-step="7">
          <span class="history-time">D2 13:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">ogilvy</span><span class="history-msg">헤드라인 4 각도</span></div>
            <div class="history-out">
              <strong>질문</strong>: "지난 주말, 무슨 생각을 했는지 기억나세요?" ← 추천<br>
              <strong>약속</strong>: "한 주의 생각, 한 통의 메일"<br>
              <strong>숫자</strong>: "매주 일요일 11시. 7일치 메모, 1통의 요약."<br>
              <strong>부정</strong>: "주중에 한 생각, 주말에 사라지는 게 정상은 아닙니다."
            </div>
          </div>
        </li>
        <li class="history-item" data-step="8">
          <span class="history-time">D2 14:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">질문 각도 채택 — 첫 방문자가 자기 경험에 비춰보게</div>
          </div>
        </li>
        <li class="history-item" data-step="9">
          <span class="history-time">D2 14:20</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">copywriting</span><span class="history-msg">서브헤드 + 기능 3개</span></div>
            <div class="history-out">서브: "매주 일요일 밤 11시..." / 기능: 📌 한 줄 메모 · 🔍 주간 요약 · 📬 이메일</div>
          </div>
        </li>
        <li class="history-item" data-step="10">
          <span class="history-time">D3 10:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">copywriting</span><span class="history-msg">CTA · 빈 상태 · 에러 메시지</span></div>
            <div class="history-out">
              CTA: "14일 무료로 시작"<br>
              빈 상태: "이번 주는 아직 메모가 없네요. 한 줄부터 시작해볼까요?"<br>
              결제 에러: "결제가 실패했어요. 카드 다시 확인해주시거나, 다른 카드 시도해주세요."
            </div>
          </div>
        </li>
        <li class="history-item" data-step="11">
          <span class="history-time">D3 17:00</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill system">SYSTEM</span><span class="history-msg">배포 완료</span></div>
            <div class="history-msg gate">총 작업 18시간. 디자이너 외주 견적 800만원/3주 → 0원/3일.</div>
          </div>
        </li>
      </ol>
    </div>
  </section>

  <%@ include file="/WEB-INF/view/demo/_signature.jspf" %>
</div>

<script>
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
