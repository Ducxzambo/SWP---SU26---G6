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
    .staff-topbar {
      display: flex; align-items: center; justify-content: space-between;
      padding: 16px 28px; background: var(--green-900); color: #fff;
    }
    .staff-topbar .brand { font-family: 'Playfair Display', serif; font-size: 18px; }
    .staff-topbar .who   { font-size: 13.5px; color: var(--green-100); display: flex; align-items: center; gap: 14px; }
    .staff-topbar a      { color: #fff; opacity: .85; }
    .staff-topbar a:hover{ opacity: 1; text-decoration: underline; }

    .flash-warning { background: #fff3cd; color: #856404; border-bottom: 1px solid #ffe28a; }

    button.pay-option {
      width: 100%; text-align: left; font-family: inherit; color: inherit;
      appearance: none; -webkit-appearance: none;
    }
    button.pay-option:focus-visible { outline: 2px solid var(--green-400); outline-offset: 2px; }

    /* ── Bảng hóa đơn, format giống receipt.jsp (biên lai) ─────────────── */
    .r-title { text-align:left; font-size:18px; font-weight:800; letter-spacing:.5px; margin-bottom:0; }
    .r-sub   { text-align:left; font-size:12px; color:#777; margin-bottom:14px; }
    table.r-items { width:100%; border-collapse:collapse; margin:12px 0; font-size:12.5px; }
    table.r-items th { border-bottom:2px solid #333; padding:6px 2px; text-align:left; font-size:10.5px; text-transform:uppercase; letter-spacing:.3px; }
    table.r-items td { padding:5px 2px; border-bottom:1px dashed #ddd; }
    .num { text-align:right; white-space:nowrap; }
    .r-item-name { font-weight:600; padding-top:10px !important; border-bottom:none !important; }
    table.r-totals { width:100%; font-size:13.5px; margin-top:6px; }
    table.r-totals td { padding:4px 0; }
    table.r-totals .val { text-align:right; font-weight:700; }
    .r-grand td { font-size:16px; color:#0f3d24; border-top:1.5px solid #333; padding-top:9px; }
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

  <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:6px;">
    <h1 style="font-family:'Playfair Display',serif;font-size:26px;color:var(--green-900);">
      Hóa đơn #${invoice.invoiceID}
    </h1>
    <a href="${ctx}/receptionist/invoice?invoiceId=${invoice.invoiceID}&from=${from}&format=pdf"
       target="_blank" class="btn-back" style="width:auto;padding:9px 18px;">
      Xuất PDF
    </a>
  </div>
  <p style="color:var(--warm-gray);font-size:14px;margin-bottom:20px;">
    Lịch hẹn #${appointment.appointmentID}
    <c:if test="${not empty appointment.customerName}"> — ${appointment.customerName}</c:if>
    <c:if test="${not empty appointment.petName}"> — 🐾 ${appointment.petName}</c:if>
  </p>

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

  <!-- Chi tiết hóa đơn — cùng format với biên lai (receipt.jsp) -->
  <div class="confirm-box" style="margin-bottom:20px;padding:24px;">
    <div class="r-title">${invoicePreview.documentLabel}</div>
    <div class="r-sub">Mã đơn hàng: ${invoicePreview.invoiceCode}</div>

    <table class="r-items">
      <thead>
      <tr><th>Sản phẩm / Dịch vụ</th><th class="num">SL</th><th class="num">Đ.Giá</th><th class="num">CK</th><th class="num">T.Tiền</th></tr>
      </thead>
      <tbody>
      <c:forEach var="li" items="${invoicePreview.items}">
        <tr><td colspan="5" class="r-item-name"><c:out value="${li.name}"/></td></tr>
        <tr>
          <td></td>
          <td class="num"><fmt:formatNumber value="${li.quantity}" maxFractionDigits="2"/></td>
          <td class="num"><fmt:formatNumber value="${li.unitPrice}" type="number" groupingUsed="true"/></td>
          <td class="num"><fmt:formatNumber value="${li.discount}" type="number" groupingUsed="true"/></td>
          <td class="num"><fmt:formatNumber value="${li.lineTotal}" type="number" groupingUsed="true"/></td>
        </tr>
      </c:forEach>
      </tbody>
    </table>

    <table class="r-totals">
      <tr><td>Tổng số lượng</td><td class="val"><fmt:formatNumber value="${invoicePreview.totalQuantity}" maxFractionDigits="2"/></td></tr>
      <tr><td>Tổng tiền hàng</td><td class="val"><fmt:formatNumber value="${invoicePreview.subTotal}" type="number" groupingUsed="true"/>đ</td></tr>
      <tr><td>Chiết khấu</td><td class="val"><fmt:formatNumber value="${invoicePreview.discountAmount}" type="number" groupingUsed="true"/>đ</td></tr>
      <tr class="r-grand"><td>Tổng phải trả</td><td class="val"><fmt:formatNumber value="${invoicePreview.totalPayable}" type="number" groupingUsed="true"/>đ</td></tr>

      <c:if test="${amountPaid > 0}">
        <tr><td>Đã trả trước</td><td class="val"><fmt:formatNumber value="${amountPaid}" type="number" groupingUsed="true"/>đ</td></tr>
        <tr class="r-grand">
          <td>${fullyPaid ? 'Đã thanh toán đủ' : 'Khách còn phải trả'}</td>
          <td class="val"><fmt:formatNumber value="${amountDue}" type="number" groupingUsed="true"/>đ</td>
        </tr>
      </c:if>
    </table>
  </div>

  <c:choose>
    <c:when test="${fullyPaid}">
      <div style="text-align:center;padding:12px 0 8px;">
        <div class="result-icon result-icon--success" style="margin:0 auto 16px;">✓</div>
        <div class="result-title result-title--success" style="font-size:20px;">Hóa đơn đã thanh toán đủ</div>
      </div>
      <div class="confirm-actions">
        <a href="${ctx}/receptionist/invoice/receipt?invoiceId=${invoice.invoiceID}&from=${from}" class="btn-confirm" style="text-align:center;">
          Xem / In hóa đơn
        </a>
      </div>
    </c:when>

    <c:otherwise>
      <div class="pay-options">

        <!-- 1) Tiền mặt -->
        <div class="pay-option" style="cursor:default;">
          <div class="pay-opt-icon">💵</div>
          <div class="pay-opt-body">
            <div class="pay-opt-title">Tiền mặt</div>
            <div class="pay-opt-amount"><fmt:formatNumber value="${amountDue}" type="number" groupingUsed="true"/>đ</div>
            <form action="${ctx}/receptionist/invoice" method="post" style="margin-top:10px;">
              <input type="hidden" name="invoiceId" value="${invoice.invoiceID}">
              <input type="hidden" name="from" value="${from}">
              <input type="hidden" name="action" value="cash">
              <button type="submit" class="btn-confirm" style="padding:9px 18px;">Xác nhận thu tiền</button>
            </form>
          </div>
        </div>

        <!-- 2) Chuyển khoản -->
        <form action="${ctx}/receptionist/invoice" method="post" style="margin:0;">
          <input type="hidden" name="invoiceId" value="${invoice.invoiceID}">
          <input type="hidden" name="from" value="${from}">
          <input type="hidden" name="action" value="bank">
          <button type="submit" class="pay-option" style="width:100%;text-align:left;">
            <div class="pay-opt-icon">🏦</div>
            <div class="pay-opt-body">
              <div class="pay-opt-title">Chuyển khoản (QR PayOS)</div>
              <div class="pay-opt-amount"><fmt:formatNumber value="${amountDue}" type="number" groupingUsed="true"/>đ</div>
            </div>
          </button>
        </form>

        <!-- 3) Tiền mặt + Chuyển khoản -->
        <div class="pay-option" style="cursor:default;">
          <div class="pay-opt-icon">💵🏦</div>
          <div class="pay-opt-body" style="width:100%;">
            <div class="pay-opt-title">Tiền mặt + Chuyển khoản</div>
            <div class="pay-opt-desc">Khách trả một phần bằng tiền mặt, phần còn lại quét QR chuyển khoản.</div>
            <form action="${ctx}/receptionist/invoice" method="post"
                  style="margin-top:10px;display:flex;gap:8px;align-items:center;">
              <input type="hidden" name="invoiceId" value="${invoice.invoiceID}">
              <input type="hidden" name="from" value="${from}">
              <input type="hidden" name="action" value="mixed">
              <input type="number" id="cashPart" name="cashPart" min="0" step="1000"
                     placeholder="Số tiền mặt khách trả"
                     style="flex:1;padding:9px 12px;border:1.5px solid var(--border);border-radius:8px;font-size:13.5px;"
                     oninput="updateMixedRemaining()">
              <button type="submit" class="btn-confirm" style="padding:9px 18px;">Tạo QR cho phần còn lại</button>
            </form>
            <div id="mixedRemainingPreview" style="margin-top:8px;font-size:13px;color:var(--green-700);"></div>
          </div>
        </div>

      </div>

      <script>
        const AMOUNT_DUE = ${amountDue};
        function updateMixedRemaining() {
          const cash = parseFloat(document.getElementById('cashPart').value || '0');
          const el = document.getElementById('mixedRemainingPreview');
          if (cash > 0 && cash < AMOUNT_DUE) {
            el.textContent = 'Số tiền cần chuyển khoản: ' + (AMOUNT_DUE - cash).toLocaleString('vi-VN') + 'đ';
          } else {
            el.textContent = '';
          }
        }
      </script>
    </c:otherwise>
  </c:choose>

  <div class="pay-qr-note">
    <div class="pay-qr-note-icon">ℹ</div>
    <div>
      <strong>Chuyển khoản QR PayOS</strong><br>
      Nếu chọn "Chuyển khoản" hoặc phần chuyển khoản trong "Tiền mặt + Chuyển khoản", màn hình sẽ
      chuyển sang trang mã QR của PayOS để khách quét bằng app ngân hàng. Sau khi chuyển khoản
      thành công, hệ thống sẽ tự động quay lại trang này và cập nhật trạng thái hóa đơn.
    </div>
  </div>

</div>

</body>
</html>