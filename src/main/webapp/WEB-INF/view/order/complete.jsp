<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="결제 완료 — promptmark"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="order-complete">
  <h1>결제가 완료되었습니다</h1>

  <p>주문 번호: <strong>#<c:out value="${order.id}"/></strong></p>
  <p>총 결제: <strong><c:out value="${order.totalAmount}"/>원</strong></p>

  <ul class="order-items">
    <c:forEach var="it" items="${items}">
      <li>
        <a href="${ctx}/app/asset/detail?id=${it.assetId}">
          <c:out value="${it.title}"/>
        </a>
        — <c:out value="${it.pricePaid}"/>원
        &middot;
        <a href="${ctx}/app/asset/download?id=${it.assetId}">다운로드</a>
      </li>
    </c:forEach>
  </ul>

  <p class="action-row">
    <a class="button" href="${ctx}/app/order/history">주문 내역 보기</a>
    <a class="button-link" href="${ctx}/app/asset/list">자산 둘러보기</a>
  </p>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
