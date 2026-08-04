<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="activeTab" value="receipts" scope="request"/>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${receipt.receiptCode} - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css?v=receipt-detail-3">
</head>
<body>
<div class="layout">
    <c:set var="activeModule" value="inventory" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>
    <main class="main-content main-content-wide">
        <div class="page-header receipt-page-header">
            <div>
                <a class="receipt-back" href="${pageContext.request.contextPath}/admin/inventory/receipts">← Danh sách phiếu nhập</a>
                <h1>Chi tiết phiếu nhập</h1>
                <p class="page-sub">Đối chiếu hàng nhập và tải hóa đơn lưu trữ</p>
            </div>
            <a class="btn btn-primary receipt-download" href="${pageContext.request.contextPath}/admin/inventory/receipts/pdf?id=${receipt.receiptID}"><span>↓</span> Tải hóa đơn PDF</a>
        </div>
        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/inventory/_subnav.jsp" %>

        <section class="receipt-document">
            <div class="receipt-document-head">
                <div class="receipt-brand">
                    <span class="receipt-brand-mark">P</span>
                    <div><span class="receipt-kicker">PETCLINIC · PHIẾU NHẬP KHO</span><h2><c:out value="${receipt.receiptCode}"/></h2></div>
                </div>
                <div class="receipt-status-block"><span class="receipt-status-dot"></span><span>Đã nhập kho</span><small>#${receipt.receiptID}</small></div>
            </div>
            <div class="receipt-meta-grid">
                <div class="receipt-meta"><i>NCC</i><div><span>Nhà cung cấp</span><strong><c:out value="${receipt.providerName}"/></strong></div></div>
                <div class="receipt-meta"><i>NV</i><div><span>Nhân viên thực hiện</span><strong><c:out value="${receipt.performedByName}"/></strong></div></div>
                <div class="receipt-meta"><i>TG</i><div><span>Ngày nhập</span><strong>${receipt.importedAtDisplay}</strong></div></div>
            </div>
            <div class="table-scroll">
                <table class="data-table receipt-table">
                    <colgroup>
                        <col class="receipt-col-index">
                        <col class="receipt-col-type">
                        <col class="receipt-col-item">
                        <col class="receipt-col-quantity">
                        <col class="receipt-col-price">
                        <col class="receipt-col-total">
                    </colgroup>
                    <thead><tr><th>#</th><th>Loại</th><th>Item</th><th class="text-right">Số lượng</th><th class="text-right">Đơn giá</th><th class="text-right">Thành tiền</th></tr></thead>
                    <tbody>
                    <c:forEach items="${receipt.details}" var="d" varStatus="i">
                        <tr><td><span class="receipt-index">${i.count}</span></td><td><span class="badge badge-info">${d.itemType}</span></td><td><strong><c:out value="${d.itemName}"/></strong></td><td class="text-right">${d.quantity}</td><td class="text-right"><fmt:formatNumber value="${d.unitPrice}" maxFractionDigits="0"/> VNĐ</td><td class="text-right receipt-money"><fmt:formatNumber value="${d.lineTotal}" maxFractionDigits="0"/> VNĐ</td></tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <div class="receipt-summary">
                <div class="receipt-note"><strong>Ghi chú</strong><span>Phiếu được tạo tự động từ dữ liệu Import Excel và đã lưu vào lịch sử giao dịch kho.</span></div>
                <div class="receipt-total"><span>Tổng thanh toán</span><strong><fmt:formatNumber value="${receipt.totalAmount}" maxFractionDigits="0"/> VNĐ</strong><small>Đã bao gồm toàn bộ mặt hàng trong phiếu</small></div>
            </div>
            <div class="receipt-footer"><span>PetClinic Inventory Management</span><span>Mã đối soát: <c:out value="${receipt.receiptCode}"/></span></div>
        </section>
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>
