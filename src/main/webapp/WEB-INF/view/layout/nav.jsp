<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="bundle.messages"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="me" value="${sessionScope.LOGIN_USER}"/>
<nav class="topbar">
  <a class="brand" href="${ctx}/">promptmark</a>
  <ul class="nav-links">
    <li><a href="${ctx}/app/plugin/list"><fmt:message key="nav.plugins"/></a></li>
    <li><a href="${ctx}/app/bundle/list">셋트</a></li>
    <li><a href="${ctx}/app/chat"><fmt:message key="nav.chat"/></a></li>
    <c:if test="${empty me}">
      <li><a href="${ctx}/app/auth/login"><fmt:message key="nav.login"/></a></li>
      <li><a href="${ctx}/app/auth/signup"><fmt:message key="nav.signup"/></a></li>
    </c:if>
    <c:if test="${not empty me}">
      <c:if test="${me.roleName == 'SELLER' or me.roleName == 'ADMIN'}">
        <li><a href="${ctx}/app/plugin/new"><fmt:message key="nav.new_plugin"/></a></li>
      </c:if>
      <c:if test="${me.roleName == 'ADMIN'}">
        <li><a href="${ctx}/app/admin/reports"><fmt:message key="nav.admin_reports"/></a></li>
      </c:if>
      <li><a href="${ctx}/app/mypage"><fmt:message key="nav.mypage"/></a></li>
      <li class="user">
        <span><c:out value="${me.nickname}"/></span>
        <form method="post" action="${ctx}/app/auth/logout" class="inline">
          <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>
          <button type="submit"><fmt:message key="nav.logout"/></button>
        </form>
      </li>
    </c:if>
  </ul>
</nav>
