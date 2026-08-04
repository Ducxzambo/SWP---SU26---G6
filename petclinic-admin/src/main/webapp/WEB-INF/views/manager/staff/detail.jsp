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
    <title><c:out value="${staff.fullName}"/> - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="staff" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1><c:out value="${staff.fullName}"/></h1>
            <p class="page-sub">
                <span class="badge badge-neutral">${staff.roleName}</span>
                <c:choose>
                    <c:when test="${staff.active}">
                        <span class="badge badge-success">Đang làm việc</span>
                    </c:when>
                    <c:otherwise>
                        <span class="badge badge-error">Đã nghỉ</span>
                    </c:otherwise>
                </c:choose>
            </p>
            <div class="page-actions">
                <a href="${pageContext.request.contextPath}/manager/staff/edit?id=${staff.staffID}"
                   class="btn btn-primary">Sửa thông tin</a>
                <a href="${pageContext.request.contextPath}/manager/staff" class="btn btn-outline">
                    Về danh sách
                </a>
            </div>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/staff/_subnav.jsp" %>

        <div class="card">
            <div class="card-header"><span class="card-title">Thông tin cá nhân</span></div>
            <div class="card-body">
                <div class="detail-grid">
                    <div><span class="detail-label">Email</span><span class="detail-value"><c:out value="${staff.email}"/></span></div>
                    <div><span class="detail-label">Điện thoại</span><span class="detail-value"><c:out value="${empty staff.phone ? '-' : staff.phone}"/></span></div>
                    <div><span class="detail-label">Chuyên môn</span><span class="detail-value"><c:out value="${empty staff.specialization ? '-' : staff.specialization}"/></span></div>
                    <div><span class="detail-label">Số chứng chỉ</span><span class="detail-value"><c:out value="${empty staff.licenseNumber ? '-' : staff.licenseNumber}"/></span></div>
                    <div><span class="detail-label">Ngày vào làm</span><span class="detail-value">${staff.hireDate}</span></div>
                    <div><span class="detail-label">Tạo tài khoản lúc</span><span class="detail-value">${fn:substring(staff.createdAt, 0, 16)}</span></div>
                </div>
            </div>
        </div>

        <div class="metric-grid">
            <div class="metric-card">
                <span class="metric-label">Ca đã hoàn thành</span>
                <strong>${performance.completedCases}</strong>
            </div>
            <div class="metric-card">
                <span class="metric-label">Tỷ lệ hoàn thành</span>
                <strong><fmt:formatNumber value="${performance.completionRate}" maxFractionDigits="1"/>%</strong>
            </div>
            <div class="metric-card">
                <span class="metric-label">Doanh thu tạo ra</span>
                <strong><fmt:formatNumber value="${performance.revenue}" type="number" groupingUsed="true"/></strong>
            </div>
        </div>

        <div class="card">
            <div class="card-header"><span class="card-title">Dịch vụ đã thực hiện (toàn thời gian)</span></div>
            <c:choose>
                <c:when test="${empty breakdown}">
                    <div class="empty-state compact"><p>Chưa có ca nào được hoàn thành.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>Dịch vụ</th>
                            <th>Số lần thực hiện</th>
                            <th>Doanh thu</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${breakdown}" var="b">
                            <tr>
                                <td><c:out value="${b.serviceName}"/></td>
                                <td>${b.completedCount}</td>
                                <td><fmt:formatNumber value="${b.revenue}" type="number" groupingUsed="true"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="card">
            <div class="card-header"><span class="card-title">Đặt lại mật khẩu</span></div>
            <div class="card-body">
                <form action="${pageContext.request.contextPath}/manager/staff/reset-password" method="post">
                    <input type="hidden" name="staffID" value="${staff.staffID}">
                    <div class="form-row col-2">
                        <div class="form-group">
                            <label class="form-label" for="newPassword">Mật khẩu mới</label>
                            <input type="password" id="newPassword" name="newPassword"
                                   class="form-control no-icon" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="confirmPassword">Xác nhận mật khẩu</label>
                            <input type="password" id="confirmPassword" name="confirmPassword"
                                   class="form-control no-icon" required>
                        </div>
                    </div>
                    <span class="form-hint">Ít nhất 6 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.</span>
                    <div class="form-actions">
                        <button type="submit" class="btn btn-secondary">Đặt lại mật khẩu</button>
                    </div>
                </form>
            </div>
        </div>

        <div class="card">
            <div class="card-header"><span class="card-title">Trạng thái tài khoản</span></div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${isSelf}">
                        <p class="text-soft">Bạn không thể tự vô hiệu hoá tài khoản đang đăng nhập.</p>
                    </c:when>
                    <c:otherwise>
                        <form action="${pageContext.request.contextPath}/manager/staff/status" method="post">
                            <input type="hidden" name="staffID" value="${staff.staffID}">
                            <input type="hidden" name="active" value="${staff.active ? 'false' : 'true'}">
                            <input type="hidden" name="redirectTo" value="/manager/staff/detail?id=${staff.staffID}">
                            <button type="submit" class="btn btn-outline">
                                ${staff.active ? 'Vô hiệu hoá nhân viên này' : 'Kích hoạt lại nhân viên này'}
                            </button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </main>
</div>

<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>
