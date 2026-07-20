<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Discharge Pet</title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body class="bg-light">
<div class="container mt-4" style="max-width:720px">

    <h4 class="mb-4">📋 Discharge Confirmation</h4>

    <c:if test="${not empty error}">
        <div class="alert alert-danger">${error}</div>
    </c:if>

    <c:if test="${not empty admission}">

        <%-- Admission summary --%>
        <div class="card mb-3 shadow-sm">
            <div class="card-header fw-semibold bg-warning-subtle">
                Admission Summary
            </div>
            <div class="card-body">
                <div class="row">
                    <div class="col-sm-6">
                        <p class="mb-1"><strong>Pet:</strong> ${admission.petName}</p>
                        <p class="mb-1"><strong>Owner:</strong> ${admission.ownerName}</p>
                        <p class="mb-1"><strong>Email:</strong> ${admission.ownerEmail}</p>
                    </div>
                    <div class="col-sm-6">
                        <p class="mb-1"><strong>Cage:</strong>
                            <span class="badge bg-secondary">${admission.cageNumber}</span>
                        </p>
                        <p class="mb-1"><strong>Admit Date:</strong> ${admission.admitDate}</p>
                        <p class="mb-1"><strong>Discharge:</strong> Today</p>
                    </div>
                </div>
                <div class="alert alert-info mt-3 mb-0">
                    <strong>Fee will be calculated automatically:</strong>
                    200,000 VND × number of days stayed.
                    Invoice will be created and sent to customer upon confirmation.
                </div>
            </div>
        </div>

        <%-- Assessment history summary --%>
        <div class="card mb-4 shadow-sm">
            <div class="card-header fw-semibold">
                Assessment History
                <span class="badge bg-primary ms-1">${assessments.size()} entries</span>
            </div>
            <div class="card-body p-0">
                <c:choose>
                    <c:when test="${empty assessments}">
                        <p class="text-muted p-3 mb-0">No assessments recorded.</p>
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
                                    <td>${a.treatmentToday}</td>
                                    <td>${a.vetName}</td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <%-- Confirm button --%>
        <form method="post"
              action="${pageContext.request.contextPath}/inpatient/discharge"
              onsubmit="return confirm('Confirm discharge and generate invoice for ${admission.petName}?')">
            <input type="hidden" name="admissionId" value="${admission.admissionID}">

            <div class="d-flex gap-2">
                <button type="submit" class="btn btn-warning fw-semibold">
                    ✅ Confirm Discharge &amp; Generate Invoice
                </button>
                <a href="${pageContext.request.contextPath}/inpatient/list"
                   class="btn btn-outline-secondary">Cancel</a>
            </div>
        </form>

    </c:if>

</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
