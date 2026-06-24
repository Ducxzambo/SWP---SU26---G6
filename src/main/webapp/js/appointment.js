/**
 * appointment.js  — PetClinic Appointment Pages
 *
 * Sections:
 *   1. Tab switching         — appointment.jsp
 *   2. Filter & Search       — appointment.jsp (Sắp tới + Lịch sử)
 *   3. Cancel modal          — appointment-detail.jsp
 *   4. Reschedule slot picker — appointment-reschedule.jsp
 */

/* ============================================================
 * 1) TAB SWITCHING
 * ============================================================ */
(function () {
  function switchTab(name, btn) {
    document.querySelectorAll('.appt-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.appt-tab-panel').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    const panel = document.getElementById('tab-' + name);
    if (panel) panel.classList.add('active');
  }
  window.switchTab = switchTab;
})();


/* ============================================================
 * 2) FILTER & SEARCH
 * ============================================================ */
(function () {
  /**
   * Bootstrap a full filter/search system for one appointment list.
   *
   * @param {string} prefix  'upcoming' | 'history'
   */
  function initFilterBar(prefix) {
    const listEl    = document.getElementById('list-' + prefix);
    const searchEl  = document.getElementById(prefix + '-search');
    const fieldEl   = document.getElementById(prefix + '-search-field');
    const statusEl  = document.getElementById(prefix + '-status');
    const sortEl    = document.getElementById(prefix + '-sort');
    const countEl   = document.getElementById(prefix + '-count');
    const noResEl   = document.getElementById(prefix + '-noresult');
    const resetBtn  = document.getElementById(prefix + '-reset');

    if (!listEl) return; // tab is empty, skip

    // All cards (frozen snapshot)
    const allCards = Array.from(listEl.querySelectorAll('.appt-card'));

    function applyFilters() {
      const q      = (searchEl?.value || '').trim().toLowerCase();
      const field  = fieldEl?.value  || 'all';
      const status = statusEl?.value || '';
      const sort   = sortEl?.value   || 'date-asc';

      let visible = allCards.filter(card => {
        // ── text search ──────────────────────────────────────────
        if (q) {
          let haystack = '';
          if (field === 'all') {
            haystack = [card.dataset.pet, card.dataset.service,
                        card.dataset.category, card.dataset.staff].join(' ');
          } else {
            haystack = card.dataset[field] || '';
          }
          if (!haystack.toLowerCase().includes(q)) return false;
        }
        // ── status filter ────────────────────────────────────────
        if (status && card.dataset.status !== status) return false;
        return true;
      });

      // ── sort ─────────────────────────────────────────────────────
      visible.sort((a, b) => {
        switch (sort) {
          case 'date-asc':
            return (a.dataset.date || '') < (b.dataset.date || '') ? -1 : 1;
          case 'date-desc':
            return (a.dataset.date || '') > (b.dataset.date || '') ? -1 : 1;
          case 'service-az':
            return (a.dataset.service || '').localeCompare(b.dataset.service || '', 'vi');
          case 'service-za':
            return (b.dataset.service || '').localeCompare(a.dataset.service || '', 'vi');
          default: return 0;
        }
      });

      // ── apply visibility + DOM order ──────────────────────────────
      allCards.forEach(c => c.classList.add('hidden'));
      visible.forEach(c => {
        c.classList.remove('hidden');
        listEl.appendChild(c); // re-order in DOM
      });

      // ── counters & empty state ────────────────────────────────────
      if (countEl) {
        countEl.textContent = visible.length + ' / ' + allCards.length + ' lịch hẹn';
      }
      if (noResEl) noResEl.style.display = visible.length === 0 ? 'block' : 'none';

      // ── show/hide reset button ────────────────────────────────────
      const isFiltered = q || status || (sort !== sortEl?.options[0]?.value);
      if (resetBtn) resetBtn.classList.toggle('visible', !!isFiltered);
    }

    // Wire events
    searchEl?.addEventListener('input',  applyFilters);
    fieldEl?.addEventListener('change',  applyFilters);
    statusEl?.addEventListener('change', applyFilters);
    sortEl?.addEventListener('change',   applyFilters);

    // Initial render (shows count without filtering)
    applyFilters();
  }

  function resetFilters(prefix) {
    const searchEl = document.getElementById(prefix + '-search');
    const fieldEl  = document.getElementById(prefix + '-search-field');
    const statusEl = document.getElementById(prefix + '-status');
    const sortEl   = document.getElementById(prefix + '-sort');
    if (searchEl) searchEl.value = '';
    if (fieldEl)  fieldEl.value  = 'all';
    if (statusEl) statusEl.value = '';
    if (sortEl)   sortEl.value   = sortEl.options[0]?.value || '';
    // Trigger re-filter by dispatching change on sortEl
    sortEl?.dispatchEvent(new Event('change'));
  }

  window.resetFilters = resetFilters;

  // Init both tabs on DOM ready
  document.addEventListener('DOMContentLoaded', function () {
    initFilterBar('upcoming');
    initFilterBar('history');
  });
})();


