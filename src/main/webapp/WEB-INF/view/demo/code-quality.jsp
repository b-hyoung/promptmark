<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle">데모 — 코드 품질 셋트</c:set>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<div class="demo-shell">
  <header class="demo-hero">
    <p class="crumb"><a href="${ctx}/app/bundle/detail?id=2">← 코드 품질 셋트</a> · 데모 (실제 실행 캡처)</p>
    <h1>PR #247 — 시니어 리뷰어 한 명 더 둔 효과</h1>
    <p class="scenario">"사용자 초대 기능" PR을 머지하기 전 자동 호출. 셋트가 3개 의미있는 코멘트만 남깁니다 (false-positive 제거됨).</p>
    <div class="demo-skills">
      <span class="demo-skill">karpathy-guidelines</span>
      <span class="demo-skill">simplify</span>
      <span class="demo-skill">claude-api</span>
    </div>
  </header>

  <nav class="demo-tabs">
    <a href="#result">📄 결과물</a>
    <a href="#history">📜 작업 히스토리 (인터랙티브)</a>
  </nav>

  <section class="demo-section" id="result">
    <span class="demo-section-label">/ result</span>
    <h2>PR 자동 리뷰 결과</h2>
    <p class="lead">12 파일 변경된 PR. 셋트가 단 3개의 의미있는 코멘트로 다 잡았습니다.</p>

    <div class="demo-canvas pr-shell">
      <div class="pr-header">
        <span class="pr-num">PR #247</span>
        <span class="pr-title">feat: add user invitation flow</span>
        <span class="pr-status">✓ Auto-review passed</span>
      </div>

      <div class="pr-comment">
        <span class="pr-avatar">SK</span>
        <div>
          <div class="pr-comment-head">
            <span class="pr-author">SkillBot</span>
            <span class="pr-tag warn">karpathy</span>
            <span class="pr-when">방금 · UserService.java:88</span>
          </div>
          <div class="pr-comment-body">⚠️ <code>userService.findById()</code> 가 <code>Optional&lt;User&gt;</code> 를 반환하는데, 바로 다음 줄에서 <code>.getEmail()</code> 을 직접 호출하고 있습니다. <code>NullPointerException</code> 가능성. 가정을 명시화하거나 <code>orElseThrow()</code> 사용 권장.</div>
          <pre class="pr-codeblock"><span class="ln-ctx">86  public void sendInvite(Long userId, String role) {</span>
<span class="ln-ctx">87      var user = userService.findById(userId);</span>
<span class="ln-del">88-     mailer.send(user.getEmail(), inviteTemplate(role));</span>
<span class="ln-add">88+     var u = user.orElseThrow(() -> new NotFoundException("user " + userId));</span>
<span class="ln-add">89+     mailer.send(u.getEmail(), inviteTemplate(role));</span></pre>
        </div>
      </div>

      <div class="pr-comment">
        <span class="pr-avatar">SK</span>
        <div>
          <div class="pr-comment-head">
            <span class="pr-author">SkillBot</span>
            <span class="pr-tag tidy">simplify</span>
            <span class="pr-when">방금 · ConfigLoader.java:14</span>
          </div>
          <div class="pr-comment-body">🧹 <code>parseConfig()</code> 가 <code>loadConfig()</code> 와 거의 같은 일을 합니다. 후자가 더 최근 추가됐고, 호출처 3곳이 모두 <code>loadConfig()</code> 만 씁니다. <code>parseConfig()</code> 는 사용처 0 → 삭제 권장.</div>
        </div>
      </div>

      <div class="pr-comment">
        <span class="pr-avatar">SK</span>
        <div>
          <div class="pr-comment-head">
            <span class="pr-author">SkillBot</span>
            <span class="pr-tag info">claude-api</span>
            <span class="pr-when">방금 · InviteAgent.java:42</span>
          </div>
          <div class="pr-comment-body">💡 <code>anthropicClient</code> 가 호출당 새 인스턴스로 생성되고 있습니다 (line 42, 67, 91). <code>cache_control</code> 활성화 + 인스턴스 재사용 시 invite 1건당 토큰 비용 <strong>$0.43 → $0.12</strong> (72% 절감). 호출처 3곳 다 정리하면 한 달 약 $180 절감 예상.</div>
        </div>
      </div>
    </div>
  </section>

  <section class="demo-section" id="history">
    <span class="demo-section-label">/ history · interactive</span>
    <h2>작업 히스토리 — 4분 18초</h2>
    <p class="lead">PR push 후크가 호출 → 셋트가 사용자에게 검토 우선순위 질문 → 답에 따라 동작.</p>

    <div class="timeline-wrap">
      <div class="timeline-rail"><div class="timeline-progress"></div></div>
      <ol class="history-list interactive-history">
        <li class="history-item" data-step="1">
          <span class="history-time">09:14</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span><span class="history-msg">git push → PR #247</span></div>
            <div class="history-msg quote">"feat: add user invitation flow · 12 files / +387 / -42"</div>
          </div>
        </li>

        <li class="history-item" data-step="2">
          <span class="history-time">09:14</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">karpathy-guidelines</span><span class="history-msg">검토 우선순위 질문</span></div>
            <div class="history-msg ask">이 PR 무엇이 가장 위험? ① 테스트 누락 ② 가정 명시 빠짐 ③ 과도한 복잡화</div>
          </div>
        </li>

        <li class="history-item" data-step="3">
          <span class="history-time">09:14</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">② 가정 명시 빠짐 — 최근 이 패턴으로 hotfix 두 번 났음</div>
          </div>
        </li>

        <li class="history-item" data-step="4">
          <span class="history-time">09:15</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">karpathy-guidelines</span><span class="history-msg">변경된 코드 스캔 — Optional/null/empty 처리</span></div>
            <div class="history-out">
              UserService.java:88 · <code>findById()</code> Optional → <code>.getEmail()</code> 직접 — NPE 가능 (1건)<br>
              InviteAgent.java:42 · <code>List&lt;Email&gt;</code> empty 체크 없이 first() — OK (already guarded)<br>
              MailerConfig.java:21 · env 가정 — 명시됨 OK
            </div>
          </div>
        </li>

        <li class="history-item" data-step="5">
          <span class="history-time">09:16</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">simplify</span><span class="history-msg">중복·죽은 코드 추가 검토 동의?</span></div>
            <div class="history-msg ask">변경 코드만 vs 전체 코드베이스 — 어디까지?</div>
          </div>
        </li>

        <li class="history-item" data-step="6">
          <span class="history-time">09:16</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">변경 코드만 — 전체 리뷰는 별도 PR로</div>
          </div>
        </li>

        <li class="history-item" data-step="7">
          <span class="history-time">09:17</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">simplify</span><span class="history-msg">변경 12 파일 한 바퀴</span></div>
            <div class="history-out">
              중복: ConfigLoader.parseConfig() ↔ loadConfig() (호출처 0개) — <strong>1건</strong><br>
              죽은 코드: 없음<br>
              복잡도: McCabe ≤ 5 — 통과
            </div>
          </div>
        </li>

        <li class="history-item" data-step="8">
          <span class="history-time">09:17</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">claude-api</span><span class="history-msg">API 호출이 추가된 파일 있나? 검토 여부</span></div>
            <div class="history-msg ask">InviteAgent.java에 anthropic 호출 3곳 발견. 비용·캐싱 검토 진행?</div>
          </div>
        </li>

        <li class="history-item" data-step="9">
          <span class="history-time">09:17</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill user">USER</span></div>
            <div class="history-msg ans">YES</div>
          </div>
        </li>

        <li class="history-item" data-step="10">
          <span class="history-time">09:18</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill">claude-api</span><span class="history-msg">호출당 새 인스턴스 — cache_control 적용 시 비용 추정</span></div>
            <div class="history-out">
              현재: 호출당 0.43달러 (system prompt 매번 풀 토큰)<br>
              <code>cache_control: ephemeral</code> + 인스턴스 재사용 → 0.12달러 (72% 절감)<br>
              월 ~600건 호출 가정 시 ~$180 절감
            </div>
          </div>
        </li>

        <li class="history-item" data-step="11">
          <span class="history-time">09:18</span>
          <div class="history-body">
            <div class="history-action"><span class="history-skill system">SYSTEM</span><span class="history-msg">PR에 3개 코멘트 게시 + 알림</span></div>
            <div class="history-msg gate">발견된 진짜 이슈만 코멘트 (3건). 통과한 14건은 노이즈 방지로 생략.</div>
          </div>
        </li>
      </ol>
    </div>
  </section>

  <%@ include file="/WEB-INF/view/demo/_signature.jspf" %>
</div>

<script>
(function() {
  const items = document.querySelectorAll('.interactive-history .history-item');
  const bar = document.querySelector('.timeline-progress');
  if (!items.length) return;
  const io = new IntersectionObserver((entries) => {
    entries.forEach(e => { if (e.isIntersecting && e.intersectionRatio > 0.4) e.target.classList.add('is-active'); });
    if (bar) bar.style.height = (document.querySelectorAll('.interactive-history .history-item.is-active').length / items.length * 100) + '%';
  }, { threshold: [0.4] });
  items.forEach(it => { io.observe(it); it.addEventListener('click', () => { items.forEach(x => x.classList.remove('is-focus')); it.classList.add('is-focus'); }); });
})();
</script>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
