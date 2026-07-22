(function () {
  if (!document.getElementById('bookingForm')) return;

  const CTX = window.APP_CTX || '';
  const state = {
    mode: 'normal',
    // Không còn chọn thú cưng ở luồng booking (Appointments.PetID nullable)
    // — chỉ còn DUY NHẤT 1 cấu hình dịch vụ/vaccine cho cả lượt đặt lịch,
    // thay vì 1 Map theo từng pet như trước.
    config: { categoryIds: new Set(), serviceIds: new Set(), vaccineIds: new Set() },
    slotKey: null,
    inpatientDate: null,
    inpatientPeriod: null,
    categories: [],
    vaccines: [],
    slots: {},
    vaccineCategoryId: 4,
    vaccineServiceId: -1,
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

  // Multi-select: chọn/bỏ chọn 1 dịch vụ trong danh sách, KHÔNG xoá các lựa
  // chọn khác.
  window.togglePetService = function (el, serviceId) {
    const cfg = state.config;
    if (cfg.serviceIds.has(serviceId)) cfg.serviceIds.delete(serviceId);
    else cfg.serviceIds.add(serviceId);
    renderServiceConfig();
    clearSlotSelection();
    refreshSlots();
    updateSummary();
  };

  // Multi-select: chọn/bỏ chọn 1 vaccine, có thể chọn nhiều vaccine cùng lúc.
  window.togglePetVaccine = function (el, vaccineId) {
    const cfg = state.config;
    if (cfg.vaccineIds.has(vaccineId)) cfg.vaccineIds.delete(vaccineId);
    else cfg.vaccineIds.add(vaccineId);
    renderServiceConfig();
    clearSlotSelection();
    refreshSlots();
    updateSummary();
  };

  // Multi-select: cho phép chọn NHIỀU category dịch vụ cùng lúc. Bỏ chọn 1
  // category sẽ tự bỏ chọn các dịch vụ/vaccine thuộc category đó.
  window.togglePetCategory = function (petId, categoryId) {
    const cfg = state.config;
    if (cfg.categoryIds.has(categoryId)) {
      cfg.categoryIds.delete(categoryId);
      const cat = state.categories.find(c => c.id === categoryId);
      if (cat) (cat.services || []).forEach(s => cfg.serviceIds.delete(s.id));
      if (categoryId === state.vaccineCategoryId) cfg.vaccineIds.clear();
      clearSlotSelection();
    } else {
      cfg.categoryIds.add(categoryId);
    }
    renderServiceConfig();
    refreshSlots();
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

  function configHasAnything() {
    return state.config.categoryIds.size > 0 || state.config.serviceIds.size > 0 || state.config.vaccineIds.size > 0;
  }

  function renderServiceConfig() {
    const panel = document.getElementById('petConfigPanel');
    const wrap = document.getElementById('petConfigsContainer');
    if (!panel || !wrap) return;
    wrap.innerHTML = '';

    try {
      const cfg = state.config;
      const card = document.createElement('div');
      card.className = 'pet-config-card';
      card.innerHTML =
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
        chip.onclick = () => window.togglePetCategory(null, cat.id);
        categoryGrid.appendChild(chip);
      });

      // Render 1 group dịch vụ/vaccine cho MỖI category đã chọn (multi-select).
      const serviceContainer = card.querySelector('.pet-service-container');
      state.categories
          .filter(cat => cfg.categoryIds.has(cat.id))
          .forEach(cat => {
            serviceContainer.appendChild(cat.id === state.vaccineCategoryId
                ? renderVaccineGroup(cfg)
                : renderServiceGroup(cfg, cat));
          });
      wrap.appendChild(card);
    } catch (err) {
      console.error('renderServiceConfig failed', err);
    }
  }

  function renderServiceGroup(cfg, cat) {
    const group = document.createElement('div');
    group.className = 'svc-group';
    group.innerHTML = '<div class="svc-divider"></div><div class="svc-group-label">' + escHtml(cat.name) +
        ' <span class="chip-sub">(chọn được nhiều)</span></div><div class="chip-grid"></div>';
    const grid = group.querySelector('.chip-grid');
    const services = cat.services || [];
    if (services.length === 0) {
      grid.innerHTML = '<p class="bk-panel-hint" style="margin:0;">Nhóm dịch vụ này hiện chưa có dịch vụ nào khả dụng.</p>';
      return group;
    }
    services.forEach(svc => {
      const chip = document.createElement('div');
      chip.className = 'chip' + (cfg.serviceIds.has(svc.id) ? ' selected' : '');
      chip.innerHTML = escHtml(svc.name) + '<span class="chip-sub">' + formatVnd(svc.price) + 'đ</span>';
      chip.onclick = () => window.togglePetService(chip, svc.id);
      grid.appendChild(chip);
    });
    return group;
  }

  function renderVaccineGroup(cfg) {
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
      chip.onclick = () => window.togglePetVaccine(chip, v.id);
      grid.appendChild(chip);
    });
    return group;
  }

  function buildPayload() {
    return {
      serviceIds: Array.from(state.config.serviceIds),
      vaccineIds: Array.from(state.config.vaccineIds),
    };
  }

  function payloadItemCount(payload) {
    return payload.serviceIds.length + payload.vaccineIds.length;
  }

  function selectedServiceIdsForCapacity() {
    const ids = new Set();
    state.config.serviceIds.forEach(id => ids.add(id));
    if (state.config.vaccineIds.size > 0 && state.vaccineServiceId > 0) ids.add(state.vaccineServiceId);
    return Array.from(ids);
  }

  function refreshSlots() {
    clearTimeout(fetchTimer);
    const hasSelection = payloadItemCount(buildPayload()) > 0;
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
    showCapacityError('');

    // Neu slotKey da co san (vd khoi phuc tu luot dat lich truoc do, hoac
    // slot vua bi nguoi khac dat mat trong luc dang xem trang), kiem tra lai
    // no co con ton tai/kha dung trong du lieu vua tai khong - tranh submit
    // 1 slotKey da het han ma khong hay biet.
    const input = document.getElementById('slotKeyInput');
    if (state.slotKey) {
      const ds = state.slotKey.split('|')[0];
      const daySlots = state.slots[ds] || [];
      const stillValid = daySlots.some(s => s.key === state.slotKey && s.available);
      if (!stillValid) {
        state.slotKey = null;
        if (input) input.value = '';
        showCapacityError('Khung giờ đã chọn trước đó không còn khả dụng, vui lòng chọn lại.');
      } else if (input) {
        input.value = state.slotKey;
      }
    }
  }

  function renderDateTabs() {
    const tabsEl = document.getElementById('dateTabs');
    const grid = document.getElementById('slotGrid');
    if (!tabsEl || !grid) return;
    const dates = Object.keys(state.slots || {}).sort();
    tabsEl.innerHTML = '';

    if (payloadItemCount(buildPayload()) === 0) {
      grid.innerHTML = '<div class="slot-loading">Vui lòng chọn dịch vụ trước</div>';
      return;
    }
    if (dates.length === 0) {
      grid.innerHTML = '<div class="slot-loading">Không có khung giờ phù hợp.</div>';
      return;
    }

    // Neu da co slotKey san (vd khoi phuc tu luot dat lich truoc do), uu
    // tien mo dung tab ngay tuong ung thay vi luon mac dinh tab dau tien.
    const preferredDate = state.slotKey ? state.slotKey.split('|')[0] : null;
    let activeIndex = 0;
    if (preferredDate) {
      const idx = dates.indexOf(preferredDate);
      if (idx >= 0) activeIndex = idx;
    }

    dates.slice(0, 14).forEach((ds, i) => {
      const tab = document.createElement('div');
      tab.className = 'date-tab' + (i === activeIndex ? ' active' : '');
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
    renderSlots(dates[Math.min(activeIndex, dates.length - 1)]);
  }

  function renderSlots(ds) {
    const grid = document.getElementById('slotGrid');
    const slots = state.slots[ds] || [];
    grid.innerHTML = '';
    slots.forEach(slot => {
      const fill = Math.min(100, slot.fill || 0);
      const card = document.createElement('div');
      card.className = 'slot-card' + (!slot.available ? ' booked' : '') + (state.slotKey === slot.key ? ' selected' : '');
      // slot.placeholder = true nghĩa là chưa chọn dịch vụ/vaccine nào — số
      // liệu load/cap lúc này chỉ là giá trị giả để hiển thị bố cục, KHÔNG
      // phải sức chứa thật, nên không hiển thị dạng "0/100 chỗ" gây hiểu lầm.
      const loadText = slot.placeholder
          ? 'Chọn dịch vụ để xem số chỗ'
          : (slot.load + '/' + slot.cap + ' chỗ');
      card.innerHTML =
          '<div class="slot-time">' + escHtml(slot.display) + '</div>' +
          '<div class="slot-fill-bar"><div class="slot-fill-inner" style="width:' + (slot.placeholder ? 0 : fill) + '%"></div></div>' +
          '<div class="slot-load">' + loadText + '</div>';
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
    const svcSection = document.getElementById('sumSvcSection');
    const slotSection = document.getElementById('sumSlotSection');
    const btnBook = document.getElementById('btnBook');
    let ready = false;

    if (state.mode === 'inpatient') {
      svcSection.style.display = '';
      document.getElementById('sumSvcs').textContent = 'Nội trú';
      slotSection.style.display = (state.inpatientDate && state.inpatientPeriod) ? '' : 'none';
      document.getElementById('sumSlot').textContent = (state.inpatientDate || '') + ' · ' +
          (state.inpatientPeriod === 'morning' ? 'Sáng' : state.inpatientPeriod === 'afternoon' ? 'Chiều' : '');
      ready = !!state.inpatientDate && !!state.inpatientPeriod;
    } else {
      const payload = buildPayload();
      const itemCount = payloadItemCount(payload);
      svcSection.style.display = itemCount > 0 ? '' : 'none';
      document.getElementById('sumSvcs').innerHTML = renderServiceSummary(payload);
      slotSection.style.display = state.slotKey ? '' : 'none';
      document.getElementById('sumSlot').textContent = state.slotKey ? state.slotKey.replace('|', ' · ') : '';
      ready = itemCount >= 1 && !!state.slotKey;
    }

    document.getElementById('sumPrice').textContent = formatVnd(currentTotal()) + 'đ';
    btnBook.disabled = !ready;
  }

  function renderServiceSummary(payload) {
    const names = [...payload.serviceIds.map(serviceNameById), ...payload.vaccineIds.map(vaccineNameById)].filter(Boolean);
    if (names.length === 0) return '';
    return '<div class="sum-pet-row">' + escHtml(names.join(', ')) + '</div>';
  }

  function currentTotal() {
    if (state.mode === 'inpatient') return 200000;
    let total = 0;
    state.config.serviceIds.forEach(id => { total += servicePriceById(id); });
    state.config.vaccineIds.forEach(id => { total += vaccinePriceById(id); });
    return total;
  }

  document.getElementById('bookingForm').addEventListener('submit', function (e) {
    const hidden = document.getElementById('hiddenInputs');
    hidden.innerHTML = '';
    document.getElementById('bookingPayloadInput').value = '';

    if (state.mode === 'inpatient') {
      if (!state.inpatientDate || !state.inpatientPeriod) {
        e.preventDefault();
        showCapacityError('Vui lòng chọn ngày và buổi nhập viện.');
      }
      return;
    }

    const payload = buildPayload();
    const itemCount = payloadItemCount(payload);
    if (itemCount < 1 || !state.slotKey) {
      e.preventDefault();
      showCapacityError('Vui lòng chọn ít nhất 1 dịch vụ hoặc vaccine, sau đó chọn khung giờ.');
      return;
    }
    document.getElementById('bookingPayloadInput').value = JSON.stringify(payload);
  });

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

  /**
   * Khoi phuc lai lua chon cua luot dat lich truoc do (category/dich vu/
   * vaccine/khung gio/ghi chu) tu window.BOOKING_RESUME - du lieu nay do
   * NewServlet dong goi tu session, chi co khi khach da tung nhap dang do
   * (vd bam "Quay lai chinh sua" tu trang confirm). Khong con khoi phuc
   * thu cung (booking khong con chon pet). Tra ve true neu co gi do duoc
   * khoi phuc.
   */
  function applyResumeIfAny() {
    const resume = window.BOOKING_RESUME;
    if (!resume) return false;

    if (resume.isInpatient) {
      selectMode('inpatient');
      if (resume.inpatientDate) {
        const dateInput = document.getElementById('inpDate');
        if (dateInput) dateInput.value = resume.inpatientDate;
        window.onInpatientDateChange(resume.inpatientDate);
      }
      if (resume.inpatientPeriod) window.selectPeriod(resume.inpatientPeriod);
    } else {
      selectMode('normal');
      let payload = { serviceIds: [], vaccineIds: [] };
      try { payload = JSON.parse(resume.payload || '{}'); } catch (e) { payload = { serviceIds: [], vaccineIds: [] }; }

      const cfg = {
        categoryIds: new Set(),
        serviceIds: new Set(payload.serviceIds || []),
        vaccineIds: new Set(payload.vaccineIds || []),
      };
      // Payload goc chi luu serviceIds/vaccineIds cu the, khong luu
      // categoryIds da chon - suy nguoc lai category tu cac service/
      // vaccine da chon do de hien thi dung cac nhom da mo san.
      state.categories.forEach(cat => {
        const hasSelectedService = (cat.services || []).some(s => cfg.serviceIds.has(s.id));
        const isVaccineCatSelected = cat.id === state.vaccineCategoryId && cfg.vaccineIds.size > 0;
        if (hasSelectedService || isVaccineCatSelected) cfg.categoryIds.add(cat.id);
      });
      state.config = cfg;

      state.slotKey = resume.slotKey || null;
    }

    const notesEl = document.getElementById('notesInput');
    if (notesEl && resume.notes) notesEl.value = resume.notes;

    return true;
  }

  async function init() {
    selectMode('normal');
    await loadSlots([]);

    const resumed = applyResumeIfAny();
    if (resumed && state.mode === 'normal' && configHasAnything()) {
      document.getElementById('slotPanel').style.display = '';
      // Tai lai slot theo dung lua chon vua khoi phuc (applySlotData se tu
      // kiem tra state.slotKey con hop le khong va huy neu khong con).
      await loadSlots(selectedServiceIdsForCapacity());
    }

    renderServiceConfig();
    updateSummary();
  }

  init();
})();
