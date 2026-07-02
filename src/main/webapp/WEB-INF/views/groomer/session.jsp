<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Hàng Chờ Grooming – PetClinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
  <style>
    .shift-tabs{display:flex;gap:8px;margin-bottom:16px;flex-wrap:wrap}
    .shift-tab{padding:6px 14px;border-radius:20px;font-size:13px;font-weight:500;
      border:1.5px solid var(--border);background:#fff;cursor:pointer;
      text-decoration:none;color:var(--text-mid);transition:var(--transition)}
    .shift-tab:hover{background:var(--teal-50);border-color:var(--teal-400)}
    .shift-tab.active{background:var(--teal-700);color:#fff;border-color:var(--teal-700)}
    .unassigned-badge{background:#fff3cd;color:#856404;border:1px solid #ffc107;
      border-radius:12px;padding:2px 10px;font-size:12px;font-weight:500}
  </style>
</head>
<body>
<div class="layout">
  <aside class="sidebar">
    <div class="sidebar-logo">🐾 PetClinic</div>
    <nav>
      <a href="${pageContext.request.contextPath}/groomer/session" class="nav-item active">Hàng chờ</a>
    </nav>
    <div class="sidebar-user">
      ${sessionScope.staff.fullName}
      <a href="${pageContext.request.contextPath}/auth/logout" class="logout-link">Đăng xuất</a>
    </div>
  </aside>
  <main class="main-content">

    <div class="page-header">
      <h1>Hàng Chờ Grooming</h1>
      <p class="page-sub">Phiên chờ grooming và đang thực hiện</p>
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

    <%-- Date filter --%>
    <form method="get" action="${pageContext.request.contextPath}/groomer/session"
          style="display:flex;gap:8px;align-items:flex-end;margin-bottom:16px;">
      <div>
        <label class="form-label">Ngày</label>
        <input type="date" name="date" value="${filterDate}" class="form-control no-icon"
               style="width:160px;" onchange="this.form.submit()">
      </div>
      <c:if test="${!isToday}">
        <a href="${pageContext.request.contextPath}/groomer/session" class="btn btn-outline btn-sm" style="margin-bottom:1px;">
          Hôm nay
        </a>
      </c:if>
    </form>

    <%-- Shift tabs --%>
    <div class="shift-tabs">
      <a href="${pageContext.request.contextPath}/groomer/session?date=${filterDate}"
         class="shift-tab ${empty shiftFilter ? 'active' : ''}">Tất cả</a>
    </div>

    <div class="card">
      <c:choose>
        <c:when test="${empty queue}">
          <div class="empty-state"><div class="empty-icon">🎉</div>
            <p>Không có thú cưng nào đang chờ grooming.</p></div>
        </c:when>
        <c:otherwise>
          <table class="data-table">
            <thead>
            <tr><th>STT</th><th>Ca</th><th>Giờ</th><th>Chủ nhân</th>
              <th>Thú cưng</th><th>Dịch vụ</th><th>Trạng thái</th><th>Thao tác</th></tr>
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
                      <span class="badge badge-info"> Đang grooming</span>
                    </c:when>
                    <c:when test="${empty appt.assignedStaffID}">
                      <span class="unassigned-badge"> Chưa có groomer</span>
                    </c:when>
                    <c:otherwise>
                      <span class="badge badge-warning"> Chờ bắt đầu</span>
                    </c:otherwise>
                  </c:choose>
                </td>
                <td style="white-space:nowrap;">
                  <c:choose>
                    <%-- Unassigned: show Accept button --%>
                    <c:when test="${empty appt.assignedStaffID}">
                      <a href="${pageContext.request.contextPath}/groomer/session?action=accept&appointmentID=${appt.appointmentID}"
                         class="btn btn-secondary btn-sm"
                         onclick="return confirm('Nhận ca grooming cho ${appt.petName}?')">
                         Nhận ca
                      </a>
                    </c:when>
                    <%-- Assigned to me, not started --%>
                    <c:when test="${appt.status == 'Arrived'}">
                      <a href="${pageContext.request.contextPath}/groomer/session?action=start&appointmentID=${appt.appointmentID}"
                         class="btn btn-primary btn-sm"
                         onclick="return confirm('Bắt đầu grooming cho ${appt.petName}?')">
                         Bắt đầu
                      </a>
                    </c:when>
                    <%-- InProgress --%>
                    <c:when test="${appt.status == 'InProgress'}">
                      <a href="${pageContext.request.contextPath}/groomer/session?action=form&appointmentID=${appt.appointmentID}"
                         class="btn btn-secondary btn-sm"> Tiếp tục</a>
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