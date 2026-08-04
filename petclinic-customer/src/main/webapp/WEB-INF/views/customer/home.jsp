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

  <section class="customer-hero">
    <h1>Xin chào, ${customer.fullName}!</h1>
    <p>Quản lý thú cưng và lịch khám của bạn một cách dễ dàng</p>
    <a href="${ctx}/booking/new" class="customer-hero-cta">
      <span class="cta-icon">➕</span>
      <span>
        <strong>Đặt lịch khám mới</strong>
        <small>Chọn dịch vụ, thú cưng và khung giờ phù hợp</small>
      </span>
    </a>
  </section>

  <!-- Recent notifications preview -->
  <section class="section" style="padding-top:40px;padding-bottom:32px;">
    <div style="max-width:900px;margin:0 auto;">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px;gap:12px;flex-wrap:wrap;">
        <h2 style="font-family:'Playfair Display',serif;font-size:22px;color:var(--green-900);min-width:0;overflow-wrap:break-word;">
          Thông báo gần đây
          <c:if test="${unreadCount > 0}">
            <span style="background:var(--green-500);color:#fff;border-radius:12px;
                  padding:2px 10px;font-size:13px;font-family:'DM Sans',sans-serif;
                  font-weight:600;margin-left:8px;">${unreadCount} mới</span>
          </c:if>
        </h2>
        <a href="${ctx}/notifications"
           style="font-size:13.5px;color:var(--green-500);font-weight:500;flex-shrink:0;">
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

  <!-- Services — kèm mô tả ngắn cho từng nhóm dịch vụ -->
  <section class="section" style="padding-top:0;background:#fff;">
    <div class="section-title" style="margin-bottom:8px;">Dịch vụ của chúng tôi</div>
    <div class="section-subtitle">Đặt lịch nhanh theo danh mục</div>
    <div class="card-grid">
      <c:forEach var="cat" items="${navCategories}">
        <div class="feature-card" style="text-align:left;">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4.5 3v6.5a4.5 4.5 0 0 0 9 0V3"/>
              <path d="M13.5 12v2.5a5.5 5.5 0 0 1-11 0V12"/>
              <circle cx="18.5" cy="15.5" r="2.5"/>
              <path d="M16.3 15.5a2.5 2.5 0 0 1-2.8-2.5"/>
            </svg>
          </div>
          <h3>${cat.name}</h3>
          <p style="margin-bottom:12px;">${cat.description}</p>
          <div style="display:flex;flex-wrap:wrap;gap:6px;">
<%--            <c:forEach var="svc" items="${cat.services}">--%>
<%--              <a href="${ctx}/booking/new?prefillCategory=${cat.categoryID}&prefillService=${svc.serviceID}"--%>
<%--                 style="padding:5px 12px;background:var(--green-50);border:1px solid var(--green-100);--%>
<%--                        border-radius:20px;font-size:12.5px;color:var(--green-700);font-weight:500;--%>
<%--                        transition:var(--transition);overflow-wrap:break-word;max-width:100%;"--%>
<%--                 onmouseover="this.style.background='var(--green-100)'"--%>
<%--                 onmouseout="this.style.background='var(--green-50)'">--%>
<%--                ${svc.name}--%>
<%--              </a>--%>
<%--            </c:forEach>--%>
          </div>
        </div>
      </c:forEach>
    </div>
  </section>

</main>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script>

fetch('${ctx}/notifications/api?limit=5')
  .then(r => r.json())
  .then(data => {
    const el = document.getElementById('homeNotifList');
    if (!data || data.length === 0) {
      el.innerHTML = '<div style="text-align:center;padding:24px;color:var(--warm-gray);font-size:14px;">Chưa có thông báo nào.</div>';
      return;
    }
    el.innerHTML = data.map(n => {
      const unreadBorder = !n.isRead ? 'border-left:3px solid var(--green-500);' : '';
      return '<div style="background:#fff;border:1px solid var(--border);border-radius:10px;' +
             'padding:14px 18px;margin-bottom:10px;overflow-wrap:break-word;' + unreadBorder + '">' +
               '<div style="font-weight:500;font-size:14px;margin-bottom:4px;">' + escHtml(n.title) + '</div>' +
               '<div style="font-size:13px;color:var(--warm-gray);">' + escHtml(n.body || '') + '</div>' +
             '</div>';
    }).join('');
  })
  .catch(() => {
    document.getElementById('homeNotifList').innerHTML =
      '<div style="text-align:center;padding:24px;color:var(--warm-gray);">Không thể tải thông báo.</div>';
  });
</script>
</body>
</html>
