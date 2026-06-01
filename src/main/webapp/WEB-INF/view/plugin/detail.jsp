<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:set var="pageTitle"><c:out value='${plugin.title}'/> — promptmark</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="me" value="${sessionScope.LOGIN_USER}"/>
<c:set var="isOwner" value="${not empty me and me.id == plugin.sellerId}"/>
<c:set var="isAdmin" value="${not empty me and me.roleName == 'ADMIN'}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="plugin-detail">
  <c:if test="${param.msg == 'reported_ok'}">
    <p class="form-notice">신고가 접수되었습니다.</p>
  </c:if>

  <header class="plugin-header">
    <span class="badge badge-${fn:toLowerCase(plugin.typeName)}"><c:out value="${plugin.typeName}"/></span>
    <h1><c:out value="${plugin.title}"/></h1>
    <p class="seller">
      판매자: <c:out value="${sellerNickname}" default="(알 수 없음)"/>
    </p>
    <p class="price">
      <c:choose>
        <c:when test="${plugin.price == 0}">무료</c:when>
        <c:otherwise><c:out value="${plugin.price}"/>원</c:otherwise>
      </c:choose>
    </p>
    <p class="meta">
      다운로드 <c:out value="${plugin.downloadCount}"/> ·
      조회 <c:out value="${plugin.viewCount}"/>
    </p>
  </header>

  <p class="summary"><c:out value="${plugin.summary}"/></p>

  <c:if test="${not empty plugin.demoUrl}">
    <p><a href="<c:out value='${plugin.demoUrl}'/>" target="_blank" rel="noopener">데모 보기</a></p>
  </c:if>

  <c:if test="${not empty videoEmbedSrc}">
    <div class="video-embed">
      <iframe src="<c:out value='${videoEmbedSrc}'/>" allowfullscreen
              frameborder="0" width="640" height="360"></iframe>
    </div>
  </c:if>

  <c:if test="${not empty tags}">
    <p class="tag-row">
      <c:forEach var="t" items="${tags}">
        <a class="tag" href="${ctx}/app/plugin/list?tag=<c:out value='${t.name}'/>">#<c:out value="${t.name}"/></a>
      </c:forEach>
    </p>
  </c:if>

  <section class="plugin-body">
    <c:choose>
      <c:when test="${plugin.typeName == 'PROMPT'}">
        <pre class="prompt-body"><c:out value="${plugin.body}"/></pre>
      </c:when>
      <c:otherwise>
        <div class="markdown-body">${bodyHtml}</div>
      </c:otherwise>
    </c:choose>
  </section>

  <div class="action-row">
    <c:if test="${not empty me and not isOwner}">
      <form method="post" action="${ctx}/app/cart/add?pluginId=${plugin.id}" class="inline">
        <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>
        <button type="submit">장바구니 담기</button>
      </form>
    </c:if>

    <c:if test="${not empty me and (plugin.price == 0 or isOwner or isAdmin)}">
      <a class="button" href="${ctx}/app/plugin/download?id=${plugin.id}">다운로드</a>
    </c:if>

    <c:if test="${isOwner or isAdmin}">
      <a class="button" href="${ctx}/app/plugin/edit?id=${plugin.id}">수정</a>
      <form method="post" action="${ctx}/app/plugin/delete?id=${plugin.id}" class="inline"
            onsubmit="return confirm('정말 삭제하시겠습니까?');">
        <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>
        <button type="submit" class="danger">삭제</button>
      </form>
    </c:if>
  </div>

  <c:if test="${not empty me and not isOwner}">
    <details class="report-form">
      <summary>신고하기</summary>
      <form method="post" action="${ctx}/app/plugin/report?id=${plugin.id}">
        <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>
        <textarea name="reason" minlength="3" maxlength="300" required
                  placeholder="사유를 적어주세요"></textarea>
        <button type="submit">신고</button>
      </form>
    </details>
  </c:if>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
