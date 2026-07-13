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
      👤 ${sessionScope.staff.fullName}
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

    <div class="shift-tabs">
      <a href="${pageContext.request.contextPath}/groomer/session?date=${filterDate}"
         class="shift-tab ${empty shiftFilter ? 'active' : ''}">Tất cả</a>
      <a href="${pageContext.request.contextPath}/groomer/session?date=${filterDate}&shift=1"
         class="shift-tab ${shiftFilter == '1' ? 'active' : ''}">Ca 1</a>
      <a href="${pageContext.request.contextPath}/groomer/session?date=${filterDate}&shift=2"
         class="shift-tab ${shiftFilter == '2' ? 'active' : ''}">Ca 2</a>
      <a href="${pageContext.request.contextPath}/groomer/session?date=${filterDate}&shift=3"
         class="shift-tab ${shiftFilter == '3' ? 'active' : ''}">Ca 3</a>
      <a href="${pageContext.request.contextPath}/groomer/session?date=${filterDate}&shift=4"
         class="shift-tab ${shiftFilter == '4' ? 'active' : ''}">Ca 4</a>
    </div>

    <div class="card">
      <c:choose>
        <c:when test="${empty queue}">
          <div class="empty-state"><div class="empty-icon"></div>
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
              <%-- unassigned = còn ít nhất 1 dịch vụ Grooming trong appointment này chưa gán staff --%>
              <c:set var="isUnassigned" value="${appt.hasUnassignedServiceInCategory('Grooming')}"/>
              <tr class="${appt.status == 'InProgress' ? 'row-inprogress' : ''}">
                <td>${loop.count}</td>
                <td><span class="badge badge-teal">Ca ${appt.slotShift}</span></td>
                <td>${appt.startTime}</td>
                <td><strong><c:out value="${appt.customerName}"/></strong></td>
                <td><c:out value="${appt.petName}"/></td>
                <td><c:out value="${appt.serviceNamesJoined}"/></td>
                <td>
                  <c:choose>
                    <c:when test="${appt.status == 'InProgress'}">
                      <span class="badge badge-info">Đang grooming</span>
                    </c:when>
                    <c:when test="${isUnassigned}">
                      <span class="unassigned-badge">Chờ nhận ca</span>
                    </c:when>
                    <c:otherwise>
                      <span class="badge badge-warning">Chờ bắt đầu</span>
                    </c:otherwise>
                  </c:choose>
                </td>
                <td style="white-space:nowrap;">
                  <c:choose>
                    <%-- Arrived (dù đã gán hay chưa) → 1 nút Bắt đầu, startSession() tự lo phần accept --%>
                    <c:when test="${appt.status == 'Arrived'}">
                      <a href="${pageContext.request.contextPath}/groomer/session?action=start&appointmentID=${appt.appointmentID}"
                         class="btn btn-primary btn-sm"
                         onclick="return confirm('Bắt đầu grooming cho ${appt.petName}?')">
                        Bắt đầu
                      </a>
                    </c:when>
                    <c:when test="${appt.status == 'InProgress'}">
                      <a href="${pageContext.request.contextPath}/groomer/session?action=form&appointmentID=${appt.appointmentID}"
                         class="btn btn-secondary btn-sm">Tiếp tục</a>
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

    <%-- Đã hoàn thành hôm nay --%>
    <details class="card" style="margin-top:20px;padding:0;">
      <summary style="cursor:pointer;padding:14px 20px;font-weight:600;color:var(--teal-800);
                    list-style:none;display:flex;align-items:center;gap:8px;">
        Đã hoàn thành hôm nay
        <c:if test="${not empty completed}"><span class="badge badge-success">${completed.size()}</span></c:if>
      </summary>
      <div style="padding:0 0 8px;">
        <c:choose>
          <c:when test="${empty completed}">
            <div class="empty-state" style="padding:32px 24px;">
              <div class="empty-icon"></div>
              <p>Chưa có phiên grooming nào hoàn thành.</p>
            </div>
          </c:when>
          <c:otherwise>
            <table class="data-table">
              <thead>
              <tr><th>STT</th><th>Ca</th><th>Giờ</th><th>Chủ nhân</th>
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
                        <a href="${pageContext.request.contextPath}/groomer/session?action=view&recordID=${appt.recordID}"
                           class="btn btn-outline btn-sm">Xem kết quả</a>
                      </c:when>
                      <c:otherwise><span class="badge badge-neutral">Không có bản ghi</span></c:otherwise>
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