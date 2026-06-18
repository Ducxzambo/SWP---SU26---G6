/**
 * booking-new.js
 * Logic toàn bộ trang đặt lịch khám mới: chọn thú cưng, chọn dịch vụ theo từng
 * thú cưng, chọn khung giờ (ngoại trú) hoặc ngày/buổi (nội trú), tính tổng tiền,
 * và submit form.
 *
 * Yêu cầu: trước khi load file này, JSP phải khai báo các biến toàn cục:
 *   window.BOOKING_CATEGORIES_JSON  (array các category, mỗi category có .services[])
 *   window.BOOKING_SLOTS_JSON       (object: "yyyy-MM-dd" -> [slot,...])
 *   window.BOOKING_TODAY            (string "yyyy-MM-dd")
 *   window.APP_CTX                  (context path, dùng cho fetch tới API slots)
 */
(function () {
  const CATS       = window.BOOKING_CATEGORIES_JSON || [];
  const SLOTS_DATA  = window.BOOKING_SLOTS_JSON || {};
  const TODAY       = window.BOOKING_TODAY || '';
  const CTX         = window.APP_CTX || '';
  const GROOMING_CAT_ID = 3;

  // ══════════════════════════════════════════════════════════
  // STATE
  // ══════════════════════════════════════════════════════════
  const state = {
    pets: new Map(),          // petId → petName
    // Per-pet service selections: petId → { groomSvcs: Map(id→svc), vetSvcs: Map(id→svc) }
    petServices: new Map(),
    slotKey: '',
    slotDisp: '',
    inpatient: false,
    inpDate: '',
    inpPeriod: '',
    currentSlots: {},         // date → slots array (fetched from server)
  };

  // ══════════════════════════════════════════════════════════
  // PET SELECTION
  // ══════════════════════════════════════════════════════════
  function togglePet(el, petId, petName) {
    if (state.pets.has(petId)) {
      state.pets.delete(petId);
      state.petServices.delete(petId);
      el.classList.remove('selected');
    } else {
      state.pets.set(petId, petName);
      state.petServices.set(petId, { groomSvcs: new Map(), vetSvcs: new Map() });
      el.classList.add('selected');
    }
    renderPetServicePanels();
    updateSummary();
  }

  // ══════════════════════════════════════════════════════════
  // PER-PET SERVICE PANELS
  // ══════════════════════════════════════════════════════════
  function renderPetServicePanels() {
    const container = document.getElementById('petServicePanels');
    if (!container) return;
    container.innerHTML = '';

    if (state.pets.size === 0) {
      hideSchedulePanels();
      return;
    }

    state.pets.forEach((petName, petId) => {
      const panel = document.createElement('div');
      panel.className = 'bk-panel';
      panel.innerHTML = `
        <div class="bk-panel-head">
          <span class="bk-step-num">2</span>
          Dịch vụ cho <strong>${escHtml(petName)}</strong>
        </div>
        <div class="bk-panel-body">
          <div class="svc-group" id="grp-groom-${petId}">
            <div class="svc-group-label">
              <span class="svc-group-icon"></span> Chăm sóc & Grooming
            </div>
            <div class="chip-grid" id="chips-groom-${petId}"></div>
            <div class="cap-bar-wrap" id="capbar-groom-${petId}" style="display:none;">
              <div class="cap-bar-label">
                Tải: <span id="capload-groom-${petId}">0</span>/<span id="capcap-groom-${petId}">?</span>
              </div>
              <div class="cap-bar"><div class="cap-bar-fill" id="capfill-groom-${petId}"></div></div>
            </div>
          </div>
          <div class="svc-divider"></div>
          <div class="svc-group" id="grp-vet-${petId}">
            <div class="svc-group-label">
              <span class="svc-group-icon"></span> Dịch vụ Y tếế
            </div>
            <div class="chip-grid" id="chips-vet-${petId}"></div>
          </div>
        </div>`;
      container.appendChild(panel);

      // Populate chips
      CATS.forEach(cat => {
        const targetId = cat.id === GROOMING_CAT_ID ? `chips-groom-${petId}` : `chips-vet-${petId}`;
        const targetEl = document.getElementById(targetId);
        if (!targetEl) return;
        (cat.services || []).forEach(svc => {
          const isInpatientSvc = /nội trú|inpatient|nhập viện/i.test(cat.name + svc.name);
          const chip = document.createElement('div');
          const petSvcs = state.petServices.get(petId);
          const isSelected = cat.id === GROOMING_CAT_ID
            ? petSvcs.groomSvcs.has(svc.id)
            : petSvcs.vetSvcs.has(svc.id);
          chip.className = 'chip' + (isSelected ? ' selected' : '');
          chip.dataset.inpatient = isInpatientSvc ? 'true' : 'false';
          chip.innerHTML = `${escHtml(svc.name)}<span class="chip-sub">${fmtMoney(svc.price)}₫</span>`;
          chip.onclick = () => toggleService(chip, petId, svc, cat.id, isInpatientSvc);
          targetEl.appendChild(chip);
        });
      });
    });

    refreshSchedulePanel();
  }

  // ══════════════════════════════════════════════════════════
  // SERVICE SELECTION (per pet)
  // ══════════════════════════════════════════════════════════
  function toggleService(el, petId, svc, catId, isInpatientSvc) {
    const petSvcs = state.petServices.get(petId);
    if (!petSvcs) return;

    const group = catId === GROOMING_CAT_ID ? petSvcs.groomSvcs : petSvcs.vetSvcs;

    if (group.has(svc.id)) {
      group.delete(svc.id);
      el.classList.remove('selected');
    } else {
      group.set(svc.id, { ...svc, isInpatient: isInpatientSvc });
      el.classList.add('selected');
    }

    // Determine overall inpatient mode: any selected service is inpatient
    let anyInpatient = false;
    state.petServices.forEach(ps => {
      ps.groomSvcs.forEach(s => { if (s.isInpatient) anyInpatient = true; });
      ps.vetSvcs.forEach(s => { if (s.isInpatient) anyInpatient = true; });
    });
    state.inpatient = anyInpatient;
    const isInpatientInput = document.getElementById('isInpatientInput');
    if (isInpatientInput) isInpatientInput.value = anyInpatient ? 'true' : 'false';

    refreshSchedulePanel();
    fetchAndRenderSlots();
    updateSummary();
  }

  // ══════════════════════════════════════════════════════════
  // SHOW / HIDE SCHEDULE PANELS
  // ══════════════════════════════════════════════════════════
  function refreshSchedulePanel() {
    const hasSvc = hasSomeSvc();
    const slotPanel = document.getElementById('slotPanel');
    const inpatientPanel = document.getElementById('inpatientPanel');
    const notesPanel = document.getElementById('notesPanel');

    if (slotPanel) slotPanel.style.display = (hasSvc && !state.inpatient) ? '' : 'none';
    if (inpatientPanel) inpatientPanel.style.display = (hasSvc && state.inpatient) ? '' : 'none';
    if (notesPanel) notesPanel.style.display = hasSvc ? '' : 'none';
  }

  function hideSchedulePanels() {
    ['slotPanel', 'inpatientPanel', 'notesPanel'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.style.display = 'none';
    });
  }

  function hasSomeSvc() {
    for (const [, ps] of state.petServices) {
      if (ps.groomSvcs.size > 0 || ps.vetSvcs.size > 0) return true;
    }
    return false;
  }

  // ══════════════════════════════════════════════════════════
  // CAPACITY BARS (per pet, per group)
  // ══════════════════════════════════════════════════════════
  function updateCapacityBars(slotsForActiveDate) {
    if (!slotsForActiveDate || !state.slotKey) return;
    const slot = slotsForActiveDate.find(s => s.key === state.slotKey);
    if (!slot) return;

    state.pets.forEach((_, petId) => {
      const bar = document.getElementById(`capbar-groom-${petId}`);
      const fill = document.getElementById(`capfill-groom-${petId}`);
      const load = document.getElementById(`capload-groom-${petId}`);
      const cap = document.getElementById(`capcap-groom-${petId}`);

      if (bar && slot.groomCap > 0) {
        bar.style.display = '';
        const pct = Math.min(100, Math.round(slot.groomLoad / slot.groomCap * 100));
        if (fill) {
          fill.style.width = pct + '%';
          fill.style.background = pct >= 100 ? 'var(--red-err)' : 'var(--green-400)';
        }
        if (load) load.textContent = slot.groomLoad;
        if (cap) cap.textContent = slot.groomCap;
      }
    });

    // Show capacity error if any group is full
    const errEl = document.getElementById('capacityError');
    if (!errEl) return;
    const msgs = [];
    const s = slotsForActiveDate.find(sl => sl.key === state.slotKey);
    if (s) {
      if (s.groomCap > 0 && s.groomLoad >= s.groomCap)
        msgs.push('Slot này đã hết chỗ cho dịch vụ Grooming. Vui lòng chọn slot khác.');
      if (s.vetCap > 0 && s.vetLoad >= s.vetCap)
        msgs.push('Slot này đã hết chỗ cho dịch vụ Y tế. Vui lòng chọn slot khác.');
    }
    if (msgs.length) {
      errEl.innerHTML = msgs.join('<br>');
      errEl.style.display = '';
    } else {
      errEl.style.display = 'none';
    }
  }

  // ══════════════════════════════════════════════════════════
  // SLOT FETCH (AJAX) + RENDER
  // ══════════════════════════════════════════════════════════
  let fetchTimer = null;

  function fetchAndRenderSlots() {
    clearTimeout(fetchTimer);
    fetchTimer = setTimeout(async () => {
      const ids = getAllSelectedServiceIds().join(',');
      try {
        const r = await fetch(CTX + '/booking/slots?serviceIds=' + encodeURIComponent(ids));
        if (!r.ok) throw new Error('Bad response: ' + r.status);
        const data = await r.json();
        Object.assign(state.currentSlots, data);
      } catch (e) {
        // use initial SLOTS_DATA as fallback
        Object.assign(state.currentSlots, SLOTS_DATA);
      }
      renderDateTabs();
    }, 200);
  }

  function getAllSelectedServiceIds() {
    const ids = new Set();
    state.petServices.forEach(ps => {
      ps.groomSvcs.forEach((_, id) => ids.add(id));
      ps.vetSvcs.forEach((_, id) => ids.add(id));
    });
    return [...ids];
  }

  const DAYS_VN = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

  function renderDateTabs() {
    const tabsEl = document.getElementById('dateTabs');
    if (!tabsEl) return;
    const dates = Object.keys(state.currentSlots).sort();
    tabsEl.innerHTML = '';

    if (!dates.length) {
      tabsEl.innerHTML = '<span class="slot-loading">Không có lịch trống.</span>';
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
      tabsEl.appendChild(tab);
    });
    renderSlots(dates[0]);
  }

  function renderSlots(ds) {
    const grid = document.getElementById('slotGrid');
    if (!grid) return;
    const slots = state.currentSlots[ds] || [];
    grid.innerHTML = '';

    if (!slots.length) {
      grid.innerHTML = '<div class="slot-loading">Không có ca trống trong ngày này.</div>';
      return;
    }

    slots.forEach(slot => {
      const isSel = slot.key === state.slotKey;
      const wrap = document.createElement('div');
      wrap.className = 'slot-card'
        + (!slot.available ? ' booked' : '')
        + (isSel ? ' selected' : '');

      const fill = Math.min(100, slot.fill || 0);
      const fillColor = fill >= 100 ? 'var(--red-err)'
        : fill >= 70 ? 'var(--amber)'
        : 'var(--green-400)';
      wrap.innerHTML = `
        <div class="slot-time">${slot.display}</div>
        <div class="slot-fill-bar">
          <div class="slot-fill-inner" style="width:${fill}%;background:${fillColor}"></div>
        </div>
        <div class="slot-load">${slot.load}/${slot.cap} chỗ</div>`;

      if (slot.available) {
        wrap.onclick = () => selectSlot(slot, wrap, state.currentSlots[ds]);
      } else {
        wrap.title = 'Ca này đã đầy';
      }
      grid.appendChild(wrap);
    });
  }

  function selectSlot(slot, el, slotsForDate) {
    document.querySelectorAll('.slot-card.selected').forEach(b => b.classList.remove('selected'));
    state.slotKey = slot.key;
    state.slotDisp = slot.display;
    const slotKeyInput = document.getElementById('slotKeyInput');
    if (slotKeyInput) slotKeyInput.value = slot.key;
    el.classList.add('selected');
    updateCapacityBars(slotsForDate);
    updateSummary();
  }

  // ══════════════════════════════════════════════════════════
  // INPATIENT
  // ══════════════════════════════════════════════════════════
  function onInpatientDateChange(val) {
    state.inpDate = val;
    const inpatientDateInput = document.getElementById('inpatientDateInput');
    if (inpatientDateInput) inpatientDateInput.value = val;
    const inpPeriodWrap = document.getElementById('inpPeriodWrap');
    if (inpPeriodWrap) inpPeriodWrap.style.display = val ? '' : 'none';
    state.inpPeriod = '';
    const inpatientPeriodInput = document.getElementById('inpatientPeriodInput');
    if (inpatientPeriodInput) inpatientPeriodInput.value = '';
    document.querySelectorAll('.period-btn').forEach(b => b.classList.remove('active'));
    updateSummary();
  }

  function selectPeriod(period) {
    state.inpPeriod = period;
    const inpatientPeriodInput = document.getElementById('inpatientPeriodInput');
    if (inpatientPeriodInput) inpatientPeriodInput.value = period;
    document.querySelectorAll('.period-btn').forEach(b => b.classList.remove('active'));
    const btn = document.getElementById(period === 'morning' ? 'btnMorning' : 'btnAfternoon');
    if (btn) btn.classList.add('active');
    updateSummary();
  }

  // ══════════════════════════════════════════════════════════
  // SUMMARY + VALIDATION
  // ══════════════════════════════════════════════════════════
  function updateSummary() {
    // Pets
    const petSec = document.getElementById('sumPetSection');
    const petBody = document.getElementById('sumPets');
    if (petSec && petBody) {
      if (state.pets.size > 0) {
        petSec.style.display = '';
        petBody.innerHTML = [...state.pets.values()].map(n =>
          `<span class="sum-chip">${escHtml(n)}</span>`).join('');
      } else {
        petSec.style.display = 'none';
      }
    }

    // Services per pet
    const svcSec = document.getElementById('sumSvcSection');
    const svcBody = document.getElementById('sumSvcs');
    const svcLines = [];
    state.petServices.forEach((ps, petId) => {
      const petName = state.pets.get(petId) || '';
      const allSvcs = [...ps.groomSvcs.values(), ...ps.vetSvcs.values()];
      if (allSvcs.length) {
        svcLines.push(`<div class="sum-pet-row"><strong>${escHtml(petName)}:</strong> ${allSvcs.map(s => escHtml(s.name)).join(', ')}</div>`);
      }
    });
    if (svcSec && svcBody) {
      if (svcLines.length) {
        svcSec.style.display = '';
        svcBody.innerHTML = svcLines.join('');
      } else {
        svcSec.style.display = 'none';
      }
    }

    // Slot/time
    const slotSec = document.getElementById('sumSlotSection');
    const slotBody = document.getElementById('sumSlot');
    let slotText = '';
    if (state.inpatient && state.inpDate && state.inpPeriod) {
      const periodLabel = state.inpPeriod === 'morning' ? 'Sáng 08:00–12:00' : 'Chiều 13:30–17:30';
      slotText = fmtDate(state.inpDate) + ' · ' + periodLabel;
    } else if (!state.inpatient && state.slotKey) {
      slotText = state.slotDisp + ' · ' + state.slotKey.split('|')[0];
    }
    if (slotSec && slotBody) {
      if (slotText) {
        slotSec.style.display = '';
        slotBody.textContent = slotText;
      } else {
        slotSec.style.display = 'none';
      }
    }

    // Price
    let total = 0;
    state.petServices.forEach((ps) => {
      [...ps.groomSvcs.values(), ...ps.vetSvcs.values()].forEach(s => total += Number(s.price || 0));
    });
    const sumPrice = document.getElementById('sumPrice');
    if (sumPrice) sumPrice.textContent = total > 0 ? fmtMoney(total) + '₫' : '—';

    // Enable submit
    const hasPet = state.pets.size > 0;
    const hasSvc = hasSomeSvc();
    const hasSlot = state.inpatient
      ? (!!state.inpDate && !!state.inpPeriod)
      : !!state.slotKey;
    const btnBook = document.getElementById('btnBook');
    if (btnBook) btnBook.disabled = !(hasPet && hasSvc && hasSlot);
    const notesPanel = document.getElementById('notesPanel');
    if (notesPanel) notesPanel.style.display = (hasPet && hasSvc) ? '' : 'none';
  }

  // ══════════════════════════════════════════════════════════
  // FORM SUBMIT
  // ══════════════════════════════════════════════════════════
  function addHidden(parent, name, val) {
    const inp = document.createElement('input');
    inp.type = 'hidden';
    inp.name = name;
    inp.value = val;
    parent.appendChild(inp);
  }

  // ══════════════════════════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════════════════════════
  function fmtMoney(n) { return Number(n).toLocaleString('vi-VN'); }
  function fmtDate(s) {
    if (!s) return '';
    const [y, m, d] = s.split('-');
    return `${d}/${m}/${y}`;
  }
  function escHtml(s) {
    if (!s) return '';
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  // Expose functions used by inline onclick/onchange handlers in the JSP markup
  window.togglePet = togglePet;
  window.onInpatientDateChange = onInpatientDateChange;
  window.selectPeriod = selectPeriod;

  // ══════════════════════════════════════════════════════════
  // INIT
  // ══════════════════════════════════════════════════════════
  document.addEventListener('DOMContentLoaded', function () {
    const bookingForm = document.getElementById('bookingForm');
    if (bookingForm) {
      bookingForm.addEventListener('submit', function (e) {
        e.preventDefault();
        const wrap = document.getElementById('hiddenInputs');
        if (!wrap) { this.submit(); return; }
        wrap.innerHTML = '';

        // Collect all unique service IDs across all pets
        const svcIds = new Set();
        state.petServices.forEach(ps => {
          ps.groomSvcs.forEach((_, id) => svcIds.add(id));
          ps.vetSvcs.forEach((_, id) => svcIds.add(id));
        });
        svcIds.forEach(id => addHidden(wrap, 'serviceIds', id));

        // Pet IDs
        state.pets.forEach((_, id) => addHidden(wrap, 'petIds', id));

        this.submit();
      });
    }

    // Auto-select first pet if only one pet
    // (URL prefill params "prefillCategory"/"prefillService" are read but not
    // currently wired to a concrete action in the original code; kept as-is to
    // preserve existing behavior, since changing it could be a feature request
    // beyond the scope of this refactor — flagged for follow-up.)
    const petChips = document.querySelectorAll('#petChips .chip');
    if (petChips.length === 1) petChips[0].click();
  });

  // Init slot data
  Object.assign(state.currentSlots, SLOTS_DATA);
  document.addEventListener('DOMContentLoaded', renderDateTabs);
})();
