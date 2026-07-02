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
          <span class="input-icon"></span>
          <input type="text" id="fullName" name="fullName" class="form-control"
                 placeholder="Nguyễn Văn A"
                 value="<c:out value='${requestScope.fullName}'/>"
                 autocomplete="name" required>
        </div>
      </div>

      <div class="form-group">
        <label for="email">Email <span style="color:var(--red-err)">*</span></label>
        <div class="input-wrap">
          <span class="input-icon"></span>
          <input type="email" id="email" name="email" class="form-control"
                 placeholder="email@example.com"
                 value="<c:out value='${requestScope.email}'/>"
                 autocomplete="email" required>
        </div>
        <div id="emailMsg" style="font-size:12px;margin-top:5px;color:var(--warm-gray);">
          Mã xác minh sẽ được gửi đến email này
        </div>
      </div>

      <div class="form-group">
        <label for="phone">Số điện thoại</label>
        <div class="input-wrap">
          <span class="input-icon"></span>
          <input type="tel" id="phone" name="phone" class="form-control"
                 placeholder="09xxxxxxxx (không bắt buộc)"
                 value="<c:out value='${requestScope.phone}'/>"
                 autocomplete="tel">
        </div>
        <div id="phoneMsg" style="font-size:12px;margin-top:5px;color:var(--warm-gray);">
          Nhập đúng 10 chữ số, bắt đầu bằng 0
        </div>
      </div>

      <div class="form-group">
        <label for="password">Mật khẩu <span style="color:var(--red-err)">*</span></label>
        <div class="input-wrap">
          <span class="input-icon"></span>
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
          <span class="input-icon"></span>
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

  // Live email format check
  const emailInput = document.getElementById('email');
  const emailMsg   = document.getElementById('emailMsg');
  const EMAIL_RE   = /^[\w.+\-]+@[\w\-]+(\.[\w\-]+)+$/;
  emailInput.addEventListener('blur', () => {
    const v = emailInput.value.trim();
    if (!v) { emailMsg.textContent = 'Mã xác minh sẽ được gửi đến email này'; emailMsg.style.color = 'var(--warm-gray)'; return; }
    if (!EMAIL_RE.test(v)) {
      emailMsg.textContent = 'Email không đúng định dạng';
      emailMsg.style.color = 'var(--red-err)';
    } else {
      emailMsg.textContent = 'Email hợp lệ';
      emailMsg.style.color = 'var(--green-500)';
    }
  });

  // Live phone format check
  const phoneInput = document.getElementById('phone');
  const phoneMsg   = document.getElementById('phoneMsg');
  const PHONE_RE   = /^0\d{9}$/;
  phoneInput.addEventListener('input', () => {
    const v = phoneInput.value.trim();
    if (!v) { phoneMsg.textContent = 'Nhập đúng 10 chữ số, bắt đầu bằng 0'; phoneMsg.style.color = 'var(--warm-gray)'; return; }
    if (!PHONE_RE.test(v)) {
      phoneMsg.textContent = 'Phải có đúng 10 chữ số và bắt đầu bằng 0';
      phoneMsg.style.color = 'var(--red-err)';
    } else {
      phoneMsg.textContent = 'Số điện thoại hợp lệ';
      phoneMsg.style.color = 'var(--green-500)';
    }
  });

  // Confirm password live check
  const pwd = document.getElementById('password');
  const cfm = document.getElementById('confirmPassword');
  const msg = document.getElementById('confirmMsg');
  cfm.addEventListener('input', () => {
    if (!cfm.value) { msg.textContent = ''; return; }
    const match = pwd.value === cfm.value;
    msg.textContent = match ? 'Mật khẩu khớp' : 'Mật khẩu không khớp';
    msg.style.color = match ? 'var(--green-500)' : 'var(--red-err)';
  });

  // Client-side validation on submit
  document.getElementById('registerForm').addEventListener('submit', function(e) {
    const fullName = document.getElementById('fullName').value.trim();
    const email    = emailInput.value.trim();
    const password = pwd.value;
    const confirm  = cfm.value;
    const phone    = phoneInput.value.trim();

    if (!fullName || !email || !password) {
      e.preventDefault();
      return alert('Vui lòng nhập đầy đủ các trường bắt buộc.');
    }
    if (!EMAIL_RE.test(email)) {
      e.preventDefault();
      return alert('Email không đúng định dạng.');
    }
    if (password !== confirm) {
      e.preventDefault();
      return alert('Mật khẩu xác nhận không khớp.');
    }
    if (!/(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{6,}/.test(password)) {
      e.preventDefault();
      return alert('Mật khẩu chưa đủ mạnh. Vui lòng kiểm tra các yêu cầu bên dưới.');
    }
    if (phone && !PHONE_RE.test(phone)) {
      e.preventDefault();
      return alert('Số điện thoại phải có đúng 10 chữ số và bắt đầu bằng 0.');
    }
  });
</script>
</body>
</html>
