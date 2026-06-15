<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Lịch khám – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/appointments.css">
</head>
<body>

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="appt-wrap">

  <div class="appt-page-header">
    <div>
      <h1>Lịch khám của bạn</h1>
      <p>Xem và quản lý tất cả các lịch hẹn</p>
    </div>
    <a href="${ctx}/booking/new" class="btn-new-booking">➕ Đặt lịch mới</a>
  </div>

  <div class="appt-tabs">
    <button class="appt-tab active" onclick="switchTab('upcoming', this)">
      Sắp tới
      <c:if test="${not empty upcoming}">
        <span class="tab-badge">${upcoming.size()}</span>
      </c:if>
    </button>
    <button class="appt-tab" onclick="switchTab('history', this)">
      Lịch sử
      <c:if test="${not empty history}">
        <span class="tab-badge tab-badge--gray">${history.size()}</span>
      </c:if>
    </button>
  </div>

  <div id="tab-upcoming" class="appt-tab-panel active">
    <c:choose>
      <c:when test="${empty upcoming}">
        <div class="appt-empty">
          <div class="appt-empty-icon">📭</div>
          <h3>Chưa có lịch hẹn nào sắp tới</h3>
          <p>Hãy đặt lịch khám để chăm sóc thú cưng của bạn</p>
          <a href="${ctx}/booking/new" class="btn-new-booking" style="display:inline-flex;margin-top:16px;">Đặt lịch ngay</a>
        </div>
      </c:when>
      <c:otherwise>
        <div class="appt-list">
          <c:forEach var="a" items="${upcoming}">
            <a href="${ctx}/appointments/detail?id=${a.appointmentID}" class="appt-card appt-card--upcoming">
              <div class="appt-card-date">
                <span class="appt-day">${a.appointmentDate.dayOfMonth}</span>
                <span class="appt-month">${a.monthDisplayVi}</span>
              </div>
              <div class="appt-card-body">
                <div class="appt-card-title">${a.serviceName}</div>
                <div class="appt-card-meta">
                  <span>${a.petName}</span>
                  <span>${a.formattedStartTime} – ${a.formattedEndTime}</span>
                  <c:if test="${not empty a.vetName}"><span>${a.vetName}</span></c:if>
                </div>
              </div>
              <div class="appt-card-right">
                <span class="status-badge status-${fn:toLowerCase(a.status)}">${a.status}</span>
                <c:if test="${a.canModify()}"><span class="appt-modifiable">Có thể chỉnh sửa</span></c:if>
                <span class="appt-arrow">›</span>
              </div>
            </a>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </div>

  <div id="tab-history" class="appt-tab-panel">
    <c:choose>
      <c:when test="${empty history}">
        <div class="appt-empty">
          <div class="appt-empty-icon">🗂</div>
          <h3>Chưa có lịch sử khám</h3>
          <p>Các lịch hẹn đã hoàn thành hoặc huỷ sẽ hiển thị ở đây</p>
        </div>
      </c:when>
      <c:otherwise>
        <div class="appt-list">
          <c:forEach var="a" items="${history}">
            <a href="${ctx}/appointments/detail?id=${a.appointmentID}" class="appt-card">
              <div class="appt-card-date appt-card-date--muted">
                <span class="appt-day">${a.appointmentDate.dayOfMonth}</span>
                <span class="appt-month">${a.monthDisplayVi}</span>
              </div>
              <div class="appt-card-body">
                <div class="appt-card-title">${a.serviceName}</div>
                <div class="appt-card-meta">
                  <span>${a.petName}</span>
                  <span>${a.formattedStartTime} – ${a.formattedEndTime}</span>
                  <c:if test="${not empty a.vetName}"><span>${a.vetName}</span></c:if>
                </div>
                <c:if test="${not empty a.notes}">
                  <div class="appt-notes-preview">${a.notes}</div>
                </c:if>
              </div>
              <div class="appt-card-right">
                <span class="status-badge status-${fn:toLowerCase(a.status)}">${a.status}</span>
                <span class="appt-arrow">›</span>
              </div>
            </a>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </div>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
<script>
function switchTab(name, btn) {
  document.querySelectorAll('.appt-tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.appt-tab-panel').forEach(p => p.classList.remove('active'));
  btn.classList.add('active');
  document.getElementById('tab-' + name).classList.add('active');
}
</script>
</body>
</html>
