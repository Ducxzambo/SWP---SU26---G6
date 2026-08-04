<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="activeModule" value="attendance" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chấm Công Nhân Viên - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <style>
        .inline-status-form { display:flex; gap:6px; align-items:center; }
        .inline-status-form select { font-size:12.5px; padding:4px 6px; }
        .shift-tabs { display:flex; gap:8px; margin-bottom:16px; flex-wrap:wrap; }
        .shift-tab {
            padding:6px 14px; border-radius:20px; font-size:13px; font-weight:500;
            border:1.5px solid var(--border); background:#fff; cursor:pointer;
            text-decoration:none; color:var(--text-mid); transition:var(--transition);
        }
        .shift-tab:hover  { background:var(--teal-50); border-color:var(--teal-400); }
        .shift-tab.active { background:var(--teal-700); color:#fff; border-color:var(--teal-700); }
        .shift-tab .auto-tag { font-size:10.5px; opacity:.85; margin-left:4px; }
        .attendance-toolbar { display:flex; gap:10px; align-items:flex-end; flex-wrap:wrap; margin-bottom:14px; }
        .attendance-search { display:flex; gap:8px; align-items:flex-end; flex-wrap:wrap; }
        .row-hidden { display:none !important; }
        .nav-item-stats { display:inline-block; margin-top:16px; padding:8px 14px; border-radius:6px; background:#0a2e2a; color:#ffffff; text-decoration:none; transition:var(--transition); }
        .nav-item-stats:hover { display:inline-block; margin-top:16px; padding:8px 14px; border-radius:6px; background:#7dd4c8; color:#1a1714; text-decoration:none; transition:var(--transition); }
    </style>
</head>
<body>
<div class="layout">
    <c:set var="activeModule" value="attendance" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Chấm Công Nhân Viên</h1>
            <p class="page-sub">
                Check-in chỉ áp dụng cho các ca trong ngày và trước khi ca kết thúc.
            </p>
        </div>


        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>

        <div class="two-column-grid">

            <div class="card">
                <div class="card-header"><span class="card-title">Check-in theo ca (hôm nay ${today})</span></div>
                <div class="card-body">
                    <form action="${pageContext.request.contextPath}/manager/attendance" method="post">
                        <input type="hidden" name="date" value="${today}">
                        <div class="form-group">
                            <label class="form-label">Nhân viên <span class="required">*</span></label>
                            <select name="staffId" class="form-control no-icon" required>
                                <c:forEach items="${staffList}" var="s">
                                    <option value="${s.staffID}"><c:out value="${s.fullName}"/> (${s.roleName})</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Ca</label>
                            <select name="shift" class="form-control no-icon" required>
                                <option value="1" ${currentShift == 1 ? 'selected' : ''}>Ca 1 (08:00–10:00)</option>
                                <option value="2" ${currentShift == 2 ? 'selected' : ''}>Ca 2 (10:00–12:00)</option>
                                <option value="3" ${currentShift == 3 ? 'selected' : ''}>Ca 3 (13:30–15:30)</option>
                                <option value="4" ${currentShift == 4 ? 'selected' : ''}>Ca 4 (15:30–17:30)</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Ghi chú</label>
                            <input type="text" name="notes" class="form-control no-icon">
                        </div>
                        <div class="form-actions">
                            <button type="submit" class="btn btn-primary">Check-in</button>
                        </div>
                    </form>
                </div>
            </div>

            <div class="card">
                <div class="card-header"><span class="card-title">Đăng ký nghỉ dài hạn</span></div>
                <div class="card-body">
                    <form action="${pageContext.request.contextPath}/manager/attendance" method="post"
                          onsubmit="return confirm('Xác nhận đăng ký nghỉ cho nhân viên này trong khoảng ngày đã chọn?');">
                        <input type="hidden" name="action" value="markRange">
                        <div class="form-group">
                            <label class="form-label">Nhân viên <span class="required">*</span></label>
                            <select name="staffId" class="form-control no-icon" required>
                                <c:forEach items="${staffList}" var="s">
                                    <option value="${s.staffID}"><c:out value="${s.fullName}"/> (${s.roleName})</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="form-row col-2">
                            <div class="form-group">
                                <label class="form-label">Từ ngày</label>
                                <input type="date" name="fromDate" class="form-control no-icon"
                                       min="${minRangeStart}" value="${minRangeStart}" required>
                            </div>
                            <div class="form-group">
                                <label class="form-label">Đến ngày</label>
                                <input type="date" name="toDate" class="form-control no-icon"
                                       min="${minRangeStart}" required>
                            </div>
                        </div>
                        <div class="form-hint" style="margin-top:-8px;margin-bottom:14px;">
                            Chỉ được đăng ký nghỉ bắt đầu từ ngày mai (${minRangeStart}) trở đi.
                        </div>
                        <div class="form-group">
                            <label class="form-label">Loại nghỉ</label>
                            <select name="status" class="form-control no-icon">
                                <option value="OnLeave">Nghỉ phép</option>
                                <option value="Absent">Vắng không phép</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Ghi chú</label>
                            <input type="text" name="notes" class="form-control no-icon" placeholder="Lý do nghỉ...">
                        </div>
                        <div class="form-hint">
                            Sau khi lưu, bạn sẽ được chuyển sang mục "Sức chứa theo ca" với các ngày bị ảnh hưởng
                            được đánh dấu đỏ để kiểm tra và điều chỉnh GroomCap/VetCap ngay.
                        </div>
                        <div class="form-actions">
                            <button type="submit" class="btn btn-secondary">Đăng ký nghỉ</button>
                        </div>
                    </form>
                </div>
            </div>

        </div>

        <%-- ── Toolbar: chọn ngày + tìm theo tên (client-side) --%>
        <div class="attendance-toolbar">
            <form method="get" action="${pageContext.request.contextPath}/manager/attendance"
                  style="display:flex;gap:8px;align-items:flex-end;">
                <div>
                    <label class="form-label">Xem chấm công ngày</label>
                    <input type="date" name="date" value="${filterDate}" class="form-control no-icon"
                           style="width:180px;" onchange="this.form.submit()">
                </div>
                <c:if test="${not empty shiftFilter}"><input type="hidden" name="shift" value="${shiftFilter}"></c:if>
            </form>

            <div class="attendance-search">
                <label class="form-label" style="margin-bottom:0;">&nbsp;</label>
                <div class="input-wrap">
                    <span class="input-icon"></span>
                    <input type="text" id="staffSearchInput" class="form-control"
                           placeholder="Tìm theo tên nhân viên..." style="width:220px;">
                </div>
            </div>
        </div>

        <%-- Tab lọc theo ca - tự động chọn ca hiện tại khi xem hôm nay --%>
        <div class="shift-tabs">
            <a href="${pageContext.request.contextPath}/manager/attendance?date=${filterDate}&shift="
                     class="shift-tab ${empty shiftFilter ? 'active' : ''}">Tất cả ca</a>
            <a href="${pageContext.request.contextPath}/manager/attendance?date=${filterDate}&shift=1"
               class="shift-tab ${shiftFilter == '1' ? 'active' : ''}">
                Ca 1 (08:00–10:00)
                <c:if test="${shiftAutoApplied and shiftFilter == '1'}"><span class="auto-tag">(tự động)</span></c:if>
            </a>
            <a href="${pageContext.request.contextPath}/manager/attendance?date=${filterDate}&shift=2"
               class="shift-tab ${shiftFilter == '2' ? 'active' : ''}">
                Ca 2 (10:00–12:00)
                <c:if test="${shiftAutoApplied and shiftFilter == '2'}"><span class="auto-tag">(tự động)</span></c:if>
            </a>
            <a href="${pageContext.request.contextPath}/manager/attendance?date=${filterDate}&shift=3"
               class="shift-tab ${shiftFilter == '3' ? 'active' : ''}">
                Ca 3 (13:30–15:30)
                <c:if test="${shiftAutoApplied and shiftFilter == '3'}"><span class="auto-tag">(tự động)</span></c:if>
            </a>
            <a href="${pageContext.request.contextPath}/manager/attendance?date=${filterDate}&shift=4"
               class="shift-tab ${shiftFilter == '4' ? 'active' : ''}">
                Ca 4 (15:30–17:30)
                <c:if test="${shiftAutoApplied and shiftFilter == '4'}"><span class="auto-tag">(tự động)</span></c:if>
            </a>
        </div>
        <c:if test="${shiftAutoApplied}">
            <div class="alert alert-info" style="margin-bottom:16px;">
                Đang tự động lọc theo <strong>ca hiện tại</strong>. Bấm "Tất cả ca" để xem toàn bộ ngày.
            </div>
        </c:if>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Chấm công ngày ${filterDate}</span>
                <span class="text-soft" id="attendanceCount">${fn:length(attendance)} dòng</span>
            </div>
            <c:choose>
                <c:when test="${empty attendance}">
                    <div class="empty-state"><p>Không có dữ liệu chấm công phù hợp với bộ lọc hiện tại.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table" id="attendanceTable">
                        <thead>
                        <tr>
                            <th>Nhân viên</th>
                            <th>Vai trò</th>
                            <th>Ca</th>
                            <th>Trạng thái</th>
                            <th>Giờ check-in</th>
                            <th>Ghi chú</th>
                            <c:if test="${isToday}"><th>Sửa trạng thái</th></c:if>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${attendance}" var="a">
                            <tr data-staffname="${fn:toLowerCase(a.staffName)}">
                                <td><strong><c:out value="${a.staffName}"/></strong></td>
                                <td><span class="badge badge-neutral">${a.roleName}</span></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.wholeDay}">Cả ngày</c:when>
                                        <c:otherwise>Ca ${a.slotShift}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.status == 'Present'}"><span class="badge badge-success">Có mặt</span></c:when>
                                        <c:when test="${a.status == 'Late'}"><span class="badge badge-warning">Đi trễ</span></c:when>
                                        <c:when test="${a.status == 'Absent'}"><span class="badge badge-error">Vắng</span></c:when>
                                        <c:otherwise><span class="badge badge-info">Nghỉ phép</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${a.checkInTime}"/></td>
                                <td><c:out value="${a.notes}"/></td>
                                <c:if test="${isToday}">
                                    <td>
                                        <form action="${pageContext.request.contextPath}/manager/attendance"
                                              method="post" class="inline-status-form">
                                            <input type="hidden" name="action" value="updateStatus">
                                            <input type="hidden" name="attendanceId" value="${a.attendanceID}">
                                            <input type="hidden" name="date" value="${filterDate}">
                                            <select name="newStatus">
                                                <option value="Present" ${a.status == 'Present' ? 'selected' : ''}>Có mặt</option>
                                                <option value="Late" ${a.status == 'Late' ? 'selected' : ''}>Đi trễ</option>
                                                <option value="Absent" ${a.status == 'Absent' ? 'selected' : ''}>Vắng</option>
                                                <option value="OnLeave" ${a.status == 'OnLeave' ? 'selected' : ''}>Nghỉ phép</option>
                                            </select>
                                            <button type="submit" class="btn btn-outline btn-sm">Lưu</button>
                                        </form>
                                    </td>
                                </c:if>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>

        <a href="${pageContext.request.contextPath}/manager/attendance/statistics"
           class="nav-item-stats">Vi phạm chấm công</a>
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
<script>
    (function () {
        var searchInput = document.getElementById('staffSearchInput');
        var table = document.getElementById('attendanceTable');
        var countEl = document.getElementById('attendanceCount');
        if (!searchInput || !table) return;

        var rows = Array.from(table.querySelectorAll('tbody tr'));

        function applySearch() {
            var q = (searchInput.value || '').trim().toLowerCase();
            var visible = 0;
            rows.forEach(function (row) {
                var match = !q || (row.dataset.staffname || '').indexOf(q) !== -1;
                row.classList.toggle('row-hidden', !match);
                if (match) visible++;
            });
            if (countEl) countEl.textContent = visible + ' / ' + rows.length + ' dòng';
        }

        searchInput.addEventListener('input', applySearch);
    })();
</script>
</body>
</html>