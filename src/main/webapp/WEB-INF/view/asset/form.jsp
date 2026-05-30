<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="csrf" value="${sessionScope.CSRF_TOKEN}"/>
<c:choose>
  <c:when test="${mode == 'edit'}">
    <c:set var="pageTitle" value="자산 수정 — promptmark"/>
    <c:set var="formAction" value="${ctx}/app/asset/edit?id=${assetId}&csrf_token=${csrf}"/>
    <c:set var="submitLabel" value="수정 저장"/>
  </c:when>
  <c:otherwise>
    <c:set var="pageTitle" value="자산 등록 — promptmark"/>
    <c:set var="formAction" value="${ctx}/app/asset/new?csrf_token=${csrf}"/>
    <c:set var="submitLabel" value="등록"/>
  </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<section class="asset-form">
  <h1>${pageTitle}</h1>

  <c:if test="${not empty errorMessage}">
    <p class="form-error"><c:out value="${errorMessage}"/></p>
  </c:if>

  <%--
    Multipart form. The CsrfFilter reads csrf_token via getParameter, which
    works on a URL query string even when the request body is multipart, so
    we put the token in the action URL.
  --%>
  <form method="post" enctype="multipart/form-data" action="${formAction}">
    <fieldset class="field">
      <legend>자산 종류</legend>
      <label>
        <input type="radio" name="type" value="PROMPT"
               <c:if test="${empty form.type or form.type == 'PROMPT'}">checked</c:if>
               <c:if test="${mode == 'edit'}">disabled</c:if>>
        프롬프트
      </label>
      <label>
        <input type="radio" name="type" value="MD"
               <c:if test="${form.type == 'MD'}">checked</c:if>
               <c:if test="${mode == 'edit'}">disabled</c:if>>
        마크다운 파일
      </label>
      <c:if test="${mode == 'edit'}">
        <input type="hidden" name="type" value="<c:out value='${form.type}'/>">
      </c:if>
      <c:if test="${not empty errors.type}">
        <small class="field-error"><c:out value="${errors.type}"/></small>
      </c:if>
    </fieldset>

    <div class="field">
      <label for="title">제목</label>
      <input id="title" name="title" type="text" maxlength="120" required
             value="<c:out value='${form.title}'/>">
      <c:if test="${not empty errors.title}">
        <small class="field-error"><c:out value="${errors.title}"/></small>
      </c:if>
    </div>

    <div class="field">
      <label for="summary">요약</label>
      <textarea id="summary" name="summary" maxlength="300" rows="2" required><c:out value="${form.summary}"/></textarea>
      <c:if test="${not empty errors.summary}">
        <small class="field-error"><c:out value="${errors.summary}"/></small>
      </c:if>
    </div>

    <div class="field" id="body-field">
      <label for="body">본문 (프롬프트)</label>
      <textarea id="body" name="body" rows="10"><c:out value="${body}"/></textarea>
      <c:if test="${not empty errors.body}">
        <small class="field-error"><c:out value="${errors.body}"/></small>
      </c:if>
    </div>

    <div class="field" id="file-field">
      <label for="file">마크다운 파일 (.md)</label>
      <input id="file" name="file" type="file" accept=".md,text/markdown,text/plain">
      <c:if test="${mode == 'edit' and not empty asset and not empty asset.fileKey}">
        <small>현재 파일: <code><c:out value="${asset.fileKey}"/></code> (새 파일을 선택하지 않으면 그대로 유지)</small>
      </c:if>
      <c:if test="${not empty errors.file}">
        <small class="field-error"><c:out value="${errors.file}"/></small>
      </c:if>
    </div>

    <div class="field">
      <label for="demo_url">데모 URL (선택)</label>
      <input id="demo_url" name="demo_url" type="url" maxlength="500"
             value="<c:out value='${form.demo_url}'/>">
      <c:if test="${not empty errors.demo_url}">
        <small class="field-error"><c:out value="${errors.demo_url}"/></small>
      </c:if>
    </div>

    <div class="field">
      <label for="video_url">영상 URL (YouTube/Vimeo, 선택)</label>
      <input id="video_url" name="video_url" type="url" maxlength="500"
             value="<c:out value='${form.video_url}'/>">
      <c:if test="${not empty errors.video_url}">
        <small class="field-error"><c:out value="${errors.video_url}"/></small>
      </c:if>
    </div>

    <div class="field">
      <label for="price">가격 (원)</label>
      <input id="price" name="price" type="number" min="0" required
             value="<c:out value='${form.price}'/>">
      <c:if test="${not empty errors.price}">
        <small class="field-error"><c:out value="${errors.price}"/></small>
      </c:if>
    </div>

    <div class="field">
      <label for="tags">태그 (콤마로 구분, 최대 40자/개)</label>
      <input id="tags" name="tags" type="text" value="<c:out value='${tagsCsv}'/>">
      <c:if test="${not empty tags}">
        <small class="tag-suggestions">예:
          <c:forEach var="t" items="${tags}" varStatus="s">
            <c:out value="${t.name}"/><c:if test="${not s.last}">, </c:if>
          </c:forEach>
        </small>
      </c:if>
    </div>

    <button type="submit">${submitLabel}</button>
  </form>
</section>

<script>
  (function () {
    var bodyField = document.getElementById('body-field');
    var fileField = document.getElementById('file-field');
    function refresh() {
      var radios = document.getElementsByName('type');
      var chosen = 'PROMPT';
      for (var i = 0; i < radios.length; i++) if (radios[i].checked) chosen = radios[i].value;
      bodyField.style.display = (chosen === 'PROMPT') ? '' : 'none';
      fileField.style.display = (chosen === 'MD') ? '' : 'none';
    }
    var radios = document.getElementsByName('type');
    for (var i = 0; i < radios.length; i++) radios[i].addEventListener('change', refresh);
    refresh();
  })();
</script>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
