<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Hồ sơ của tôi – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/profile.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="account-wrap">

  <div class="account-head">
    <h1>Hồ sơ của tôi</h1>
    <p>Quản lý thông tin cá nhân và bảo mật tài khoản của bạn.</p>
  </div>

  <c:if test="${profileIncomplete}">
    <div class="account-alert">
      <span class="icon">⚠️</span>
      <div>
        Tài khoản của bạn còn thiếu <strong>email</strong> hoặc <strong>số điện thoại</strong>.
        Vui lòng cập nhật đầy đủ thông tin liên hệ bên dưới để tiếp tục sử dụng hệ thống.
      </div>
    </div>
  </c:if>

  <div class="account-layout">

    <%-- ══════════════════ Sidebar: tóm tắt tài khoản ══════════════════ --%>
    <aside class="account-sidebar">
      <div class="account-avatar">${displayInitial}</div>
      <div class="account-sidebar-name">${customer.fullName}</div>
      <div class="account-sidebar-sub">Khách hàng PetClinic</div>

      <div class="account-checklist">
        <div class="account-checklist-item ${empty customer.phone ? 'pending' : 'done'}">
          <span class="ci-icon">${empty customer.phone ? '!' : '✓'}</span>
          <span>Số điện thoại</span>
        </div>
        <div class="account-checklist-item ${empty customer.email ? 'pending' : 'done'}">
          <span class="ci-icon">${empty customer.email ? '!' : '✓'}</span>
          <span>Email</span>
        </div>
      </div>

      <a href="${ctx}/home" class="account-back-link">← Quay lại trang chủ</a>
    </aside>

    <%-- ══════════════════ Main: các thẻ thiết lập ══════════════════ --%>
    <div class="account-main">

      <%-- Card: Thông tin cá nhân --%>
      <div class="account-card">
        <div class="account-card-head">
          <h2>👤 Họ và tên</h2>
        </div>
        <div class="account-card-body">
          <c:if test="${errorSection == 'name'}">
            <div class="account-alert-inline">✗ ${error}</div>
          </c:if>
          <form action="${ctx}/profile/name" method="post" novalidate>
            <div class="form-group">
              <label for="fullName">Họ và tên</label>
              <input type="text" id="fullName" name="fullName" class="form-control"
                     value="<c:out value='${displayName}'/>" required maxlength="100">
            </div>
            <div class="form-actions">
              <button type="submit" class="btn-save">Lưu thay đổi</button>
            </div>
          </form>
        </div>
      </div>

      <%-- Card: Số điện thoại --%>
      <div class="account-card">
        <div class="account-card-head">
          <h2>📱 Số điện thoại</h2>
          <c:choose>
            <c:when test="${empty customer.phone}">
              <span class="account-badge account-badge-missing">Chưa cập nhật</span>
            </c:when>
            <c:otherwise>
              <span class="account-badge account-badge-ok">Đã có</span>
            </c:otherwise>
          </c:choose>
        </div>
        <div class="account-card-body">
          <c:if test="${errorSection == 'phone'}">
            <div class="account-alert-inline">✗ ${error}</div>
          </c:if>
          <form action="${ctx}/profile/phone" method="post" novalidate>
            <div class="form-group">
              <label for="phone">Số điện thoại</label>
              <input type="tel" id="phone" name="phone"
                     class="form-control${empty displayPhone ? ' field-missing' : ''}"
                     value="<c:out value='${displayPhone}'/>"
                     placeholder="09xxxxxxxx" required>
              <div class="form-hint">Nhập đúng 10 chữ số, bắt đầu bằng 0</div>
            </div>
            <div class="form-actions">
              <button type="submit" class="btn-save">Lưu thay đổi</button>
            </div>
          </form>
        </div>
      </div>

      <%-- Card: Email — fix cứng, chỉ cho thiết lập một lần ══════════ --%>
      <div class="account-card account-card-full">
        <div class="account-card-head">
          <h2>✉️ Email</h2>
          <c:choose>
            <c:when test="${empty customer.email}">
              <span class="account-badge account-badge-missing">Chưa cập nhật</span>
            </c:when>
            <c:otherwise>
              <span class="account-badge account-badge-locked">🔒 Đã khoá</span>
            </c:otherwise>
          </c:choose>
        </div>
        <div class="account-card-body">
          <c:if test="${errorSection == 'email'}">
            <div class="account-alert-inline">✗ ${error}</div>
          </c:if>

          <c:choose>
            <%-- ── Đã có email: chỉ hiển thị, không cho sửa ── --%>
            <c:when test="${not empty customer.email}">
              <div class="account-current-value">
                <span>${customer.email}</span>
                <span class="account-badge account-badge-ok">Đã xác thực</span>
              </div>
              <div class="account-locked-note">
                <span class="icon">🔒</span>
                <span>Email đã được thiết lập cho tài khoản này và không thể thay đổi, nhằm đảm bảo
                  an toàn và tránh nhầm lẫn trong việc xác minh danh tính. Nếu bạn cần hỗ trợ, vui
                  lòng liên hệ với PetClinic.</span>
              </div>
            </c:when>

            <%-- ── Chưa có email: cho phép thiết lập lần đầu ── --%>
            <c:otherwise>
              <div class="account-current-value">
                <span class="empty">Chưa có email</span>
              </div>
              <div class="account-subform">
                <div class="account-subform-label">Thêm email cho tài khoản</div>
                <form action="${ctx}/profile/email" method="post" novalidate>
                  <div class="form-group">
                    <label for="newEmail">Email</label>
                    <input type="email" id="newEmail" name="newEmail" class="form-control"
                           value="<c:out value='${displayNewEmail}'/>"
                           placeholder="ban@example.com" required>
                    <div class="form-hint">
                      Mã xác minh gồm 6 chữ số sẽ được gửi đến email này. Sau khi xác minh, email sẽ
                      không thể thay đổi được nữa.
                    </div>
                  </div>
                  <div class="form-actions">
                    <button type="submit" class="btn-save">Gửi mã xác minh</button>
                  </div>
                </form>
              </div>
            </c:otherwise>
          </c:choose>
        </div>
      </div>

      <%-- Card: Đổi mật khẩu — không cần mật khẩu hiện tại ══════════ --%>
      <div class="account-card account-card-full">
        <div class="account-card-head">
          <h2>🔒 Đổi mật khẩu</h2>
        </div>
        <div class="account-card-body">
          <c:if test="${errorSection == 'password'}">
            <div class="account-alert-inline">✗ ${error}</div>
          </c:if>
          <form action="${ctx}/profile/password" method="post" novalidate>
            <div class="form-group">
              <label for="newPassword">Mật khẩu mới</label>
              <div class="input-wrap">
                <input type="password" id="newPassword" name="newPassword"
                       class="form-control has-toggle" autocomplete="new-password" required>
                <button type="button" class="toggle-pwd" id="toggleNewPwd"
                        onclick="togglePassword('newPassword','toggleNewPwd')">👁</button>
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
              <label for="confirmPassword">Xác nhận mật khẩu mới</label>
              <div class="input-wrap">
                <input type="password" id="confirmPassword" name="confirmPassword"
                       class="form-control has-toggle" autocomplete="new-password" required>
                <button type="button" class="toggle-pwd" id="toggleConfirmPwd"
                        onclick="togglePassword('confirmPassword','toggleConfirmPwd')">👁</button>
              </div>
            </div>
            <div class="form-actions">
              <button type="submit" class="btn-save">Đổi mật khẩu</button>
            </div>
          </form>
        </div>
      </div>

    </div><!-- /account-main -->
  </div><!-- /account-layout -->

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
<script src="${ctx}/js/auth.js"></script>
<script>
  initPasswordStrength('newPassword', 'pwdBar', 'pwdLabel', 'pwdReqs');
</script>
</body>
</html>
