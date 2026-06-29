<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Check-in – PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <style>
        .shift-tabs { display:flex; gap:8px; margin-bottom:16px; flex-wrap:wrap; }
        .shift-tab  { padding:6px 14px; border-radius:20px; font-size:13px; font-weight:500;
            border:1.5px solid var(--border); background:#fff; cursor:pointer;
            text-decoration:none; color:var(--text-mid); transition:var(--transition); }
        .shift-tab:hover  { background:var(--teal-50); border-color:var(--teal-400); }
        .shift-tab.active { background:var(--teal-700); color:#fff; border-color:var(--teal-700); }
        .shift-tab .count { font-size:11px; opacity:.75; margin-left:4px; }
        .slot-bar { display:flex; align-items:center; gap:10px; margin-bottom:20px;
            background:var(--teal-50); border:1px solid var(--teal-100); border-radius:8px; padding:10px 16px; }
        .slot-bar .label { font-size:13px; color:var(--teal-700); font-weight:500; }
        .slot-bar .count { font-size:20px; font-weight:700; color:var(--teal-800); }
        .slot-bar .full  { color:var(--red-400); }
        .modal-backdrop { display:none; position:fixed; inset:0; background:rgba(0,0,0,.45);
            z-index:1000; align-items:center; justify-content:center; }
        .modal-backdrop.open { display:flex; }
        .modal { background:#fff; border-radius:12px; padding:28px 32px; width:100%;
            max-width:520px; max-height:88vh; overflow-y:auto;
            box-shadow:0 20px 60px rgba(0,0,0,.2); animation:slideUp .25s ease both; }
        @keyframes slideUp { from{opacity:0;transform:translateY(16px)} to{opacity:1;transform:none} }
        .modal h3 { font-size:17px; font-weight:700; color:var(--teal-800); margin-bottom:6px; }
        .modal .step-label { font-size:12px; color:var(--text-soft); margin-bottom:16px; }
        .modal-footer { display:flex; gap:10px; justify-content:flex-end; margin-top:20px; }
        .toolbar { display:flex; gap:10px; align-items:flex-end; flex-wrap:wrap; margin-bottom:16px; }

        .customer-found-box {
            background:var(--green-100); border:1.5px solid #bbf7d0; border-radius:10px;
            padding:14px 16px; margin-bottom:16px;
        }
        .customer-found-box .name { font-weight:700; color:#15803d; font-size:15px; }
        .customer-found-box .phone { font-size:13px; color:#15803d; opacity:.85; }

        .customer-new-box {
            background:#fff8e1; border:1.5px solid #ffe082; border-radius:10px;
            padding:14px 16px; margin-bottom:16px; font-size:13px; color:#8a6d00;
        }

        .pet-radio-list { display:flex; flex-direction:column; gap:8px; margin-bottom:8px; }
        .pet-radio-item {
            display:flex; align-items:center; gap:10px; border:1.5px solid var(--border);
            border-radius:8px; padding:10px 12px; cursor:pointer; transition:var(--transition);
        }
        .pet-radio-item:hover { border-color:var(--teal-400); }
        .pet-radio-item input[type=radio] { accent-color:var(--teal-500); cursor:pointer; }
        .pet-radio-item .pet-info { font-size:14px; }
        .pet-radio-item .pet-info .name { font-weight:600; color:var(--teal-800); }
        .pet-radio-item .pet-info .sub  { font-size:12.5px; color:var(--text-soft); }

        .new-pet-toggle {
            font-size:13px; color:var(--teal-500); cursor:pointer; text-decoration:underline;
            margin-top:4px; display:inline-block;
        }
    </style>
</head>
<body>
<div class="layout">
    <aside class="sidebar">
        <div class="sidebar-logo">🐾 PetClinic</div>
        <nav>
            <a href="${pageContext.request.contextPath}/receptionist/checkin" class="nav-item active">Check-in</a>
        </nav>
        <div class="sidebar-user">
            👤 ${sessionScope.staff.fullName}
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
                        <span class="input-icon"></span>
                        <input type="text" name="q" class="form-control" placeholder="Tên chủ / thú cưng..."
                               value="<c:out value='${keyword}'/>">
                    </div>
                </div>
                <button type="submit" class="btn btn-secondary">Tìm</button>
                <c:if test="${not empty keyword}">
                    <a href="${pageContext.request.contextPath}/receptionist/checkin?date=${filterDate}" class="btn btn-outline">Xóa</a>
                </c:if>
            </form>

            <div style="margin-left:auto;">
                <label class="form-label">&nbsp;</label>
                <button type="button" class="btn btn-primary" onclick="openWalkIn()">Thêm lịch hẹn</button>
            </div>
        </div>

        <%-- Shift tabs --%>
        <div class="shift-tabs">
            <a href="${pageContext.request.contextPath}/receptionist/checkin?date=${filterDate}<c:if test='${not empty keyword}'>&q=${keyword}</c:if>"
               class="shift-tab ${empty shiftFilter ? 'active' : ''}">Tất cả ca</a>

        </div>

        <%-- Slot status bar --%>
        <c:if test="${isToday}">
            <div class="slot-bar">
                <span class="label">Ca hiện tại (${currentShiftLabel}):</span>
                <span class="count ${currentSlotCount >= 10 ? 'full' : ''}">${currentSlotCount}/10 pets</span>
                <c:choose>
                    <c:when test="${currentSlotCount >= 10}"><span class="badge badge-error">Đã đầy ca</span></c:when>
                    <c:otherwise><span class="badge badge-success">Còn chỗ</span></c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <%-- Table --%>
        <div class="card">
            <c:choose>
                <c:when test="${empty appointments}">
                    <div class="empty-state"><div class="empty-icon">📋</div><p>Không có lịch hẹn nào cần check-in.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr><th>#</th><th>Ca</th><th>Giờ hẹn</th><th>Tên chủ</th>
                            <th>Thú cưng</th><th>Dịch vụ</th><th>Bác sĩ</th><th>Thao tác</th></tr>
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
                                        <c:when test="${not empty appt.vetName}"><c:out value="${appt.vetName}"/></c:when>
                                        <c:otherwise><span class="badge badge-warning">Chưa phân công</span></c:otherwise>
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

<%-- ═══════════════════════════════════════════════════════════════════════════
     WALK-IN MODAL — 2 bước: (1) tra SĐT, (2) hiện form theo kết quả tra cứu
     ═══════════════════════════════════════════════════════════════════════════ --%>
<div class="modal-backdrop ${walkInStep == 2 ? 'open' : ''}" id="walkInModal"
     onclick="if(event.target===this)closeWalkIn()">
    <div class="modal">

        <c:if test="${not empty walkInError}">
            <div class="alert alert-error" style="margin-bottom:14px;">
                <span class="alert-icon">✕</span> ${walkInError}
            </div>
        </c:if>

        <%-- ── BƯỚC 1: nhập số điện thoại để tra cứu ──────────────────────────── --%>
        <c:if test="${walkInStep != 2}">
            <h3>Thêm lịch hẹn</h3>
            <p class="step-label">Bước 1/2 — Nhập số điện thoại để tra cứu</p>

            <form action="${pageContext.request.contextPath}/receptionist/checkin" method="post">
                <input type="hidden" name="action" value="walkinLookup">
                <div class="form-group">
                    <label class="form-label">Số điện thoại khách <span class="required">*</span></label>
                    <input type="tel" name="phone" class="form-control no-icon" autofocus
                           placeholder="VD: 0912345678" pattern="[0-9]{9,11}" required>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline" onclick="closeWalkIn()">Hủy</button>
                    <button type="submit" class="btn btn-primary">Tra cứu →</button>
                </div>
            </form>
        </c:if>

        <%-- ── BƯỚC 2: hiện form tương ứng tùy có/không tìm thấy khách ─────────── --%>
        <c:if test="${walkInStep == 2}">
            <h3>Thêm lịch hẹn</h3>
            <p class="step-label">Bước 2/2 — <c:out value="${walkInPhone}"/></p>

            <form action="${pageContext.request.contextPath}/receptionist/checkin" method="post" id="walkInForm">
                <input type="hidden" name="action" value="walkinSubmit">
                <input type="hidden" name="phone" value="<c:out value='${walkInPhone}'/>">

                    <%-- ═══ TRƯỜNG HỢP A: Khách đã tồn tại ═══ --%>
                <c:if test="${not empty walkInCustomer}">
                    <input type="hidden" name="customerID" value="${walkInCustomer.customerID}">

                    <div class="customer-found-box">
                        <div class="name">✓ Tìm thấy khách hàng</div>
                        <div class="phone"><c:out value="${walkInCustomer.fullName}"/> — <c:out value="${walkInPhone}"/></div>
                    </div>

                    <c:choose>
                        <c:when test="${not empty walkInPets}">
                            <div class="form-group">
                                <label class="form-label">Chọn thú cưng <span class="required">*</span></label>
                                <div class="pet-radio-list" id="petRadioList">
                                    <c:forEach items="${walkInPets}" var="pet">
                                        <label class="pet-radio-item">
                                            <input type="radio" name="petID" value="${pet.petID}"
                                                   onclick="hideNewPetFields()" required>
                                            <div class="pet-info">
                                                <div class="name"><c:out value="${pet.name}"/></div>
                                                <div class="sub"><c:out value="${pet.speciesName}"/> — <c:out value="${pet.breedName}"/></div>
                                            </div>
                                        </label>
                                    </c:forEach>
                                </div>
                                <span class="new-pet-toggle" onclick="showNewPetFields()">+ Khách mang thú cưng MỚI (chưa có trong hệ thống)</span>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="customer-new-box">
                                Khách hàng này chưa có thú cưng nào trong hệ thống. Vui lòng nhập thông tin thú cưng mới bên dưới.
                            </div>
                        </c:otherwise>
                    </c:choose>

                    <%-- Khối nhập pet mới — ẩn/hiện bằng CSS --%>
                    <div id="newPetFields" style="${empty walkInPets ? '' : 'display:none;'}">
                        <div class="form-group">
                            <label class="form-label">Tên thú cưng mới <span class="required">*</span></label>
                            <input type="text" name="petName" class="form-control no-icon" placeholder="VD: Mèo Mun">
                        </div>
                        <div class="form-row col-2">
                            <div class="form-group">
                                <label class="form-label">Loài</label>
                                <input type="text" name="species" class="form-control no-icon" placeholder="VD: Chó, Mèo">
                            </div>
                            <div class="form-group">
                                <label class="form-label">Giống</label>
                                <input type="text" name="breed" class="form-control no-icon" placeholder="VD: Poodle">
                            </div>
                        </div>
                    </div>
                </c:if>

                    <%-- ═══ TRƯỜNG HỢP B: Khách MỚI hoàn toàn ═══ --%>
                <c:if test="${empty walkInCustomer}">
                    <div class="customer-new-box">
                        Số điện thoại này chưa có trong hệ thống. Vui lòng nhập thông tin khách và thú cưng mới.
                    </div>

                    <div class="form-group">
                        <label class="form-label">Họ và tên khách <span class="required">*</span></label>
                        <input type="text" name="fullName" class="form-control no-icon" placeholder="VD: Nguyễn Văn A" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Tên thú cưng <span class="required">*</span></label>
                        <input type="text" name="petName" class="form-control no-icon" placeholder="VD: Mochi" required>
                    </div>
                    <div class="form-row col-2">
                        <div class="form-group">
                            <label class="form-label">Loài</label>
                            <input type="text" name="species" class="form-control no-icon" placeholder="VD: Chó, Mèo">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Giống</label>
                            <input type="text" name="breed" class="form-control no-icon" placeholder="VD: Poodle">
                        </div>
                    </div>
                </c:if>

                    <%-- ═══ Chung cho cả 2 trường hợp: Dịch vụ + Bác sĩ ═══ --%>
                <div class="form-group">
                    <label class="form-label">Dịch vụ <span class="required">*</span></label>
                    <select name="serviceID" class="form-control" required>
                        <option value="">— Chọn dịch vụ —</option>
                        <c:forEach items="${services}" var="svc">
                            <option value="${svc.serviceID}">
                                <c:out value="${svc.name}"/>
                                <c:if test="${svc.price > 0}"> — <fmt:formatNumber value="${svc.price}" type="number" groupingUsed="true"/>đ</c:if>
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-group">
                    <label class="form-label">Phân công bác sĩ <span class="required">*</span></label>
                    <select name="vetID" class="form-control" required>
                        <option value="">— Chọn bác sĩ —</option>
                        <c:forEach items="${staffs}" var="staff">
                            <option value="${staff.staffID}"><c:out value="${staff.fullName}"/></option>
                        </c:forEach>
                    </select>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-outline" onclick="closeWalkIn()">Hủy</button>
                    <button type="button" class="btn btn-outline" onclick="backToStep1()">← Đổi SĐT</button>
                    <button type="submit" class="btn btn-primary" ${currentSlotFull ? 'disabled' : ''}>
                        Tạo & Check-in
                    </button>
                </div>
            </form>
        </c:if>

    </div>
</div>

<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
<script>
    function openWalkIn()  { document.getElementById('walkInModal').classList.add('open'); }
    function closeWalkIn() { document.getElementById('walkInModal').classList.remove('open'); }

    function showNewPetFields() {
        document.getElementById('newPetFields').style.display = '';
        document.querySelectorAll('#petRadioList input[type=radio]').forEach(r => r.checked = false);
    }
    function hideNewPetFields() {
        document.getElementById('newPetFields').style.display = 'none';
    }

    function backToStep1() {
        // Quay lại bước 1: submit 1 form rỗng action=walkinLookup không kèm phone
        // Đơn giản nhất: load lại trang để bắt đầu modal từ đầu
        window.location.href = '${pageContext.request.contextPath}/receptionist/checkin?date=${filterDate}';
    }

    <c:if test="${walkInStep == 2}">
    // Tự mở modal khi trang load lại sau khi tra cứu SĐT (đã có class "open" sẵn ở backdrop,
    // đoạn này chỉ đảm bảo phòng trường hợp CSS chưa kịp áp dụng)
    document.addEventListener('DOMContentLoaded', openWalkIn);
    </c:if>
</script>
</body>
</html>
