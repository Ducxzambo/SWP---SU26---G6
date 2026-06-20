<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Check-in – PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <style>
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

        .slot-bar {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 20px;
            background: var(--teal-50);
            border: 1px solid var(--teal-100);
            border-radius: 8px;
            padding: 10px 16px;
        }

        .slot-bar .label {
            font-size: 13px;
            color: var(--teal-700);
            font-weight: 500;
        }

        .slot-bar .count {
            font-size: 20px;
            font-weight: 700;
            color: var(--teal-800);
        }

        .slot-bar .full {
            color: var(--red-400);
        }

        .modal-backdrop {
            display: none;
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, .45);
            z-index: 1000;
            align-items: center;
            justify-content: center;
        }

        .modal-backdrop.open {
            display: flex;
        }

        .modal {
            background: #fff;
            border-radius: 12px;
            padding: 28px 32px;
            width: 100%;
            max-width: 480px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, .2);
            animation: slideUp .25s ease both;
        }

        @keyframes slideUp {
            from {
                opacity: 0;
                transform: translateY(16px)
            }
            to {
                opacity: 1;
                transform: none
            }
        }

        .modal h3 {
            font-size: 17px;
            font-weight: 700;
            color: var(--teal-800);
            margin-bottom: 16px;
        }

        .modal-footer {
            display: flex;
            gap: 10px;
            justify-content: flex-end;
            margin-top: 20px;
        }

        .toolbar {
            display: flex;
            gap: 10px;
            align-items: flex-end;
            flex-wrap: wrap;
            margin-bottom: 16px;
        }
    </style>
</head>
<body>
<div class="layout">
    <aside class="sidebar">
        <div class="sidebar-logo">🐾 PetClinic</div>
        <nav>
            <a href="${pageContext.request.contextPath}/receptionist/checkin" class="nav-item active">✅ Check-in</a>
        </nav>
        <div class="sidebar-user">
            ${sessionScope.staff.fullName}
            <a href="${pageContext.request.contextPath}/auth/logout" class="logout-link">Đăng xuất</a>
        </div>
    </aside>

    <main class="main-content">
        <div class="page-header">
            <h1>Check-in Bệnh Nhân</h1>
            <p class="page-sub">Xác nhận thú cưng đã đến phòng khám</p>
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
            <form method="get" action="${pageContext.request.contextPath}/receptionist/checkin"
                  style="display:flex;gap:8px;align-items:flex-end;">
                <div>
                    <label class="form-label">Ngày</label>
                    <input type="date" name="date" value="${filterDate}" class="form-control no-icon"
                           style="width:160px;" onchange="this.form.submit()">
                </div>
                <c:if test="${not empty keyword}"><input type="hidden" name="q" value="${keyword}"></c:if>
                <c:if test="${not empty shiftFilter}"><input type="hidden" name="shift" value="${shiftFilter}"></c:if>
            </form>

            <form method="get" action="${pageContext.request.contextPath}/receptionist/checkin"
                  style="display:flex;gap:8px;align-items:flex-end;">
                <input type="hidden" name="date" value="${filterDate}">
                <c:if test="${not empty shiftFilter}"><input type="hidden" name="shift" value="${shiftFilter}"></c:if>
                <div>
                    <label class="form-label">Tìm kiếm</label>
                    <div class="input-wrap" style="width:240px;">
                        <input type="text" name="q" class="form-control" placeholder="Tên chủ / thú cưng..."
                               value="<c:out value='${keyword}'/>">
                    </div>
                </div>
                <button type="submit" class="btn btn-secondary">Tìm</button>
                <c:if test="${not empty keyword}">
                    <a href="${pageContext.request.contextPath}/receptionist/checkin?date=${filterDate}"
                       class="btn btn-outline">Xóa</a>
                </c:if>
            </form>

            <div style="margin-left:auto;">
                <label class="form-label">&nbsp;</label>
                <button type="button" class="btn btn-primary" onclick="openWalkIn()">Thêm Lịch Hẹn</button>
            </div>
        </div>

        <%-- Shift tabs --%>
        <div class="shift-tabs">
            <a href="${pageContext.request.contextPath}/receptionist/checkin?date=${filterDate}<c:if test='${not empty keyword}'>&q=${keyword}</c:if>"
               class="shift-tab ${empty shiftFilter ? 'active' : ''}">Tất cả ca</a>
        </div>

        <%-- Slot status bar (today only) --%>
        <c:if test="${isToday}">
            <div class="slot-bar">
                <span class="label">Ca hiện tại (${currentShiftLabel}):</span>
                <span class="count ${currentSlotCount >= 10 ? 'full' : ''}">${currentSlotCount}/10 pets</span>
                <c:choose>
                    <c:when test="${currentSlotCount >= 10}">
                        <span class="badge badge-error">Đã đầy ca</span>
                    </c:when>
                    <c:otherwise>
                        <span class="badge badge-success">Còn chỗ</span>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <%-- Table --%>
        <div class="card">
            <c:choose>
                <c:when test="${empty appointments}">
                    <div class="empty-state">
                        <div class="empty-icon">📋</div>
                        <p>Không có lịch hẹn nào cần check-in.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Ca</th>
                            <th>Giờ hẹn</th>
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
                                <td><span class="badge badge-teal">Ca ${appt.slotShift}</span></td>
                                <td>${appt.startTime}</td>
                                <td><strong><c:out value="${appt.customerName}"/></strong></td>
                                <td><c:out value="${appt.petName}"/></td>
                                <td><c:out value="${appt.serviceName}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty appt.vetName}"><c:out
                                                value="${appt.vetName}"/></c:when>
                                        <c:otherwise><span
                                                class="badge badge-warning">Chưa phân công</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <form action="${pageContext.request.contextPath}/receptionist/checkin" method="post"
                                          onsubmit="return confirm('Check-in cho ${appt.petName}?')">
                                        <input type="hidden" name="appointmentID" value="${appt.appointmentID}">
                                        <c:if test="${not empty appt.assignedVetID}">
                                            <input type="hidden" name="vetID" value="${appt.assignedVetID}">
                                        </c:if>
                                        <button type="submit" class="btn btn-primary btn-sm">Check-in</button>
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

