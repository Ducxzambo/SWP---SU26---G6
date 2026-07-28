<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- Sub-nav for the Inventory module. The including page sets a request
     attribute "activeTab" (list | stockIn | thresholds | transactions | report)
     before including this fragment so the right tab gets highlighted. --%>
<nav class="subnav">
    <a href="${pageContext.request.contextPath}/manager/inventory"
       class="subnav-item ${activeTab == 'list' ? 'active' : ''}">Tồn kho</a>
    <a href="${pageContext.request.contextPath}/manager/inventory/stock-in"
       class="subnav-item ${activeTab == 'stockIn' ? 'active' : ''}">Nhập kho</a>
    <a href="${pageContext.request.contextPath}/manager/inventory/thresholds"
       class="subnav-item ${activeTab == 'thresholds' ? 'active' : ''}">Ngưỡng cảnh báo</a>
    <a href="${pageContext.request.contextPath}/manager/inventory/transactions"
       class="subnav-item ${activeTab == 'transactions' ? 'active' : ''}">Lịch sử giao dịch</a>
    <a href="${pageContext.request.contextPath}/manager/inventory/report"
       class="subnav-item ${activeTab == 'report' ? 'active' : ''}">Báo cáo</a>
</nav>
