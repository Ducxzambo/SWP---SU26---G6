<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx"      value="${pageContext.request.contextPath}"/>
<c:set var="customer" value="${sessionScope.customer}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>PetClinic – Trang của bạn</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">

</head>
<body>

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<main class="main-content">

  <!-- Customer hero greeting -->
  <section class="customer-hero">
    <h1>Xin chào, ${customer.fullName}!</h1>
    <p>Quản lý thú cưng và lịch khám của bạn một cách dễ dàng</p>
  </section>

  <!-- Quick access cards -->
  <div class="quick-access">

    <!-- Thú cưng của bạn -->
    <a href="${ctx}/pets" class="qa-card" style="text-decoration:none;color:inherit;">
      <div class="qa-icon">🐾</div>
      <h3>Thú cưng của bạn</h3>
      <p>Xem hồ sơ, lịch sử khám và thông tin các thú cưng</p>
    </a>

    <!-- Lịch khám -->
    <a href="${ctx}/appointments" class="qa-card" style="text-decoration:none;color:inherit;">
      <div class="qa-icon">📅</div>
      <h3>Lịch khám</h3>
      <p>Xem và quản lý các cuộc hẹn đã đặt, lịch tái khám</p>
    </a>

    <!-- New Booking (prominent) -->
    <a href="${ctx}/booking/new" class="qa-card booking-card" style="text-decoration:none;">
      <div class="qa-icon">➕</div>
      <h3>Đặt lịch khám mới</h3>
      <p>Chọn dịch vụ, thú cưng và khung giờ phù hợp</p>
    </a>

  </div>

  <!-- Recent notifications preview -->
  <section class="section" style="padding-top:48px;padding-bottom:40px;">
    <div style="max-width:900px;margin:0 auto;">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px;">
        <h2 style="font-family:'Playfair Display',serif;font-size:22px;color:var(--green-900);">
          Thông báo gần đây
          <c:if test="${unreadCount > 0}">
            <span style="background:var(--green-500);color:#fff;border-radius:12px;
                  padding:2px 10px;font-size:13px;font-family:'DM Sans',sans-serif;
                  font-weight:600;margin-left:8px;">${unreadCount} mới</span>
          </c:if>
        </h2>
        <a href="${ctx}/notifications"
           style="font-size:13.5px;color:var(--green-500);font-weight:500;">
          Xem tất cả →
        </a>
      </div>

      <div id="homeNotifList">
        <div style="text-align:center;padding:32px;color:var(--warm-gray);font-size:14px;">
          Đang tải thông báo...
        </div>
      </div>
    </div>
  </section>

  <!-- Services quick links -->
  <section class="section" style="padding-top:0;background:#fff;">
    <div class="section-title" style="margin-bottom:8px;">Dịch vụ của chúng tôi</div>
    <div class="section-subtitle">Đặt lịch nhanh theo danh mục</div>
    <div class="card-grid">
      <c:forEach var="cat" items="${navCategories}">
        <div class="feature-card" style="text-align:left;">
          <div class="icon">🩺</div>
          <h3>${cat.name}</h3>
          <div style="display:flex;flex-wrap:wrap;gap:6px;margin-top:10px;">
            <c:forEach var="svc" items="${cat.services}">
              <a href="${ctx}/booking/new?prefillCategory=${cat.categoryID}&prefillService=${svc.serviceID}"
                 style="padding:5px 12px;background:var(--green-50);border:1px solid var(--green-100);
                        border-radius:20px;font-size:12.5px;color:var(--green-700);font-weight:500;
                        transition:var(--transition);"
                 onmouseover="this.style.background='var(--green-100)'"
                 onmouseout="this.style.background='var(--green-50)'">
                ${svc.name}
              </a>
            </c:forEach>
          </div>
        </div>
      </c:forEach>
    </div>
  </section>

</main>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script>
// Load recent notifications inline
fetch('${ctx}/notifications/api?limit=5')
  .then(r => r.json())
  .then(data => {
    const el = document.getElementById('homeNotifList');
    if (!data || data.length === 0) {
      el.innerHTML = '<div style="text-align:center;padding:24px;color:var(--warm-gray);font-size:14px;">Chưa có thông báo nào.</div>';
      return;
    }
    el.innerHTML = data.map(n => `
      <div style="background:#fff;border:1px solid var(--border);border-radius:10px;
                  padding:14px 18px;margin-bottom:10px;${!n.isRead ? 'border-left:3px solid var(--green-500);' : ''}">
        <div style="font-weight:500;font-size:14px;margin-bottom:4px;">${n.title}</div>
        <div style="font-size:13px;color:var(--warm-gray);">${n.body}</div>
      </div>`).join('');
  })
  .catch(() => {
    document.getElementById('homeNotifList').innerHTML =
      '<div style="text-align:center;padding:24px;color:var(--warm-gray);">Không thể tải thông báo.</div>';
  });
</script>
</body>
</html>
