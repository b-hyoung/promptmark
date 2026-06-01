<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.HashMap, java.util.Map" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="bundle.messages"/>
<c:set var="pageTitle"><c:out value='${bundle.name}'/> — promptmark</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<%-- 단품 합산 vs 셋트 가격 (절약액 계산) --%>
<c:set var="sumOfParts" value="${0}"/>
<c:forEach var="p" items="${bundle.plugins}">
  <c:set var="sumOfParts" value="${sumOfParts + p.price}"/>
</c:forEach>
<c:set var="savings" value="${sumOfParts - bundle.price}"/>
<c:set var="savingsPct" value="${sumOfParts > 0 ? (savings * 100 / sumOfParts) : 0}"/>

<%-- 셋트 안에서의 plugin 역할 (slug:title -> 한 단어 역할). DB 컬럼 없이 정적 매핑. --%>
<%
    Map<String,String> roles = new HashMap<>();
    // blog-automation
    roles.put("blog-automation:superpowers",  "글감 정리");
    roles.put("blog-automation:copywriting",  "카피 생성");
    roles.put("blog-automation:stop-slop",    "AI 티 정리");
    // code-quality
    roles.put("code-quality:karpathy-guidelines", "실수 검토");
    roles.put("code-quality:simplify",            "코드 정리");
    roles.put("code-quality:claude-api",          "통합 점검");
    // design-ready
    roles.put("design-ready:frontend-design", "UI 기반");
    roles.put("design-ready:ogilvy",          "헤드라인");
    roles.put("design-ready:copywriting",     "본문 카피");
    // ad-copy
    roles.put("ad-copy:ogilvy",      "헤드라인 5~8안");
    roles.put("ad-copy:copywriting", "본문·CTA");
    roles.put("ad-copy:stop-slop",   "클리셰 제거");
    // ai-app-launch
    roles.put("ai-app-launch:claude-api",      "API 백엔드");
    roles.put("ai-app-launch:frontend-design", "UI·스트리밍");
    roles.put("ai-app-launch:simplify",        "출시 직전 정리");
    // mvp-bootstrap
    roles.put("mvp-bootstrap:superpowers",         "범위 결정");
    roles.put("mvp-bootstrap:frontend-design",     "UI 시스템");
    roles.put("mvp-bootstrap:copywriting",         "카피 통합");
    roles.put("mvp-bootstrap:karpathy-guidelines", "PR 검토");
    request.setAttribute("pluginRoles", roles);
%>

<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="bundle-hero">
  <div class="bundle-hero-inner">
    <div class="chip-row">
      <span class="chip-meta">📦 큐레이션 셋트</span>
      <span class="chip-meta">⚡ 즉시 사용 가능</span>
      <c:if test="${not empty bundle.plugins}">
        <span class="chip-meta">🧩 플러그인 ${bundle.plugins.size()}개</span>
      </c:if>
    </div>

    <h1 class="bundle-title"><c:out value="${bundle.name}"/></h1>
    <p class="bundle-tagline"><c:out value="${bundle.tagline}"/></p>

    <p class="bundle-meta">
      <span>👁 <c:out value="${bundle.viewCount}"/> 조회</span>
    </p>

    <div class="price-card">
      <div class="price-card-left">
        <c:choose>
          <c:when test="${bundle.price == 0}">
            <div class="price-big price-free">FREE<span class="won">· 오픈소스</span></div>
            <div class="price-compare">
              <span class="hint">셋트로 묶인 모든 스킬을 클로드 코드에 바로 설치할 수 있습니다.</span>
            </div>
          </c:when>
          <c:otherwise>
            <div class="price-big"><fmt:formatNumber value="${bundle.price}" type="number"/><span class="won">원</span></div>
            <c:if test="${savings > 0}">
              <div class="price-compare">
                <span class="price-strike"><fmt:formatNumber value="${sumOfParts}" type="number"/>원 단품 합산</span>
                <span class="savings-badge">✓ <fmt:formatNumber value="${savings}" type="number"/>원 절약 (<fmt:formatNumber value="${savingsPct}" maxFractionDigits="0"/>%)</span>
              </div>
            </c:if>
          </c:otherwise>
        </c:choose>
      </div>
      <div class="price-card-right">
        <a class="btn btn-cta" href="${ctx}/app/demo/show?slug=${bundle.slug}#result">
          🎁 셋트로 만든 데모 →
        </a>
        <a class="btn-ghost btn-cta-sub" href="${ctx}/app/demo/show?slug=${bundle.slug}#history">
          📜 작업 히스토리
        </a>
      </div>
    </div>
  </div>
</section>

<c:if test="${not empty bundle.plugins}">
<section class="bundle-flow" aria-label="셋트 흐름">
  <div class="flow-strip">
    <c:forEach var="p" items="${bundle.plugins}" varStatus="loop">
      <c:set var="roleKey" value="${bundle.slug}:${p.title}"/>
      <c:set var="roleText" value="${pluginRoles[roleKey]}"/>
      <div class="flow-node">
        <div class="flow-num">${loop.index + 1}</div>
        <div class="flow-plugin"><c:out value="${p.title}"/></div>
        <div class="flow-role"><c:out value="${empty roleText ? p.summary : roleText}"/></div>
      </div>
      <c:if test="${!loop.last}">
        <div class="flow-arrow" aria-hidden="true">→</div>
      </c:if>
    </c:forEach>
  </div>
  <div class="flow-outcome">
    <span class="flow-outcome-label">결과</span>
    <span class="flow-outcome-text"><c:out value="${bundle.tagline}"/></span>
  </div>
