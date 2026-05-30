<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="결제 — promptmark"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="checkout">
  <h1>결제 (목업)</h1>

  <c:if test="${not empty errorMessage}">
    <p class="form-error"><c:out value="${errorMessage}"/></p>
  </c:if>

  <c:choose>
    <c:when test="${empty cart}">
      <p class="empty">결제할 자산이 없습니다.</p>
      <p><a class="button" href="${ctx}/app/cart/view">장바구니로 돌아가기</a></p>
    </c:when>
    <c:otherwise>
      <ul class="checkout-items">
        <c:forEach var="item" items="${cart}">
          <li>
            <span><c:out value="${item.title}"/></span>
            <span>
              <c:choose>
                <c:when test="${item.price == 0}">무료</c:when>
                <c:otherwise><c:out value="${item.price}"/>원</c:otherwise>
              </c:choose>
            </span>
          </li>
        </c:forEach>
      </ul>
      <p class="total">총계: <strong><c:out value="${total}"/>원</strong></p>

      <form method="post" action="${ctx}/app/order/checkout">
        <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>
        <button type="submit">결제 확정</button>
        <a class="button-link" href="${ctx}/app/cart/view">취소</a>
      </form>
    </c:otherwise>
  </c:choose>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
