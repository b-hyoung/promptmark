<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:set var="pageTitle" value="주문 내역 — promptmark"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="order-history">
  <h1>주문 내역</h1>

  <c:choose>
    <c:when test="${empty orders}">
      <p class="empty">아직 주문 내역이 없습니다.</p>
    </c:when>
    <c:otherwise>
      <table class="orders-table">
        <thead>
          <tr><th>주문 번호</th><th>일시</th><th>금액</th><th>상태</th><th></th></tr>
        </thead>
        <tbody>
          <c:forEach var="o" items="${orders}">
            <tr>
              <td>#<c:out value="${o.id}"/></td>
              <td><c:out value="${o.createdAt}"/></td>
              <td><c:out value="${o.totalAmount}"/>원</td>
              <td><c:out value="${o.statusName}"/></td>
              <td>
                <a href="${ctx}/app/order/complete?orderId=${o.id}">상세</a>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
