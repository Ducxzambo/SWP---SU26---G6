/* ── main.js – PetClinic Site-wide JS ──────────────────────────────────── */

// ── Notification panel ────────────────────────────────────────────────────
let notifLoaded = false;

function toggleNotif() {
  const panel = document.getElementById('notifPanel');
  if (!panel) return;

  const isOpen = panel.classList.toggle('open');
  if (isOpen && !notifLoaded) {
    loadNotifications();
  }

  // Close when clicking outside
  if (isOpen) {
    setTimeout(() => {
      document.addEventListener('click', closeNotifOnOutside, { once: true });
    }, 10);
  }
}

function closeNotifOnOutside(e) {
  const panel = document.getElementById('notifPanel');
  const btn   = document.getElementById('notifBtn');
  if (panel && !panel.contains(e.target) && btn && !btn.contains(e.target)) {
    panel.classList.remove('open');
  }
}

function loadNotifications() {
  const ctx  = document.querySelector('meta[name="ctx"]')?.content || '';
  const list = document.getElementById('notifList');
  if (!list) return;

  fetch(ctx + '/notifications/api?limit=10')
    .then(r => r.json())
    .then(data => {
      notifLoaded = true;
      if (!data || data.length === 0) {
        list.innerHTML = '<div class="notif-empty">Không có thông báo nào.</div>';
        return;
      }
      list.innerHTML = data.map(n => `
        <div class="notif-item ${n.read ? '' : 'unread'}">
          <div class="notif-title">${escHtml(n.title)}</div>
          <div class="notif-body">${escHtml(n.body)}</div>
          <div class="notif-time">${formatRelativeTime(n.createdAt)}</div>
        </div>`).join('');
    })
    .catch(() => {
      list.innerHTML = '<div class="notif-empty">Không thể tải thông báo.</div>';
    });
}

function markAllRead(e) {
  e.preventDefault();
  const ctx = document.querySelector('meta[name="ctx"]')?.content || '';
  fetch(ctx + '/notifications/mark-read', { method: 'POST' })
    .then(() => {
      // Remove all unread badges
      document.querySelectorAll('.notif-badge').forEach(el => el.remove());
      document.querySelectorAll('.notif-item.unread').forEach(el => el.classList.remove('unread'));
      // Reset nav badge
      const span = document.querySelector('.nav-link span[style*="background:var(--green-400)"]');
      if (span) span.remove();
      notifLoaded = false; // force reload next open
    });
}

// ── Flash auto-dismiss ────────────────────────────────────────────────────
document.querySelectorAll('.flash').forEach(el => {
  setTimeout(() => {
    el.style.transition = 'opacity .5s ease';
    el.style.opacity    = '0';
    setTimeout(() => el.remove(), 500);
  }, 4000);
});

// ── Helpers ───────────────────────────────────────────────────────────────
function escHtml(str) {
  if (!str) return '';
  return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
            .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

function formatRelativeTime(isoStr) {
  if (!isoStr) return '';
  try {
    const diff = Date.now() - new Date(isoStr).getTime();
    const min  = Math.floor(diff / 60000);
    const hr   = Math.floor(diff / 3600000);
    const day  = Math.floor(diff / 86400000);
    if (min  <  1) return 'Vừa xong';
    if (min  < 60) return min  + ' phút trước';
    if (hr   < 24) return hr   + ' giờ trước';
    if (day  <  7) return day  + ' ngày trước';
    return new Date(isoStr).toLocaleDateString('vi-VN');
  } catch { return ''; }
}

// ── Mobile hamburger (stub – expand per project need) ────────────────────
const hamburger = document.querySelector('.nav-hamburger');
if (hamburger) {
  hamburger.addEventListener('click', () => {
    const links = document.querySelector('.nav-links');
    if (links) links.style.display = links.style.display === 'flex' ? 'none' : 'flex';
  });
}
