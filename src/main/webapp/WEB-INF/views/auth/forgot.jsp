<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Quên mật khẩu – PetClinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body>

<div class="auth-card">

  <div class="auth-header">
    <div class="logo"><span class="paw">🐾</span>Pet<span>Clinic</span></div>
    <h2>Khôi phục mật khẩu</h2>
  </div>

  <div class="auth-body">

    <!-- Steps: 1 active -->
    <div class="step-indicator">
      <div class="step-dot active"></div>
      <div class="step-dot"></div>
      <div class="step-dot"></div>
    </div>

    <c:if test="${not empty requestScope.error}">
      <div class="alert alert-error">
        <span class="alert-icon">!</span>
        <span>${requestScope.error}</span>
      </div>
    </c:if>

    <div class="info-box">
      <strong>Quên mật khẩu?</strong>
      Nhập email đã đăng ký. Chúng tôi sẽ gửi mã xác minh để bạn đặt lại mật khẩu.
    </div>

    <form action="${pageContext.request.contextPath}/auth/forgot"
          method="post" id="forgotForm" novalidate>

      <div class="form-group">
        <label for="email">Email</label>
        <div class="input-wrap">
          <span class="input-icon">✉</span>
          <input type="email" id="email" name="email" class="form-control"
                 placeholder="email@example.com"
                 autocomplete="username" required autofocus>
        </div>
      </div>

      <button type="submit" class="btn btn-primary">Gửi mã xác minh →</button>
    </form>

    <div class="auth-footer">
      <a href="${pageContext.request.contextPath}/auth/login">← Quay lại đăng nhập</a>
    </div>
  </div>
</div>

<script src="${pageContext.request.contextPath}/js/auth.js"></script>
<script>
  document.getElementById('forgotForm').addEventListener('submit', function(e) {
    const val = document.getElementById('identifier').value.trim();
    if (!val) { e.preventDefault(); alert('Vui lòng nhập email hoặc số điện thoại.'); }
  });
</script>
</body>
</html>
