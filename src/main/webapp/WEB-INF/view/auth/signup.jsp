<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="회원가입 — promptmark"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="auth-form">
  <h1>회원가입</h1>

  <c:if test="${not empty errorMessage}">
    <p class="form-error"><c:out value="${errorMessage}"/></p>
  </c:if>

  <form method="post" action="${pageContext.request.contextPath}/app/auth/signup" novalidate>
    <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>

    <div class="field">
      <label for="email">이메일</label>
      <input type="email" id="email" name="email" maxlength="120" required
             value="<c:out value='${form.email}'/>">
      <c:if test="${not empty errors.email}">
        <small class="field-error"><c:out value="${errors.email}"/></small>
      </c:if>
    </div>

    <div class="field">
      <label for="password">비밀번호</label>
      <input type="password" id="password" name="password" minlength="8" maxlength="64" required>
      <c:if test="${not empty errors.password}">
        <small class="field-error"><c:out value="${errors.password}"/></small>
      </c:if>
    </div>

    <div class="field">
      <label for="password_confirm">비밀번호 확인</label>
      <input type="password" id="password_confirm" name="password_confirm" required>
      <c:if test="${not empty errors.password_confirm}">
        <small class="field-error"><c:out value="${errors.password_confirm}"/></small>
      </c:if>
    </div>

    <div class="field">
      <label for="nickname">닉네임</label>
      <input type="text" id="nickname" name="nickname" minlength="2" maxlength="20" required
             value="<c:out value='${form.nickname}'/>">
      <c:if test="${not empty errors.nickname}">
        <small class="field-error"><c:out value="${errors.nickname}"/></small>
      </c:if>
    </div>

    <div class="field">
      <label for="role">역할</label>
      <select id="role" name="role">
        <option value="USER"   <c:if test="${form.role == 'USER' or empty form.role}">selected</c:if>>구매자 (USER)</option>
        <option value="SELLER" <c:if test="${form.role == 'SELLER'}">selected</c:if>>판매자 (SELLER)</option>
      </select>
      <c:if test="${not empty errors.role}">
        <small class="field-error"><c:out value="${errors.role}"/></small>
      </c:if>
    </div>

    <button type="submit">가입하기</button>
  </form>

  <p><a href="${pageContext.request.contextPath}/app/auth/login">이미 계정이 있으신가요? 로그인</a></p>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
