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
  <title>${pet.name} – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/pets.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="profile-wrap">

  <a href="${ctx}/pets" class="detail-back">← Danh sách thú cưng</a>

  <%-- ── Hero ─────────────────────────────────────────────────── --%>
  <div class="profile-hero">
    <div class="profile-avatar">${pet.speciesEmoji}</div>
    <div class="profile-hero-info">
      <div class="profile-pet-name">${pet.name}</div>
      <div class="profile-meta-tags">
        <span class="profile-tag">${pet.speciesName}</span>
        <span class="profile-tag">${pet.breedName}</span>
        <span class="profile-tag">${pet.genderDisplay}</span>
        <span class="profile-tag">${pet.ageDisplay}</span>
        <c:if test="${pet.weight != null}">
          <span class="profile-tag">${pet.weight} kg</span>
        </c:if>
        <c:if test="${not empty pet.formattedDateOfBirth}">
          <span class="profile-tag">Sinh: ${pet.formattedDateOfBirth}</span>
        </c:if>
      </div>
    </div>
    <div class="profile-hero-actions">
      <a href="${ctx}/pets/edit?id=${pet.petID}" class="btn-edit-pet">Sửa</a>
      <a href="${ctx}/booking/new" class="btn-edit-pet"
         style="background:var(--green-400);border-color:var(--green-400);color:var(--green-900);">
        Đặt lịch
      </a>
    </div>
  </div>

  <%-- ── Stats ───────────────────────────────────────────────── --%>
  <div class="profile-stats">
    <div class="profile-stat-card">
      <div class="profile-stat-num">${pet.totalAppointments}</div>
      <div class="profile-stat-lbl">Tổng lịch khám</div>
    </div>
    <div class="profile-stat-card">
      <div class="profile-stat-num">${pet.doneAppointments}</div>
      <div class="profile-stat-lbl">Đã hoàn thành</div>
    </div>
    <div class="profile-stat-card">
      <div class="profile-stat-num">${vaccineCount}</div>
      <div class="profile-stat-lbl">Vaccine đã tiêm</div>
    </div>
    <div class="profile-stat-card">
      <div class="profile-stat-num" style="font-size:16px;">
        <c:choose>
          <c:when test="${not empty pet.lastVisitDate}">
            ${fn:substring(pet.lastVisitDate,8,10)}/${fn:substring(pet.lastVisitDate,5,7)}/${fn:substring(pet.lastVisitDate,0,4)}
          </c:when>
          <c:otherwise>—</c:otherwise>
        </c:choose>
      </div>
      <div class="profile-stat-lbl">Lần khám gần nhất</div>
    </div>
  </div>

  <%-- ── Cột mốc & thống kê chung (thay cho liệt kê toàn bộ lịch sử) ─────── --%>
  <div class="profile-section">
    <div class="profile-section-head">
      Cột mốc &amp; Dòng thời gian
      <span style="margin-left:auto;font-size:12px;color:var(--warm-gray);font-weight:400;">
      ${cancelledCount} huỷ · ${noShowCount} vắng mặt
    </span>
    </div>
    <div class="profile-section-body" style="padding:24px 22px;">

      <c:choose>
        <c:when test="${empty timeline}">
          <div class="db-empty-mini" style="color:var(--warm-gray);text-align:center;font-size:13px;padding:10px 0 20px;">
            Chưa có dữ liệu để hiển thị trên dòng thời gian.
          </div>
        </c:when>
        <c:otherwise>
          <div class="tl-legend">
            <span><span class="tl-legend-dot tl-dot-green"></span>Đã hoàn thành</span>
            <span><span class="tl-legend-dot tl-dot-amber"></span>Đã xác nhận</span>
            <span><span class="tl-legend-dot tl-dot-black"></span>Vaccine / Tái khám</span>
          </div>
          <div class="tl-wrap">
            <div class="tl-line"></div>
            <div class="tl-dots">
              <c:forEach var="ev" items="${timeline}" varStatus="vs">
                <c:set var="tlPos" value="${fn:length(timeline) == 1 ? 50 : (vs.index * 100 / (fn:length(timeline) - 1))}"/>
                <c:choose>
                  <c:when test="${not empty ev.appointmentId}">
                    <a href="${ctx}/appointments/detail?id=${ev.appointmentId}"
                       class="tl-dot ${ev.dotClass}" style="left:${tlPos}%;"
                       title="${ev.typeLabel} — ${ev.label} (${ev.formattedDate})"></a>
                  </c:when>
                  <c:otherwise>
                  <span class="tl-dot ${ev.dotClass}" style="left:${tlPos}%;"
                        title="${ev.typeLabel} — ${ev.label} (${ev.formattedDate})"></span>
                  </c:otherwise>
                </c:choose>
              </c:forEach>
            </div>
          </div>
          <div class="tl-hint">Di chuột vào từng chấm để xem chi tiết.</div>
        </c:otherwise>
      </c:choose>

      <div class="milestone-list" style="margin-top:8px;">
        <%-- ...keep the existing firstDone / lastDone / latestVaccine / nextDueVaccine / nextUpcoming milestone-item blocks exactly as before... --%>
      </div>

    </div>
  </div>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>
