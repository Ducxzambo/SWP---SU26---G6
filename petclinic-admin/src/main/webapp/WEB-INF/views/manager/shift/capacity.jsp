<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeModule" value="capacity" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sức Chứa Theo Ca - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <style>
        .cap-table { width:100%; border-collapse:collapse; font-size:13.5px; }
        .cap-table th, .cap-table td { border:1px solid var(--border); padding:8px 10px; text-align:center; }
        .cap-table th { background:var(--teal-50); color:var(--teal-700); font-weight:600; }
        .cap-table td.date-cell { text-align:left; font-weight:600; background:var(--bg); white-space:nowrap; }
        .cap-table tr.alert-row td.date-cell { background:#fdecea; color:#b91c1c; }
        .cap-alert-note { font-size:11px; color:#b91c1c; font-weight:700; display:block; margin-top:2px; }
        .cap-inputs { display:flex; flex-direction:column; gap:4px; align-items:center; }
        .cap-inputs label { font-size:11px; color:var(--text-soft); }
        .cap-inputs input { width:64px; padding:4px 6px; border:1px solid var(--border); border-radius:6px; text-align:center; }
        .bulk-shift-checks { display:flex; gap:14px; align-items:center; flex-wrap:wrap; }
        .bulk-shift-checks label { display:flex; align-items:center; gap:5px; font-size:13px; font-weight:400; }
    </style>
</head>
<body>
<div class="layout">
    <c:set var="activeModule" value="capacity" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Sức Chứa Theo Ca</h1>
            <p class="page-sub">
                Thiết lập số lượng khách tối đa Groomer/Vet có thể nhận trong từng ca —
                chỉ áp dụng cho ${daysAhead} ngày tới, trùng khung thời gian khách hàng có thể đặt lịch.
            </p>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>

        <c:if test="${not empty sessionScope.capacityAlertFrom and not empty sessionScope.capacityAlertTo}">
            <div class="alert alert-warning">
                <span class="alert-icon">!</span>
                Có thay đổi nhân sự (nghỉ dài hạn) từ ${sessionScope.capacityAlertFrom} đến
                ${sessionScope.capacityAlertTo}. Các ngày liên quan trong bảng bên dưới được đánh dấu đỏ —
                vui lòng kiểm tra và điều chỉnh GroomCap/VetCap nếu cần.
            </div>
        </c:if>

        <%-- ── Bulk apply: set 1 loạt cho nhiều ngày x nhiều ca ── --%>
        <div class="card">
            <div class="card-header"><span class="card-title">Áp dụng hàng loạt</span></div>
            <div class="card-body">
                <form action="${pageContext.request.contextPath}/manager/capacity" method="post">
                    <input type="hidden" name="action" value="bulkApply">
                    <div class="form-row col-2">
                        <div class="form-group">
                            <label class="form-label">Từ ngày</label>
                            <input type="date" name="bulkFrom" class="form-control no-icon"
                                   min="${fromDate}" max="${toDate}" value="${fromDate}" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Đến ngày</label>
                            <input type="date" name="bulkTo" class="form-control no-icon"
                                   min="${fromDate}" max="${toDate}" value="${toDate}" required>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Áp dụng cho ca</label>
                        <div class="bulk-shift-checks">
                            <c:forEach var="s" items="${shifts}">
                                <label><input type="checkbox" name="shifts" value="${s}" checked> Ca ${s}</label>
                            </c:forEach>
                        </div>
                    </div>
                    <div class="form-row col-2">
                        <div class="form-group">
                            <label class="form-label">GroomCap</label>
                            <input type="number" name="bulkGroomCap" min="0" step="1" class="form-control no-icon" value="0" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">VetCap</label>
                            <input type="number" name="bulkVetCap" min="0" step="1" class="form-control no-icon" value="0" required>
                        </div>
                    </div>
                    <div class="form-hint">Áp dụng cùng 1 cặp GroomCap/VetCap cho toàn bộ ngày x ca đã chọn ở trên, không cần sửa từng ô.</div>
                    <div class="form-actions">
                        <button type="submit" class="btn btn-secondary">Áp dụng hàng loạt</button>
                    </div>
                </form>
            </div>
        </div>

        <%-- ── Bảng chi tiết theo từng ngày x từng ca (vẫn sửa được thủ công) ── --%>
        <form action="${pageContext.request.contextPath}/manager/capacity" method="post">
            <div class="card">
                <div class="card-header"><span class="card-title">Chi tiết từ ${fromDate} đến ${toDate}</span></div>
                <div class="card-body" style="overflow-x:auto;padding:0;">
                    <table class="cap-table">
                        <thead>
                        <tr>
                            <th>Ngày</th>
                            <c:forEach var="s" items="${shifts}">
                                <th>Ca ${s}</th>
                            </c:forEach>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach begin="0" end="${daysAhead - 1}" var="offset">
                            <c:set var="d" value="${fromDate.plusDays(offset)}"/>
                            <c:set var="isAlert" value="${false}"/>
                            <c:if test="${not empty sessionScope.capacityAlertFrom and not empty sessionScope.capacityAlertTo}">
                                <c:if test="${!d.isBefore(sessionScope.capacityAlertFrom) and !d.isAfter(sessionScope.capacityAlertTo)}">
                                </c:if>
                            </c:if>
                            <c:set var="dStr" value="${d}"/>
                            <c:set var="inAlertRange"
                                    value="${not empty sessionScope.capacityAlertFrom
                                             and not empty sessionScope.capacityAlertTo
                                             and dStr.toString() >= sessionScope.capacityAlertFrom
                                             and dStr.toString() <= sessionScope.capacityAlertTo}"/>
                            <tr class="${inAlertRange ? 'alert-row' : ''}">
                                <td class="date-cell">
                                    ${d}
                                    <c:if test="${inAlertRange}">
                                        <span class="cap-alert-note">⚠ Có thay đổi nhân sự</span>
                                    </c:if>
                                </td>
                                <c:forEach var="s" items="${shifts}">
                                    <c:set var="row" value="${existing[d][s]}"/>
                                    <td>
                                        <div class="cap-inputs">
                                            <label>Groomer</label>
                                            <input type="number" min="0" step="1"
                                                   name="groomCap_${d}_${s}"
                                                   value="${not empty row ? row.groomCap : 0}">
                                            <label>Vet</label>
                                            <input type="number" min="0" step="1"
                                                   name="vetCap_${d}_${s}"
                                                   value="${not empty row ? row.vetCap : 0}">
                                        </div>
                                    </td>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="form-actions">
                <button type="submit" class="btn btn-primary">Lưu chi tiết</button>
            </div>
        </form>

        <%-- Xoá cảnh báo sau khi đã hiển thị 1 lần, giống flashSuccess/flashError --%>
        <c:remove var="capacityAlertFrom" scope="session"/>
        <c:remove var="capacityAlertTo" scope="session"/>
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>