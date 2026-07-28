<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="activeTab" value="list" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản Lý Nhân Viên - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="staff" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Quản Lý Nhân Viên</h1>
            <p class="page-sub">Danh sách toàn bộ nhân viên phòng khám</p>
            <div class="page-actions">
                <a href="${pageContext.request.contextPath}/manager/staff/edit" class="btn btn-primary">
                    + Thêm nhân viên
                </a>
            </div>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/staff/_subnav.jsp" %>

        <form action="${pageContext.request.contextPath}/manager/staff" method="get" class="filter-bar">
            <div class="input-wrap">
                <span class="input-icon"></span>
                <input type="text" name="q" class="form-control"
                       placeholder="Tìm theo tên hoặc email..."
                       value="<c:out value='${keyword}'/>">
            </div>
            <select name="role" class="form-control no-icon">
                <option value="">Tất cả vai trò</option>
                <c:forEach items="${roles}" var="r">
                    <option value="${r.roleName}" ${role == r.roleName ? 'selected' : ''}>
                        <c:out value="${r.roleName}"/>
                    </option>
                </c:forEach>
            </select>
            <select name="status" class="form-control no-icon">
                <option value="">Tất cả trạng thái</option>
                <option value="active" ${status == 'active' ? 'selected' : ''}>Đang làm việc</option>
                <option value="inactive" ${status == 'inactive' ? 'selected' : ''}>Đã nghỉ</option>
            </select>
            <button type="submit" class="btn btn-secondary">Lọc</button>
            <a href="${pageContext.request.contextPath}/manager/staff" class="btn btn-outline">Xóa lọc</a>
        </form>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Nhân viên</span>
                <span class="text-soft">${fn:length(staffList)} người</span>
            </div>
            <c:choose>
                <c:when test="${empty staffList}">
                    <div class="empty-state"><p>Không có nhân viên phù hợp với bộ lọc.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Họ tên</th>
                            <th>Vai trò</th>
                            <th>Email</th>
                            <th>Điện thoại</th>
                            <th>Trạng thái</th>
                            <th>Hành động</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${staffList}" var="s" varStatus="loop">
                            <tr>
                                <td>${loop.count}</td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/manager/staff/detail?id=${s.staffID}">
                                        <strong><c:out value="${s.fullName}"/></strong>
                                    </a>
                                </td>
                                <td><span class="badge badge-neutral">${s.roleName}</span></td>
                                <td><c:out value="${s.email}"/></td>
                                <td><c:out value="${empty s.phone ? '-' : s.phone}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${s.active}">
                                            <span class="badge badge-success">Đang làm việc</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-error">Đã nghỉ</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <form action="${pageContext.request.contextPath}/manager/staff/status"
                                          method="post" class="inline-form">
                                        <input type="hidden" name="staffID" value="${s.staffID}">
                                        <input type="hidden" name="active" value="${s.active ? 'false' : 'true'}">
                                        <input type="hidden" name="redirectTo"
                                               value="/manager/staff?q=<c:out value='${keyword}'/>&role=<c:out value='${role}'/>&status=<c:out value='${status}'/>">
                                        <button type="submit" class="btn btn-outline btn-sm">
                                            ${s.active ? 'Vô hiệu hoá' : 'Kích hoạt lại'}
                                        </button>
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
