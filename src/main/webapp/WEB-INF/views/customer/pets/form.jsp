<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx"      value="${pageContext.request.contextPath}"/>
<c:set var="isEdit"   value="${not empty editMode and editMode}"/>
<c:set var="p"        value="${pet}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${isEdit ? 'Sửa thông tin' : 'Thêm thú cưng'} – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
  <link rel="stylesheet" href="${ctx}/css/pets.css">
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div class="pet-form-wrap">

  <a href="${isEdit ? ctx.concat('/pets/profile?id=').concat(p.petID) : ctx.concat('/pets')}"
     class="detail-back">
    ← ${isEdit ? 'Quay lại hồ sơ' : 'Danh sách thú cưng'}
  </a>

  <div class="pet-form-card">
    <div class="pet-form-head">
      ${isEdit ? 'Chỉnh sửa thông tin' : 'Thêm thú cưng mới'}
    </div>
    <div class="pet-form-body">

      <c:if test="${not empty requestScope.error}">
        <div style="background:#f8d7da;border:1px solid #f5c6cb;border-radius:8px;
                    padding:12px 16px;color:#721c24;font-size:14px;margin-bottom:20px;">
          ✗ ${requestScope.error}
        </div>
      </c:if>

      <form action="${ctx}/pets/${isEdit ? 'edit' : 'new'}" method="post" novalidate>
        <c:if test="${isEdit}">
          <input type="hidden" name="petId" value="${p.petID}">
        </c:if>

        <div class="form-group">
          <label>Tên thú cưng <span class="req">*</span></label>
          <input type="text" name="name" class="form-control" required
                 placeholder="VD: Mochi, Coco, Pudding..."
                 value="${not empty p ? p.name : ''}">
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>Loài <span class="req">*</span></label>
            <select name="speciesName" class="form-control" required>
              <option value="">-- Chọn loài --</option>
              <c:forEach var="sp" items="${['Chó','Mèo','Chim','Thỏ','Cá','Khác']}">
                <option value="${sp}" ${not empty p and p.speciesName eq sp ? 'selected' : ''}>${sp}</option>
              </c:forEach>
            </select>
          </div>
          <div class="form-group">
            <label>Giống <span class="req">*</span></label>
            <input type="text" name="breedName" class="form-control" required
                   placeholder="VD: Poodle, Corgi, Ba Tư..."
                   value="${not empty p ? p.breedName : ''}">
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>Giới tính</label>
            <select name="gender" class="form-control">
              <option value="Unknown" ${empty p or p.gender eq 'Unknown' ? 'selected' : ''}>Không rõ</option>
              <option value="Male"    ${not empty p and p.gender eq 'Male'    ? 'selected' : ''}>Đực</option>
              <option value="Female"  ${not empty p and p.gender eq 'Female'  ? 'selected' : ''}>Cái</option>
            </select>
          </div>
          <div class="form-group">
            <label>Cân nặng (kg)</label>
            <input type="number" name="weight" class="form-control"
                   step="0.1" min="0.1" max="200" placeholder="VD: 3.5"
                   value="${not empty p and p.weight != null ? p.weight : ''}">
          </div>
        </div>

        <div class="form-group">
          <label>Ngày sinh</label>
          <input type="date" name="dateOfBirth" class="form-control"
                 max="${pageContext.request.getAttribute('today')}"
                 value="${not empty p and p.dateOfBirth != null ? p.dateOfBirth : ''}">
          <div class="form-hint">Dùng để tính tuổi và nhắc lịch vaccine</div>
        </div>

        <div class="form-actions">
          <a href="${isEdit ? ctx.concat('/pets/profile?id=').concat(p.petID) : ctx.concat('/pets')}"
             class="btn-back-link">Huỷ</a>
          <button type="submit" class="btn-save">
            ${isEdit ? 'Lưu thay đổi' : 'Thêm thú cưng'}
          </button>
        </div>
      </form>

      <%-- Delete zone (edit mode only) --%>
      <c:if test="${isEdit}">
        <div class="delete-zone">
          <h4>⚠ Xoá thú cưng</h4>
          <p>Thú cưng sẽ bị ẩn khỏi danh sách nhưng lịch sử khám vẫn được lưu lại.</p>
          <button class="btn-delete-pet" onclick="openDeleteModal()">Xoá thú cưng này</button>
        </div>
      </c:if>

    </div><%-- /pet-form-body --%>
  </div>

</div>

<%-- Delete confirm modal (edit mode only) --%>
<c:if test="${isEdit}">
<div class="delete-modal-overlay" id="deleteModal">
  <div class="delete-modal-box">
    <div class="delete-modal-head">
      <span style="font-size:22px;">⚠️</span>
      <h2>Xác nhận xoá thú cưng</h2>
    </div>
    <div class="delete-modal-body">
      Bạn sắp xoá <strong>${p.name}</strong> khỏi danh sách.<br>
      Hành động này <strong>không thể hoàn tác</strong> (lịch sử khám vẫn được giữ lại).
    </div>
    <div class="delete-modal-foot">
      <button class="modal-btn-secondary" onclick="closeDeleteModal()">Không, quay lại</button>
      <form action="${ctx}/pets/delete" method="post" style="flex:1;">
        <input type="hidden" name="petId" value="${p.petID}">
        <button type="submit" class="modal-btn-danger" style="width:100%;">Xác nhận xoá</button>
      </form>
    </div>
  </div>
</div>
</c:if>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>

<script>
  function openDeleteModal() {
    const m = document.getElementById('deleteModal');
    if (m) { m.classList.add('open'); document.body.style.overflow = 'hidden'; }
  }
  function closeDeleteModal() {
    const m = document.getElementById('deleteModal');
    if (m) { m.classList.remove('open'); document.body.style.overflow = ''; }
  }
  const overlay = document.getElementById('deleteModal');
  if (overlay) overlay.addEventListener('click', e => { if (e.target === overlay) closeDeleteModal(); });

  // Set today as max date for dateOfBirth
  const dobInput = document.querySelector('input[name="dateOfBirth"]');
  if (dobInput && !dobInput.getAttribute('max')) {
    dobInput.max = new Date().toISOString().split('T')[0];
  }
</script>
</body>
</html>
