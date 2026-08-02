<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="isReceipt" value="${fn:startsWith(receipt.documentLabel, 'BIÊN LAI')}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <title>${receipt.documentLabel} ${receipt.invoiceCode} - PetClinic</title>
  <style>
    @media print {
      .no-print { display:none !important; }
      body { margin:0; background:#fff; }
      .receipt-box { box-shadow:none !important; border:none !important; }
    }
    body { font-family:'DM Sans',Arial,sans-serif; background:#f4f4f2; margin:0; padding:24px; }
    .r-actions { max-width:460px; margin:0 auto 16px; display:flex; gap:10px; }
    .r-btn { flex:1; padding:11px; border-radius:8px; border:none; font-size:13.5px; font-weight:600;
      cursor:pointer; text-align:center; text-decoration:none; font-family:inherit; }
    .r-btn-print { background:#1a5c38; color:#fff; }
    .r-btn-pdf   { background:#0f3d24; color:#fff; }
    .r-btn-back  { background:#fff; border:1.5px solid #ccc; color:#444; }
    .receipt-box { max-width:460px; margin:0 auto; background:#fff; border:1px solid #ddd;
      border-radius:10px; padding:28px 24px; }
    .r-biz-name { text-align:center; font-size:17px; font-weight:800; color:#0f3d24; }
    .r-biz-sub  { text-align:center; font-size:11px; color:#777; margin-bottom:10px; }
    .r-title { text-align:center; font-size:19px; font-weight:800; letter-spacing:1px; margin-bottom:2px; }
    .r-subtitle { text-align:center; font-size:12px; font-weight:700; color:#555; margin-bottom:16px; }
    .r-section-title { font-size:12px; font-weight:700; color:#0f3d24; margin:16px 0 6px;
      border-bottom:1px solid #eee; padding-bottom:4px; }
    .r-meta  { font-size:13px; margin-bottom:5px; display:flex; justify-content:space-between; gap:10px; }
    .r-meta span:first-child { color:#666; flex-shrink:0; }
    table.r-items { width:100%; border-collapse:collapse; margin:10px 0; font-size:12.5px; }
    table.r-items th { border-bottom:2px solid #333; padding:6px 2px; text-align:left;
      font-size:10.5px; text-transform:uppercase; letter-spacing:.3px; }
    table.r-items td { padding:5px 2px; border-bottom:1px dashed #ddd; }
    .num { text-align:right; white-space:nowrap; }
    table.r-totals { width:100%; font-size:13.5px; margin-top:6px; }
    table.r-totals td { padding:4px 0; }
    table.r-totals .val { text-align:right; font-weight:700; }
    .r-grand td { font-size:16px; color:#0f3d24; border-top:1.5px solid #333; padding-top:9px; }
    .r-words { margin-top:10px; font-size:12.5px; font-style:italic; }
    .r-note   { margin-top:22px; font-size:11.5px; color:#666; text-align:center; line-height:1.6; }
    .r-thanks { margin-top:8px; text-align:center; font-weight:800; font-size:14px;
      color:#0f3d24; letter-spacing:.5px; }
    .r-sign-table { width:100%; margin-top:30px; }
    .r-sign-col { width:50%; text-align:center; vertical-align:top; font-size:12.5px; }
    .r-sign-title { font-weight:700; }
    .r-sign-hint  { font-size:10.5px; color:#777; margin-top:2px; }
    .r-sign-space { height:56px; }
    .r-sign-name  { font-weight:700; }
  </style>
</head>
<body>

<div class="r-actions no-print">
  <a href="${ctx}/receptionist/invoice?invoiceId=${receipt.invoiceIdRaw}&from=${from}" class="r-btn r-btn-back">← Quay lại</a>
  <button type="button" class="r-btn r-btn-print" onclick="window.print()">In / Xuất PDF</button>
  <a href="${ctx}/receptionist/invoice/receipt?invoiceId=${receipt.invoiceIdRaw}&format=pdf" class="r-btn r-btn-pdf">Tải file PDF</a>
</div>

<div class="receipt-box">
  <div class="r-biz-name">PetClinic</div>
  <div class="r-biz-sub">123 Đường ABC, TP. Hà Nội · (028) 123 456 789 · petclinicweb123@gmail.com</div>
  <div class="r-title">${receipt.documentLabel}</div>
  <c:if test="${isReceipt}">
    <div class="r-subtitle">
      <c:choose>
        <c:when test="${receipt.documentLabel == 'BIÊN LAI LẦN 1'}">THU TIỀN ĐẶT CỌC / TRẢ TRƯỚC</c:when>
        <c:otherwise>QUYẾT TOÁN HOÀN THÀNH DỊCH VỤ</c:otherwise>
      </c:choose>
    </div>
  </c:if>

  <c:choose>
    <c:when test="${isReceipt}">
      <div class="r-meta"><span>Liên kết hóa đơn tổng</span><strong>${receipt.invoiceCode}</strong></div>
      <div class="r-meta"><span>Mã biên lai</span>
        <strong>BL-${receipt.invoiceIdRaw}${receipt.documentLabel == 'BIÊN LAI LẦN 2' ? '-02' : '-01'}</strong></div>
      <div class="r-meta"><span>Thời gian</span><strong>${receipt.issuedAtDisplay}</strong></div>
      <div class="r-meta"><span>Người nộp tiền</span>
        <strong><c:out value="${receipt.customerName}"/><c:if test="${not empty receipt.customerPhone}"> (${receipt.customerPhone})</c:if></strong></div>
      <div class="r-meta"><span>Nội dung thu</span><strong style="text-align:right;max-width:260px;"><c:out value="${receipt.purposeText}"/></strong></div>
      <div class="r-meta"><span>Hình thức thanh toán</span><strong><c:out value="${receipt.paymentMethodDisplay}"/></strong></div>

      <table class="r-totals">
        <tr class="r-grand"><td>Số tiền thực thu</td><td class="val"><fmt:formatNumber value="${receipt.paidAmount}" type="number" groupingUsed="true"/>đ</td></tr>
      </table>
      <div class="r-words">Bằng chữ: <em><c:out value="${receipt.amountInWords}"/></em></div>

      <c:if test="${not empty receipt.remainingAmount and receipt.remainingAmount > 0}">
        <div class="r-note">Còn lại phải thanh toán vào ngày ${receipt.remainingDueDate}:
          <strong><fmt:formatNumber value="${receipt.remainingAmount}" type="number" groupingUsed="true"/>đ</strong></div>
      </c:if>

      <table class="r-sign-table">
        <tr>
          <td class="r-sign-col">
            <div class="r-sign-title">Người nộp tiền</div>
            <div class="r-sign-hint">(Ký, ghi rõ họ tên nếu cần)</div>
            <div class="r-sign-space"></div>
            <div class="r-sign-name"><c:out value="${receipt.customerName}"/></div>
          </td>
          <td class="r-sign-col">
            <div class="r-sign-title">Người thu tiền / Thủ quỹ</div>
            <div class="r-sign-hint">(Hệ thống xác thực điện tử)</div>
            <div class="r-sign-space"></div>
            <div class="r-sign-name"><c:out value="${receipt.staffName}"/></div>
          </td>
        </tr>
      </table>
    </c:when>

    <c:otherwise>
      <div class="r-meta"><span>Mã hóa đơn</span><strong>${receipt.invoiceCode}</strong></div>
      <div class="r-meta"><span>Ngày/giờ</span><strong>${receipt.issuedAtDisplay}</strong></div>
      <div class="r-meta"><span>Nhân viên</span><strong><c:out value="${receipt.staffName}"/></strong></div>

      <div class="r-section-title">THÔNG TIN KHÁCH HÀNG</div>
      <div class="r-meta"><span>Khách hàng</span><strong><c:out value="${receipt.customerName}"/></strong></div>
      <div class="r-meta"><span>Điện thoại</span><strong><c:out value="${empty receipt.customerPhone ? '-' : receipt.customerPhone}"/></strong></div>

      <c:if test="${not empty receipt.petName}">
        <div class="r-section-title">THÔNG TIN THÚ CƯNG</div>
        <div class="r-meta"><span>Tên Pet</span><strong><c:out value="${receipt.petName}"/></strong></div>
      </c:if>

      <div class="r-section-title">CHI TIẾT DỊCH VỤ &amp; SẢN PHẨM</div>
      <table class="r-items">
        <thead><tr><th>#</th><th>Sản phẩm / Dịch vụ</th><th class="num">SL</th><th class="num">Đ.Giá</th><th class="num">T.Tiền</th></tr></thead>
        <tbody>
        <c:forEach var="li" items="${receipt.items}" varStatus="vs">
          <tr>
            <td>${vs.count}</td>
            <td><c:out value="${li.name}"/></td>
            <td class="num"><fmt:formatNumber value="${li.quantity}" maxFractionDigits="2"/></td>
            <td class="num"><fmt:formatNumber value="${li.unitPrice}" type="number" groupingUsed="true"/></td>
            <td class="num"><fmt:formatNumber value="${li.lineTotal}" type="number" groupingUsed="true"/></td>
          </tr>
        </c:forEach>
        </tbody>
      </table>

      <table class="r-totals">
        <tr><td>Tạm tính</td><td class="val"><fmt:formatNumber value="${receipt.subTotal}" type="number" groupingUsed="true"/>đ</td></tr>
        <tr><td>Chiết khấu</td><td class="val"><fmt:formatNumber value="${receipt.discountAmount}" type="number" groupingUsed="true"/>đ</td></tr>
        <tr class="r-grand"><td>Tổng giá trị HĐ</td><td class="val"><fmt:formatNumber value="${receipt.totalPayable}" type="number" groupingUsed="true"/>đ</td></tr>
        <c:if test="${not empty receipt.prepaidAmount}">
          <tr><td>Đã trả trước</td><td class="val">- <fmt:formatNumber value="${receipt.prepaidAmount}" type="number" groupingUsed="true"/>đ</td></tr>
          <tr class="r-grand"><td>Còn lại phải thanh toán</td><td class="val"><fmt:formatNumber value="${receipt.amountDue}" type="number" groupingUsed="true"/>đ</td></tr>
        </c:if>
      </table>

      <c:if test="${not empty receipt.note}">
        <div class="r-note">* Ghi chú: ${receipt.note}</div>
      </c:if>
    </c:otherwise>
  </c:choose>

  <div class="r-note">Quý khách được phép khiếu nại hoàn tiền trong vòng 48h kể từ ngày thanh toán.</div>
  <div class="r-thanks">CẢM ƠN QUÝ KHÁCH VÀ HẸN GẶP LẠI!</div>
</div>

</body>
</html>