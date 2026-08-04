<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx"      value="${pageContext.request.contextPath}"/>
<c:set var="customer" value="${sessionScope.customer}"/>
<%-- Meta tag lets main.js know the context path without inline scripts --%>
<meta name="ctx" content="${ctx}">
<header class="site-header">
  <link rel="stylesheet" href="${ctx}/css/reviews.css">
  <nav class="nav-inner">

    <!-- Logo -->
    <a href="${ctx}/" class="nav-logo"><svg class="nav-inline-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="7" cy="8" r="2"/><circle cx="12" cy="5" r="2"/><circle cx="17" cy="8" r="2"/><path d="M12 11c-3 0-5 2-5 5 0 2 1 3 3 2l2-1 2 1c2 1 3 0 3-2 0-3-2-5-5-5z"/></svg> Pet<span>Clinic</span></a>

    <!-- Main nav links -->
    <ul class="nav-links">

      <!-- Trang chủ -->
      <li class="nav-item">
        <a href="${ctx}/" class="nav-link">Trang chủ</a>
      </li>

      <c:if test="${empty customer}">
        <!-- Giới thiệu -->
        <li class="nav-item">
        <span class="nav-link">Giới thiệu <svg class="nav-inline-icon chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m7 9 5 5 5-5"/></svg></span>
        <div class="nav-dropdown">
        <a href="${ctx}/#intro">Lời giới thiệu</a>
        <a href="${ctx}/#facility">Cơ sở vật chất</a>
        <a href="${ctx}/#team">Nhân viên</a>
        <a href="${ctx}/#vision">Tầm nhìn và Phát triển</a>
        </div>
        </li>
      </c:if>


      <!-- Dịch vụ (dynamic from DB) -->
      <li class="nav-item">
        <a href="${ctx}/services" class="nav-link">Dịch vụ <svg class="nav-inline-icon chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m7 9 5 5 5-5"/></svg></a>
        <div class="nav-dropdown" style="min-width:240px;">
          <c:forEach var="cat" items="${navCategories}">
            <div class="dd-group">
              <div class="dd-group-label">
                <span>${cat.name}</span>
                <c:if test="${not empty cat.services}"><svg class="nav-inline-icon arr" viewBox="0 0 24 24" aria-hidden="true"><path d="m9 6 6 6-6 6"/></svg></c:if>
              </div>
              <c:if test="${not empty cat.services}">
                <div class="dd-sub">
                  <c:forEach var="svc" items="${cat.services}">
                    <a href="${ctx}/services?category=${cat.categoryID}&service=${svc.serviceID}">
                      ${svc.name}
                    </a>
                  </c:forEach>
                </div>
              </c:if>
            </div>
          </c:forEach>
          <c:if test="${empty navCategories}">
            <span style="padding:12px 14px;font-size:13px;color:var(--warm-gray);display:block;">
              Đang cập nhật...
            </span>
          </c:if>
        </div>
      </li>

      <!-- Cộng đồng -->
      <li class="nav-item">
        <a href="${ctx}/community" class="nav-link">Cộng đồng</a>
      </li>

      <!-- Liên hệ -->
      <li class="nav-item">
        <a href="${ctx}/contact" class="nav-link">Liên hệ</a>
      </li>

      <!-- ── Customer-only nav items ──────────────────────────────────── -->
            <c:if test="${not empty customer}">
            </c:if>

    </ul><!-- /nav-links -->

    <!-- Right-side actions -->
    <div class="nav-actions">

      <c:choose>
        <c:when test="${not empty customer}">

          <!-- Big quick-access buttons -->
          <div class="nav-big-btns">
            <a href="${ctx}/pets" class="nav-big-btn pets">Thú cưng</a>
            <a href="${ctx}/appointments" class="nav-big-btn schedule">Lịch khám</a>
            <a href="${ctx}/booking/new" class="nav-big-btn booking"><svg class="nav-inline-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg> Đặt lịch</a>
          </div>

          <!-- Profile dropdown -->
          <div class="nav-profile">
            <button class="nav-profile-btn" id="navProfileBtn" title="Tài khoản"
                    onclick="toggleProfileMenu()">
              <div class="nav-avatar">
                ${customer.fullName.substring(0,1).toUpperCase()}
              </div>
              <svg class="nav-inline-icon chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m7 9 5 5 5-5"/></svg>
            </button>
            <div class="nav-profile-panel" id="navProfilePanel">
              <a href="${ctx}/profile" class="nav-profile-item">Hồ sơ của tôi</a>
              <div class="nav-profile-divider"></div>
              <form action="${ctx}/auth/logout" method="post" style="margin:0;">
                <button type="submit" class="nav-profile-item nav-profile-item-danger">Đăng xuất</button>
              </form>
            </div>
          </div>

        </c:when>
        <c:otherwise>
          <!-- Guest buttons -->
          <a href="${ctx}/auth/login"    class="btn-login">Đăng nhập</a>
          <a href="${ctx}/auth/register" class="btn-signup">Đăng ký</a>
        </c:otherwise>
      </c:choose>

    </div><!-- /nav-actions -->
  </nav>
</header>

<!-- Flash messages -->
<c:if test="${not empty sessionScope.flashSuccess}">
  <div class="flash flash-success">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div class="flash flash-error">${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>
