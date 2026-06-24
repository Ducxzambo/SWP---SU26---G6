<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Thú cưng của tôi – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/pets.css">
  <style>
    /* ── Overrides for simplified list view ────────────────────── */
    .pets-wrap { max-width: 760px; }

    /* FAB add button */
    .fab-add {
      position: fixed;
      bottom: 32px; right: 32px;
      width: 52px; height: 52px;
      background: var(--green-700);
      color: #fff;
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 24px; line-height: 1;
      text-decoration: none;
      box-shadow: 0 4px 16px rgba(15,61,36,.35);
      transition: var(--transition);
      z-index: 100;
    }
    .fab-add:hover { background: var(--green-900); transform: scale(1.08); }

    /* Search + filter bar */
    .pet-filter-bar {
      display: flex; gap: 10px; align-items: center;
      margin-bottom: 18px; flex-wrap: wrap;
    }
    .pet-search-wrap {
      flex: 1; min-width: 180px;
      display: flex; align-items: center;
      border: 1.5px solid var(--border);
      border-radius: 10px; background: #fff; overflow: hidden;
    }
    .pet-search-icon { padding: 0 12px; color: var(--warm-gray); font-size: 15px; flex-shrink:0; }
    .pet-search-input {
      border: none; outline: none; font-family: 'DM Sans', sans-serif;
      font-size: 13.5px; padding: 9px 12px 9px 0;
      flex: 1; color: var(--text-dark); background: #fff;
    }
    .pet-search-input::placeholder { color: var(--warm-gray); }
    .pet-filter-select {
      border: 1.5px solid var(--border); border-radius: 10px;
      padding: 9px 12px; font-family: 'DM Sans', sans-serif;
      font-size: 13px; color: var(--text-mid); background: #fff;
      cursor: pointer; outline: none; min-width: 130px;
    }
    .pet-filter-select:focus { border-color: var(--green-500); }
    .pet-filter-count {
      font-size: 12.5px; color: var(--warm-gray); white-space: nowrap;
    }

    /* Simplified list row */
    .pet-list { display: flex; flex-direction: column; gap: 10px; }
    .pet-row {
      display: flex; align-items: center; gap: 16px;
      background: #fff;
      border: 1.5px solid var(--border);
      border-radius: var(--radius);
      padding: 14px 18px;
      text-decoration: none; color: inherit;
      transition: var(--transition);
    }
    .pet-row:hover {
      border-color: var(--green-400);
      box-shadow: var(--shadow-sm);
      transform: translateX(3px);
    }
    .pet-row-avatar {
      width: 46px; height: 46px; border-radius: 50%;
      background: var(--green-50);
      display: flex; align-items: center; justify-content: center;
      font-size: 24px; flex-shrink: 0;
      border: 1.5px solid var(--green-100);
    }
    .pet-row-main { flex: 1; min-width: 0; }
    .pet-row-name {
      font-size: 15px; font-weight: 600; color: var(--text-dark);
      margin-bottom: 3px;
    }
    .pet-row-sub {
      font-size: 12.5px; color: var(--warm-gray);
      display: flex; flex-wrap: wrap; gap: 8px;
    }
    .pet-row-sub span::before { content: '·'; margin-right: 8px; }
    .pet-row-sub span:first-child::before { content: ''; margin: 0; }
    .pet-row-stats {
      display: flex; gap: 20px; flex-shrink: 0; align-items: center;
    }
    .pet-row-stat { text-align: center; }
    .pet-row-stat-num {
      font-size: 17px; font-weight: 700; color: var(--green-700); display: block;
    }
    .pet-row-stat-lbl { font-size: 11px; color: var(--warm-gray); }
    .pet-row-arrow {
      color: var(--border); font-size: 20px; flex-shrink: 0;
    }
    /* Badge: vaccine status */
    .pet-vax-badge {
      font-size: 11.5px; font-weight: 600;
      padding: 2px 9px; border-radius: 20px; white-space: nowrap;
    }
    .pet-vax-badge.overdue  { background: #f8d7da; color: #721c24; }
    .pet-vax-badge.due-soon { background: #fff3cd; color: #856404; }
    .pet-vax-badge.ok       { background: var(--green-50); color: var(--green-700); }

    .pet-no-results {
      text-align: center; padding: 40px; color: var(--warm-gray); display: none;
    }
    .pet-row.hidden { display: none !important; }

    @media (max-width: 600px) {
      .pet-row-stats { display: none; }
      .fab-add { bottom: 20px; right: 20px; }
    }
  </style>
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="pets-wrap">

  <div class="pets-page-header">
    <div>
      <h1>Thú cưng của tôi</h1>
      <p>Quản lý hồ sơ và lịch sử khám của tất cả thú cưng</p>
    </div>
  </div>

  <c:choose>
    <c:when test="${empty pets}">
      <div class="pets-empty">
        <div class="pets-empty-icon">🐾</div>
        <h2>Chưa có thú cưng nào</h2>
        <p>Thêm thú cưng để bắt đầu đặt lịch khám và theo dõi sức khoẻ</p>
        <a href="${ctx}/pets/new" class="btn-new-booking">Thêm thú cưng đầu tiên</a>
      </div>
    </c:when>
    <c:otherwise>

      <%-- Filter bar --%>
      <div class="pet-filter-bar">
        <div class="pet-search-wrap">
          <span class="pet-search-icon">🔍</span>
          <input class="pet-search-input" id="petSearch"
                 placeholder="Tìm theo tên, loài, giống..." autocomplete="off">
        </div>
        <select class="pet-filter-select" id="petFilterSpecies">
          <option value="">Tất cả loài</option>
          <c:forEach var="p" items="${pets}">
            <option value="${p.speciesName}">${p.speciesName}</option>
          </c:forEach>
        </select>
        <select class="pet-filter-select" id="petSort">
          <option value="name-az">Tên A–Z</option>
          <option value="name-za">Tên Z–A</option>
          <option value="visits-desc">Nhiều lịch nhất</option>
          <option value="recent">Khám gần nhất</option>
        </select>
        <span class="pet-filter-count" id="petCount"></span>
      </div>

      <%-- Pet list --%>
      <div class="pet-list" id="petList">
        <c:forEach var="p" items="${pets}">
          <a href="${ctx}/pets/profile?id=${p.petID}" class="pet-row"
             data-name="${fn:toLowerCase(p.name)}"
             data-species="${fn:toLowerCase(p.speciesName)}"
             data-breed="${fn:toLowerCase(p.breedName)}"
             data-visits="${p.totalAppointments}"
             data-lastvisit="${not empty p.lastVisitDate ? p.lastVisitDate : '0000-00-00'}">

            <div class="pet-row-avatar">${p.speciesEmoji}</div>

            <div class="pet-row-main">
              <div class="pet-row-name">${p.name}</div>
              <div class="pet-row-sub">
                <span>${p.speciesName}</span>
                <span>${p.breedName}</span>
                <span>${p.genderDisplay}</span>
                <span>${p.ageDisplay}</span>
                <c:if test="${p.weight != null}"><span>${p.weight} kg</span></c:if>
              </div>
            </div>

            <div class="pet-row-stats">
              <div class="pet-row-stat">
                <span class="pet-row-stat-num">${p.doneAppointments}</span>
                <span class="pet-row-stat-lbl">Đã khám</span>
              </div>
              <div class="pet-row-stat">
                <span class="pet-row-stat-num" style="font-size:14px;">
                  <c:choose>
                    <c:when test="${not empty p.lastVisitDate}">
                      ${fn:substring(p.lastVisitDate,8,10)}/${fn:substring(p.lastVisitDate,5,7)}
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                  </c:choose>
                </span>
                <span class="pet-row-stat-lbl">Lần cuối</span>
              </div>
            </div>

            <div class="pet-row-arrow">›</div>
          </a>
        </c:forEach>
      </div>
      <div class="pet-no-results" id="petNoResults">Không tìm thấy thú cưng phù hợp.</div>

    </c:otherwise>
  </c:choose>

</div>

<%-- FAB: add pet --%>
<c:if test="${not empty pets}">
  <a href="${ctx}/pets/new" class="fab-add" title="Thêm thú cưng mới">+</a>
</c:if>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script>
(function () {
  const searchEl  = document.getElementById('petSearch');
  const speciesEl = document.getElementById('petFilterSpecies');
  const sortEl    = document.getElementById('petSort');
  const countEl   = document.getElementById('petCount');
  const noResEl   = document.getElementById('petNoResults');
  const listEl    = document.getElementById('petList');
  if (!listEl) return;

  const allRows = Array.from(listEl.querySelectorAll('.pet-row'));

  // Deduplicate species options
  const seen = new Set();
  speciesEl.querySelectorAll('option').forEach(opt => {
    if (!opt.value) return;
    if (seen.has(opt.value)) opt.remove();
    else seen.add(opt.value);
  });

  function apply() {
    const q       = (searchEl.value || '').trim().toLowerCase();
    const species = (speciesEl.value || '').toLowerCase();
    const sort    = sortEl.value || 'name-az';

    let visible = allRows.filter(row => {
      if (q) {
        const hay = [row.dataset.name, row.dataset.species, row.dataset.breed].join(' ');
        if (!hay.includes(q)) return false;
      }
      if (species && row.dataset.species !== species) return false;
      return true;
    });

    visible.sort((a, b) => {
      switch (sort) {
        case 'name-az':     return a.dataset.name.localeCompare(b.dataset.name, 'vi');
        case 'name-za':     return b.dataset.name.localeCompare(a.dataset.name, 'vi');
        case 'visits-desc': return parseInt(b.dataset.visits||0) - parseInt(a.dataset.visits||0);
        case 'recent':      return (b.dataset.lastvisit||'').localeCompare(a.dataset.lastvisit||'');
        default: return 0;
      }
    });

    allRows.forEach(r => r.classList.add('hidden'));
    visible.forEach(r => { r.classList.remove('hidden'); listEl.appendChild(r); });

    countEl.textContent = visible.length + ' / ' + allRows.length + ' thú cưng';
    noResEl.style.display = visible.length === 0 ? 'block' : 'none';
  }

  searchEl?.addEventListener('input',  apply);
  speciesEl?.addEventListener('change', apply);
  sortEl?.addEventListener('change',   apply);
  apply(); // initial render
})();
</script>
</body>
</html>
