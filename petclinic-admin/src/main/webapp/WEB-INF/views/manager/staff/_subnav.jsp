<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- Sub-nav for the Staff module. Detail/Edit are drill-downs reached from
     the list, not peer tabs, so only the two top-level screens appear here.
     The including page sets "activeTab" (list | statistics) beforehand. --%>
<nav class="subnav">
    <a href="${pageContext.request.contextPath}/manager/staff"
       class="subnav-item ${activeTab == 'list' ? 'active' : ''}">Danh sách nhân viên</a>
    <a href="${pageContext.request.contextPath}/manager/staff/statistics"
       class="subnav-item ${activeTab == 'statistics' ? 'active' : ''}">Thống kê hiệu suất</a>
</nav>
