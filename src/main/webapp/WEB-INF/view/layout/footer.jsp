<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="ctxF" value="${pageContext.request.contextPath}"/>
</main>
<footer class="site-footer-full">
  <div class="footer-grid">
    <div>
      <div class="footer-brand">promptmark<span style="color:var(--accent);">.</span></div>
      <p class="footer-tag">Claude AI 스킬을 검증된 조합으로 큐레이션합니다.</p>
    </div>
    <div>
      <h4>탐색</h4>
      <ul>
        <li><a href="${ctxF}/app/bundle/list">셋트 목록</a></li>
        <li><a href="${ctxF}/app/plugin/list">개별 플러그인</a></li>
        <li><a href="${ctxF}/app/chat/page">AI 추천 챗봇</a></li>
      </ul>
    </div>
    <div>
      <h4>사이트</h4>
      <ul>
        <li><a href="${ctxF}/app/info/about">About · 큐레이션 철학</a></li>
        <li><a href="${ctxF}/">홈</a></li>
      </ul>
    </div>
    <div>
      <h4>큐레이터</h4>
      <ul>
        <li>b-hyoung</li>
        <li><span class="muted">매주 새 셋트 추가</span></li>
      </ul>
    </div>
  </div>
  <div class="footer-bottom">
    <small>© 2026 <strong>promptmark</strong> · 모든 셋트는 큐레이터가 직접 한 번 이상 사용해 검증한 조합입니다.</small>
  </div>
</footer>
</body>
</html>
