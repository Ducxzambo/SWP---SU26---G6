<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Inpatient Detail — Receptionist</title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body class="bg-light">
<div class="container mt-4" style="max-width:800px">

    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="mb-0">🏥 Inpatient Detail</h4>
        <a href="${pageContext.request.contextPath}/inpatient/list"
           class="btn btn-sm btn-outline-secondary">← Back to List</a>
    </div>

    <c:if test="${not empty admission}">

        <%-- Status badge --%>
        <c:if test="${admission.status eq 'Critical'}">
            <div class="alert alert-danger fw-bold">
                ⚠️ This pet is currently in CRITICAL condition.
            </div>
        </c:if>

        <%-- Info card --%>
        <div class="card shadow-sm mb-3">
            <div class="card-header fw-semibold">Patient Information</div>
            <div class="card-body">
                <div class="row">
                    <div class="col-sm-4">
                        <p class="mb-1"><small class="text-muted">Pet</small></p>
                        <p class="fw-bold">${admission.petName}</p>
                    </div>
                    <div class="col-sm-4">
                        <p class="mb-1"><small class="text-muted">Owner</small></p>
                        <p class="fw-bold">${admission.ownerName}</p>
                    </div>
                    <div class="col-sm-4">
                        <p class="mb-1"><small class="text-muted">Cage</small></p>
                        <p class="fw-bold">
                            <span class="badge bg-secondary fs-6">
                                ${admission.cageNumber}
                            </span>
                        </p>
                    </div>
                    <div class="col-sm-4">
                        <p class="mb-1"><small class="text-muted">Admit Date</small></p>
                        <p class="fw-bold">${admission.admitDate}</p>
                    </div>
                    <div class="col-sm-4">
                        <p class="mb-1"><small class="text-muted">Status</small></p>
                        <span class="badge fs-6
                            ${admission.status eq 'Admitted' ? 'bg-success' :
                              admission.status eq 'Critical' ? 'bg-danger'  : 'bg-secondary'}">
                            ${admission.status}
                        </span>
                    </div>
                    <div class="col-sm-4">
                        <p class="mb-1"><small class="text-muted">Discharge Date</small></p>
                        <p class="fw-bold">
                            <c:choose>
                                <c:when test="${not empty admission.dischargeDate}">
                                    ${admission.dischargeDate}
                                </c:when>
                                <c:otherwise>
                                    <span class="text-muted">—</span>
                                </c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                </div>
            </div>
        </div>

        <%-- Assessment history --%>
        <div class="card shadow-sm mb-3">
            <div class="card-header fw-semibold">
                Daily Assessment History
                <span class="badge bg-primary ms-1">${assessments.size()}</span>
            </div>
            <div class="card-body p-0">
                <c:choose>
                    <c:when test="${empty assessments}">
                        <p class="text-muted p-3 mb-0">No assessments recorded yet.</p>
                    </c:when>
                    <c:otherwise>
                        <table class="table table-sm table-striped mb-0">
                            <thead class="table-secondary">
                                <tr>
                                    <th>Date</th>
                                    <th>Condition</th>
                                    <th>Treatment</th>
                                    <th>Vet</th>
                                </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${assessments}" var="a">
                                <tr>
                                    <td>${a.assessmentDate}</td>
                                    <td>${a.condition}</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty a.treatmentToday}">
                                                ${a.treatmentToday}
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted">—</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>${a.vetName}</td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <%-- Actions --%>
        <c:if test="${admission.status ne 'Discharged'}">
            <div class="d-flex gap-2">
                <a href="${pageContext.request.contextPath}/inpatient/discharge?admissionId=${admission.admissionID}"
                   class="btn btn-warning">
                    Discharge Pet
                </a>
            </div>
        </c:if>

    </c:if>

</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
