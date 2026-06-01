<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Quên mật khẩu – Pet Clinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>
<body>
<div class="auth-wrapper">
  <div class="auth-card">
    <div class="auth-logo">Pet Clinic</div>
    <h2>Quên mật khẩu</h2>
    <p class="auth-desc">Nhập email đăng ký, chúng tôi sẽ gửi link đặt lại mật khẩu.</p>

    <c:if test="${not empty successMsg}">
      <div class="alert alert-success">${successMsg}</div>
    </c:if>
    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger">${errorMsg}</div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/forgot-password">
      <div class="form-group">
        <label for="email">Email</label>
        <input type="email" id="email" name="email" required
               placeholder="you@example.com" autofocus>
      </div>
      <button type="submit" class="btn-primary">Gửi link</button>
    </form>

    <p class="auth-footer">
      <a href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a>
    </p>
  </div>
</div>
</body>
</html>
