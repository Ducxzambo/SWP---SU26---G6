/**
 * confirm.js
 * Format lại các slot-key thô "yyyy-MM-dd|HH:mm" thành chuỗi hiển thị
 * kiểu "Thứ X, dd/MM/yyyy lúc HH:mm – HH:mm" trên trang xác nhận đặt lịch.
 */
(function () {
  const DAYS_VN = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

  document.querySelectorAll('[id^="slot-"]').forEach(td => {
    const raw = td.textContent.trim();
    if (!raw.includes('|')) return;

    const [datePart, timePart] = raw.split('|');
    const d = new Date(datePart + 'T00:00:00');

    // Sửa lỗi: nếu datePart không hợp lệ, Date sẽ là Invalid Date -> bỏ qua để
    // tránh hiển thị "NaN/NaN/NaN" cho người dùng.
    if (isNaN(d.getTime())) return;

    const dd  = d.getDate().toString().padStart(2, '0');
    const mm  = (d.getMonth() + 1).toString().padStart(2, '0');
    const dow = DAYS_VN[d.getDay()];

    const timeParts = timePart.split(':').map(Number);
    const hh  = timeParts[0] || 0;
    const min = timeParts[1] || 0;

    const endMin = hh * 60 + min + 120;
    const endH = Math.floor(endMin / 60).toString().padStart(2, '0');
    const endM = (endMin % 60).toString().padStart(2, '0');

    td.textContent = dow + ' ' + dd + '/' + mm + '/' + d.getFullYear()
      + ' lúc ' + timePart + ' – ' + endH + ':' + endM;
  });
})();
