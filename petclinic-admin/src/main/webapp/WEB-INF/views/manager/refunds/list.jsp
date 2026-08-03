<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="activeTab" value="list" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Yêu Cầu Hoàn Tiền - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="refunds" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Yêu Cầu Hoàn Tiền</h1>
            <p class="page-sub">Danh sách toàn bộ yêu cầu hoàn tiền, yêu cầu đang chờ xử lý hiển thị trước</p>
            <div class="page-actions">
                <a href="${pageContext.request.contextPath}/manager/refunds/create" class="btn btn-primary">
                    + Tạo yêu cầu mới
                </a>
            </div>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/refunds/_subnav.jsp" %>

        <div class="metric-grid">
            <div class="metric-card">
                <span class="metric-label">Đang chờ xử lý</span>
                <strong>${requestedCount}</strong>
            </div>
            <div class="metric-card">
                <span class="metric-label">Tổng số yêu cầu</span>
                <strong>${fn:length(refunds)}</strong>
            </div>
        </div>

        <form action="${pageContext.request.contextPath}/manager/refunds" method="get" class="filter-bar report-filter">
            <select name="status" class="form-control no-icon" onchange="this.form.submit()">
                <option value="">Tất cả trạng thái</option>
                <option value="Requested" ${status == 'Requested' ? 'selected' : ''}>Đang chờ xử lý</option>
                <option value="Processed" ${status == 'Processed' ? 'selected' : ''}>Đã hoàn tiền</option>
                <option value="Rejected" ${status == 'Rejected' ? 'selected' : ''}>Đã từ chối</option>
            </select>
            <select name="sort" class="form-control no-icon" onchange="this.form.submit()">
                <option value="date_asc" ${sort == 'date_asc' || empty sort ? 'selected' : ''}>Cũ nhất</option>
                <option value="date_desc" ${sort == 'date_desc'  ? 'selected' : ''}>Mới nhất</option>
                <option value="amount_desc" ${sort == 'amount_desc' ? 'selected' : ''}>Số tiền giảm dần</option>
                <option value="amount_asc" ${sort == 'amount_asc' ? 'selected' : ''}>Số tiền tăng dần</option>
            </select>
        </form>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Danh sách yêu cầu</span>
            </div>
            <c:choose>
                <c:when test="${empty refunds}">
                    <div class="empty-state"><p>Không có yêu cầu hoàn tiền phù hợp với bộ lọc.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Khách hàng</th>
                            <th>Thú cưng</th>
                            <th>Ngày hẹn</th>
                            <th>Số tiền hoàn</th>
                            <th>Trạng thái</th>
                            <th>Ngày yêu cầu</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${refunds}" var="r">
                            <tr>
                                <td>${r.refundID}</td>
                                <td><c:out value="${r.customerName}"/></td>
                                <td><c:out value="${r.petName}"/></td>
                                <td>${r.formattedAppointmentDate}</td>
                                <td><fmt:formatNumber value="${r.paidAmount}" type="number" groupingUsed="true"/>đ</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${r.requested}">
                                            <span class="badge badge-warning">Đang chờ xử lý</span>
                                        </c:when>
                                        <c:when test="${r.processed}">
                                            <span class="badge badge-success">Đã hoàn tiền</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-error">Đã từ chối</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${r.formattedRequestedAt}</td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/manager/refunds/detail?id=${r.refundID}"
                                       class="btn btn-outline btn-sm">Xem chi tiết</a>
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
