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
      <a href="${ctx}/pets/edit?id=${pet.petID}" class="btn-edit-pet">✏ Sửa</a>
      <a href="${ctx}/booking/new?prefillPet=${pet.petID}" class="btn-edit-pet"
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
      <div class="profile-stat-num">${vaccines.size()}</div>
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

  <%-- ── Vaccine passport ────────────────────────────────────── --%>
  <div class="profile-section">
    <div class="profile-section-head">
      Vaccine passport
      <c:set var="overdueCount"  value="0"/>
      <c:set var="dueSoonCount"  value="0"/>
      <c:forEach var="v" items="${vaccines}">
        <c:if test="${v.overdue}">  <c:set var="overdueCount"  value="${overdueCount  + 1}"/></c:if>
        <c:if test="${v.dueSoon}">  <c:set var="dueSoonCount"  value="${dueSoonCount  + 1}"/></c:if>
      </c:forEach>
      <c:if test="${overdueCount > 0}">
        <span style="margin-left:auto;background:#f8d7da;color:#721c24;
              border-radius:20px;padding:3px 10px;font-size:12px;font-weight:600;">
          ${overdueCount} quá hạn
        </span>
      </c:if>
      <c:if test="${overdueCount == 0 and dueSoonCount > 0}">
        <span style="margin-left:auto;background:#fff3cd;color:#856404;
              border-radius:20px;padding:3px 10px;font-size:12px;font-weight:600;">
          ${dueSoonCount} sắp đến hạn
        </span>
      </c:if>
    </div>
    <div class="profile-section-body">
      <c:choose>
        <c:when test="${empty vaccines}">
          <div style="padding:28px;text-align:center;color:var(--warm-gray);font-size:14px;">
            Chưa có thông tin vaccine.
          </div>
        </c:when>
        <c:otherwise>
          <table class="vaccine-table">
            <thead>
              <tr>
                <th>Vaccine</th>
                <th>Ngày tiêm</th>
                <th>Bác sĩ</th>
                <th>Ngày nhắc tiếp</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="v" items="${vaccines}">
                <tr>
                  <td style="font-weight:600;color:var(--text-dark);">${v.vaccineName}</td>
                  <td>${v.formattedAdministeredDate}</td>
                  <td>${not empty v.staffName ? v.staffName : '—'}</td>
                  <td>${v.formattedNextDueDate}</td>
                  <td>
                    <c:choose>
                      <c:when test="${v.overdue}">
                        <span class="vaccine-overdue">Quá hạn</span>
                      </c:when>
                      <c:when test="${v.dueSoon}">
                        <span class="vaccine-due-soon">Sắp đến hạn</span>
                      </c:when>
                      <c:otherwise>
                        <span class="vaccine-ok">✓ Đúng hạn</span>
                      </c:otherwise>
                    </c:choose>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </c:otherwise>
      </c:choose>
    </div>
  </div>

  <%-- ── Medical history timeline ────────────────────────────── --%>
  <div class="profile-section">
    <div class="profile-section-head">
      Lịch sử khám bệnh
      <span style="margin-left:auto;font-size:12.5px;color:var(--warm-gray);font-weight:400;">
        ${appointments.size()} lần
      </span>
    </div>
    <div class="profile-section-body">
      <c:choose>
        <c:when test="${empty appointments}">
          <div style="padding:28px;text-align:center;color:var(--warm-gray);font-size:14px;">
            Chưa có lịch sử khám bệnh.
            <a href="${ctx}/booking/new?prefillPet=${pet.petID}"
               style="color:var(--green-500);font-weight:500;margin-left:4px;">Đặt lịch ngay →</a>
          </div>
        </c:when>
        <c:otherwise>
          <div class="timeline">
            <c:forEach var="a" items="${appointments}" varStatus="vs">
              <div class="timeline-item">
                <%-- Dot + line --%>
                <div class="timeline-dot-wrap">
                  <div class="timeline-dot ${fn:toLowerCase(a.status)}"></div>
                  <c:if test="${!vs.last}"><div class="timeline-line"></div></c:if>
                </div>
                <%-- Content --%>
                <div class="timeline-content">
                  <div class="timeline-service">${a.serviceName}</div>
                  <div class="timeline-meta">
                    <span>${a.formattedAppointmentDate}</span>
                    <span>${a.formattedStartTime} – ${a.formattedEndTime}</span>
                    <c:if test="${not empty a.staffName}">
                      <span>${a.staffName}</span>
                    </c:if>
                    <span class="status-badge status-${fn:toLowerCase(a.status)}"
                          style="padding:2px 8px;font-size:11px;">${a.status}</span>
                  </div>
                  <%-- Medical summary if exists --%>
                  <c:if test="${not empty a.diagnosis}">
                    <div class="timeline-summary">
                      <strong>Chẩn đoán:</strong> ${a.diagnosis}
                    </div>
                  </c:if>
                  <c:if test="${not empty a.notes and empty a.diagnosis}">
                    <div class="timeline-summary">${a.notes}</div>
                  </c:if>
                  <a href="${ctx}/appointments/detail?id=${a.appointmentID}"
                     class="timeline-link" style="margin-top:6px;display:inline-block;">
                    Xem chi tiết →
                  </a>
                </div>
              </div>
            </c:forEach>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </div>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>
