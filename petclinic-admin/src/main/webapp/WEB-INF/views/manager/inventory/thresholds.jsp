<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="activeTab" value="thresholds" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ngưỡng Cảnh Báo - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="inventory" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Ngưỡng Cảnh Báo Tồn Kho</h1>
            <p class="page-sub">Xem item nào đang tồn thấp/hết hàng và chỉnh ngưỡng cảnh báo cho từng item</p>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/inventory/_subnav.jsp" %>

        <form action="${pageContext.request.contextPath}/admin/inventory/thresholds" method="get" class="filter-bar">
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
            <a href="${pageContext.request.contextPath}/admin/inventory/thresholds" class="btn btn-outline">Xóa lọc</a>
        </form>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Ngưỡng cảnh báo theo item</span>
                <span class="text-soft">${fn:length(inventory)} dòng - tồn thấp/hết hàng hiển thị trước</span>
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
                            <th>Trạng thái</th>
                            <th>Ngưỡng hiện tại</th>
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
                                <td>
                                    <form action="${pageContext.request.contextPath}/admin/inventory/thresholds"
                                          method="post" class="inline-form">
                                        <input type="hidden" name="itemType" value="${item.itemType}">
                                        <input type="hidden" name="itemID" value="${item.itemID}">
                                        <input type="hidden" name="q" value="<c:out value='${keyword}'/>">
                                        <input type="hidden" name="filterItemType" value="<c:out value='${itemType}'/>">
                                        <input type="hidden" name="stockLevel" value="<c:out value='${stockLevel}'/>">
                                        <input type="number" name="minStockLevel"
                                               class="form-control no-icon compact-input"
                                               min="0" step="1" value="${item.effectiveMinStockLevel}">
                                        <button type="submit" class="btn btn-secondary btn-sm">Lưu</button>
                                    </form>
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
