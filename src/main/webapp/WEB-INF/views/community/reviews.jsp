<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Đánh giá từ cộng đồng – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/reviews.css">
</head>
<body>

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="community-wrap">

  <%-- ── Hero ──────────────────────────────────────────────────── --%>
  <div class="community-hero">
    <div class="community-hero-left">
      <h1>Đánh giá từ cộng đồng</h1>
      <p>Những chia sẻ thực tế từ các khách hàng đã sử dụng dịch vụ tại PetClinic</p>
    </div>
    <div class="community-hero-stats">
      <div class="community-stat">
        <span class="community-stat-num">${totalReviews}</span>
        <span class="community-stat-lbl">Đánh giá</span>
      </div>
      <div class="community-stat">
        <span class="community-stat-num">${avgRating}</span>
        <span class="community-stat-lbl">Điểm trung bình</span>
      </div>
    </div>
  </div>

  <%-- ── Filter form ─────────────────────────────────────────────── --%>
  <form class="community-filters" method="get" action="${ctx}/community" id="filterForm">

  <div class="filters-top-row">
    <%-- Category --%>
    <div class="community-filter-group">
      <label>Loại dịch vụ</label>
      <select name="categoryId" class="community-filter-select" id="filterCategory"
              onchange="onCategoryChange(this.value); this.form.submit();">
        <option value="0">Tất cả</option>
        <c:forEach var="cat" items="${categories}">
          <option value="${cat.categoryID}"
            ${f_categoryId == cat.categoryID ? 'selected' : ''}>${cat.name}</option>
        </c:forEach>
      </select>
    </div>

    <%-- Service (filtered by category via JS) --%>
    <div class="community-filter-group">
      <label>Dịch vụ cụ thể</label>
      <select name="serviceId" class="community-filter-select" id="filterService" onchange="this.form.submit();">
        <option value="0">Tất cả</option>
        <c:forEach var="cat" items="${categories}">
          <c:forEach var="svc" items="${cat.services}">
            <option value="${svc.serviceID}"
                    data-cat="${cat.categoryID}"
                    ${f_serviceId == svc.serviceID ? 'selected' : ''}>
              ${svc.name}
            </option>
          </c:forEach>
        </c:forEach>
      </select>
    </div>

    <%-- Vet / Groomer --%>
    <div class="community-filter-group">
      <label>Bác sĩ / Thợ grooming</label>
      <select name="staffId" class="community-filter-select" onchange="this.form.submit();">
        <option value="0">Tất cả</option>
        <%-- Build unique staff list from reviews --%>
        <c:forEach var="rv" items="${reviews}">
          <c:if test="${not empty rv.staffName}">
            <option value="${rv.staffID}"
              ${f_staffId == rv.staffID ? 'selected' : ''}>${rv.staffName}</option>
          </c:if>
        </c:forEach>
      </select>
    </div>

    <%-- Pet species --%>
    <div class="community-filter-group">
      <label>Loài thú cưng</label>
      <select name="petSpecies" class="community-filter-select" onchange="this.form.submit();">
        <option value="">Tất cả</option>
        <c:forEach var="sp" items="${allSpecies}">
          <option value="${sp}" ${f_petSpecies eq sp ? 'selected' : ''}>${sp}</option>
        </c:forEach>
      </select>
    </div>

    <%-- Sort --%>
    <div class="community-filter-group">
      <label>Sắp xếp</label>
      <select name="sort" class="community-filter-select" onchange="this.form.submit();">
        <option value="newest"      ${f_sort eq 'newest'      ? 'selected' : ''}>Mới nhất</option>
        <option value="rating_desc" ${f_sort eq 'rating_desc' ? 'selected' : ''}>Điểm cao nhất</option>
        <option value="rating_asc"  ${f_sort eq 'rating_asc'  ? 'selected' : ''}>Điểm thấp nhất</option>
      </select>
    </div>
  </div>
  <div class="filters-bottom-row">
    <%-- Star filter --%>
    <div class="community-filter-group">
      <label>Số sao tối thiểu</label>
      <div class="star-filter-row">
        <c:forEach var="n" items="${[0,1,2,3,4,5]}">
          <button type="button" class="star-filter-btn ${f_minRating == n ? 'active' : ''}"
                  onclick="setMinRating(${n}, this)">
            <c:choose>
              <c:when test="${n == 0}">Tất cả</c:when>
              <c:otherwise>${n}★</c:otherwise>
            </c:choose>
          </button>
        </c:forEach>
        <input type="hidden" name="minRating" id="minRatingInput" value="${f_minRating}">
      </div>
    </div>
    <div>
      <a href="${ctx}/community" class="btn-filter-reset">Xoá lọc</a>
    </div>
  </div>
  </form>

  <%-- ── Result count --%>
  <div class="result-count">
    Hiển thị <strong>${totalReviews}</strong> đánh giá
    <c:if test="${f_categoryId > 0 or f_serviceId > 0 or f_staffId > 0
               or not empty f_petSpecies or f_minRating > 0}">
      (đã lọc)
    </c:if>
    · Điểm TB: <strong>${avgRating} ⭐</strong>
  </div>

  <%-- ── Review grid --%>
  <div class="review-grid">
    <c:choose>
      <c:when test="${empty reviews}">
        <div class="review-empty">
          <div class="review-empty-icon"></div>
          <p>Chưa có đánh giá nào phù hợp với bộ lọc hiện tại.</p>
        </div>
      </c:when>
      <c:otherwise>
        <c:forEach var="rv" items="${reviews}">
          <div class="review-card">

            <%-- Stars + date --%>
            <div class="review-card-header">
              <div class="review-card-stars">${rv.starsDisplay}</div>
              <div class="review-card-date">${rv.formattedCreatedAt}</div>
            </div>

            <%-- Service name --%>
            <div class="review-card-service">${rv.serviceName}</div>

            <%-- Tags: category, staff, pet species --%>
            <div class="review-card-meta">
              <c:if test="${not empty rv.categoryName}">
                <span class="review-meta-tag">${rv.categoryName}</span>
              </c:if>
              <c:if test="${not empty rv.staffName}">
                <span class="review-meta-tag staff">${rv.staffName}</span>
              </c:if>
              <c:if test="${not empty rv.petSpecies}">
                <span class="review-meta-tag species">${rv.petSpecies}</span>
              </c:if>
            </div>

            <%-- Comment --%>
            <div class="review-card-comment">
              <c:choose>
                <c:when test="${not empty rv.comment}">${rv.comment}</c:when>
                <c:otherwise><span style="font-style:normal;color:var(--warm-gray);">Không có nhận xét.</span></c:otherwise>
              </c:choose>
            </div>

            <%-- Anonymous author --%>
            <div class="review-card-author">
              — ${rv.anonymousLabel}
            </div>

          </div>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </div>

