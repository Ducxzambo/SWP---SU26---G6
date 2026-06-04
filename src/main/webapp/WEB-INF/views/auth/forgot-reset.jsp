<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Đặt lại mật khẩu – PetClinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body>

<div class="auth-card">

  <div class="auth-header">
    <div class="logo"><span class="paw">🐾</span>Pet<span>Clinic</span></div>
    <h2>Đặt lại mật khẩu</h2>
  </div>

  <div class="auth-body">

    <!-- Steps: 3 active -->
    <div class="step-indicator">
      <div class="step-dot"></div>
      <div class="step-dot"></div>
      <div class="step-dot active"></div>
    </div>

    <c:if test="${not empty requestScope.error}">
      <div class="alert alert-error">
        <span class="alert-icon">!</span>
        <span>${requestScope.error}</span>
      </div>
    </c:if>

    <div class="info-box">
      <strong>🔐 Tạo mật khẩu mới</strong>
      Mật khẩu mới phải khác mật khẩu cũ và đáp ứng các yêu cầu bên dưới.
    </div>

    <form action="${pageContext.request.contextPath}/auth/forgot/reset"
          method="post" id="resetForm" novalidate>

      <div class="form-group">
        <label for="password">Mật khẩu mới <span style="color:var(--red-err)">*</span></label>
        <div class="input-wrap">
          <span class="input-icon">🔒</span>
          <input type="password" id="password" name="password" class="form-control"
                 placeholder="Mật khẩu mới" autocomplete="new-password" required>
          <button type="button" class="toggle-pwd" id="togglePwd"
                  onclick="togglePassword('password','togglePwd')">👁</button>
        </div>
        <div class="pwd-strength">
          <div class="pwd-strength-bar"><div class="pwd-strength-fill" id="pwdBar"></div></div>
          <span class="pwd-strength-label" id="pwdLabel"></span>
        </div>
        <ul class="pwd-requirements" id="pwdReqs">
          <li data-req="req-len">6+ ký tự</li>
          <li data-req="req-upper">Chữ in hoa</li>
          <li data-req="req-lower">Chữ thường</li>
          <li data-req="req-digit">Chữ số</li>
          <li data-req="req-spec">Ký tự đặc biệt</li>
        </ul>
      </div>

      <div class="form-group">
        <label for="confirmPassword">Xác nhận mật khẩu <span style="color:var(--red-err)">*</span></label>
        <div class="input-wrap">
          <span class="input-icon">🔒</span>
          <input type="password" id="confirmPassword" name="confirmPassword" class="form-control"
                 placeholder="Nhập lại mật khẩu mới" autocomplete="new-password" required>
          <button type="button" class="toggle-pwd" id="toggleConfirm"
                  onclick="togglePassword('confirmPassword','toggleConfirm')">👁</button>
        </div>
        <div id="confirmMsg" style="font-size:12px;margin-top:5px;"></div>
      </div>

      <button type="submit" class="btn btn-primary">Đặt lại mật khẩu ✓</button>
    </form>

    <div class="auth-footer">
      <a href="${pageContext.request.contextPath}/auth/login">← Quay lại đăng nhập</a>
    </div>
  </div>
</div>

<script src="${pageContext.request.contextPath}/js/auth.js"></script>
<script>
  initPasswordStrength('password', 'pwdBar', 'pwdLabel', 'pwdReqs');

  const pwd = document.getElementById('password');
  const cfm = document.getElementById('confirmPassword');
  const msg = document.getElementById('confirmMsg');
  cfm.addEventListener('input', () => {
    if (!cfm.value) { msg.textContent = ''; return; }
    const match = pwd.value === cfm.value;
    msg.textContent = match ? '✓ Mật khẩu khớp' : '✗ Mật khẩu không khớp';
    msg.style.color = match ? 'var(--green-500)' : 'var(--red-err)';
  });

  document.getElementById('resetForm').addEventListener('submit', function(e) {
    const password = pwd.value;
    const confirm  = cfm.value;
    if (!password || !confirm) {
      e.preventDefault();
      return alert('Vui lòng nhập đầy đủ thông tin.');
    }
    if (password !== confirm) {
      e.preventDefault();
      return alert('Mật khẩu xác nhận không khớp.');
    }
    const strongEnough = /(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{6,}/.test(password);
    if (!strongEnough) {
      e.preventDefault();
      return alert('Mật khẩu chưa đủ mạnh. Hãy kiểm tra danh sách yêu cầu.');
    }
  });

  // Focus new password
  document.getElementById('password').focus();
</script>
</body>
</html>
