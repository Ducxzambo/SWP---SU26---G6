<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="activeTab" value="receipts" scope="request"/>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Phiếu nhập kho - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>
<div class="layout">
    <c:set var="activeModule" value="inventory" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>
    <main class="main-content main-content-wide">
        <div class="page-header receipt-page-header">
            <div>
                <h1>Phiếu nhập kho</h1>
                <p class="page-sub">Hóa đơn được tạo tự động sau mỗi lần import Excel thành công</p>
            </div>
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/admin/inventory/stock-in">+ Import nhập kho</a>
        </div>
        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/inventory/_subnav.jsp" %>

        <div class="card receipt-list-card">
            <div class="card-header">
                <span class="card-title">Danh sách phiếu nhập</span>
                <span class="text-soft">${fn:length(receipts)} phiếu</span>
            </div>
            <c:choose>
                <c:when test="${empty receipts}">
                    <div class="empty-state compact">
                        <p>Chưa có phiếu nhập. Phiếu sẽ xuất hiện sau khi bạn import Excel thành công.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-scroll">
                        <table class="data-table receipt-table">
                            <thead><tr><th>Mã phiếu</th><th>Nhà cung cấp</th><th>Nhân viên</th><th>Ngày nhập</th><th class="text-right">Tổng tiền</th><th>Thao tác</th></tr></thead>
                            <tbody>
                            <c:forEach items="${receipts}" var="r">
                                <tr>
                                    <td><span class="receipt-code"><c:out value="${r.receiptCode}"/></span></td>
                                    <td><c:out value="${r.providerName}"/></td>
                                    <td><c:out value="${r.performedByName}"/></td>
                                    <td>${r.importedAtDisplay}</td>
                                    <td class="text-right receipt-money"><fmt:formatNumber value="${r.totalAmount}" maxFractionDigits="0"/> VNĐ</td>
                                    <td><div class="receipt-actions"><a class="btn btn-outline btn-sm" href="${pageContext.request.contextPath}/admin/inventory/receipts/detail?id=${r.receiptID}">Xem chi tiết</a><a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/admin/inventory/receipts/pdf?id=${r.receiptID}">↓ PDF</a></div></td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>
