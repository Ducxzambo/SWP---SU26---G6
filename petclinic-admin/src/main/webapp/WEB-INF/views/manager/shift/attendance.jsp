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
                Check-in. Chỉ áp dụng cho các ca trong ngày và trước khi ca kết thúc.
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
                                <input type="date" name="fromDate" class="form-control no-icon" required>
                            </div>
                            <div class="form-group">
                                <label class="form-label">Đến ngày</label>
                                <input type="date" name="toDate" class="form-control no-icon" required>
                            </div>
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

        <form action="${pageContext.request.contextPath}/manager/attendance" method="get"
              style="display:flex;gap:10px;align-items:flex-end;margin:20px 0;">
            <div>
                <label class="form-label">Xem chấm công ngày</label>
                <input type="date" name="date" value="${filterDate}" class="form-control no-icon"
                       style="width:180px;" onchange="this.form.submit()">
            </div>

        </form>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Chấm công ngày ${filterDate}</span>
                <span class="text-soft">${fn:length(attendance)} dòng</span>
            </div>
            <c:choose>
                <c:when test="${empty attendance}">
                    <div class="empty-state"><p>Chưa có dữ liệu chấm công cho ngày này.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
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
                            <tr>
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
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>