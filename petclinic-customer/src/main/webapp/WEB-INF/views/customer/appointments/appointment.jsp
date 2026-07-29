<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Lịch khám – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/appointments.css">
  <link rel="stylesheet" href="${ctx}/css/customer-dashboard.css">
  <style>
    .filter-bar {
      display:flex; flex-wrap:wrap; gap:10px; align-items:center;
      padding:14px 0 18px; border-bottom:1px solid var(--border); margin-bottom:18px;
    }
    .filter-search-wrap {
      display:flex; flex:1; min-width:220px; max-width:340px;
      border:1.5px solid var(--border); border-radius:10px; background:#fff; overflow:hidden;
    }
    .filter-search-select {
      border:none; outline:none; font-family:'DM Sans',sans-serif; font-size:13px;
      background:var(--sand); padding:0 10px; color:var(--text-mid); cursor:pointer;
      border-right:1px solid var(--border); flex-shrink:0;
    }
    .filter-search-input {
      border:none; outline:none; font-family:'DM Sans',sans-serif; font-size:13.5px;
      padding:9px 12px; flex:1; color:var(--text-dark); background:#fff;
    }
    .filter-search-input::placeholder { color:var(--warm-gray); }
    .filter-select {
      border:1.5px solid var(--border); border-radius:10px; padding:8px 12px;
      font-family:'DM Sans',sans-serif; font-size:13px; color:var(--text-mid);
      background:#fff; cursor:pointer; outline:none;
    }
    .filter-select:focus { border-color:var(--green-500); }
    .filter-count { font-size:12.5px; color:var(--warm-gray); margin-left:auto; white-space:nowrap; }
    .filter-reset { font-size:12.5px; color:var(--green-500); cursor:pointer; font-weight:500; background:none; border:none; padding:0; display:none; }
    .filter-reset.visible { display:inline; }
    .no-results-msg { text-align:center; padding:40px 0; color:var(--warm-gray); font-size:14px; display:none; }
    .appt-card.hidden { display:none !important; }
  </style>
