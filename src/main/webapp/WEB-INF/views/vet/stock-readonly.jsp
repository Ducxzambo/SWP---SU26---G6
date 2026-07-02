<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
    <aside class="sidebar">
        <div class="sidebar-logo">PetClinic</div>
        <nav>
            <a href="${pageContext.request.contextPath}/vet/examination" class="nav-item">
                Hàng chờ khám
            </a>
            <a href="${pageContext.request.contextPath}/vet/stock" class="nav-item active">
                Tồn kho
            </a>
        </nav>
        <div class="sidebar-user">
            ${sessionScope.staff.fullName}
            <a href="${pageContext.request.contextPath}/auth/logout" class="logout-link">Đăng xuất</a>
        </div>
    </aside>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Tồn Kho</h1>
            <p class="page-sub">Chế độ chỉ xem: tên item, loại, đơn vị và số lượng còn</p>
        </div>

        <c:if test="${not empty requestScope.error}">
            <div class="alert alert-error"><span class="alert-icon">x</span> ${requestScope.error}</div>
        </c:if>

        <form action="${pageContext.request.contextPath}/vet/stock" method="get" class="filter-bar">
            <div class="input-wrap">
                <span class="input-icon"></span>
                <input type="text" name="q" class="form-control"
                       placeholder="Tìm item..."
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
            <a href="${pageContext.request.contextPath}/vet/stock" class="btn btn-outline">Xóa lọc</a>
        </form>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Danh sách khả dụng</span>
                <span class="text-soft">${fn:length(inventory)} dòng</span>
            </div>
            <c:choose>
                <c:when test="${empty inventory}">
                    <div class="empty-state"><p>Không có item phù hợp.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Loại</th>
                            <th>Item</th>
                            <th>Đơn vị</th>
                            <th>Tồn kho</th>
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
                                <td>
                                    <c:choose>
                                        <c:when test="${item.outOfStock}">
                                            <span class="badge badge-error">Không khả dụng</span>
                                        </c:when>
                                        <c:when test="${item.lowStock}">
                                            <span class="badge badge-warning">Sắp hết</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-success">Khả dụng</span>
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
