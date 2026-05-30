<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="bundle.messages"/>
<c:set var="pageTitle" value="401 — promptmark"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="error-page">
  <p class="error-code">401</p>
  <h1>인증이 필요합니다</h1>
  <p class="error-detail">
    <c:out value="${errorMessage}" default="로그인이 필요한 페이지입니다."/>
  </p>
  <div class="error-actions">
    <c:if test="${empty sessionScope.LOGIN_USER}">
      <a class="btn" href="${ctx}/app/auth/login">
        <fmt:message key="error.back_login"/>
      </a>
    </c:if>
    <a class="btn btn-secondary" href="${ctx}/">
      <fmt:message key="error.back_home"/>
    </a>
  </div>
  <p class="trace">
    <fmt:message key="error.trace_id"/>
    <code><%= request.getAttribute("traceId") %></code>
  </p>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
