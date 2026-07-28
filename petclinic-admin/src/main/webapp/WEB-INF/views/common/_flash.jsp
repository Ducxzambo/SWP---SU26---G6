<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- Shared flash-message block, included by every staff (manager/vet) screen. --%>
<c:if test="${not empty sessionScope.flashSuccess}">
    <div class="alert alert-success"><span class="alert-icon">✓</span> ${sessionScope.flashSuccess}</div>
    <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashWarning}">
    <div class="alert alert-warning"><span class="alert-icon">!</span> ${sessionScope.flashWarning}</div>
    <c:remove var="flashWarning" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error"><span class="alert-icon">x</span> ${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session"/>
</c:if>
<c:if test="${not empty requestScope.error}">
    <div class="alert alert-error"><span class="alert-icon">x</span> ${requestScope.error}</div>
</c:if>
