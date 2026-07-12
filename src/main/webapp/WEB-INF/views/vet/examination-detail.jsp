<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="com.petclinic.model.Medicine" %>
<%@ page import="java.util.List" %>
<%
    // Build medicine JSON safely server-side (handles quotes, backslashes, etc.)
    List<Medicine> __meds = (List<Medicine>) request.getAttribute("medicines");
    StringBuilder __medJson = new StringBuilder("[");
    if (__meds != null) {
        for (int __i = 0; __i < __meds.size(); __i++) {
            Medicine __m = __meds.get(__i);
            if (__i > 0) __medJson.append(",");
            __medJson.append("{")
                    .append("\"id\":").append(__m.getMedicineID()).append(",")
                    .append("\"name\":\"").append(jsonEscape(__m.getName())).append("\",")
                    .append("\"unit\":\"").append(jsonEscape(__m.getUnit())).append("\",")
                    .append("\"price\":").append(__m.getUnitPrice()).append(",")
                    .append("\"stock\":").append(__m.getStockQty())
                    .append("}");
        }
    }
    __medJson.append("]");
%>
<%!
    /** Escape a string for safe embedding inside a JSON string literal. */
    private String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>
        <c:choose>
            <c:when test="${not empty record}">Bệnh Án #${record.recordID} – PetClinic</c:when>
            <c:otherwise>Khám Bệnh – PetClinic</c:otherwise>
        </c:choose>
    </title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <style>
        /* Checklist card */
        .checklist-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
        }
        @media(max-width:700px){ .checklist-grid { grid-template-columns: 1fr; } }

        .check-item {
            border: 1.5px solid var(--border);
            border-radius: 8px;
            overflow: hidden;
            transition: border-color .2s;
        }
        .check-item.checked {
            border-color: var(--teal-400);
            background: var(--teal-50);
        }
        .check-item-header {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 10px 14px;
            cursor: pointer;
            user-select: none;
        }
        .check-item-header input[type=checkbox] {
            width: 18px; height: 18px;
            accent-color: var(--teal-500);
            cursor: pointer;
            flex-shrink: 0;
        }
        .check-item-header label {
            font-size: 14px;
            font-weight: 500;
            color: var(--text);
            cursor: pointer;
            flex: 1;
        }
        .check-item-note {
            display: none;
            padding: 0 14px 10px 42px;
        }
        .check-item-note textarea {
            width: 100%;
            font-size: 13px;
            padding: 6px 10px;
            border: 1px solid var(--border);
            border-radius: 6px;
            resize: vertical;
            min-height: 56px;
            font-family: inherit;
            color: var(--text);
            background: #fff;
        }
        .check-item-note textarea:focus {
            outline: none;
            border-color: var(--teal-500);
            box-shadow: 0 0 0 2px rgba(30,138,120,.1);
        }
        .check-item.checked .check-item-note { display: block; }

        /* Read-only checklist */
        .check-read-item {
            padding: 10px 14px;
            border: 1px solid var(--teal-100);
            border-radius: 8px;
            background: var(--teal-50);
            margin-bottom: 8px;
        }
        .check-read-item .name { font-weight: 600; color: var(--teal-800); font-size: 14px; }
        .check-read-item .note { font-size: 13px; color: var(--text-mid); margin-top: 3px; }

        /* Prescription table */
        .rx-table { width:100%; border-collapse:collapse; font-size:14px; }
        .rx-table thead th {
            font-size:12px; font-weight:600; color:var(--text-soft);
            text-transform:uppercase; letter-spacing:.5px;
            padding:8px 10px; border-bottom:1px solid var(--border); text-align:left;
        }
        .rx-table tbody td { padding:8px 10px; border-bottom:1px solid var(--border); vertical-align:middle; }
        .rx-table tbody tr:last-child td { border-bottom:none; }
        .rx-table select.form-control,
        .rx-table input.form-control  { font-size:13px; padding:6px 10px; }
        .add-row-btn {
            background:none; border:1.5px dashed var(--teal-400); color:var(--teal-500);
            border-radius:8px; padding:8px 16px; font-size:13px; font-family:inherit;
            cursor:pointer; width:100%; margin-top:10px; transition:var(--transition);
        }
        .add-row-btn:hover { background:var(--teal-50); }
        .rm-btn {
            background:none; border:none; color:var(--red-400); cursor:pointer;
            font-size:18px; line-height:1; padding:2px 6px; border-radius:4px;
        }
        .rm-btn:hover { background:var(--red-100); }

        /* History accordion */
        .hist-item { border:1px solid var(--border); border-radius:8px; margin-bottom:8px; overflow:hidden; }
        .hist-hdr {
            display:flex; justify-content:space-between; align-items:center;
            padding:10px 14px; background:var(--bg); cursor:pointer; user-select:none;
        }
        .hist-hdr:hover { background:var(--teal-50); }
        .hist-body { display:none; padding:14px 16px; border-top:1px solid var(--border); font-size:13.5px; line-height:1.7; }
        .hist-body.open { display:block; }
        .hist-body dl { display:grid; grid-template-columns:130px 1fr; gap:4px 10px; }
        .hist-body dt { font-weight:600; color:var(--text-mid); }

        /* Pet strip */
        .pet-strip {
            display:flex; gap:12px; flex-wrap:wrap;
            background:var(--teal-50); border:1px solid var(--teal-100);
            border-radius:10px; padding:14px 18px; margin-bottom:20px;
        }
        .pet-strip-item { display:flex; flex-direction:column; gap:2px; min-width:100px; }
        .pet-strip-item .lbl { font-size:11px; text-transform:uppercase; letter-spacing:.6px; color:var(--text-soft); font-weight:500; }
        .pet-strip-item .val { font-size:14px; font-weight:600; color:var(--teal-800); }
    </style>
