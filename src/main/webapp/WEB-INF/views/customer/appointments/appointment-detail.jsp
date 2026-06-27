<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Chi tiết lịch khám – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/appointments.css">
  <link rel="stylesheet" href="${ctx}/css/reviews.css">
</head>
<body>

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="detail-wrap">

  <!-- Back link -->
  <a href="${ctx}/appointments" class="detail-back">← Quay lại danh sách</a>

  <!-- Header card -->
  <div class="detail-hero">
      <div class="detail-hero-left">
        <div class="detail-service-label">${appt.categoryName}</div>
        <h1 class="detail-service-name">${appt.serviceName}</h1>
        <div class="detail-hero-meta">
          <span><strong>${appt.petName}</strong></span>
          <c:if test="${not empty appt.appointmentDate}">
            <span><strong>${appt.formattedAppointmentDate}</strong></span>
          </c:if>
          <c:if test="${not empty appt.startTime and not empty appt.endTime}">
            <span><strong>${appt.formattedStartTime} – ${appt.formattedEndTime}</strong></span>
          </c:if>
          <c:if test="${not empty appt.staffName}">
            <span><strong>${appt.staffName}</strong></span>
          </c:if>
        </div>
      </div>
    <div class="detail-hero-right">
      <span class="status-badge status-badge--lg status-${fn:toLowerCase(appt.status)}">${appt.status}</span>
    </div>
  </div>

  <!-- Action buttons (only for Pending/Confirmed and canModify) -->
  <c:if test="${appt.canModify()}">
    <div class="detail-actions">
      <a href="${ctx}/appointments/reschedule?id=${appt.appointmentID}" class="btn-reschedule">
        Đổi lịch
      </a>
      <button class="btn-cancel-open" onclick="openCancelModal()">
        Huỷ lịch hẹn
      </button>
    </div>
  </c:if>
  <c:if test="${(appt.status eq 'Pending' or appt.status eq 'Confirmed') and !appt.canModify()}">
    <div class="detail-lock-notice">
      Không thể chỉnh sửa — lịch hẹn trong vòng 12 giờ tới
    </div>
  </c:if>

  <!-- Pay now button for Pending appointments with unpaid invoice -->
  <c:if test="${appt.status eq 'Pending' and not empty invoice and invoice.status eq 'Unpaid'}">
    <div style="margin-bottom:20px;">
      <a href="${ctx}/appointments/pay?id=${appt.appointmentID}"
         class="btn-pay-now">
        Thanh toán ngay để xác nhận lịch hẹn
      </a>
    </div>
  </c:if>

  <div class="detail-grid">

    <!-- Notes -->
    <c:if test="${not empty appt.notes}">
      <div class="detail-section">
        <div class="detail-section-head">Ghi chú</div>
        <div class="detail-section-body">
          <p class="detail-note-text">${appt.notes}</p>
        </div>
      </div>
    </c:if>

    <!-- Medical Record -->
    <c:choose>
      <c:when test="${not empty medicalRecord}">
        <div class="detail-section">
          <div class="detail-section-head">Bệnh án</div>
          <div class="detail-section-body">
            <div class="detail-field-grid">
              <c:if test="${medicalRecord.weight != null}">
                <div class="detail-field">
                  <span class="field-label">Cân nặng</span>
                  <span class="field-value">${medicalRecord.weight} kg</span>
                </div>
              </c:if>
              <c:if test="${medicalRecord.temperature != null}">
                <div class="detail-field">
                  <span class="field-label">Thân nhiệt</span>
                  <span class="field-value">${medicalRecord.temperature} °C</span>
                </div>
              </c:if>
              <c:if test="${not empty medicalRecord.staffName}">
                <div class="detail-field">
                  <span class="field-label">Nhân viên phụ trách</span>
                  <span class="field-value">${medicalRecord.staffName}</span>
                </div>
              </c:if>
            </div>

            <c:if test="${not empty medicalRecord.symptoms}">
              <div class="detail-block">
                <div class="detail-block-label">Triệu chứng</div>
                <div class="detail-block-text">${medicalRecord.symptoms}</div>
              </div>
            </c:if>
            <c:if test="${not empty medicalRecord.diagnosis}">
              <div class="detail-block">
                <div class="detail-block-label">Chẩn đoán</div>
                <div class="detail-block-text">${medicalRecord.diagnosis}</div>
              </div>
            </c:if>
            <c:if test="${not empty medicalRecord.treatmentPlan}">
              <div class="detail-block">
                <div class="detail-block-label">Phác đồ điều trị</div>
                <div class="detail-block-text">${medicalRecord.treatmentPlan}</div>
              </div>
            </c:if>
          </div>
        </div>

        <!-- Prescriptions -->
        <c:if test="${not empty medicalRecord.prescriptions}">
          <div class="detail-section">
            <div class="detail-section-head">Đơn thuốc</div>
            <div class="detail-section-body" style="padding:0;">
              <table class="detail-table">
                <thead>
                  <tr>
                    <th>Thuốc</th>
                    <th>Đơn vị</th>
                    <th>Liều dùng</th>
                    <th>Số lượng</th>
                    <th style="text-align:right;">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="rx" items="${medicalRecord.prescriptions}">
                    <tr>
                      <td class="td-name">${rx.medicineName}</td>
                      <td>${rx.unit}</td>
                      <td class="td-dosage">${rx.dosage}</td>
                      <td>${rx.quantity}</td>
                      <td style="text-align:right;">
                        <fmt:formatNumber value="${rx.lineTotal}" type="number" groupingUsed="true"/>₫
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </div>
        </c:if>
      </c:when>
      <c:otherwise>
        <c:if test="${appt.status eq 'Done'}">
          <div class="detail-section">
            <div class="detail-section-head">Bệnh án</div>
            <div class="detail-section-body">
              <p class="detail-empty-text">Bệnh án chưa được cập nhật.</p>
            </div>
          </div>
        </c:if>
      </c:otherwise>
    </c:choose>

    <!-- Invoice & Payments -->
    <c:if test="${not empty invoice}">
      <div class="detail-section">
        <div class="detail-section-head">
          Hoá đơn
          <span class="inv-status inv-status--${fn:toLowerCase(invoice.status)}">${invoice.status}</span>
        </div>
        <div class="detail-section-body" style="padding:0;">

          <c:if test="${not empty invoice.items}">
            <table class="detail-table">
              <thead>
                <tr>
                  <th>Loại</th>
                  <th>Mô tả</th>
                  <th>SL</th>
                  <th style="text-align:right;">Đơn giá</th>
                  <th style="text-align:right;">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="item" items="${invoice.items}">
                  <tr>
                    <td><span class="item-type-badge">${item.itemType}</span></td>
                    <td>${item.description}</td>
                    <td>${item.quantity}</td>
                    <td style="text-align:right;"><fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true"/>₫</td>
                    <td style="text-align:right;font-weight:600;"><fmt:formatNumber value="${item.lineTotal}" type="number" groupingUsed="true"/>₫</td>
                  </tr>
                </c:forEach>
              </tbody>
              <tfoot>
                <tr class="tfoot-total">
                  <td colspan="4" style="text-align:right;font-weight:600;">Tổng cộng</td>
                  <td style="text-align:right;font-weight:700;font-size:16px;color:var(--green-700);">
                    <fmt:formatNumber value="${invoice.totalAmount}" type="number" groupingUsed="true"/>₫
                  </td>
                </tr>
              </tfoot>
            </table>
          </c:if>

          <c:if test="${not empty invoice.payments}">
            <div style="padding:16px 20px;border-top:1px solid var(--border);">
              <div style="font-size:13px;font-weight:600;color:var(--warm-gray);
                          text-transform:uppercase;letter-spacing:.5px;margin-bottom:10px;">
                Lịch sử thanh toán
              </div>
               <c:forEach var="pay" items="${invoice.payments}">
                 <div class="payment-row">
                   <span class="pay-method">${pay.method}</span>
                   <span class="pay-amount"><fmt:formatNumber value="${pay.amount}" type="number" groupingUsed="true"/>₫</span>
                   <span class="pay-by">bởi ${pay.processedByName}</span>
                   <c:if test="${not empty pay.paidAt}">
                     <span class="pay-date">${pay.formattedPaidAt}</span>
                   </c:if>
                 </div>
               </c:forEach>
            </div>
          </c:if>

        </div>
      </div>
    </c:if>

  </div><%-- /detail-grid --%>
  <%-- ── Review section (chỉ hiện khi status = Done) ─── --%>
    <%@ include file="/WEB-INF/views/customer/appointments/review-form-fragment.jsp" %>
