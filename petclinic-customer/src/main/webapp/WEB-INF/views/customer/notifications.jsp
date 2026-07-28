<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Thông báo – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/notification.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="notif-page-wrap">

  <%-- ── Page header ─────────────────────────────────────────── --%>
  <div class="notif-page-header">
    <h1>
      Thông báo
      <c:if test="${unreadCount > 0}">
        <span class="unread-total">${unreadCount} mới</span>
      </c:if>
    </h1>
    <c:if test="${unreadCount > 0}">
      <form action="${ctx}/notifications/mark-read" method="post" style="margin:0;">
        <button type="submit" class="btn-mark-all">Đánh dấu tất cả đã đọc</button>
      </form>
    </c:if>
  </div>

  <%-- ── Tab navigation ─────────────────────────────────────── --%>
  <nav class="notif-tabs">

    <a class="notif-tab ${activeTab eq 'ALL' ? 'active' : ''}"
       href="${ctx}/notifications">
      Tất cả
      <c:if test="${unreadCount > 0}">
        <span class="notif-tab-badge">${unreadCount}</span>
      </c:if>
    </a>

    <a class="notif-tab ${activeTab eq 'REMINDER' ? 'active' : ''}"
       href="${ctx}/notifications?tab=REMINDER">
      Nhắc lịch
      <c:if test="${cntReminder > 0}">
        <span class="notif-tab-badge">${cntReminder}</span>
      </c:if>
    </a>

    <a class="notif-tab ${activeTab eq 'PAYMENT' ? 'active' : ''}"
       href="${ctx}/notifications?tab=PAYMENT">
      Thanh toán
      <c:if test="${cntPayment > 0}">
        <span class="notif-tab-badge">${cntPayment}</span>
      </c:if>
    </a>

    <a class="notif-tab ${activeTab eq 'EXAM_RESULT' ? 'active' : ''}"
       href="${ctx}/notifications?tab=EXAM_RESULT">
      Kết quả khám
      <c:if test="${cntExam > 0}">
        <span class="notif-tab-badge">${cntExam}</span>
      </c:if>
    </a>

    <a class="notif-tab ${activeTab eq 'CARE_TIP' ? 'active' : ''}"
       href="${ctx}/notifications?tab=CARE_TIP">
      Chăm sóc & Hỗ trợ
      <c:if test="${cntCare > 0}">
        <span class="notif-tab-badge">${cntCare}</span>
      </c:if>
    </a>

  </nav>

  <%-- ── Notification list ───────────────────────────────────── --%>
  <c:choose>
    <c:when test="${empty notifications}">
      <div class="notif-empty">
        <h3>Không có thông báo nào</h3>
        <p>
          <c:choose>
            <c:when test="${activeTab eq 'REMINDER'}">Chưa có nhắc lịch hẹn nào. Hãy đặt lịch khám!</c:when>
            <c:when test="${activeTab eq 'PAYMENT'}">Chưa có thông báo thanh toán.</c:when>
            <c:when test="${activeTab eq 'EXAM_RESULT'}">Chưa có kết quả khám nào được cập nhật.</c:when>
            <c:when test="${activeTab eq 'CARE_TIP'}">Chưa có mẹo chăm sóc hay tin hỗ trợ nào.</c:when>
            <c:otherwise>Bạn chưa có thông báo nào. Các thông báo về lịch hẹn, thanh toán sẽ xuất hiện ở đây.</c:otherwise>
          </c:choose>
        </p>
        <c:if test="${activeTab eq 'ALL' or activeTab eq 'REMINDER'}">
          <a href="${ctx}/booking/new" style="display:inline-block;margin-top:16px;
             padding:10px 22px;background:var(--green-700);color:#fff;border-radius:10px;
             font-size:14px;font-weight:600;text-decoration:none;">Đặt lịch ngay</a>
        </c:if>
      </div>
    </c:when>
    <c:otherwise>
      <div class="notif-list" id="notifList">
        <c:forEach var="n" items="${notifications}">
          <c:choose>
            <%-- Clickable if has actionUrl --%>
            <c:when test="${not empty n.actionUrl}">
              <a href="${ctx}${n.actionUrl}"
                 class="notif-item ${n.typeColor} ${n.read ? '' : 'unread'}"
                 id="ni-${n.notificationID}"
                 onclick="markReadOnClick(${n.notificationID})">
                <div class="notif-content">
                  <div class="notif-title">${n.title}</div>
                  <div class="notif-body">${n.body}</div>
                  <div class="notif-footer-row">
                    <span class="notif-category-tag">${n.categoryLabel}</span>
                    <span class="notif-time">${n.relativeTime}</span>
                    <span class="notif-action-link">Xem chi tiết →</span>
                  </div>
                </div>
                <c:if test="${!n.read}">
                  <div class="notif-unread-dot"></div>
                  <button type="button" class="btn-mark-single"
                          title="Đánh dấu đã đọc"
                          onclick="markSingle(event, ${n.notificationID})">✓</button>
                </c:if>
              </a>
            </c:when>
            <%-- Non-clickable (INFO, CARE_TIP without link) --%>
            <c:otherwise>
              <div class="notif-item ${n.typeColor} ${n.read ? '' : 'unread'}"
                   id="ni-${n.notificationID}">
                <div class="notif-content">
                  <div class="notif-title">${n.title}</div>
                  <div class="notif-body">${n.body}</div>
                  <div class="notif-footer-row">
                    <span class="notif-category-tag">${n.categoryLabel}</span>
                    <span class="notif-time">${n.relativeTime}</span>
                  </div>
                </div>
                <c:if test="${!n.read}">
                  <div class="notif-unread-dot"></div>
                  <button type="button" class="btn-mark-single"
                          title="Đánh dấu đã đọc"
                          onclick="markSingle(event, ${n.notificationID})">✓</button>
                </c:if>
              </div>
            </c:otherwise>
          </c:choose>
        </c:forEach>
      </div>
    </c:otherwise>
  </c:choose>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script>
const CTX = '${ctx}';

/** Mark a single notification read via AJAX then hide dot. */
function markSingle(e, id) {
  e.preventDefault();
  e.stopPropagation();
  fetch(CTX + '/notifications/mark-read', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' },
    body: 'id=' + id
  }).then(r => r.json()).then(d => {
    const el = document.getElementById('ni-' + id);
    if (el) {
      el.classList.remove('unread');
      el.querySelector('.notif-unread-dot')?.remove();
      el.querySelector('.btn-mark-single')?.remove();
    }
    updateBadge(d.unread);
  });
}

/** Mark read on navigation click (fire-and-forget). */
function markReadOnClick(id) {
  fetch(CTX + '/notifications/mark-read', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' },
    body: 'id=' + id
  }).catch(() => {});
}

/** Update header bell badge. */
function updateBadge(count) {
  const badge = document.querySelector('.notif-badge');
  if (!badge) return;
  if (count <= 0) { badge.remove(); return; }
  badge.textContent = count > 9 ? '9+' : count;
}

/** Auto-poll unread count every 60s to keep badge fresh. */
setInterval(() => {
  fetch(CTX + '/notifications/count', { headers: { 'Accept': 'application/json' } })
    .then(r => r.json())
    .then(d => updateBadge(d.unread))
    .catch(() => {});
}, 60000);
</script>
</body>
</html>
