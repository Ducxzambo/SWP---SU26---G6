<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản Lý Kho - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <aside class="sidebar">
        <div class="sidebar-logo">PetClinic</div>
        <nav>
            <a href="${pageContext.request.contextPath}/manager/stock" class="nav-item active">
                Quản lý kho
            </a>
        </nav>
        <div class="sidebar-user">
            ${sessionScope.staff.fullName}
            <a href="${pageContext.request.contextPath}/auth/logout" class="logout-link">Đăng xuất</a>
        </div>
    </aside>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Quản Lý Kho</h1>
            <p class="page-sub">Thuốc, vaccine, nhập kho, cảnh báo tồn thấp và báo cáo movement</p>
        </div>

        <c:if test="${not empty sessionScope.flashSuccess}">
            <div class="alert alert-success"><span class="alert-icon">✓</span> ${sessionScope.flashSuccess}</div>
            <c:remove var="flashSuccess" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.flashWarning}">
            <div class="alert alert-warning"><span class="alert-icon">!</span> ${sessionScope.flashWarning}</div>
            <c:remove var="flashWarning" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.flashError}">
            <div class="alert alert-error"><span class="alert-icon">x</span> ${sessionScope.flashError}</div>
            <c:remove var="flashError" scope="session"/>
        </c:if>
        <c:if test="${not empty requestScope.error}">
            <div class="alert alert-error"><span class="alert-icon">x</span> ${requestScope.error}</div>
        </c:if>

        <div class="metric-grid">
            <div class="metric-card">
                <span class="metric-label">Tổng item</span>
                <strong>${fn:length(allItems)}</strong>
            </div>
            <div class="metric-card">
                <span class="metric-label">Cảnh báo tồn thấp</span>
                <strong>${fn:length(lowStock)}</strong>
            </div>
            <div class="metric-card">
                <span class="metric-label">Giao dịch gần đây</span>
                <strong>${fn:length(transactions)}</strong>
            </div>
        </div>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Nhập kho</span>
            </div>
            <div class="card-body">
                <form action="${pageContext.request.contextPath}/manager/stock" method="post" id="stockInForm">
                    <input type="hidden" name="action" value="stockIn">

                    <div class="form-row col-3">
                        <div class="form-group">
                            <label class="form-label" for="stockItemKey">Item có sẵn</label>
                            <select id="stockItemKey" name="stockItemKey" class="form-control no-icon">
                                <option value="">Tạo item mới</option>
                                <c:forEach items="${allItems}" var="item">
                                    <option value="${item.itemType}:${item.itemID}">
                                        [${item.itemType}] <c:out value="${item.displayName}"/> - còn ${item.stockQty}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="itemType">Loại item mới</label>
                            <select id="itemType" name="itemType" class="form-control no-icon">
                                <option value="Medicine">Medicine</option>
                                <option value="Vaccine">Vaccine</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="itemName">Tên item mới</label>
                            <input type="text" id="itemName" name="itemName"
                                   class="form-control no-icon" placeholder="VD: Amoxicillin 250mg">
                        </div>
                    </div>

                    <div class="form-row col-3">
                        <div class="form-group">
                            <label class="form-label" for="quantity">
                                Số lượng <span class="required">*</span>
                            </label>
                            <input type="number" id="quantity" name="quantity"
                                   class="form-control no-icon" min="1" step="1" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="unit">Đơn vị thuốc</label>
                            <input type="text" id="unit" name="unit"
                                   class="form-control no-icon" placeholder="viên, chai, lọ">
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="unitPrice">Đơn giá</label>
                            <input type="number" id="unitPrice" name="unitPrice"
                                   class="form-control no-icon" min="0" step="0.01">
                        </div>
                    </div>

                    <div class="form-row col-3">
                        <div class="form-group">
                            <label class="form-label" for="minStockLevel">Ngưỡng cảnh báo thuốc</label>
                            <input type="number" id="minStockLevel" name="minStockLevel"
                                   class="form-control no-icon" min="0" step="1" placeholder="10">
                        </div>
                    </div>

                    <div class="form-actions">
                        <button type="submit" class="btn btn-primary">Lưu nhập kho</button>
                    </div>
                </form>
            </div>
        </div>

        <form action="${pageContext.request.contextPath}/manager/stock" method="get" class="filter-bar">
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
            <a href="${pageContext.request.contextPath}/manager/stock" class="btn btn-outline">Xóa lọc</a>
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
                            <th>Cập nhật ngưỡng</th>
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
                                <td>
                                    <c:choose>
                                        <c:when test="${item.medicine}">
                                            <form action="${pageContext.request.contextPath}/manager/stock"
                                                  method="post" class="inline-form">
                                                <input type="hidden" name="action" value="threshold">
                                                <input type="hidden" name="medicineID" value="${item.itemID}">
                                                <input type="number" name="minStockLevel"
                                                       class="form-control no-icon compact-input"
                                                       min="0" step="1" value="${item.effectiveMinStockLevel}">
                                                <button type="submit" class="btn btn-secondary btn-sm">Lưu</button>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-soft">-</span>
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

        <div class="card">
            <div class="card-header">
                <span class="card-title">Cảnh báo tồn thấp</span>
            </div>
            <c:choose>
                <c:when test="${empty lowStock}">
                    <div class="empty-state compact"><p>Chưa có item dưới ngưỡng cảnh báo.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>Loại</th>
                            <th>Item</th>
                            <th>Tồn</th>
                            <th>Ngưỡng</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${lowStock}" var="item">
                            <tr>
                                <td>${item.itemType}</td>
                                <td><c:out value="${item.name}"/></td>
                                <td>${item.stockQty}</td>
                                <td>${item.effectiveMinStockLevel}</td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>

        <form action="${pageContext.request.contextPath}/manager/stock" method="get" class="filter-bar report-filter">
            <input type="date" name="fromDate" class="form-control no-icon"
                   value="<c:out value='${fromDate}'/>">
            <input type="date" name="toDate" class="form-control no-icon"
                   value="<c:out value='${toDate}'/>">
            <button type="submit" class="btn btn-secondary">Xem báo cáo</button>
            <a class="btn btn-outline"
               href="${pageContext.request.contextPath}/manager/stock?action=export&fromDate=${fromDate}&toDate=${toDate}">
                Xuất Excel CSV
            </a>
        </form>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Báo cáo movement</span>
            </div>
            <c:choose>
                <c:when test="${empty movementReport}">
                    <div class="empty-state compact"><p>Chưa có giao dịch kho trong khoảng thời gian này.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>Loại</th>
                            <th>Item</th>
                            <th>Stock-in</th>
                            <th>Stock-out</th>
                            <th>Net</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${movementReport}" var="r">
                            <tr>
                                <td>${r.itemType}</td>
                                <td><c:out value="${r.itemName}"/></td>
                                <td>${r.totalStockIn}</td>
                                <td>${r.totalStockOut}</td>
                                <td>
                                    <span class="${r.netChange < 0 ? 'text-danger' : 'text-success'}">
                                        ${r.netChange}
                                    </span>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Giao dịch kho gần đây</span>
            </div>
            <c:choose>
                <c:when test="${empty transactions}">
                    <div class="empty-state compact"><p>Chưa có giao dịch kho.</p></div>
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

<script>
    var stockInForm = document.getElementById('stockInForm');
    if (stockInForm) {
        stockInForm.addEventListener('submit', function (e) {
            var existing = document.getElementById('stockItemKey').value;
            var itemType = document.getElementById('itemType').value;
            var name = document.getElementById('itemName').value.trim();
            var unit = document.getElementById('unit').value.trim();
            var price = document.getElementById('unitPrice').value.trim();

            if (!existing && (!name || price === '' || (itemType === 'Medicine' && !unit))) {
                e.preventDefault();
                alert('Khi tạo item mới, vui lòng nhập tên, đơn giá và đơn vị nếu là Medicine.');
            }
        });
    }
</script>
<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>