</section>
</c:if>

<section class="bundle-steps">
  <header class="section-header">
    <span class="section-label">/ steps</span>
    <h2>각 단계 자세히</h2>
    <p>플러그인별 정확한 역할과 다음 단계로 전달되는 결과물입니다.</p>
  </header>

  <c:choose>
    <c:when test="${empty bundle.plugins}">
      <p class="empty">아직 매핑된 플러그인이 없습니다.</p>
    </c:when>
    <c:otherwise>
      <ol class="step-row">
        <c:forEach var="p" items="${bundle.plugins}" varStatus="loop">
          <c:set var="roleKey" value="${bundle.slug}:${p.title}"/>
          <c:set var="roleText" value="${pluginRoles[roleKey]}"/>
          <li class="step-card">
            <div class="step-card-header">
              <span class="step-number">${loop.index + 1}단계</span>
              <h3 class="step-plugin-name">
                <c:out value="${p.title}"/>
                <c:if test="${not empty roleText}">
                  <span class="step-role-tag"><c:out value="${roleText}"/></span>
                </c:if>
              </h3>
            </div>
            <p class="step-plugin-summary"><c:out value="${p.summary}"/></p>
            <div class="step-meta">
              <c:choose>
                <c:when test="${p.price == 0}">
                  <span class="step-price step-price-free">FREE</span>
                </c:when>
                <c:otherwise>
                  <span class="step-price"><fmt:formatNumber value="${p.price}" type="number"/>원</span>
                </c:otherwise>
              </c:choose>
              <a class="step-link" href="${ctx}/app/plugin/detail?id=${p.id}">상세 보기 →</a>
            </div>
          </li>
        </c:forEach>
      </ol>
    </c:otherwise>
  </c:choose>
</section>

<c:if test="${not empty bundle.story}">
  <section class="bundle-story">
    <div class="story-body markdown-body">${storyHtml}</div>
  </section>
</c:if>

<%-- 관련 셋트 (slug 기반 정적 추천) --%>
<%
    local.promptmark.dto.Bundle _b = (local.promptmark.dto.Bundle) request.getAttribute("bundle");
    java.util.Map<String, String[][]> rel = new java.util.HashMap<>();
    rel.put("blog-automation",  new String[][] {{"ad-copy","광고 카피","마케팅 헤드라인까지"},{"design-ready","디자인 마감","랜딩 페이지가 필요하면"},{"mvp-bootstrap","MVP 부트스트랩","제품 자체를 만든다면"}});
    rel.put("code-quality",     new String[][] {{"ai-app-launch","Claude API 앱","Claude API 쓰는 앱이라면"},{"mvp-bootstrap","MVP 부트스트랩","코드 + UI + 카피 한 번에"},{"design-ready","디자인 마감","UI까지 한 번에"}});
    rel.put("design-ready",     new String[][] {{"ad-copy","광고 카피","랜딩 + 광고 카피"},{"blog-automation","블로그 자동화","블로그까지 같은 결로"},{"mvp-bootstrap","MVP 부트스트랩","UI + 제품 출시"}});
    rel.put("ad-copy",          new String[][] {{"blog-automation","블로그 자동화","블로그 콘텐츠로 확장"},{"design-ready","디자인 마감","랜딩까지 셋트로"},{"mvp-bootstrap","MVP 부트스트랩","제품부터 광고까지"}});
    rel.put("ai-app-launch",    new String[][] {{"code-quality","코드 품질","API 코드 리뷰"},{"mvp-bootstrap","MVP 부트스트랩","제품으로 마감"},{"design-ready","디자인 마감","UI를 더 깊이"}});
    rel.put("mvp-bootstrap",    new String[][] {{"ai-app-launch","Claude API 앱","AI 기능 더 깊이"},{"design-ready","디자인 마감","디자인을 더 깊이"},{"code-quality","코드 품질","코드 품질 가드"}});
    if (_b != null) request.setAttribute("related", rel.get(_b.getSlug()));
%>
<c:if test="${not empty related}">
<section class="bundle-related">
  <h2>이 셋트가 마음에 들면 →</h2>
  <p>같은 사람을 다른 도메인으로 데려가는 셋트들</p>
  <div class="related-grid">
    <c:forEach var="r" items="${related}">
      <a class="related-card" href="${ctx}/app/bundle/list">
        <div class="related-tag">/${r[0]}</div>
        <h3>${r[1]}</h3>
        <p>${r[2]}</p>
      </a>
    </c:forEach>
  </div>
</section>
</c:if>

<footer class="bundle-footer-meta">
  <span>큐레이션: 관리자</span>
  <c:if test="${not empty sessionScope.LOGIN_USER && sessionScope.LOGIN_USER.role == 'ADMIN'}">
    <form method="post" action="${ctx}/app/bundle/delete" onsubmit="return confirm('이 셋트를 삭제하시겠습니까?');" class="inline-form">
      <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>
      <input type="hidden" name="id" value="${bundle.id}">
      <button type="submit" class="btn-danger">셋트 삭제</button>
    </form>
  </c:if>
</footer>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
