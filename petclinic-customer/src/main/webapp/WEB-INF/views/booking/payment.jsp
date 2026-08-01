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
    <c:if test="${isInpatient}">
    <div class="pay-sum-row pay-sum-row--deposit">
      <span>Tiền cọc nội trú (cố định)</span>
      <strong><fmt:formatNumber value="${depositAmount}" type="number" groupingUsed="true"/>₫</strong>
    </div>
    </c:if>
  </div>

  <div class="pay-options">

    <c:if test="${!isInpatient}">
    <div class="pay-option" id="optFull" onclick="selectPay('full', this)">
      <div class="pay-opt-check" id="checkFull"></div>
      <div class="pay-opt-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="6" width="18" height="12" rx="2"/><path d="M3 10h18M7 14h3"/></svg></div>
      <div class="pay-opt-body">
        <div class="pay-opt-title">Trả toàn bộ</div>
        <div class="pay-opt-desc">Thanh toán 100% chi phí ngay khi đặt lịch.</div>
        <div class="pay-opt-amount" id="amtFull">
          <fmt:formatNumber value="${totalPrice}" type="number" groupingUsed="true"/>₫
        </div>
      </div>
    </div>

    <div class="pay-option" id="optPartial" onclick="selectPay('partial', this)">
      <div class="pay-opt-check" id="checkPartial"></div>
      <div class="pay-opt-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8"/><path d="M12 8v8M9 10h5a2 2 0 0 1 0 4H10"/></svg></div>
      <div class="pay-opt-body">
        <div class="pay-opt-title">Đặt cọc 50%</div>
        <div class="pay-opt-desc">Đặt cọc trước 50%, phần còn lại thanh toán vào ngày khám.</div>
        <div class="pay-opt-amount pay-opt-amount--deposit" id="amtPartial">
          <fmt:formatNumber value="${depositAmount}" type="number" groupingUsed="true"/>₫
          <span class="pay-opt-remaining">
            (còn lại <fmt:formatNumber value="${totalPrice - depositAmount}" type="number" groupingUsed="true"/>₫)
          </span>
        </div>
      </div>
    </div>
    </c:if>

    <c:if test="${isInpatient}">
    <div class="pay-option" id="optPartial" onclick="selectPay('partial', this)">
      <div class="pay-opt-check" id="checkPartial"></div>
      <div class="pay-opt-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 21V5h16v16M9 9h6M12 6v6M8 21v-4h8v4"/></svg></div>
      <div class="pay-opt-body">
        <div class="pay-opt-title">Đặt cọc trước</div>
        <div class="pay-opt-desc">Cọc cố định cho dịch vụ nội trú. Chi phí thực tế tính khi xuất viện.</div>
        <div class="pay-opt-amount pay-opt-amount--deposit" id="amtPartial">
          <fmt:formatNumber value="${depositAmount}" type="number" groupingUsed="true"/>₫
        </div>
      </div>
    </div>
    </c:if>

  </div><!-- /pay-options -->

  <!-- QR info note -->
  <div class="pay-qr-note">
    <div class="pay-qr-note-icon"></div>
    <div>
      <strong>Thanh toán bằng mã QR động</strong><br>
      Sau khi chọn thanh toán, bạn sẽ được chuyển đến trang QR của PayOS.
      Quét mã bằng app ngân hàng và xác nhận chuyển khoản - lịch hẹn sẽ ở trạng thái <strong>Xác nhận</strong>.
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
