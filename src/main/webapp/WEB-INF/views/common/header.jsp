<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx"      value="${pageContext.request.contextPath}"/>
<c:set var="customer" value="${sessionScope.customer}"/>
<%-- Meta tag lets main.js know the context path without inline scripts --%>
<meta name="ctx" content="${ctx}">
<header class="site-header">
  <nav class="nav-inner">

    <!-- Logo -->
    <a href="${ctx}/" class="nav-logo">🐾 Pet<span>Clinic</span></a>

    <!-- Main nav links -->
    <ul class="nav-links">

      <!-- Trang chủ -->
      <li class="nav-item">
        <a href="${ctx}/" class="nav-link">Trang chủ</a>
      </li>

      <!-- Giới thiệu -->
      <li class="nav-item">
        <span class="nav-link">Giới thiệu <span class="chevron">▾</span></span>
        <div class="nav-dropdown">
          <a href="${ctx}/about#intro">Lời giới thiệu</a>
          <a href="${ctx}/about#facility">Cơ sở vật chất</a>
          <a href="${ctx}/about#team">Nhân viên</a>
          <a href="${ctx}/about#vision">Tầm nhìn và Phát triển</a>
        </div>
      </li>

      <!-- Dịch vụ (dynamic from DB) -->
      <li class="nav-item">
        <span class="nav-link">Dịch vụ <span class="chevron">▾</span></span>
        <div class="nav-dropdown" style="min-width:240px;">
          <c:forEach var="cat" items="${navCategories}">
            <div class="dd-group">
              <div class="dd-group-label">
                <span>${cat.name}</span>
                <c:if test="${not empty cat.services}"><span class="arr">›</span></c:if>
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

      <!-- Kiến thức -->
      <li class="nav-item">
        <span class="nav-link">Kiến thức <span class="chevron">▾</span></span>
        <div class="nav-dropdown">
          <a href="${ctx}/knowledge/diseases">Các loại bệnh</a>
          <a href="${ctx}/knowledge/breeds">Các giống thú cưng</a>
          <a href="${ctx}/knowledge/cases">Ca bệnh thực tế</a>
          <a href="${ctx}/knowledge/care-tips">Lưu ý chăm sóc thú cưng</a>
        </div>
      </li>

      <!-- Tin tức -->
      <li class="nav-item">
        <a href="${ctx}/news" class="nav-link">Tin tức</a>
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
        <li class="nav-item">
          <a href="${ctx}/notifications" class="nav-link">Thông báo
            <c:if test="${unreadCount > 0}">
              <span style="background:var(--green-400);color:var(--green-900);
                border-radius:10px;padding:1px 7px;font-size:11px;font-weight:700;">
                ${unreadCount}
              </span>
            </c:if>
          </a>
        </li>
      </c:if>

    </ul><!-- /nav-links -->

    <!-- Right-side actions -->
    <div class="nav-actions">

      <c:choose>
        <c:when test="${not empty customer}">

          <!-- Notification bell -->
          <button class="notif-btn" id="notifBtn" title="Thông báo" onclick="toggleNotif()">
            🔔
            <c:if test="${unreadCount > 0}">
              <span class="notif-badge">${unreadCount > 9 ? '9+' : unreadCount}</span>
            </c:if>
          </button>

          <!-- Big quick-access buttons -->
          <div class="nav-big-btns">
            <a href="${ctx}/pets" class="nav-big-btn pets">Thú cưng</a>
            <a href="${ctx}/appointments" class="nav-big-btn schedule">Lịch khám</a>
            <a href="${ctx}/booking/new" class="nav-big-btn booking">➕ Đặt lịch</a>
          </div>

          <!-- Avatar + name -->
          <div class="nav-customer">
            <div class="nav-avatar">
              ${customer.fullName.substring(0,1).toUpperCase()}
            </div>
            <span style="max-width:120px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
              ${customer.fullName}
            </span>
          </div>

          <!-- Logout -->
          <form action="${ctx}/auth/logout" method="post" style="margin:0;">
            <button type="submit" class="btn-logout">Đăng xuất</button>
          </form>

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

<!-- Notification slide-down panel (customer only) -->
<c:if test="${not empty customer}">
  <div class="notif-panel" id="notifPanel">
    <div class="notif-panel-head">
      <h4>Thông báo</h4>
      <a href="${ctx}/notifications/mark-read" onclick="markAllRead(event)">Đánh dấu đã đọc</a>
    </div>
    <div class="notif-list" id="notifList">
      <div class="notif-empty" id="notifLoading">Đang tải...</div>
    </div>
  </div>
</c:if>

<!-- Flash messages -->
<c:if test="${not empty sessionScope.flashSuccess}">
  <div class="flash flash-success">✓ ${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div class="flash flash-error">✗ ${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>
