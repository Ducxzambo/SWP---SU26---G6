<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Check-in Grooming – PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <style>
        .shift-tabs{display:flex;gap:8px;margin-bottom:16px;flex-wrap:wrap}
        .shift-tab{padding:6px 14px;border-radius:20px;font-size:13px;font-weight:500;
            border:1.5px solid var(--border);background:#fff;cursor:pointer;
            text-decoration:none;color:var(--text-mid);transition:var(--transition)}
        .shift-tab:hover{background:var(--teal-50);border-color:var(--teal-400)}
        .shift-tab.active{background:var(--teal-700);color:#fff;border-color:var(--teal-700)}
        .toolbar{display:flex;gap:10px;align-items:flex-end;flex-wrap:wrap;margin-bottom:16px}
    </style>
</head>
<body>
<div class="layout">
    <aside class="sidebar">
        <div class="sidebar-logo">🐾 PetClinic</div>
        <nav>
            <a href="${pageContext.request.contextPath}/receptionist/checkin" class="nav-item">✅ Check-in Khám</a>
            <a href="${pageContext.request.contextPath}/receptionist/grooming-checkin" class="nav-item active">✂️ Check-in Grooming</a>
        </nav>
        <div class="sidebar-user">
            👤 ${sessionScope.staff.fullName}
            <a href="${pageContext.request.contextPath}/auth/logout" class="logout-link">Đăng xuất</a>
        </div>
    </aside>
    <main class="main-content">

        <div class="page-header">
            <h1>✂️ Check-in Grooming</h1>
            <p class="page-sub">Xác nhận thú cưng đến làm đẹp</p>
        </div>

        <c:if test="${not empty sessionScope.flashSuccess}">
            <div class="alert alert-success"><span class="alert-icon">✓</span> ${sessionScope.flashSuccess}</div>
            <c:remove var="flashSuccess" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.flashWarning}">
            <div class="alert alert-warning"><span class="alert-icon">⚠</span> ${sessionScope.flashWarning}</div>
            <c:remove var="flashWarning" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.flashError}">
            <div class="alert alert-error"><span class="alert-icon">✕</span> ${sessionScope.flashError}</div>
            <c:remove var="flashError" scope="session"/>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-error"><span class="alert-icon">✕</span> ${error}</div>
        </c:if>

        <%-- Toolbar --%>
        <div class="toolbar">
            <form method="get" action="${pageContext.request.contextPath}/receptionist/grooming-checkin"
                  style="display:flex;gap:8px;align-items:flex-end;">
                <div>
                    <label class="form-label">Ngày</label>
                    <input type="date" name="date" value="${filterDate}" class="form-control no-icon"
                           style="width:160px;" onchange="this.form.submit()">
                </div>
                <c:if test="${not empty shiftFilter}"><input type="hidden" name="shift" value="${shiftFilter}"></c:if>
            </form>
        </div>

        <%-- Shift tabs --%>
        <div class="shift-tabs">
            <a href="${pageContext.request.contextPath}/receptionist/grooming-checkin?date=${filterDate}"
               class="shift-tab ${empty shiftFilter ? 'active' : ''}">Tất cả ca</a>
            <a href="${pageContext.request.contextPath}/receptionist/grooming-checkin?date=${filterDate}&shift=1"
               class="shift-tab ${shiftFilter == '1' ? 'active' : ''}">Ca 1 08:00–10:00</a>
            <a href="${pageContext.request.contextPath}/receptionist/grooming-checkin?date=${filterDate}&shift=2"
               class="shift-tab ${shiftFilter == '2' ? 'active' : ''}">Ca 2 10:00–12:00</a>
            <a href="${pageContext.request.contextPath}/receptionist/grooming-checkin?date=${filterDate}&shift=3"
               class="shift-tab ${shiftFilter == '3' ? 'active' : ''}">Ca 3 13:30–15:30</a>
            <a href="${pageContext.request.contextPath}/receptionist/grooming-checkin?date=${filterDate}&shift=4"
               class="shift-tab ${shiftFilter == '4' ? 'active' : ''}">Ca 4 15:30–17:30</a>
        </div>

        <div class="card">
            <c:choose>
                <c:when test="${empty appointments}">
                    <div class="empty-state"><div class="empty-icon">✂️</div>
                        <p>Không có lịch grooming nào cần check-in.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr><th>#</th><th>Ca</th><th>Giờ hẹn</th><th>Tên chủ</th>
                            <th>Thú cưng</th><th>Dịch vụ</th><th>Groomer</th><th>Thao tác</th></tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${appointments}" var="appt" varStatus="loop">
                            <tr>
                                <td>${loop.count}</td>
                                <td><span class="badge badge-teal">Ca ${appt.slotShift}</span></td>
                                <td>${appt.startTime}</td>
                                <td><strong><c:out value="${appt.customerName}"/></strong></td>
                                <td><c:out value="${appt.petName}"/></td>
                                <td><c:out value="${appt.serviceName}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty appt.groomerName}"><c:out value="${appt.groomerName}"/></c:when>
                                        <c:otherwise><span class="badge badge-warning">Chưa phân công</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <form action="${pageContext.request.contextPath}/receptionist/grooming-checkin"
                                          method="post" style="display:flex;gap:6px;align-items:center;">
                                        <input type="hidden" name="appointmentID" value="${appt.appointmentID}">
                                        <select name="groomerID" class="form-control" style="width:160px;padding:5px 8px;font-size:13px;">
                                            <option value="">— Groomer (tùy chọn) —</option>
                                            <c:forEach items="${groomers}" var="g">
                                                <option value="${g.staffID}" ${appt.assignedGroomerID == g.staffID ? 'selected' : ''}>
                                                    <c:out value="${g.fullName}"/>
                                                </option>
                                            </c:forEach>
                                        </select>
                                        <button type="submit" class="btn btn-primary btn-sm"
                                                onclick="return confirm('Check-in grooming cho ${appt.petName}?')">
                                            ✅ Check-in
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