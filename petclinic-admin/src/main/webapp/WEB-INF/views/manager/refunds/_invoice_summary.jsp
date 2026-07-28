<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%-- Shared Invoice + InvoiceItems + Payments summary block. The including
     page sets a request attribute "invoice" (com.petclinic.model.Invoice,
     may be null) beforehand. Read-only - used on both the refund Detail
     screen and the "create new request" form's preview. --%>
<c:choose>
    <c:when test="${empty invoice}">
        <div class="card">
            <div class="card-header"><span class="card-title">Hoá đơn</span></div>
            <div class="empty-state compact"><p>Không tìm thấy hoá đơn cho lịch hẹn này.</p></div>
        </div>
    </c:when>
    <c:otherwise>
        <div class="card">
            <div class="card-header">
                <span class="card-title">Hoá đơn #${invoice.invoiceID}</span>
                <span class="badge badge-neutral">${invoice.status}</span>
            </div>
            <div class="card-body">
                <div class="detail-grid">
                    <div><span class="detail-label">Tổng tiền hoá đơn</span>
                        <span class="detail-value"><fmt:formatNumber value="${invoice.totalAmount}" type="number" groupingUsed="true"/>đ</span></div>
                    <div><span class="detail-label">Phụ phí khác</span>
                        <span class="detail-value"><c:out value="${empty invoice.otherFees ? '-' : invoice.otherFees}"/></span></div>
                </div>
            </div>
            <c:if test="${not empty invoice.items}">
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>Loại</th>
                        <th>Mô tả</th>
                        <th>SL</th>
                        <th>Đơn giá</th>
                        <th>Thành tiền</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${invoice.items}" var="it">
                        <tr>
                            <td><span class="badge badge-neutral">${it.itemType}</span></td>
                            <td><c:out value="${it.description}"/></td>
                            <td>${it.quantity}</td>
                            <td><fmt:formatNumber value="${it.unitPrice}" type="number" groupingUsed="true"/></td>
                            <td><fmt:formatNumber value="${it.lineTotal}" type="number" groupingUsed="true"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:if>
        </div>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Lịch sử thanh toán</span>
                <span class="text-soft">${fn:length(invoice.payments)} giao dịch</span>
            </div>
            <c:choose>
                <c:when test="${empty invoice.payments}">
                    <div class="empty-state compact"><p>Chưa có giao dịch thanh toán nào.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>Thời gian</th>
                            <th>Phương thức</th>
                            <th>Số tiền</th>
                            <th>Người xử lý</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${invoice.payments}" var="p">
                            <tr>
                                <td>${p.formattedPaidAt}</td>
                                <td><c:out value="${p.method}"/></td>
                                <td><fmt:formatNumber value="${p.amount}" type="number" groupingUsed="true"/>đ</td>
                                <td><c:out value="${empty p.processedByName ? '-' : p.processedByName}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
    </c:otherwise>
</c:choose>
