<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<c:if test="${appt.status eq 'Done'}">
<div class="review-section">
  <div class="review-section-head">
    <div>Đánh giá lịch khám</div>

    <c:if test="${not empty review}">
      <%-- Nút form edit --%>
      <button type="button" class="btn-sm btn-outline" onclick="toggleEditReview()">Sửa</button>
    </c:if>
  </div>


  <div class="review-section-body">

    <%-- ── KHỐI HIỂN THỊ KẾT QUẢ ĐÃ ĐÁNH GIÁ ─── --%>
    <c:if test="${not empty review}">
      <div id="reviewDisplayBlock" class="review-done-card">
        <div class="review-done-stars">${review.starsDisplay}</div>
        <div>
          <div class="review-done-comment">
            <c:choose>
              <c:when test="${not empty review.comment}">${review.comment}</c:when>
              <c:otherwise><em style="color:var(--warm-gray);">Không có nhận xét.</em></c:otherwise>
            </c:choose>
          </div>
          <div class="review-done-meta" style="display:flex; justify-content:space-between; align-items:center;">
            <div>
              Đã đánh giá lúc ${review.formattedCreatedAt}
              <c:if test="${review.isPublic}">
                <span class="review-public-badge">Hiển thị công khai</span>
              </c:if>
              <c:if test="${!review.isPublic}">
                <span class="review-public-badge" style="background:var(--sand);color:var(--warm-gray);border-color:var(--border);">Riêng tư</span>
              </c:if>
            </div>
          </div>
        </div>
      </div>
    </c:if>

    <%-- ── KHỐI FORM ─── --%>
    <div id="reviewFormBlock" style="${not empty review ? 'display:none;' : ''}">
      <p style="font-size:14px;color:var(--text-mid);margin-bottom:18px;">
        <c:choose>
            <c:when test="${not empty review}">Vui lòng cập nhật lại thông tin đánh giá của bạn.</c:when>
            <c:otherwise>Lịch khám đã hoàn thành. Hãy dành 30 giây để chia sẻ trải nghiệm của bạn!</c:otherwise>
        </c:choose>
      </p>

      <form action="${ctx}/reviews/submit" method="post" id="reviewForm">
        <input type="hidden" name="appointmentId" value="${appt.appointmentID}">
        <%-- Lấy rating cũ nếu có, nếu không thì để trống --%>
        <input type="hidden" name="rating" id="ratingInput" value="${not empty review ? review.rating : ''}">

        <%-- Star picker --%>
        <div style="display:flex;align-items:center;margin-bottom:18px;gap:4px;">
          <div class="star-picker" id="starPicker">
            <button type="button" class="star-btn" data-val="1">★</button>
            <button type="button" class="star-btn" data-val="2">★</button>
            <button type="button" class="star-btn" data-val="3">★</button>
            <button type="button" class="star-btn" data-val="4">★</button>
            <button type="button" class="star-btn" data-val="5">★</button>
          </div>
          <span class="star-label" id="starLabel">Chọn sao</span>
        </div>

        <%-- Comment --%>
        <textarea class="review-textarea" name="comment"
                  placeholder="Nhận xét về dịch vụ, bác sĩ, thái độ phục vụ... (không bắt buộc)"
                  maxlength="1000">${not empty review ? review.comment : ''}</textarea>

        <%-- Public consent --%>
        <div class="review-consent">
          <input type="checkbox" id="isPublic" name="isPublic" ${review.isPublic ? 'checked' : ''}>
          <label for="isPublic">
            <strong>Cho phép hiển thị đánh giá này công khai</strong> trên trang cộng đồng.
            Chúng tôi cam kết <strong>không hiển thị</strong> tên, thông tin cá nhân của bạn.
          </label>
        </div>

        <div style="display:flex; gap:10px;">
          <button type="submit" class="btn-review-submit" id="btnReviewSubmit" ${empty review ? 'disabled' : ''}>
            ${not empty review ? 'Cập nhật đánh giá' : 'Gửi đánh giá'}
          </button>
          <c:if test="${not empty review}">
             <button type="button" class="btn-review-cancel" onclick="toggleEditReview()">Hủy</button>
          </c:if>
        </div>
      </form>
    </div>

  </div>
</div>

<script>
// Hàm ẩn/hiện form edit
function toggleEditReview() {
    const displayBlock = document.getElementById('reviewDisplayBlock');
    const formBlock = document.getElementById('reviewFormBlock');
    if (displayBlock.style.display === 'none') {
        displayBlock.style.display = 'block';
        formBlock.style.display = 'none';
    } else {
        displayBlock.style.display = 'none';
        formBlock.style.display = 'block';
    }
}

// Logic khởi tạo sao
(function () {
  const picker   = document.getElementById('starPicker');
  if (!picker) return;
  const stars    = picker.querySelectorAll('.star-btn');
  const input    = document.getElementById('ratingInput');
  const label    = document.getElementById('starLabel');
  const submitBtn= document.getElementById('btnReviewSubmit');

  const LABELS = ['', 'Rất tệ', 'Tệ', 'Bình thường', 'Tốt', 'Tuyệt vời!'];

  // Nếu có sẵn input.value (trường hợp edit) --> parse nó ra số
  let selected = parseInt(input.value) || 0;

  function paint(upTo) {
    stars.forEach((s, i) => s.classList.toggle('active', i < upTo));
  }

  // Khởi tạo màu cho các sao nếu đang ở chế độ Edit
  if(selected > 0) {
      paint(selected);
      label.textContent = LABELS[selected];
  }

  stars.forEach((btn, idx) => {
    btn.addEventListener('mouseover', () => paint(idx + 1));
    btn.addEventListener('mouseout',  () => paint(selected));
    btn.addEventListener('click', () => {
      selected       = idx + 1;
      input.value    = selected;
      label.textContent = LABELS[selected] || '';
      paint(selected);
      submitBtn.disabled = false;
    });
  });
})();
</script>
</c:if>