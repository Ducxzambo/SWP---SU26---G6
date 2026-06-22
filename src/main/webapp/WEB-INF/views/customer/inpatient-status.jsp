<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>My Pet's Hospital Stay</title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body class="bg-light">
<div class="container mt-4" style="max-width:700px">

    <h4 class="mb-1">🐾 Inpatient Status</h4>
    <p class="text-muted mb-4">Track your pet's daily condition during hospital stay.</p>

    <c:choose>
        <c:when test="${empty admissions}">
            <div class="alert alert-info">
                None of your pets are currently admitted.
            </div>
        </c:when>
        <c:otherwise>
            <c:forEach items="${admissions}" var="a">
                <div class="card shadow-sm mb-3
                    ${a.status eq 'Critical' ? 'border-danger border-2' : ''}">
                    <div class="card-header d-flex justify-content-between align-items-center
                        ${a.status eq 'Critical' ? 'bg-danger text-white' : ''}">
                        <span class="fw-bold">${a.petName}</span>
                        <span class="badge
                            ${a.status eq 'Admitted'   ? 'bg-success' :
                              a.status eq 'Critical'   ? 'bg-light text-danger fw-bold' :
                                                         'bg-secondary'}">
                            ${a.status}
                        </span>
                    </div>
                    <div class="card-body">
                        <div class="row mb-2">
                            <div class="col-4">
                                <small class="text-muted">Cage</small>
                                <p class="fw-bold mb-0">
                                    <span class="badge bg-secondary">${a.cageNumber}</span>
                                </p>
                            </div>
                            <div class="col-4">
                                <small class="text-muted">Admitted</small>
                                <p class="fw-bold mb-0">${a.admitDate}</p>
                            </div>
                            <div class="col-4">
                                <small class="text-muted">Discharged</small>
                                <p class="fw-bold mb-0">
                                    <c:choose>
                                        <c:when test="${not empty a.dischargeDate}">
                                            ${a.dischargeDate}
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted">Still admitted</span>
                                        </c:otherwise>
                                    </c:choose>
                                </p>
                            </div>
                        </div>

                        <c:if test="${a.status eq 'Critical'}">
                            <div class="alert alert-danger py-2 mb-2">
                                ⚠️ Your pet is in critical condition.
                                Please contact the clinic immediately.
                            </div>
                        </c:if>

                        <a href="${pageContext.request.contextPath}/inpatient/detail?id=${a.admissionID}"
                           class="btn btn-sm btn-outline-primary">
                            View Daily Reports →
                        </a>
                    </div>
                </div>
            </c:forEach>
        </c:otherwise>
    </c:choose>

</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
