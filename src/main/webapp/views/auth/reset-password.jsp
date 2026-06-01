<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Đặt lại mật khẩu – Pet Clinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>
<body>
<div class="auth-wrapper">
  <div class="auth-card">
    <div class="auth-logo">Pet Clinic</div>
    <h2>Đặt lại mật khẩu</h2>

    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger">${errorMsg}</div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/reset-password"
          id="resetForm">
      <input type="hidden" name="token" value="${token}">

      <div class="form-group">
        <label for="newPassword">Mật khẩu mới <span class="hint">(tối thiểu 6 ký tự)</span></label>
        <input type="password" id="newPassword" name="newPassword"
               required minlength="6" autofocus placeholder="••••••••">
      </div>

      <div class="form-group">
        <label for="confirmPassword">Xác nhận mật khẩu</label>
        <input type="password" id="confirmPassword" name="confirmPassword"
               required placeholder="••••••••">
        <span class="field-error" id="confirmError"></span>
      </div>

      <button type="submit" class="btn-primary">Cập nhật mật khẩu</button>
    </form>
  </div>
</div>
<script>
  document.getElementById('resetForm').addEventListener('submit', function(e) {
    const pw  = document.getElementById('newPassword').value;
    const cpw = document.getElementById('confirmPassword').value;
    const err = document.getElementById('confirmError');
    if (pw !== cpw) {
      err.textContent = 'Mật khẩu xác nhận không khớp.';
      e.preventDefault();
    }
  });
</script>
</body>
</html>
