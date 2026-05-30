<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="401 — promptmark"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>
<section class="error-page">
  <h1>401 — 인증이 필요합니다</h1>
  <p><c:out value="${errorMessage}" default="로그인이 필요한 페이지입니다."/></p>
  <p class="trace">trace: <code><%= request.getAttribute("traceId") %></code></p>
</section>
<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