/* ============================================================
 * 3) CANCEL MODAL  — appointment-detail.jsp
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
    const cb = document.getElementById('confirmCheck');
    if (cb) {
      cb.checked = false;
      const btn = document.getElementById('btnConfirmCancel');
      if (btn) btn.disabled = true;
    }
    const ta = document.getElementById('cancelReason');
    if (ta) ta.value = '';
  }

  window.openCancelModal  = openCancelModal;
  window.closeCancelModal = closeCancelModal;

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
 * 4) RESCHEDULE  — appointment-reschedule.jsp
 *
 * Requires these globals set by the JSP before this file loads:
 *   window.RESCHEDULE_SLOTS_DATA   = { "yyyy-MM-dd": [slot,...] }
 *   window.RESCHEDULE_IS_INPATIENT = true | false
 * ============================================================ */
(function () {
  if (!document.getElementById('rescheduleForm')) return;

  const SLOTS_DATA   = window.RESCHEDULE_SLOTS_DATA   || {};
  const IS_INPATIENT = !!window.RESCHEDULE_IS_INPATIENT;
  const DAYS_VN      = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

  let selectedKey = '';

  /* ── Outpatient: date tabs + slot grid ─────────────────────── */
  function renderDateTabs() {
    const tabsEl = document.getElementById('dateTabs');
    if (!tabsEl) return;
    const dates = Object.keys(SLOTS_DATA).sort();
    tabsEl.innerHTML = '';

    if (!dates.length) {
      tabsEl.innerHTML = '<span class="slot-loading">Không có lịch trống phù hợp trong 30 ngày tới.</span>';
      const grid = document.getElementById('slotGrid');
      if (grid) grid.innerHTML = '';
      return;
    }

    dates.forEach((ds, i) => {
      const d   = new Date(ds + 'T00:00:00');
      const tab = document.createElement('div');
      tab.className    = 'date-tab' + (i === 0 ? ' active' : '');
      tab.dataset.date = ds;
      tab.innerHTML    = `<div class="dt-day">${DAYS_VN[d.getDay()]}</div>
                          <div class="dt-date">${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}</div>`;
      tab.onclick = () => {
        document.querySelectorAll('.date-tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        renderSlots(ds);
      };
      tabsEl.appendChild(tab);
    });
    renderSlots(dates[0]);
  }

  function renderSlots(ds) {
    const grid  = document.getElementById('slotGrid');
    if (!grid) return;
    const slots = SLOTS_DATA[ds] || [];
    grid.innerHTML = '';

    if (!slots.length) {
      grid.innerHTML = '<div class="slot-loading">Không có ca trống.</div>';
      return;
    }

    slots.forEach(slot => {
      const fill      = Math.min(100, slot.fill || 0);
      const fillColor = fill >= 100 ? 'var(--red-err)'
                      : fill >= 70  ? 'var(--amber)' : 'var(--green-400)';
      const wrap = document.createElement('div');
      wrap.className = 'slot-card'
        + (!slot.available    ? ' booked'   : '')
        + (slot.key === selectedKey ? ' selected' : '');
      wrap.innerHTML = `
        <div class="slot-time">${slot.display}</div>
        <div class="slot-fill-bar">
          <div class="slot-fill-inner" style="width:${fill}%;background:${fillColor}"></div>
        </div>
        <div class="slot-load">${slot.load}/${slot.cap} chỗ</div>`;

      if (slot.available) {
        wrap.onclick = () => selectSlot(slot, wrap, ds);
      } else {
        wrap.title = 'Ca này đã đầy';
      }
      grid.appendChild(wrap);
    });
  }

  function selectSlot(slot, btn, ds) {
    document.querySelectorAll('.slot-card.selected').forEach(b => b.classList.remove('selected'));
    selectedKey = slot.key;
    btn.classList.add('selected');

    const slotInput = document.getElementById('slotKeyInput');
    if (slotInput) slotInput.value = slot.key;

    // Build display label
    const d = new Date(ds + 'T00:00:00');
    const dd = String(d.getDate()).padStart(2,'0');
    const mm = String(d.getMonth()+1).padStart(2,'0');
    const dayLabel = DAYS_VN[d.getDay()];
    setSelectionText(`✓ Đã chọn: <strong>${dayLabel} ${dd}/${mm}/${d.getFullYear()} – ${slot.display}</strong>`);
    enableReschedule(true);
  }

  /* ── Inpatient: date + period ───────────────────────────────── */
  let inpPeriod = '';

  function onInpDateChange(val) {
    const wrap = document.getElementById('inpPeriodWrap');
    if (wrap) wrap.style.display = val ? '' : 'none';
    inpPeriod = '';
    document.querySelectorAll('.period-btn').forEach(b => b.classList.remove('active'));
    clearSlotKey();
    enableReschedule(false);
  }

  function selectPeriod(p) {
    inpPeriod = p;
    document.querySelectorAll('.period-btn').forEach(b => b.classList.remove('active'));
    const btn = document.getElementById(p === 'morning' ? 'btnMorning' : 'btnAfternoon');
    if (btn) btn.classList.add('active');

    const dateEl = document.getElementById('inpDate');
    const date   = dateEl ? dateEl.value : '';
    if (!date) return;

    const slotKey = date + '|' + p;
    const slotInput = document.getElementById('slotKeyInput');
    if (slotInput) slotInput.value = slotKey;

    const label = p === 'morning' ? 'Sáng 08:00–12:00' : 'Chiều 13:30–17:30';
    const [y, m, d] = date.split('-');
    setSelectionText(`✓ Đã chọn: <strong>${d}/${m}/${y} – ${label}</strong>`);
    enableReschedule(true);
  }

  window.onInpDateChange = onInpDateChange;
  window.selectPeriod    = selectPeriod;

  /* ── Shared helpers ─────────────────────────────────────────── */
  function setSelectionText(html) {
    const el = document.getElementById('selectionText');
    if (el) { el.innerHTML = html; el.style.color = 'var(--green-700)'; }
  }
  function enableReschedule(ok) {
    const btn = document.getElementById('btnReschedule');
    if (btn) btn.disabled = !ok;
  }
  function clearSlotKey() {
    const el = document.getElementById('slotKeyInput');
    if (el) el.value = '';
  }

  /* ── Confirm dialog before submit ───────────────────────────── */
  document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('rescheduleForm');
    if (!form) return;
    form.addEventListener('submit', function (e) {
      const slotInput = document.getElementById('slotKeyInput');
      if (!slotInput || !slotInput.value) { e.preventDefault(); return; }
      const selEl  = document.getElementById('selectionText');
      const selTxt = selEl ? selEl.textContent.replace('✓ Đã chọn: ','') : '';
      if (!confirm('Xác nhận đổi sang:\n' + selTxt + '\n\nTrạng thái sẽ về "Pending". Tiếp tục?')) {
        e.preventDefault();
      }
    });
  });

  // Init
  if (!IS_INPATIENT) renderDateTabs();
})();
