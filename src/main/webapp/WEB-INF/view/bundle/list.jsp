<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<fmt:setBundle basename="bundle.messages"/>
<c:set var="pageTitle">큐레이션 셋트 — promptmark</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="bundle-list">
  <h1>큐레이션 셋트</h1>

  <p class="search-meta">총 <c:out value="${total}"/>건</p>

  <c:if test="${not empty sessionScope.LOGIN_USER && sessionScope.LOGIN_USER.role == 'ADMIN'}">
    <p><a class="btn-primary" href="${ctx}/app/bundle/new">+ 새 셋트 만들기</a></p>
  </c:if>

  <c:choose>
    <c:when test="${empty bundles}">
      <p class="empty">아직 등록된 셋트가 없습니다.</p>
    </c:when>
    <c:otherwise>
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
    </c:otherwise>
  </c:choose>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
