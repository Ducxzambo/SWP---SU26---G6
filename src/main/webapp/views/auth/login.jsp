<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Đăng nhập – Pet Clinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>
<body>
<div class="auth-wrapper">
  <div class="auth-card">
    <div class="auth-logo">Pet Clinic</div>
    <h2>Đăng nhập</h2>

    <%-- Thông báo sau khi register / reset password thành công --%>
    <c:if test="${param.registered eq '1'}">
      <div class="alert alert-success">Đăng ký thành công! Vui lòng đăng nhập.</div>
    </c:if>
    <c:if test="${param.reset eq '1'}">
      <div class="alert alert-success">Mật khẩu đã được cập nhật!</div>
    </c:if>
    <c:if test="${param.logout eq '1'}">
      <div class="alert alert-info">Bạn đã đăng xuất.</div>
    </c:if>

    <%-- Lỗi đăng nhập --%>
    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger">${errorMsg}</div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/login" novalidate>
      <div class="form-group">
        <label for="email">Email</label>
        <input type="email" id="email" name="email"
               value="${emailVal}" required autofocus
               placeholder="you@example.com">
      </div>

      <div class="form-group">
        <label for="password">Mật khẩu</label>
        <input type="password" id="password" name="password" required
               placeholder="••••••••">
      </div>

      <div class="form-row">
        <label class="checkbox-label">
          <input type="checkbox" name="remember"> Ghi nhớ đăng nhập
        </label>
        <a href="${pageContext.request.contextPath}/forgot-password" class="link-muted">
          Quên mật khẩu?
        </a>
      </div>

      <button type="submit" class="btn-primary">Đăng nhập</button>
    </form>

    <p class="auth-footer">
      Chưa có tài khoản?
      <a href="${pageContext.request.contextPath}/register">Đăng ký ngay</a>
    </p>
  </div>
</div>
</body>
</html>
