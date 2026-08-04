<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="activeModule" value="attendance-stats" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vi Phạm Chấm Công - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <style>
        tr.attendance-alert td { background:#fdecea; }
        .cnt-alert { color:#b91c1c; font-weight:800; }
    </style>
</head>
<body>
<div class="layout">
    <c:set var="activeModule" value="attendance-stats" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Vi Phạm Chấm Công</h1>
            <p class="page-sub">
                Cảnh báo khi: Vắng (Absent) &gt; 9 ca, Nghỉ phép (OnLeave) &gt; 3 ngày, hoặc Đi trễ (Late) &gt; 15 ca.
            </p>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>

        <form action="${pageContext.request.contextPath}/manager/attendance/statistics" method="get" class="filter-bar report-filter">
            <input type="date" name="fromDate" class="form-control no-icon" value="<c:out value='${fromDate}'/>">
            <input type="date" name="toDate" class="form-control no-icon" value="<c:out value='${toDate}'/>">
            <button type="submit" class="btn btn-secondary">Xem thống kê</button>
            <a href="${pageContext.request.contextPath}/manager/attendance/statistics" class="btn btn-outline">Toàn bộ thời gian</a>
        </form>

        <div class="metric-grid" style="grid-template-columns:repeat(2,1fr);max-width:600px;">
            <div class="metric-card">
                <span class="metric-label">Tổng nhân viên</span>
                <strong>${fn:length(summary)}</strong>
            </div>
            <div class="metric-card">
                <span class="metric-label">Nhân viên đang bị cảnh báo</span>
                <strong class="cnt-alert">${alertCount}</strong>
            </div>
        </div>

        <div class="card">
            <div class="card-header"><span class="card-title">Chi tiết theo nhân viên</span></div>
            <c:choose>
                <c:when test="${empty summary}">
                    <div class="empty-state compact"><p>Chưa có dữ liệu chấm công.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>Nhân viên</th><th>Vai trò</th>
                            <th>Đi trễ (Late)</th><th>Vắng (Absent)</th><th>Nghỉ phép (OnLeave)</th>
                            <th>Trạng thái</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${summary}" var="s">
                            <tr class="${s.anyAlert ? 'attendance-alert' : ''}">
                                <td>
                                    <a href="${pageContext.request.contextPath}/manager/staff/detail?id=${s.staffID}">
                                        <strong><c:out value="${s.staffName}"/></strong>
                                    </a>
                                </td>
                                <td><span class="badge badge-neutral">${s.roleName}</span></td>
                                <td class="${s.lateAlert ? 'cnt-alert' : ''}">${s.lateCount}</td>
                                <td class="${s.absentAlert ? 'cnt-alert' : ''}">${s.absentCount}</td>
                                <td class="${s.onLeaveAlert ? 'cnt-alert' : ''}">${s.onLeaveCount}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${s.anyAlert}"><span class="badge badge-error">Cần chú ý</span></c:when>
                                        <c:otherwise><span class="badge badge-success">Bình thường</span></c:otherwise>
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