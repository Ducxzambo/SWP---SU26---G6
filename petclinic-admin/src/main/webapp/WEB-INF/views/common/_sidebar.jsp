<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- Shared manager sidebar. The including page sets a request attribute
     "activeModule" (inventory | staff | refunds) before including this
     fragment so the right top-level item gets highlighted. --%>
<aside class="sidebar">
    <div class="sidebar-logo">PetClinic</div>
    <nav>
        <a href="${pageContext.request.contextPath}/admin/inventory"
           class="nav-item ${activeModule == 'inventory' ? 'active' : ''}">Quản lý kho</a>
        <a href="${pageContext.request.contextPath}/admin/staff"
           class="nav-item ${activeModule == 'staff' ? 'active' : ''}">Quản lý nhân viên</a>
        <a href="${pageContext.request.contextPath}/admin/refunds"
           class="nav-item ${activeModule == 'refunds' ? 'active' : ''}">Quản lý hoàn tiền</a>
        <a href="${pageContext.request.contextPath}/admin/providers"
           class="nav-item ${activeModule == 'providers' ? 'active' : ''}">Quản lý nhà cung cấp</a>
    </nav>
    <div class="sidebar-user">
        ${sessionScope.staff.fullName}
        <a href="${pageContext.request.contextPath}/auth/staff/logout" class="logout-link">Đăng xuất</a>
    </div>
</aside>
