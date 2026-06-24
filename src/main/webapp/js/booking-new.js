(function () {
  if (!document.getElementById('bookingForm')) return; // DOM guard

  const CTX        = window.APP_CTX || '';
  const CATS       = window.BOOKING_CATEGORIES_JSON || [];
  const VACCINES   = window.BOOKING_VACCINES_JSON   || [];
  const SLOTS_DATA = window.BOOKING_SLOTS_JSON      || {};
  const VACCINE_CATEGORY_ID  = window.VACCINE_CATEGORY_ID;
  const VACCINE_SERVICE_ID   = window.VACCINE_SERVICE_ID;
  const INPATIENT_SERVICE_ID = window.INPATIENT_SERVICE_ID;

  const state = {
    mode: 'normal',           // 'normal' | 'inpatient'
    categoryId: null,
    selectedServiceIds: new Set(),   // dịch vụ thường (category != Vaccine)
    selectedVaccineIds: new Set(),   // category == Vaccine
    selectedPetIds: new Set(),
    inpatientPetIds: new Set(),
    slotKey: null,
    inpatientDate: null,
    inpatientPeriod: null,
    currentSlots: {},
  };
  Object.assign(state.currentSlots, SLOTS_DATA);

  let fetchTimer = null;

  // ── MODE: normal vs inpatient (loại trừ nhau) ─────────────────────────────
  window.selectMode = function (mode) {
    state.mode = mode;
    document.getElementById('btnModeNormal').classList.toggle('active', mode === 'normal');
    document.getElementById('btnModeInpatient').classList.toggle('active', mode === 'inpatient');
    document.getElementById('normalModePanels').style.display    = mode === 'normal'    ? '' : 'none';
    document.getElementById('inpatientModePanels').style.display = mode === 'inpatient' ? '' : 'none';
    document.getElementById('isInpatientInput').value = (mode === 'inpatient') ? 'true' : 'false';
    document.getElementById('notesPanel').style.display = '';
    updateSummary();
  };

  // ── STEP: Category chips ──────────────────────────────────────────────────
  function renderCategoryChips() {
    const wrap = document.getElementById('categoryChips');
    if (!wrap) return;
    wrap.innerHTML = '';
    CATS.forEach(cat => {
      const chip = document.createElement('div');
      chip.className = 'chip';
      chip.textContent = cat.name;
      chip.onclick = () => selectCategory(cat, chip);
      wrap.appendChild(chip);
    });
  }

  function selectCategory(cat, chipEl) {
    state.categoryId = cat.id;
    state.selectedServiceIds = new Set();
    state.selectedVaccineIds = new Set();

    document.querySelectorAll('#categoryChips .chip').forEach(c => c.classList.remove('selected'));
    chipEl.classList.add('selected');

    const isVaccine = cat.id === VACCINE_CATEGORY_ID;
    document.getElementById('serviceListPanel').style.display = isVaccine ? 'none' : '';
    document.getElementById('vaccineListPanel').style.display = isVaccine ? '' : 'none';

    if (isVaccine) {
      renderVaccineChips();
      // Category Vaccine dùng đúng 1 service placeholder cố định làm ServiceID.
      if (VACCINE_SERVICE_ID > 0) state.selectedServiceIds.add(VACCINE_SERVICE_ID);
    } else {
      renderServiceChips(cat.services || []);
    }

    document.getElementById('petPanel').style.display = '';
    refreshSlotPanelVisibility();
    fetchAndRenderSlots();
    updateSummary();
  }

  function renderServiceChips(services) {
    const wrap = document.getElementById('serviceChips');
    wrap.innerHTML = '';
    services.forEach(svc => {
      const chip = document.createElement('div');
      chip.className = 'chip';
      chip.innerHTML = escHtml(svc.name) +
          '<span class="chip-sub">' + formatVnd(svc.price) + 'đ</span>';
      chip.onclick = () => {
        if (state.selectedServiceIds.has(svc.id)) {
          state.selectedServiceIds.delete(svc.id);
          chip.classList.remove('selected');
        } else {
          state.selectedServiceIds.add(svc.id);
          chip.classList.add('selected');
        }
        refreshSlotPanelVisibility();
        fetchAndRenderSlots();
        updateSummary();
      };
      wrap.appendChild(chip);
    });
  }

  function renderVaccineChips() {
    const wrap = document.getElementById('vaccineChips');
    wrap.innerHTML = '';
    VACCINES.forEach(v => {
      const chip = document.createElement('div');
      chip.className = 'chip';
      chip.innerHTML = escHtml(v.name) +
          '<span class="chip-sub">' + formatVnd(v.price) + 'đ · còn ' + v.stock + '</span>';
      chip.onclick = () => {
        if (state.selectedVaccineIds.has(v.id)) {
          state.selectedVaccineIds.delete(v.id);
          chip.classList.remove('selected');
        } else {
          state.selectedVaccineIds.add(v.id);
          chip.classList.add('selected');
        }
        updateSummary();
      };
      wrap.appendChild(chip);
    });
  }

  function hasSomeSelection() {
    if (state.categoryId === VACCINE_CATEGORY_ID) return state.selectedVaccineIds.size > 0;
    return state.selectedServiceIds.size > 0;
  }

  function refreshSlotPanelVisibility() {
    const slotPanel = document.getElementById('slotPanel');
    if (slotPanel) slotPanel.style.display = hasSomeSelection() ? '' : 'none';
  }

  // ── STEP: Pet chips (normal mode) ─────────────────────────────────────────
  window.togglePet = function (el, petId) {
    if (state.selectedPetIds.has(petId)) {
      state.selectedPetIds.delete(petId);
      el.classList.remove('selected');
    } else {
      state.selectedPetIds.add(petId);
      el.classList.add('selected');
    }
    updateSummary();
  };

  window.toggleInpatientPet = function (el, petId) {
    if (state.inpatientPetIds.has(petId)) {
      state.inpatientPetIds.delete(petId);
      el.classList.remove('selected');
    } else {
      state.inpatientPetIds.add(petId);
      el.classList.add('selected');
    }
    updateSummary();
  };

  // ── STEP: Slot fetch + render ──────────────────────────────────────────────
  function fetchAndRenderSlots() {
    clearTimeout(fetchTimer);
    fetchTimer = setTimeout(async () => {
      const ids = Array.from(state.selectedServiceIds).join(',');
      try {
        const r = await fetch(CTX + '/booking/slots?serviceIds=' + encodeURIComponent(ids));
        if (!r.ok) throw new Error('Bad response: ' + r.status);
        const data = await r.json();
        Object.assign(state.currentSlots, data);
      } catch (e) {
        Object.assign(state.currentSlots, SLOTS_DATA);
      }
      renderDateTabs();
    }, 200);
  }

  function renderDateTabs() {
    const tabsEl = document.getElementById('dateTabs');
    if (!tabsEl) return;
    const dates = Object.keys(state.currentSlots).sort();
    tabsEl.innerHTML = '';
    dates.slice(0, 14).forEach((ds, i) => {
      const tab = document.createElement('div');
      tab.className = 'date-tab' + (i === 0 ? ' active' : '');
      const d = new Date(ds + 'T00:00:00');
      tab.innerHTML = ['CN','T2','T3','T4','T5','T6','T7'][d.getDay()] +
          '<br><small>' + d.getDate() + '/' + (d.getMonth() + 1) + '</small>';
      tab.onclick = () => {
        document.querySelectorAll('.date-tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        renderSlots(ds);
      };
      tabsEl.appendChild(tab);
    });
    if (dates.length > 0) renderSlots(dates[0]);
  }

  function renderSlots(ds) {
    const grid = document.getElementById('slotGrid');
    if (!grid) return;
    const slots = state.currentSlots[ds] || [];
    if (slots.length === 0) {
      grid.innerHTML = '<div class="slot-loading">Không có khung giờ nào.</div>';
      return;
    }
    grid.innerHTML = '';
    slots.forEach(slot => {
      const fill = Math.min(100, slot.fill || 0);
      const wrap = document.createElement('div');
      wrap.className = 'slot-card' + (!slot.available ? ' slot-card--full' : '') +
          (state.slotKey === slot.key ? ' selected' : '');
      wrap.innerHTML =
          '<div class="slot-time">' + slot.display + '</div>' +
          '<div class="slot-fill-bar"><div class="slot-fill" style="width:' + fill + '%"></div></div>' +
          '<div class="slot-load">' + slot.load + '/' + slot.cap + ' chỗ</div>';
      if (slot.available) {
        wrap.onclick = () => {
          state.slotKey = slot.key;
          document.getElementById('slotKeyInput').value = slot.key;
          document.querySelectorAll('.slot-card').forEach(c => c.classList.remove('selected'));
          wrap.classList.add('selected');
          updateSummary();
        };
      }
      grid.appendChild(wrap);
    });
  }

  // ── Inpatient date/period ─────────────────────────────────────────────────
  window.onInpatientDateChange = function (val) {
    state.inpatientDate = val;
    document.getElementById('inpatientDateInput').value = val;
    updateSummary();
  };

  window.selectPeriod = function (period) {
    state.inpatientPeriod = period;
    document.getElementById('inpatientPeriodInput').value = period;
    document.getElementById('btnMorning').classList.toggle('active', period === 'morning');
    document.getElementById('btnAfternoon').classList.toggle('active', period === 'afternoon');
    updateSummary();
  };

  // ── Summary + submit guard ────────────────────────────────────────────────
  function currentTotal() {
    let total = 0;
    const petCount = state.mode === 'inpatient' ? state.inpatientPetIds.size : state.selectedPetIds.size;
    if (state.mode === 'inpatient') {
      total = 200000; // chỉ hiển thị tiền đặt cọc cố định cho nội trú (logic cũ)
      return { total, deposit: 200000, isDeposit: true };
    }
    if (state.categoryId === VACCINE_CATEGORY_ID) {
      VACCINES.filter(v => state.selectedVaccineIds.has(v.id))
          .forEach(v => { total += v.price * petCount; });
    } else {
      const cat = CATS.find(c => c.id === state.categoryId);
      (cat ? cat.services : []).filter(s => state.selectedServiceIds.has(s.id))
          .forEach(s => { total += s.price * petCount; });
    }
    return { total, deposit: Math.max(1, Math.round(total * 0.2)), isDeposit: false };
  }

  function updateSummary() {
    const petSection = document.getElementById('sumPetSection');
    const svcSection = document.getElementById('sumSvcSection');
    const slotSection= document.getElementById('sumSlotSection');
    const btnBook    = document.getElementById('btnBook');

    let ready = false;

    if (state.mode === 'inpatient') {
      petSection.style.display = state.inpatientPetIds.size > 0 ? '' : 'none';
      document.getElementById('sumPets').textContent = state.inpatientPetIds.size + ' thú cưng';
      svcSection.style.display = '';
      document.getElementById('sumSvcs').textContent = 'Nội trú';
      slotSection.style.display = (state.inpatientDate && state.inpatientPeriod) ? '' : 'none';
      document.getElementById('sumSlot').textContent =
          (state.inpatientDate || '') + ' · ' + (state.inpatientPeriod === 'morning' ? 'Sáng' : state.inpatientPeriod === 'afternoon' ? 'Chiều' : '');
      ready = state.inpatientPetIds.size > 0 && !!state.inpatientDate && !!state.inpatientPeriod;
    } else {
      petSection.style.display = state.selectedPetIds.size > 0 ? '' : 'none';
      document.getElementById('sumPets').textContent = state.selectedPetIds.size + ' thú cưng';

      const hasSel = hasSomeSelection();
      svcSection.style.display = hasSel ? '' : 'none';
      if (state.categoryId === VACCINE_CATEGORY_ID) {
        document.getElementById('sumSvcs').textContent =
            VACCINES.filter(v => state.selectedVaccineIds.has(v.id)).map(v => v.name).join(', ');
      } else {
        const cat = CATS.find(c => c.id === state.categoryId);
        document.getElementById('sumSvcs').textContent =
            (cat ? (cat.services || []) : []).filter(s => state.selectedServiceIds.has(s.id))
                .map(s => s.name).join(', ');
      }

      slotSection.style.display = state.slotKey ? '' : 'none';
      document.getElementById('sumSlot').textContent = state.slotKey ? state.slotKey.replace('|', ' · ') : '';

      ready = state.selectedPetIds.size > 0 && hasSel && !!state.slotKey;
    }

    const { total } = currentTotal();
    document.getElementById('sumPrice').textContent = formatVnd(total) + 'đ';
    btnBook.disabled = !ready;
  }

  // ── Submit: inject hidden serviceIds[]/petIds[]/vaccineIds[] ──────────────
  document.getElementById('bookingForm').addEventListener('submit', function () {
    const hidden = document.getElementById('hiddenInputs');
    hidden.innerHTML = '';

    function addHidden(name, value) {
      const inp = document.createElement('input');
      inp.type = 'hidden'; inp.name = name; inp.value = value;
      hidden.appendChild(inp);
    }

    if (state.mode === 'inpatient') {
      if (INPATIENT_SERVICE_ID > 0) addHidden('serviceIds', INPATIENT_SERVICE_ID);
      state.inpatientPetIds.forEach(id => addHidden('petIds', id));
    } else {
      state.selectedServiceIds.forEach(id => addHidden('serviceIds', id));
      state.selectedPetIds.forEach(id => addHidden('petIds', id));
      state.selectedVaccineIds.forEach(id => addHidden('vaccineIds', id));
    }
  });

  // ── Helpers ────────────────────────────────────────────────────────────────
  function formatVnd(n) {
    return new Intl.NumberFormat('vi-VN').format(Math.round(n || 0));
  }

  function escHtml(s) {
    if (s == null) return '';
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  // ── Init ───────────────────────────────────────────────────────────────────
  selectMode('normal');
  renderCategoryChips();
  renderDateTabs();
  updateSummary();
})();
