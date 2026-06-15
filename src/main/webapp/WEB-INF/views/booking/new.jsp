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
    <p>Chọn dịch vụ, thú cưng và một khung giờ phù hợp</p>
  </div>

  <c:if test="${not empty requestScope.error}">
    <div class="bk-alert bk-alert--error">✗ ${requestScope.error}</div>
  </c:if>

  <c:if test="${empty pets}">
    <div class="bk-alert bk-alert--info">
      🐾 Bạn chưa có thú cưng nào. <a href="${ctx}/pets/new">Thêm thú cưng</a> trước khi đặt lịch.
    </div>
  </c:if>

  <form action="${ctx}/booking/new" method="post" id="bookingForm">
    <input type="hidden" name="isInpatient"     id="isInpatientInput" value="false">
    <input type="hidden" name="inpatientDate"   id="inpatientDateInput" value="">
    <input type="hidden" name="inpatientPeriod" id="inpatientPeriodInput" value="">
    <input type="hidden" name="slotKey"         id="slotKeyInput" value="">

    <div class="bk-layout">
      <!-- LEFT col -->
      <div class="bk-panels">

        <!-- Panel 1: Category chips -->
        <div class="bk-panel">
          <div class="bk-panel-head">Loại dịch vụ</div>
          <div class="bk-panel-body">
            <div class="chip-grid" id="catChips">
              <c:forEach var="cat" items="${categories}">
                <div class="chip" data-cat-id="${cat.categoryID}" onclick="toggleCategory(this,${cat.categoryID})">${cat.name}</div>
              </c:forEach>
            </div>
          </div>
        </div>

        <!-- Panel 2: Service chips (dynamic) -->
        <div class="bk-panel">
          <div class="bk-panel-head">Dịch vụ</div>
          <div class="bk-panel-body">
            <div id="svcEmpty" style="color:var(--warm-gray);font-size:13.5px;">Hãy chọn loại dịch vụ trước</div>
            <div class="chip-grid" id="svcChips" style="display:none;"></div>
          </div>
        </div>

        <!-- Panel 3: Pet chips -->
        <div class="bk-panel">
          <div class="bk-panel-head">Thú cưng</div>
          <div class="bk-panel-body">
            <c:choose>
              <c:when test="${not empty pets}">
                <div class="chip-grid">
                  <c:forEach var="pet" items="${pets}">
                    <div class="chip" data-pet-id="${pet.petID}" onclick="togglePet(this,${pet.petID},'${pet.name}')">
                      ${pet.name} <span style="font-size:11px;opacity:.6;">(${pet.speciesName})</span>
                    </div>
                  </c:forEach>
                </div>
              </c:when>
              <c:otherwise><p style="color:var(--warm-gray);font-size:13.5px;">Chưa có thú cưng.</p></c:otherwise>
            </c:choose>
          </div>
        </div>

        <!-- Panel 4a: Outpatient slot picker -->
        <div class="bk-panel" id="slotPanel">
          <div class="bk-panel-head">Chọn khung giờ <span class="bk-panel-hint">(120 phút · chọn 1 ca)</span></div>
          <div class="bk-panel-body">
            <div class="date-tabs" id="dateTabs"></div>
            <div class="slot-grid" id="slotGrid">
              <div style="grid-column:1/-1;color:var(--warm-gray);font-size:13.5px;">Đang tải lịch...</div>
            </div>
          </div>
        </div>

        <!-- Panel 4b: Inpatient date + period -->
        <div class="bk-panel" id="inpatientPanel" style="display:none;">
          <div class="bk-panel-head">Chọn ngày và buổi nội trú</div>
          <div class="bk-panel-body">
            <div class="inpatient-row">
              <div class="form-field">
                <label>Ngày nhập viện</label>
                <input type="date" id="inpDate" min="${today}"
                  style="width:100%;padding:10px 12px;border:1.5px solid var(--border);border-radius:8px;font-size:14px;font-family:inherit;"
                  onchange="onInpatientDateChange(this.value)">
              </div>
              <div id="inpPeriodWrap" style="display:none;">
                <label style="font-size:13px;color:var(--warm-gray);margin-bottom:8px;display:block;">Buổi</label>
                <div class="period-btns">
                  <button type="button" class="period-btn" id="btnMorning"   onclick="selectPeriod('morning')">Sáng (08:00–12:00)</button>
                  <button type="button" class="period-btn" id="btnAfternoon" onclick="selectPeriod('afternoon')">Chiều (13:30–17:30)</button>
                </div>
                <div id="inpSlotAvail" style="font-size:12.5px;margin-top:10px;color:var(--warm-gray);"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Panel 5: Notes -->
        <div class="bk-panel">
          <div class="bk-panel-head">Ghi chú</div>
          <div class="bk-panel-body">
            <textarea name="notes" rows="3"
              style="width:100%;padding:10px 12px;border:1.5px solid var(--border);border-radius:8px;
                     font-size:14px;font-family:inherit;resize:vertical;outline:none;"
              placeholder="Mô tả triệu chứng, yêu cầu đặc biệt..."></textarea>
          </div>
        </div>
      </div><!-- /bk-panels -->

      <!-- RIGHT col: summary -->
      <div class="bk-summary">
        <h3>Tóm tắt</h3>
        <div class="sum-row"><span class="sum-label">Dịch vụ</span><span class="sum-val" id="sumSvc">Chưa chọn</span></div>
        <div class="sum-row"><span class="sum-label">Thú cưng</span><span class="sum-val" id="sumPet">Chưa chọn</span></div>
        <div class="sum-row"><span class="sum-label">Thời gian</span><span class="sum-val" id="sumSlot">Chưa chọn</span></div>
        <div class="sum-row sum-row--total"><span class="sum-label">Ước tính</span><span class="sum-val sum-price" id="sumPrice">—</span></div>
        <button type="submit" class="btn-book" id="btnBook" disabled>Tiếp theo →</button>
        <p class="sum-hint">Bạn sẽ được xem lại trước khi thanh toán</p>
      </div>
    </div><!-- /bk-layout -->

    <!-- Hidden arrays -->
    <div id="hiddenInputs"></div>
  </form>
