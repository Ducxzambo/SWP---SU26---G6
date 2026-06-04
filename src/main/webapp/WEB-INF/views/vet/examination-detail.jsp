<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
</head>
<body>

<div class="layout">

    <%-- ── Sidebar ──────────────────────────────────────────────────────────── --%>
    <aside class="sidebar">
        <div class="sidebar-logo">🐾 PetClinic</div>
        <nav>
            <a href="${pageContext.request.contextPath}/vet/examination" class="nav-item active">
                 Hàng chờ khám
            </a>
        </nav>
        <div class="sidebar-user">
             ${sessionScope.staff.fullName}
            <a href="${pageContext.request.contextPath}/auth/logout" class="logout-link">Đăng xuất</a>
        </div>
    </aside>

    <%-- ── Main content ──────────────────────────────────────────────────────── --%>
    <main class="main-content">

        <%-- ═══════════════════════════════════════════════════════════════════
             BRANCH A: READ-ONLY VIEW  (recordID present → record loaded)
             ═══════════════════════════════════════════════════════════════════ --%>
        <c:if test="${not empty record}">

            <div class="page-header">
                <h1> Bệnh Án #${record.recordID}</h1>
                <p class="page-sub">Thông tin khám bệnh – chỉ xem</p>
            </div>

            <a href="${pageContext.request.contextPath}/vet/examination"
               class="btn btn-outline btn-sm" style="margin-bottom:20px;">← Quay lại hàng chờ</a>

            <%-- Patient summary strip --%>
            <div class="pet-info-strip">
                <div class="pet-info-item">
                    <span class="label">Thú cưng</span>
                    <span class="value">🐾 <c:out value="${record.petName}"/></span>
                </div>
                <div class="pet-info-item">
                    <span class="label">Chủ nhân</span>
                    <span class="value"><c:out value="${record.ownerName}"/></span>
                </div>
                <div class="pet-info-item">
                    <span class="label">Bác sĩ</span>
                    <span class="value"><c:out value="${record.vetName}"/></span>
                </div>
                <div class="pet-info-item">
                    <span class="label">Ngày khám</span>
                    <span class="value">
                        <c:out value="${fn:substring(record.createdAt, 8, 10)}/${fn:substring(record.createdAt, 5, 7)}/${fn:substring(record.createdAt, 0, 4)} ${fn:substring(record.createdAt, 11, 16)}"/>
                    </span>
                </div>
            </div>

            <%-- Vitals --%>
            <div class="card">
                <div class="card-header">
                    <span class="card-title"> Thông số</span>
                </div>
                <div class="card-body">
                    <div class="form-row col-2">
                        <div>
                            <div class="record-field">
                                <span class="record-field-label">Cân nặng (kg)</span>
                                <span class="record-field-value">
                                    <c:choose>
                                        <c:when test="${not empty record.weight}">${record.weight}</c:when>
                                        <c:otherwise>—</c:otherwise>
                                    </c:choose>
                                </span>
                            </div>
                        </div>
                        <div>
                            <div class="record-field">
                                <span class="record-field-label">Thân nhiệt (°C)</span>
                                <span class="record-field-value">
                                    <c:choose>
                                        <c:when test="${not empty record.temperature}">${record.temperature}</c:when>
                                        <c:otherwise>—</c:otherwise>
                                    </c:choose>
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <%-- Clinical notes --%>
            <div class="card">
                <div class="card-header">
                    <span class="card-title"> Ghi chú lâm sàng</span>
                </div>
                <div class="card-body">
                    <div class="record-field">
                        <span class="record-field-label">Triệu chứng</span>
                        <span class="record-field-value"><c:out value="${record.symptoms}"/></span>
                    </div>
                    <div class="record-field">
                        <span class="record-field-label">Chẩn đoán</span>
                        <span class="record-field-value"><c:out value="${record.diagnosis}"/></span>
                    </div>
                    <div class="record-field">
                        <span class="record-field-label">Phác đồ điều trị</span>
                        <span class="record-field-value"><c:out value="${record.treatmentPlan}"/></span>
                    </div>
                </div>
            </div>

            <%-- Prescription --%>
            <c:if test="${record.hasPrescription()}">
                <div class="card">
                    <div class="card-header">
                        <span class="card-title"> Đơn thuốc</span>
                    </div>
                    <div class="card-body" style="padding:0;">
                        <table class="data-table">
                            <thead>
                            <tr>
                                <th>#</th>
                                <th>Tên thuốc</th>
                                <th>Liều dùng</th>
                                <th>Số lượng</th>
                                <th>Đơn vị</th>
                                <th>Đơn giá</th>
                                <th>Thành tiền</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${record.prescriptionItems}" var="item" varStatus="vs">
                                <tr>
                                    <td>${vs.count}</td>
                                    <td><strong><c:out value="${item.medicineName}"/></strong></td>
                                    <td><c:out value="${item.dosage}"/></td>
                                    <td>${item.quantity}</td>
                                    <td><c:out value="${item.medicineUnit}"/></td>
                                    <td>
                                        <fmt:formatNumber value="${item.unitPrice}" type="currency"
                                                          currencySymbol="" groupingUsed="true"/>
                                    </td>
                                    <td>
                                        <strong>
                                            <fmt:formatNumber value="${item.lineTotal}" type="currency"
                                                              currencySymbol="" groupingUsed="true"/>
                                        </strong>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </c:if>

        </c:if><%-- end read-only view --%>


        <%-- ═══════════════════════════════════════════════════════════════════
             BRANCH B: EXAMINATION FORM  (appointment loaded, no record yet)
             ═══════════════════════════════════════════════════════════════════ --%>
        <c:if test="${not empty appointment}">

            <div class="page-header">
                <h1>🩺 Khám Bệnh</h1>
                <p class="page-sub">
                    <c:out value="${appointment.petName}"/> –
                    <c:out value="${appointment.customerName}"/> –
                    <c:out value="${appointment.serviceName}"/>
                </p>
            </div>

            <a href="${pageContext.request.contextPath}/vet/examination"
               class="btn btn-outline btn-sm" style="margin-bottom:20px;">← Quay lại hàng chờ</a>

            <%-- Error banner --%>
            <c:if test="${not empty error}">
                <div class="alert alert-error">
                    <span class="alert-icon">✕</span>
                    <span>${error}</span>
                </div>
            </c:if>

            <%-- Patient strip --%>
            <div class="pet-info-strip">
                <div class="pet-info-item">
                    <span class="label">Thú cưng</span>
                    <span class="value"> <c:out value="${appointment.petName}"/></span>
                </div>
                <div class="pet-info-item">
                    <span class="label">Chủ nhân</span>
                    <span class="value"><c:out value="${appointment.customerName}"/></span>
                </div>
                <div class="pet-info-item">
                    <span class="label">Bác sĩ</span>
                    <span class="value"><c:out value="${appointment.vetName != null ? appointment.vetName : sessionScope.staff.fullName}"/></span>
                </div>
                <div class="pet-info-item">
                    <span class="label">Dịch vụ</span>
                    <span class="value"><c:out value="${appointment.serviceName}"/></span>
                </div>
                <div class="pet-info-item">
                    <span class="label">Trạng thái</span>
                    <span class="value">
                        <span class="badge badge-info"> Đang khám</span>
                    </span>
                </div>
            </div>

            <%-- ── Pet Medical History (collapsible) ────────────────────────── --%>
            <c:if test="${not empty history}">
                <div class="card">
                    <div class="card-header">
                        <span class="card-title"> Lịch sử khám bệnh (${fn:length(history)} lần)</span>
                        <button type="button" class="btn btn-outline btn-sm" id="toggleHistoryBtn"
                                onclick="toggleHistory()">Hiện / Ẩn</button>
                    </div>
                    <div id="historySection" style="display:none; padding:16px 24px;">
                        <c:forEach items="${history}" var="h" varStatus="vs">
                            <div class="history-item">
                                <div class="history-item-header" onclick="toggleAccordion(this)">
                                    <span>
                                        <strong>#${vs.count}</strong> –
                                        <c:out value="${fn:substring(h.createdAt, 8, 10)}/${fn:substring(h.createdAt, 5, 7)}/${fn:substring(h.createdAt, 0, 4)}"/>
                                        &nbsp;|&nbsp; <c:out value="${h.diagnosis != null ? h.diagnosis : 'Chưa có chẩn đoán'}"/>
                                    </span>
                                    <span class="toggle-icon">▼</span>
                                </div>
                                <div class="history-item-body">
                                    <dl>
                                        <dt>Cân nặng</dt>
                                        <dd>${not empty h.weight ? h.weight : '—'} kg</dd>
                                        <dt>Thân nhiệt</dt>
                                        <dd>${not empty h.temperature ? h.temperature : '—'} °C</dd>
                                        <dt>Triệu chứng</dt>
                                        <dd><c:out value="${not empty h.symptoms ? h.symptoms : '—'}"/></dd>
                                        <dt>Chẩn đoán</dt>
                                        <dd><c:out value="${not empty h.diagnosis ? h.diagnosis : '—'}"/></dd>
                                        <dt>Phác đồ</dt>
                                        <dd><c:out value="${not empty h.treatmentPlan ? h.treatmentPlan : '—'}"/></dd>
                                    </dl>
                                    <c:if test="${h.hasPrescription()}">
                                        <div style="margin-top:12px;">
                                            <strong style="font-size:13px;"> Đơn thuốc:</strong>
                                            <table class="prescription-table" style="margin-top:8px;">
                                                <thead>
                                                <tr>
                                                    <th>Thuốc</th><th>Liều dùng</th>
                                                    <th>SL</th><th>Đơn vị</th>
                                                </tr>
                                                </thead>
                                                <tbody>
                                                <c:forEach items="${h.prescriptionItems}" var="pi">
                                                    <tr>
                                                        <td><c:out value="${pi.medicineName}"/></td>
                                                        <td><c:out value="${pi.dosage}"/></td>
                                                        <td>${pi.quantity}</td>
                                                        <td><c:out value="${pi.medicineUnit}"/></td>
                                                    </tr>
                                                </c:forEach>
                                                </tbody>
                                            </table>
                                        </div>
                                    </c:if>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </c:if>

            <%-- ── Examination Form ───────────────────────────────────────────── --%>
            <form action="${pageContext.request.contextPath}/vet/examination"
                  method="post" id="examForm" novalidate>

                <input type="hidden" name="appointmentID" value="${appointment.appointmentID}">

                    <%-- Vitals --%>
                <div class="card">
                    <div class="card-header">
                        <span class="card-title"> Thông số</span>
                    </div>
                    <div class="card-body">
                        <div class="form-row col-2">
                            <div class="form-group">
                                <label class="form-label" for="weight">Cân nặng (kg)</label>
                                <input type="number" id="weight" name="weight" class="form-control no-icon"
                                       step="0.01" min="0" max="999"
                                       placeholder="VD: 4.5"
                                       value="<c:out value='${param.weight}'/>">
                            </div>
                            <div class="form-group">
                                <label class="form-label" for="temperature">Thân nhiệt (°C)</label>
                                <input type="number" id="temperature" name="temperature" class="form-control no-icon"
                                       step="0.1" min="30" max="45"
                                       placeholder="VD: 38.5"
                                       value="<c:out value='${param.temperature}'/>">
                            </div>
                        </div>
                    </div>
                </div>

                    <%-- Clinical notes --%>
                <div class="card">
                    <div class="card-header">
                        <span class="card-title"> Ghi chú lâm sàng</span>
                    </div>
                    <div class="card-body">
                        <div class="form-group">
                            <label class="form-label" for="symptoms">
                                Triệu chứng <span class="required">*</span>
                            </label>
                            <textarea id="symptoms" name="symptoms" class="form-control"
                                      rows="3" placeholder="Mô tả triệu chứng quan sát được..."
                                      required><c:out value='${not empty symptoms ? symptoms : param.symptoms}'/></textarea>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="diagnosis">
                                Chẩn đoán <span class="required">*</span>
                            </label>
                            <textarea id="diagnosis" name="diagnosis" class="form-control"
                                      rows="3" placeholder="Kết quả chẩn đoán..."
                                      required><c:out value='${not empty diagnosis ? diagnosis : param.diagnosis}'/></textarea>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="treatmentPlan">Phác đồ điều trị</label>
                            <textarea id="treatmentPlan" name="treatmentPlan" class="form-control"
                                      rows="3" placeholder="Hướng dẫn điều trị, chú ý chăm sóc..."><c:out value='${not empty treatmentPlan ? treatmentPlan : param.treatmentPlan}'/></textarea>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="followUpDate">Ngày tái khám</label>
                            <input type="date" id="followUpDate" name="followUpDate"
                                   class="form-control no-icon" style="max-width:220px;"
                                   min="<%= java.time.LocalDate.now().plusDays(1) %>">
                            <div class="form-hint">Để trống nếu không cần tái khám.</div>
                        </div>
                    </div>
                </div>

                    <%-- Prescription --%>
                <div class="card">
                    <div class="card-header">
                        <span class="card-title"> Đơn thuốc</span>
                        <span style="font-size:13px;color:var(--text-soft);">Để trống nếu không kê đơn</span>
                    </div>
                    <div class="card-body">
                        <table class="prescription-table" id="prescriptionTable">
                            <thead>
                            <tr>
                                <th style="width:32%">Thuốc</th>
                                <th style="width:34%">Liều dùng</th>
                                <th style="width:18%">Số lượng</th>
                                <th style="width:10%">Tồn kho</th>
                                <th style="width:6%"></th>
                            </tr>
                            </thead>
                            <tbody id="prescriptionBody">
                                <%-- Rows injected by JS --%>
                            </tbody>
                        </table>
                        <button type="button" class="add-row-btn" onclick="addPrescriptionRow()">
                            + Thêm thuốc vào đơn
                        </button>

                            <%-- Sửa lại cú pháp JSON an toàn --%>
                        <script id="medicineData" type="application/json">
                            [
                            <c:forEach items="${medicines}" var="m" varStatus="vs">
                                {
                                "id":    ${m.medicineID},
                                "name":  "<c:out value='${m.name}'/>",
                                "unit":  "<c:out value='${m.unit}'/>",
                                "price": ${m.unitPrice},
                                "stock": ${m.stockQty}
                                }${not vs.last ? ',' : ''}
                            </c:forEach>
                            ]
                        </script>
                    </div>
                </div>

                    <%-- Submit --%>
                <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:4px;padding-bottom:40px;">
                    <a href="${pageContext.request.contextPath}/vet/examination"
                       class="btn btn-outline btn-lg">Hủy bỏ</a>
                    <button type="submit" class="btn btn-primary btn-lg" id="submitBtn">
                        Lưu bệnh án & Hoàn thành khám
                    </button>
                </div>

            </form>

        </c:if><%-- end examination form --%>


        <%-- Fallback if neither record nor appointment loaded --%>
        <c:if test="${empty record && empty appointment}">
            <div class="page-header">
                <h1>🩺 Khám Bệnh</h1>
            </div>
            <div class="card">
                <div class="empty-state">
                    <div class="empty-icon">⚠️</div>
                    <p>Không tìm thấy dữ liệu. <a href="${pageContext.request.contextPath}/vet/examination">Quay lại hàng chờ</a></p>
                </div>
            </div>
        </c:if>

    </main>
