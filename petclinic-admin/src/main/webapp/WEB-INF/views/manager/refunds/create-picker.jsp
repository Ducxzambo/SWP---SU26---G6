<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="activeTab" value="create" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chọn Lịch Hẹn - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="refunds" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Tạo Yêu Cầu Hoàn Tiền</h1>
            <p class="page-sub">Bước 1/2 — Chọn lịch hẹn đã Huỷ / Vắng mặt / Hoàn thành cần hoàn tiền</p>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/refunds/_subnav.jsp" %>

        <form action="${pageContext.request.contextPath}/manager/refunds/create" method="get" class="refund-search-bar">
            <div class="refund-search-input-wrap">
                <span class="input-icon"></span>
                <input type="text" name="q" class="form-control"
                       placeholder="Tìm theo tên khách hàng hoặc tên thú cưng..."
                       value="<c:out value='${keyword}'/>"
                       onchange="this.form.submit()">
            </div>
            <select name="status" class="form-control no-icon refund-status-select" onchange="this.form.submit()">
                <option value="">Tất cả trạng thái</option>
                <option value="Cancelled" ${status == 'Cancelled' ? 'selected' : ''}>Đã huỷ</option>
                <option value="NoShow" ${status == 'NoShow' ? 'selected' : ''}>Vắng mặt</option>
                <option value="Done" ${status == 'Done' ? 'selected' : ''}>Hoàn thành</option>
            </select>
        </form>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Lịch hẹn có thể tạo yêu cầu hoàn tiền</span>
                <span class="text-soft">${fn:length(appointments)} kết quả</span>
            </div>
            <c:choose>
                <c:when test="${empty appointments}">
                    <div class="empty-state"><p>Không có lịch hẹn phù hợp.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Mã hóa đơn</th>
                            <th>Khách hàng</th>
                            <th>Thú cưng</th>
                            <th>Ngày hẹn</th>
                            <th>Dịch vụ</th>
                            <th>Trạng thái</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${appointments}" var="a">
                            <tr>
                                <td>${a.appointmentID}</td>
                                <td><c:out value="${empty a.invoiceCode ? '-' : a.invoiceCode}"/></td>
                                <td><c:out value="${a.customerName}"/></td>
                                <td><c:out value="${empty a.petName ? 'Chưa chọn' : a.petName}"/></td>
                                <td>${a.formattedAppointmentDate}</td>
                                <td><c:out value="${empty a.serviceName ? '-' : a.serviceName}"/></td>
                                <td><span class="badge badge-neutral">${a.status}</span></td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/manager/refunds/create?appointmentId=${a.appointmentID}"
                                       class="btn btn-outline btn-sm">Chọn</a>
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