</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script>
    // Define functions EARLY so they are available during DOM rendering
    let CATS = [];
    let SLOTS_DATA = {};
    const TODAY = '<c:out value="${today}" />';

    // Parse JSON blobs passed from servlet (use c:out escapeXml=false to emit raw JSON)
    try {
      const catsRaw = '<c:out value="${categoriesJson}" escapeXml="false" />';
      CATS = catsRaw ? JSON.parse(catsRaw) : [];
    } catch (e) {
      console.error('Failed to parse categoriesJson:', e);
      CATS = [];
    }
    try {
      const slotsRaw = '<c:out value="${slotsJson}" escapeXml="false" />';
      SLOTS_DATA = slotsRaw ? JSON.parse(slotsRaw) : {};
    } catch (e) {
      console.error('Failed to parse slotsJson:', e);
      SLOTS_DATA = {};
    }

    // ── State ──────────────────────────────────────────────────────────────────
    const state = {
      cats:     new Set(),
      services: new Map(),   // id → {id, name, price, duration}
      pets:     new Map(),   // id → name
      slotKey:  '',
      slotDisp: '',
      inpatient:    false,
      inpDate:  '',
      inpPeriod:'',
    };

    // ── Category ──────��────────────────────────────────────────────────────────
    function toggleCategory(el, id) {
      console.log('toggleCategory: el=', el, 'id=', id);
      try {
        if (state.cats.has(id)) {
          state.cats.delete(id);
          el.classList.remove('selected');
          console.log('Deselected category:', id);
        }
        else {
          state.cats.add(id);
          el.classList.add('selected');
          console.log('Selected category:', id);
        }

        // Detect inpatient by category name
        let inpatient = false;
        state.cats.forEach(cid => {
          const cat = CATS.find(c => c.id === cid);
          if (cat && /nội trú|inpatient|nhập viện/i.test(cat.name)) inpatient = true;
        });
        state.inpatient = inpatient;
        document.getElementById('isInpatientInput').value = inpatient ? 'true' : 'false';
        document.getElementById('slotPanel').style.display     = inpatient ? 'none' : '';
        document.getElementById('inpatientPanel').style.display= inpatient ? '' : 'none';

        renderServiceChips();
        updateSummary();
      } catch (e) {
        console.error('Lỗi toggleCategory:', e);
      }
    }

    function renderServiceChips() {
      console.log('renderServiceChips called, CATS=', CATS);
      try {
        const grid  = document.getElementById('svcChips');
        const empty = document.getElementById('svcEmpty');
        grid.innerHTML = '';
        let any = false;

        if (!CATS || !Array.isArray(CATS)) {
          console.warn('CATS không phải array');
          return;
        }

        CATS.forEach(cat => {
          if (!state.cats.has(cat.id)) return;
          if (!cat.services || !Array.isArray(cat.services)) {
            console.warn('Services không phải array cho category:', cat.id);
            return;
          }

          cat.services.forEach(svc => {
            any = true;
            const div = document.createElement('div');
            const sel = state.services.has(svc.id);
            div.className = 'chip' + (sel ? ' selected' : '');
            div.innerHTML = svc.name + '<span style="font-size:11px;opacity:.6;margin-left:4px;">'
                          + Number(svc.price).toLocaleString('vi-VN') + '₫</span>';
            div.onclick = () => toggleService(div, svc);
            grid.appendChild(div);
          });
        });
        empty.style.display = any ? 'none' : '';
        grid.style.display  = any ? 'flex'  : 'none';

        // remove services no longer in selected cats
        const validIds = new Set();
        CATS.forEach(cat => {
          if (state.cats.has(cat.id) && cat.services && Array.isArray(cat.services)) {
            cat.services.forEach(s => validIds.add(s.id));
          }
        });
        for (const id of state.services.keys()) if (!validIds.has(id)) state.services.delete(id);
      } catch (e) {
        console.error('Lỗi renderServiceChips:', e);
      }
    }

    function toggleService(el, svc) {
      console.log('toggleService: svc=', svc);
      try {
        if (state.services.has(svc.id)) {
          state.services.delete(svc.id);
          el.classList.remove('selected');
          console.log('Deselected service:', svc.id);
        }
        else {
          state.services.set(svc.id, svc);
          el.classList.add('selected');
          console.log('Selected service:', svc.id);
        }
        // Regenerate slots with selected service IDs for capacity check
        if (!state.inpatient) regenerateSlots();
        updateSummary();
      } catch (e) {
        console.error('Lỗi toggleService:', e);
      }
    }

    // ── Pets ───────────────────────────────────────────────────────────────────
    function togglePet(el, id, name) {
      console.log('togglePet: id=', id, 'name=', name);
      try {
        if (state.pets.has(id)) {
          state.pets.delete(id);
          el.classList.remove('selected');
          console.log('Deselected pet:', id);
        }
        else {
          state.pets.set(id, name);
          el.classList.add('selected');
          console.log('Selected pet:', id);
        }
        updateSummary();
      } catch (e) {
        console.error('Lỗi togglePet:', e);
      }
    }

    // ── Outpatient slots ───────────────────────────────────────────────────────
    const DAYS_VN = ['CN','T2','T3','T4','T5','T6','T7'];

    function renderDateTabs() {
      console.log('renderDateTabs called');
      try {
        const el = document.getElementById('dateTabs');

        if (!SLOTS_DATA || typeof SLOTS_DATA !== 'object') {
          console.error('SLOTS_DATA không hợp lệ');
          el.innerHTML = '<span style="color:var(--warm-gray);font-size:13.5px;">Không thể tải lịch.</span>';
          return;
        }

        const dates  = Object.keys(SLOTS_DATA).sort();
        console.log('Dates available:', dates);
        el.innerHTML = '';
        if (!dates.length) {
          el.innerHTML = '<span style="color:var(--warm-gray);font-size:13.5px;">Không có lịch trống trong 30 ngày tới.</span>';
          return;
        }
        dates.forEach((ds, i) => {
          const d   = new Date(ds + 'T00:00:00');
          const tab = document.createElement('div');
          tab.className   = 'date-tab' + (i === 0 ? ' active' : '');
          tab.dataset.date= ds;
          tab.innerHTML   = '<div class="dt-day">' + DAYS_VN[d.getDay()] + '</div><div class="dt-date">' + String(d.getDate()).padStart(2,'0') + '/' + String(d.getMonth()+1).padStart(2,'0') + '</div>';
          tab.onclick     = () => { document.querySelectorAll('.date-tab').forEach(t=>t.classList.remove('active')); tab.classList.add('active'); renderSlots(ds); };
          el.appendChild(tab);
        });
        if (dates.length > 0) renderSlots(dates[0]);
      } catch (e) {
        console.error('Lỗi renderDateTabs:', e);
      }
    }

    function renderSlots(ds) {
      console.log('renderSlots called for date:', ds);
      try {
        const grid  = document.getElementById('slotGrid');
        const slots = (SLOTS_DATA && SLOTS_DATA[ds]) ? SLOTS_DATA[ds] : [];
        console.log('Slots for date:', slots);
        grid.innerHTML = '';
        if (!slots || !Array.isArray(slots) || !slots.length) {
          grid.innerHTML = '<div style="grid-column:1/-1;color:var(--warm-gray);">Không có ca trống.</div>';
          return;
        }

        slots.forEach(slot => {
          const wrap = document.createElement('div');
          wrap.className = 'slot-card' + (!slot.available ? ' booked' : '') + (state.slotKey === slot.key ? ' selected' : '');
          wrap.innerHTML =
            '<div class="slot-time">' + slot.display + '</div>' +
            '<div class="slot-fill-bar"><div class="slot-fill-inner" style="width:' + slot.fill + '%"></div></div>' +
            '<div class="slot-load">' + slot.load + '/' + Math.ceil(slot.cap) + ' chỗ</div>';
          if (slot.available) wrap.onclick = () => selectSlot(slot, wrap);
          else                wrap.title   = 'Ca này đã đầy';
          grid.appendChild(wrap);
        });
      } catch (e) {
        console.error('Lỗi renderSlots:', e);
      }
    }

    function selectSlot(slot, el) {
      console.log('selectSlot: slot=', slot);
      try {
        document.querySelectorAll('.slot-card.selected').forEach(b => b.classList.remove('selected'));
        state.slotKey  = slot.key;
        state.slotDisp = slot.display;
        document.getElementById('slotKeyInput').value = slot.key;
        el.classList.add('selected');
        console.log('Selected slot:', slot.key);
        updateSummary();
      } catch (e) {
        console.error('Lỗi selectSlot:', e);
      }
    }

    async function regenerateSlots() {
      // AJAX to refresh slot availability with selected service IDs
      const ids = Array.from(state.services.keys()).join(',');
      try {
        const r = await fetch('${ctx}/booking/slots?serviceIds=' + ids);
        const data = await r.json();
        if (data && typeof data === 'object') {
          Object.assign(SLOTS_DATA, data);
          const activeDate = document.querySelector('.date-tab.active')?.dataset.date;
          if (activeDate && SLOTS_DATA[activeDate]) renderSlots(activeDate);
        }
      } catch(e) {
        console.error('Lỗi khi tải lịch:', e);
        /* keep existing SLOTS_DATA */
      }
    }

    // ── Inpatient ──────────────────────────────────────────────────────────────
    function onInpatientDateChange(val) {
      console.log('onInpatientDateChange called with val:', val);
      try {
        state.inpDate = val;
        document.getElementById('inpatientDateInput').value = val;
        document.getElementById('inpPeriodWrap').style.display = val ? '' : 'none';
        state.inpPeriod = '';
        document.getElementById('inpatientPeriodInput').value = '';
        document.querySelectorAll('.period-btn').forEach(b => b.classList.remove('active'));
        document.getElementById('inpSlotAvail').textContent = '';
        updateSummary();
      } catch (e) {
        console.error('Lỗi onInpatientDateChange:', e);
      }
    }

    function selectPeriod(period) {
      console.log('selectPeriod called with period:', period);
      try {
        state.inpPeriod = period;
        document.getElementById('inpatientPeriodInput').value = period;
        document.querySelectorAll('.period-btn').forEach(b => b.classList.remove('active'));
        document.getElementById(period === 'morning' ? 'btnMorning' : 'btnAfternoon').classList.add('active');
        // Show availability note
        document.getElementById('inpSlotAvail').textContent = period === 'morning'
          ? '🌅 Buổi sáng: 08:00 – 12:00' : '🌆 Buổi chiều: 13:30 – 17:30';
        updateSummary();
      } catch (e) {
        console.error('Lỗi selectPeriod:', e);
      }
    }

    // ── Summary + validation ───────────────────────────────────────────────────
    function updateSummary() {
      try {
        const svcNames = Array.from(state.services.values()).map(s=>s.name);
        const petNames = Array.from(state.pets.values());
        console.log('updateSummary: services=', svcNames, 'pets=', petNames);

        setSum('sumSvc',  svcNames.length ? svcNames.join(', ') : null);
        setSum('sumPet',  petNames.length ? petNames.join(', ') : null);

        if (state.inpatient) {
          const slot = state.inpDate && state.inpPeriod
            ? (state.inpPeriod === 'morning' ? 'Sáng 08:00–12:00' : 'Chiều 13:30–17:30') + ', ' + fmtDate(state.inpDate)
            : null;
          setSum('sumSlot', slot);
        } else {
          setSum('sumSlot', state.slotKey ? state.slotDisp : null);
        }

        // Price
        const total = Array.from(state.services.values())
          .reduce((s, svc) => s + Number(svc.price), 0) * Math.max(1, state.pets.size);
        document.getElementById('sumPrice').textContent = total > 0
          ? total.toLocaleString('vi-VN') + '₫' : '—';

        // Enable submit
        const slotOk = state.inpatient ? (!!state.inpDate && !!state.inpPeriod) : !!state.slotKey;
        document.getElementById('btnBook').disabled =
          !(state.services.size > 0 && state.pets.size > 0 && slotOk);
      } catch (e) {
        console.error('Lỗi updateSummary:', e);
      }
    }

    function setSum(id, val) {
      try {
        const el = document.getElementById(id);
        if (!el) {
          console.warn('Element not found:', id);
          return;
        }
        el.textContent  = val || 'Chưa chọn';
        el.style.color  = val ? 'var(--text-dark)' : 'var(--warm-gray)';
        el.style.fontWeight = val ? '500' : '400';
      } catch (e) {
        console.error('Lỗi setSum:', e);
      }
    }

    function fmtDate(s) {
      if (!s) return '';
      const [y,m,d] = s.split('-');
      return d+'/'+m+'/'+y;
    }

    // ── Submit: inject hidden arrays ───────────────────────────────────────────
    function initFormListener() {
      const form = document.getElementById('bookingForm');
      if (form) {
        form.addEventListener('submit', function(e) {
          e.preventDefault();
          const wrap = document.getElementById('hiddenInputs');
          wrap.innerHTML = '';
          state.services.forEach((_,id) => addHidden(wrap, 'serviceIds', id));
          state.pets.forEach((_,id)     => addHidden(wrap, 'petIds',     id));
          this.submit();
        });
      }
    }

    function addHidden(parent, name, val) {
      const inp = document.createElement('input');
      inp.type = 'hidden'; inp.name = name; inp.value = val;
      parent.appendChild(inp);
    }

    // ── Prefill from URL ───────────────────────────────────────────────────────
    function doPrefill() {
      const p = new URLSearchParams(location.search);
      const pc = parseInt(p.get('prefillCategory'));
      const ps = parseInt(p.get('prefillService'));
      if (pc) { const el = document.querySelector('[data-cat-id="'+pc+'"]'); if(el) el.click(); }
      if (ps) { setTimeout(() => { const el = document.querySelector('[data-svc-id="'+ps+'"]'); if(el) el.click(); }, 80); }
    }

    // ── Init ───────────────────────────────────────────────────────────────────
    function initBookingPage() {
      try {
        console.log('=== Initializing booking page ===');
        renderDateTabs();
        initFormListener();
        doPrefill();
        console.log('✓ Booking page initialized successfully');
      } catch(e) {
        console.error('✗ Error initializing booking page:', e);
        const dt = document.getElementById('dateTabs');
        if (dt) dt.innerHTML = '<span style="color:red;">Lỗi tải trang. Vui lòng tải lại.</span>';
      }
    }

    // Add init handler when DOM is ready
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', initBookingPage);
    } else {
      initBookingPage();
    }
  </script>

</body>
</html>
