<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="bundle.messages"/>
<c:set var="pageTitle"><fmt:message key="admin.reports_title"/> — promptmark</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="admin-reports">
  <h1><fmt:message key="admin.reports_title"/></h1>

  <c:if test="${param.msg == 'resolved_ok'}">
    <p class="form-notice">신고가 처리되었습니다.</p>
  </c:if>
  <c:if test="${param.msg == 'banned_ok'}">
    <p class="form-notice">사용자가 정지되었습니다.</p>
  </c:if>

  <c:choose>
    <c:when test="${empty reports}">
      <p class="empty">대기 중인 신고가 없습니다.</p>
    </c:when>
    <c:otherwise>
      <table class="list-table">
        <thead>
          <tr>
            <th>#</th>
            <th>자산</th>
            <th>종류 / 상태</th>
            <th>사유</th>
            <th>신고자</th>
            <th>일시</th>
            <th>처리</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="r" items="${reports}">
            <tr>
              <td><c:out value="${r.reportId}"/></td>
              <td>
                <a href="${ctx}/app/asset/detail?id=${r.assetId}">
                  <c:out value="${r.assetTitle}"/>
                </a>
              </td>
              <td>
                <c:out value="${r.assetTypeName}"/> / <c:out value="${r.assetStatusName}"/>
              </td>
              <td><c:out value="${r.reason}"/></td>
              <td>
                <c:out value="${r.reporterNickname}"/>
                <form method="post" action="${ctx}/app/admin/user/ban" class="inline"
                      onsubmit="return confirm('이 사용자를 정지하시겠습니까?');">
                  <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>
                  <input type="hidden" name="userId" value="${r.reporterId}">
                  <button type="submit" class="btn-danger">
                    <fmt:message key="admin.ban_user"/>
                  </button>
                </form>
              </td>
              <td><c:out value="${r.reportedAt}"/></td>
              <td>
                <form method="post" action="${ctx}/app/admin/report/resolve" class="inline">
                  <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>
                  <input type="hidden" name="id" value="${r.reportId}">
                  <button type="submit" name="action" value="HIDE">
                    <fmt:message key="admin.action_hide"/>
                  </button>
                  <button type="submit" name="action" value="DELETE" class="btn-danger"
                          onclick="return confirm('이 자산을 삭제하시겠습니까?');">
                    <fmt:message key="admin.action_delete"/>
                  </button>
                  <button type="submit" name="action" value="REJECT">
                    <fmt:message key="admin.action_reject"/>
                  </button>
                </form>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
