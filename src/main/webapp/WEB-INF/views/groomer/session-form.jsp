<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>
    <c:choose>
      <c:when test="${not empty record}">Phiên Grooming #${record.recordID} – PetClinic</c:when>
      <c:otherwise>Grooming – PetClinic</c:otherwise>
    </c:choose>
  </title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
  <style>
    .pet-strip{display:flex;gap:12px;flex-wrap:wrap;background:var(--teal-50);
      border:1px solid var(--teal-100);border-radius:10px;padding:14px 18px;margin-bottom:20px}
    .pet-strip-item{display:flex;flex-direction:column;gap:2px;min-width:100px}
    .pet-strip-item .lbl{font-size:11px;text-transform:uppercase;letter-spacing:.6px;color:var(--text-soft);font-weight:500}
    .pet-strip-item .val{font-size:14px;font-weight:600;color:var(--teal-800)}

    .opt-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:8px}
    .opt-pill{display:flex;align-items:center;gap:8px;border:1.5px solid var(--border);
      border-radius:8px;padding:9px 12px;cursor:pointer;transition:var(--transition);font-size:13.5px}
    .opt-pill:hover{border-color:var(--teal-400)}
    .opt-pill input[type=radio]{accent-color:var(--teal-500);cursor:pointer}
    .opt-pill.selected{border-color:var(--teal-500);background:var(--teal-50);font-weight:600;color:var(--teal-800)}

    .flag-box{border:1.5px solid var(--red-100);background:#fff8f7;border-radius:10px;padding:14px 16px;margin-top:6px}
    .flag-box.active{border-color:var(--red-400)}
    .flag-note{display:none;margin-top:10px}
    .flag-note.show{display:block}

    .hist-item{border:1px solid var(--border);border-radius:8px;margin-bottom:8px;overflow:hidden}
    .hist-hdr{display:flex;justify-content:space-between;align-items:center;padding:10px 14px;
      background:var(--bg);cursor:pointer;user-select:none}
    .hist-hdr:hover{background:var(--teal-50)}
    .hist-body{display:none;padding:14px 16px;border-top:1px solid var(--border);font-size:13.5px;line-height:1.7}
    .hist-body.open{display:block}
    .hist-body dl{display:grid;grid-template-columns:130px 1fr;gap:4px 10px}
    .hist-body dt{font-weight:600;color:var(--text-mid)}
  </style>
</head>
<body>
<div class="layout">
  <aside class="sidebar">
    <div class="sidebar-logo">🐾 PetClinic</div>
    <nav><a href="${pageContext.request.contextPath}/groomer/session" class="nav-item active"> Hàng chờ</a></nav>
    <div class="sidebar-user">
      👤 ${sessionScope.staff.fullName}
      <a href="${pageContext.request.contextPath}/auth/logout" class="logout-link">Đăng xuất</a>
    </div>
  </aside>
  <main class="main-content">

    <%-- ═══════════ READ-ONLY VIEW ═══════════ --%>
    <c:if test="${not empty record}">
      <div class="page-header"><h1>📋 Phiên Grooming #${record.recordID}</h1><p class="page-sub">Chỉ xem</p></div>
      <a href="${pageContext.request.contextPath}/groomer/session" class="btn btn-outline btn-sm" style="margin-bottom:18px;">← Quay lại</a>

      <div class="pet-strip">
        <div class="pet-strip-item"><span class="lbl">Thú cưng</span><span class="val">🐾 <c:out value="${record.petName}"/></span></div>
        <div class="pet-strip-item"><span class="lbl">Chủ nhân</span><span class="val"><c:out value="${record.ownerName}"/></span></div>
        <div class="pet-strip-item"><span class="lbl">Groomer</span><span class="val"><c:out value="${record.groomerName}"/></span></div>
        <div class="pet-strip-item"><span class="lbl">Dịch vụ</span><span class="val"><c:out value="${record.serviceName}"/></span></div>
        <div class="pet-strip-item"><span class="lbl">Ngày</span><span class="val">${record.createdAt}</span></div>
      </div>

      <div class="card" style="margin-bottom:16px;">
        <div class="card-header"><span class="card-title"> Kết quả grooming</span></div>
        <div class="card-body">
          <div class="record-field"><span class="record-field-label">Tình trạng lông</span>
            <span class="record-field-value">${not empty record.coatCondition ? record.coatCondition : '—'}</span></div>
          <div class="record-field"><span class="record-field-label">Hành vi thú cưng</span>
            <span class="record-field-value">${not empty record.behavior ? record.behavior : '—'}</span></div>
          <div class="record-field"><span class="record-field-label">Sản phẩm sử dụng</span>
            <span class="record-field-value"><c:out value="${not empty record.productsUsed ? record.productsUsed : '—'}"/></span></div>
          <div class="record-field"><span class="record-field-label">Ghi chú</span>
            <span class="record-field-value" style="white-space:pre-wrap;"><c:out value="${not empty record.notes ? record.notes : '—'}"/></span></div>
        </div>
      </div>

      <c:if test="${record.flagForVet}">
        <div class="alert alert-warning">
          <span class="alert-icon"></span>
          <span><strong>Đã gắn cờ cho bác sĩ:</strong> <c:out value="${record.flagReason}"/></span>
        </div>
      </c:if>
    </c:if>


    <%-- ═══════════ GROOMING FORM ═══════════ --%>
    <c:if test="${not empty appointment}">
      <div class="page-header">
        <h1>✂️ Grooming Session</h1>
        <p class="page-sub"><c:out value="${appointment.petName}"/> — <c:out value="${appointment.customerName}"/> — <c:out value="${appointment.serviceName}"/></p>
      </div>
      <a href="${pageContext.request.contextPath}/groomer/session" class="btn btn-outline btn-sm" style="margin-bottom:18px;">← Quay lại hàng chờ</a>

      <c:if test="${not empty error}"><div class="alert alert-error"><span class="alert-icon">✕</span> ${error}</div></c:if>

      <div class="pet-strip">
        <div class="pet-strip-item"><span class="lbl">Thú cưng</span><span class="val">🐾 <c:out value="${appointment.petName}"/></span></div>
        <div class="pet-strip-item"><span class="lbl">Chủ nhân</span><span class="val"><c:out value="${appointment.customerName}"/></span></div>
        <div class="pet-strip-item"><span class="lbl">Dịch vụ</span><span class="val"><c:out value="${appointment.serviceName}"/></span></div>
        <div class="pet-strip-item"><span class="lbl">Ca</span><span class="val">Ca ${appointment.slotShift}</span></div>
        <div class="pet-strip-item"><span class="lbl">Trạng thái</span><span class="val"><span class="badge badge-info">✂️ Đang grooming</span></span></div>
      </div>

      <%-- Spa history (read-only, NO medical record access) --%>
      <c:if test="${not empty history}">
        <div class="card" style="margin-bottom:16px;">
          <div class="card-header">
            <span class="card-title">Lịch sử Spa (${history.size()} lần)</span>
            <button type="button" class="btn btn-outline btn-sm" onclick="toggleHist()">Hiện / Ẩn</button>
          </div>
          <div id="histSection" style="display:none;padding:14px 20px;">
            <c:forEach items="${history}" var="h" varStatus="vs">
              <div class="hist-item">
                <div class="hist-hdr" onclick="toggleAcc(this)">
                  <span><strong>#${vs.count}</strong> — ${h.createdAt} &nbsp;|&nbsp; <c:out value="${h.serviceName}"/></span>
                  <span>▼</span>
                </div>
                <div class="hist-body">
                  <dl>
                    <dt>Tình trạng lông</dt><dd>${not empty h.coatCondition ? h.coatCondition : '—'}</dd>
                    <dt>Hành vi</dt><dd>${not empty h.behavior ? h.behavior : '—'}</dd>
                    <dt>Sản phẩm dùng</dt><dd><c:out value="${not empty h.productsUsed ? h.productsUsed : '—'}"/></dd>
                    <dt>Ghi chú</dt><dd><c:out value="${not empty h.notes ? h.notes : '—'}"/></dd>
                  </dl>
                </div>
              </div>
            </c:forEach>
          </div>
        </div>
      </c:if>

      <form action="${pageContext.request.contextPath}/groomer/session" method="post" id="groomForm" novalidate>
        <input type="hidden" name="appointmentID" value="${appointment.appointmentID}">

          <%-- Coat condition --%>
        <div class="card" style="margin-bottom:16px;">
          <div class="card-header"><span class="card-title">Tình trạng lông</span></div>
          <div class="card-body">
            <div class="opt-grid" id="coatGrid">
              <label class="opt-pill"><input type="radio" name="coatCondition" value="Good" onchange="selectPill(this,'coatGrid')"> Tốt</label>
              <label class="opt-pill"><input type="radio" name="coatCondition" value="Matted" onchange="selectPill(this,'coatGrid')"> Rối / Vón</label>
              <label class="opt-pill"><input type="radio" name="coatCondition" value="Shedding" onchange="selectPill(this,'coatGrid')"> Rụng nhiều</label>
              <label class="opt-pill"><input type="radio" name="coatCondition" value="Dirty" onchange="selectPill(this,'coatGrid')"> Bẩn</label>
            </div>
          </div>
        </div>

          <%-- Behavior --%>
        <div class="card" style="margin-bottom:16px;">
          <div class="card-header"><span class="card-title">Hành vi thú cưng</span></div>
          <div class="card-body">
            <div class="opt-grid" id="behaviorGrid">
              <label class="opt-pill"><input type="radio" name="behavior" value="Calm" onchange="selectPill(this,'behaviorGrid')"> Bình tĩnh</label>
              <label class="opt-pill"><input type="radio" name="behavior" value="Cooperative" onchange="selectPill(this,'behaviorGrid')"> Hợp tác</label>
              <label class="opt-pill"><input type="radio" name="behavior" value="Anxious" onchange="selectPill(this,'behaviorGrid')"> Lo lắng</label>
              <label class="opt-pill"><input type="radio" name="behavior" value="Aggressive" onchange="selectPill(this,'behaviorGrid')"> Hung dữ</label>
            </div>
          </div>
        </div>

          <%-- Products used --%>
        <div class="card" style="margin-bottom:16px;">
          <div class="card-header"><span class="card-title">Sản phẩm sử dụng</span></div>
          <div class="card-body">
            <input type="text" name="productsUsed" class="form-control no-icon"
                   placeholder="VD: Sữa tắm Hartz, dầu xả dưỡng lông, phấn thơm...">
            <div class="form-hint">Liệt kê sản phẩm đã dùng.</div>
          </div>
        </div>

          <%-- Notes --%>
        <div class="card" style="margin-bottom:16px;">
          <div class="card-header"><span class="card-title">Ghi chú thêm</span></div>
          <div class="card-body">
            <textarea name="notes" class="form-control" rows="3" placeholder="Ghi chú khác về phiên grooming..."></textarea>
          </div>
        </div>

          <%-- Flag for vet --%>
        <div class="card" style="margin-bottom:16px;">
          <div class="card-header"><span class="card-title">Phát hiện bất thường?</span></div>
          <div class="card-body">
            <div class="flag-box" id="flagBox">
              <label style="display:flex;align-items:center;gap:10px;cursor:pointer;">
                <input type="checkbox" name="flagForVet" id="flagCheck" onchange="toggleFlag(this)" style="width:18px;height:18px;accent-color:var(--red-400);">
                <span style="font-weight:500;">Gắn cờ cho bác sĩ thú y kiểm tra (da, ký sinh trùng, vết thương...)</span>
              </label>
              <div class="flag-note" id="flagNote">
            <textarea name="flagReason" class="form-control" rows="2"
                      placeholder="Mô tả bất thường phát hiện được (VD: nốt đỏ trên da, vết cắn, sưng tai...)"></textarea>
              </div>
            </div>
          </div>
        </div>

        <div style="display:flex;gap:12px;justify-content:flex-end;padding-bottom:40px;">
          <a href="${pageContext.request.contextPath}/groomer/session" class="btn btn-outline btn-lg">Hủy</a>
          <button type="submit" class="btn btn-primary btn-lg" id="submitBtn">Hoàn thành Grooming</button>
        </div>
      </form>
    </c:if>

    <c:if test="${empty record && empty appointment}">
      <div class="page-header"><h1> Grooming</h1></div>
      <div class="card"><div class="empty-state"><div class="empty-icon">⚠️</div>
        <p>Không tìm thấy dữ liệu. <a href="${pageContext.request.contextPath}/groomer/session">Quay lại hàng chờ</a></p></div></div>
    </c:if>

  </main>
</div>

<script src="${pageContext.request.contextPath}/js/dashboard.js"></script>
<script>
  function selectPill(input, gridId) {
    document.querySelectorAll('#'+gridId+' .opt-pill').forEach(p => p.classList.remove('selected'));
    input.closest('.opt-pill').classList.add('selected');
  }
  function toggleFlag(cb) {
    const box = document.getElementById('flagBox');
    const note = document.getElementById('flagNote');
    if (cb.checked) { box.classList.add('active'); note.classList.add('show'); }
    else            { box.classList.remove('active'); note.classList.remove('show'); }
  }
  function toggleHist() {
    const s = document.getElementById('histSection');
    if (s) s.style.display = s.style.display === 'none' ? 'block' : 'none';
  }
  function toggleAcc(hdr) {
    const body = hdr.nextElementSibling;
    const icon = hdr.querySelector('span:last-child');
    body.classList.toggle('open');
    if (icon) icon.textContent = body.classList.contains('open') ? '▲' : '▼';
  }
  document.getElementById('groomForm')?.addEventListener('submit', function(e) {
    const flagged = document.getElementById('flagCheck').checked;
    const reason = document.querySelector('[name="flagReason"]').value.trim();
    if (flagged && !reason) {
      e.preventDefault();
      alert('Vui lòng ghi rõ lý do gắn cờ cho bác sĩ.');
      return;
    }
    document.getElementById('submitBtn').disabled = true;
    document.getElementById('submitBtn').textContent = '⏳ Đang lưu...';
  });
</script>
</body>
</html>
