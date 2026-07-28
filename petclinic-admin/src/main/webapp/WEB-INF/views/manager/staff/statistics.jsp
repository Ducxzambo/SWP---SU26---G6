<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="activeTab" value="statistics" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thống Kê Hiệu Suất Nhân Viên - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="staff" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Thống Kê Hiệu Suất Nhân Viên</h1>
            <p class="page-sub">So sánh số ca đã thực hiện và doanh thu giữa các nhân viên đang làm việc</p>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/staff/_subnav.jsp" %>

        <form action="${pageContext.request.contextPath}/manager/staff/statistics" method="get"
              class="filter-bar report-filter">
            <input type="date" name="fromDate" class="form-control no-icon"
                   value="<c:out value='${fromDate}'/>">
            <input type="date" name="toDate" class="form-control no-icon"
                   value="<c:out value='${toDate}'/>">
            <select name="role" class="form-control no-icon">
                <option value="">Tất cả vai trò</option>
                <c:forEach items="${roles}" var="r">
                    <option value="${r.roleName}" ${role == r.roleName ? 'selected' : ''}>
                        <c:out value="${r.roleName}"/>
                    </option>
                </c:forEach>
            </select>
            <button type="submit" class="btn btn-secondary">Xem thống kê</button>
        </form>

        <div class="metric-grid">
            <div class="metric-card">
                <span class="metric-label">Nhân viên</span>
                <strong>${staffCount}</strong>
            </div>
            <div class="metric-card">
                <span class="metric-label">Tổng ca hoàn thành</span>
                <strong>${totalCompleted}</strong>
            </div>
            <div class="metric-card">
                <span class="metric-label">Tổng doanh thu</span>
                <strong><fmt:formatNumber value="${totalRevenue}" type="number" groupingUsed="true"/></strong>
            </div>
        </div>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Xếp hạng theo số ca hoàn thành</span>
                <a class="btn btn-outline btn-sm"
                   href="${pageContext.request.contextPath}/manager/staff/statistics?action=export&fromDate=${fromDate}&toDate=${toDate}&role=<c:out value='${role}'/>">
                    Xuất CSV
                </a>
            </div>
            <c:choose>
                <c:when test="${empty performance}">
                    <div class="empty-state compact"><p>Chưa có dữ liệu trong khoảng thời gian này.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>Nhân viên</th>
                            <th>Vai trò</th>
                            <th>Hoàn thành</th>
                            <th style="width:180px;">So sánh</th>
                            <th>Hủy</th>
                            <th>Không đến</th>
                            <th>Tỷ lệ hoàn thành</th>
                            <th>Doanh thu</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${performance}" var="p">
                            <tr>
                                <td>
                                    <a href="${pageContext.request.contextPath}/manager/staff/detail?id=${p.staffID}">
                                        <strong><c:out value="${p.fullName}"/></strong>
                                    </a>
                                </td>
                                <td><span class="badge badge-neutral">${p.roleName}</span></td>
                                <td>${p.completedCases}</td>
                                <td>
                                    <div class="bar-track">
                                        <div class="bar-fill" style="width:${p.completedCases * 100 / maxCases}%;"></div>
                                    </div>
                                </td>
                                <td>${p.cancelledCases}</td>
                                <td>${p.noShowCases}</td>
                                <td><fmt:formatNumber value="${p.completionRate}" maxFractionDigits="1"/>%</td>
                                <td><fmt:formatNumber value="${p.revenue}" type="number" groupingUsed="true"/></td>
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
