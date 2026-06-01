<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="bundle.messages"/>
<c:set var="pageTitle"><fmt:message key="plugin.list_title"/> — promptmark</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="plugin-list">
  <h1><fmt:message key="plugin.list_title"/></h1>

  <c:if test="${param.msg == 'deleted_ok'}">
    <p class="form-notice">자산이 삭제되었습니다.</p>
  </c:if>

  <form class="search-form" method="get" action="${ctx}/app/plugin/list">
    <input type="search" name="q" value="<c:out value='${q}'/>" placeholder="검색어">
    <select name="type">
      <option value="">전체 종류</option>
      <option value="PROMPT" <c:if test="${type == 'PROMPT'}">selected</c:if>>프롬프트</option>
      <option value="MD"     <c:if test="${type == 'MD'}">selected</c:if>>마크다운</option>
    </select>
    <input type="text" name="tag" value="<c:out value='${tag}'/>" placeholder="태그">
    <select name="sort">
      <option value="recent"  <c:if test="${sort == 'recent'}">selected</c:if>>최신순</option>
      <option value="popular" <c:if test="${sort == 'popular'}">selected</c:if>>인기순</option>
    </select>
    <button type="submit">검색</button>
  </form>

  <p class="search-meta">총 <c:out value="${total}"/>건</p>

  <c:choose>
    <c:when test="${empty plugins}">
      <p class="empty">조건에 맞는 자산이 없습니다.</p>
    </c:when>
    <c:otherwise>
      <ul class="plugin-grid">
        <c:forEach var="a" items="${plugins}">
          <li class="plugin-card">
            <a href="${ctx}/app/plugin/detail?id=${a.id}">
              <span class="badge badge-${fn:toLowerCase(a.typeName)}"><c:out value="${a.typeName}"/></span>
              <h2><c:out value="${a.title}"/></h2>
              <p class="summary"><c:out value="${a.summary}"/></p>
              <p class="price">
                <c:choose>
                  <c:when test="${a.price == 0}">무료</c:when>
                  <c:otherwise><c:out value="${a.price}"/>원</c:otherwise>
                </c:choose>
              </p>
              <p class="meta">
                다운로드 <c:out value="${a.downloadCount}"/> ·
                조회 <c:out value="${a.viewCount}"/>
              </p>
            </a>
          </li>
        </c:forEach>
      </ul>
    </c:otherwise>
  </c:choose>

  <nav class="pagination">
    <c:set var="qsBase">q=<c:out value='${q}'/>&amp;type=<c:out value='${type}'/>&amp;tag=<c:out value='${tag}'/>&amp;sort=<c:out value='${sort}'/></c:set>
    <c:if test="${page > 1}">
      <a class="pg" href="${ctx}/app/plugin/list?${qsBase}&amp;page=${page - 1}">이전</a>
    </c:if>
    <span class="pg-info">${page} / ${totalPages}</span>
    <c:if test="${page < totalPages}">
      <a class="pg" href="${ctx}/app/plugin/list?${qsBase}&amp;page=${page + 1}">다음</a>
    </c:if>
  </nav>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
