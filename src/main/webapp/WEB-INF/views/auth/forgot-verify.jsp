<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Nhập mã xác minh – PetClinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body>

<div class="auth-card">

  <div class="auth-header">
    <div class="logo"><span class="paw">🐾</span>Pet<span>Clinic</span></div>
    <h2>Nhập mã xác minh</h2>
  </div>

  <div class="auth-body">

    <!-- Steps: 2 active -->
    <div class="step-indicator">
      <div class="step-dot"></div>
      <div class="step-dot active"></div>
      <div class="step-dot"></div>
    </div>

    <c:if test="${not empty requestScope.error}">
      <div class="alert alert-error">
        <span class="alert-icon">!</span>
        <span>${requestScope.error}</span>
      </div>
    </c:if>

    <div class="info-box">
      <strong>📨 Mã đã được gửi</strong>
      Mã xác minh 6 chữ số đã được gửi đến
      <span class="highlight">${sessionScope.forgot_identifier}</span>.
      <br>Mã có hiệu lực trong <strong>10 phút</strong>.
    </div>

    <form action="${pageContext.request.contextPath}/auth/forgot/verify"
          method="post" id="verifyForm" novalidate>

      <div class="form-group">
        <label>Mã xác minh</label>
        <div class="otp-input-group" id="otpGroup">
          <input class="otp-digit" type="text">
          <input class="otp-digit" type="text">
          <input class="otp-digit" type="text">
          <input class="otp-digit" type="text">
          <input class="otp-digit" type="text">
          <input class="otp-digit" type="text">
        </div>
        <input type="hidden" id="otp" name="otp" class="otp-hidden">
      </div>

      <button type="submit" class="btn btn-primary" id="verifyBtn" disabled>
        Xác minh →
      </button>
    </form>

    <div class="resend-wrap" style="margin-top:20px;">
      Chưa nhận được mã?
      <span id="resend-timer">60s</span>
      <a id="resendLink" style="display:none"
         data-url="${pageContext.request.contextPath}/auth/forgot/resend-otp"
         onclick="resendOtp(this)">Gửi lại mã</a>
    </div>

    <div class="auth-footer" style="margin-top:16px;">
      <a href="${pageContext.request.contextPath}/auth/forgot">← Đổi email/số điện thoại</a>
    </div>
  </div>
</div>

<script src="${pageContext.request.contextPath}/js/auth.js"></script>
<script>
  initOtpInputs('otpGroup', 'otp');
  initResendCountdown('resend-timer', 'resendLink', 60);

  const verifyBtn = document.getElementById('verifyBtn');
  const otpInput  = document.getElementById('otp');

  // Watch OTP input changes to enable/disable submit
  document.getElementById('otpGroup').querySelectorAll('.otp-digit').forEach(d => {
    d.addEventListener('input', () => {
      verifyBtn.disabled = otpInput.value.length !== 6;
    });
  });

  document.getElementById('verifyForm').addEventListener('submit', function(e) {
    if (otpInput.value.length !== 6) {
      e.preventDefault();
      alert('Vui lòng nhập đầy đủ 6 chữ số.');
    }
  });
</script>
</body>
</html>
