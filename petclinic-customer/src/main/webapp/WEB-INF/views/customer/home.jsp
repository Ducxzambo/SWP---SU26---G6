<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx"      value="${pageContext.request.contextPath}"/>
<c:set var="customer" value="${sessionScope.customer}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>PetClinic – Trang của bạn</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/customer-dashboard.css">
</head>
<body>

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<main class="main-content" style="max-width:1240px;margin:0 auto;padding:28px 24px 60px;">

  <div class="db-topbar">Trang của bạn<c:if test="${not empty customer}"> / <strong>${customer.fullName}</strong></c:if></div>

  <div class="db-welcome">
    <h1>Xin chào, ${customer.fullName}! <span class="db-welcome-badge">KHÁCH HÀNG</span></h1>
    <p>Theo dõi lịch khám, thú cưng và thông báo của bạn tại một nơi duy nhất.</p>
  </div>

  <div class="db-metric-grid">
    <div class="db-metric-card">
      <div class="db-metric-label">Lịch hẹn sắp tới</div>
      <div class="db-metric-num">${upcomingCount}</div>
      <span class="db-metric-tag green">Đang chờ</span>
      <span class="db-metric-sub">trong tổng ${totalAppointments} lịch hẹn</span>
    </div>
    <div class="db-metric-card">
      <div class="db-metric-label">Thú cưng của bạn</div>
      <div class="db-metric-num">${totalPets}</div>
      <span class="db-metric-tag gray">Đã đăng ký</span>
      <span class="db-metric-sub">hồ sơ thú cưng</span>
    </div>
    <div class="db-metric-card">
      <div class="db-metric-label">Đã hoàn thành</div>
      <div class="db-metric-num">${doneCount}</div>
      <span class="db-metric-tag green">Hoàn tất</span>
      <span class="db-metric-sub">lượt khám / chăm sóc</span>
    </div>
    <div class="db-metric-card">
      <div class="db-metric-label">Thông báo chưa đọc</div>
      <div class="db-metric-num">${unreadCount}</div>
      <c:choose>
        <c:when test="${unreadCount > 0}"><span class="db-metric-tag amber">Mới</span></c:when>
        <c:otherwise><span class="db-metric-tag gray">Đã đọc hết</span></c:otherwise>
      </c:choose>
      <span class="db-metric-sub">cập nhật gần đây</span>
    </div>
  </div>

  <div class="db-section-head">Truy Cập Nhanh</div>
  <div class="db-quick-grid">
    <a href="${ctx}/booking/new" class="db-quick-card">
      <div class="db-quick-icon"></div><span>Đặt lịch khám mới</span>
    </a>
    <a href="${ctx}/pets" class="db-quick-card">
      <div class="db-quick-icon"></div><span>Thú cưng của tôi</span>
    </a>
    <a href="${ctx}/appointments" class="db-quick-card">
      <div class="db-quick-icon"></div><span>Lịch khám</span>
    </a>
    <a href="${ctx}/notifications" class="db-quick-card">
      <div class="db-quick-icon"></div>
      <span>Thông báo<c:if test="${unreadCount > 0}"> (${unreadCount})</c:if></span>
    </a>
  </div>

  <div class="db-section-head">Tổng Quan Lịch Hẹn</div>
  <div class="db-split">

    <div class="db-panel">
      <div class="db-panel-head">
        <h3>Phân bố lịch hẹn theo trạng thái</h3>
        <a href="${ctx}/appointments">Xem tất cả →</a>
      </div>
      <div class="db-panel-sub">Toàn bộ lịch hẹn của bạn, phân theo trạng thái xử lý</div>

      <c:choose>
        <c:when test="${totalAppointments == 0}">
          <div class="db-empty-mini">Bạn chưa có lịch hẹn nào. Hãy đặt lịch đầu tiên!</div>
        </c:when>
        <c:otherwise>
          <c:forEach var="entry" items="${statusDist}">
            <c:if test="${entry.value > 0}">
              <div class="db-bar-row">
                <div class="db-bar-label">${entry.key}</div>
                <div class="db-bar-track">
                  <div class="db-bar-fill" style="width:${entry.value * 100 / maxDist}%;">${entry.value} lịch</div>
                </div>
              </div>
            </c:if>
          </c:forEach>
        </c:otherwise>
      </c:choose>
    </div>

    <div class="db-panel">
      <div class="db-panel-head">
        <h3>Lịch hẹn gần đây</h3>
        <a href="${ctx}/appointments">Xem tất cả →</a>
      </div>
      <div class="db-panel-sub">5 lượt gần nhất</div>

      <div class="db-recent-list">
        <c:forEach var="a" items="${recentAppointments}">
          <a href="${ctx}/appointments/detail?id=${a.appointmentID}" style="text-decoration:none;color:inherit;">
            <div class="db-recent-item">
              <div class="db-recent-avatar">🐾</div>
              <div class="db-recent-body">
                <div class="db-recent-title">${a.serviceName}</div>
                <div class="db-recent-sub">${a.formattedAppointmentDate}<c:if test="${not empty a.petName}"> · ${a.petName}</c:if></div>
              </div>
              <span class="db-recent-status db-status-${fn:toLowerCase(a.status)}">${a.status}</span>
            </div>
          </a>
        </c:forEach>
        <c:if test="${empty recentAppointments}">
          <div class="db-empty-mini">Chưa có lịch hẹn nào.</div>
        </c:if>
      </div>
    </div>

  </div>

<%--  <div class="db-section-head">Dịch Vụ Của Chúng Tôi</div>--%>
<%--  <div class="card-grid">--%>
<%--    <c:forEach var="cat" items="${navCategories}">--%>
<%--      <div class="feature-card" style="text-align:left;">--%>
<%--        <div class="icon-wrap">--%>
<%--          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">--%>
<%--            <path d="M4.5 3v6.5a4.5 4.5 0 0 0 9 0V3"/>--%>
<%--            <path d="M13.5 12v2.5a5.5 5.5 0 0 1-11 0V12"/>--%>
<%--            <circle cx="18.5" cy="15.5" r="2.5"/>--%>
<%--            <path d="M16.3 15.5a2.5 2.5 0 0 1-2.8-2.5"/>--%>
<%--          </svg>--%>
<%--        </div>--%>
<%--        <h3>${cat.name}</h3>--%>
<%--        <p style="margin-bottom:12px;">${cat.description}</p>--%>
<%--      </div>--%>
<%--    </c:forEach>--%>
<%--  </div>--%>

</main>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>