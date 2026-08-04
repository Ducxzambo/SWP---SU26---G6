<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="activeTab" value="list" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Yêu Cầu Hoàn Tiền #${refund.refundID} - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="refunds" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Yêu Cầu Hoàn Tiền #${refund.refundID}</h1>
            <p class="page-sub">
                <c:choose>
                    <c:when test="${refund.requested}">
                        <span class="badge badge-warning">Đang chờ xử lý</span>
                    </c:when>
                    <c:when test="${refund.processed}">
                        <span class="badge badge-success">Đã hoàn tiền</span>
                    </c:when>
                    <c:otherwise>
                        <span class="badge badge-error">Đã từ chối</span>
                    </c:otherwise>
                </c:choose>
            </p>
            <div class="page-actions">
                <a href="${pageContext.request.contextPath}/manager/refunds" class="btn btn-outline">Về danh sách</a>
            </div>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/refunds/_subnav.jsp" %>

        <div class="card">
            <div class="card-header"><span class="card-title">Thông tin yêu cầu</span></div>
            <div class="card-body">
                <div class="detail-grid">
                    <div><span class="detail-label">Khách hàng</span>
                        <span class="detail-value"><c:out value="${refund.customerName}"/></span></div>
                    <div><span class="detail-label">Điện thoại</span>
                        <span class="detail-value"><c:out value="${empty refund.customerPhone ? '-' : refund.customerPhone}"/></span></div>
                    <div><span class="detail-label">Thú cưng</span>
                        <span class="detail-value"><c:out value="${refund.petName}"/></span></div>
                    <div><span class="detail-label">Ngày hẹn</span>
                        <span class="detail-value">${refund.formattedAppointmentDate} <span class="badge badge-neutral">${refund.appointmentStatus}</span></span></div>
                    <div><span class="detail-label">Số tiền yêu cầu hoàn</span>
                        <span class="detail-value"><fmt:formatNumber value="${refund.paidAmount}" type="number" groupingUsed="true"/>đ
                            <span class="text-soft">(hoá đơn <fmt:formatNumber value="${refund.totalAmount}" type="number" groupingUsed="true"/>đ)</span></span></div>
                    <div><span class="detail-label">Ngày yêu cầu</span>
                        <span class="detail-value">${refund.formattedRequestedAt}</span></div>
                </div>
                <div class="detail-grid" style="margin-top:18px;">
                    <div style="grid-column:1 / -1;"><span class="detail-label">Lý do (từ khách hàng)</span>
                        <span class="detail-value"><c:out value="${empty refund.reason ? '-' : refund.reason}"/></span></div>
                </div>
            </div>
        </div>

        <%@ include file="/WEB-INF/views/manager/refunds/_invoice_summary.jsp" %>

        <c:if test="${refund.requested}">
            <div class="card">
                <div class="card-header"><span class="card-title">Thông tin ngân hàng nhận tiền</span></div>
                <div class="card-body">
                    <div class="detail-grid">
                        <div><span class="detail-label">Ngân hàng</span>
                            <span class="detail-value"><c:out value="${refund.bankCode}"/></span></div>
                        <div><span class="detail-label">Số tài khoản</span>
                            <span class="detail-value"><c:out value="${refund.accountNumber}"/></span></div>
                        <div><span class="detail-label">Tên chủ tài khoản</span>
                            <span class="detail-value"><c:out value="${refund.accountName}"/></span></div>
                    </div>

                    <c:if test="${not empty vietQrUrl}">
                        <div class="vietqr-block">
                            <img src="${vietQrUrl}" alt="Mã QR chuyển khoản" class="vietqr-image">
                            <p class="text-soft">Quét mã bằng app ngân hàng của bạn để chuyển khoản
                                <strong><fmt:formatNumber value="${refund.paidAmount}" type="number" groupingUsed="true"/>đ</strong>
                                cho khách hàng, sau đó bấm "Đã chuyển khoản — Hoàn tất" bên dưới.</p>
                        </div>
                    </c:if>
                </div>
            </div>

            <div class="card">
                <div class="card-header"><span class="card-title">Xác nhận hoàn tiền</span></div>
                <div class="card-body">
                    <form action="${pageContext.request.contextPath}/manager/refunds/process" method="post"
                          onsubmit="return confirm('Xác nhận bạn ĐÃ chuyển khoản thành công cho khách hàng?');">
                        <input type="hidden" name="refundId" value="${refund.refundID}">
                        <button type="submit" class="btn btn-primary">Đã chuyển khoản — Hoàn tất</button>
                    </form>
                </div>
            </div>

            <div class="card">
                <div class="card-header"><span class="card-title">Từ chối yêu cầu</span></div>
                <div class="card-body">
                    <form action="${pageContext.request.contextPath}/manager/refunds/reject" method="post"
                          onsubmit="return confirm('Xác nhận từ chối yêu cầu hoàn tiền này?');">
                        <input type="hidden" name="refundId" value="${refund.refundID}">
                        <div class="form-group">
                            <label class="form-label" for="rejectReason">
                                Lý do từ chối <span class="required">*</span>
                            </label>
                            <textarea id="rejectReason" name="rejectReason" class="form-control no-icon"
                                      rows="3" required placeholder="VD: Thông tin ngân hàng không hợp lệ..."></textarea>
                        </div>
                        <div class="form-actions">
                            <button type="submit" class="btn btn-outline">Từ chối hoàn tiền</button>
                        </div>
                    </form>
                </div>
            </div>
        </c:if>

        <c:if test="${refund.processed}">
            <div class="alert alert-success">
                <span class="alert-icon">✓</span>
                Đã hoàn tất chuyển khoản lúc ${refund.formattedRefundedAt}. Email thông báo đã được gửi cho khách hàng.
            </div>
        </c:if>

        <c:if test="${refund.rejected}">
            <div class="alert alert-error">
                <span class="alert-icon">x</span>
                Yêu cầu đã bị từ chối. Lý do: <c:out value="${refund.rejectReason}"/>
            </div>
        </c:if>
    </main>
</div>

<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>
