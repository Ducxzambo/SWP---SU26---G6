<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lịch Sử Của Tôi – PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <style>
        .toolbar { display:flex; gap:10px; align-items:flex-end; flex-wrap:wrap; margin-bottom:16px; }
        .shift-tabs { display:flex; gap:8px; margin-bottom:16px; flex-wrap:wrap; }
        .shift-tab  { padding:6px 14px; border-radius:20px; font-size:13px; font-weight:500;
            border:1.5px solid var(--border); background:#fff; cursor:pointer;
            text-decoration:none; color:var(--text-mid); transition:var(--transition); }
        .shift-tab:hover  { background:var(--teal-50); border-color:var(--teal-400); }
        .shift-tab.active { background:var(--teal-700); color:#fff; border-color:var(--teal-700); }
        .shift-tab .count { font-size:11px; opacity:.75; margin-left:4px; }
    </style>
</head>
<body>
<div class="layout">
    <aside class="sidebar">
        <div class="sidebar-logo">🐾 PetClinic</div>
        <nav>
            <a href="${pageContext.request.contextPath}/vet/examination" class="nav-item">Hàng chờ khám</a>
            <a href="${pageContext.request.contextPath}/vet/examination?action=history" class="nav-item active">Lịch sử</a>
        </nav>
        <div class="sidebar-user">
            👤 ${sessionScope.staff.fullName}
            <a href="${pageContext.request.contextPath}/auth/staff/logout" class="logout-link">Đăng xuất</a>
        </div>
    </aside>

    <main class="main-content">
        <div class="page-header">
            <h1>Lịch Sử Của Tôi</h1>
            <p class="page-sub">Các ca bạn đã lưu bệnh án xong (lịch hẹn có thể vẫn chờ nhân viên khác hoàn tất)</p>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-error"><span class="alert-icon">✕</span> ${error}</div>
        </c:if>

        <div class="toolbar">
            <form method="get" action="${pageContext.request.contextPath}/vet/examination"
                  style="display:flex;gap:8px;align-items:flex-end;">
                <input type="hidden" name="action" value="history">
                <div>
                    <label class="form-label">Ngày</label>
                    <input type="date" name="date" value="${filterDate}" class="form-control no-icon"
                           style="width:160px;" onchange="this.form.submit()">
                </div>
            </form>
            <c:if test="${!isToday}">
                <a href="${pageContext.request.contextPath}/vet/examination?action=history" class="btn btn-outline btn-sm" style="margin-bottom:1px;">
                    Về hôm nay
                </a>
            </c:if>
        </div>

        <div class="shift-tabs">
            <a href="${pageContext.request.contextPath}/vet/examination?action=history&date=${filterDate}"
               class="shift-tab ${empty shiftFilter ? 'active' : ''}">Tất cả ca</a>
            <a href="${pageContext.request.contextPath}/vet/examination?action=history&date=${filterDate}&shift=1"
               class="shift-tab ${shiftFilter == '1' ? 'active' : ''}">Ca 1 <span class="count">08:00–10:00</span></a>
            <a href="${pageContext.request.contextPath}/vet/examination?action=history&date=${filterDate}&shift=2"
               class="shift-tab ${shiftFilter == '2' ? 'active' : ''}">Ca 2 <span class="count">10:00–12:00</span></a>
            <a href="${pageContext.request.contextPath}/vet/examination?action=history&date=${filterDate}&shift=3"
               class="shift-tab ${shiftFilter == '3' ? 'active' : ''}">Ca 3 <span class="count">13:30–15:30</span></a>
            <a href="${pageContext.request.contextPath}/vet/examination?action=history&date=${filterDate}&shift=4"
               class="shift-tab ${shiftFilter == '4' ? 'active' : ''}">Ca 4 <span class="count">15:30–17:30</span></a>
        </div>

        <div class="card">
            <c:choose>
                <c:when test="${empty completed}">
                    <div class="empty-state">
                        <div class="empty-icon">📭</div>
                        <p>Chưa có ca khám nào bạn đã hoàn thành trong ngày này.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr><th>STT</th><th>Ca</th><th>Giờ hẹn</th><th>Tên chủ</th>
                            <th>Thú cưng</th><th>Dịch vụ</th><th>Thao tác</th></tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${completed}" var="appt" varStatus="loop">
                            <tr>
                                <td>${loop.count}</td>
                                <td><span class="badge badge-teal">Ca ${appt.slotShift}</span></td>
                                <td>${appt.startTime}</td>
                                <td><strong><c:out value="${appt.customerName}"/></strong></td>
                                <td><c:out value="${appt.petName}"/></td>
                                <td><c:out value="${appt.serviceNamesJoined}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty appt.recordID}">
                                            <a href="${pageContext.request.contextPath}/vet/examination?action=view&recordID=${appt.recordID}"
                                               class="btn btn-outline btn-sm">📋 Xem bệnh án</a>
                                        </c:when>
                                        <c:otherwise><span class="badge badge-neutral">Không có bệnh án</span></c:otherwise>
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