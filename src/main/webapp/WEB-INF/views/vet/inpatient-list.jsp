<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Inpatient List — Receptionist</title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body class="bg-light">
<div class="container-fluid mt-4 px-4">

    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="mb-0">🏥 Active Inpatient Cases</h4>
    </div>

    <c:if test="${not empty param.success}">
        <div class="alert alert-success alert-dismissible fade show">
            <c:choose>
                <c:when test="${param.success eq 'admitted'}">
                    Pet admitted successfully.
                </c:when>
                <c:otherwise>Operation completed.</c:otherwise>
            </c:choose>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <c:choose>
        <c:when test="${empty admissions}">
            <div class="alert alert-info">No active inpatient cases at the moment.</div>
        </c:when>
        <c:otherwise>
            <div class="card shadow-sm">
                <div class="card-body p-0">
                    <table class="table table-hover table-bordered mb-0">
                        <thead class="table-dark">
                            <tr>
                                <th>#</th>
                                <th>Pet</th>
                                <th>Owner</th>
                                <th>Cage</th>
                                <th>Admit Date</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${admissions}" var="a" varStatus="st">
                            <tr class="${a.status eq 'Critical' ? 'table-danger' : ''}">
                                <td>${st.count}</td>
                                <td><strong>${a.petName}</strong></td>
                                <td>${a.ownerName}</td>
                                <td>
                                    <span class="badge bg-secondary">${a.cageNumber}</span>
                                </td>
                                <td>${a.admitDate}</td>
                                <td>
                                    <span class="badge
                                        ${a.status eq 'Admitted'  ? 'bg-success' :
                                          a.status eq 'Critical'  ? 'bg-danger'  :
                                                                    'bg-secondary'}">
                                        ${a.status}
                                    </span>
                                </td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/inpatient/detail?id=${a.admissionID}"
                                       class="btn btn-sm btn-outline-primary">View</a>
                                    <a href="${pageContext.request.contextPath}/inpatient/discharge?admissionId=${a.admissionID}"
                                       class="btn btn-sm btn-outline-warning">Discharge</a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </c:otherwise>
    </c:choose>

</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
