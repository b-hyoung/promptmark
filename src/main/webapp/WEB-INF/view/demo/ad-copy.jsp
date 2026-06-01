<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle">데모 — 광고 카피 셋트</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<div class="demo-shell">
  <header class="demo-hero">
    <p class="crumb"><a href="${ctx}/app/bundle/detail?id=4">← 광고 카피 셋트</a> · 데모 (실제 실행 캡처)</p>
    <h1>인보이스 자동화 SaaS — 광고 A/B/C 캠페인</h1>
    <p class="scenario">B2B SaaS 광고 의뢰. 타겟 정의 → 각도 4안 → 본문 마감 → 클리셰 제거. 실 운영 7일 결과 포함.</p>
    <div class="demo-skills">
      <span class="demo-skill">ogilvy</span>
      <span class="demo-skill">copywriting</span>
      <span class="demo-skill">stop-slop</span>
    </div>
  </header>

  <nav class="demo-tabs">
    <a href="#result">📄 결과물</a>
    <a href="#history">📜 작업 히스토리 (인터랙티브)</a>
  </nav>

  <section class="demo-section" id="result">
    <span class="demo-section-label">/ result</span>
    <h2>광고 3안 (A/B/C) — 7일 운영 후 CTR</h2>
    <p class="lead">A 2.1% · B 3.4% · <strong style="color:var(--accent);">C 4.8%</strong> → C 채택.</p>
    <div class="demo-canvas ad-stack">
      <div class="ad-card">
        <div class="ad-platform">Meta Ads · A안 · CTR 2.1%</div>
        <div class="ad-head">매달 14시간을 인보이스에 쓰는 회계 담당자에게.</div>
        <div class="ad-desc">이 도구를 보여주면 30분 안에 도입을 결정합니다. 14일 무료 체험.</div>
        <div class="ad-link"><span>invoice-pro.kr · 광고</span><span class="ad-cta">무료 체험</span></div>
      </div>
      <div class="ad-card">
        <div class="ad-platform">Meta Ads · B안 · CTR 3.4%</div>
        <div class="ad-head">300개 거래처가 있어도 이번 달 마감은 19분이면 끝.</div>
        <div class="ad-desc">SAP 안 깔아도 됩니다. 엑셀에서 그대로 옮겨오는 데 3분.</div>
        <div class="ad-link"><span>invoice-pro.kr · 광고</span><span class="ad-cta">19분 데모 보기</span></div>
      </div>
      <div class="ad-card" style="border-color: var(--accent);">
        <div class="ad-platform" style="color:var(--accent);">Meta Ads · C안 · CTR 4.8% · 채택</div>
        <div class="ad-head">"엑셀 함수로 어떻게든 되겠지" —<br>그 어떻게든이 매달 14시간을 잡아먹습니다.</div>
        <div class="ad-desc">300개 거래처 마감을 19분으로. 가입 30초, 신용카드 필요 없음, 14일 무료.</div>
        <div class="ad-link"><span>invoice-pro.kr · 광고</span><span class="ad-cta">14시간 → 19분, 확인</span></div>
      </div>
    </div>
  </section>

  <section class="demo-section" id="history">
    <span class="demo-section-label">/ history · interactive</span>
    <h2>작업 히스토리 — 42분 + 7일 측정</h2>
    <div class="timeline-wrap">
      <div class="timeline-rail"><div class="timeline-progress"></div></div>
      <ol class="history-list interactive-history">
        <li class="history-item" data-step="1">
          <span class="history-time">11:08</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span><span class="history-msg">의뢰</span></div>
            <div class="history-msg quote">"인보이스 자동화 SaaS. 타겟은 중소 회계 담당자. CTR 3% 이상."</div>
          </div>
        </li>
        <li class="history-item" data-step="2">
          <span class="history-time">11:08</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">ogilvy</span><span class="history-msg">타겟 좁히기</span></div>
            <div class="history-msg ask">"중소 회계 담당자" 안에서 더 구체적으로? ① 100~500거래처 ② 500~2000 ③ 거래처 무관 · 인원수 기준 ④ 회계 외주받는 기업</div>
          </div>
        </li>
        <li class="history-item" data-step="3">
          <span class="history-time">11:09</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">① 100~500거래처 — 가장 시간 많이 걸리는 구간</div>
          </div>
        </li>
        <li class="history-item" data-step="4">
          <span class="history-time">11:10</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">ogilvy</span><span class="history-msg">헤드라인 8안 (4 각도)</span></div>
            <div class="history-out">
              <strong>약속</strong>: ① 14시간 → 19분 ② 마감을 잊어버리세요<br>
              <strong>숫자</strong>: ③ 300개 거래처 19분 ④ 14시간 절약<br>
              <strong>부정</strong>: ⑤ 엑셀로 어떻게든... ⑥ SAP 없이 사는 법<br>
              <strong>호기심</strong>: ⑦ 회계 담당자가 안 알려주는 ⑧ 19분이 가능한 이유
            </div>
          </div>
        </li>
        <li class="history-item" data-step="5">
          <span class="history-time">11:11</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">ogilvy</span><span class="history-msg">A/B/C 추천</span></div>
            <div class="history-msg ask">CTR 비교용 A/B/C 어떻게 가는 게 좋을까? ① 약속·숫자·부정 ② 숫자·부정·호기심 ③ 약속·부정·호기심</div>
          </div>
        </li>
        <li class="history-item" data-step="6">
          <span class="history-time">11:11</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">① 약속·숫자·부정 — 호기심은 클릭 후 이탈 위험</div>
          </div>
        </li>
        <li class="history-item" data-step="7">
          <span class="history-time">11:15</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">copywriting</span><span class="history-msg">A/B/C 각각 본문 + CTA + sub-CTA</span></div>
            <div class="history-out">A: "이 도구를 보여주면 30분 안에 도입을..." · B: "SAP 안 깔아도..." · C: "300개 거래처 마감을 19분으로..."</div>
          </div>
        </li>
        <li class="history-item" data-step="8">
          <span class="history-time">11:25</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">stop-slop</span><span class="history-msg">마케팅 클리셰 검출 → 교체</span></div>
            <div class="history-out">
              <code>혁신적인</code> → 삭제 (불필요)<br>
              <code>맞춤형 솔루션</code> → "회계팀에 딱 맞는"<br>
              <code>패러다임</code> → 삭제<br>
              <code>최적화된 워크플로</code> → "엑셀에서 그대로 옮겨오는"
            </div>
          </div>
        </li>
        <li class="history-item" data-step="9">
          <span class="history-time">11:30</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill system">SYSTEM</span><span class="history-msg">3안 발행 → Meta Ads Manager</span></div>
            <div class="history-msg gate">예산 일평균 ₩50,000 × 7일 × 3안 · 동일 타겟</div>
          </div>
        </li>
        <li class="history-item" data-step="10">
          <span class="history-time">+7일</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill system">SYSTEM</span><span class="history-msg">CTR 측정 결과 회수 → C로 전환</span></div>
            <div class="history-out">A 2.1% / B 3.4% / <strong>C 4.8%</strong> · CPC ₩340 → ₩185 · 가입전환율 2.3배</div>
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