</div><%-- /community-wrap --%>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script>
// ── Category → Service cascade filter ────────────────────────────────────
const ALL_SERVICES = (() => {
  const opts = document.querySelectorAll('#filterService option[data-cat]');
  return Array.from(opts).map(o => ({
    val: o.value, cat: o.dataset.cat, text: o.textContent.trim()
  }));
})();

function onCategoryChange(catId) {
  const sel = document.getElementById('filterService');
  const current = sel.value;
  sel.innerHTML = '<option value="0">Tất cả</option>';
  ALL_SERVICES
    .filter(s => catId === '0' || s.cat === catId)
    .forEach(s => {
      const opt = document.createElement('option');
      opt.value = s.val;
      opt.textContent = s.text;
      if (s.val === current) opt.selected = true;
      sel.appendChild(opt);
    });
}

// Run on load to apply existing category filter
onCategoryChange(document.getElementById('filterCategory').value);

// ── Min-rating pill buttons ───────────────────────────────────────────────
function setMinRating(val, btn) {
  document.getElementById('minRatingInput').value = val;
  document.querySelectorAll('.star-filter-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');

  // Tự động submit form khi chọn sao
  document.getElementById('filterForm').submit();
}

// ── Deduplicate staff options (server may send dupes when no filter) ────────
(function dedupeStaffs() {
  const sel  = document.querySelector('select[name="staffId"]');
  const seen = new Set();
  Array.from(sel.options).forEach(opt => {
    if (!opt.value || opt.value === '0') { seen.add('0'); return; }
    if (seen.has(opt.value)) opt.remove();
    else seen.add(opt.value);
  });
})();
</script>
</body>
</html>