<%-- Walk-in modal --%>
<div class="modal-backdrop" id="walkInModal" onclick="if(event.target===this)closeWalkIn()">
    <div class="modal">
        <h3>Tạo Lịch Hẹn</h3>
        <c:if test="${currentSlotFull}">
            <div class="alert alert-error" style="margin-bottom:12px;">
                <span class="alert-icon">⚠</span> Ca hiện tại đã đủ 10 thú cưng. Không thể nhận thêm.
            </div>
        </c:if>
        <form action="${pageContext.request.contextPath}/receptionist/checkin" method="post" id="walkInForm">
            <input type="hidden" name="action" value="walkin">
            <div class="form-row col-2">
                <div class="form-group">
                    <label class="form-label">CustomerID <span class="required">*</span></label>
                    <input type="number" name="customerID" class="form-control no-icon" min="1" required>
                </div>
                <div class="form-group">
                    <label class="form-label">PetID <span class="required">*</span></label>
                    <input type="number" name="petID" class="form-control no-icon" min="1" required>
                </div>
            </div>
            <div class="form-group">
                <label class="form-label">ServiceID <span class="required">*</span></label>
                <input type="number" name="serviceID" class="form-control no-icon" min="1" value="1" required>
                <div class="form-hint">Khám tổng quát = 1</div>
            </div>
            <div class="form-group">
                <label class="form-label">Bác sĩ phụ trách <span class="required">*</span></label>
                <select name="vetID" class="form-control" required>
                    <option value="">— Chọn bác sĩ —</option>
                    <c:forEach items="${vets}" var="vet">
                        <option value="${vet.staffID}"><c:out value="${vet.fullName}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline" onclick="closeWalkIn()">Hủy</button>
                <button type="submit" class="btn btn-primary" ${currentSlotFull ? 'disabled' : ''}>Tạo & Check-in
                </button>
            </div>
        </form>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
<script>
    function openWalkIn() {
        document.getElementById('walkInModal').classList.add('open');
    }

    function closeWalkIn() {
        document.getElementById('walkInModal').classList.remove('open');
    }
</script>
</body>
</html>
