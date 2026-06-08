<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hàng Chờ Khám – PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <aside class="sidebar">
        <div class="sidebar-logo">PetClinic</div>
        <nav>
            <a href="${pageContext.request.contextPath}/vet/examination" class="nav-item active">
                Hàng chờ khám
            </a>
        </nav>
        <div class="sidebar-user">
             ${sessionScope.staff.fullName}
            <a href="${pageContext.request.contextPath}/auth/logout" class="logout-link">Đăng xuất</a>
        </div>
    </aside>

    <main class="main-content">

        <div class="page-header">
            <h1>Hàng Chờ Khám Hôm Nay</h1>
            <p class="page-sub">Bệnh nhân đã check-in, chờ khám hoặc đang khám</p>
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
        <c:if test="${not empty requestScope.error}">
            <div class="alert alert-error">
                <span class="alert-icon">✕</span> ${requestScope.error}
            </div>
        </c:if>

        <%-- Queue --%>
        <div class="card">
            <c:choose>
                <c:when test="${empty queue}">
                    <div class="empty-state">
                        <div class="empty-icon"></div>
                        <p>Không có bệnh nhân nào đang chờ hoặc đang khám.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>STT</th>
                            <th>Giờ hẹn</th>
                            <th>Tên chủ</th>
                            <th>Thú cưng</th>
                            <th>Dịch vụ</th>
                            <th>Trạng thái</th>
                            <th>Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${queue}" var="appt" varStatus="loop">
                            <tr class="${appt.status == 'InProgress' ? 'row-inprogress' : ''}">
                                <td>${loop.count}</td>
                                <td>${appt.startTime}</td>
                                <td><strong><c:out value="${appt.customerName}"/></strong></td>
                                <td><c:out value="${appt.petName}"/></td>
                                <td><c:out value="${appt.serviceName}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${appt.status == 'InProgress'}">
                                            <span class="badge badge-info"> Đang khám</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-warning"> Chờ khám</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${appt.status == 'Arrived'}">
                                            <%-- Start examination: Arrived → InProgress --%>
                                            <a href="${pageContext.request.contextPath}/vet/examination?action=start&appointmentID=${appt.appointmentID}"
                                               class="btn btn-primary btn-sm"
                                               onclick="return confirm('Bắt đầu khám cho ${appt.petName}?')">Bắt đầu khám
                                            </a>
                                        </c:when>
                                        <c:when test="${appt.status == 'InProgress'}">
                                            <%-- Continue: go to examination form --%>
                                            <a href="${pageContext.request.contextPath}/vet/examination?action=form&appointmentID=${appt.appointmentID}"
                                               class="btn btn-secondary btn-sm">Tiếp tục khám
                                            </a>
                                        </c:when>
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
