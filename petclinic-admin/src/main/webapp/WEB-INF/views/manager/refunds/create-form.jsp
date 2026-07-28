<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="activeTab" value="create" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tạo Yêu Cầu Hoàn Tiền - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="refunds" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Tạo Yêu Cầu Hoàn Tiền</h1>
            <p class="page-sub">Bước 2/2 — Nhập thông tin hoàn tiền cho lịch hẹn #${appt.appointmentID}</p>
            <div class="page-actions">
                <a href="${pageContext.request.contextPath}/manager/refunds/create" class="btn btn-outline">
                    ← Chọn lịch hẹn khác
                </a>
            </div>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/refunds/_subnav.jsp" %>

        <div class="card">
            <div class="card-header"><span class="card-title">Lịch hẹn đã chọn</span></div>
            <div class="card-body">
                <div class="detail-grid">
                    <div><span class="detail-label">Khách hàng</span>
                        <span class="detail-value"><c:out value="${appt.customerName}"/></span></div>
                    <div><span class="detail-label">Thú cưng</span>
                        <span class="detail-value"><c:out value="${appt.petName}"/></span></div>
                    <div><span class="detail-label">Ngày hẹn</span>
                        <span class="detail-value">${appt.formattedAppointmentDate}</span></div>
                    <div><span class="detail-label">Trạng thái</span>
                        <span class="detail-value"><span class="badge badge-neutral">${appt.status}</span></span></div>
                </div>
            </div>
        </div>

        <%@ include file="/WEB-INF/views/manager/refunds/_invoice_summary.jsp" %>

        <div class="card">
            <div class="card-header"><span class="card-title">Thông tin hoàn tiền</span></div>
            <div class="card-body">
                <form action="${pageContext.request.contextPath}/manager/refunds/create" method="post" id="mrfForm">
                    <input type="hidden" name="appointmentId" value="${appt.appointmentID}">

                    <div class="form-group">
                        <label class="form-label" for="mrfReason">
                            Lý do hoàn tiền <span class="required">*</span>
                        </label>
                        <textarea id="mrfReason" name="reason" class="form-control no-icon" rows="3" required></textarea>
                    </div>

                    <div class="form-row col-2">
                        <div class="form-group">
                            <label class="form-label" for="mrfAmount">
                                Số tiền hoàn <span class="required">*</span>
                            </label>
                            <input type="number" id="mrfAmount" name="amount" class="form-control no-icon"
                                   min="1" step="1">
                        </div>
                        <div class="form-group" style="align-self:end;padding-bottom:10px;">
                            <label style="display:flex;align-items:center;gap:8px;font-size:14px;">
                                <input type="checkbox" id="mrfFullRefund" name="fullRefund" value="true"
                                       onchange="mrfToggleFullRefund(this.checked)" checked>
                                Hoàn tiền 100% số tiền đã thanh toán
                            </label>
                        </div>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Thông tin ngân hàng nhận tiền hoàn</label>
                        <div class="refund-method-tabs">
                            <button type="button" class="refund-method-btn active" data-method="qr"
                                    onclick="mrfSwitchMethod('qr')">Quét ảnh mã QR</button>
                            <button type="button" class="refund-method-btn" data-method="manual"
                                    onclick="mrfSwitchMethod('manual')">Nhập thủ công</button>
                        </div>
                    </div>

                    <div id="mrfQrBlock" class="form-group">
                        <input type="file" id="mrfQrFile" accept="image/*"
                               onchange="mrfHandleQrFile(this.files[0])">
                        <p id="mrfQrStatus" class="refund-qr-status"></p>
                        <canvas id="mrfQrCanvas" style="display:none;"></canvas>
                    </div>

                    <div class="form-row col-3">
                        <div class="form-group">
                            <label class="form-label" for="mrfBankSelect">Ngân hàng</label>
                            <select id="mrfBankSelect" name="bankCode" class="form-control no-icon">
                                <option value="">-- Chọn ngân hàng --</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="mrfAccountNumber">Số tài khoản</label>
                            <input type="text" id="mrfAccountNumber" name="accountNumber" class="form-control no-icon">
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="mrfAccountName">Tên chủ tài khoản</label>
                            <input type="text" id="mrfAccountName" name="accountName" class="form-control no-icon">
                        </div>
                    </div>

                    <div class="form-actions">
                        <button type="submit" class="btn btn-primary">Tạo yêu cầu &amp; Tiếp tục hoàn tiền</button>
                    </div>
                </form>
            </div>
        </div>
    </main>
</div>

<script src="https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.js"></script>
<script src="${pageContext.request.contextPath}/js/manager-refund-qr.js"></script>
<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>
