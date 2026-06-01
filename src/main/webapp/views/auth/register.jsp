<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Đăng ký – Pet Clinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>
<body>
<div class="auth-wrapper">
  <div class="auth-card">
    <div class="auth-logo">Pet Clinic</div>
    <h2>Tạo tài khoản</h2>

    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger">${errorMsg}</div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/register"
          novalidate id="registerForm">

      <div class="form-group">
        <label for="fullName">Họ và tên</label>
        <input type="text" id="fullName" name="fullName"
               value="${fullNameVal}" required placeholder="Nguyễn Văn A">
      </div>

      <div class="form-group">
        <label for="email">Email</label>
        <input type="email" id="email" name="email"
               value="${emailVal}" required placeholder="you@example.com">
      </div>

      <div class="form-group">
        <label for="phone">Số điện thoại</label>
        <input type="tel" id="phone" name="phone"
               value="${phoneVal}" placeholder="0901234567">
      </div>

      <div class="form-group">
        <label for="password">Mật khẩu <span class="hint">(tối thiểu 6 ký tự)</span></label>
        <input type="password" id="password" name="password"
               required minlength="6" placeholder="••••••••">
      </div>

      <div class="form-group">
        <label for="confirmPassword">Xác nhận mật khẩu</label>
        <input type="password" id="confirmPassword" name="confirmPassword"
               required placeholder="••••••••">
        <span class="field-error" id="confirmError"></span>
      </div>

      <button type="submit" class="btn-primary">Đăng ký</button>
    </form>

    <p class="auth-footer">
      Đã có tài khoản?
      <a href="${pageContext.request.contextPath}/login">Đăng nhập</a>
    </p>
  </div>
</div>
<script>
  document.getElementById('registerForm').addEventListener('submit', function(e) {
    const pw  = document.getElementById('password').value;
    const cpw = document.getElementById('confirmPassword').value;
    const err = document.getElementById('confirmError');
    if (pw !== cpw) {
      err.textContent = 'Mật khẩu xác nhận không khớp.';
      e.preventDefault();
    } else {
      err.textContent = '';
    }
  });
</script>
</body>
</html>
