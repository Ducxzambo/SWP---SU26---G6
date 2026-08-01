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
    <div class="logo"><span class="paw"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="7" cy="8" r="2"/><circle cx="12" cy="5" r="2"/><circle cx="17" cy="8" r="2"/><path d="M12 11c-3 0-5 2-5 5 0 2 1 3 3 2l2-1 2 1c2 1 3 0 3-2 0-3-2-5-5-5z"/></svg></span>Pet<span>Clinic</span></div>
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
          <span class="input-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></svg></span>
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
