<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Đặt lịch khám – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/booking.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="booking-wrap">
  <div class="booking-header">
    <h1>Đặt lịch khám mới</h1>
    <p>Chọn thú cưng, dịch vụ và một khung giờ</p>
  </div>

  <c:if test="${not empty requestScope.error}">
    <div class="bk-alert bk-alert--error">✗ ${requestScope.error}</div>
  </c:if>
  <c:if test="${empty pets}">
    <div class="bk-alert bk-alert--info">
      Bạn chưa có thú cưng. <a href="${ctx}/pets/new">Thêm thú cưng</a> để đặt lịch.
    </div>
  </c:if>

  <form action="${ctx}/booking/new" method="post" id="bookingForm">
    <input type="hidden" name="isInpatient"     id="isInpatientInput"  value="false">
    <input type="hidden" name="inpatientDate"   id="inpatientDateInput" value="">
    <input type="hidden" name="inpatientPeriod" id="inpatientPeriodInput" value="">
    <input type="hidden" name="slotKey"         id="slotKeyInput"      value="">
    <div id="hiddenInputs"><%-- serviceIds + petIds injected on submit --%></div>

    <div class="bk-layout">

      <%-- ══════════════ LEFT COLUMN ══════════════ --%>
      <div class="bk-panels">

        <%-- STEP 1: Pet chips --%>
        <div class="bk-panel">
          <div class="bk-panel-head">
            <span class="bk-step-num">1</span> Chọn thú cưng
          </div>
          <div class="bk-panel-body">
            <c:choose>
              <c:when test="${not empty pets}">
                <div class="chip-grid" id="petChips">
                  <c:forEach var="pet" items="${pets}">
                    <div class="chip" data-pet-id="${pet.petID}"
                         onclick="togglePet(this, ${pet.petID}, '${pet.name}')">
                      <span class="chip-icon"></span>
                      ${pet.name}
                      <span class="chip-sub">${pet.speciesName}</span>
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

        <%-- STEP 2: Per-pet service panels (rendered by JS) --%>
        <div id="petServicePanels">
          <%-- Populated dynamically when a pet is selected --%>
        </div>

        <%-- Capacity error banner (shown when a group is full) --%>
        <div id="capacityError" class="bk-alert bk-alert--error" style="display:none;"></div>

        <%-- STEP 3a: Slot picker (outpatient) --%>
        <div class="bk-panel" id="slotPanel" style="display:none;">
          <div class="bk-panel-head">
            <span class="bk-step-num">3</span> Chọn khung giờ
            <span class="bk-panel-hint">120 phút · chọn 1 ca</span>
          </div>
          <div class="bk-panel-body">
            <div class="date-tabs" id="dateTabs"></div>
            <div class="slot-grid" id="slotGrid">
              <div class="slot-loading">Vui lòng chọn dịch vụ trước</div>
            </div>
          </div>
        </div>

        <%-- STEP 3b: Inpatient date + period --%>
        <div class="bk-panel" id="inpatientPanel" style="display:none;">
          <div class="bk-panel-head">
            <span class="bk-step-num">3</span> Chọn ngày và buổi nội trú
          </div>
          <div class="bk-panel-body">
            <div class="inpatient-row">
              <div class="form-field">
                <label>Ngày nhập viện</label>
                <input type="date" id="inpDate" min="${today}"
                       class="field-input"
                       onchange="onInpatientDateChange(this.value)">
              </div>
              <div id="inpPeriodWrap" style="display:none;">
                <label class="field-label">Buổi</label>
                <div class="period-btns">
                  <button type="button" class="period-btn" id="btnMorning"
                          onclick="selectPeriod('morning')">
                    Sáng<br><small>08:00 – 12:00</small>
                  </button>
                  <button type="button" class="period-btn" id="btnAfternoon"
                          onclick="selectPeriod('afternoon')">
                    Chiều<br><small>13:30 – 17:30</small>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <%-- Notes --%>
        <div class="bk-panel" id="notesPanel" style="display:none;">
          <div class="bk-panel-head">Ghi chú (không bắt buộc)</div>
          <div class="bk-panel-body">
            <textarea name="notes" rows="3" class="field-textarea"
                      placeholder="Mô tả triệu chứng, yêu cầu đặc biệt..."></textarea>
          </div>
        </div>

      </div><%-- /bk-panels --%>

      <%-- ══════════════ SUMMARY SIDEBAR ══════════════ --%>
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
          <span id="sumPrice" class="sum-price">—</span>
        </div>

        <button type="submit" class="btn-book" id="btnBook" disabled>
          Xem lại và xác nhận →
        </button>
        <p class="sum-hint">Bạn sẽ được xem lại trước khi thanh toán</p>
      </div>

    </div><%-- /bk-layout --%>
  </form>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script>
  /*
    Sửa lỗi: bản gốc viết trực tiếp
      const CATS       = ${categoriesJson};
      const SLOTS_DATA = ${slotsJson};
      const TODAY      = '${today}';
    Nếu các biến EL "categoriesJson" / "slotsJson" không có trong scope, EL sẽ
    render ra chuỗi rỗng, biến dòng trên thành "const CATS = ;" — lỗi cú pháp
    JavaScript khiến TOÀN BỘ trang đặt lịch không hoạt động (vì đây là file lớn
    nhất, mọi tương tác chọn thú cưng/dịch vụ/khung giờ đều phụ thuộc vào nó).
    Sửa lại bằng cách luôn cung cấp giá trị mặc định hợp lệ ngay trong EL.
  */
  window.BOOKING_CATEGORIES_JSON = ${not empty categoriesJson ? categoriesJson : '[]'};
  window.BOOKING_SLOTS_JSON      = ${not empty slotsJson ? slotsJson : '{}'};
  window.BOOKING_TODAY           = '${today}';
  window.APP_CTX                 = '${ctx}';
</script>
<script src="${ctx}/js/booking-new.js"></script>
</body>
</html>
