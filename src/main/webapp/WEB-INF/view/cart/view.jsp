<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="장바구니 — promptmark"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="cart-view">
  <h1>장바구니</h1>

  <c:choose>
    <c:when test="${empty cart}">
      <p class="empty">장바구니가 비어 있습니다.</p>
      <p><a class="button" href="${ctx}/app/plugin/list">자산 둘러보기</a></p>
    </c:when>
    <c:otherwise>
      <table class="cart-table">
        <thead>
          <tr><th>제목</th><th>종류</th><th>가격</th><th></th></tr>
        </thead>
        <tbody>
          <c:forEach var="item" items="${cart}">
            <tr>
              <td>
                <a href="${ctx}/app/plugin/detail?id=${item.pluginId}">
                  <c:out value="${item.title}"/>
                </a>
              </td>
              <td><c:out value="${item.typeName}"/></td>
              <td>
                <c:choose>
                  <c:when test="${item.price == 0}">무료</c:when>
                  <c:otherwise><c:out value="${item.price}"/>원</c:otherwise>
                </c:choose>
              </td>
              <td>
                <form method="post" action="${ctx}/app/cart/remove?pluginId=${item.pluginId}" class="inline">
                  <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>
                  <button type="submit" class="link">제거</button>
                </form>
              </td>
            </tr>
          </c:forEach>
        </tbody>
        <tfoot>
          <tr>
            <td colspan="2">총계</td>
            <td colspan="2"><strong><c:out value="${total}"/>원</strong></td>
          </tr>
        </tfoot>
      </table>

      <p class="action-row">
        <a class="button" href="${ctx}/app/order/checkout">결제하기</a>
      </p>
    </c:otherwise>
  </c:choose>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
