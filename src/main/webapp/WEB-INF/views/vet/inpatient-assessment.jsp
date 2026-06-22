<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Daily Assessment</title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body class="bg-light">
<div class="container mt-4" style="max-width:760px">

    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="mb-0">🩺 Daily Assessment</h4>
        <a href="${pageContext.request.contextPath}/inpatient/list"
           class="btn btn-sm btn-outline-secondary">← Back</a>
    </div>

    <%-- Success / Error alerts --%>
    <c:if test="${not empty param.success}">
        <div class="alert alert-success alert-dismissible fade show">
            Assessment saved successfully. Customer has been notified.
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger alert-dismissible fade show">
            ${error}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <c:if test="${not empty admission}">

        <%-- Patient info --%>
        <div class="card shadow-sm mb-3">
            <div class="card-header fw-semibold">Patient</div>
            <div class="card-body">
                <div class="row">
                    <div class="col-sm-3">
                        <small class="text-muted">Pet</small>
                        <p class="fw-bold mb-0">${admission.petName}</p>
                    </div>
                    <div class="col-sm-3">
                        <small class="text-muted">Owner</small>
                        <p class="fw-bold mb-0">${admission.ownerName}</p>
                    </div>
                    <div class="col-sm-3">
                        <small class="text-muted">Cage</small>
                        <p class="fw-bold mb-0">
                            <span class="badge bg-secondary">${admission.cageNumber}</span>
                        </p>
                    </div>
                    <div class="col-sm-3">
                        <small class="text-muted">Status</small>
                        <p class="mb-0">
                            <span class="badge
                                ${admission.status eq 'Critical' ? 'bg-danger' : 'bg-success'}">
                                ${admission.status}
                            </span>
                        </p>
                    </div>
                </div>
            </div>
        </div>

        <%-- Assessment form (only if today not yet done) --%>
        <c:choose>
            <c:when test="${todayDone}">
                <div class="alert alert-info">
                    ✅ Today's assessment has already been submitted for this patient.
                </div>
            </c:when>
            <c:otherwise>
                <div class="card shadow-sm mb-3">
                    <div class="card-header fw-semibold">
                        Today's Assessment
                        <span class="badge bg-info text-dark ms-1">
                            <%= java.time.LocalDate.now() %>
                        </span>
                    </div>
                    <div class="card-body">
                        <form method="post"
                              action="${pageContext.request.contextPath}/inpatient/assessment">
                            <input type="hidden" name="admissionId"
                                   value="${admission.admissionID}">

                            <div class="mb-3">
                                <label class="form-label fw-semibold">
                                    Current Condition
                                    <span class="text-danger">*</span>
                                </label>
                                <textarea name="condition" class="form-control"
                                    rows="3" required
                                    placeholder="Weight, temperature, behavior, symptoms..."></textarea>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold">
                                    Treatment Today
                                </label>
                                <textarea name="treatmentToday" class="form-control"
                                    rows="3"
                                    placeholder="Medications given, procedures performed..."></textarea>
                            </div>

                            <div class="mb-3 form-check">
                                <input type="checkbox" class="form-check-input"
                                       id="markCritical" name="markCritical">
                                <label class="form-check-label text-danger fw-semibold"
                                       for="markCritical">
                                    ⚠️ Mark as CRITICAL — owner will be notified immediately
                                </label>
                            </div>

                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary">
                                    Save Assessment
                                </button>
                                <a href="${pageContext.request.contextPath}/inpatient/list"
                                   class="btn btn-outline-secondary">Cancel</a>
                            </div>
                        </form>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>

        <%-- Previous assessments --%>
        <div class="card shadow-sm">
            <div class="card-header fw-semibold">
                Assessment History
                <span class="badge bg-primary ms-1">${assessments.size()}</span>
            </div>
            <div class="card-body p-0">
                <c:choose>
                    <c:when test="${empty assessments}">
                        <p class="text-muted p-3 mb-0">No previous assessments.</p>
                    </c:when>
                    <c:otherwise>
                        <table class="table table-sm table-striped mb-0">
                            <thead class="table-secondary">
                                <tr>
                                    <th>Date</th>
                                    <th>Condition</th>
                                    <th>Treatment</th>
                                    <th>Recorded by</th>
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
                                    <td>Dr. ${a.vetName}</td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

    </c:if>

</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
