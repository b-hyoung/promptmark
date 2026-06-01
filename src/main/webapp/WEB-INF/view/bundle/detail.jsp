<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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
        <div class="price-big"><fmt:formatNumber value="${bundle.price}" type="number"/><span class="won">원</span></div>
        <c:if test="${savings > 0}">
          <div class="price-compare">
            <span class="price-strike"><fmt:formatNumber value="${sumOfParts}" type="number"/>원 단품 합산</span>
            <span class="savings-badge">✓ <fmt:formatNumber value="${savings}" type="number"/>원 절약 (<fmt:formatNumber value="${savingsPct}" maxFractionDigits="0"/>%)</span>
          </div>
        </c:if>
      </div>
      <div class="price-card-right">
        <c:choose>
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

<section class="bundle-steps">
  <header class="section-header">
    <span class="section-label">/ steps</span>
    <h2>이 셋트가 어떻게 동작하나요?</h2>
    <p>각 플러그인이 단계별로 맡는 역할입니다.</p>
  </header>

  <c:choose>
    <c:when test="${empty bundle.plugins}">
      <p class="empty">아직 매핑된 플러그인이 없습니다.</p>
    </c:when>
    <c:otherwise>
      <ol class="step-row">
        <c:forEach var="p" items="${bundle.plugins}" varStatus="loop">
          <li class="step-card">
            <div class="step-number">Step ${loop.index + 1}</div>
            <h3 class="step-plugin-name"><c:out value="${p.title}"/></h3>
            <p class="step-plugin-summary"><c:out value="${p.summary}"/></p>
            <div class="step-meta">
              <span class="step-price"><fmt:formatNumber value="${p.price}" type="number"/>원</span>
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
