<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="AI 추천 챗봇 — promptmark"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%@ include file="/WEB-INF/view/layout/header.jsp" %>

<style>
  .chat-shell { max-width: 760px; margin: 0 auto; padding: 1rem 0; }
  .chat-header { display: flex; justify-content: space-between; align-items: center;
                 margin-bottom: 0.75rem; }
  .chat-messages { border: 1px solid #e5e5e5; border-radius: 8px; padding: 1rem;
                   min-height: 320px; max-height: 520px; overflow-y: auto;
                   background: #fafafa; margin-bottom: 0.75rem; }
  .bubble { padding: 0.6rem 0.85rem; border-radius: 12px; margin-bottom: 0.5rem;
            max-width: 80%; white-space: pre-wrap; word-break: break-word; line-height: 1.4; }
  .bubble.user { background: #2f7df5; color: #fff; margin-left: auto; }
  .bubble.bot  { background: #fff; color: #1a1a1a; border: 1px solid #e5e5e5; }
  .bubble.note { background: #fff7d6; color: #5a4500; font-size: 0.85rem; max-width: 100%; }
  .chat-row { display: flex; }
  .chat-row.user { justify-content: flex-end; }
  .chat-input { display: flex; gap: 0.5rem; }
  .chat-input textarea { flex: 1; padding: 0.6rem; font-size: 1rem; resize: vertical;
                        min-height: 64px; border: 1px solid #ccc; border-radius: 6px; }
  .chat-input button { padding: 0.6rem 1rem; cursor: pointer; }
  .card-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
               gap: 0.5rem; margin-top: 0.5rem; }
  .asset-mini-card { border: 1px solid #ddd; border-radius: 6px; padding: 0.5rem 0.75rem;
                     background: #fff; }
  .asset-mini-card a { color: #2f7df5; text-decoration: none; font-weight: 600; }
  .asset-mini-card .meta { font-size: 0.8rem; color: #666; margin-top: 0.25rem; }
  .asset-mini-card .summary { font-size: 0.85rem; margin-top: 0.25rem; color: #333; }
  .trace-toggle { font-size: 0.85rem; color: #666; cursor: pointer; margin-top: 0.5rem; }
  .trace-block { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 0.75rem;
                 background: #f1f1f1; padding: 0.5rem; border-radius: 6px;
                 white-space: pre-wrap; word-break: break-word; }
  .source-badge { display: inline-block; font-size: 0.7rem; background: #eee; color: #333;
                  padding: 0.1rem 0.5rem; border-radius: 4px; margin-left: 0.4rem; }
  .source-badge.rule { background: #fff7d6; color: #5a4500; }
</style>

<section class="chat-shell">
  <div class="chat-header">
    <h1>AI 추천 챗봇</h1>
    <button type="button" id="resetBtn">초기화</button>
  </div>

  <p class="form-notice">자연어로 질문하면 데이터베이스에서 어울리는 프롬프트/MD 자산을 찾아드려요.</p>

  <div id="messages" class="chat-messages" aria-live="polite"></div>

  <form id="chatForm" class="chat-input" autocomplete="off">
    <label for="messageInput" class="sr-only">메시지 입력</label>
    <textarea id="messageInput" name="message" required maxlength="1000"
              placeholder="예: 자기소개서를 정리해주는 프롬프트가 필요해요"></textarea>
    <button type="submit" id="sendBtn">보내기</button>
  </form>
</section>

<script>
(function () {
  var CTX = "${ctx}";
  var CSRF_TOKEN = "${sessionScope.CSRF_TOKEN}";
  var STORAGE_KEY = "promptmark.chat.v1";

  var messagesEl = document.getElementById("messages");
  var formEl = document.getElementById("chatForm");
  var inputEl = document.getElementById("messageInput");
  var sendBtn = document.getElementById("sendBtn");
  var resetBtn = document.getElementById("resetBtn");

  function loadHistory() {
    try {
      var raw = sessionStorage.getItem(STORAGE_KEY);
      if (!raw) return [];
      var parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch (e) {
      return [];
    }
  }

  function saveHistory(history) {
    try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify(history)); }
    catch (e) { /* sessionStorage full or disabled */ }
  }

  function clearHistory() {
    try { sessionStorage.removeItem(STORAGE_KEY); } catch (e) {}
  }

  function appendUserBubble(text) {
    var row = document.createElement("div");
    row.className = "chat-row user";
    var b = document.createElement("div");
    b.className = "bubble user";
    b.textContent = text;
    row.appendChild(b);
    messagesEl.appendChild(row);
    scrollToBottom();
  }

  function appendNoteBubble(text) {
    var row = document.createElement("div");
    row.className = "chat-row";
    var b = document.createElement("div");
    b.className = "bubble note";
    b.textContent = text;
    row.appendChild(b);
    messagesEl.appendChild(row);
    scrollToBottom();
  }

  function appendBotBubble(payload) {
    var row = document.createElement("div");
    row.className = "chat-row";
    var b = document.createElement("div");
    b.className = "bubble bot";

    var ans = document.createElement("div");
    ans.textContent = payload.answer || "";
    if (payload.source) {
      var badge = document.createElement("span");
      badge.className = "source-badge" + (payload.source === "RULE_FALLBACK" ? " rule" : "");
      badge.textContent = payload.source === "RULE_FALLBACK" ? "RULE" : "AGENT";
      ans.appendChild(badge);
    }
    b.appendChild(ans);

    if (payload.items && payload.items.length) {
      var grid = document.createElement("div");
      grid.className = "card-grid";
      payload.items.forEach(function (it) {
        grid.appendChild(renderCard(it));
      });
      b.appendChild(grid);
    }

    if (payload.trace && payload.trace.length) {
      var toggle = document.createElement("div");
      toggle.className = "trace-toggle";
      toggle.textContent = "에이전트 사고 보기";
      var traceBox = document.createElement("pre");
      traceBox.className = "trace-block";
      traceBox.textContent = JSON.stringify(payload.trace, null, 2);
      traceBox.style.display = "none";
      toggle.addEventListener("click", function () {
        traceBox.style.display = traceBox.style.display === "none" ? "block" : "none";
      });
      b.appendChild(toggle);
      b.appendChild(traceBox);
    }

    row.appendChild(b);
    messagesEl.appendChild(row);
    scrollToBottom();
  }

  function renderCard(item) {
    var card = document.createElement("div");
    card.className = "asset-mini-card";
    var titleLink = document.createElement("a");
    titleLink.href = CTX + "/app/asset/detail?id=" + encodeURIComponent(item.id);
    titleLink.textContent = item.title || "(제목 없음)";
    card.appendChild(titleLink);
    var meta = document.createElement("div");
    meta.className = "meta";
    var pricePart = (item.price === 0 || item.price == null) ? "무료"
      : (item.price + "원");
    meta.textContent = (item.type || "") + " · " + pricePart
      + (typeof item.score === "number" ? (" · score " + item.score.toFixed(2)) : "");
    card.appendChild(meta);
    if (item.summary) {
      var sum = document.createElement("div");
      sum.className = "summary";
      sum.textContent = item.summary;
      card.appendChild(sum);
    }
    return card;
  }

  function scrollToBottom() {
    messagesEl.scrollTop = messagesEl.scrollHeight;
  }

  function renderHistory() {
    messagesEl.innerHTML = "";
    var history = loadHistory();
    history.forEach(function (entry) {
      if (entry.kind === "user") appendUserBubble(entry.text);
      else if (entry.kind === "bot") appendBotBubble(entry.payload);
      else if (entry.kind === "note") appendNoteBubble(entry.text);
    });
    if (history.length === 0) {
      appendNoteBubble("안녕하세요! 무엇이 필요한지 알려주세요.");
    }
  }

  function pushHistory(entry) {
    var history = loadHistory();
    history.push(entry);
    if (history.length > 100) history = history.slice(history.length - 100);
    saveHistory(history);
  }

  formEl.addEventListener("submit", function (ev) {
    ev.preventDefault();
    var message = (inputEl.value || "").trim();
    if (message.length === 0) return;

    inputEl.value = "";
    sendBtn.disabled = true;
    appendUserBubble(message);
    pushHistory({ kind: "user", text: message });

    fetch(CTX + "/app/chat/recommend", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": CSRF_TOKEN
      },
      body: JSON.stringify({ message: message })
    }).then(function (res) {
      return res.json().then(function (json) {
        if (!res.ok) {
          var msg = json && json.error && json.error.message
            ? json.error.message
            : "오류가 발생했어요 (" + res.status + ")";
          throw new Error(msg);
        }
        return json;
      });
    }).then(function (payload) {
      appendBotBubble(payload);
      pushHistory({ kind: "bot", payload: payload });
    }).catch(function (err) {
      var text = "요청 처리에 실패했어요: " + (err.message || "알 수 없는 오류");
      appendNoteBubble(text);
      pushHistory({ kind: "note", text: text });
    }).then(function () {
      sendBtn.disabled = false;
      inputEl.focus();
    });
  });

  resetBtn.addEventListener("click", function () {
    if (confirm("대화 기록을 초기화할까요?")) {
      clearHistory();
      renderHistory();
    }
  });

  renderHistory();
  inputEl.focus();
})();
</script>

<%@ include file="/WEB-INF/view/layout/footer.jsp" %>
