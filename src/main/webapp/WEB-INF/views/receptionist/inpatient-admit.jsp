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
</head>
<body class="bg-light">
<div class="container mt-4" style="max-width:600px">

    <h4 class="mb-4">🏥 Admit Pet for Inpatient Care</h4>

    <%-- Alert messages --%>
    <c:if test="${not empty error}">
        <div class="alert alert-danger alert-dismissible fade show">
            ${error}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <div class="card shadow-sm">
        <div class="card-body">
            <form method="post"
                  action="${pageContext.request.contextPath}/inpatient/admit">

                <%-- Hidden fields passed from examination page --%>
                <input type="hidden" name="recordId"      value="${recordId}">
                <input type="hidden" name="petId"         value="${petId}">
                <input type="hidden" name="appointmentId" value="${appointmentId}">

                <%-- Cage number --%>
                <div class="mb-3">
                    <label class="form-label fw-semibold">
                        Cage / Room Number <span class="text-danger">*</span>
                    </label>
                    <input type="text" name="cageNumber"
                           class="form-control" placeholder="e.g. A1, B2, C3"
                           maxlength="20" required autofocus>

                    <c:if test="${not empty occupiedCages}">
                        <div class="form-text text-danger mt-1">
                            <strong>Currently occupied:</strong>
                            <c:forEach items="${occupiedCages}" var="cage" varStatus="st">
                                <span class="badge bg-danger">${cage}</span>
                            </c:forEach>
                        </div>
                    </c:if>
                    <c:if test="${empty occupiedCages}">
                        <div class="form-text text-success">
                            All cages are currently available.
                        </div>
                    </c:if>
                </div>

                <%-- Admit date (display only — system uses GETDATE()) --%>
                <div class="mb-4">
                    <label class="form-label fw-semibold">Admit Date</label>
                    <input type="text" class="form-control bg-light"
                           value="${pageContext.response.locale} — Today"
                           readonly>
                    <div class="form-text">Set automatically to today's date.</div>
                </div>

                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-primary">
                        Confirm Admission
                    </button>
                    <a href="${pageContext.request.contextPath}/inpatient/list"
                       class="btn btn-outline-secondary">Cancel</a>
                </div>

            </form>
        </div>
    </div>

</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
