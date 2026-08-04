<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Xác minh OTP – PetClinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body>

<div class="auth-card">

  <div class="auth-header">
    <div class="logo"><span class="paw">🐾</span>Pet<span>Clinic</span></div>
    <h2>Xác minh tài khoản</h2>
  </div>

  <div class="auth-body">

    <!-- Step indicator -->
    <div class="step-indicator">
      <div class="step-dot"></div>
      <div class="step-dot active"></div>
    </div>

    <c:if test="${not empty requestScope.error}">
      <div class="alert alert-error">
        <span class="alert-icon">!</span>
        <span>${requestScope.error}</span>
      </div>
    </c:if>

    <!-- Info -->
    <div class="info-box">
      <strong>Kiểm tra hộp thư của bạn</strong>
      Chúng tôi đã gửi mã xác minh 6 chữ số đến
      <span class="highlight">${sessionScope.pendingReg_email}</span>.
      <br>Mã có hiệu lực trong <strong>10 phút</strong>.
    </div>

    <form action="${pageContext.request.contextPath}/auth/register/verify"
          method="post" id="verifyForm" novalidate>

      <div class="form-group">
        <label>Mã xác minh Email</label>
        <div class="otp-input-group" id="emailOtpGroup">
          <input class="otp-digit" type="text">
          <input class="otp-digit" type="text">
          <input class="otp-digit" type="text">
          <input class="otp-digit" type="text">
          <input class="otp-digit" type="text">
          <input class="otp-digit" type="text">
        </div>
        <input type="hidden" id="emailOtp" name="emailOtp" class="otp-hidden">
      </div>

      <button type="submit" class="btn btn-primary" id="verifyBtn" disabled>
        Xác minh và tạo tài khoản
      </button>
    </form>

    <!-- Resend -->
    <div class="resend-wrap">
      Chưa nhận được mã?
      <span id="resend-timer">60s</span>
      <a id="resendLink" style="display:none"
         data-url="${pageContext.request.contextPath}/auth/register/resend-otp"
         onclick="resendOtp(this)">Gửi lại mã</a>
    </div>

    <div class="auth-footer" style="margin-top:16px;">
      <a href="${pageContext.request.contextPath}/auth/register">← Quay lại đăng ký</a>
    </div>
  </div>
</div>

<script src="${pageContext.request.contextPath}/js/auth.js"></script>
<script>
  // Khởi tạo OTP input + truyền callback để enable nút submit
  initOtpInputs('emailOtpGroup', 'emailOtp', onOtpChange);
  initResendCountdown('resend-timer', 'resendLink', 60);

  const verifyBtn = document.getElementById('verifyBtn');
  const emailOtp  = document.getElementById('emailOtp');

  function onOtpChange() {
    verifyBtn.disabled = emailOtp.value.length !== 6;
  }

  document.getElementById('verifyForm').addEventListener('submit', function(e) {
    if (emailOtp.value.length !== 6) {
      e.preventDefault();
      alert('Vui lòng nhập đầy đủ mã xác minh 6 chữ số.');
    }
  });
</script>
</body>
</html>
