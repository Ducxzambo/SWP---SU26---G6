(function () {
  if (!document.getElementById('bookingForm')) return;

  const CTX = window.APP_CTX || '';
  const state = {
    mode: 'normal',
    pets: new Map(),
    petConfigs: new Map(),
    inpatientPetIds: new Set(),
    slotKey: null,
    inpatientDate: null,
    inpatientPeriod: null,
    categories: [],
    vaccines: [],
    slots: {},
    vaccineCategoryId: 4,
    vaccineServiceId: -1,
    inpatientServiceId: -1,
  };

  let fetchTimer = null;

  window.selectMode = function (mode) {
    state.mode = mode;
    document.getElementById('btnModeNormal').classList.toggle('active', mode === 'normal');
    document.getElementById('btnModeInpatient').classList.toggle('active', mode === 'inpatient');
    document.getElementById('normalModePanels').style.display = mode === 'normal' ? '' : 'none';
    document.getElementById('inpatientModePanels').style.display = mode === 'inpatient' ? '' : 'none';
    document.getElementById('isInpatientInput').value = mode === 'inpatient' ? 'true' : 'false';
    document.getElementById('notesPanel').style.display = '';
    updateSummary();
  };

  // Vẫn chỉ cho chọn ĐÚNG 1 thú cưng / lượt đặt lịch (single-select, dùng
  // lại UI chip-grid cũ — chọn pet khác sẽ tự bỏ chọn pet trước đó). Nhưng
  // với pet đã chọn, nay cho phép chọn NHIỀU category/dịch vụ/vaccine.
  window.toggleMainPet = function (el, petId, petName) {
    if (state.petConfigs.has(petId)) {
      state.petConfigs.delete(petId);
      state.pets.delete(petId);
      el.classList.remove('selected');
    } else {
      state.pets.clear();
      state.petConfigs.clear();
      document.querySelectorAll('#mainPetChips .chip').forEach(c => c.classList.remove('selected'));
      state.pets.set(petId, petName || ('Pet #' + petId));
      state.petConfigs.set(petId, { categoryIds: new Set(), serviceIds: new Set(), vaccineIds: new Set() });
      el.classList.add('selected');
    }
    clearSlotSelection();
    renderPetConfigs();
    refreshSlots();
    updateSummary();
  };

  // Multi-select: chọn/bỏ chọn 1 dịch vụ trong danh sách, KHÔNG xoá các lựa
  // chọn khác (khác hẳn hành vi cũ vốn chỉ cho đúng 1 dịch vụ toàn bộ).
  window.togglePetService = function (el, petId, serviceId) {
    const cfg = state.petConfigs.get(petId);
    if (!cfg) return;
    if (cfg.serviceIds.has(serviceId)) cfg.serviceIds.delete(serviceId);
    else cfg.serviceIds.add(serviceId);
    renderPetConfigs();
    clearSlotSelection();
    refreshSlots();
    updateSummary();
  };

  // Multi-select: chọn/bỏ chọn 1 vaccine, có thể chọn nhiều vaccine cùng lúc.
  window.togglePetVaccine = function (el, petId, vaccineId) {
    const cfg = state.petConfigs.get(petId);
    if (!cfg) return;
    if (cfg.vaccineIds.has(vaccineId)) cfg.vaccineIds.delete(vaccineId);
    else cfg.vaccineIds.add(vaccineId);
    renderPetConfigs();
    clearSlotSelection();
    refreshSlots();
    updateSummary();
  };

  // Multi-select: cho phép chọn NHIỀU category dịch vụ cùng lúc (thay vì chỉ
  // 1 category như trước). Bỏ chọn 1 category sẽ tự bỏ chọn các dịch vụ/
  // vaccine thuộc category đó.
  window.togglePetCategory = function (petId, categoryId) {
    const cfg = state.petConfigs.get(petId);
    if (!cfg) return;
    if (cfg.categoryIds.has(categoryId)) {
      cfg.categoryIds.delete(categoryId);
      const cat = state.categories.find(c => c.id === categoryId);
      if (cat) (cat.services || []).forEach(s => cfg.serviceIds.delete(s.id));
      if (categoryId === state.vaccineCategoryId) cfg.vaccineIds.clear();
      clearSlotSelection();
    } else {
      cfg.categoryIds.add(categoryId);
    }
    renderPetConfigs();
    refreshSlots();
    updateSummary();
  };

  // Nội trú: cũng chỉ cho chọn ĐÚNG 1 thú cưng / lượt đặt.
  window.toggleInpatientPet = function (el, petId) {
    if (state.inpatientPetIds.has(petId)) {
      state.inpatientPetIds.delete(petId);
      el.classList.remove('selected');
    } else {
      state.inpatientPetIds.clear();
      document.querySelectorAll('#inpPetChips .chip').forEach(c => c.classList.remove('selected'));
      state.inpatientPetIds.add(petId);
      el.classList.add('selected');
    }
    updateSummary();
  };

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

  function clearSlotSelection() {
    state.slotKey = null;
    document.getElementById('slotKeyInput').value = '';
  }

  function renderPetConfigs() {
    const panel = document.getElementById('petConfigPanel');
    const wrap = document.getElementById('petConfigsContainer');
    if (!panel || !wrap) return;
    panel.style.display = state.petConfigs.size > 0 ? '' : 'none';
    wrap.innerHTML = '';

    Array.from(state.petConfigs.keys()).forEach(petId => {
      const cfg = state.petConfigs.get(petId);
      const card = document.createElement('div');
      card.className = 'pet-config-card';
      card.innerHTML =
          '<div class="pet-config-title">' + escHtml(state.pets.get(petId)) + '</div>' +
          '<div class="svc-group">' +
          '<div class="svc-group-label">Nhóm dịch vụ <span class="chip-sub">(chọn được nhiều)</span></div>' +
          '<div class="chip-grid pet-category-grid"></div>' +
          '</div>' +
          '<div class="pet-service-container"></div>';

      const categoryGrid = card.querySelector('.pet-category-grid');
      state.categories.forEach(cat => {
        const chip = document.createElement('div');
        chip.className = 'chip' + (cfg.categoryIds.has(cat.id) ? ' selected' : '');
        chip.textContent = cat.name;
        chip.onclick = () => window.togglePetCategory(petId, cat.id);
        categoryGrid.appendChild(chip);
      });

      // Render 1 group dịch vụ/vaccine cho MỖI category đã chọn (multi-select).
      const serviceContainer = card.querySelector('.pet-service-container');
      state.categories
          .filter(cat => cfg.categoryIds.has(cat.id))
          .forEach(cat => {
            serviceContainer.appendChild(cat.id === state.vaccineCategoryId
                ? renderVaccineGroup(petId, cfg)
                : renderServiceGroup(petId, cfg, cat));
          });
      wrap.appendChild(card);
    });
  }

  function renderServiceGroup(petId, cfg, cat) {
    const group = document.createElement('div');
    group.className = 'svc-group';
    group.innerHTML = '<div class="svc-divider"></div><div class="svc-group-label">' + escHtml(cat.name) +
        ' <span class="chip-sub">(chọn được nhiều)</span></div><div class="chip-grid"></div>';
    const grid = group.querySelector('.chip-grid');
    (cat.services || []).forEach(svc => {
      const chip = document.createElement('div');
      chip.className = 'chip' + (cfg.serviceIds.has(svc.id) ? ' selected' : '');
      chip.innerHTML = escHtml(svc.name) + '<span class="chip-sub">' + formatVnd(svc.price) + 'đ</span>';
      chip.onclick = () => window.togglePetService(chip, petId, svc.id);
      grid.appendChild(chip);
    });
    return group;
  }

  function renderVaccineGroup(petId, cfg) {
    const group = document.createElement('div');
    group.className = 'svc-group';
    group.innerHTML =
        '<div class="svc-divider"></div><div class="svc-group-label">Vaccine <span class="chip-sub">(chọn được nhiều)</span></div>' +
        '<div class="chip-grid"></div><p class="bk-panel-hint" style="margin-top:8px;">Chỉ hiển thị vaccine còn đủ tồn kho.</p>';
    const grid = group.querySelector('.chip-grid');
    state.vaccines.forEach(v => {
      const chip = document.createElement('div');
      chip.className = 'chip' + (cfg.vaccineIds.has(v.id) ? ' selected' : '');
      chip.innerHTML = escHtml(v.name) + '<span class="chip-sub">' + formatVnd(v.price) + 'đ · còn ' + v.stock + '</span>';
      chip.onclick = () => window.togglePetVaccine(chip, petId, v.id);
      grid.appendChild(chip);
    });
    return group;
  }

  function buildPayload() {
    return Array.from(state.petConfigs.entries()).map(([petId, cfg]) => ({
      petId,
      serviceIds: Array.from(cfg.serviceIds),
      vaccineIds: Array.from(cfg.vaccineIds),
    })).filter(row => row.serviceIds.length > 0 || row.vaccineIds.length > 0);
  }

  function selectedServiceIdsForCapacity() {
    const ids = new Set();
    state.petConfigs.forEach(cfg => {
      cfg.serviceIds.forEach(id => ids.add(id));
      if (cfg.vaccineIds.size > 0 && state.vaccineServiceId > 0) ids.add(state.vaccineServiceId);
    });
    return Array.from(ids);
  }

  function refreshSlots() {
    clearTimeout(fetchTimer);
    const hasSelection = buildPayload().length > 0;
    document.getElementById('slotPanel').style.display = hasSelection ? '' : 'none';
    if (!hasSelection) {
      renderDateTabs();
      return;
    }
    fetchTimer = setTimeout(() => loadSlots(selectedServiceIdsForCapacity()), 180);
  }

  async function loadSlots(serviceIds) {
    try {
      const r = await fetch(CTX + '/booking/slots?serviceIds=' + encodeURIComponent((serviceIds || []).join(',')));
      if (!r.ok) throw new Error('Bad response: ' + r.status);
      applySlotData(await r.json());
    } catch (e) {
      showCapacityError('Không thể tải khung giờ. Vui lòng thử lại.');
    }
    renderDateTabs();
  }

  function applySlotData(data) {
    state.categories = data.categories || state.categories || [];
    state.vaccines = data.vaccines || state.vaccines || [];
    state.slots = data.slots || {};
    state.vaccineCategoryId = data.vaccineCategoryId || state.vaccineCategoryId;
    state.vaccineServiceId = data.vaccineServiceId || state.vaccineServiceId;
    state.inpatientServiceId = data.inpatientServiceId || state.inpatientServiceId;
    showCapacityError('');
  }

  function renderDateTabs() {
    const tabsEl = document.getElementById('dateTabs');
    const grid = document.getElementById('slotGrid');
    if (!tabsEl || !grid) return;
    const dates = Object.keys(state.slots || {}).sort();
    tabsEl.innerHTML = '';

    if (buildPayload().length === 0) {
      grid.innerHTML = '<div class="slot-loading">Vui lòng chọn thú cưng và dịch vụ trước</div>';
      return;
    }
    if (dates.length === 0) {
      grid.innerHTML = '<div class="slot-loading">Không có khung giờ phù hợp.</div>';
      return;
    }

    dates.slice(0, 14).forEach((ds, i) => {
      const tab = document.createElement('div');
      tab.className = 'date-tab' + (i === 0 ? ' active' : '');
      const d = new Date(ds + 'T00:00:00');
      tab.innerHTML = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'][d.getDay()] +
          '<br><small>' + d.getDate() + '/' + (d.getMonth() + 1) + '</small>';
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
    const slots = state.slots[ds] || [];
    grid.innerHTML = '';
    slots.forEach(slot => {
      const fill = Math.min(100, slot.fill || 0);
      const card = document.createElement('div');
      card.className = 'slot-card' + (!slot.available ? ' booked' : '') + (state.slotKey === slot.key ? ' selected' : '');
      card.innerHTML =
          '<div class="slot-time">' + escHtml(slot.display) + '</div>' +
          '<div class="slot-fill-bar"><div class="slot-fill-inner" style="width:' + fill + '%"></div></div>' +
          '<div class="slot-load">' + slot.load + '/' + slot.cap + ' chỗ</div>';
      if (slot.available) {
        card.onclick = () => {
          state.slotKey = slot.key;
          document.getElementById('slotKeyInput').value = slot.key;
          document.querySelectorAll('.slot-card').forEach(c => c.classList.remove('selected'));
          card.classList.add('selected');
          updateSummary();
        };
      }
      grid.appendChild(card);
    });
  }

  function updateSummary() {
    const petSection = document.getElementById('sumPetSection');
    const svcSection = document.getElementById('sumSvcSection');
    const slotSection = document.getElementById('sumSlotSection');
    const btnBook = document.getElementById('btnBook');
    let ready = false;

    if (state.mode === 'inpatient') {
      petSection.style.display = state.inpatientPetIds.size > 0 ? '' : 'none';
      document.getElementById('sumPets').textContent = state.inpatientPetIds.size + ' thú cưng';
      svcSection.style.display = '';
      document.getElementById('sumSvcs').textContent = 'Nội trú';
      slotSection.style.display = (state.inpatientDate && state.inpatientPeriod) ? '' : 'none';
      document.getElementById('sumSlot').textContent = (state.inpatientDate || '') + ' · ' +
          (state.inpatientPeriod === 'morning' ? 'Sáng' : state.inpatientPeriod === 'afternoon' ? 'Chiều' : '');
      ready = state.inpatientPetIds.size > 0 && !!state.inpatientDate && !!state.inpatientPeriod;
    } else {
      const payload = buildPayload();
      petSection.style.display = state.petConfigs.size > 0 ? '' : 'none';
      document.getElementById('sumPets').innerHTML = Array.from(state.pets.values())
          .map(name => '<span class="sum-chip">' + escHtml(name) + '</span>').join('');
      svcSection.style.display = payload.length > 0 ? '' : 'none';
      document.getElementById('sumSvcs').innerHTML = renderServiceSummary(payload);
      slotSection.style.display = state.slotKey ? '' : 'none';
      document.getElementById('sumSlot').textContent = state.slotKey ? state.slotKey.replace('|', ' · ') : '';
      // Nay cho phép NHIỀU mục (dịch vụ + vaccine), chỉ cần tối thiểu 1 mục.
      const itemCount = payload.length > 0 ? (payload[0].serviceIds.length + payload[0].vaccineIds.length) : 0;
      ready = payload.length === 1 && itemCount >= 1 && !!state.slotKey;
    }

    document.getElementById('sumPrice').textContent = formatVnd(currentTotal()) + 'đ';
    btnBook.disabled = !ready;
  }

  function renderServiceSummary(payload) {
    return payload.map(row => {
      const names = [...row.serviceIds.map(serviceNameById), ...row.vaccineIds.map(vaccineNameById)].filter(Boolean);
      return '<div class="sum-pet-row"><strong>' + escHtml(state.pets.get(row.petId)) +
          ':</strong> ' + escHtml(names.join(', ')) + '</div>';
    }).join('');
  }

  function currentTotal() {
    if (state.mode === 'inpatient') return 200000;
    let total = 0;
    state.petConfigs.forEach(cfg => {
      cfg.serviceIds.forEach(id => { total += servicePriceById(id); });
      cfg.vaccineIds.forEach(id => { total += vaccinePriceById(id); });
    });
    return total;
  }

  document.getElementById('bookingForm').addEventListener('submit', function (e) {
    const hidden = document.getElementById('hiddenInputs');
    hidden.innerHTML = '';
    document.getElementById('bookingPayloadInput').value = '';

    if (state.mode === 'inpatient') {
      state.inpatientPetIds.forEach(id => addHidden(hidden, 'petIds', id));
      return;
    }

    const payload = buildPayload();
    const itemCount = payload.length > 0 ? (payload[0].serviceIds.length + payload[0].vaccineIds.length) : 0;
    if (payload.length !== 1 || itemCount < 1 || !state.slotKey) {
      e.preventDefault();
      showCapacityError('Vui lòng chọn 1 thú cưng và ít nhất 1 dịch vụ hoặc vaccine, sau đó chọn khung giờ.');
      return;
    }
    document.getElementById('bookingPayloadInput').value = JSON.stringify(payload);
  });

  function addHidden(parent, name, value) {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    parent.appendChild(input);
  }

  function serviceById(id) {
    for (const cat of state.categories) {
      const found = (cat.services || []).find(s => s.id === id);
      if (found) return found;
    }
    return null;
  }

  function serviceNameById(id) {
    const svc = serviceById(id);
    return svc ? svc.name : '';
  }

  function servicePriceById(id) {
    const svc = serviceById(id);
    return svc ? Number(svc.price || 0) : 0;
  }

  function vaccineNameById(id) {
    const v = state.vaccines.find(item => item.id === id);
    return v ? v.name : '';
  }

  function vaccinePriceById(id) {
    const v = state.vaccines.find(item => item.id === id);
    return v ? Number(v.price || 0) : 0;
  }

  function showCapacityError(message) {
    const el = document.getElementById('capacityError');
    if (!el) return;
    el.textContent = message || '';
    el.style.display = message ? '' : 'none';
  }

  function formatVnd(n) {
    return new Intl.NumberFormat('vi-VN').format(Math.round(n || 0));
  }

  function escHtml(s) {
    if (s == null) return '';
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
  }

  async function init() {
    selectMode('normal');
    await loadSlots([]);
    renderPetConfigs();
    updateSummary();
  }

  init();
})();
