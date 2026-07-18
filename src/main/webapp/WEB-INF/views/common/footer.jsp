<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<footer class="site-footer">
  <div class="footer-grid">

    <div class="footer-brand">
      <div class="logo">🐾 Pet<span>Clinic</span></div>
      <p>Chúng tôi cung cấp dịch vụ thú y toàn diện với đội ngũ bác sĩ giàu kinh nghiệm,
         tận tâm chăm sóc mỗi người bạn bốn chân của bạn.</p>
    </div>

    <div class="footer-col">
      <h4>Dịch vụ</h4>
      <ul>
        <c:forEach var="cat" items="${navCategories}">
          <li><a href="${ctx}/services?category=${cat.categoryID}">${cat.name}</a></li>
        </c:forEach>
        <c:if test="${empty navCategories}">
          <li><a href="${ctx}/services">Xem tất cả dịch vụ</a></li>
        </c:if>
      </ul>
    </div>

    <div class="footer-col">
      <h4>Thông tin</h4>
      <ul>
        <li><a href="${ctx}/#intro">Giới thiệu</a></li>
        <li><a href="${ctx}/#facility">Cơ sở vật chất</a></li>
        <li><a href="${ctx}/#team">Đội ngũ</a></li>
        <li><a href="${ctx}/contact">Liên hệ</a></li>
      </ul>
    </div>

    <div class="footer-col">
      <h4>Liên hệ</h4>
      <ul>
        <li><a href="tel:+84123456789">(028) 123 456 789</a></li>
        <li><a href="mailto:petclinicweb123@gmail.com">petclinicweb123@gmail.com</a></li>
        <li><a href="${ctx}/contact">123 Đường ABC, TP. Hà Nội</a></li>
        <li><a href="${ctx}/contact">T2–T7: 8:00 – 17:00</a></li>
      </ul>
    </div>

  </div>
  <div class="footer-bottom">
    © 2026 PetClinic. Mọi quyền được bảo lưu.
  </div>
</footer>

<script src="${ctx}/js/main.js"></script>
