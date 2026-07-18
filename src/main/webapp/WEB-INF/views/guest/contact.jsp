<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Liên hệ – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/content.css">
</head>
<body>

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<main class="main-content">

  <section class="page-hero">
    <h1>Liên hệ với chúng tôi</h1>
    <p>Có câu hỏi về dịch vụ hoặc cần hỗ trợ đặt lịch? Gửi tin nhắn cho PetClinic, chúng tôi sẽ phản hồi sớm nhất có thể.</p>
  </section>

  <div class="contact-wrap">
    <div class="contact-grid">

      <!-- Info card -->
      <div class="contact-info-card">
        <h2>Thông tin liên hệ</h2>

        <div class="contact-info-item">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20.5 16.9v2.6a1.6 1.6 0 0 1-1.75 1.6 15.8 15.8 0 0 1-6.9-2.45 15.6 15.6 0 0 1-4.8-4.8 15.8 15.8 0 0 1-2.45-6.93A1.6 1.6 0 0 1 6.14 4.5h2.6a1.6 1.6 0 0 1 1.6 1.38c.11.84.32 1.66.6 2.45a1.6 1.6 0 0 1-.36 1.69L9.4 11.2a12.8 12.8 0 0 0 4.8 4.8l1.18-1.18a1.6 1.6 0 0 1 1.69-.36c.79.29 1.61.5 2.45.61a1.6 1.6 0 0 1 1.38 1.63Z"/>
            </svg>
          </div>
          <div class="txt">
            <h4>Hotline</h4>
            <a href="tel:+84123456789">(028) 123 456 789</a>
          </div>
        </div>

        <div class="contact-info-item">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="5" width="18" height="14" rx="2"/>
              <path d="M3.5 6.5 12 12.5l8.5-6"/>
            </svg>
          </div>
          <div class="txt">
            <h4>Email</h4>
            <a href="mailto:petclinicweb123@gmail.com">petclinicweb123@gmail.com</a>
          </div>
        </div>

        <div class="contact-info-item">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 21.5s-7-6.1-7-11.5a7 7 0 0 1 14 0c0 5.4-7 11.5-7 11.5Z"/>
              <circle cx="12" cy="10" r="2.6"/>
            </svg>
          </div>
          <div class="txt">
            <h4>Địa chỉ</h4>
            <p>123 Đường ABC, TP. Hà Nội</p>
          </div>
        </div>

        <div class="contact-info-item">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="8.5"/>
              <path d="M12 7v5.2l3.5 2"/>
            </svg>
          </div>
          <div class="txt">
            <h4>Giờ làm việc</h4>
            <p>Thứ 2 – Thứ 7: 8:00 – 17:00</p>
          </div>
        </div>
      </div>

      <!-- Form card -->
      <div class="contact-form-card">
        <h2>Gửi tin nhắn cho chúng tôi</h2>

        <form method="post" action="${ctx}/contact">
          <div class="contact-form-row">
            <div class="contact-form-group">
              <label for="fullName">Họ tên</label>
              <input type="text" id="fullName" name="fullName" class="contact-input"
                     value="${prefillName}" placeholder="Nguyễn Văn A" required>
            </div>
            <div class="contact-form-group">
              <label for="phone">Số điện thoại</label>
              <input type="tel" id="phone" name="phone" class="contact-input"
                     value="${prefillPhone}" placeholder="09xx xxx xxx">
            </div>
          </div>

          <div class="contact-form-group">
            <label for="email">Email</label>
            <input type="email" id="email" name="email" class="contact-input"
                   value="${prefillEmail}" placeholder="ban@email.com" required>
          </div>

          <div class="contact-form-group">
            <label for="subject">Tiêu đề</label>
            <input type="text" id="subject" name="subject" class="contact-input"
                   placeholder="Vd: Hỏi về dịch vụ tiêm phòng" required>
          </div>

          <div class="contact-form-group">
            <label for="message">Nội dung</label>
            <textarea id="message" name="message" class="contact-textarea"
                      placeholder="Nội dung bạn muốn gửi cho PetClinic..." required></textarea>
          </div>

          <button type="submit" class="contact-submit-btn">Gửi tin nhắn</button>
        </form>
      </div>

    </div>
  </div>

</main>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>
