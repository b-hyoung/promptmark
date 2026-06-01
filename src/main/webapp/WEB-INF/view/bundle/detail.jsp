<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="bundle.messages"/>
<c:set var="pageTitle"><c:out value='${bundle.name}'/> — promptmark</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<article class="bundle-detail plugin-detail">
  <header>
    <h1><c:out value="${bundle.name}"/></h1>
    <p class="lead"><c:out value="${bundle.tagline}"/></p>
    <p class="meta">
      <span class="price"><fmt:formatNumber value="${bundle.price}" type="number"/>원</span>
      &middot; 조회 <c:out value="${bundle.viewCount}"/>
    </p>
  </header>

  <section>
    <h2>이 셋트에 담긴 플러그인</h2>
    <c:choose>
      <c:when test="${empty bundle.plugins}">
        <p class="empty">아직 플러그인이 매핑되지 않았습니다.</p>
      </c:when>
      <c:otherwise>
        <ul class="plugin-list-inline">
          <c:forEach var="p" items="${bundle.plugins}">
            <li>
              <a href="${ctx}/app/plugin/detail?id=${p.id}">
                <strong><c:out value="${p.title}"/></strong>
              </a>
              <span class="muted"> &mdash; <c:out value="${p.summary}"/></span>
            </li>
          </c:forEach>
        </ul>
      </c:otherwise>
    </c:choose>
  </section>

  <c:if test="${not empty bundle.story}">
    <section>
      <h2>왜 이 조합인가</h2>
      <div class="story" style="white-space: pre-line; line-height: 1.6;"><c:out value="${bundle.story}"/></div>
    </section>
  </c:if>

  <c:if test="${not empty sessionScope.LOGIN_USER && sessionScope.LOGIN_USER.role == 'ADMIN'}">
    <form method="post" action="${ctx}/app/bundle/delete" onsubmit="return confirm('이 셋트를 삭제하시겠습니까?');">
      <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>
      <input type="hidden" name="id" value="${bundle.id}">
      <button type="submit" style="background:#d33; color:white; padding:6px 14px; border:none; border-radius:4px;">셋트 삭제</button>
    </form>
  </c:if>
</article>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
