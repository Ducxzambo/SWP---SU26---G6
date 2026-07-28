<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- Sub-nav for the Refund module. Detail is a drill-down reached from the
     list, not a peer tab. The including page sets "activeTab"
     (list | create) beforehand. --%>
<nav class="subnav">
    <a href="${pageContext.request.contextPath}/manager/refunds"
       class="subnav-item ${activeTab == 'list' ? 'active' : ''}">Yêu cầu hoàn tiền</a>
    <a href="${pageContext.request.contextPath}/manager/refunds/create"
       class="subnav-item ${activeTab == 'create' ? 'active' : ''}">Tạo yêu cầu mới</a>
</nav>
