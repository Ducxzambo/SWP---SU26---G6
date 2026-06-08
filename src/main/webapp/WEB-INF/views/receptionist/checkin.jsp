<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Check-in Bệnh Nhân – PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<%-- ── Sidebar / nav placeholder ────────────────────────────────────────── --%>
<div class="layout">
    <aside class="sidebar">
        <div class="sidebar-logo"> PetClinic</div>
        <nav>
            <a href="${pageContext.request.contextPath}/receptionist/checkin" class="nav-item active">
                 Check-in
            </a>
            <a href="${pageContext.request.contextPath}/receptionist/appointments" class="nav-item">
                 Lịch hẹn
            </a>
        </nav>
        <div class="sidebar-user">
            👤 ${sessionScope.staff.fullName}
            <a href="${pageContext.request.contextPath}/auth/logout" class="logout-link">Đăng xuất</a>
        </div>
    </aside>

    <main class="main-content">

        <div class="page-header">
            <h1>Check-in Bệnh Nhân</h1>
            <p class="page-sub">Danh sách lịch hẹn hôm nay đang chờ check-in</p>
        </div>

        <%-- Flash messages --%>
        <c:if test="${not empty sessionScope.flashSuccess}">
            <div class="alert alert-success">
                <span class="alert-icon">✓</span> ${sessionScope.flashSuccess}
            </div>
            <c:remove var="flashSuccess" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.flashWarning}">
            <div class="alert alert-warning">
                <span class="alert-icon">⚠</span> ${sessionScope.flashWarning}
            </div>
            <c:remove var="flashWarning" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.flashError}">
            <div class="alert alert-error">
                <span class="alert-icon">✕</span> ${sessionScope.flashError}
            </div>
            <c:remove var="flashError" scope="session"/>
        </c:if>
        <c:if test="${not empty requestScope.error}">
            <div class="alert alert-error">
                <span class="alert-icon">✕</span> ${requestScope.error}
            </div>
        </c:if>

        <%-- Search bar --%>
        <form action="${pageContext.request.contextPath}/receptionist/checkin"
              method="get" class="search-bar">
            <div class="input-wrap">
                <span class="input-icon"></span>
                <input type="text" name="q" class="form-control"
                       placeholder="Tìm theo tên chủ hoặc tên thú cưng..."
                       value="<c:out value='${keyword}'/>">
            </div>
            <button type="submit" class="btn btn-secondary">Tìm kiếm</button>
            <c:if test="${not empty keyword}">
                <a href="${pageContext.request.contextPath}/receptionist/checkin"
                   class="btn btn-outline">Xem tất cả</a>
            </c:if>
        </form>

        <%-- Appointment table --%>
        <div class="card">
            <c:choose>
                <c:when test="${empty appointments}">
                    <div class="empty-state">
                        <div class="empty-icon"></div>
                        <p>Không có lịch hẹn nào cần check-in hôm nay.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Giờ</th>
                            <th>Tên chủ</th>
                            <th>Thú cưng</th>
                            <th>Dịch vụ</th>
                            <th>Bác sĩ</th>
                            <th>Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${appointments}" var="appt" varStatus="loop">
                            <tr>
                                <td>${loop.count}</td>
                                <td>
                                        ${appt.startTime}
                                </td>
                                <td><strong><c:out value="${appt.customerName}"/></strong></td>
                                <td><c:out value="${appt.petName}"/></td>
                                <td><c:out value="${appt.serviceName}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty appt.vetName}">
                                            <c:out value="${appt.vetName}"/>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-warning">Chưa phân công</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                        <%-- Check-in form (POST) --%>
                                    <form action="${pageContext.request.contextPath}/receptionist/checkin"
                                          method="post"
                                          onsubmit="return confirmCheckIn('${appt.customerName}', '${appt.petName}')">
                                        <input type="hidden" name="appointmentID" value="${appt.appointmentID}">
                                            <%-- vetID stays as-is unless receptionist changes it in a future select --%>
                                        <c:if test="${not empty appt.assignedVetID}">
                                            <input type="hidden" name="vetID" value="${appt.assignedVetID}">
                                        </c:if>
                                        <button type="submit" class="btn btn-primary btn-sm">
                                             Check-in
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

<script>
    function confirmCheckIn(ownerName, petName) {
        return confirm('Xác nhận check-in cho ' + petName + ' (Chủ: ' + ownerName + ')?');
    }
</script>
<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>
