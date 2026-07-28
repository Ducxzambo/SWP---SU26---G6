<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="activeTab" value="list" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tồn Kho - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="inventory" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Quản Lý Kho</h1>
            <p class="page-sub">Danh sách tồn kho hiện tại của thuốc và vaccine</p>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/inventory/_subnav.jsp" %>

        <div class="metric-grid">
            <div class="metric-card">
                <span class="metric-label">Tổng item</span>
                <strong>${totalItems}</strong>
            </div>
            <a class="metric-card metric-card-link"
               href="${pageContext.request.contextPath}/manager/inventory/thresholds?stockLevel=low">
                <span class="metric-label">Tồn thấp</span>
                <strong>${lowStockCount}</strong>
            </a>
            <a class="metric-card metric-card-link"
               href="${pageContext.request.contextPath}/manager/inventory/thresholds?stockLevel=out">
                <span class="metric-label">Hết hàng</span>
                <strong>${outOfStockCount}</strong>
            </a>
        </div>

        <form action="${pageContext.request.contextPath}/manager/inventory" method="get" class="filter-bar">
            <div class="input-wrap">
                <span class="input-icon"></span>
                <input type="text" name="q" class="form-control"
                       placeholder="Tìm theo tên item..."
                       value="<c:out value='${keyword}'/>">
            </div>
            <select name="itemType" class="form-control no-icon">
                <option value="">Tất cả loại</option>
                <option value="Medicine" ${itemType == 'Medicine' ? 'selected' : ''}>Medicine</option>
                <option value="Vaccine" ${itemType == 'Vaccine' ? 'selected' : ''}>Vaccine</option>
            </select>
            <select name="stockLevel" class="form-control no-icon">
                <option value="">Tất cả tồn kho</option>
                <option value="available" ${stockLevel == 'available' ? 'selected' : ''}>Còn hàng</option>
                <option value="low" ${stockLevel == 'low' ? 'selected' : ''}>Tồn thấp</option>
                <option value="out" ${stockLevel == 'out' ? 'selected' : ''}>Hết hàng</option>
            </select>
            <button type="submit" class="btn btn-secondary">Lọc</button>
            <a href="${pageContext.request.contextPath}/manager/inventory" class="btn btn-outline">Xóa lọc</a>
        </form>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Tồn kho hiện tại</span>
                <span class="text-soft">${fn:length(inventory)} dòng</span>
            </div>
            <c:choose>
                <c:when test="${empty inventory}">
                    <div class="empty-state"><p>Không có item phù hợp với bộ lọc.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Loại</th>
                            <th>Item</th>
                            <th>Đơn vị</th>
                            <th>Tồn</th>
                            <th>Ngưỡng</th>
                            <th>Đơn giá</th>
                            <th>Trạng thái</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${inventory}" var="item" varStatus="loop">
                            <tr>
                                <td>${loop.count}</td>
                                <td><span class="badge badge-neutral">${item.itemType}</span></td>
                                <td><strong><c:out value="${item.name}"/></strong></td>
                                <td><c:out value="${empty item.unit ? '-' : item.unit}"/></td>
                                <td>${item.stockQty}</td>
                                <td>${item.effectiveMinStockLevel}</td>
                                <td><fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${item.outOfStock}">
                                            <span class="badge badge-error">Hết hàng</span>
                                        </c:when>
                                        <c:when test="${item.lowStock}">
                                            <span class="badge badge-warning">Tồn thấp</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-success">Còn hàng</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
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