</head>
<body>
<div class="layout">
    <aside class="sidebar">
        <div class="sidebar-logo">🐾 PetClinic</div>
        <nav>
            <a href="${pageContext.request.contextPath}/vet/examination" class="nav-item active"> Hàng chờ khám</a>
        </nav>
        <div class="sidebar-user">
             ${sessionScope.staff.fullName}
            <a href="${pageContext.request.contextPath}/auth/logout" class="logout-link">Đăng xuất</a>
        </div>
    </aside>

    <main class="main-content">

        <%-- ═══════════════════════════════════════════════════════════════════════════
             READ-ONLY VIEW
             ═══════════════════════════════════════════════════════════════════════════ --%>
        <c:if test="${not empty record}">
            <div class="page-header">
                <h1>Bệnh Án #${record.recordID}</h1>
                <p class="page-sub">Chỉ xem — không chỉnh sửa</p>
            </div>
            <a href="${pageContext.request.contextPath}/vet/examination" class="btn btn-outline btn-sm" style="margin-bottom:18px;">← Quay lại</a>

            <div class="pet-strip">
                <div class="pet-strip-item"><span class="lbl">Thú cưng</span><span class="val">🐾 <c:out value="${record.petName}"/></span></div>
                <div class="pet-strip-item"><span class="lbl">Chủ nhân</span><span class="val"><c:out value="${record.ownerName}"/></span></div>
                <div class="pet-strip-item"><span class="lbl">Bác sĩ</span><span class="val"><c:out value="${record.staffName}"/></span></div>
                <div class="pet-strip-item"><span class="lbl">Ngày khám</span><span class="val">${record.createdAt}</span></div>
            </div>

            <%-- Vitals --%>
            <div class="card" style="margin-bottom:16px;">
                <div class="card-header"><span class="card-title">Thông số</span></div>
                <div class="card-body">
                    <div class="form-row col-2">
                        <div class="record-field"><span class="record-field-label">Cân nặng (kg)</span>
                            <span class="record-field-value">${not empty record.weight ? record.weight : '—'}</span></div>
                        <div class="record-field"><span class="record-field-label">Thân nhiệt (°C)</span>
                            <span class="record-field-value">${not empty record.temperature ? record.temperature : '—'}</span></div>
                    </div>
                </div>
            </div>

            <%-- Symptoms --%>
            <div class="card" style="margin-bottom:16px;">
                <div class="card-header"><span class="card-title">Triệu chứng</span></div>
                <div class="card-body">
                    <p style="white-space:pre-wrap;font-size:14px;"><c:out value="${record.symptoms}"/></p>
                </div>
            </div>

            <%-- Diagnosis (lab tests) --%>
            <div class="card" style="margin-bottom:16px;">
                <div class="card-header"><span class="card-title">Chẩn đoán / Xét nghiệm</span></div>
                <div class="card-body">
                    <c:choose>
                        <c:when test="${not empty record.diagnosis}">
                            <p style="white-space:pre-wrap;font-size:14px;"><c:out value="${record.diagnosis}"/></p>
                        </c:when>
                        <c:otherwise><p style="color:var(--text-soft)">—</p></c:otherwise>
                    </c:choose>
                </div>
            </div>

            <%-- Treatment plan --%>
            <div class="card" style="margin-bottom:16px;">
                <div class="card-header"><span class="card-title">Phác đồ điều trị</span></div>
                <div class="card-body">
                    <c:choose>
                        <c:when test="${not empty record.treatmentPlan}">
                            <p style="white-space:pre-wrap;font-size:14px;"><c:out value="${record.treatmentPlan}"/></p>
                        </c:when>
                        <c:otherwise><p style="color:var(--text-soft)">—</p></c:otherwise>
                    </c:choose>
                </div>
            </div>

            <%-- Prescription --%>
            <c:if test="${record.hasPrescription()}">
                <div class="card">
                    <div class="card-header"><span class="card-title">Đơn thuốc</span></div>
                    <div class="card-body" style="padding:0;">
                        <table class="data-table">
                            <thead>
                            <tr><th>#</th><th>Tên thuốc</th><th>Liều dùng</th><th>Số lượng</th><th>Đơn vị</th><th>Đơn giá</th><th>Thành tiền</th></tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${record.prescriptionItems}" var="pi" varStatus="vs">
                                <tr>
                                    <td>${vs.count}</td>
                                    <td><strong><c:out value="${pi.medicineName}"/></strong></td>
                                    <td><c:out value="${pi.dosage}"/></td>
                                    <td>${pi.quantity}</td>
                                    <td><c:out value="${pi.unit}"/></td>
                                    <td><fmt:formatNumber value="${pi.unitPrice}" type="number" groupingUsed="true"/></td>
                                    <td><strong><fmt:formatNumber value="${pi.lineTotal}" type="number" groupingUsed="true"/></strong></td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </c:if>
        </c:if>


        <%-- ═══════════════════════════════════════════════════════════════════════════
             EXAMINATION FORM
             ═══════════════════════════════════════════════════════════════════════════ --%>
        <c:if test="${not empty appointment}">
            <div class="page-header">
                <h1>🩺 Khám Bệnh</h1>
                <p class="page-sub"><c:out value="${appointment.petName}"/> — <c:out value="${appointment.customerName}"/> — <c:out value="${appointment.serviceName}"/></p>
            </div>
            <a href="${pageContext.request.contextPath}/vet/examination" class="btn btn-outline btn-sm" style="margin-bottom:18px;">← Quay lại hàng chờ</a>

            <c:if test="${not empty error}">
                <div class="alert alert-error"><span class="alert-icon">✕</span> ${error}</div>
            </c:if>

            <%-- Pet strip --%>
            <div class="pet-strip">
                <div class="pet-strip-item"><span class="lbl">Thú cưng</span><span class="val">🐾 <c:out value="${appointment.petName}"/></span></div>
                <div class="pet-strip-item"><span class="lbl">Chủ nhân</span><span class="val"><c:out value="${appointment.customerName}"/></span></div>
                <div class="pet-strip-item"><span class="lbl">Bác sĩ</span><span class="val">/></span></div>
                <div class="pet-strip-item"><span class="lbl">Ca</span><span class="val">Ca ${appointment.slotShift}</span></div>
                <div class="pet-strip-item"><span class="lbl">Trạng thái</span><span class="val"><span class="badge badge-info">Đang khám</span></span></div>
            </div>

            <%-- History --%>
            <c:if test="${not empty history}">
                <div class="card" style="margin-bottom:16px;">
                    <div class="card-header">
                        <span class="card-title">Lịch sử khám (${history.size()} lần)</span>
                        <button type="button" class="btn btn-outline btn-sm" onclick="toggleHist()">Hiện / Ẩn</button>
                    </div>
                    <div id="histSection" style="display:none;padding:14px 20px;">
                        <c:forEach items="${history}" var="h" varStatus="vs">
                            <div class="hist-item">
                                <div class="hist-hdr" onclick="toggleAcc(this)">
                                    <span><strong>#${vs.count}</strong> — ${h.createdAt} &nbsp;|&nbsp; <c:out value="${not empty h.diagnosis ? h.diagnosis : 'Chưa có chẩn đoán'}"/></span>
                                    <span>▼</span>
                                </div>
                                <div class="hist-body">
                                    <dl>
                                        <dt>Cân nặng</dt><dd>${not empty h.weight ? h.weight : '—'} kg</dd>
                                        <dt>Thân nhiệt</dt><dd>${not empty h.temperature ? h.temperature : '—'} °C</dd>
                                        <dt>Triệu chứng</dt><dd><c:out value="${not empty h.symptoms ? h.symptoms : '—'}"/></dd>
                                        <dt>Chẩn đoán</dt><dd><c:out value="${not empty h.diagnosis ? h.diagnosis : '—'}"/></dd>
                                        <dt>Phác đồ</dt><dd><c:out value="${not empty h.treatmentPlan ? h.treatmentPlan : '—'}"/></dd>
                                    </dl>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </c:if>

            <%-- ── FORM ────────────────────────────────────────────────────────────── --%>
            <form action="${pageContext.request.contextPath}/vet/examination" method="post" id="examForm" novalidate>
                <input type="hidden" name="appointmentID" value="${appointment.appointmentID}">

                    <%-- 1. Vitals --%>
                <div class="card" style="margin-bottom:16px;">
                    <div class="card-header"><span class="card-title">Thông số</span></div>
                    <div class="card-body">
                        <div class="form-row col-2">
                            <div class="form-group">
                                <label class="form-label" for="weight">Cân nặng (kg)</label>
                                <input type="number" id="weight" name="weight" class="form-control no-icon"
                                       step="0.01" min="0" max="999" placeholder="VD: 4.5">
                            </div>
                            <div class="form-group">
                                <label class="form-label" for="temperature">Thân nhiệt (°C)</label>
                                <input type="number" id="temperature" name="temperature" class="form-control no-icon"
                                       step="0.1" min="30" max="45" placeholder="VD: 38.5">
                            </div>
                        </div>
                    </div>
                </div>

                    <%-- 2. Symptoms --%>
                <div class="card" style="margin-bottom:16px;">
                    <div class="card-header"><span class="card-title">Triệu chứng <span style="color:var(--red-400)">*</span></span></div>
                    <div class="card-body">
        <textarea name="symptoms" id="symptoms" class="form-control" rows="3"
                  placeholder="Mô tả triệu chứng quan sát được..." required><c:out value="${symptoms}"/></textarea>
                    </div>
                </div>

                    <%-- 3. Lab tests (xét nghiệm) --%>
                <div class="card" style="margin-bottom:16px;">
                    <div class="card-header">
                        <span class="card-title">🔬 Chẩn đoán — Loại xét nghiệm</span>
                        <span style="font-size:12px;color:var(--text-soft);">Tích chọn và ghi chú từng loại</span>
                    </div>
                    <div class="card-body">
                        <c:choose>
                            <c:when test="${not empty labTests}">
                                <div class="checklist-grid" id="labGrid">
                                    <c:forEach items="${labTests}" var="llt">
                                        <div class="check-item" id="labItem_${llt.serviceID}">
                                            <div class="check-item-header">
                                                <input type="checkbox"
                                                       id="lab_${llt.serviceID}"
                                                       name="labTestID[]"
                                                       value="${llt.serviceID}"
                                                       onchange="toggleCheckItem(this, 'labItem_${llt.serviceID}')">
                                                <input type="hidden" name="labTestName_${llt.serviceID}" value="<c:out value='${llt.name}'/>">
                                                <label for="lab_${llt.serviceID}"><c:out value="${llt.name}"/></label>
                                                <c:if test="${llt.price > 0}">
                      <span style="font-size:12px;color:var(--text-soft);">
                        <fmt:formatNumber value="${llt.price}" type="number" groupingUsed="true"/>đ
                      </span>
                                                </c:if>
                                            </div>
                                            <div class="check-item-note">
                    <textarea name="labTestNote_${llt.serviceID}"
                              placeholder="Ghi chú cho ${llt.name}..."></textarea>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="alert alert-warning">
                                    <span class="alert-icon">⚠</span>
                                    Chưa có danh sách xét nghiệm. Vui lòng chạy script SQL bp02_extend.sql.
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                    <%-- 4. Treatment plans (phác đồ) --%>
                <div class="card" style="margin-bottom:16px;">
                    <div class="card-header">
                        <span class="card-title"> Phác đồ điều trị</span>
                        <span style="font-size:12px;color:var(--text-soft);">Tích chọn và ghi chú từng phác đồ</span>
                    </div>
                    <div class="card-body">
                        <c:choose>
                            <c:when test="${not empty treatmentPlans}">
                                <div class="checklist-grid" id="treatGrid">
                                    <c:forEach items="${treatmentPlans}" var="tp">
                                        <div class="check-item" id="treatItem_${tp.serviceID}">
                                            <div class="check-item-header">
                                                <input type="checkbox"
                                                       id="treat_${tp.serviceID}"
                                                       name="treatmentID[]"
                                                       value="${tp.serviceID}"
                                                       onchange="toggleCheckItem(this, 'treatItem_${tp.serviceID}')">
                                                <input type="hidden" name="treatmentName_${tp.serviceID}" value="<c:out value='${tp.name}'/>">
                                                <label for="treat_${tp.serviceID}"><c:out value="${tp.name}"/></label>
                                            </div>
                                            <div class="check-item-note">
                    <textarea name="treatmentNote_${tp.serviceID}"
                              placeholder="Ghi chú cho ${tp.name}..."></textarea>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="alert alert-warning">
                                    <span class="alert-icon">⚠</span>
                                    Chưa có danh sách phác đồ. Vui lòng chạy script SQL bp02_extend.sql.
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                    <%-- 5. Follow-up date --%>
                <div class="card" style="margin-bottom:16px;">
                    <div class="card-header"><span class="card-title"> Ngày tái khám</span></div>
                    <div class="card-body">
                        <input type="date" name="followUpDate" class="form-control no-icon" style="max-width:200px;"
                               min="<%= java.time.LocalDate.now().plusDays(1) %>">
                        <div class="form-hint">Để trống nếu không cần tái khám.</div>
                    </div>
                </div>

                    <%-- 6. Prescription --%>
                <div class="card" style="margin-bottom:16px;">
                    <div class="card-header">
                        <span class="card-title">Kê đơn thuốc</span>
                        <span style="font-size:12px;color:var(--text-soft);">Để trống nếu không kê đơn</span>
                    </div>
                    <div class="card-body">
                        <table class="rx-table" id="rxTable">
                            <thead>
                            <tr>
                                <th style="width:35%">Thuốc</th>
                                <th style="width:33%">Liều dùng</th>
                                <th style="width:16%">Số lượng</th>
                                <th style="width:10%">Còn kho</th>
                                <th style="width:6%"></th>
                            </tr>
                            </thead>
                            <tbody id="rxBody"></tbody>
                        </table>
                        <button type="button" class="add-row-btn" onclick="addRxRow()">+ Thêm thuốc</button>

                            <%-- Medicine data for JS (built safely server-side, see scriptlet at top of file) --%>
                        <script type="application/json" id="medData"><%= __medJson.toString() %></script>
                    </div>
                </div>

                    <%-- Submit --%>
                <div style="display:flex;gap:12px;justify-content:flex-end;padding-bottom:40px;">
                    <a href="${pageContext.request.contextPath}/vet/examination" class="btn btn-outline btn-lg">Hủy</a>
                    <button type="submit" class="btn btn-primary btn-lg" id="submitBtn">
                        Lưu bệnh án & Hoàn thành khám
                    </button>
                </div>
            </form>
        </c:if>

        <%-- Fallback --%>
        <c:if test="${empty record && empty appointment}">
            <div class="page-header"><h1> Khám Bệnh</h1></div>
            <div class="card"><div class="empty-state"><div class="empty-icon">⚠️</div>
                <p>Không tìm thấy dữ liệu. <a href="${pageContext.request.contextPath}/vet/examination">Quay lại hàng chờ</a></p>
            </div></div>
        </c:if>

    </main>
