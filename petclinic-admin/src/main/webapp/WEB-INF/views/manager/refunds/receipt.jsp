<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>${receipt.documentLabel} ${receipt.invoiceCode} - PetClinic</title>
    <style>
        @media print { .no-print{display:none!important;} body{margin:0;background:#fff;} .receipt-box{box-shadow:none!important;border:none!important;} }
        body { font-family:'DM Sans',Arial,sans-serif; background:#f4f4f2; margin:0; padding:24px; }
        .r-actions { max-width:460px; margin:0 auto 16px; display:flex; gap:10px; }
        .r-btn { flex:1; padding:11px; border-radius:8px; border:none; font-size:13.5px; font-weight:600; cursor:pointer; text-align:center; text-decoration:none; font-family:inherit; }
        .r-btn-print { background:#155c50; color:#fff; }
        .r-btn-pdf   { background:#0a2e2a; color:#fff; }
        .r-btn-back  { background:#fff; border:1.5px solid #ccc; color:#444; }
        .receipt-box { max-width:460px; margin:0 auto; background:#fff; border:1px solid #ddd; border-radius:10px; padding:28px 24px; }
        .r-biz-name { text-align:center; font-size:17px; font-weight:800; color:#0a2e2a; }
        .r-biz-sub  { text-align:center; font-size:11px; color:#777; margin-bottom:10px; }
        .r-title { text-align:center; font-size:19px; font-weight:800; letter-spacing:1px; margin-bottom:2px; }
        .r-subtitle { text-align:center; font-size:12px; font-weight:700; color:#555; margin-bottom:16px; }
        .r-meta  { font-size:13px; margin-bottom:5px; display:flex; justify-content:space-between; gap:10px; }
        .r-meta span:first-child { color:#666; flex-shrink:0; }
        table.r-totals { width:100%; font-size:13.5px; margin-top:6px; }
        table.r-totals td { padding:4px 0; }
        table.r-totals .val { text-align:right; font-weight:700; }
        .r-grand td { font-size:16px; color:#0a2e2a; border-top:1.5px solid #333; padding-top:9px; }
        .r-words { margin-top:10px; font-size:12.5px; font-style:italic; }
        .r-note   { margin-top:22px; font-size:11.5px; color:#666; text-align:center; line-height:1.6; }
        .r-thanks { margin-top:8px; text-align:center; font-weight:800; font-size:14px; color:#0a2e2a; letter-spacing:.5px; }
    </style>
</head>
<body>

<div class="r-actions no-print">
    <a href="${ctx}/manager/refunds/detail?id=${refundId}" class="r-btn r-btn-back">← Quay lại</a>
    <button type="button" class="r-btn r-btn-print" onclick="window.print()">In</button>
    <a href="${ctx}/manager/refunds/receipt?id=${refundId}&format=pdf" class="r-btn r-btn-pdf">Tải PDF</a>
</div>

<div class="receipt-box">
    <div class="r-biz-name">PetClinic</div>
    <div class="r-biz-sub">123 Đường ABC, TP. Hà Nội · (028) 123 456 789 · petclinicweb123@gmail.com</div>
    <div class="r-title">${receipt.documentLabel}</div>
    <div class="r-subtitle">XÁC NHẬN CHUYỂN KHOẢN HOÀN TIỀN CHO KHÁCH HÀNG</div>

    <div class="r-meta"><span>Mã yêu cầu hoàn tiền</span><strong><c:out value="${receipt.invoiceCode}"/></strong></div>
    <div class="r-meta"><span>Mã biên lai</span><strong><c:out value="${receipt.paymentCode}"/></strong></div>
    <div class="r-meta"><span>Thời gian</span><strong>${receipt.issuedAtDisplay}</strong></div>
    <div class="r-meta"><span>Khách hàng</span>
        <strong><c:out value="${receipt.customerName}"/><c:if test="${not empty receipt.customerPhone}"> (${receipt.customerPhone})</c:if></strong></div>
    <c:if test="${not empty receipt.petName}">
        <div class="r-meta"><span>Thú cưng</span><strong><c:out value="${receipt.petName}"/></strong></div>
    </c:if>
    <div class="r-meta"><span>Nội dung hoàn tiền</span><strong style="text-align:right;max-width:260px;"><c:out value="${receipt.purposeText}"/></strong></div>
    <div class="r-meta"><span>Hình thức hoàn tiền</span><strong><c:out value="${receipt.paymentMethodDisplay}"/></strong></div>


    <table class="r-totals">
        <tr class="r-grand"><td>Số tiền đã hoàn</td>
            <td class="val"><fmt:formatNumber value="${receipt.paidAmount}" type="number" groupingUsed="true"/>đ</td></tr>
    </table>
    <div class="r-words">Bằng chữ: <em><c:out value="${receipt.amountInWords}"/></em></div>

    <c:if test="${not empty receipt.note}">
        <div class="r-note">* Ghi chú: <c:out value="${receipt.note}"/></div>
    </c:if>
</div>

</body>
</html>