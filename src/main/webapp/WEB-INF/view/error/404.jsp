<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="404 — promptmark"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>
<section class="error-page">
  <h1>404 — 페이지를 찾을 수 없습니다</h1>
  <p><c:out value="${errorMessage}" default="요청하신 경로를 찾을 수 없습니다."/></p>
  <p class="trace">trace: <code><%= request.getAttribute("traceId") %></code></p>
</section>
<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
