<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Đăng ký – PetClinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body>

<div class="auth-card">

  <div class="auth-header">
    <div class="logo"><span class="paw">🐾</span>Pet<span>Clinic</span></div>
    <h2>Tạo tài khoản mới</h2>
  </div>

  <div class="auth-body">

    <!-- Step indicator -->
    <div class="step-indicator">
      <div class="step-dot active"></div>
      <div class="step-dot"></div>
    </div>

    <!-- Error -->
    <c:if test="${not empty requestScope.error}">
      <div class="alert alert-error">
        <span class="alert-icon">!</span>
        <span>${requestScope.error}</span>
      </div>
    </c:if>

    <form action="${pageContext.request.contextPath}/auth/register" method="post"
          id="registerForm" novalidate>

      <div class="form-group">
        <label for="fullName">Họ và tên <span style="color:var(--red-err)">*</span></label>
        <div class="input-wrap">
          <span class="input-icon">👤</span>
          <input type="text" id="fullName" name="fullName" class="form-control"
                 placeholder="Nguyễn Văn A"
                 value="<c:out value='${requestScope.fullName}'/>"
                 autocomplete="name" required>
        </div>
      </div>

      <div class="form-group">
        <label for="email">Email <span style="color:var(--red-err)">*</span></label>
        <div class="input-wrap">
          <span class="input-icon">✉</span>
          <input type="email" id="email" name="email" class="form-control"
                 placeholder="email@example.com"
                 value="<c:out value='${requestScope.email}'/>"
                 autocomplete="email" required>
        </div>
        <div style="font-size:12px;color:var(--warm-gray);margin-top:5px;">
          ✉ Mã xác minh sẽ được gửi đến email này
        </div>
      </div>

      <div class="form-group">
        <label for="phone">Số điện thoại</label>
        <div class="input-wrap">
          <span class="input-icon">📱</span>
          <input type="tel" id="phone" name="phone" class="form-control"
                 placeholder="09xxxxxxxx (không bắt buộc)"
                 value="<c:out value='${requestScope.phone}'/>"
                 autocomplete="tel">
        </div>
        <div style="font-size:12px;color:var(--warm-gray);margin-top:5px;">
          📱 Nếu nhập, mã xác minh SMS sẽ được gửi đến số này
        </div>
      </div>

      <div class="form-group">
        <label for="password">Mật khẩu <span style="color:var(--red-err)">*</span></label>
        <div class="input-wrap">
          <span class="input-icon">🔒</span>
          <input type="password" id="password" name="password" class="form-control"
                 placeholder="Tối thiểu 6 ký tự" autocomplete="new-password" required>
          <button type="button" class="toggle-pwd" id="togglePwd"
                  onclick="togglePassword('password','togglePwd')">👁</button>
        </div>

        <!-- Strength bar -->
        <div class="pwd-strength">
          <div class="pwd-strength-bar"><div class="pwd-strength-fill" id="pwdBar"></div></div>
          <span class="pwd-strength-label" id="pwdLabel"></span>
        </div>

        <!-- Requirements checklist -->
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
                 placeholder="Nhập lại mật khẩu" autocomplete="new-password" required>
          <button type="button" class="toggle-pwd" id="toggleConfirm"
                  onclick="togglePassword('confirmPassword','toggleConfirm')">👁</button>
        </div>
        <div id="confirmMsg" style="font-size:12px;margin-top:5px;"></div>
      </div>

      <button type="submit" class="btn btn-primary" id="submitBtn">
        Tiếp theo – Xác minh tài khoản →
      </button>
    </form>

    <div class="auth-footer">
      Đã có tài khoản? <a href="${pageContext.request.contextPath}/auth/login">Đăng nhập</a>
    </div>
  </div>
</div>

<script src="${pageContext.request.contextPath}/js/auth.js"></script>
<script>
  initPasswordStrength('password', 'pwdBar', 'pwdLabel', 'pwdReqs');
  formatVnPhone(document.getElementById('phone'));

  // Confirm password live check
  const pwd = document.getElementById('password');
  const cfm = document.getElementById('confirmPassword');
  const msg = document.getElementById('confirmMsg');
  cfm.addEventListener('input', () => {
    if (!cfm.value) { msg.textContent = ''; return; }
    const match = pwd.value === cfm.value;
    msg.textContent = match ? '✓ Mật khẩu khớp' : '✗ Mật khẩu không khớp';
    msg.style.color = match ? 'var(--green-500)' : 'var(--red-err)';
  });

  // Client-side validation
  document.getElementById('registerForm').addEventListener('submit', function(e) {
    const fullName = document.getElementById('fullName').value.trim();
    const email    = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const confirm  = document.getElementById('confirmPassword').value;
    const phone    = document.getElementById('phone').value.trim();

    if (!fullName || !email || !password) {
      e.preventDefault();
      return alert('Vui lòng nhập đầy đủ các trường bắt buộc.');
    }
    if (password !== confirm) {
      e.preventDefault();
      return alert('Mật khẩu xác nhận không khớp.');
    }
    const strongEnough = /(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{6,}/.test(password);
    if (!strongEnough) {
      e.preventDefault();
      return alert('Mật khẩu chưa đủ mạnh. Vui lòng kiểm tra các yêu cầu bên dưới.');
    }
    if (phone && !/^0[3-9]\d{8}$/.test(phone)) {
      e.preventDefault();
      return alert('Số điện thoại không đúng định dạng Việt Nam (VD: 0912345678).');
    }
  });
</script>
</body>
</html>
