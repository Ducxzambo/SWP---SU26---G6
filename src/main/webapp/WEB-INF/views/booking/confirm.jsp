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
  <div style="text-align:center;margin-bottom:28px;">
    <h1 style="font-family:'Playfair Display',serif;font-size:26px;color:var(--green-900);margin-bottom:6px;">
      Xác nhận đặt lịch
    </h1>
    <p style="color:var(--warm-gray);font-size:14px;">
      Vui lòng kiểm tra lại thông tin trước khi xác nhận
    </p>
  </div>

  <div class="confirm-box">
    <div class="confirm-box-head">Thông tin đặt lịch</div>

    <c:choose>
      <c:when test="${isInpatient}">
        <table class="confirm-table">
          <tr>
            <td>Thú cưng</td>
            <td>
              <c:forEach var="pet" items="${selectedPets}">
                <span class="confirm-chip confirm-chip--pet">
                  ${pet.name} <span style="opacity:.65;">(${pet.speciesName})</span>
                </span>
              </c:forEach>
            </td>
          </tr>
          <tr>
            <td>Dịch vụ</td>
            <td><span class="confirm-chip">Nội trú</span></td>
          </tr>
          <tr>
            <td>Ngày nhập viện</td>
            <td><strong>${inpatientDate}</strong></td>
          </tr>
          <tr>
            <td>Buổi</td>
            <td>${inpatientPeriod}</td>
          </tr>
        </table>
      </c:when>

      <c:otherwise>
        <div class="confirm-pet-list">
          <c:forEach var="row" items="${petBreakdown}">
            <div class="confirm-pet-card">
              <div class="confirm-pet-title">
                ${row.pet.name}
                <span>${row.pet.speciesName}</span>
              </div>

              <div class="confirm-item-list">
                <c:forEach var="svc" items="${row.services}">
                  <div class="confirm-item-row">
                    <span class="confirm-item-name">${svc.name}</span>
                    <span class="confirm-item-price"><fmt:formatNumber value="${svc.price}" type="number" groupingUsed="true"/>đ</span>
                  </div>
                </c:forEach>
                <c:forEach var="vac" items="${row.vaccines}">
                  <div class="confirm-item-row">
                    <span class="confirm-item-name">${vac.name}</span>
                    <span class="confirm-item-price"><fmt:formatNumber value="${vac.unitPrice}" type="number" groupingUsed="true"/>đ</span>
                  </div>
                </c:forEach>
              </div>
            </div>
          </c:forEach>
        </div>

        <div class="confirm-box-head" style="background:var(--green-50);color:var(--text-dark);font-size:14px;border-top:1px solid var(--border);">
          Khung giờ đã chọn
        </div>
        <table class="confirm-table">
          <tr>
            <td>Thời gian</td>
            <td id="slot-0">${slotKey}</td>
          </tr>
        </table>
      </c:otherwise>
    </c:choose>

    <c:if test="${not empty notes}">
      <div class="confirm-box-head" style="background:var(--green-50);color:var(--text-dark);font-size:14px;border-top:1px solid var(--border);">
        Ghi chú
      </div>
      <table class="confirm-table">
        <tr><td colspan="2">${notes}</td></tr>
      </table>
    </c:if>

    <div class="confirm-box-head" style="background:var(--green-50);color:var(--text-dark);font-size:14px;border-top:1px solid var(--border);">
      Ước tính chi phí
    </div>
    <table class="confirm-table">
      <c:if test="${!isInpatient}">
        <tr style="background:var(--green-50);">
          <td style="font-weight:600;color:var(--green-900);">Cần thanh toán (100%)</td>
          <td style="font-weight:700;color:var(--green-700);font-size:16px;">
            <fmt:formatNumber value="${totalPrice}" type="number" groupingUsed="true"/>đ
          </td>
        </tr>
      </c:if>
      <c:if test="${isInpatient}">
        <tr style="background:var(--green-50);">
          <td style="font-weight:600;color:var(--green-900);">Đặt cọc nội trú</td>
          <td style="font-weight:700;color:var(--green-700);font-size:16px;">
            <fmt:formatNumber value="${depositAmount}" type="number" groupingUsed="true"/>đ
          </td>
        </tr>
      </c:if>
    </table>
  </div>

  <div style="background:var(--green-50);border:1px solid var(--green-100);border-radius:10px;padding:14px 18px;margin-top:20px;font-size:13.5px;color:var(--green-700);line-height:1.7;">
    <strong>Lưu ý:</strong> Sau khi đặt lịch, trạng thái sẽ là <strong>Chờ xác nhận</strong>.
    Nếu có bất kỳ yêu cầu hay thay đổi, vui lòng liên hệ trước lịch hẹn 12 tiếng.
    Bạn có thể mở rộng các dịch vụ khi đến khám.
  </div>

  <div class="confirm-actions">
    <a href="${ctx}/booking/new" class="btn-back">Quay lại chỉnh sửa</a>
    <form action="${ctx}/booking/confirm" method="post" style="flex:2;">
      <button type="submit" class="btn-confirm" style="width:100%;">
        Xác nhận đặt lịch
      </button>
    </form>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
<script src="${ctx}/js/confirm.js"></script>
</body>
</html>
