<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle">데모 — 블로그 자동화 셋트</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<div class="demo-shell">
  <header class="demo-hero">
    <p class="crumb"><a href="${ctx}/app/bundle/detail?id=1">← 블로그 자동화 셋트</a> · 데모 (실제 실행 캡처)</p>
    <h1>도구를 더 찾는 게 답인 줄 알았다.<br>6개 셋트를 만들고 나서 생각이 바뀌었다.</h1>
    <p class="scenario">사용자가 promptmark 큐레이터에게 "promptmark 만든 과정"이라는 주제를 던졌고, blog-automation 셋트(superpowers · copywriting · stop-slop)가 실제로 동작한 진짜 인터랙션입니다.</p>
    <div class="demo-skills">
      <span class="demo-skill">superpowers · brainstorming</span>
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
    <h2>최종 산출물 — 블로그 글</h2>
    <p class="lead">실제로 셋트가 위 인터랙션을 거쳐 만든 발행 가능 상태의 초안입니다.</p>

    <div class="demo-canvas">
      <article class="blog-article">
        <div class="blog-meta">2026.06.01 · 6분 읽기 · 작성 31분</div>
        <h1>도구를 더 찾는 게 답인 줄 알았다.<br>6개 셋트를 만들고 나서 생각이 바뀌었다.</h1>
        <p class="subhead">100개+ Claude Code Skill 중 8개만 골랐다. 그리고 6개의 셋트로 묶었다. 그러고 나서야 마켓이 시작됐다.</p>

        <p>Claude Code Skill 카탈로그를 처음 봤을 때 내가 한 일은 "뭐가 더 있나" 였다. 새 스킬을 보면 일단 깔아보고, 더 좋은 게 있을까 싶어 또 찾았다. 카탈로그가 늘어날수록 막막함도 늘었다.</p>

        <p>어느 날 사이드 프로젝트 하나를 새로 시작하면서 깨달았다. <strong>도구가 부족해서가 아니었다.</strong> 같은 도구를 쓰는데도 일이 안 풀리는 이유는 "이 작업에 어떤 도구를 어떤 순서로?" 라는 질문에 답이 없어서였다.</p>

        <h2>그래서 8개만 골랐다</h2>

        <p>한 달간 자주 쓰던 100여 개 스킬 중 8개만 남겼다. 기준은 단순했다 — <strong>"한 도메인에서 다른 도구로는 대체가 안 되는 것"</strong>. 100개 중 그 기준에 맞는 게 정확히 8개였다.</p>

        <p>그 8개를 6개의 도메인별 셋트로 묶었다. 블로그 자동화, 코드 품질, 디자인 마감, 광고 카피, AI 앱 출시, MVP 부트스트랩. 같은 스킬이 셋트마다 다른 역할을 했다. <code>copywriting</code> 은 블로그에서는 "본문 작성"이고, 광고 셋트에서는 "본문·CTA"고, 디자인 셋트에서는 "마감 카피" 였다.</p>

        <div class="pull">같은 도구가 셋트마다 다른 일을 한다. 그게 도구 추가만으로는 안 나오는 가치다.</div>

        <h2>도구를 끌리는 게 아니라 묶기였다</h2>

        <p>6개 셋트를 만들고 나서야 한 줄로 정리됐다. 막힘의 원인은 도구 수가 아니라 "이 일에 이 조합" 의 부재였다. 다음 사이드 프로젝트에서 새 도구를 찾기 전에, 가진 도구를 어떻게 묶을지부터 그렸다. 그 후로 시작 시간이 절반쯤 줄었다.</p>

        <p>지금 promptmark에 올라온 6개 셋트는 그 과정에서 자기 자신을 위해 먼저 만든 것들이다. 같은 막힘을 똑같이 겪을 사람이 있을 것 같아서 그대로 공개했다.</p>
      </article>
    </div>
  </section>

  <section class="demo-section" id="history">
    <span class="demo-section-label">/ history · interactive</span>
    <h2>작업 히스토리 — 31분 (실제 인터랙션)</h2>
    <p class="lead">스크롤을 내리면 각 단계가 차례로 활성화됩니다. 왼쪽의 timeline 점이 진행 상태를 보여줘요.</p>

    <div class="timeline-wrap">
      <div class="timeline-rail" aria-hidden="true">
        <div class="timeline-progress" id="timelineProgress"></div>
      </div>

      <ol class="history-list interactive-history" id="historyList">
        <li class="history-item" data-step="1">
          <span class="history-time">14:23</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span><span class="history-msg">의뢰 — 글 주제 한 줄</span></div>
            <div class="history-msg quote">"promptmark 만든 과정을 글로 쓰고 싶다."</div>
          </div>
        </li>

        <li class="history-item" data-step="2">
          <span class="history-time">14:24</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">superpowers · brainstorming</span><span class="history-msg">독자 정의 질문</span></div>
            <div class="history-msg ask">핵심 독자는 누구인가요? ① 인디 개발자  ② Claude Code 깊이 쓰는 개발자  ③ AI 도구 탐색자 · 조합을 찾는 사람</div>
          </div>
        </li>

        <li class="history-item" data-step="3">
          <span class="history-time">14:25</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">③ AI 도구 탐색자 · 조합을 찾는 사람</div>
          </div>
        </li>

        <li class="history-item" data-step="4">
          <span class="history-time">14:26</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">superpowers · brainstorming</span><span class="history-msg">핵심 메시지 질문</span></div>
            <div class="history-msg ask">독자가 글을 끝까지 읽고 "한 줄로" 가져갈 메시지는? ① 관점 전환  ② 실용 안내  ③ 개인 서사</div>
          </div>
        </li>

        <li class="history-item" data-step="5">
          <span class="history-time">14:27</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">"도구를 끌리는 게 아니라 조합을 설계하는 게 핵심이다"</div>
          </div>
        </li>

        <li class="history-item" data-step="6">
          <span class="history-time">14:28</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">superpowers · brainstorming</span><span class="history-msg">글 각도 3안 제시</span></div>
            <div class="history-out">
              ① 1인칭 회고 — "6개 셋트를 만들고 깨달은 것"<br>
              ② 경제학 대조 — "100개 스킬 중 8개면 왜 충분한가"<br>
              ③ 사례 나열 — "한 도구 vs 셋트 · 6가지 비교"
            </div>
          </div>
        </li>

        <li class="history-item" data-step="7">
          <span class="history-time">14:30</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">① 1인칭 회고 선택</div>
          </div>
        </li>

        <li class="history-item" data-step="8">
          <span class="history-time">14:31</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill system">SYSTEM</span><span class="history-msg">brainstorming spec 확정 · copywriting으로 이관</span></div>
            <div class="history-msg gate">spec: 독자 = AI 도구 탐색자 / 메시지 = 조합 설계 / 각도 = 1인칭 회고</div>
          </div>
        </li>

        <li class="history-item" data-step="9">
          <span class="history-time">14:32</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">copywriting</span><span class="history-msg">헤드라인 3안 생성</span></div>
            <div class="history-out">
              A. "100개 스킬 중 8개만 골랐다. 그러고 나서야 마켓이 시작됐다." <span style="color:var(--text-mute);">— 숫자+반전</span><br>
              B. <strong>"도구를 더 찾는 게 답인 줄 알았다. 6개 셋트를 만들고 나서 생각이 바뀌었다."</strong> <span style="color:var(--accent);">← 채택 (1인칭 회고에 정확)</span><br>
              C. "내가 만든 AI 도구 마켓 — 첫 질문은 '8개로 충분한가' 였다."
            </div>
          </div>
        </li>

        <li class="history-item" data-step="10">
          <span class="history-time">14:33</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">copywriting</span><span class="history-msg">서브헤드 + 본문 골격</span></div>
            <div class="history-out">
              Subhead: "100개+ Claude Code Skill 중 8개만 골랐다. 6개의 셋트로 묶었다. 그러고 나서야 마켓이 시작됐다."<br>
              Body: Lead → "그래서 8개만 골랐다" → 핵심 인용 (pull) → "도구를 끌리는 게 아니라 묶기였다"
            </div>
          </div>
        </li>

        <li class="history-item" data-step="11">
          <span class="history-time">14:35</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">stop-slop</span><span class="history-msg">AI 어휘 검출 → 교체</span></div>
            <div class="history-out">
              <code>수많은 도구들이 즐비한</code> → "Claude Code Skill 카탈로그를 처음 봤을 때"<br>
              <code>혁신적인 워크플로</code> → 삭제 (말 자체가 불필요)<br>
              <code>딥다이브</code> → "한 달간 자주 쓰던"<br>
              <code>패러다임 시프트</code> → "생각이 바뀌었다"
            </div>
          </div>
        </li>

        <li class="history-item" data-step="12">
          <span class="history-time">14:54</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill system">SYSTEM</span><span class="history-msg">발행 가능 상태</span></div>
            <div class="history-out">총 소요 31분. 단어 수 387. AI-tell 점수: 3% (목표 5% 이하). 헤드라인 길이: 56자 (Ogilvy 가이드 65자 이내).</div>
          </div>
        </li>
      </ol>
    </div>
  </section>

  <%@ include file="/WEB-INF/view/demo/_signature.jspf" %>
</div>

<script>
// scroll-driven activation
(function() {
  const items = document.querySelectorAll('#historyList .history-item');
  const bar = document.getElementById('timelineProgress');
  if (!items.length) return;

  const io = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting && entry.intersectionRatio > 0.4) {
        entry.target.classList.add('is-active');
      }
    });
    // progress bar = activated items / total
    const active = document.querySelectorAll('#historyList .history-item.is-active').length;
    if (bar) bar.style.height = ((active / items.length) * 100) + '%';
  }, { threshold: [0.4] });

  items.forEach(it => io.observe(it));

  // click to toggle a specific step focus
  items.forEach(it => {
    it.addEventListener('click', () => {
      items.forEach(x => x.classList.remove('is-focus'));
      it.classList.add('is-focus');
    });
  });
})();
</script>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
