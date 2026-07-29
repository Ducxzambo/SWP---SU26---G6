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
      Cột mốc &amp; Thống kê
      <span style="margin-left:auto;font-size:12px;color:var(--warm-gray);font-weight:400;">
      ${cancelledCount} huỷ · ${noShowCount} vắng mặt
    </span>
    </div>
    <div class="profile-section-body" style="padding:18px 20px;">
      <div class="milestone-list">

        <c:if test="${not empty firstDone}">
          <div class="milestone-item">
            <div class="milestone-icon">🎉</div>
            <div class="milestone-body">
              <div class="milestone-title">Lần khám đầu tiên</div>
              <div class="milestone-sub">${firstDone.serviceName} · ${firstDone.formattedAppointmentDate}</div>
            </div>
            <a href="${ctx}/appointments/detail?id=${firstDone.appointmentID}" class="milestone-link">Xem →</a>
          </div>
        </c:if>

        <c:if test="${not empty lastDone}">
          <div class="milestone-item">
            <div class="milestone-icon">🩺</div>
            <div class="milestone-body">
              <div class="milestone-title">Lần khám gần nhất</div>
              <div class="milestone-sub">${lastDone.serviceName} · ${lastDone.formattedAppointmentDate}</div>
            </div>
            <a href="${ctx}/appointments/detail?id=${lastDone.appointmentID}" class="milestone-link">Xem →</a>
          </div>
        </c:if>

        <c:if test="${not empty latestVaccine}">
          <div class="milestone-item">
            <div class="milestone-icon">💉</div>
            <div class="milestone-body">
              <div class="milestone-title">Mũi tiêm gần nhất</div>
              <div class="milestone-sub">${latestVaccine.vaccineName} · ${latestVaccine.formattedAdministeredDate}</div>
            </div>
          </div>
        </c:if>

        <c:if test="${not empty nextDueVaccine}">
          <div class="milestone-item">
            <div class="milestone-icon">⏰</div>
            <div class="milestone-body">
              <div class="milestone-title">Vaccine sắp đến hạn</div>
              <div class="milestone-sub">${nextDueVaccine.vaccineName} · nhắc lại ${nextDueVaccine.formattedNextDueDate}</div>
            </div>
          </div>
        </c:if>

        <c:if test="${not empty nextUpcoming}">
          <div class="milestone-item">
            <div class="milestone-icon">📅</div>
            <div class="milestone-body">
              <div class="milestone-title">Lịch hẹn sắp tới</div>
              <div class="milestone-sub">${nextUpcoming.serviceName} · ${nextUpcoming.formattedAppointmentDate}</div>
            </div>
            <a href="${ctx}/appointments/detail?id=${nextUpcoming.appointmentID}" class="milestone-link">Xem →</a>
          </div>
        </c:if>

        <c:if test="${empty firstDone and empty lastDone and empty latestVaccine and empty nextDueVaccine and empty nextUpcoming}">
          <div class="db-empty-mini" style="color:var(--warm-gray);text-align:center;padding:24px 0;font-size:13px;">
            Chưa có cột mốc nào được ghi nhận.
          </div>
        </c:if>

      </div>
    </div>
  </div>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>
