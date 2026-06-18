/**
 * appointment.js
 * Gộp toàn bộ logic JS cho cụm trang "Lịch khám":
 *   1) appointments.jsp         — chuyển tab Sắp tới / Lịch sử
 *   2) appointment-detail.jsp   — modal xác nhận huỷ lịch hẹn
 *   3) appointment-reschedule.jsp — chọn khung giờ / ngày-buổi nội trú mới
 *
 * Mỗi phần được bọc trong IIFE riêng để không xung đột biến/scope với nhau,
 * vì các trang trên không cùng lúc render chung (chỉ 1 trang được mở tại 1 thời
 * điểm), nhưng vẫn an toàn nếu sau này được gộp chung hoặc load nhiều lần.
 *
 * Phần (3) chỉ chạy nếu các biến toàn cục sau được JSP khai báo trước khi load
 * file này (xem appointment-reschedule.jsp):
 *   window.RESCHEDULE_SLOTS_DATA   (object: "yyyy-MM-dd" -> [slot,...])
 *   window.RESCHEDULE_IS_INPATIENT (boolean)
 */

/* ============================================================
 * 1) APPOINTMENTS LIST — chuyển tab Sắp tới / Lịch sử
 * ============================================================ */
(function () {
  function switchTab(name, btn) {
    document.querySelectorAll('.appt-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.appt-tab-panel').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');

    const panel = document.getElementById('tab-' + name);
    if (panel) panel.classList.add('active');
  }

  // Expose for inline onclick in appointments.jsp
  window.switchTab = switchTab;
})();

/* ============================================================
 * 2) APPOINTMENT DETAIL — modal xác nhận huỷ lịch hẹn
 * ============================================================ */
(function () {
  function openCancelModal() {
    const modal = document.getElementById('cancelModal');
    if (!modal) return;
    modal.classList.add('open');
    document.body.style.overflow = 'hidden';
  }

  function closeCancelModal() {
    const modal = document.getElementById('cancelModal');
    if (!modal) return;
    modal.classList.remove('open');
    document.body.style.overflow = '';

    // Reset state
    const cb = document.getElementById('confirmCheck');
    if (cb) {
      cb.checked = false;
      const btnConfirm = document.getElementById('btnConfirmCancel');
      if (btnConfirm) btnConfirm.disabled = true;
    }
    const ta = document.getElementById('cancelReason');
    if (ta) ta.value = '';
  }

  // Expose for inline onclick in appointment-detail.jsp
  window.openCancelModal = openCancelModal;
  window.closeCancelModal = closeCancelModal;

  // Close on overlay click
  document.addEventListener('DOMContentLoaded', function () {
    const overlay = document.getElementById('cancelModal');
    if (overlay) {
      overlay.addEventListener('click', e => {
        if (e.target === overlay) closeCancelModal();
      });
    }
  });
})();

/* ============================================================
 * 3) APPOINTMENT RESCHEDULE — chọn khung giờ / ngày-buổi mới
 * ============================================================ */
