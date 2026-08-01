<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<title>Hóa đơn ${receipt.invoiceCode} - PetClinic</title>
<style>
  @media print {
    .no-print { display:none !important; }
    body { margin:0; background:#fff; }
    .receipt-box { box-shadow:none !important; border:none !important; }
  }
  body { font-family:'DM Sans',Arial,sans-serif; background:#f4f4f2; margin:0; padding:24px; }
  .r-actions { max-width:420px; margin:0 auto 16px; display:flex; gap:10px; }
  .r-btn { flex:1; padding:11px; border-radius:8px; border:none; font-size:13.5px; font-weight:600;
           cursor:pointer; text-align:center; text-decoration:none; font-family:inherit; }
  .r-btn-print { background:#1a5c38; color:#fff; }
  .r-btn-pdf   { background:#0f3d24; color:#fff; }
  .r-btn-back  { background:#fff; border:1.5px solid #ccc; color:#444; }
  .receipt-box { max-width:420px; margin:0 auto; background:#fff; border:1px solid #ddd;
                 border-radius:10px; padding:28px 24px; }
  .r-title { text-align:center; font-size:20px; font-weight:800; letter-spacing:1px; margin-bottom:2px; }
  .r-sub   { text-align:center; font-size:12px; color:#777; margin-bottom:18px; }
  .r-meta  { font-size:13px; margin-bottom:5px; display:flex; justify-content:space-between; gap:10px; }
  .r-meta span:first-child { color:#666; flex-shrink:0; }
  table.r-items { width:100%; border-collapse:collapse; margin:16px 0; font-size:12.5px; }
  table.r-items th { border-bottom:2px solid #333; padding:6px 2px; text-align:left;
                      font-size:10.5px; text-transform:uppercase; letter-spacing:.3px; }
  table.r-items td { padding:5px 2px; border-bottom:1px dashed #ddd; }
  .num { text-align:right; white-space:nowrap; }
  .r-item-name { font-weight:600; padding-top:10px !important; border-bottom:none !important; }
  table.r-totals { width:100%; font-size:13.5px; margin-top:6px; }
  table.r-totals td { padding:4px 0; }
  table.r-totals .val { text-align:right; font-weight:700; }
  .r-grand td { font-size:16px; color:#0f3d24; border-top:1.5px solid #333; padding-top:9px; }
  .r-note   { margin-top:22px; font-size:11.5px; color:#666; text-align:center; line-height:1.6; }
  .r-thanks { margin-top:8px; text-align:center; font-weight:800; font-size:14px;
              color:#0f3d24; letter-spacing:.5px; }
</style>
</head>
<body>

<div class="r-actions no-print">
  <a href="${ctx}/receptionist/invoice?invoiceId=${receipt.invoiceIdRaw}&from=${from}" class="r-btn r-btn-back">← Quay lại</a>
  <button type="button" class="r-btn r-btn-print" onclick="window.print()">In / Xuất PDF</button>
  <a href="${ctx}/receptionist/invoice/receipt?invoiceId=${receipt.invoiceIdRaw}&format=pdf" class="r-btn r-btn-pdf">Tải file PDF</a>
</div>

<div class="receipt-box">
  <div class="r-title">${receipt.documentLabel}</div>
  <div class="r-sub">PetClinic · 123 Đường ABC, TP. Hà Nội · (028) 123 456 789</div>

  <div class="r-meta"><span>Mã đơn hàng</span><strong>${receipt.invoiceCode}</strong></div>
  <div class="r-meta"><span>Ngày/giờ</span><strong>${receipt.issuedAtDisplay}</strong></div>
  <div class="r-meta"><span>Nhân viên</span><strong><c:out value="${receipt.staffName}"/></strong></div>
  <div class="r-meta"><span>Khách hàng</span><strong><c:out value="${receipt.customerName}"/></strong></div>

  <table class="r-items">
    <thead><tr><th>Sản phẩm / Dịch vụ</th><th class="num">SL</th><th class="num">Đ.Giá</th><th class="num">CK</th><th class="num">T.Tiền</th></tr></thead>
    <tbody>
      <c:forEach var="li" items="${receipt.items}">
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
    <tr><td>Tổng số lượng</td><td class="val"><fmt:formatNumber value="${receipt.totalQuantity}" maxFractionDigits="2"/></td></tr>
    <tr><td>Tổng tiền hàng</td><td class="val"><fmt:formatNumber value="${receipt.subTotal}" type="number" groupingUsed="true"/>đ</td></tr>
    <tr><td>Chiết khấu</td><td class="val"><fmt:formatNumber value="${receipt.discountAmount}" type="number" groupingUsed="true"/>đ</td></tr>
    <tr class="r-grand"><td>Tổng phải trả</td><td class="val"><fmt:formatNumber value="${receipt.totalPayable}" type="number" groupingUsed="true"/>đ</td></tr>

    <c:if test="${receipt.documentLabel == 'HÓA ĐƠN'}">
      <tr><td>Đã trả trước</td><td class="val"><fmt:formatNumber value="${receipt.prepaidAmount}" type="number" groupingUsed="true"/>đ</td></tr>
      <tr class="r-grand"><td>Khách còn phải trả</td><td class="val"><fmt:formatNumber value="${receipt.amountDue}" type="number" groupingUsed="true"/>đ</td></tr>
      <tr><td>Tiền trả lại</td><td class="val"><fmt:formatNumber value="${receipt.changeAmount}" type="number" groupingUsed="true"/>đ</td></tr>
    </c:if>

    <c:if test="${not empty receipt.paidStatusLabel}">
      <tr><td>Đã trả (${receipt.paidStatusLabel})</td><td class="val"><fmt:formatNumber value="${receipt.paidAmount}" type="number" groupingUsed="true"/>đ</td></tr>
    </c:if>
    <c:if test="${not empty receipt.remainingAmount}">
      <tr><td>Còn phải thanh toán vào ngày ${receipt.remainingDueDate}</td><td class="val"><fmt:formatNumber value="${receipt.remainingAmount}" type="number" groupingUsed="true"/>đ</td></tr>
    </c:if>
    <c:if test="${receipt.documentLabel == 'BIÊN LAI LẦN 2'}">
      <tr><td>Tiền trả lại</td><td class="val"><fmt:formatNumber value="${receipt.changeAmount}" type="number" groupingUsed="true"/>đ</td></tr>
    </c:if>
  </table>

  <c:if test="${not empty receipt.note}">
    <div class="r-note">Ghi chú: ${receipt.note}</div>
  </c:if>
  <div class="r-note">Quý khách được phép khiếu nại hoàn tiền trong vòng 48h kể từ ngày thanh toán.</div>
  <div class="r-thanks">CẢM ƠN QUÝ KHÁCH VÀ HẸN GẶP LẠI!</div>
</div>

</body>
</html>