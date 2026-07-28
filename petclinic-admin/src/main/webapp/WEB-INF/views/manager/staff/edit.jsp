<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeTab" value="list" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${isNew ? 'Thêm Nhân Viên' : 'Sửa Thông Tin Nhân Viên'} - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="staff" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>${isNew ? 'Thêm Nhân Viên' : 'Sửa Thông Tin Nhân Viên'}</h1>
            <p class="page-sub">
                ${isNew ? 'Đăng ký tài khoản nhân viên mới' : 'Cập nhật hồ sơ nhân viên hiện có'}
            </p>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/staff/_subnav.jsp" %>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Thông tin nhân viên</span>
            </div>
            <div class="card-body">
                <form action="${pageContext.request.contextPath}/manager/staff/edit" method="post">
                    <c:if test="${!isNew}">
                        <input type="hidden" name="staffID" value="${staff.staffID}">
                    </c:if>

                    <div class="form-row col-2">
                        <div class="form-group">
                            <label class="form-label" for="fullName">
                                Họ tên <span class="required">*</span>
                            </label>
                            <input type="text" id="fullName" name="fullName" class="form-control no-icon"
                                   value="<c:out value='${staff.fullName}'/>" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="email">
                                Email <span class="required">*</span>
                            </label>
                            <input type="email" id="email" name="email" class="form-control no-icon"
                                   value="<c:out value='${staff.email}'/>" required>
                        </div>
                    </div>

                    <div class="form-row col-2">
                        <div class="form-group">
                            <label class="form-label" for="phone">Điện thoại</label>
                            <input type="text" id="phone" name="phone" class="form-control no-icon"
                                   value="<c:out value='${staff.phone}'/>">
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="roleID">
                                Vai trò <span class="required">*</span>
                            </label>
                            <select id="roleID" name="roleID" class="form-control no-icon" required>
                                <option value="">-- Chọn vai trò --</option>
                                <c:forEach items="${roles}" var="r">
                                    <option value="${r.roleID}" ${staff.roleID == r.roleID ? 'selected' : ''}>
                                        <c:out value="${r.roleName}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>

                    <div class="form-row col-2">
                        <div class="form-group">
                            <label class="form-label" for="specialization">Chuyên môn</label>
                            <input type="text" id="specialization" name="specialization"
                                   class="form-control no-icon" placeholder="VD: Ngoại khoa thú nhỏ"
                                   value="<c:out value='${staff.specialization}'/>">
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="licenseNumber">Số chứng chỉ hành nghề</label>
                            <input type="text" id="licenseNumber" name="licenseNumber"
                                   class="form-control no-icon"
                                   value="<c:out value='${staff.licenseNumber}'/>">
                        </div>
                    </div>

                    <div class="form-row col-2">
                        <div class="form-group">
                            <label class="form-label" for="hireDate">Ngày vào làm</label>
                            <input type="date" id="hireDate" name="hireDate" class="form-control no-icon"
                                   value="${staff.hireDate}">
                        </div>
                    </div>

                    <c:if test="${isNew}">
                        <div class="form-row col-2">
                            <div class="form-group">
                                <label class="form-label" for="password">
                                    Mật khẩu ban đầu <span class="required">*</span>
                                </label>
                                <input type="password" id="password" name="password"
                                       class="form-control no-icon" required>
                                <span class="form-hint">
                                    Ít nhất 6 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.
                                </span>
                            </div>
                        </div>
                    </c:if>

                    <div class="form-actions">
                        <button type="submit" class="btn btn-primary">
                            ${isNew ? 'Thêm nhân viên' : 'Lưu thay đổi'}
                        </button>
                        <a href="${pageContext.request.contextPath}/manager/staff" class="btn btn-outline">
                            Hủy
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </main>
</div>

<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>
