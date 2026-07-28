<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Xác minh email mới – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/profile.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="account-wrap" style="max-width:480px;">

  <div class="account-head" style="text-align:center;">
    <h1>Xác minh email mới</h1>
    <p>Nhập mã 6 chữ số vừa được gửi đến<br><strong>${pendingEmail}</strong></p>
  </div>

  <div class="account-card">
    <div class="account-card-body">

      <c:if test="${not empty requestScope.error}">
        <div class="account-alert-inline">✗ ${error}</div>
      </c:if>

      <form action="${ctx}/profile/email/verify" method="post" novalidate id="verifyForm">
        <div class="otp-input-group" id="otpGroup">
          <input type="text" class="otp-digit" inputmode="numeric" pattern="[0-9]" maxlength="1">
          <input type="text" class="otp-digit" inputmode="numeric" pattern="[0-9]" maxlength="1">
          <input type="text" class="otp-digit" inputmode="numeric" pattern="[0-9]" maxlength="1">
          <input type="text" class="otp-digit" inputmode="numeric" pattern="[0-9]" maxlength="1">
          <input type="text" class="otp-digit" inputmode="numeric" pattern="[0-9]" maxlength="1">
          <input type="text" class="otp-digit" inputmode="numeric" pattern="[0-9]" maxlength="1">
        </div>
        <input type="hidden" name="otp" id="otpHidden">

        <div class="account-info-box">Mã có hiệu lực trong 10 phút.</div>

        <div class="form-actions" style="justify-content:center;margin-top:18px;">
          <button type="submit" class="btn-save" style="width:100%;">Xác nhận</button>
        </div>
      </form>

      <div class="resend-wrap">
        Không nhận được mã?
        <span id="resend-timer">60s</span>
        <a id="resendLink" data-url="${ctx}/profile/email/resend" onclick="resendOtp(this)">Gửi lại mã</a>
      </div>

    </div>
  </div>

  <div style="text-align:center;">
    <a href="${ctx}/profile" class="account-back-link">← Huỷ và quay lại hồ sơ</a>
  </div>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
<script src="${ctx}/js/auth.js"></script>
<script>
  initOtpInputs('otpGroup', 'otpHidden');
  initResendCountdown('resend-timer', 'resendLink', 60);

  document.getElementById('verifyForm').addEventListener('submit', function (e) {
    var otp = document.getElementById('otpHidden').value;
    if (otp.length !== 6) {
      e.preventDefault();
      alert('Vui lòng nhập đủ 6 chữ số của mã xác minh.');
    }
  });
</script>
</body>
</html>
