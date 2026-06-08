<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập nhân viên – PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
    <style>
        .role-badge {
            display: inline-block;
            background: rgba(61, 171, 107, .15);
            color: var(--green-400);
            border: 1px solid rgba(61, 171, 107, .25);
            border-radius: 6px;
            font-size: 11px;
            font-weight: 600;
            letter-spacing: .6px;
            text-transform: uppercase;
            padding: 3px 10px;
            margin-bottom: 4px;
        }
    </style>
</head>
<body>

<div class="auth-card">

    <div class="auth-header">
        <div class="logo"><span class="paw">🐾</span>Pet<span>Clinic</span></div>
        <h2>Cổng nhân viên</h2>
    </div>

    <div class="auth-body">

        <c:if test="${not empty sessionScope.flashSuccess}">
            <div class="alert alert-success">
                <span class="alert-icon">✓</span>
                <span>${sessionScope.flashSuccess}</span>
            </div>
            <c:remove var="flashSuccess" scope="session"/>
        </c:if>

        <c:if test="${not empty requestScope.error}">
            <div class="alert alert-error">
                <span class="alert-icon">!</span>
                <span>${requestScope.error}</span>
            </div>
        </c:if>

        <div class="info-box">
            <strong>🏥 Dành cho nhân viên</strong>
            Trang này chỉ dành cho Bác sĩ, Lễ tân và nhân viên nội bộ.
        </div>

        <form action="${pageContext.request.contextPath}/auth/staff/login"
              method="post" id="staffLoginForm" novalidate>

            <div class="form-group">
                <label for="email">Email nhân viên</label>
                <div class="input-wrap">
                    <span class="input-icon">✉</span>
                    <input type="email" id="email" name="email" class="form-control"
                           placeholder="staff@petclinic.com"
                           value="<c:out value='${requestScope.email}'/>"
                           autocomplete="username" required autofocus>
                </div>
            </div>

            <div class="form-group">
                <label for="password">Mật khẩu</label>
                <div class="input-wrap">
                    <span class="input-icon">🔒</span>
                    <input type="password" id="password" name="password" class="form-control"
                           placeholder="••••••••" autocomplete="current-password" required>
                    <button type="button" class="toggle-pwd" id="togglePwd"
                            onclick="togglePassword('password','togglePwd')">👁
                    </button>
                </div>
            </div>

            <button type="submit" class="btn btn-primary">Đăng nhập →</button>
        </form>

        <div class="auth-footer">
            <a href="${pageContext.request.contextPath}/auth/login">← Trang khách hàng</a>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/auth.js"></script>
<script>
    document.getElementById('staffLoginForm').addEventListener('submit', function (e) {
        if (!document.getElementById('email').value.trim() ||
            !document.getElementById('password').value) {
            e.preventDefault();
            alert('Vui lòng nhập đầy đủ thông tin.');
        }
    });
</script>
</body>
</html>
