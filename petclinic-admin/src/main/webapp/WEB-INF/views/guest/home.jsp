<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>PetClinic – Chăm sóc thú cưng tận tâm</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<main class="main-content">

  <!-- Hero -->
  <section class="hero">
    <div class="hero-inner">
      <h1>Chăm sóc thú cưng<br>với <em>tình yêu thương</em></h1>
      <p>Đội ngũ bác sĩ thú y chuyên nghiệp, cơ sở vật chất hiện đại, luôn sẵn sàng
         đồng hành cùng bạn trong việc chăm sóc người bạn bốn chân.</p>
      <div class="hero-cta">
        <a href="${ctx}/auth/register" class="cta-primary">Đăng ký ngay</a>
        <a href="${ctx}/auth/login"    class="cta-secondary">Đăng nhập</a>
      </div>
    </div>
  </section>

  <!-- Why PetClinic -->
  <section class="section" style="background:#fff;">
    <div class="section-title">Tại sao chọn PetClinic?</div>
    <div class="section-subtitle">Chúng tôi đặt sức khỏe thú cưng của bạn lên hàng đầu</div>
    <div class="card-grid">
      <div class="feature-card">
        <div class="icon">️</div>
        <h3>Bác sĩ chuyên nghiệp</h3>
        <p>Đội ngũ bác sĩ thú y được đào tạo bài bản với nhiều năm kinh nghiệm thực tiễn.</p>
      </div>
      <div class="feature-card">
        <div class="icon"></div>
        <h3>Cơ sở hiện đại</h3>
        <p>Trang thiết bị y tế tiên tiến, phòng khám vô trùng theo tiêu chuẩn quốc tế.</p>
      </div>
      <div class="feature-card">
        <div class="icon"></div>
        <h3>Thuốc chính hãng</h3>
        <p>Toàn bộ thuốc và vaccine được nhập khẩu từ các nhà cung cấp uy tín.</p>
      </div>
      <div class="feature-card">
        <div class="icon"></div>
        <h3>Đặt lịch dễ dàng</h3>
        <p>Đặt lịch khám trực tuyến nhanh chóng, nhận nhắc nhở tự động qua email.</p>
      </div>
    </div>
  </section>

  <!-- Services preview -->
  <section class="section">
    <div class="section-title">Dịch vụ nổi bật</div>
    <div class="section-subtitle">Từ khám tổng quát đến phẫu thuật chuyên sâu</div>
    <div class="card-grid">
      <c:forEach var="cat" items="${navCategories}">
        <div class="feature-card" style="text-align:left;">
          <div class="icon"></div>
          <h3>${cat.name}</h3>
          <p>
            <c:forEach var="svc" items="${cat.services}" varStatus="vs">
              <c:if test="${vs.index < 3}">${svc.name}<c:if test="${!vs.last && vs.index < 2}">, </c:if></c:if>
            </c:forEach>
            <c:if test="${cat.services.size() > 3}"> và thêm...</c:if>
          </p>
          <a href="${ctx}/services?category=${cat.categoryID}"
             style="display:inline-block;margin-top:14px;font-size:13px;color:var(--green-500);font-weight:500;">
            Xem chi tiết →
          </a>
        </div>
      </c:forEach>
    </div>
  </section>

  <!-- CTA -->
  <section class="section" style="background:var(--green-900);padding:60px 32px;text-align:center;">
    <h2 style="font-family:'Playfair Display',serif;font-size:32px;color:#fff;margin-bottom:14px;">
      Sẵn sàng đặt lịch khám?
    </h2>
    <p style="color:rgba(255,255,255,.75);margin-bottom:28px;font-size:15px;">
      Đăng ký tài khoản miễn phí và đặt lịch ngay hôm nay.
    </p>
    <a href="${ctx}/auth/register" class="cta-primary"
       style="display:inline-block;padding:14px 36px;background:var(--green-400);
              color:var(--green-900);border-radius:10px;font-weight:600;font-size:15px;">
      Bắt đầu ngay
    </a>
  </section>

</main>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>
