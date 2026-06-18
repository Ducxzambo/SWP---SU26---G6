<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Đổi lịch khám – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/appointments.css">
  <link rel="stylesheet" href="${ctx}/css/booking.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="detail-wrap">

  <a href="${ctx}/appointments/detail?id=${appt.appointmentID}" class="detail-back">
    Quay lại chi tiết
  </a>

  <%-- Current appointment info --%>
  <div class="detail-hero" style="margin-bottom:20px;">
    <div class="detail-hero-left">
      <div class="detail-service-label">Đổi lịch hẹn</div>
      <h1 class="detail-service-name">${appt.serviceName}</h1>
      <div class="detail-hero-meta">
        <span>${appt.petName}</span>
        <span>Lịch cũ:
          <strong>${appt.formattedAppointmentDate}</strong>
          lúc <strong>${appt.formattedStartTime}</strong>
        </span>
      </div>
    </div>
    <div class="detail-hero-right">
      <span class="status-badge status-badge--lg status-${fn:toLowerCase(appt.status)}">${appt.status}</span>
    </div>
  </div>

  <%-- Notice --%>
  <div class="reschedule-notice">
    Chỉ hiển thị khung giờ còn trống và cách hiện tại ít nhất <strong>12 giờ</strong>.
  </div>

  <%-- Error/success flash --%>
  <c:if test="${not empty requestScope.error}">
    <div class="bk-alert bk-alert--error">✗ ${requestScope.error}</div>
  </c:if>

  <form action="${ctx}/appointments/reschedule" method="post" id="rescheduleForm">
    <input type="hidden" name="appointmentId" value="${appt.appointmentID}">
    <input type="hidden" id="slotKeyInput"    name="slotKey"        value="">

    <c:choose>
      <c:when test="${isInpatient}">
        <%-- ── Inpatient: date + period picker ──────────────────────────── --%>
        <div class="bk-panel">
          <div class="bk-panel-head">🏨 Chọn ngày và buổi nội trú mới</div>
          <div class="bk-panel-body">
            <input type="hidden" name="isInpatient" value="true">
            <div class="inpatient-row">
              <div class="form-field">
                <label class="field-label">Ngày nhập viện mới</label>
                <input type="date" id="inpDate" name="inpatientDate"
                       min="${today}" class="field-input"
                       onchange="onInpDateChange(this.value)">
              </div>
              <div id="inpPeriodWrap" style="display:none;">
                <label class="field-label">Buổi</label>
                <div class="period-btns">
                  <button type="button" class="period-btn" id="btnMorning"
                          onclick="selectPeriod('morning')">
                    🌅 Sáng<br><small>08:00 – 12:00</small>
                  </button>
                  <button type="button" class="period-btn" id="btnAfternoon"
                          onclick="selectPeriod('afternoon')">
                    🌆 Chiều<br><small>13:30 – 17:30</small>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </c:when>
      <c:otherwise>
        <%-- ── Outpatient: date tabs + slot grid ─────────────────────────── --%>
        <div class="bk-panel" style="border-bottom-left-radius:0;border-bottom-right-radius:0;">
          <div class="bk-panel-head">Chọn ngày và khung giờ mới</div>
          <div class="bk-panel-body">
            <div class="date-tabs" id="dateTabs"></div>
            <div class="slot-grid" id="slotGrid">
              <div class="slot-loading">Đang tải...</div>
            </div>
          </div>
        </div>
      </c:otherwise>
    </c:choose>

    <%-- Footer: selection summary + confirm button --%>
    <div class="reschedule-footer">
      <div class="reschedule-selection">
        <span id="selectionText" style="color:var(--warm-gray);">Chưa chọn khung giờ nào</span>
      </div>
      <button type="submit" class="btn-confirm-reschedule" id="btnReschedule" disabled>
        ✓ Xác nhận đổi lịch
      </button>
    </div>
  </form>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script>
  /*
    Sửa lỗi: bản gốc viết trực tiếp
      const SLOTS_DATA = ${slotsJson};
      const IS_INPATIENT = ${isInpatient};
    Nếu biến EL "slotsJson" hoặc "isInpatient" không tồn tại trong scope (null/chưa
    set), EL sẽ render ra chuỗi rỗng, biến dòng trên thành "const SLOTS_DATA = ;"
    — lỗi cú pháp JavaScript làm toàn bộ script không chạy. Sửa lại bằng cách luôn
    cung cấp giá trị mặc định hợp lệ (object rỗng / false) ngay trong EL.
  */
  window.RESCHEDULE_SLOTS_DATA   = ${not empty slotsJson ? slotsJson : '{}'};
  window.RESCHEDULE_IS_INPATIENT = ${isInpatient ? 'true' : 'false'};
</script>
<script src="${ctx}/js/appointment.js"></script>
</body>
</html>
