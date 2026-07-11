<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Đặt lịch khám - PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/booking.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="booking-wrap">
  <div class="booking-header">
    <h1>Đặt lịch khám mới</h1>
    <p>Chọn thú cưng, các dịch vụ và khung giờ phù hợp</p>
  </div>

  <c:if test="${not empty requestScope.error}">
    <div class="bk-alert bk-alert--error">${requestScope.error}</div>
  </c:if>
  <c:if test="${empty pets}">
    <div class="bk-alert bk-alert--info">
      Bạn chưa có thú cưng. <a href="${ctx}/pets/new">Thêm thú cưng</a> để đặt lịch.
    </div>
  </c:if>

  <form action="${ctx}/booking/new" method="post" id="bookingForm">
    <input type="hidden" name="isInpatient" id="isInpatientInput" value="false">
    <input type="hidden" name="inpatientDate" id="inpatientDateInput" value="">
    <input type="hidden" name="inpatientPeriod" id="inpatientPeriodInput" value="">
    <input type="hidden" name="slotKey" id="slotKeyInput" value="">
    <input type="hidden" name="bookingPayload" id="bookingPayloadInput" value="">
    <div id="hiddenInputs"></div>

    <div class="bk-layout">
      <div class="bk-panels">
        <div class="bk-panel">
          <div class="bk-panel-head"><span class="bk-step-num">1</span> Loại lịch hẹn</div>
          <div class="bk-panel-body">
            <div class="period-btns">
              <button type="button" class="period-btn active" id="btnModeNormal" onclick="selectMode('normal')">
                Khám / Spa / Vaccine<br><small>Chọn dịch vụ &amp; khung giờ</small>
              </button>
              <button type="button" class="period-btn" id="btnModeInpatient" onclick="selectMode('inpatient')">
                Đặt nội trú<br><small>Nhập viện theo buổi sáng/chiều</small>
              </button>
            </div>
          </div>
        </div>

        <div id="normalModePanels">
          <div class="bk-panel">
            <div class="bk-panel-head"><span class="bk-step-num">2</span> Chọn thú cưng</div>
            <div class="bk-panel-body">
              <c:choose>
                <c:when test="${not empty pets}">
                  <div class="chip-grid" id="mainPetChips">
                    <c:forEach var="pet" items="${pets}">
                      <div class="chip" data-pet-id="${pet.petID}" data-pet-name="${pet.name}"
                           onclick="toggleMainPet(this, ${pet.petID}, this.dataset.petName)">
                        ${pet.name} <span class="chip-sub">${pet.speciesName}</span>
                      </div>
                    </c:forEach>
                  </div>
                </c:when>
                <c:otherwise>
                  <p class="bk-empty">Chưa có thú cưng. <a href="${ctx}/pets/new">Thêm ngay</a></p>
                </c:otherwise>
              </c:choose>
            </div>
          </div>

          <div class="bk-panel" id="petConfigPanel" style="display:none;">
            <div class="bk-panel-head"><span class="bk-step-num">3</span> Chọn dịch vụ
              <span class="bk-panel-hint">chọn được nhiều nhóm &amp; dịch vụ</span>
            </div>
            <div class="bk-panel-body">
              <div class="bk-alert bk-alert--info" style="margin-bottom:14px;">
                Chọn 1 hoặc nhiều nhóm dịch vụ, sau đó chọn 1 hoặc nhiều dịch vụ (hoặc vaccine) trong từng nhóm. Bạn có thể mở rộng thêm khi đến khám.
              </div>
              <div id="petConfigsContainer"></div>
            </div>
          </div>

          <div id="capacityError" class="bk-alert bk-alert--error" style="display:none;"></div>

          <div class="bk-panel" id="slotPanel" style="display:none;">
            <div class="bk-panel-head">
              <span class="bk-step-num">4</span> Chọn khung giờ
              <span class="bk-panel-hint">120 phút · chọn 1 ca</span>
            </div>
            <div class="bk-panel-body">
              <div class="date-tabs" id="dateTabs"></div>
              <div class="slot-grid" id="slotGrid">
                <div class="slot-loading">Vui lòng chọn thú cưng và dịch vụ trước</div>
              </div>
            </div>
          </div>
        </div>

        <div id="inpatientModePanels" style="display:none;">
          <div class="bk-panel" id="inpPetPanel">
            <div class="bk-panel-head"><span class="bk-step-num">2</span> Chọn thú cưng</div>
            <div class="bk-panel-body">
              <div class="chip-grid" id="inpPetChips">
                <c:forEach var="pet" items="${pets}">
                  <div class="chip" data-pet-id="${pet.petID}" onclick="toggleInpatientPet(this, ${pet.petID})">
                    ${pet.name} <span class="chip-sub">${pet.speciesName}</span>
                  </div>
                </c:forEach>
              </div>
            </div>
          </div>

          <div class="bk-panel" id="inpatientPanel">
            <div class="bk-panel-head"><span class="bk-step-num">3</span> Chọn ngày và buổi nội trú</div>
            <div class="bk-panel-body">
              <div class="inpatient-row">
                <div class="form-field">
                  <label>Ngày nhập viện</label>
                  <input type="date" id="inpDate" min="${today}" class="field-input"
                         onchange="onInpatientDateChange(this.value)">
                </div>
                <div>
                  <label class="field-label">Buổi</label>
                  <div class="period-btns">
                    <button type="button" class="period-btn" id="btnMorning" onclick="selectPeriod('morning')">
                      Sáng<br><small>08:00 - 12:00</small>
                    </button>
                    <button type="button" class="period-btn" id="btnAfternoon" onclick="selectPeriod('afternoon')">
                      Chiều<br><small>13:30 - 17:30</small>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="bk-panel" id="notesPanel" style="display:none;">
          <div class="bk-panel-head">Ghi chú (không bắt buộc)</div>
          <div class="bk-panel-body">
            <textarea name="notes" rows="3" class="field-textarea"
                      placeholder="Mô tả triệu chứng, yêu cầu đặc biệt..."></textarea>
          </div>
        </div>
      </div>

      <div class="bk-summary">
        <h3>Tóm tắt đặt lịch</h3>

        <div id="sumPetSection" class="sum-section" style="display:none;">
          <div class="sum-section-label">Thú cưng</div>
          <div class="sum-section-body" id="sumPets"></div>
        </div>

        <div id="sumSvcSection" class="sum-section" style="display:none;">
          <div class="sum-section-label">Dịch vụ</div>
          <div class="sum-section-body" id="sumSvcs"></div>
        </div>

        <div id="sumSlotSection" class="sum-section" style="display:none;">
          <div class="sum-section-label">Thời gian</div>
          <div class="sum-section-body" id="sumSlot"></div>
        </div>

        <div class="sum-total-row">
          <span>Ước tính</span>
          <span id="sumPrice" class="sum-price">-</span>
        </div>

        <button type="submit" class="btn-book" id="btnBook" disabled>
          Xem lại và xác nhận →
        </button>
        <p class="sum-hint">Bạn sẽ được xem lại trước khi thanh toán</p>
      </div>
    </div>
  </form>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script>
  window.BOOKING_TODAY = '${today}';
  window.BOOKING_PREFILL_PET = '${prefillPet}';
  window.BOOKING_PREFILL_CAT = '${prefillCat}';
  window.APP_CTX = '${ctx}';
</script>
<script src="${ctx}/js/booking-new.js"></script>
</body>
</html>
