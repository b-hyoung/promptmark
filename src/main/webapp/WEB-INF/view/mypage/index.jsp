<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="bundle.messages"/>
<c:set var="pageTitle"><fmt:message key="mypage.title"/> — promptmark</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="mypage">
  <h1><fmt:message key="mypage.title"/></h1>

  <p class="mypage-greet">
    <c:out value="${loginUser.nickname}"/>
    <small>(<c:out value="${loginUser.email}"/> · <c:out value="${loginUser.roleName}"/>)</small>
  </p>

  <ul class="mypage-summary">
    <li><strong><c:out value="${fn:length(myAssets)}" default="0"/></strong>
        <span><fmt:message key="mypage.my_assets"/></span></li>
    <li><strong><c:out value="${fn:length(myOrders)}" default="0"/></strong>
        <span><fmt:message key="mypage.my_orders"/></span></li>
    <li><strong><c:out value="${fn:length(recentDownloads)}" default="0"/></strong>
        <span><fmt:message key="mypage.recent_downloads"/></span></li>
  </ul>

  <%-- ───── My Assets ───── --%>
  <div class="card">
    <h2><fmt:message key="mypage.my_assets"/></h2>
    <c:choose>
      <c:when test="${empty myAssets}">
        <p class="empty">
          아직 등록한 자산이 없습니다 —
          <a href="${ctx}/app/asset/new">자산 등록하기</a>
        </p>
      </c:when>
      <c:otherwise>
        <table class="list-table">
          <thead>
            <tr><th>제목</th><th>종류</th><th>상태</th><th>가격</th><th>조회/다운로드</th><th></th></tr>
          </thead>
          <tbody>
            <c:forEach var="a" items="${myAssets}">
              <tr>
                <td><a href="${ctx}/app/asset/detail?id=${a.id}"><c:out value="${a.title}"/></a></td>
                <td><c:out value="${a.typeName}"/></td>
                <td><c:out value="${a.statusName}"/></td>
                <td>
                  <c:choose>
                    <c:when test="${a.price == 0}">무료</c:when>
                    <c:otherwise><c:out value="${a.price}"/>원</c:otherwise>
                  </c:choose>
                </td>
                <td><c:out value="${a.viewCount}"/> / <c:out value="${a.downloadCount}"/></td>
                <td><a href="${ctx}/app/asset/edit?id=${a.id}">편집</a></td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </c:otherwise>
    </c:choose>
  </div>

  <%-- ───── My Orders ───── --%>
  <div class="card">
    <h2><fmt:message key="mypage.my_orders"/></h2>
    <c:choose>
      <c:when test="${empty myOrders}">
        <p class="empty">
          아직 구매한 자산이 없습니다 —
          <a href="${ctx}/app/asset/list">자산 둘러보기</a>
        </p>
      </c:when>
      <c:otherwise>
        <table class="list-table">
          <thead>
            <tr><th>주문 번호</th><th>일시</th><th>구성</th><th>금액</th><th>상태</th><th></th></tr>
          </thead>
          <tbody>
            <c:forEach var="o" items="${myOrders}">
              <tr>
                <td>#<c:out value="${o.id}"/></td>
                <td><c:out value="${o.createdAt}"/></td>
                <td>
                  <c:set var="items" value="${orderItems[o.id]}"/>
                  <c:choose>
                    <c:when test="${empty items}">—</c:when>
                    <c:otherwise>
                      <c:forEach var="it" items="${items}" varStatus="vs">
                        <c:out value="${it.title}"/><c:if test="${not vs.last}">, </c:if>
                      </c:forEach>
                    </c:otherwise>
                  </c:choose>
                </td>
                <td><c:out value="${o.totalAmount}"/>원</td>
                <td><c:out value="${o.statusName}"/></td>
                <td><a href="${ctx}/app/order/complete?orderId=${o.id}">상세</a></td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </c:otherwise>
    </c:choose>
  </div>

  <%-- ───── Recent Downloads ───── --%>
  <div class="card">
    <h2><fmt:message key="mypage.recent_downloads"/></h2>
    <c:choose>
      <c:when test="${empty recentDownloads}">
        <p class="empty">최근 다운로드 내역이 없습니다.</p>
      </c:when>
      <c:otherwise>
        <ul class="dl-list">
          <c:forEach var="d" items="${recentDownloads}">
            <li>
              <a href="${ctx}/app/asset/detail?id=${d.id}"><c:out value="${d.title}"/></a>
              <small>· <c:out value="${d.typeName}"/></small>
            </li>
          </c:forEach>
        </ul>
      </c:otherwise>
    </c:choose>
  </div>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
