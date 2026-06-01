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
        <c:choose>
          <c:when test="${bundle.price == 0}">
            <a class="btn btn-cta" href="https://github.com/obra/superpowers" target="_blank" rel="noopener">↗ GitHub에서 설치</a>
          </c:when>
          <c:when test="${not empty sessionScope.LOGIN_USER}">
            <a class="btn btn-cta" href="${ctx}/app/cart/view">▶ 구매하기</a>
          </c:when>
          <c:otherwise>
            <a class="btn btn-cta" href="${ctx}/app/auth/login">로그인하고 구매</a>
          </c:otherwise>
        </c:choose>
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
    <header class="section-header">
      <span class="section-label">/ story</span>
      <h2>왜 이 조합인가</h2>
    </header>
    <div class="story-body"><c:out value="${bundle.story}"/></div>
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
