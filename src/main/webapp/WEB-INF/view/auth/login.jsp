<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="bundle.messages"/>
<c:set var="pageTitle"><fmt:message key="nav.login"/> — promptmark</c:set>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="auth-form">
  <h1><fmt:message key="nav.login"/></h1>

  <c:if test="${not empty notice}">
    <p class="form-notice"><c:out value="${notice}"/></p>
  </c:if>
  <c:if test="${not empty errorMessage}">
    <p class="form-error"><c:out value="${errorMessage}"/></p>
  </c:if>
  <c:if test="${not empty param.msg and param.msg != 'signup_ok' and empty notice}">
    <p class="form-notice"><c:out value="${param.msg}"/></p>
  </c:if>

  <form method="post" action="${pageContext.request.contextPath}/app/auth/login" novalidate>
    <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>
    <input type="hidden" name="next" value="<c:out value='${next}'/>">

    <div class="field">
      <label for="email"><fmt:message key="auth.email"/></label>
      <input type="email" id="email" name="email" maxlength="120" required
             value="<c:out value='${emailEcho}'/>">
    </div>

    <div class="field">
      <label for="password"><fmt:message key="auth.password"/></label>
      <input type="password" id="password" name="password" required>
    </div>

    <button type="submit"><fmt:message key="auth.login_submit"/></button>
  </form>

  <p><a href="${pageContext.request.contextPath}/app/auth/signup">계정이 없으신가요? <fmt:message key="nav.signup"/></a></p>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
