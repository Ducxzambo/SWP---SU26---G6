<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Xác nhận đặt lịch – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="confirm-card">

  <div style="text-align:center;margin-bottom:28px;">
    <div style="font-size:48px;margin-bottom:12px;"></div>
    <h1 style="font-family:'Playfair Display',serif;font-size:26px;
               color:var(--green-900);margin-bottom:6px;">Xác nhận đặt lịch</h1>
    <p style="color:var(--warm-gray);font-size:14px;">
      Vui lòng kiểm tra lại thông tin trước khi xác nhận
    </p>
  </div>

  <div class="confirm-box">
    <div class="confirm-box-head">Thông tin dịch vụ</div>
    <table class="confirm-table">
      <tr>
        <td>Dịch vụ</td>
        <td>
          <c:forEach var="svc" items="${selectedServices}" varStatus="vs">
            <c:if test="${svc.price > 0}">
              <span style="display:inline-block;padding:3px 10px;background:var(--green-50);
                           border:1px solid var(--green-100);border-radius:20px;
                           font-size:13px;color:var(--green-700);margin:2px;">
                ${svc.name}
              </span>
            </c:if>
          </c:forEach>
          <c:forEach var="vac" items="${selectedVaccines}" varStatus="vs">
            <span style="display:inline-block;padding:3px 10px;background:var(--green-50);
                         border:1px solid var(--green-100);border-radius:20px;
                         font-size:13px;color:var(--green-700);margin:2px;">
              ${vac.name}
            </span>
          </c:forEach>
        </td>
      </tr>
      <tr>
        <td>Thú cưng</td>
        <td>
          <c:forEach var="pet" items="${selectedPets}" varStatus="vs">
            <span style="display:inline-block;padding:3px 10px;background:var(--sand);
                         border:1px solid var(--border);border-radius:20px;
                         font-size:13px;margin:2px;">
              ${pet.name} <span style="opacity:.6;">(${pet.speciesName})</span>
            </span>
          </c:forEach>
        </td>
      </tr>
    </table>

    <!-- Outpatient: show slot list -->
    <c:if test="${!isInpatient}">
      <div class="confirm-box-head" style="background:var(--green-50);color:var(--text-dark);
           font-size:14px;border-top:1px solid var(--border);">
        Khung giờ đã chọn
      </div>
      <table class="confirm-table">
        <c:if test="${not empty slotKey}">
          <tr>
            <td id="slot-0" style="color:var(--text-mid);font-size:14px;">
              ${slotKey}
            </td>
            <td><span style="color:var(--green-700);font-size:13px;"></span></td>
          </tr>
        </c:if>
      </table>
    </c:if>

    <!-- Inpatient: show dates -->
    <c:if test="${isInpatient}">
      <div class="confirm-box-head" style="background:var(--green-50);color:var(--text-dark);
           font-size:14px;border-top:1px solid var(--border);">
        Thời gian nội trú
      </div>
      <table class="confirm-table">
        <tr>
          <td>Ngày nhập viện</td>
          <td><strong>${startDate}</strong></td>
        </tr>
        <c:if test="${not empty endDate}">
          <tr>
            <td>Dự kiến xuất viện</td>
            <td>${endDate}</td>
          </tr>
        </c:if>
      </table>
    </c:if>

    <!-- Notes -->
    <c:if test="${not empty notes}">
      <div class="confirm-box-head" style="background:var(--green-50);color:var(--text-dark);
           font-size:14px;border-top:1px solid var(--border);">
        Ghi chú
      </div>
      <table class="confirm-table">
        <tr><td colspan="2">${notes}</td></tr>
      </table>
    </c:if>

    <!-- Cost estimate -->
    <div class="confirm-box-head" style="background:var(--green-50);color:var(--text-dark);
         font-size:14px;border-top:1px solid var(--border);">
      Ước tính chi phí
    </div>
    <table class="confirm-table">
      <%--
        Sửa lỗi: đã loại bỏ biến "totalCost" và "lineCount" cũ — đây là dead code,
        được set nhưng không bao giờ được dùng để tính toán hay hiển thị (giá trị
        hiển thị thực tế lấy từ biến request-scope "totalPrice" do controller tính sẵn).
        "lineCount" còn bị gán đè 2 lần liên tiếp trong bản gốc, là dấu hiệu code thử
        nghiệm còn sót lại. Giữ lại đúng phần đang thực sự render ra UI.
      --%>
      <c:forEach var="svc" items="${selectedServices}">
        <c:if test="${svc.price > 0}">
          <tr>
            <td>${svc.name}
              <span style="font-size:12px;color:var(--warm-gray);">
                ${selectedPets.size()} thú cưng
              </span>
            </td>
            <td>
              <fmt:formatNumber value="${svc.price}" type="number" groupingUsed="true"/>₫
            </td>
          </tr>
        </c:if>
      </c:forEach>
      <c:forEach var="vac" items="${selectedVaccines}">
        <tr>
          <td>${vac.name}
            <span style="font-size:12px;color:var(--warm-gray);">
              ${selectedPets.size()} thú cưng
            </span>
          </td>
          <td>
            <fmt:formatNumber value="${vac.unitPrice}" type="number" groupingUsed="true"/>₫
          </td>
        </tr>
      </c:forEach>
      <tr style="background:var(--green-50);">
        <td style="font-weight:600;color:var(--green-900);">Tổng ước tính</td>
        <td style="font-weight:700;color:var(--green-700);font-size:16px;">
          <fmt:formatNumber value="${totalPrice}" type="number" groupingUsed="true"/>₫
        </td>
      </tr>
      <tr>
        <td colspan="2" style="font-size:12px;color:var(--warm-gray);padding-top:8px;">
          * Giá chính thức sẽ được xác nhận sau khi bác sĩ kiểm tra.
          Thanh toán tại phòng khám sau khi hoàn tất dịch vụ.
        </td>
      </tr>
    </table>
  </div>

  <!-- Info note -->
  <div style="background:var(--green-50);border:1px solid var(--green-100);
              border-radius:10px;padding:14px 18px;margin-top:20px;
              font-size:13.5px;color:var(--green-700);line-height:1.7;">
    <strong>Lưu ý:</strong> Sau khi đặt lịch, trạng thái sẽ là <strong>Chờ xác nhận</strong>.
    Chúng tôi sẽ liên hệ xác nhận trong vòng 12 giờ làm việc.
    Nếu cần thay đổi, vui lòng liên hệ trước ít nhất <strong>12 tiếng</strong>.
  </div>

  <!-- Actions -->
  <div class="confirm-actions">
    <a href="${ctx}/booking/new" class="btn-back">← Quay lại chỉnh sửa</a>
    <form action="${ctx}/booking/confirm" method="post" style="flex:2;">
      <button type="submit" class="btn-confirm" style="width:100%;">
        ✓ Xác nhận đặt lịch
      </button>
    </form>
  </div>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script src="${ctx}/js/confirm.js"></script>
</body>
</html>
