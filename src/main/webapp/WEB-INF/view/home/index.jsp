<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:set var="pageTitle">promptmark — Claude AI 스킬 셋트 큐레이션 마켓</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="home-hero">
  <span class="eyebrow">CURATED · ${bundleCount}개 셋트</span>
  <h1>플러그인 하나가 아니라,<br><span style="color:var(--accent);">검증된 조합</span>으로 시작하세요.</h1>
  <p class="lead">Claude AI 스킬을 한 사람이 직접 골라 묶고, 그 셋트로 직접 만든 결과물까지 공개합니다. 단품 합산보다 항상 저렴합니다.</p>
  <div class="cta-row">
    <a class="btn" href="${ctx}/app/bundle/list">🎁 셋트 둘러보기</a>
    <a class="btn-ghost" href="${ctx}/app/info/about">큐레이션 철학 →</a>
  </div>
</section>

<section class="home-section">
  <h2>이렇게 작동합니다</h2>
  <p class="sub">셋트는 단순한 묶음이 아닙니다. 각 셋트마다 큐레이터가 직접 그 셋트로 만든 데모와 히스토리를 함께 공개합니다.</p>
  <div class="how-grid">
    <div class="how-cell">
      <div class="num">1</div>
      <h3>셋트를 둘러보세요</h3>
      <p>도메인별 (글쓰기·코딩·디자인·광고·앱 출시·MVP)로 큐레이션된 6개 셋트.</p>
    </div>
    <div class="how-cell">
      <div class="num">2</div>
      <h3>실제 결과물을 확인</h3>
      <p>각 셋트마다 "이 셋트로 만든 데모" 페이지. 텍스트 약속이 아니라 진짜 산출물.</p>
    </div>
    <div class="how-cell">
      <div class="num">3</div>
      <h3>작업 히스토리까지 공개</h3>
      <p>어떤 스킬이 언제 어떤 결과를 만들었는지 분 단위 로그.</p>
    </div>
  </div>
</section>

<section class="home-section">
  <h2>큐레이션된 셋트 (${bundleCount})</h2>
  <p class="sub">단품 합산보다 평균 22% 저렴. 각각 다른 도메인.</p>
  <ul class="bundle-grid plugin-grid">
    <c:forEach var="b" items="${bundles}">
      <li class="plugin-card">
        <a href="${ctx}/app/bundle/detail?id=${b.id}">
          <c:choose>
            <c:when test="${fn:startsWith(b.thumbnail, 'mock:')}">
              <%@ include file="/WEB-INF/view/bundle/_mock_thumb.jspf" %>
            </c:when>
            <c:when test="${not empty b.thumbnail}">
              <img src="<c:out value='${b.thumbnail}'/>" alt="" style="width:100%; aspect-ratio:4/3; object-fit:cover; border-radius:6px;">
            </c:when>
          </c:choose>
          <h2 style="margin-top:14px;"><c:out value="${b.name}"/></h2>
          <p class="summary"><c:out value="${b.tagline}"/></p>
          <p class="price"><fmt:formatNumber value="${b.price}" type="number"/>원</p>
        </a>
      </li>
    </c:forEach>
  </ul>
</section>

<section class="home-trust">
  <div class="trust-cell"><strong>${bundleCount}</strong><span>큐레이션 셋트</span></div>
  <div class="trust-cell"><strong>8</strong><span>개별 스킬</span></div>
  <div class="trust-cell"><strong>22%</strong><span>평균 절약</span></div>
  <div class="trust-cell"><strong>0건</strong><span>외부 의존 (모두 오픈 스킬)</span></div>
</section>

<section class="home-cta">
  <h2>한 셋트로 어떤 결과가 나오는지 직접 보세요.</h2>
  <p>가장 인기 셋트 — 블로그 자동화. 28분 만에 발행 직전 글이 완성됩니다.</p>
  <a class="btn" href="${ctx}/app/demo/show?slug=blog-automation">🎁 블로그 자동화 데모 보기 →</a>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