</head>
<body>

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="appt-wrap">

  <div class="appt-page-header">
    <div>
      <h1>Lịch khám của bạn</h1>
      <p>Xem và quản lý tất cả các lịch hẹn</p>
    </div>
    <a href="${ctx}/booking/new" class="btn-new-booking">➕ Đặt lịch mới</a>
  </div>

  <div class="appt-toolbar">
    <div class="view-toggle">
      <a href="${ctx}/appointments?view=list${not empty petFilter ? '&petId='.concat(petFilter) : ''}"
         class="view-toggle-btn ${view == 'list' ? 'active' : ''}">📋 Danh sách</a>
      <a href="${ctx}/appointments?view=calendar${not empty petFilter ? '&petId='.concat(petFilter) : ''}"
         class="view-toggle-btn ${view == 'calendar' ? 'active' : ''}">📅 Lịch</a>
    </div>

    <form method="get" action="${ctx}/appointments" class="pet-filter-form">
      <input type="hidden" name="view" value="${view}">
      <select name="petId" onchange="this.form.submit()">
        <option value="">Tất cả thú cưng</option>
        <c:forEach var="p" items="${pets}">
          <option value="${p.petID}" ${petFilter == p.petID ? 'selected' : ''}><c:out value="${p.name}"/></option>
        </c:forEach>
      </select>
    </form>
  </div>

  <c:choose>

    <%-- ══════════════ CALENDAR VIEW ══════════════ --%>
    <c:when test="${view == 'calendar'}">
      <div id="apptCalendarRoot"></div>
      <script>
        window.APP_CTX = '${ctx}';
        window.APPT_CALENDAR_DATA = ${not empty calendarJson ? calendarJson : '[]'};
      </script>
      <script src="${ctx}/js/appointment-calendar.js"></script>
    </c:when>

    <%-- ══════════════ LIST VIEW (mô hình cũ + phân trang) ══════════════ --%>
    <c:otherwise>

      <div class="appt-tabs">
        <button class="appt-tab active" onclick="switchTab('upcoming', this)">
          Sắp tới
          <c:if test="${upcomingCount > 0}"><span class="tab-badge" id="badge-upcoming">${upcomingCount}</span></c:if>
        </button>
        <button class="appt-tab" onclick="switchTab('history', this)">
          Lịch sử
          <c:if test="${historyCount > 0}"><span class="tab-badge tab-badge--gray" id="badge-history">${historyCount}</span></c:if>
        </button>
      </div>

      <%-- ══════════════ TAB: SẮP TỚI ══════════════ --%>
      <div id="tab-upcoming" class="appt-tab-panel active">
        <c:choose>
          <c:when test="${empty upcoming}">
            <div class="appt-empty">
              <div class="appt-empty-icon">📭</div>
              <h3>Chưa có lịch hẹn nào sắp tới</h3>
              <p>Hãy đặt lịch khám để chăm sóc thú cưng của bạn</p>
              <a href="${ctx}/booking/new" class="btn-new-booking" style="display:inline-flex;margin-top:16px;">Đặt lịch ngay</a>
            </div>
          </c:when>
          <c:otherwise>
            <div class="filter-bar" id="fb-upcoming">
              <div class="filter-search-wrap">
                <select class="filter-search-select" id="upcoming-search-field">
                  <option value="all">Tất cả</option>
                  <option value="pet">Thú cưng</option>
                  <option value="service">Dịch vụ</option>
                  <option value="staff">Bác sĩ</option>
                </select>
                <input class="filter-search-input" id="upcoming-search"
                       placeholder="Tìm kiếm trong trang này..." autocomplete="off">
              </div>
              <select class="filter-select" id="upcoming-status">
                <option value="">Tất cả trạng thái</option>
                <option value="Pending">Chờ xác nhận</option>
                <option value="Confirmed">Đã xác nhận</option>
                <option value="InProgress">Đang khám</option>
              </select>
              <select class="filter-select" id="upcoming-sort">
                <option value="date-asc">Ngày: Gần nhất</option>
                <option value="date-desc">Ngày: Xa nhất</option>
                <option value="service-az">Dịch vụ: A–Z</option>
                <option value="service-za">Dịch vụ: Z–A</option>
              </select>
              <span class="filter-count" id="upcoming-count"></span>
              <button class="filter-reset" id="upcoming-reset" onclick="resetFilters('upcoming')">Xoá bộ lọc</button>
            </div>

            <div class="appt-list" id="list-upcoming">
              <c:forEach var="a" items="${upcoming}">
                <a href="${ctx}/appointments/detail?id=${a.appointmentID}"
                   class="appt-card appt-card--upcoming"
                   data-pet="${fn:escapeXml(a.petName)}"
                   data-service="${fn:escapeXml(a.serviceName)}"
                   data-category="${fn:escapeXml(a.categoryName)}"
                   data-staff="${fn:escapeXml(a.staffName)}"
                   data-status="${a.status}"
                   data-date="${a.appointmentDate}">
                  <div class="appt-card-date">
                    <span class="appt-day">${a.appointmentDate.dayOfMonth}</span>
                    <span class="appt-month">${a.monthDisplayVi}</span>
                  </div>
                  <div class="appt-card-body">
                    <div class="appt-card-title">${a.serviceName}</div>
                    <div class="appt-card-meta">
                      <span>${a.petName}</span>
                      <span>${a.formattedStartTime} – ${a.formattedEndTime}</span>
                      <c:if test="${not empty a.staffName}"><span>${a.staffName}</span></c:if>
                    </div>
                  </div>
                  <div class="appt-card-right">
                    <span class="status-badge status-${fn:toLowerCase(a.status)}">${a.status}</span>
                    <c:if test="${a.canModify()}"><span class="appt-modifiable">Có thể chỉnh sửa</span></c:if>
                    <span class="appt-arrow">›</span>
                  </div>
                </a>
              </c:forEach>
            </div>
            <div class="no-results-msg" id="upcoming-noresult">Không tìm thấy lịch hẹn phù hợp.</div>

            <c:if test="${upTotalPages > 1}">
              <div class="db-pagination">
                <c:forEach var="i" begin="1" end="${upTotalPages}">
                  <a class="db-page-link ${i == upPage ? 'active' : ''}"
                     href="${ctx}/appointments?view=list&upPage=${i}&histPage=${histPage}${not empty petFilter ? '&petId='.concat(petFilter) : ''}">${i}</a>
                </c:forEach>
              </div>
            </c:if>
          </c:otherwise>
        </c:choose>
      </div>

      <%-- ══════════════ TAB: LỊCH SỬ ══════════════ --%>
      <div id="tab-history" class="appt-tab-panel">
        <c:choose>
          <c:when test="${empty history}">
            <div class="appt-empty">
              <div class="appt-empty-icon">🗂</div>
              <h3>Chưa có lịch sử khám</h3>
              <p>Các lịch hẹn đã hoàn thành hoặc huỷ sẽ hiển thị ở đây</p>
            </div>
          </c:when>
          <c:otherwise>
            <div class="filter-bar" id="fb-history">
              <div class="filter-search-wrap">
                <select class="filter-search-select" id="history-search-field">
                  <option value="all">Tất cả</option>
                  <option value="pet">Thú cưng</option>
                  <option value="service">Dịch vụ</option>
                  <option value="category">Loại dịch vụ</option>
                  <option value="staff">Bác sĩ</option>
                </select>
                <input class="filter-search-input" id="history-search"
                       placeholder="Tìm kiếm trong trang này..." autocomplete="off">
              </div>
              <select class="filter-select" id="history-status">
                <option value="">Tất cả trạng thái</option>
                <option value="Done">Đã hoàn thành</option>
                <option value="Cancelled">Đã huỷ</option>
                <option value="NoShow">Vắng mặt</option>
              </select>
              <select class="filter-select" id="history-sort">
                <option value="date-desc">Ngày: Mới nhất</option>
                <option value="date-asc">Ngày: Cũ nhất</option>
                <option value="service-az">Dịch vụ: A–Z</option>
                <option value="service-za">Dịch vụ: Z–A</option>
              </select>
              <span class="filter-count" id="history-count"></span>
              <button class="filter-reset" id="history-reset" onclick="resetFilters('history')">Xoá bộ lọc</button>
            </div>

            <div class="appt-list" id="list-history">
              <c:forEach var="a" items="${history}">
                <a href="${ctx}/appointments/detail?id=${a.appointmentID}"
                   class="appt-card"
                   data-pet="${fn:escapeXml(a.petName)}"
                   data-service="${fn:escapeXml(a.serviceName)}"
                   data-category="${fn:escapeXml(a.categoryName)}"
                   data-staff="${fn:escapeXml(a.staffName)}"
                   data-status="${a.status}"
                   data-date="${a.appointmentDate}">
                  <div class="appt-card-date appt-card-date--muted">
                    <span class="appt-day">${a.appointmentDate.dayOfMonth}</span>
                    <span class="appt-month">${a.monthDisplayVi}</span>
                  </div>
                  <div class="appt-card-body">
                    <div class="appt-card-title">${a.serviceName}</div>
                    <div class="appt-card-meta">
                      <span>${a.petName}</span>
                      <span>${a.formattedStartTime} – ${a.formattedEndTime}</span>
                      <c:if test="${not empty a.staffName}"><span>${a.staffName}</span></c:if>
                    </div>
                    <c:if test="${not empty a.notes}"><div class="appt-notes-preview">${a.notes}</div></c:if>
                  </div>
                  <div class="appt-card-right">
                    <span class="status-badge status-${fn:toLowerCase(a.status)}">${a.status}</span>
                    <span class="appt-arrow">›</span>
                  </div>
                </a>
              </c:forEach>
            </div>
            <div class="no-results-msg" id="history-noresult">Không tìm thấy lịch hẹn phù hợp.</div>

            <c:if test="${histTotalPages > 1}">
              <div class="db-pagination">
                <c:forEach var="i" begin="1" end="${histTotalPages}">
                  <a class="db-page-link ${i == histPage ? 'active' : ''}"
                     href="${ctx}/appointments?view=list&upPage=${upPage}&histPage=${i}${not empty petFilter ? '&petId='.concat(petFilter) : ''}">${i}</a>
                </c:forEach>
              </div>
            </c:if>
          </c:otherwise>
        </c:choose>
      </div>

    </c:otherwise>
  </c:choose>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
<script src="${ctx}/js/appointment.js"></script>
</body>
</html>