</div>

<%-- ── Scripts ─────────────────────────────────────────────────────────────── --%>
<script>
    /* ── Medicine data ────────────────────────────────────────────────────────── */
    const MEDICINES = JSON.parse(document.getElementById('medicineData')?.textContent || '[]');

    /* ── Sửa lại hàm build để đồng bộ thuộc tính data-stock ───────────────────── */
    function buildMedicineSelect(selectedId) {
        let html = '<option value="">— Chọn thuốc —</option>';
        MEDICINES.forEach(m => {
            const sel = (selectedId && m.id == selectedId) ? ' selected' : '';
            html += `<option value="${m.id}" data-unit="${m.unit}" data-price="${m.price}" data-stock="${m.stock}"${sel}>
                    ${m.name} (${m.unit}) — còn ${m.stock}
                 </option>`;
        });
        return html;
    }

    /* ── Add prescription row ─────────────────────────────────────────────────── */
    let rowIndex = 0;

    function addPrescriptionRow() {
        rowIndex++;
        const tbody = document.getElementById('prescriptionBody');
        const tr = document.createElement('tr');
        tr.dataset.row = rowIndex;
        tr.innerHTML = `
        <td>
            <select name="medicineID[]" class="form-control" onchange="onMedicineChange(this)"
                    style="padding-left:10px;">
                \${buildMedicineSelect()}
            </select>
        </td>
        <td>
            <input type="text" name="dosage[]" class="form-control"
                   placeholder="VD: 2 lần/ngày, 1 viên/lần">
        </td>
        <td>
            <input type="number" name="quantity[]" class="form-control"
                   min="1" step="1" value="1" style="width:80px;">
        </td>
        <td class="stock-cell" style="color:var(--text-soft);font-size:13px;">—</td>
        <td>
            <button type="button" class="remove-row-btn" onclick="removeRow(this)" title="Xóa hàng">×</button>
        </td>
    `;
        tbody.appendChild(tr);
    }

    function onMedicineChange(sel) {
        const opt  = sel.options[sel.selectedIndex];
        const stock = opt.dataset.stock;
        const stockCell = sel.closest('tr').querySelector('.stock-cell');
        if (stock !== undefined && opt.value) {
            stockCell.textContent = stock + ' ' + (opt.dataset.unit || '');
            stockCell.style.color = parseInt(stock) < 5 ? 'var(--red-400)' : 'var(--text-soft)';
        } else {
            stockCell.textContent = '—';
            stockCell.style.color = 'var(--text-soft)';
        }
    }

    function removeRow(btn) {
        btn.closest('tr').remove();
    }

    /* ── History section toggle ───────────────────────────────────────────────── */
    function toggleHistory() {
        const s = document.getElementById('historySection');
        if (s) s.style.display = s.style.display === 'none' ? 'block' : 'none';
    }

    function toggleAccordion(header) {
        const body = header.nextElementSibling;
        const icon = header.querySelector('.toggle-icon');
        if (body.classList.contains('open')) {
            body.classList.remove('open');
            if (icon) icon.textContent = '▼';
        } else {
            body.classList.add('open');
            if (icon) icon.textContent = '▲';
        }
    }

    /* ── Form validation ──────────────────────────────────────────────────────── */
    const examForm = document.getElementById('examForm');
    if (examForm) {
        examForm.addEventListener('submit', function (e) {
            const symptoms  = document.getElementById('symptoms')?.value.trim();
            const diagnosis = document.getElementById('diagnosis')?.value.trim();

            if (!symptoms || !diagnosis) {
                e.preventDefault();
                alert('Vui lòng nhập Triệu chứng và Chẩn đoán trước khi lưu bệnh án.');
                return;
            }

            // Validate prescription rows
            const medSelects  = document.querySelectorAll('[name="medicineID[]"]');
            const qtyInputs   = document.querySelectorAll('[name="quantity[]"]');
            let valid = true;

            medSelects.forEach((sel, i) => {
                if (sel.value === '') return; // skip empty rows
                const qty = qtyInputs[i]?.value;
                if (!qty || parseInt(qty) < 1) {
                    alert('Số lượng thuốc phải lớn hơn 0.');
                    valid = false;
                }

                // warn if exceeds stock
                const opt   = sel.options[sel.selectedIndex];
                const stock = parseInt(opt.dataset.stock || 0);
                const need  = parseInt(qty || 0);
                if (need > stock) {
                    if (!confirm(`Thuốc "${opt.text.split(' (')[0]}" chỉ còn ${stock} trong kho, bạn muốn kê ${need}. Tiếp tục?`)) {
                        valid = false;
                    }
                }
            });
            if (!valid) e.preventDefault();
            else {
                document.getElementById('submitBtn').disabled = true;
                document.getElementById('submitBtn').textContent = '⏳ Đang lưu…';
            }
        });
    }
</script>

<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>