<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Đổi lịch khám – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/appointments.css">
</head>
<body>

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="detail-wrap">

  <a href="${ctx}/appointments/detail?id=${appt.appointmentID}" class="detail-back">← Quay lại chi tiết</a>

  <div class="detail-hero" style="margin-bottom:24px;">
    <div class="detail-hero-left">
      <div class="detail-service-label">Đổi lịch hẹn</div>
      <h1 class="detail-service-name">${appt.serviceName}</h1>
      <div class="detail-hero-meta">
        <span>${appt.petName}</span>
        <span>Lịch cũ: <strong>${appt.appointmentDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))}</strong>
          lúc <strong>${appt.startTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}</strong></span>
      </div>
    </div>
    <div class="detail-hero-right">
      <span class="status-badge status-badge--lg status-${fn:toLowerCase(appt.status)}">${appt.status}</span>
    </div>
  </div>

  <div class="reschedule-notice">
    Chỉ hiển thị các khung giờ còn trống và cách hiện tại ít nhất <strong>12 giờ</strong>.
    Sau khi đổi, trạng thái sẽ chuyển về <strong>Pending</strong>.
  </div>

  <form action="${ctx}/appointments/reschedule" method="post" id="rescheduleForm">
    <input type="hidden" name="appointmentId" value="${appt.appointmentID}">
    <input type="hidden" id="slotKeyInput"    name="slotKey" value="">

    <!-- Date tabs -->
    <div class="bk-panel" style="margin-bottom:0;border-bottom-left-radius:0;border-bottom-right-radius:0;">
      <div class="bk-panel-head">Chọn ngày và khung giờ mới</div>
      <div class="bk-panel-body">
        <div class="date-tabs" id="dateTabs"></div>
        <div class="slot-grid" id="slotGrid">
          <div style="grid-column:1/-1;color:var(--warm-gray);font-size:13.5px;">Đang tải...</div>
        </div>
      </div>
    </div>

    <!-- Selection summary + submit -->
    <div class="reschedule-footer">
      <div class="reschedule-selection" id="selectionSummary">
        <span id="selectionText" style="color:var(--warm-gray);">Chưa chọn khung giờ nào</span>
      </div>
      <button type="submit" class="btn-confirm-reschedule" id="btnReschedule" disabled>
        ✓ Xác nhận đổi lịch
      </button>
    </div>

  </form>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script>
const SLOTS_DATA = ${slotsJson};
const DAYS_VN    = ['CN','T2','T3','T4','T5','T6','T7'];

let selectedKey     = '';
let selectedDisplay = '';

// ── Build date tabs ──────────────────────────────────────────────────────
function renderDateTabs() {
  const tabsEl = document.getElementById('dateTabs');
  const dates  = Object.keys(SLOTS_DATA).sort();

  if (dates.length === 0) {
    tabsEl.innerHTML = '<span style="color:var(--warm-gray);font-size:13.5px;padding:8px 0;display:block;">Không có lịch trống phù hợp trong 30 ngày tới.</span>';
    document.getElementById('slotGrid').innerHTML = '';
    return;
  }

  dates.forEach((dateStr, i) => {
    const d   = new Date(dateStr + 'T00:00:00');
    const tab = document.createElement('div');
    tab.className = 'date-tab' + (i === 0 ? ' active' : '');
    tab.dataset.date = dateStr;
    tab.innerHTML = `<div class="dt-day">${DAYS_VN[d.getDay()]}</div>
                     <div class="dt-date">${d.getDate().toString().padStart(2,'0')}/${(d.getMonth()+1).toString().padStart(2,'0')}</div>`;
    tab.onclick = () => {
      document.querySelectorAll('.date-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      renderSlots(dateStr);
    };
    tabsEl.appendChild(tab);
  });

  renderSlots(dates[0]);
}

// ── Build slot buttons ───────────────────────────────────────────────────
function renderSlots(dateStr) {
  const grid  = document.getElementById('slotGrid');
  const slots = SLOTS_DATA[dateStr] || [];
  grid.innerHTML = '';

  if (slots.length === 0) {
    grid.innerHTML = '<div style="grid-column:1/-1;color:var(--warm-gray);font-size:13.5px;padding:8px 0;">Không có ca trống.</div>';
    return;
  }

  slots.forEach(slot => {
    const btn = document.createElement('div');
    const isSelected = slot.key === selectedKey;
    btn.className = 'slot-btn'
      + (!slot.available ? ' booked' : '')
      + (isSelected      ? ' selected' : '');
    btn.textContent = slot.display;

    if (slot.available) {
      btn.onclick = () => selectSlot(slot, btn);
    } else {
      btn.title = 'Đã được đặt';
    }
    grid.appendChild(btn);
  });
}

function selectSlot(slot, btn) {
  // Deselect previous
  document.querySelectorAll('.slot-btn.selected').forEach(b => b.classList.remove('selected'));
  selectedKey     = slot.key;
  selectedDisplay = slot.display;
  btn.classList.add('selected');
  document.getElementById('slotKeyInput').value = slot.key;

  // Parse display date from active tab
  const activeTab = document.querySelector('.date-tab.active');
  const [yy,mm,dd] = activeTab.dataset.date.split('-');
  const dateLabel  = `${dd}/${mm}/${yy}`;
  const dayLabel   = activeTab.querySelector('.dt-day').textContent;

  document.getElementById('selectionText').innerHTML =
    `✓ Đã chọn: <strong>${dayLabel} ${dateLabel} – ${slot.display}</strong>`;
  document.getElementById('selectionText').style.color = 'var(--green-700)';
  document.getElementById('btnReschedule').disabled = false;
}

// Confirm before submit
document.getElementById('rescheduleForm').addEventListener('submit', function(e) {
  if (!selectedKey) { e.preventDefault(); return; }
  const ok = confirm(`Xác nhận đổi sang:\n${document.getElementById('selectionText').textContent.replace('✓ Đã chọn: ','')}\n\nTrạng thái sẽ về "Pending". Tiếp tục?`);
  if (!ok) e.preventDefault();
});

renderDateTabs();
</script>
</body>
</html>
