<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="activeTab" value="report" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Báo Cáo Biến Động Kho - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="inventory" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Báo Cáo Biến Động Kho</h1>
            <p class="page-sub">Tổng nhập/xuất theo item trong khoảng thời gian, xuất được ra CSV</p>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/inventory/_subnav.jsp" %>

        <form action="${pageContext.request.contextPath}/manager/inventory/report" method="get" class="filter-bar report-filter">
            <input type="date" name="fromDate" class="form-control no-icon"
                   value="<c:out value='${fromDate}'/>">
            <input type="date" name="toDate" class="form-control no-icon"
                   value="<c:out value='${toDate}'/>">
            <button type="submit" class="btn btn-secondary">Xem báo cáo</button>
            <a class="btn btn-outline"
               href="${pageContext.request.contextPath}/manager/inventory/report?action=export&fromDate=${fromDate}&toDate=${toDate}">
                Xuất Excel CSV
            </a>
        </form>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Báo cáo movement</span>
                <span class="text-soft">${fn:length(movementReport)} dòng</span>
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
    </main>
</div>

<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>
