/**
 * payment.js
 * Gộp toàn bộ logic JS cho cụm trang "Thanh toán":
 *   1) payment.jsp        — chọn hình thức thanh toán (toàn bộ / đặt cọc)
 *   2) payment-result.jsp — poll trạng thái lịch hẹn khi đang chờ xác nhận
 *
 * Mỗi phần được bọc trong IIFE riêng và tự kiểm tra phần tử DOM đặc trưng của
 * trang mình trước khi chạy, nên có thể an toàn dùng chung 1 file cho cả 2 trang.
 *
 * Phần (2) yêu cầu JSP khai báo trước khi load file này (xem payment-result.jsp):
 *   window.APP_CTX                  (context path)
 *   window.PAYMENT_RESULT_APPT_ID   (string, có thể rỗng)
 */

/* ============================================================
 * 1) PAYMENT — chọn hình thức thanh toán (toàn bộ / đặt cọc)
 * ============================================================ */
(function () {
  const payForm = document.getElementById('payForm');
  if (!payForm) return; // không phải trang payment.jsp

  let selectedPayType = '';

  function selectPay(type, el) {
    selectedPayType = type;

    const payTypeInput = document.getElementById('payTypeInput');
    if (payTypeInput) payTypeInput.value = type;

    // Visual
    document.querySelectorAll('.pay-option').forEach(o => o.classList.remove('active'));
    document.querySelectorAll('.pay-opt-check').forEach(c => c.classList.remove('active'));
    el.classList.add('active');

    const checkEl = document.getElementById('check' + (type === 'full' ? 'Full' : 'Partial'));
    if (checkEl) checkEl.classList.add('active');

    // Update button label
    const isFull = type === 'full';
    const amtEl = document.getElementById(isFull ? 'amtFull' : 'amtPartial');
    const amount = amtEl ? amtEl.textContent.split('(')[0].trim() : '';

    const btnPay = document.getElementById('btnPay');
    if (btnPay) {
      btnPay.textContent = 'Thanh toán ' + amount + ' →';
      btnPay.disabled = false;
    }
  }

  // Expose for inline onclick in payment.jsp
  window.selectPay = selectPay;

  payForm.addEventListener('submit', function (e) {
    if (!selectedPayType) {
      e.preventDefault();
      alert('Vui lòng chọn hình thức thanh toán.');
    }
  });
})();

/* ============================================================
 * 2) PAYMENT RESULT — poll trạng thái lịch hẹn khi đang chờ
 * ============================================================ */
(function () {
  const apptId = window.PAYMENT_RESULT_APPT_ID || '';
  if (!apptId) return; // không có lịch hẹn để theo dõi (trang success, hoặc thiếu appt)

  const ctx = window.APP_CTX || '';

  let checks = 0;
  const MAX_CHECKS = 10;
  const POLL_INTERVAL_MS = 3000;

  const timer = setInterval(async () => {
    checks++;
    if (checks > MAX_CHECKS) {
      clearInterval(timer);
      return;
    }
    try {
      const r = await fetch(ctx + '/appointments/status?id=' + encodeURIComponent(apptId));
      if (!r.ok) return;
      const d = await r.json();
      if (d.status === 'Confirmed') {
        clearInterval(timer);
        location.reload();
      }
    } catch (e) {
      // Bỏ qua lỗi mạng tạm thời, sẽ tự thử lại ở lần poll kế tiếp.
    }
  }, POLL_INTERVAL_MS);
})();