</div>

<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
<script>
    // ── Medicine data ─────────────────────────────────────────────────────────
    const MEDS = JSON.parse(document.getElementById('medData')?.textContent || '[]');

    function buildMedOptions() {
        return '<option value="">— Chọn thuốc —</option>' +
            MEDS.map(m =>
                '<option value="'+m.id+'" data-unit="'+m.unit+'" data-stock="'+m.stock+'">'
                + m.name + ' ('+m.unit+') — còn '+m.stock+'</option>'
            ).join('');
    }

    let rxIdx = 0;
    function addRxRow() {
        rxIdx++;
        const tr = document.createElement('tr');
        tr.innerHTML =
            '<td><select name="medicineID[]" class="form-control" onchange="onMedChange(this)" style="padding-left:10px;">'
            + buildMedOptions() +
            '</select></td>'
            + '<td><input type="text" name="dosage[]" class="form-control" placeholder="VD: 2 lần/ngày, 1 viên/lần"></td>'
            + '<td><input type="number" name="quantity[]" class="form-control" min="1" step="1" value="1" style="width:80px;"></td>'
            + '<td class="stk" style="color:var(--text-soft);font-size:13px;">—</td>'
            + '<td><button type="button" class="rm-btn" onclick="this.closest(\'tr\').remove()" title="Xóa">×</button></td>';
        document.getElementById('rxBody').appendChild(tr);
    }

    function onMedChange(sel) {
        const opt = sel.options[sel.selectedIndex];
        const cell = sel.closest('tr').querySelector('.stk');
        if (opt.value) {
            const stock = opt.dataset.stock;
            cell.textContent = stock + ' ' + (opt.dataset.unit || '');
            cell.style.color = parseInt(stock) < 5 ? 'var(--red-400)' : 'var(--text-soft)';
        } else {
            cell.textContent = '—';
            cell.style.color = 'var(--text-soft)';
        }
    }

    // ── Checklist toggle ──────────────────────────────────────────────────────
    function toggleCheckItem(cb, itemId) {
        const item = document.getElementById(itemId);
        if (cb.checked) { item.classList.add('checked'); }
        else            { item.classList.remove('checked'); }
    }

    // ── History accordion ─────────────────────────────────────────────────────
    function toggleHist() {
        const s = document.getElementById('histSection');
        if (s) s.style.display = s.style.display === 'none' ? 'block' : 'none';
    }
    function toggleAcc(hdr) {
        const body = hdr.nextElementSibling;
        const icon = hdr.querySelector('span:last-child');
        body.classList.toggle('open');
        if (icon) icon.textContent = body.classList.contains('open') ? '▲' : '▼';
    }

    // ── Form validation ───────────────────────────────────────────────────────
    document.getElementById('examForm')?.addEventListener('submit', function(e) {
        if (!document.getElementById('symptoms').value.trim()) {
            e.preventDefault();
            alert('Vui lòng nhập Triệu chứng trước khi lưu.');
            return;
        }

        const meds = document.querySelectorAll('[name="medicineID[]"]');
        const qtys = document.querySelectorAll('[name="quantity[]"]');
        for (let i = 0; i < meds.length; i++) {
            if (!meds[i].value) continue;
            const qty = parseInt(qtys[i]?.value || 0);
            if (qty < 1) { e.preventDefault(); alert('Số lượng thuốc phải lớn hơn 0.'); return; }
            const opt = meds[i].options[meds[i].selectedIndex];
            const stock = parseInt(opt.dataset.stock || 0);
            if (qty > stock) {
                if (!confirm('Thuốc "' + opt.text.split(' (')[0] + '" chỉ còn ' + stock + ', bạn muốn kê ' + qty + '. Tiếp tục?')) {
                    e.preventDefault(); return;
                }
            }
        }

        document.getElementById('submitBtn').disabled = true;
        document.getElementById('submitBtn').textContent = '⏳ Đang lưu...';
    });
</script>
</body>
</html>
