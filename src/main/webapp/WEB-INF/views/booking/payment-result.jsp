<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Kết quả thanh toán – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/booking.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="result-wrap">

  <c:choose>
    <%-- Determine result by polling invoice status --%>
    <c:when test="${not empty invoice and (invoice.status eq 'Paid' or invoice.status eq 'PartiallyPaid')}">

      <!-- SUCCESS -->
      <div class="result-icon result-icon--success">✓</div>
      <h1 class="result-title result-title--success">Thanh toán thành công!</h1>
      <p class="result-subtitle">
        <c:choose>
          <c:when test="${invoice.status eq 'Paid'}">Bạn đã thanh toán toàn bộ chi phí.</c:when>
          <c:otherwise>Tiền cọc đã được xác nhận. Vui lòng thanh toán phần còn lại tại phòng khám.</c:otherwise>
        </c:choose>
      </p>

      <div class="result-card">
        <div class="result-row"><span>Mã lịch hẹn</span><strong>#${appt.appointmentID}</strong></div>
        <div class="result-row"><span>Dịch vụ</span><strong>${appt.serviceName}</strong></div>
        <div class="result-row"><span>Thú cưng</span><strong>${appt.petName}</strong></div>
        <div class="result-row"><span>Thời gian</span>
          <%--
            Sửa lỗi: bản gốc dùng
              ${appt.appointmentDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))}
            Cú pháp này KHÔNG hợp lệ trong JSP EL — EL không cho phép gọi static
            method (DateTimeFormatter.ofPattern) hay method chain kiểu Java thuần.
            Điều này sẽ gây lỗi runtime (PropertyNotFoundException / ELException),
            khiến trang bị lỗi 500. Sửa lại dùng đúng các getter "formatted..."
            mà model Appointment đã cung cấp sẵn, nhất quán với các JSP khác trong
            cùng dự án (xem appointments.jsp, appointment-detail.jsp).
          --%>
          <strong>${appt.formattedAppointmentDate} lúc ${appt.formattedStartTime}</strong>
        </div>
        <div class="result-row"><span>Trạng thái</span>
          <span class="status-badge status-confirmed">Đã xác nhận</span>
        </div>
        <div class="result-row"><span>Thanh toán</span>
          <strong><fmt:formatNumber value="${invoice.totalAmount}" type="number" groupingUsed="true"/>₫</strong>
        </div>
      </div>

      <div class="result-email-note">
        Hóa đơn và thông tin lịch hẹn đã được gửi vào email của bạn.
        Chúng tôi sẽ nhắc nhở trước 48 giờ và 18 giờ.
      </div>

      <div class="result-actions">
        <a href="${ctx}/appointments/detail?id=${appt.appointmentID}" class="btn-result-primary">Xem chi tiết lịch hẹn</a>
        <a href="${ctx}/appointments" class="btn-result-secondary">Về danh sách lịch</a>
      </div>

    </c:when>
    <c:otherwise>

      <!-- PENDING / UNKNOWN -->
      <div class="result-icon result-icon--pending"></div>
      <h1 class="result-title result-title--pending">Đang chờ xác nhận thanh toán</h1>
      <p class="result-subtitle">
        Lịch hẹn đã được tạo và đang ở trạng thái <strong>Chờ xác nhận</strong>.
        Nếu bạn đã thanh toán, hệ thống sẽ cập nhật trong vài giây.
        Nếu chưa, bạn có thể thanh toán từ trang chi tiết lịch hẹn.
      </p>

      <c:if test="${not empty appt}">
        <div class="result-card">
          <div class="result-row"><span>Mã lịch hẹn</span><strong>#${appt.appointmentID}</strong></div>
          <div class="result-row"><span>Dịch vụ</span><strong>${appt.serviceName}</strong></div>
          <div class="result-row"><span>Thời gian</span>
            <%-- Sửa lỗi tương tự phần trên: thay .format(DateTimeFormatter...) bằng getter có sẵn --%>
            <strong>${appt.formattedAppointmentDate} lúc ${appt.formattedStartTime}</strong>
          </div>
          <div class="result-row"><span>Trạng thái</span>
            <span class="status-badge status-pending">Chờ xác nhận</span>
          </div>
        </div>
      </c:if>

      <div class="result-actions">
        <c:if test="${not empty appt}">
          <a href="${ctx}/appointments/detail?id=${appt.appointmentID}" class="btn-result-primary">Thanh toán ngay</a>
        </c:if>
        <a href="${ctx}/appointments" class="btn-result-secondary">Về danh sách lịch</a>
      </div>

      <!-- Auto-refresh to check payment status -->
      <c:if test="${not empty appt}">
        <script>
          window.APP_CTX = '${ctx}';
          window.PAYMENT_RESULT_APPT_ID = '${appt.appointmentID}';
        </script>
        <script src="${ctx}/js/payment.js"></script>
      </c:if>

    </c:otherwise>
  </c:choose>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>
