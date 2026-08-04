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

  <!-- ══════════════════════════════════════════════════════════════════
       HERO
  ══════════════════════════════════════════════════════════════════ -->
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

  <!-- ══════════════════════════════════════════════════════════════════
       TRUST / STATS STRIP — overlaps the bottom edge of the hero
  ══════════════════════════════════════════════════════════════════ -->
  <div class="stats-strip">
    <div class="stat-item">
      <div class="icon-wrap">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 6.5h11M4 12h11M4 17.5h6"/>
          <path d="M16.5 15l2 2 3.5-4"/>
        </svg>
      </div>
      <span class="stat-num">${empty navCategories ? '7' : navCategories.size()}</span>
      <span class="stat-label">Nhóm dịch vụ chăm sóc</span>
    </div>
    <div class="stat-item">
      <div class="icon-wrap">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4.5 3v6.5a4.5 4.5 0 0 0 9 0V3"/>
          <path d="M13.5 12v2.5a5.5 5.5 0 0 1-11 0V12"/>
          <circle cx="18.5" cy="15.5" r="2.5"/>
          <path d="M16.3 15.5a2.5 2.5 0 0 1-2.8-2.5"/>
        </svg>
      </div>
      <span class="stat-num">Chuyên khoa</span>
      <span class="stat-label">Bác sĩ thú y giàu kinh nghiệm</span>
    </div>
    <div class="stat-item">
      <div class="icon-wrap">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="8.5"/>
          <path d="M12 7v5.2l3.5 2"/>
        </svg>
      </div>
      <span class="stat-num">24/7</span>
      <span class="stat-label">Đặt lịch trực tuyến</span>
    </div>
    <div class="stat-item">
      <div class="icon-wrap">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 20.3s-7-4.3-9.3-8.7C.9 8.1 2.5 4.6 6 4.1c2-.3 3.6.7 6 2.9 2.4-2.2 4-3.2 6-2.9 3.5.5 5.1 4 3.3 7.5-2.3 4.4-9.3 8.7-9.3 8.7Z"/>
        </svg>
      </div>
      <span class="stat-num">Tận tâm</span>
      <span class="stat-label">Với từng người bạn bốn chân</span>
    </div>
  </div>

  <!-- ══════════════════════════════════════════════════════════════════
       WHY PETCLINIC
  ══════════════════════════════════════════════════════════════════ -->
  <section class="section" style="background:#fff;">
    <div class="section-title">Tại sao chọn PetClinic?</div>
    <div class="section-subtitle">Chúng tôi đặt sức khỏe thú cưng của bạn lên hàng đầu</div>
    <div class="card-grid">
      <div class="feature-card">
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4.5 3v6.5a4.5 4.5 0 0 0 9 0V3"/>
            <path d="M13.5 12v2.5a5.5 5.5 0 0 1-11 0V12"/>
            <circle cx="18.5" cy="15.5" r="2.5"/>
            <path d="M16.3 15.5a2.5 2.5 0 0 1-2.8-2.5"/>
          </svg>
        </div>
        <h3>Bác sĩ chuyên nghiệp</h3>
        <p>Đội ngũ bác sĩ thú y được đào tạo bài bản với nhiều năm kinh nghiệm thực tiễn.</p>
      </div>
      <div class="feature-card">
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 21V5a1 1 0 0 1 1-1h7a1 1 0 0 1 1 1v16"/>
            <path d="M13 10h6a1 1 0 0 1 1 1v10"/>
            <path d="M2.5 21h19"/>
            <path d="M7 7.5h2M7 11.5h2M7 15.5h2"/>
            <path d="M16.5 14.5h1M16.5 17.5h1"/>
          </svg>
        </div>
        <h3>Cơ sở hiện đại</h3>
        <p>Trang thiết bị y tế tiên tiến, phòng khám vô trùng theo tiêu chuẩn quốc tế.</p>
      </div>
      <div class="feature-card">
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 3l2 2-2.2 2.2"/>
            <path d="M18.8 5.2 16 8"/>
            <path d="M14.5 6.5l3 3L8 19l-4.5 1.5L5 16 14.5 6.5Z"/>
            <path d="M11.5 9.5l3 3M9 12l3 3"/>
          </svg>
        </div>
        <h3>Thuốc chính hãng</h3>
        <p>Toàn bộ thuốc và vaccine được nhập khẩu từ các nhà cung cấp uy tín.</p>
      </div>
      <div class="feature-card">
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3.5" y="5" width="17" height="15" rx="2"/>
            <path d="M3.5 9.5h17"/>
            <path d="M8 3v3M16 3v3"/>
            <path d="M8.5 14.2l2 2 4.5-4.5"/>
          </svg>
        </div>
        <h3>Đặt lịch dễ dàng</h3>
        <p>Đặt lịch khám trực tuyến nhanh chóng, nhận nhắc nhở tự động qua email.</p>
      </div>
    </div>
  </section>

  <!-- ══════════════════════════════════════════════════════════════════
       GIỚI THIỆU — Lời giới thiệu (#intro)
  ══════════════════════════════════════════════════════════════════ -->
  <section class="about-section" id="intro">
    <span class="about-kicker">Giới thiệu</span>
    <div class="about-intro-grid">
      <div class="about-intro-text">
        <h2>Một phòng khám, một lời cam kết bằng cả trái tim</h2>
        <p>PetClinic ra đời từ một tâm niệm giản dị: mỗi thú cưng đều xứng đáng được chăm sóc
           tận tâm như một thành viên trong gia đình. Từ những buổi khám sức khỏe định kỳ,
           tiêm phòng, cho đến các ca điều trị và phẫu thuật chuyên sâu, đội ngũ bác sĩ thú y
           của chúng tôi luôn đồng hành cùng bạn ở mọi bước trên hành trình chăm sóc người bạn
           bốn chân.</p>
        <p>Không dừng lại ở vai trò một phòng khám, PetClinic hướng đến trở thành người bạn
           đồng hành đáng tin cậy của mọi gia đình có thú cưng — nơi công nghệ đặt lịch hiện
           đại gặp gỡ sự chăm sóc y tế tận tâm, giúp việc theo dõi sức khỏe thú cưng trở nên
           đơn giản và an tâm hơn bao giờ hết.</p>
      </div>
      <div class="about-commit-card">
        <div class="about-commit-item">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3l7 3v6c0 4.5-3 8-7 9-4-1-7-4.5-7-9V6l7-3Z"/>
              <path d="M9 12l2 2 4-4"/>
            </svg>
          </div>
          <div class="txt">
            <h4>An toàn &amp; vô trùng</h4>
            <p>Quy trình khám chữa tuân thủ nghiêm ngặt tiêu chuẩn vệ sinh thú y.</p>
          </div>
        </div>
        <div class="about-commit-item">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="8.5"/>
              <path d="M12 7v5.2l3.5 2"/>
            </svg>
          </div>
          <div class="txt">
            <h4>Đúng giờ, đúng hẹn</h4>
            <p>Đặt lịch trực tuyến giúp bạn chủ động thời gian, hạn chế chờ đợi.</p>
          </div>
        </div>
        <div class="about-commit-item">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 20.3s-7-4.3-9.3-8.7C.9 8.1 2.5 4.6 6 4.1c2-.3 3.6.7 6 2.9 2.4-2.2 4-3.2 6-2.9 3.5.5 5.1 4 3.3 7.5-2.3 4.4-9.3 8.7-9.3 8.7Z"/>
            </svg>
          </div>
          <div class="txt">
            <h4>Tận tâm &amp; thấu hiểu</h4>
            <p>Mỗi thú cưng được theo dõi và chăm sóc như một cá thể riêng biệt.</p>
          </div>
        </div>
      </div>
    </div>
  </section>

  <!-- ══════════════════════════════════════════════════════════════════
       CƠ SỞ VẬT CHẤT (#facility)
  ══════════════════════════════════════════════════════════════════ -->
  <section class="about-section alt" id="facility">
    <span class="about-kicker">Cơ sở vật chất</span>
    <div class="section-title">Không gian được thiết kế vì thú cưng của bạn</div>
    <div class="section-subtitle">Trang thiết bị hiện đại, quy trình khép kín từ khám bệnh đến hồi phục</div>
    <div class="facility-grid">
      <div class="facility-card">
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4.5 3v6.5a4.5 4.5 0 0 0 9 0V3"/>
            <path d="M13.5 12v2.5a5.5 5.5 0 0 1-11 0V12"/>
            <circle cx="18.5" cy="15.5" r="2.5"/>
            <path d="M16.3 15.5a2.5 2.5 0 0 1-2.8-2.5"/>
          </svg>
        </div>
        <h3>Phòng khám &amp; điều trị</h3>
        <p>Không gian khám tổng quát, điều trị nội khoa được vệ sinh vô trùng theo quy chuẩn.</p>
      </div>
      <div class="facility-card">
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 20l10-10"/>
            <path d="M14 10l2.3-2.3a2 2 0 0 1 2.9 2.8L17 13"/>
            <path d="M4 20l1.3-4.2L9 14.5"/>
          </svg>
        </div>
        <h3>Phòng phẫu thuật</h3>
        <p>Khu phẫu thuật riêng biệt, trang bị thiết bị gây mê và hồi sức hiện đại.</p>
      </div>
      <div class="facility-card">
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="currentColor" stroke="none">
            <ellipse cx="7.5" cy="9" rx="1.7" ry="2.2"/>
            <ellipse cx="12" cy="6.5" rx="1.7" ry="2.2"/>
            <ellipse cx="16.5" cy="9" rx="1.7" ry="2.2"/>
            <path d="M12 12c-3.1 0-5.6 2-5.6 4.6 0 1.5 1.3 2.7 2.9 2.4.9-.2 1.7-.6 2.7-.6s1.8.4 2.7.6c1.6.3 2.9-.9 2.9-2.4 0-2.6-2.5-4.6-5.6-4.6Z"/>
          </svg>
        </div>
        <h3>Grooming &amp; Spa</h3>
        <p>Không gian tắm gội, cắt tỉa và chăm sóc làm đẹp thoải mái cho thú cưng.</p>
      </div>
      <div class="facility-card">
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <rect x="5" y="4" width="14" height="17" rx="2"/>
            <path d="M9 3.5h6a1 1 0 0 1 1 1V6H8V4.5a1 1 0 0 1 1-1Z"/>
            <path d="M7.5 12h2.5l1.3-2.6 1.8 4.4 1.3-2.1h2.1"/>
          </svg>
        </div>
        <h3>Chẩn đoán &amp; xét nghiệm</h3>
        <p>Hỗ trợ bác sĩ phát hiện sớm các vấn đề sức khỏe tiềm ẩn.</p>
      </div>
      <div class="facility-card">
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 20.3s-7-4.3-9.3-8.7C.9 8.1 2.5 4.6 6 4.1c2-.3 3.6.7 6 2.9 2.4-2.2 4-3.2 6-2.9 3.5.5 5.1 4 3.3 7.5-2.3 4.4-9.3 8.7-9.3 8.7Z"/>
          </svg>
        </div>
        <h3>Khu lưu trú nội trú</h3>
        <p>Không gian nghỉ dưỡng cho thú cưng cần theo dõi dài ngày sau điều trị.</p>
      </div>
      <div class="facility-card">
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 21c-4-1-7-4.8-7-9.5C5 7 8 4 12 3c4 1 7 4 7 8.5 0 4.7-3 8.5-7 9.5Z"/>
            <path d="M12 3v18"/>
          </svg>
        </div>
        <h3>Góc chờ thân thiện</h3>
        <p>Không gian chờ thoáng đãng, giảm căng thẳng cho cả chủ nuôi và thú cưng.</p>
      </div>
    </div>
  </section>

  <!-- ══════════════════════════════════════════════════════════════════
       ĐỘI NGŨ (#team)
  ══════════════════════════════════════════════════════════════════ -->
  <section class="about-section" id="team">
    <span class="about-kicker">Đội ngũ</span>
    <div class="section-title">Luôn sẵn sàng vì thú cưng của bạn</div>
    <div class="section-subtitle">Những con người tận tâm phía sau mỗi lượt khám</div>
    <div class="team-grid">
      <div class="team-card">
        <div class="team-avatar">Đ</div>
        <h3>Lê Trung Đức</h3>
        <span class="team-role">Bác sĩ thú y</span>
        <p>Chuyên khoa Phẫu thuật ngoại khoa</p>
      </div>
      <div class="team-card">
        <div class="team-avatar">T</div>
        <h3>Hoàng Minh Tâm</h3>
        <span class="team-role">Bác sĩ thú y</span>
        <p>Chuyên khoa Ký sinh trùng &amp; Vaccine</p>
      </div>
      <div class="team-card">
        <div class="team-avatar">G</div>
        <h3>Nguyễn Thị Groomer</h3>
        <span class="team-role">Chuyên viên Grooming</span>
        <p>Chăm sóc lông &amp; làm đẹp cho thú cưng</p>
      </div>
      <div class="team-card">
        <div class="team-avatar">B</div>
        <h3>Trần Thị Bình</h3>
        <span class="team-role">Lễ tân &amp; Thu ngân</span>
        <p>Hỗ trợ đặt lịch, tư vấn và giải đáp thắc mắc</p>
      </div>
    </div>
  </section>

  <!-- ══════════════════════════════════════════════════════════════════
       TẦM NHÌN VÀ PHÁT TRIỂN (#vision)
  ══════════════════════════════════════════════════════════════════ -->
  <section class="about-section alt" id="vision">
    <span class="about-kicker">Tầm nhìn &amp; phát triển</span>
    <div class="vision-wrap">
      <p class="vision-quote">
        "Chúng tôi tin rằng chăm sóc thú cưng không chỉ dừng lại ở việc chữa bệnh —
        đó là một hành trình đồng hành lâu dài giữa phòng khám, chủ nuôi và người bạn bốn chân."
      </p>
      <div class="vision-pillars">
        <div class="vision-pillar">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3l7 3v6c0 4.5-3 8-7 9-4-1-7-4.5-7-9V6l7-3Z"/>
              <path d="M9 12l2 2 4-4"/>
            </svg>
          </div>
          <h4>Chăm sóc toàn diện</h4>
          <p>Từ khám sức khỏe định kỳ, tiêm phòng đến điều trị chuyên sâu — tất cả trong một hệ thống duy nhất.</p>
        </div>
        <div class="vision-pillar">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3.5" y="5" width="17" height="15" rx="2"/>
              <path d="M3.5 9.5h17"/>
              <path d="M8 3v3M16 3v3"/>
              <path d="M8.5 14.2l2 2 4.5-4.5"/>
            </svg>
          </div>
          <h4>Công nghệ đồng hành</h4>
          <p>Đặt lịch, theo dõi hồ sơ và nhận nhắc nhở tự động chỉ trong vài thao tác.</p>
        </div>
        <div class="vision-pillar">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 20.3s-7-4.3-9.3-8.7C.9 8.1 2.5 4.6 6 4.1c2-.3 3.6.7 6 2.9 2.4-2.2 4-3.2 6-2.9 3.5.5 5.1 4 3.3 7.5-2.3 4.4-9.3 8.7-9.3 8.7Z"/>
            </svg>
          </div>
          <h4>Tận tâm &amp; minh bạch</h4>
          <p>Chi phí rõ ràng, tư vấn trung thực, luôn đặt lợi ích của thú cưng lên hàng đầu.</p>
        </div>
      </div>
    </div>
  </section>

  <!-- ══════════════════════════════════════════════════════════════════
       DỊCH VỤ NỔI BẬT  (dynamic — từ ServiceDAO)
  ══════════════════════════════════════════════════════════════════ -->
  <section class="section">
    <div class="section-title">Dịch vụ nổi bật</div>
    <div class="section-subtitle">Từ khám tổng quát đến phẫu thuật chuyên sâu</div>
    <div class="card-grid">
      <c:forEach var="cat" items="${navCategories}">
        <div class="feature-card" style="text-align:left;">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 6.5h11M4 12h11M4 17.5h6"/>
              <path d="M16.5 15l2 2 3.5-4"/>
            </svg>
          </div>
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

  <!-- ══════════════════════════════════════════════════════════════════
       CTA
  ══════════════════════════════════════════════════════════════════ -->
  <section class="home-cta-band">
    <h2>Sẵn sàng đặt lịch khám?</h2>
    <p>Đăng ký tài khoản miễn phí và đặt lịch ngay hôm nay.</p>
    <a href="${ctx}/auth/register" class="cta-primary">Bắt đầu ngay</a>
  </section>

</main>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>
