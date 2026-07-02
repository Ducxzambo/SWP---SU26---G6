<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Thanh toán – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/booking.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="pay-wrap">

  <div class="pay-header">
    <h1>Chọn hình thức thanh toán</h1>
    <p>Thanh toán an toàn qua VietQR · PayOS</p>
  </div>

  <!-- Pricing summary -->
  <div class="pay-summary-box">
    <div class="pay-sum-row">
      <span>Tổng chi phí dịch vụ</span>
      <strong><fmt:formatNumber value="${totalPrice}" type="number" groupingUsed="true"/>₫</strong>
    </div>
    <div class="pay-sum-row pay-sum-row--deposit">
      <span>Tiền cọc tối thiểu <c:if test="${isInpatient}">(200.000đ nội trú)</c:if><c:if test="${!isInpatient}">(50.000đ đặt cọc)</c:if></span>
      <strong><fmt:formatNumber value="${depositAmount}" type="number" groupingUsed="true"/>₫</strong>
    </div>
  </div>

  <!-- Two payment options -->
  <div class="pay-options">

    <!-- Full payment -->
    <!-- <div class="pay-option" id="optFull" onclick="selectPay('full', this)">
      <div class="pay-opt-check" id="checkFull"></div>
      <div class="pay-opt-icon"></div>
      <div class="pay-opt-body">
        <div class="pay-opt-title">Thanh toán toàn bộ</div>
        <div class="pay-opt-desc">Thanh toán đầy đủ ngay bây giờ. Lịch hẹn được xác nhận ngay.</div>
        <div class="pay-opt-amount" id="amtFull">
          <fmt:formatNumber value="${totalPrice}" type="number" groupingUsed="true"/>₫
        </div>
      </div>
    </div> -->

    <!-- Partial payment (deposit) -->
    <div class="pay-option" id="optPartial" onclick="selectPay('partial', this)">
      <div class="pay-opt-check" id="checkPartial"></div>
      <div class="pay-opt-icon"></div>
      <div class="pay-opt-body">
        <div class="pay-opt-title">Đặt cọc trước</div>
        <div class="pay-opt-desc">
          Chỉ cần đặt cọc để giữ chỗ.
          <c:if test="${isInpatient}">Số tiền cọc cố định cho dịch vụ nội trú.</c:if>
          <c:if test="${!isInpatient}">Số tiền còn lại thanh toán tại phòng khám.</c:if>
        </div>
        <div class="pay-opt-amount pay-opt-amount--deposit" id="amtPartial">
          <fmt:formatNumber value="${depositAmount}" type="number" groupingUsed="true"/>₫
          <span class="pay-opt-remaining">
          <c:set var="ctx" value="${pageContext.request.contextPath}"/>
          <c:if test="${totalPrice - depositAmount > 0}">
            (còn lại <fmt:formatNumber value="${totalPrice - depositAmount}" type="number" groupingUsed="true"/>₫ tại quầy)
          </c:if>
          <c:if test="${totalPrice - depositAmount < 0}">
             (số tiền nộp dư <fmt:formatNumber value="${depositAmount - totalPrice}" type="number" groupingUsed="true"/>₫ sẽ được refund tại quầy)
          </c:if>
          </span>
        </div>
      </div>
    </div>

  </div><!-- /pay-options -->

  <!-- QR info note -->
  <div class="pay-qr-note">
    <div class="pay-qr-note-icon"></div>
    <div>
      <strong>Thanh toán bằng mã QR động (VietQR)</strong><br>
      Sau khi chọn hình thức thanh toán, bạn sẽ được chuyển đến trang QR của PayOS.
      Quét mã bằng app ngân hàng và xác nhận chuyển khoản — lịch hẹn sẽ được xác nhận ngay sau 2–3 giây.
    </div>
  </div>

  <!-- Submit -->
  <form action="${ctx}/booking/payment" method="post" id="payForm">
    <input type="hidden" name="payType" id="payTypeInput" value="">
    <button type="submit" class="btn-pay" id="btnPay" disabled>
      Tiến hành thanh toán
    </button>
  </form>

  <a href="${ctx}/appointments/detail?id=${apptId}" class="pay-skip-link">
    Bỏ qua, thanh toán sau (lịch hẹn sẽ ở trạng thái Chờ)
  </a>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script src="${ctx}/js/payment.js"></script>
</body>
</html>
