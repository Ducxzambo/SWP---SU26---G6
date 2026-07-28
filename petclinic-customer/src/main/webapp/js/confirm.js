/**
 * Format slot keys from "yyyy-MM-dd|HH:mm" to a customer-facing label.
 */
(function () {
  const DAYS_VN = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

  document.querySelectorAll('[id^="slot-"]').forEach(cell => {
    const raw = cell.textContent.trim();
    if (!raw.includes('|')) return;

    const [datePart, timePart] = raw.split('|');
    const date = new Date(datePart + 'T00:00:00');
    if (Number.isNaN(date.getTime())) return;

    const dd = String(date.getDate()).padStart(2, '0');
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dow = DAYS_VN[date.getDay()];

    const [hhRaw, minRaw] = timePart.split(':').map(Number);
    const startMinutes = (hhRaw || 0) * 60 + (minRaw || 0);
    const endMinutes = startMinutes + 120;
    const endH = String(Math.floor(endMinutes / 60)).padStart(2, '0');
    const endM = String(endMinutes % 60).padStart(2, '0');

    cell.textContent = dow + ' ' + dd + '/' + mm + '/' + date.getFullYear()
      + ' lúc ' + timePart + ' - ' + endH + ':' + endM;
  });
})();
