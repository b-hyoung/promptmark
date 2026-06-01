<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle">About — promptmark</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<div class="about-shell">
  <header class="about-hero">
    <span class="eyebrow">ABOUT promptmark</span>
    <h1>스킬을 사는 게 아니라, <span style="color:var(--accent);">조합을 사는 곳</span>.</h1>
    <p>Claude Code Skill이 100개를 넘어가면서 사용자가 막힙니다. <strong>"어떤 걸 묶어 써야 진짜로 결과가 나오는지"</strong>를 모릅니다. promptmark는 그걸 한 사람이 직접 골라서 알려주는 곳입니다.</p>
  </header>

  <section class="about-block">
    <h2>큐레이션 철학</h2>
    <ol class="about-list">
      <li><strong>한 도구로 풀리지 않는 일만</strong> — 단일 스킬 1개로 충분하면 셋트가 아닙니다. 셋트는 항상 2~4개의 스킬이 정확히 보완할 때만 묶입니다.</li>
      <li><strong>실측 가능한 결과</strong> — "더 빠르게", "더 나은" 같은 모호한 약속은 안 합니다. 각 셋트는 시간·비용·CTR·라운드트립 같은 수치로 효과를 검증합니다.</li>
      <li><strong>큐레이터가 직접 만들어본 것만</strong> — 모든 셋트는 큐레이터가 진짜로 한 번 이상 사용해서 결과물을 낸 조합입니다. 그 결과물과 작업 히스토리를 그대로 공개합니다.</li>
      <li><strong>한 도메인 한 시점 한 셋트</strong> — 같은 도메인에 비슷한 셋트 여러 개를 두지 않습니다. 선택 피로를 줄입니다.</li>
    </ol>
  </section>

  <section class="about-block">
    <h2>왜 마켓플레이스인가</h2>
    <p>Claude Code Skill은 오픈소스로 GitHub에 다 있습니다. 누구나 무료로 가져다 쓸 수 있죠. 그런데도 사람들은 막힙니다. <strong>"이 일에 어떤 스킬을 어떤 순서로?"</strong> 가 답이 없으니까요.</p>
    <p>이 사이트가 파는 것은 스킬 그 자체가 아니라 <strong>"이 일에는 이 조합"</strong> 이라는 결정의 시간입니다. 셋트 하나를 사면, 같은 도메인에서 며칠을 헤맬 시행착오를 한 번 건너뜁니다.</p>
  </section>

  <section class="about-block">
    <h2>큐레이터</h2>
    <div class="curator-card">
      <div class="curator-avatar">b</div>
      <div>
        <h3>b-hyoung</h3>
        <p>Claude Code 헤비 유저. 사이드 프로젝트 3개 운영 중. 각 셋트는 자기 일에서 실제로 한 번 이상 묶어 쓴 조합입니다.</p>
      </div>
    </div>
  </section>

  <section class="about-block">
    <h2>지금 보고 있는 사이트가 만들어진 과정</h2>
    <p>참고로 — promptmark 자체도 위에 있는 셋트 중 하나(<a href="${ctx}/app/bundle/detail?id=6">MVP 부트스트랩</a>)로 만들었습니다. 7일, 솔로 작업. 진짜 한 흐름이 통하는지 자기 자신에게 먼저 적용해봤습니다.</p>
  </section>

  <section class="about-cta">
    <h2>다음 한 셋트를 골라보세요.</h2>
    <a class="btn" href="${ctx}/app/bundle/list">전체 셋트 보기 →</a>
  </section>
</div>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
