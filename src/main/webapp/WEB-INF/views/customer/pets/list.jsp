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
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="pets-list-wrap">

  <div class="pets-page-header">
    <div>
      <h1>Thú cưng của tôi</h1>
      <p>Quản lý hồ sơ và lịch sử khám của tất cả thú cưng</p>
    </div>
    </a>
  </div>

  <c:choose>
    <c:when test="${empty pets}">
      <div class="pets-empty">
        <div class="pets-empty-icon">🐾</div>
        <h2>Chưa có thú cưng nào</h2>
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
