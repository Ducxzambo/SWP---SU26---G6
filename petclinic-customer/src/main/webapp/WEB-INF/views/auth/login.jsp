<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Đăng nhập – PetClinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body>

<div class="auth-card">

  <!-- Header -->
  <div class="auth-header">
    <div class="logo"><span class="paw"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="7" cy="8" r="2"/><circle cx="12" cy="5" r="2"/><circle cx="17" cy="8" r="2"/><path d="M12 11c-3 0-5 2-5 5 0 2 1 3 3 2l2-1 2 1c2 1 3 0 3-2 0-3-2-5-5-5z"/></svg></span>Pet<span>Clinic</span></div>
    <h2>Đăng nhập tài khoản</h2>
  </div>

  <!-- Body -->
  <div class="auth-body">

    <!-- Flash success (from register / reset password) -->
    <c:if test="${not empty sessionScope.flashSuccess}">
      <div class="alert alert-success">
        <span class="alert-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg></span>
        <span>${sessionScope.flashSuccess}</span>
      </div>
      <c:remove var="flashSuccess" scope="session"/>
    </c:if>

    <!-- Error -->
    <c:if test="${not empty requestScope.error}">
      <div class="alert alert-error">
        <span class="alert-icon">!</span>
        <span>${requestScope.error}</span>
      </div>
    </c:if>

    <!-- OAuth error param -->
    <c:if test="${param.error == 'oauth_failed' || param.error == 'oauth_denied'}">
      <div class="alert alert-error">
        <span class="alert-icon">!</span>
        <span>Đăng nhập Google thất bại. Vui lòng thử lại.</span>
      </div>
    </c:if>

    <!-- Google login button -->
    <a href="${pageContext.request.contextPath}/auth/google" class="btn btn-google">
      <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" alt="Google">
      Tiếp tục với Google
    </a>

    <div class="divider">hoặc đăng nhập bằng tài khoản</div>

    <!-- Login form -->
    <form action="${pageContext.request.contextPath}/auth/login" method="post" id="loginForm" novalidate>

      <div class="form-group">
        <label for="identifier">Email hoặc SĐT</label>
        <div class="input-wrap">
          <span class="input-icon"></span>
          <input type="text" id="identifier" name="identifier" class="form-control"
                 placeholder="email@example.com hoặc 0xxxxxxxxx"
                 value="<c:out value='${requestScope.identifier}'/>"
                 autocomplete="username" required>
        </div>
      </div>

      <div class="form-group">
        <label for="password">Mật khẩu</label>
        <div class="input-wrap">
          <span class="input-icon"></span>
          <input type="password" id="password" name="password" class="form-control"
                 placeholder="••••••••" autocomplete="current-password" required>
          <button type="button" class="toggle-pwd" id="togglePwd"
                  onclick="togglePassword('password','togglePwd')" title="Hiện/ẩn mật khẩu"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg></button>
        </div>
      </div>

      <a href="${pageContext.request.contextPath}/auth/forgot" class="forgot-link">Quên mật khẩu?</a>

      <div class="form-check">
        <input type="checkbox" id="rememberMe" name="rememberMe">
        <label for="rememberMe">Ghi nhớ đăng nhập trong 30 ngày</label>
      </div>

      <button type="submit" class="btn btn-primary">Đăng nhập</button>
    </form>

    <div class="auth-footer">
      Chưa có tài khoản?
      <a href="${pageContext.request.contextPath}/auth/register">Đăng ký ngay</a>
    </div>
  </div>
</div>

<script src="${pageContext.request.contextPath}/js/auth.js"></script>
<script>
  // Focus identifier field on load
  document.getElementById('identifier').focus();

  // Client-side validation
  document.getElementById('loginForm').addEventListener('submit', function(e) {
    const id  = document.getElementById('identifier').value.trim();
    const pwd = document.getElementById('password').value;
    if (!id || !pwd) {
      e.preventDefault();
      alert('Vui lòng nhập đầy đủ thông tin đăng nhập.');
    }
  });
</script>
</body>
</html>
