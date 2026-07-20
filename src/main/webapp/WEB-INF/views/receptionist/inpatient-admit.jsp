<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Admit Pet — Inpatient</title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <style>
        .cage-card {
            cursor: pointer;
            border: 2px solid #dee2e6;
            transition: all .2s;
        }
        .cage-card:hover { border-color: #0d6efd; background: #f0f7ff; }
        .cage-card.selected { border-color: #0d6efd; background: #cfe2ff; }
        .cage-card.occupied { opacity: .5; cursor: not-allowed; }
    </style>
</head>
<body class="bg-light">
<div class="container mt-4" style="max-width:680px">

    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="mb-0">🏥 Admit Pet for Inpatient Care</h4>
        <a href="${pageContext.request.contextPath}/inpatient/list"
           class="btn btn-sm btn-outline-secondary">← Back</a>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger alert-dismissible fade show">
                ${error}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <form method="post"
          action="${pageContext.request.contextPath}/inpatient/admit"
          id="admitForm">

        <input type="hidden" name="recordId"      value="${recordId}">
        <input type="hidden" name="petId"         value="${petId}">
        <input type="hidden" name="appointmentId" value="${appointmentId}">
        <%-- cageID filled by JS when card is clicked --%>
        <input type="hidden" name="cageID" id="selectedCageID">

        <%-- Cage selection ─────────────────────────────────────────── --%>
        <div class="card shadow-sm mb-3">
            <div class="card-header fw-semibold">
                Select Available Cage
                <span class="badge bg-success ms-1">${availableCages.size()} available</span>
            </div>
            <div class="card-body">

                <c:choose>
                    <c:when test="${empty availableCages}">
                        <div class="alert alert-warning mb-0">
                            No cages are currently available.
                            Please discharge a patient or add a new cage first.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="row g-2" id="cageGrid">
                            <c:forEach items="${availableCages}" var="cage">
                                <div class="col-6 col-md-4">
                                    <div class="cage-card rounded p-3 text-center"
                                         data-cage-id="${cage.cageID}"
                                         data-cage-number="${cage.cageNumber}"
                                         onclick="selectCage(this)">
                                        <div class="fw-bold fs-5">${cage.cageNumber}</div>
                                        <small class="text-muted">
                                                ${not empty cage.cageType ? cage.cageType : 'Standard'}
                                        </small>
                                        <c:if test="${not empty cage.notes}">
                                            <div class="text-muted" style="font-size:11px">
                                                    ${cage.notes}
                                            </div>
                                        </c:if>
                                        <div class="mt-1">
                                            <span class="badge bg-success">Available</span>
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>

                        <div id="selectedLabel" class="mt-3 d-none">
                            <span class="fw-semibold">Selected:</span>
                            <span class="badge bg-primary fs-6" id="selectedBadge"></span>
                        </div>
                    </c:otherwise>
                </c:choose>

            </div>
        </div>

        <%-- Admit date ─────────────────────────────────────────────── --%>
        <div class="card shadow-sm mb-4">
            <div class="card-body">
                <label class="form-label fw-semibold">Admit Date</label>
                <input type="text" class="form-control bg-light"
                       value="Today — set automatically" readonly>
            </div>
        </div>

        <%-- Submit ─────────────────────────────────────────────────── --%>
        <div class="d-flex gap-2">
            <button type="submit" class="btn btn-primary"
                    id="submitBtn" disabled
                    onclick="return validateForm()">
                Confirm Admission
            </button>
            <a href="${pageContext.request.contextPath}/inpatient/list"
               class="btn btn-outline-secondary">Cancel</a>
        </div>

    </form>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
    function selectCage(el) {
        // Deselect all
        document.querySelectorAll('.cage-card').forEach(c => {
            c.classList.remove('selected');
        });
        // Select clicked
        el.classList.add('selected');

        const id     = el.dataset.cageId;
        const number = el.dataset.cageNumber;

        document.getElementById('selectedCageID').value = id;
        document.getElementById('selectedBadge').textContent = number;
        document.getElementById('selectedLabel').classList.remove('d-none');
        document.getElementById('submitBtn').disabled = false;
    }

    function validateForm() {
        const cageID = document.getElementById('selectedCageID').value;
        if (!cageID) {
            alert('Please select a cage first.');
            return false;
        }
        return true;
    }
</script>
</body>
</html>