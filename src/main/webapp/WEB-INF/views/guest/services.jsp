<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Dịch vụ – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/content.css">
</head>
<body>

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<main class="main-content">

  <section class="page-hero">
    <h1>Dịch vụ của chúng tôi</h1>
    <p>Khám tổng quát, tiêm phòng, phẫu thuật, grooming và nhiều dịch vụ khác — minh bạch giá, đặt lịch trực tuyến nhanh chóng.</p>
  </section>

  <div class="svc-wrap">
    <c:if test="${empty categories}">
      <div class="svc-empty">Hiện chưa có dịch vụ nào được thiết lập.</div>
    </c:if>

    <c:forEach var="cat" items="${categories}">
      <div class="svc-category" id="cat-${cat.categoryID}">
        <div class="svc-category-head">
          <div class="icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 6.5h11M4 12h11M4 17.5h6"/>
              <path d="M16.5 15l2 2 3.5-4"/>
            </svg>
          </div>
          <h2>${cat.name}</h2>
        </div>

        <c:choose>
          <c:when test="${empty cat.services}">
            <div class="svc-empty" style="padding:20px;">Nhóm dịch vụ này chưa có mục nào đang hoạt động.</div>
          </c:when>
          <c:otherwise>
            <div class="svc-list">
              <c:forEach var="svc" items="${cat.services}">
                <div class="svc-item ${focusService == svc.serviceID ? 'highlight' : ''}" id="svc-${svc.serviceID}">
                  <div class="svc-item-info">
                    <h4>${svc.name}</h4>
                    <div class="svc-item-meta">${svc.durationMinutes} phút</div>
                  </div>
                  <div class="svc-item-price">
                    <fmt:formatNumber value="${svc.price}" type="number" groupingUsed="true"/>đ
                  </div>
                </div>
              </c:forEach>
            </div>
            <a href="${ctx}/booking/new?prefillCat=${cat.categoryID}"
               style="display:inline-block;margin-top:18px;font-size:13.5px;color:var(--green-500);font-weight:600;">
              Đặt lịch nhóm dịch vụ này →
            </a>
          </c:otherwise>
        </c:choose>
      </div>
    </c:forEach>
  </div>

</main>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<c:if test="${focusCategory > 0}">
<script>
  document.addEventListener('DOMContentLoaded', function () {
    var target = document.getElementById('cat-${focusCategory}');
    if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
</script>
</c:if>
</body>
</html>