(function () {
  // Chỉ chạy phần này nếu trang hiện tại có form đổi lịch (#rescheduleForm),
  // để tránh log lỗi không cần thiết trên các trang khác cùng load file gộp này.
  if (!document.getElementById('rescheduleForm')) return;

  const SLOTS_DATA   = window.RESCHEDULE_SLOTS_DATA || {};
  const IS_INPATIENT = !!window.RESCHEDULE_IS_INPATIENT;
  const DAYS_VN       = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

  let selectedKey = '', selectedDisplay = '';

  // ══════ OUTPATIENT: slot grid ══════
  function renderDateTabs() {
    const el = document.getElementById('dateTabs');
    if (!el) return;
    const dates = Object.keys(SLOTS_DATA).sort();
    el.innerHTML = '';

    if (!dates.length) {
      el.innerHTML = '<span class="slot-loading">Không có lịch trống phù hợp trong 30 ngày tới.</span>';
      const grid = document.getElementById('slotGrid');
      if (grid) grid.innerHTML = '';
      return;
    }

    dates.forEach((ds, i) => {
      const d = new Date(ds + 'T00:00:00');
      const tab = document.createElement('div');
      tab.className = 'date-tab' + (i === 0 ? ' active' : '');
      tab.dataset.date = ds;
      tab.innerHTML = `<div class="dt-day">${DAYS_VN[d.getDay()]}</div>
                        <div class="dt-date">${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}</div>`;
      tab.onclick = () => {
        document.querySelectorAll('.date-tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        renderSlots(ds);
      };
      el.appendChild(tab);
    });
    renderSlots(dates[0]);
  }

  function renderSlots(ds) {
    const grid = document.getElementById('slotGrid');
    if (!grid) return;
    const slots = SLOTS_DATA[ds] || [];
    grid.innerHTML = '';

    if (!slots.length) {
      grid.innerHTML = '<div class="slot-loading">Không có ca trống.</div>';
      return;
    }

    slots.forEach(slot => {
      const wrap = document.createElement('div');
      const fill = Math.min(100, slot.fill || 0);
      const fillColor = fill >= 100 ? 'var(--red-err)' : fill >= 70 ? 'var(--amber)' : 'var(--green-400)';
      wrap.className = 'slot-card'
        + (!slot.available ? ' booked' : '')
        + (slot.key === selectedKey ? ' selected' : '');
      wrap.innerHTML = `
        <div class="slot-time">${slot.display}</div>
        <div class="slot-fill-bar">
          <div class="slot-fill-inner" style="width:${fill}%;background:${fillColor}"></div>
        </div>
        <div class="slot-load">${slot.load}/${slot.cap} chỗ</div>`;

      if (slot.available) {
        wrap.onclick = () => selectSlot(slot, wrap);
      } else {
        wrap.title = 'Ca này đã đầy';
      }
      grid.appendChild(wrap);
    });
  }

  function selectSlot(slot, btn) {
    document.querySelectorAll('.slot-card.selected').forEach(b => b.classList.remove('selected'));
    selectedKey = slot.key;
    selectedDisplay = slot.display;

    const slotKeyInput = document.getElementById('slotKeyInput');
    if (slotKeyInput) slotKeyInput.value = slot.key;
    btn.classList.add('selected');

    const activeTab = document.querySelector('.date-tab.active');
    if (!activeTab) return;
    const [yy, mm, dd] = activeTab.dataset.date.split('-');
    const dayLabel = activeTab.querySelector('.dt-day').textContent;

    const selectionText = document.getElementById('selectionText');
    if (selectionText) {
      selectionText.innerHTML = `✓ Đã chọn: <strong>${dayLabel} ${dd}/${mm}/${yy} – ${slot.display}</strong>`;
      selectionText.style.color = 'var(--green-700)';
    }
    const btnReschedule = document.getElementById('btnReschedule');
    if (btnReschedule) btnReschedule.disabled = false;
  }

  // ══════ INPATIENT: date + period ══════
  let inpPeriod = '';

  function onInpDateChange(val) {
    const wrap = document.getElementById('inpPeriodWrap');
    if (wrap) wrap.style.display = val ? '' : 'none';
    inpPeriod = '';
    document.querySelectorAll('.period-btn').forEach(b => b.classList.remove('active'));
    checkInpatientReady(val, inpPeriod);
  }

  function selectPeriod(p) {
    inpPeriod = p;
    document.querySelectorAll('.period-btn').forEach(b => b.classList.remove('active'));
    const btn = document.getElementById(p === 'morning' ? 'btnMorning' : 'btnAfternoon');
    if (btn) btn.classList.add('active');

    // Build a fake slotKey for inpatient: "date|period"
    const dateInput = document.getElementById('inpDate');
    const date = dateInput ? dateInput.value : '';
    const slotKeyInput = document.getElementById('slotKeyInput');
    if (slotKeyInput) slotKeyInput.value = date + '|' + p;
    checkInpatientReady(date, p);
  }

  function checkInpatientReady(date, period) {
    const ok = !!date && !!period;
    const selectionText = document.getElementById('selectionText');
    if (ok && selectionText) {
      const label = period === 'morning' ? 'Sáng 08:00–12:00' : 'Chiều 13:30–17:30';
      const [y, m, d] = date.split('-');
      selectionText.innerHTML = `Đã chọn: <strong>${d}/${m}/${y} · ${label}</strong>`;
      selectionText.style.color = 'var(--green-700)';
    }
    const btnReschedule = document.getElementById('btnReschedule');
    if (btnReschedule) btnReschedule.disabled = !ok;
  }

  // Expose to inline onclick/onchange handlers in the JSP markup
  window.onInpDateChange = onInpDateChange;
  window.selectPeriod = selectPeriod;

  // ══════ Confirm dialog before submit ══════
  document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('rescheduleForm');
    if (!form) return;
    form.addEventListener('submit', function (e) {
      const slotKeyInput = document.getElementById('slotKeyInput');
      if (!slotKeyInput || !slotKeyInput.value) {
        e.preventDefault();
        return;
      }
      const selectionText = document.getElementById('selectionText');
      const selText = selectionText
        ? selectionText.textContent.replace('✓ Đã chọn: ', '')
        : '';
      if (!confirm('Xác nhận đổi sang:\n' + selText + '\n\nTrạng thái sẽ về "Pending". Tiếp tục?')) {
        e.preventDefault();
      }
    });
  });

  if (!IS_INPATIENT) {
    renderDateTabs();
  }
})();
