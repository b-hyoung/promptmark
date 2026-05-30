<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="me" value="${sessionScope.LOGIN_USER}"/>
<nav class="topbar">
  <a class="brand" href="${ctx}/">promptmark</a>
  <ul class="nav-links">
    <li><a href="${ctx}/app/asset/list">자산 목록</a></li>
    <c:if test="${empty me}">
      <li><a href="${ctx}/app/auth/login">로그인</a></li>
      <li><a href="${ctx}/app/auth/signup">회원가입</a></li>
    </c:if>
    <c:if test="${not empty me}">
      <c:if test="${me.roleName == 'SELLER' or me.roleName == 'ADMIN'}">
        <li><a href="${ctx}/app/asset/new">자산 등록</a></li>
      </c:if>
      <c:if test="${me.roleName == 'ADMIN'}">
        <li><a href="${ctx}/app/admin/reports">신고 관리</a></li>
      </c:if>
      <li><a href="${ctx}/app/mypage">마이페이지</a></li>
      <li class="user">
        <span><c:out value="${me.nickname}"/></span>
        <form method="post" action="${ctx}/app/auth/logout" class="inline">
          <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>
          <button type="submit">로그아웃</button>
        </form>
      </li>
    </c:if>
  </ul>
</nav>