</div><%-- /detail-wrap --%>

<!-- Cancel modal -->
<c:if test="${appt.canModify()}">
<div class="modal-overlay" id="cancelModal">
  <div class="modal-box">
    <div class="modal-head">
      <span class="modal-icon">⚠️</span>
      <h2>Xác nhận huỷ lịch hẹn</h2>
    </div>
      <div class="modal-body">
        <p>Bạn sắp huỷ lịch khám:</p>
        <div class="modal-appt-summary">
          <strong>${appt.serviceName}</strong> —
          <c:if test="${not empty appt.appointmentDate}">
            ${appt.formattedAppointmentDate}
          </c:if>
          <c:if test="${not empty appt.startTime}">
            lúc ${appt.formattedStartTime}
          </c:if>
        </div>

      <form action="${ctx}/appointments/cancel" method="post" id="cancelForm">
        <input type="hidden" name="appointmentId" value="${appt.appointmentID}">

        <div class="modal-field">
          <label for="cancelReason">Lý do huỷ <span style="color:var(--warm-gray);font-size:12px;">(không bắt buộc)</span></label>
          <textarea id="cancelReason" name="cancelReason" rows="3"
                    placeholder="Ví dụ: Thú cưng đã khỏi, thay đổi lịch cá nhân..."></textarea>
        </div>

        <div class="modal-confirm-check">
          <input type="checkbox" id="confirmCheck" onchange="document.getElementById('btnConfirmCancel').disabled = !this.checked">
          <label for="confirmCheck">Tôi xác nhận muốn huỷ lịch hẹn này</label>
        </div>
      </form>
    </div>
    <div class="modal-foot">
      <button class="modal-btn-secondary" onclick="closeCancelModal()">Không, quay lại</button>
      <button class="modal-btn-danger" id="btnConfirmCancel" disabled
              onclick="document.getElementById('cancelForm').submit()">
        Xác nhận huỷ
      </button>
    </div>
  </div>
</div>
</c:if>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script src="${ctx}/js/appointment.js"></script>
</body>
</html>
