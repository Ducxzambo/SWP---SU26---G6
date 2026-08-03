<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Xác nhận đặt lịch - PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/booking.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="confirm-card">

  <div class="confirm-page-header">
    <h1>Xác nhận đặt lịch</h1>
    <p>Vui lòng kiểm tra lại thông tin trước khi xác nhận</p>
  </div>

  <div class="confirm-box">
    <div class="confirm-box-head">Thông tin đặt lịch</div>

    <c:if test="${not empty selectedPet}">
      <div class="confirm-box-subhead">Thú cưng</div>
      <table class="confirm-table">
        <tr>
          <td>Tên</td>
          <td><strong><c:out value="${selectedPet.name}"/></strong></td>
        </tr>
        <tr>
          <td>Loài / Giống</td>
          <td>
            <c:out value="${selectedPet.speciesName}"/>
            <c:if test="${not empty selectedPet.breedName}"> / <c:out value="${selectedPet.breedName}"/></c:if>
          </td>
        </tr>
      </table>
    </c:if>

    <c:choose>
      <c:when test="${isInpatient}">
        <table class="confirm-table">
          <tr><td>Dịch vụ</td><td><span class="confirm-chip">Nội trú</span></td></tr>
          <tr><td>Ngày nhập viện</td><td><strong>${inpatientDate}</strong></td></tr>
          <tr><td>Buổi</td><td>${inpatientPeriod}</td></tr>
        </table>
      </c:when>

      <c:otherwise>
        <div class="confirm-item-list">
          <c:forEach var="svc" items="${services}">
            <div class="confirm-item-row">
              <span class="confirm-item-name">${svc.name}</span>
              <span class="confirm-item-price"><fmt:formatNumber value="${svc.price}" type="number" groupingUsed="true"/>đ</span>
            </div>
          </c:forEach>
          <c:forEach var="vac" items="${vaccines}">
            <div class="confirm-item-row">
              <span class="confirm-item-name">${vac.name}</span>
              <span class="confirm-item-price"><fmt:formatNumber value="${vac.unitPrice}" type="number" groupingUsed="true"/>đ</span>
            </div>
          </c:forEach>
        </div>

        <div class="confirm-box-subhead">Khung giờ đã chọn</div>
        <table class="confirm-table">
          <tr><td>Thời gian</td><td id="slot-0">${slotKey}</td></tr>
        </table>
      </c:otherwise>
    </c:choose>

    <c:if test="${not empty notes}">
      <div class="confirm-box-subhead">Ghi chú</div>
      <table class="confirm-table">
        <tr><td colspan="2">${notes}</td></tr>
      </table>
    </c:if>
  </div>

  <div class="confirm-info-note">
    <strong>Lưu ý:</strong> Sau khi đặt lịch, trạng thái sẽ là <strong>Chờ xác nhận</strong>.
    Nếu có bất kỳ yêu cầu hay thay đổi, vui lòng liên hệ trước lịch hẹn 12 tiếng.
    Bạn có thể mở rộng các dịch vụ khi đến khám.
  </div>

  <div class="confirm-actions">
    <a href="${ctx}/booking/new" class="btn-back">Quay lại chỉnh sửa</a>
    <form action="${ctx}/booking/confirm" method="post" class="confirm-submit-form">
      <button type="submit" class="btn-confirm">Xác nhận đặt lịch</button>
    </form>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
<script src="${ctx}/js/confirm.js"></script>
</body>
</html>