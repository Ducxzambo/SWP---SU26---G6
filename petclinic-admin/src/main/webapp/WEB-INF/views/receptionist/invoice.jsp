<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Hóa đơn #${invoice.invoiceID} - PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/booking.css">
  <style>
    /* Thanh trên cùng dành riêng cho nhân viên (không dùng header.jsp của khách hàng) */
    .staff-topbar {
      display: flex; align-items: center; justify-content: space-between;
      padding: 16px 28px; background: var(--green-900); color: #fff;
    }
    .staff-topbar .brand { font-family: 'Playfair Display', serif; font-size: 18px; }
    .staff-topbar .who   { font-size: 13.5px; color: var(--green-100); display: flex; align-items: center; gap: 14px; }
    .staff-topbar a      { color: #fff; opacity: .85; }
    .staff-topbar a:hover{ opacity: 1; text-decoration: underline; }

    .flash-warning { background: #fff3cd; color: #856404; border-bottom: 1px solid #ffe28a; }

    /* .pay-option ở booking.css vốn thiết kế cho <div onclick>; ở trang này mỗi
       lựa chọn là 1 action submit ngay lập tức nên bọc trong <button> — reset
       lại vài thuộc tính mặc định của <button> cho khớp style .pay-option cũ. */
    button.pay-option {
      width: 100%; text-align: left; font-family: inherit; color: inherit;
      appearance: none; -webkit-appearance: none;
    }
    button.pay-option:focus-visible { outline: 2px solid var(--green-400); outline-offset: 2px; }

    .invoice-due-box {
      background: var(--green-50); border: 1px solid var(--green-100); border-radius: var(--radius);
      padding: 16px 20px; margin-bottom: 24px;
    }
    .invoice-due-row {
      display: flex; justify-content: space-between; font-size: 14px; padding: 5px 0; color: var(--text-mid);
    }
    .invoice-due-row--total {
      border-top: 1px dashed var(--green-100); margin-top: 8px; padding-top: 10px;
      color: var(--green-900); font-weight: 700; font-size: 16px;
    }
  </style>
</head>
<body>

<div class="staff-topbar">
  <span class="brand">🐾 PetClinic — Lễ tân</span>
  <span class="who">
    <c:if test="${not empty staff}">${staff.fullName}</c:if>
    <a href="${ctx}/receptionist/${from}">← Quay lại ${from == 'history' ? 'Lịch sử' : 'Check-in'}</a>
  </span>
</div>

<c:if test="${not empty sessionScope.flashSuccess}">
  <div class="flash flash-success"><span>✓</span>&nbsp;${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashWarning}">
  <div class="flash flash-warning"><span>⚠</span>&nbsp;${sessionScope.flashWarning}</div>
  <c:remove var="flashWarning" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div class="flash flash-error"><span>✕</span>&nbsp;${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>

<div class="confirm-card">

  <div style="text-align:center;margin-bottom:28px;">
    <h1 style="font-family:'Playfair Display',serif;font-size:26px;color:var(--green-900);margin-bottom:6px;">
      Hóa đơn #${invoice.invoiceID}
    </h1>
    <p style="color:var(--warm-gray);font-size:14px;">
      Lịch hẹn #${appointment.appointmentID}
      <c:if test="${not empty appointment.customerName}"> — ${appointment.customerName}</c:if>
      <c:if test="${not empty appointment.petName}"> — 🐾 ${appointment.petName}</c:if>
    </p>
  </div>

  <!-- Thông tin lịch hẹn -->
  <div class="confirm-box" style="margin-bottom:20px;">
    <div class="confirm-box-head">Thông tin lịch hẹn</div>
    <table class="confirm-table">
      <tr>
        <td>Ngày khám</td>
        <td>${appointment.appointmentDate} · ${appointment.startTime}</td>
      </tr>
      <tr>
        <td>Trạng thái</td>
        <td><strong>${appointment.status}</strong></td>
      </tr>
    </table>
  </div>

  <!-- Chi tiết hóa đơn -->
  <div class="confirm-box" style="margin-bottom:20px;">
    <div class="confirm-box-head">Chi tiết hóa đơn</div>

    <div class="confirm-item-list">
      <c:forEach var="item" items="${invoice.items}">
        <div class="confirm-item-row">
          <span class="confirm-item-name">
            ${item.description}
            <c:if test="${item.quantity != 1}"> (x<fmt:formatNumber value="${item.quantity}" maxFractionDigits="2"/>)</c:if>
          </span>
          <span class="confirm-item-price"><fmt:formatNumber value="${item.lineTotal}" type="number" groupingUsed="true"/>đ</span>
        </div>
      </c:forEach>
      <c:if test="${empty invoice.items}">
        <div class="confirm-item-row">
          <span class="confirm-item-name" style="color:var(--warm-gray);">Chưa có dòng dịch vụ nào</span>
        </div>
      </c:if>
    </div>
  </div>

  <!-- Tổng hợp thanh toán -->
  <div class="invoice-due-box">
    <div class="invoice-due-row">
      <span>Tổng cộng</span>
      <strong><fmt:formatNumber value="${invoice.totalAmount}" type="number" groupingUsed="true"/>đ</strong>
    </div>
    <c:if test="${amountPaid > 0}">
      <div class="invoice-due-row">
        <span>Đã thanh toán</span>
        <strong><fmt:formatNumber value="${amountPaid}" type="number" groupingUsed="true"/>đ</strong>
      </div>
    </c:if>
    <div class="invoice-due-row invoice-due-row--total">
      <span>${fullyPaid ? 'Đã thanh toán đủ' : 'Còn phải thu'}</span>
      <span><fmt:formatNumber value="${amountDue}" type="number" groupingUsed="true"/>đ</span>
    </div>
  </div>

  <c:choose>

    <%-- ── Đã thanh toán đủ: hiện trạng thái xác nhận, không còn action gì ── --%>
    <c:when test="${fullyPaid}">
      <div style="text-align:center;padding:12px 0 8px;">
        <div class="result-icon result-icon--success" style="margin:0 auto 16px;">✓</div>
        <div class="result-title result-title--success" style="font-size:20px;">Hóa đơn đã thanh toán đủ</div>
        <p class="result-subtitle" style="margin-bottom:8px;">Cảm ơn quý khách đã sử dụng dịch vụ tại PetClinic.</p>
      </div>
      <div class="confirm-actions">
        <a href="${ctx}/receptionist/${from}" class="btn-confirm" style="text-align:center;">
          Xong, quay lại ${from == 'history' ? 'Lịch sử' : 'Check-in'}
        </a>
      </div>
    </c:when>

    <%-- ── Còn phải thu: 2 lựa chọn thu tiền ── --%>
    <c:otherwise>

      <div class="pay-options">

        <form action="${ctx}/receptionist/invoice" method="post" style="margin:0;">
          <input type="hidden" name="invoiceId" value="${invoice.invoiceID}">
          <input type="hidden" name="from" value="${from}">
          <input type="hidden" name="action" value="cash">
          <button type="submit" class="pay-option">
            <div class="pay-opt-icon">💵</div>
            <div class="pay-opt-body">
              <div class="pay-opt-title">Nhận tiền mặt</div>
              <div class="pay-opt-desc">Khách thanh toán trực tiếp bằng tiền mặt tại quầy.</div>
              <div class="pay-opt-amount">
                <fmt:formatNumber value="${amountDue}" type="number" groupingUsed="true"/>đ
              </div>
            </div>
          </button>
        </form>

        <form action="${ctx}/receptionist/invoice" method="post" style="margin:0;">
          <input type="hidden" name="invoiceId" value="${invoice.invoiceID}">
          <input type="hidden" name="from" value="${from}">
          <input type="hidden" name="action" value="bank">
          <button type="submit" class="pay-option">
            <div class="pay-opt-icon">🏦</div>
            <div class="pay-opt-body">
              <div class="pay-opt-title">Chuyển khoản (QR PayOS)</div>
              <div class="pay-opt-desc">Khách quét mã QR VietQR để chuyển khoản qua PayOS.</div>
              <div class="pay-opt-amount">
                <fmt:formatNumber value="${amountDue}" type="number" groupingUsed="true"/>đ
              </div>
            </div>
          </button>
        </form>

      </div><!-- /pay-options -->

      <div class="pay-qr-note">
        <div class="pay-qr-note-icon">ℹ</div>
        <div>
          <strong>Chuyển khoản QR PayOS</strong><br>
          Nếu chọn chuyển khoản, màn hình sẽ chuyển sang trang mã QR của PayOS để khách quét
          bằng app ngân hàng. Sau khi chuyển khoản thành công, hệ thống sẽ tự động quay lại
          trang này và cập nhật trạng thái hóa đơn.
        </div>
      </div>

    </c:otherwise>
  </c:choose>

</div>

</body>
</html>
