<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="activeTab" value="transactions" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lịch Sử Giao Dịch Kho - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="inventory" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Lịch Sử Giao Dịch Kho</h1>
            <p class="page-sub">Nhật ký từng lần nhập/xuất kho, mới nhất trước</p>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/inventory/_subnav.jsp" %>

        <form action="${pageContext.request.contextPath}/manager/inventory/transactions" method="get" class="filter-bar report-filter">
            <input type="date" name="fromDate" class="form-control no-icon"
                   value="<c:out value='${fromDate}'/>">
            <input type="date" name="toDate" class="form-control no-icon"
                   value="<c:out value='${toDate}'/>">
            <select name="itemType" class="form-control no-icon">
                <option value="">Tất cả loại</option>
                <option value="Medicine" ${itemType == 'Medicine' ? 'selected' : ''}>Medicine</option>
                <option value="Vaccine" ${itemType == 'Vaccine' ? 'selected' : ''}>Vaccine</option>
            </select>
            <button type="submit" class="btn btn-secondary">Lọc</button>
        </form>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Giao dịch kho</span>
                <span class="text-soft">${fn:length(transactions)} dòng</span>
            </div>
            <c:choose>
                <c:when test="${empty transactions}">
                    <div class="empty-state compact"><p>Chưa có giao dịch kho phù hợp với bộ lọc.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>Thời gian</th>
                            <th>Loại</th>
                            <th>Item</th>
                            <th>Stock</th>
                            <th>Số lượng</th>
                            <th>Lý do</th>
                            <th>Người thực hiện</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${transactions}" var="t">
                            <tr>
                                <td><c:out value="${t.transactionDate}"/></td>
                                <td>${t.itemType}</td>
                                <td><c:out value="${t.itemName}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${t.stockIn}">
                                            <span class="badge badge-success">Stock-in</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-info">Stock-out</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${t.absoluteQuantity}</td>
                                <td><c:out value="${t.reason}"/></td>
                                <td><c:out value="${empty t.performedByName ? '-' : t.performedByName}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
</div>

<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>
