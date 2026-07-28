<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeTab" value="stockIn" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nhập Kho - PetClinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>

<div class="layout">
    <c:set var="activeModule" value="inventory" scope="request"/>
    <%@ include file="/WEB-INF/views/common/_sidebar.jsp" %>

    <main class="main-content main-content-wide">
        <div class="page-header">
            <h1>Nhập Kho</h1>
            <p class="page-sub">Bổ sung tồn kho cho item có sẵn, hoặc đăng ký item thuốc/vaccine mới</p>
        </div>

        <%@ include file="/WEB-INF/views/common/_flash.jsp" %>
        <%@ include file="/WEB-INF/views/manager/inventory/_subnav.jsp" %>

        <div class="card">
            <div class="card-header">
                <span class="card-title">Thông tin nhập kho</span>
            </div>
            <div class="card-body">
                <form action="${pageContext.request.contextPath}/manager/inventory/stock-in"
                      method="post" id="stockInForm">

                    <div class="form-row col-3">
                        <div class="form-group">
                            <label class="form-label" for="stockItemKey">Item có sẵn</label>
                            <select id="stockItemKey" name="stockItemKey" class="form-control no-icon">
                                <option value="">Tạo item mới</option>
                                <c:forEach items="${allItems}" var="item">
                                    <option value="${item.itemType}:${item.itemID}">
                                        [${item.itemType}] <c:out value="${item.displayName}"/> - còn ${item.stockQty}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="itemType">Loại item mới</label>
                            <select id="itemType" name="itemType" class="form-control no-icon">
                                <option value="Medicine">Medicine</option>
                                <option value="Vaccine">Vaccine</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="itemName">Tên item mới</label>
                            <input type="text" id="itemName" name="itemName"
                                   class="form-control no-icon" placeholder="VD: Amoxicillin 250mg">
                        </div>
                    </div>

                    <div class="form-row col-3">
                        <div class="form-group">
                            <label class="form-label" for="quantity">
                                Số lượng nhập <span class="required">*</span>
                            </label>
                            <input type="number" id="quantity" name="quantity"
                                   class="form-control no-icon" min="1" step="1" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="unit">Đơn vị thuốc</label>
                            <input type="text" id="unit" name="unit"
                                   class="form-control no-icon" placeholder="viên, chai, lọ">
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="unitPrice">Đơn giá</label>
                            <input type="number" id="unitPrice" name="unitPrice"
                                   class="form-control no-icon" min="0" step="0.01">
                        </div>
                    </div>

                    <div class="form-row col-3" id="newItemThresholdRow">
                        <div class="form-group">
                            <label class="form-label" for="minStockLevel">Ngưỡng cảnh báo ban đầu</label>
                            <input type="number" id="minStockLevel" name="minStockLevel"
                                   class="form-control no-icon" min="0" step="1" placeholder="10">
                            <span class="form-hint">
                                Chỉ áp dụng khi tạo item mới. Muốn đổi ngưỡng của item đã có,
                                dùng màn <a href="${pageContext.request.contextPath}/manager/inventory/thresholds">Ngưỡng cảnh báo</a>.
                            </span>
                        </div>
                    </div>

                    <div class="form-actions">
                        <button type="submit" class="btn btn-primary">Lưu nhập kho</button>
                        <a href="${pageContext.request.contextPath}/manager/inventory" class="btn btn-outline">
                            Về danh sách tồn kho
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </main>
</div>

<script>
    var stockInForm = document.getElementById('stockInForm');
    var stockItemKey = document.getElementById('stockItemKey');
    var thresholdRow = document.getElementById('newItemThresholdRow');

    function toggleNewItemFields() {
        var isExisting = !!stockItemKey.value;
        thresholdRow.style.display = isExisting ? 'none' : '';
    }

    if (stockItemKey) {
        stockItemKey.addEventListener('change', toggleNewItemFields);
        toggleNewItemFields();
    }

    if (stockInForm) {
        stockInForm.addEventListener('submit', function (e) {
            var existing = stockItemKey.value;
            var itemType = document.getElementById('itemType').value;
            var name = document.getElementById('itemName').value.trim();
            var unit = document.getElementById('unit').value.trim();
            var price = document.getElementById('unitPrice').value.trim();

            if (!existing && (!name || price === '' || (itemType === 'Medicine' && !unit))) {
                e.preventDefault();
                alert('Khi tạo item mới, vui lòng nhập tên, đơn giá và đơn vị nếu là Medicine.');
            }
        });
    }
</script>
<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
</body>
</html>
