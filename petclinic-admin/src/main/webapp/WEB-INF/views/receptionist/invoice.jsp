<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Hóa đơn #${invoice.invoiceID} - PetClinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
  <style>
    .pay-flow-steps { display:flex; align-items:center; gap:0; margin-bottom:24px; flex-wrap:wrap; }
    .pay-flow-step { display:flex; align-items:center; gap:8px; font-size:13px; color:var(--text-soft); }
    .pay-flow-step .pfs-num {
      width:26px; height:26px; border-radius:50%; background:var(--border); color:#fff;
      display:flex; align-items:center; justify-content:center; font-weight:700; font-size:12.5px; flex-shrink:0;
    }
    .pay-flow-step.done .pfs-num { background:var(--green-400); }
    .pay-flow-step.active .pfs-num { background:var(--teal-700); }
    .pay-flow-step.active { color:var(--teal-800); font-weight:600; }
    .pay-flow-line { width:40px; height:2px; background:var(--border); margin:0 8px; }
    .pay-flow-line.done { background:var(--green-400); }

    .inv-summary-table { width:100%; border-collapse:collapse; font-size:13.5px; }
    .inv-summary-table th {
      border-bottom:2px solid var(--border); padding:8px 6px; text-align:left;
      font-size:11px; text-transform:uppercase; letter-spacing:.4px; color:var(--text-soft);
    }
    .inv-summary-table td { padding:7px 6px; border-bottom:1px dashed var(--border); }
    .inv-num { text-align:right; white-space:nowrap; }
    .inv-item-name { font-weight:600; color:var(--text); }
    .inv-totals-table { width:100%; font-size:14px; margin-top:8px; }
    .inv-totals-table td { padding:5px 0; }
    .inv-totals-table .val { text-align:right; font-weight:700; }
    .inv-grand td { font-size:17px; color:var(--teal-800); border-top:2px solid var(--border); padding-top:10px; }
    .inv-due-row td { color:#b91c1c; }

    .pay-methods { display:flex; flex-direction:column; gap:14px; margin-top:8px; }
    .pay-method-card { border:1.5px solid var(--border); border-radius:var(--radius); padding:18px 20px; background:#fff; transition:var(--transition); }
    .pay-method-card:hover { border-color:var(--teal-400); }
    .pay-method-head { display:flex; align-items:center; gap:12px; margin-bottom:6px; }
    .pay-method-icon {
      width:40px; height:40px; border-radius:10px; background:var(--teal-50); color:var(--teal-700);
      display:flex; align-items:center; justify-content:center; font-size:19px; flex-shrink:0;
    }
    .pay-method-title { font-size:15px; font-weight:700; color:var(--text); }
    .pay-method-amount { font-size:19px; font-weight:700; color:var(--teal-700); margin-left:auto; white-space:nowrap; }
    .pay-method-desc { font-size:13px; color:var(--text-soft); margin:0 0 12px 52px; line-height:1.6; }
    .pay-method-redirect-hint {
      display:flex; align-items:flex-start; gap:6px; margin:0 0 12px 52px;
      font-size:12px; color:var(--teal-700); background:var(--teal-50); border-radius:6px; padding:6px 10px;
    }
    .pay-method-form { margin-left:52px; display:flex; gap:10px; align-items:center; flex-wrap:wrap; }
    .pay-method-form input[type=number] {
      flex:1; min-width:180px; padding:9px 12px; border:1.5px solid var(--border); border-radius:8px; font-size:13.5px;
    }
    .pay-mixed-preview { margin:8px 0 0 52px; font-size:12.5px; color:var(--teal-700); font-weight:600; }

    .fully-paid-banner { text-align:center; padding:28px 20px; }
    .fully-paid-icon {
      width:60px; height:60px; border-radius:50%; background:var(--green-100); color:#15803d;
      display:flex; align-items:center; justify-content:center; font-size:28px; margin:0 auto 14px;
    }
    .fully-paid-title { font-size:18px; font-weight:700; color:var(--teal-800); }
  </style>
</head>
<body>
<div class="layout">
  <aside class="sidebar">
    <div class="sidebar-logo">🐾 PetClinic</div>
    <nav>
      <a href="${pageContext.request.contextPath}/receptionist/checkin" class="nav-item ${from == 'checkin' ? 'active' : ''}">Check-in</a>
      <a href="${pageContext.request.contextPath}/receptionist/history" class="nav-item ${from == 'history' ? 'active' : ''}">Lịch sử</a>
    </nav>
    <div class="sidebar-user">
      ${sessionScope.staff.fullName}
      <a href="${pageContext.request.contextPath}/auth/staff/logout" class="logout-link">Đăng xuất</a>
    </div>
  </aside>

  <main class="main-content">
    <div class="page-header">
      <h1>Hóa đơn #${invoice.invoiceID}</h1>
      <p class="page-sub">
        Lịch hẹn #${appointment.appointmentID}
        <c:if test="${not empty appointment.customerName}"> — <c:out value="${appointment.customerName}"/></c:if>
        <c:if test="${not empty appointment.petName}"> — 🐾 <c:out value="${appointment.petName}"/></c:if>
      </p>
      <div class="page-actions">
        <a href="${pageContext.request.contextPath}/receptionist/${from}" class="btn btn-outline">
          ← Quay lại ${from == 'history' ? 'Lịch sử' : 'Check-in'}
        </a>
      </div>
    </div>

    <%@ include file="/WEB-INF/views/common/_flash.jsp" %>

    <div class="pay-flow-steps">
      <div class="pay-flow-step done"><span class="pfs-num">✓</span> Tạo hóa đơn</div>
      <div class="pay-flow-line ${fullyPaid ? 'done' : ''}"></div>
      <div class="pay-flow-step ${fullyPaid ? 'done' : 'active'}"><span class="pfs-num">${fullyPaid ? '✓' : '2'}</span> Chọn phương thức thanh toán</div>
      <div class="pay-flow-line ${fullyPaid ? 'done' : ''}"></div>
      <div class="pay-flow-step ${fullyPaid ? 'active' : ''}"><span class="pfs-num">3</span> Biên lai</div>
    </div>

    <div class="card">
      <div class="card-header"><span class="card-title">Thông tin lịch hẹn</span></div>
      <div class="card-body">
        <div class="detail-grid">
          <div><span class="detail-label">Ngày khám</span>
            <span class="detail-value">${appointment.appointmentDate} · ${appointment.startTime}</span></div>
          <div><span class="detail-label">Trạng thái</span>
            <span class="detail-value"><span class="badge badge-neutral">${appointment.status}</span></span></div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <span class="card-title">${invoicePreview.documentLabel}</span>
        <span class="text-soft">Mã đơn: ${invoicePreview.invoiceCode}</span>
      </div>
      <div class="card-body">
        <table class="inv-summary-table">
          <thead>
          <tr><th>Sản phẩm / Dịch vụ</th><th class="inv-num">SL</th><th class="inv-num">Đ.Giá</th><th class="inv-num">CK</th><th class="inv-num">T.Tiền</th></tr>
          </thead>
          <tbody>
          <c:forEach var="li" items="${invoicePreview.items}">
            <tr>
              <td class="inv-item-name"><c:out value="${li.name}"/></td>
              <td class="inv-num"><fmt:formatNumber value="${li.quantity}" maxFractionDigits="2"/></td>
              <td class="inv-num"><fmt:formatNumber value="${li.unitPrice}" type="number" groupingUsed="true"/></td>
              <td class="inv-num"><fmt:formatNumber value="${li.discount}" type="number" groupingUsed="true"/></td>
              <td class="inv-num"><fmt:formatNumber value="${li.lineTotal}" type="number" groupingUsed="true"/></td>
            </tr>
          </c:forEach>
          </tbody>
        </table>

        <table class="inv-totals-table">
          <tr><td>Tổng số lượng</td><td class="val"><fmt:formatNumber value="${invoicePreview.totalQuantity}" maxFractionDigits="2"/></td></tr>
          <tr><td>Tổng tiền hàng</td><td class="val"><fmt:formatNumber value="${invoicePreview.subTotal}" type="number" groupingUsed="true"/>đ</td></tr>
          <tr><td>Chiết khấu</td><td class="val"><fmt:formatNumber value="${invoicePreview.discountAmount}" type="number" groupingUsed="true"/>đ</td></tr>
          <tr class="inv-grand"><td>Tổng phải trả</td><td class="val"><fmt:formatNumber value="${invoicePreview.totalPayable}" type="number" groupingUsed="true"/>đ</td></tr>
          <c:if test="${amountPaid > 0}">
            <tr><td>Đã trả trước</td><td class="val"><fmt:formatNumber value="${amountPaid}" type="number" groupingUsed="true"/>đ</td></tr>
            <tr class="inv-grand inv-due-row">
              <td>${fullyPaid ? 'Đã thanh toán đủ' : 'Khách còn phải trả'}</td>
              <td class="val"><fmt:formatNumber value="${amountDue}" type="number" groupingUsed="true"/>đ</td>
            </tr>
          </c:if>
        </table>
      </div>
    </div>

    <c:choose>
      <c:when test="${fullyPaid}">
        <div class="card">
          <div class="card-body fully-paid-banner">
            <div class="fully-paid-icon">✓</div>
            <div class="fully-paid-title">Hóa đơn đã thanh toán đủ</div>
            <p class="page-sub" style="margin-top:6px;">Bạn có thể xem hoặc in lại biên lai bên dưới.</p>
            <div style="margin-top:18px;">
              <a href="${pageContext.request.contextPath}/receptionist/invoice/receipt?invoiceId=${invoice.invoiceID}&from=${from}"
                 class="btn btn-primary">Xem / In biên lai →</a>
            </div>
          </div>
        </div>
      </c:when>

      <c:otherwise>
        <div class="card">
          <div class="card-header"><span class="card-title">Chọn phương thức thanh toán</span></div>
          <div class="card-body">
            <div class="pay-methods">

              <div class="pay-method-card">
                <div class="pay-method-head">
                  <div class="pay-method-title">Tiền mặt</div>
                  <div class="pay-method-amount"><fmt:formatNumber value="${amountDue}" type="number" groupingUsed="true"/>đ</div>
                </div>
                <p class="pay-method-desc">Thu đủ toàn bộ số tiền còn lại bằng tiền mặt, xác nhận ngay tại quầy.</p>
                <form action="${pageContext.request.contextPath}/receptionist/invoice" method="post" class="pay-method-form">
                  <input type="hidden" name="invoiceId" value="${invoice.invoiceID}">
                  <input type="hidden" name="from" value="${from}">
                  <input type="hidden" name="action" value="cash">
                  <button type="submit" class="btn btn-primary">Xác nhận đã thu tiền mặt</button>
                </form>
              </div>

              <div class="pay-method-card">
                <div class="pay-method-head">
                  <div class="pay-method-title">Chuyển khoản (QR PayOS)</div>
                  <div class="pay-method-amount"><fmt:formatNumber value="${amountDue}" type="number" groupingUsed="true"/>đ</div>
                </div>
                <p class="pay-method-desc">Toàn bộ số tiền còn lại sẽ được thu qua mã QR chuyển khoản.</p>
                <form action="${pageContext.request.contextPath}/receptionist/invoice" method="post" class="pay-method-form">
                  <input type="hidden" name="invoiceId" value="${invoice.invoiceID}">
                  <input type="hidden" name="from" value="${from}">
                  <input type="hidden" name="action" value="bank">
                  <button type="submit" class="btn btn-secondary">Tạo mã QR chuyển khoản →</button>
                </form>
              </div>

              <div class="pay-method-card">
                <div class="pay-method-head">
                  <div class="pay-method-title">Tiền mặt + Chuyển khoản</div>
                </div>
                <p class="pay-method-desc">Khách trả một phần bằng tiền mặt, phần còn lại quét QR chuyển khoản.</p>
                <form action="${pageContext.request.contextPath}/receptionist/invoice" method="post" class="pay-method-form">
                  <input type="hidden" name="invoiceId" value="${invoice.invoiceID}">
                  <input type="hidden" name="from" value="${from}">
                  <input type="hidden" name="action" value="mixed">
                  <input type="number" id="cashPart" name="cashPart" min="0" step="1000"
                         placeholder="Số tiền mặt khách trả" oninput="updateMixedRemaining()">
                  <button type="submit" class="btn btn-secondary">Tạo QR cho phần còn lại</button>
                </form>
                <div id="mixedRemainingPreview" class="pay-mixed-preview"></div>
              </div>

            </div>
          </div>
        </div>
      </c:otherwise>
    </c:choose>
  </main>
</div>

<script>
  const AMOUNT_DUE = ${amountDue};
  function updateMixedRemaining() {
    const cash = parseFloat(document.getElementById('cashPart').value || '0');
    const el = document.getElementById('mixedRemainingPreview');
    el.textContent = (cash > 0 && cash < AMOUNT_DUE)
            ? 'Số tiền cần chuyển khoản: ' + (AMOUNT_DUE - cash).toLocaleString('vi-VN') + 'đ'
            : '';
  }
</script>
<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>