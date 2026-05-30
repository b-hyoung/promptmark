<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="403 — promptmark"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>
<section class="error-page">
  <h1>403 — 접근 권한이 없습니다</h1>
  <p><c:out value="${errorMessage}" default="이 작업을 수행할 권한이 없습니다."/></p>
  <p class="trace">trace: <code><%= request.getAttribute("traceId") %></code></p>
</section>
<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
