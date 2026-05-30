<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="bundle.messages"/>
<c:set var="pageTitle" value="400 — promptmark"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="error-page">
  <p class="error-code">400</p>
  <h1>잘못된 요청</h1>
  <p class="error-detail">
    <c:out value="${errorMessage}" default="요청 형식이 올바르지 않습니다."/>
  </p>
  <div class="error-actions">
    <a class="btn" href="${ctx}/"><fmt:message key="error.back_home"/></a>
    <a class="btn btn-secondary" href="#" onclick="history.back(); return false;">
      <fmt:message key="error.back_prev"/>
    </a>
  </div>
  <p class="trace">
    <fmt:message key="error.trace_id"/>
    <code><%= request.getAttribute("traceId") %></code>
  </p>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
