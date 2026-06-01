<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="bundle.messages"/>
<c:set var="pageTitle">새 셋트 — promptmark</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="bundle-form plugin-form">
  <h1>새 셋트 만들기</h1>

  <form method="post" action="${ctx}/app/bundle/new">
    <%@ include file="/WEB-INF/view/layout/csrf.jspf" %>

    <label>slug (URL용, 영소문자/숫자/하이픈)
      <input name="slug" required pattern="[a-z0-9]([a-z0-9\-]{0,38}[a-z0-9])?" maxlength="40">
    </label>

    <label>셋트 이름
      <input name="name" required minlength="2" maxlength="100">
    </label>

    <label>한 줄 슬로건
      <input name="tagline" maxlength="200">
    </label>

    <label>스토리 (왜 이 조합인지, Markdown 허용)
      <textarea name="story" rows="6"></textarea>
    </label>

    <label>가격(원)
      <input name="price" type="number" min="0" required value="0">
    </label>

    <label>썸네일 URL
      <input name="thumbnail" type="url">
    </label>

    <fieldset>
      <legend>포함할 플러그인 (Ctrl/⌘ 누르고 복수 선택)</legend>
      <select name="pluginIds" multiple size="10" style="width: 100%; height: auto;">
        <c:forEach var="p" items="${allPlugins}">
          <option value="${p.id}"><c:out value="${p.title}"/> — <c:out value="${p.summary}"/></option>
        </c:forEach>
      </select>
    </fieldset>

    <button class="btn-primary" type="submit">생성</button>
    <a href="${ctx}/app/bundle/list">취소</a>
  </form>
</section>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
