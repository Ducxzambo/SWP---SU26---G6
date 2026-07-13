<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hàng Chờ Khám – PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <style>
        .toolbar {
            display: flex;
            gap: 10px;
            align-items: flex-end;
            flex-wrap: wrap;
            margin-bottom: 16px;
        }

        .shift-tabs {
            display: flex;
            gap: 8px;
            margin-bottom: 16px;
            flex-wrap: wrap;
        }

        .shift-tab {
            padding: 6px 14px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 500;
            border: 1.5px solid var(--border);
            background: #fff;
            cursor: pointer;
            text-decoration: none;
            color: var(--text-mid);
            transition: var(--transition);
        }

        .shift-tab:hover {
            background: var(--teal-50);
            border-color: var(--teal-400);
        }

        .shift-tab.active {
            background: var(--teal-700);
            color: #fff;
            border-color: var(--teal-700);
        }

        .shift-tab .count {
            font-size: 11px;
            opacity: .75;
            margin-left: 4px;
        }
    </style>
</head>
<body>
<div class="layout">
    <aside class="sidebar">
        <div class="sidebar-logo">🐾 PetClinic</div>
        <nav>
            <a href="${pageContext.request.contextPath}/vet/examination" class="nav-item active">Hàng chờ khám</a>
        </nav>
        <div class="sidebar-user">
             ${sessionScope.staff.fullName}
            <a href="${pageContext.request.contextPath}/auth/logout" class="logout-link">Đăng xuất</a>
        </div>
    </aside>

    <main class="main-content">
        <div class="page-header">
            <h1>Hàng Chờ Khám</h1>
            <p class="page-sub">Bệnh nhân đã check-in, chờ khám hoặc đang khám</p>
        </div>

        <c:if test="${not empty sessionScope.flashSuccess}">
            <div class="alert alert-success"><span class="alert-icon">✓</span> ${sessionScope.flashSuccess}</div>
            <c:remove var="flashSuccess" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.flashWarning}">
            <div class="alert alert-warning"><span class="alert-icon">⚠</span> ${sessionScope.flashWarning}</div>
            <c:remove var="flashWarning" scope="session"/>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-error"><span class="alert-icon">✕</span> ${error}</div>
        </c:if>

        <%-- Date + shift filter --%>
        <div class="toolbar">
            <form method="get" action="${pageContext.request.contextPath}/vet/examination"
                  style="display:flex;gap:8px;align-items:flex-end;">
                <div>
                    <label class="form-label">Ngày</label>
                    <input type="date" name="date" value="${filterDate}" class="form-control no-icon"
                           style="width:160px;" onchange="this.form.submit()">
                </div>
                <c:if test="${not empty shiftFilter}"><input type="hidden" name="shift" value="${shiftFilter}"></c:if>
            </form>
            <c:if test="${!isToday}">
                <a href="${pageContext.request.contextPath}/vet/examination" class="btn btn-outline btn-sm"
                   style="margin-bottom:1px;">
                    Về hôm nay
                </a>
            </c:if>
        </div>

        <div class="shift-tabs">
            <a href="${pageContext.request.contextPath}/vet/examination?date=${filterDate}"
               class="shift-tab ${empty shiftFilter ? 'active' : ''}">Tất cả ca</a>
            <a href="${pageContext.request.contextPath}/vet/examination?date=${filterDate}&shift=1"
               class="shift-tab ${shiftFilter == '1' ? 'active' : ''}">Ca 1 <span class="count">08:00–10:00</span></a>
            <a href="${pageContext.request.contextPath}/vet/examination?date=${filterDate}&shift=2"
               class="shift-tab ${shiftFilter == '2' ? 'active' : ''}">Ca 2 <span class="count">10:00–12:00</span></a>
            <a href="${pageContext.request.contextPath}/vet/examination?date=${filterDate}&shift=3"
               class="shift-tab ${shiftFilter == '3' ? 'active' : ''}">Ca 3 <span class="count">13:30–15:30</span></a>
            <a href="${pageContext.request.contextPath}/vet/examination?date=${filterDate}&shift=4"
               class="shift-tab ${shiftFilter == '4' ? 'active' : ''}">Ca 4 <span class="count">15:30–17:30</span></a>
        </div>

        <div class="card">
            <c:choose>
                <c:when test="${empty queue}">
                    <div class="empty-state">
                        <p>Không có bệnh nhân nào đang chờ hoặc đang khám.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>STT</th>
                            <th>Ca</th>
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
                                <td><span class="badge badge-teal">Ca ${appt.slotShift}</span></td>
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
                                            <a href="${pageContext.request.contextPath}/vet/examination?action=start&appointmentID=${appt.appointmentID}"
                                               class="btn btn-primary btn-sm"
                                               onclick="return confirm('Bat dau kham cho ${appt.petName}?')">
                                                 Bắt đầu khám
                                            </a>
                                        </c:when>
                                        <c:when test="${appt.status == 'InProgress'}">
                                            <a href="${pageContext.request.contextPath}/vet/examination?action=form&appointmentID=${appt.appointmentID}"
                                               class="btn btn-secondary btn-sm"> Tiếp tục khám</a>
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

        <details class="card" style="margin-top:20px;padding:0;">
            <summary style="cursor:pointer;padding:14px 20px;font-weight:600;color:var(--teal-800);
                    list-style:none;display:flex;align-items:center;gap:8px;">
                Đã hoàn thành hôm nay
                <c:if test="${not empty completed}">
                    <span class="badge badge-success">${completed.size()}</span>
                </c:if>
            </summary>
            <div style="padding:0 0 8px;">
                <c:choose>
                    <c:when test="${empty completed}">
                        <div class="empty-state" style="padding:32px 24px;">
                            <div class="empty-icon"></div>
                            <p>Chưa có ca khám nào hoàn thành.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <table class="data-table">
                            <thead>
                            <tr>
                                <th>STT</th><th>Ca</th><th>Giờ hẹn</th><th>Tên chủ</th>
                                <th>Thú cưng</th><th>Dịch vụ</th><th>Thao tác</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${completed}" var="appt" varStatus="loop">
                                <tr>
                                    <td>${loop.count}</td>
                                    <td><span class="badge badge-teal">Ca ${appt.slotShift}</span></td>
                                    <td>${appt.startTime}</td>
                                    <td><strong><c:out value="${appt.customerName}"/></strong></td>
                                    <td><c:out value="${appt.petName}"/></td>
                                    <td><c:out value="${appt.serviceName}"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty appt.recordID}">
                                                <a href="${pageContext.request.contextPath}/vet/examination?action=view&recordID=${appt.recordID}"
                                                   class="btn btn-outline btn-sm">Xem bệnh án</a>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-neutral">Không có bệnh án</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>
            </div>
        </details>
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>
