<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cage Management</title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <style>
        .cage-available   { border-left: 4px solid #198754; }
        .cage-occupied    { border-left: 4px solid #dc3545; }
        .cage-maintenance { border-left: 4px solid #6c757d; }
    </style>
</head>
<body class="bg-light">
<div class="container-fluid mt-4 px-4">

    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="mb-0">🏠 Cage Management</h4>
        <div class="d-flex gap-2">
            <a href="${pageContext.request.contextPath}/inpatient/list"
               class="btn btn-sm btn-outline-secondary">← Inpatient List</a>
            <button class="btn btn-sm btn-primary"
                    data-bs-toggle="modal" data-bs-target="#addCageModal">
                + Add Cage
            </button>
        </div>
    </div>

    <%-- Alerts --%>
    <c:if test="${not empty param.success}">
        <div class="alert alert-success alert-dismissible fade show">
            Operation completed successfully.
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger alert-dismissible fade show">
                ${error}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <%-- Stats bar --%>
    <c:set var="available"   value="0"/>
    <c:set var="occupied"    value="0"/>
    <c:set var="maintenance" value="0"/>
    <c:forEach items="${cages}" var="c">
        <c:if test="${c.status eq 'Available'}">
            <c:set var="available" value="${available + 1}"/>
        </c:if>
        <c:if test="${c.status eq 'Occupied'}">
            <c:set var="occupied" value="${occupied + 1}"/>
        </c:if>
        <c:if test="${c.status eq 'Maintenance'}">
            <c:set var="maintenance" value="${maintenance + 1}"/>
        </c:if>
    </c:forEach>
    <div class="row g-2 mb-3">
        <div class="col-4">
            <div class="card text-center border-success">
                <div class="card-body py-2">
                    <div class="fw-bold fs-4 text-success">${available}</div>
                    <small class="text-muted">Available</small>
                </div>
            </div>
        </div>
        <div class="col-4">
            <div class="card text-center border-danger">
                <div class="card-body py-2">
                    <div class="fw-bold fs-4 text-danger">${occupied}</div>
                    <small class="text-muted">Occupied</small>
                </div>
            </div>
        </div>
        <div class="col-4">
            <div class="card text-center border-secondary">
                <div class="card-body py-2">
                    <div class="fw-bold fs-4 text-secondary">${maintenance}</div>
                    <small class="text-muted">Maintenance</small>
                </div>
            </div>
        </div>
    </div>

    <%-- Cage grid --%>
    <c:choose>
        <c:when test="${empty cages}">
            <div class="alert alert-info">No cages configured yet.</div>
        </c:when>
        <c:otherwise>
            <div class="row g-3">
                <c:forEach items="${cages}" var="cage">
                    <div class="col-sm-6 col-md-4 col-lg-3">
                        <div class="card shadow-sm h-100
                        ${cage.status eq 'Available'   ? 'cage-available'   :
                          cage.status eq 'Occupied'    ? 'cage-occupied'    :
                                                         'cage-maintenance'}">
                            <div class="card-body">
                                    <%-- Cage number + status --%>
                                <div class="d-flex justify-content-between align-items-start mb-2">
                                    <h5 class="mb-0 fw-bold">${cage.cageNumber}</h5>
                                    <span class="badge
                                    ${cage.status eq 'Available'   ? 'bg-success' :
                                      cage.status eq 'Occupied'    ? 'bg-danger'  :
                                                                     'bg-secondary'}">
                                            ${cage.status}
                                    </span>
                                </div>

                                    <%-- Type + notes --%>
                                <p class="text-muted mb-1" style="font-size:13px">
                                        ${not empty cage.cageType ? cage.cageType : 'Standard'}
                                </p>
                                <c:if test="${not empty cage.notes}">
                                    <p class="text-muted mb-2" style="font-size:12px">
                                            ${cage.notes}
                                    </p>
                                </c:if>

                                    <%-- Occupied info --%>
                                <c:if test="${cage.status eq 'Occupied'}">
                                    <div class="alert alert-danger py-1 px-2 mb-2"
                                         style="font-size:12px">
                                        🐾 <strong>${cage.currentPetName}</strong>
                                        <br>Since: ${cage.admitDate}
                                    </div>
                                </c:if>

                                    <%-- Actions --%>
                                <c:if test="${cage.status ne 'Occupied'}">
                                    <form method="post"
                                          action="${pageContext.request.contextPath}/inpatient/cages">
                                        <input type="hidden" name="action"  value="toggle">
                                        <input type="hidden" name="cageID"  value="${cage.cageID}">
                                        <c:choose>
                                            <c:when test="${cage.status eq 'Maintenance'}">
                                                <input type="hidden" name="isActive" value="1">
                                                <button type="submit"
                                                        class="btn btn-sm btn-outline-success w-100">
                                                    ✅ Set Available
                                                </button>
                                            </c:when>
                                            <c:otherwise>
                                                <input type="hidden" name="isActive" value="0">
                                                <button type="submit"
                                                        class="btn btn-sm btn-outline-secondary w-100"
                                                        onclick="return confirm('Set to maintenance?')">
                                                    🔧 Set Maintenance
                                                </button>
                                            </c:otherwise>
                                        </c:choose>
                                    </form>
                                </c:if>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%-- Add Cage Modal --%>
<div class="modal fade" id="addCageModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Add New Cage</h5>
                <button type="button" class="btn-close"
                        data-bs-dismiss="modal"></button>
            </div>
            <form method="post"
                  action="${pageContext.request.contextPath}/inpatient/cages">
                <input type="hidden" name="action" value="add">
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label fw-semibold">
                            Cage Number <span class="text-danger">*</span>
                        </label>
                        <input type="text" name="cageNumber"
                               class="form-control text-uppercase"
                               placeholder="e.g. CAGE-D01" required maxlength="20">
                        <div class="form-text">Will be converted to uppercase automatically.</div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Cage Type</label>
                        <select name="cageType" class="form-select">
                            <option value="Small">Small</option>
                            <option value="Medium" selected>Medium</option>
                            <option value="Large">Large</option>
                            <option value="ICU">ICU</option>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Notes</label>
                        <input type="text" name="notes"
                               class="form-control" maxlength="200"
                               placeholder="e.g. For small dogs and cats">
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary"
                            data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">Add Cage</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>