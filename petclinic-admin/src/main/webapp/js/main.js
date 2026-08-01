/* ── main.js – PetClinic Site-wide JS ──────────────────────────────────── */

const CTX = document.querySelector('meta[name="ctx"]')?.content || '';

// ─── Notification dropdown ──────────────────────────────────────────────────
let notifLoaded    = false;
let notifAllData   = [];   // full list (all types)
let notifActiveTab = 'ALL';

function toggleNotif() {
  const panel = document.getElementById('notifPanel');
  if (!panel) return;
  const isOpen = panel.classList.toggle('open');
  if (isOpen && !notifLoaded) loadNotifications();
  if (isOpen) {
    setTimeout(() => document.addEventListener('click', closeNotifOutside, { once: true }), 10);
  }
}

function closeNotifOutside(e) {
  const panel = document.getElementById('notifPanel');
  const btn   = document.getElementById('notifBtn');
  if (panel && !panel.contains(e.target) && btn && !btn.contains(e.target)) {
    panel.classList.remove('open');
  }
}

function loadNotifications() {
  fetch(CTX + '/notifications/api?limit=20')
    .then(r => r.json())
    .then(data => {
      notifLoaded  = true;
      notifAllData = data;
      renderDropdown('ALL');
    })
    .catch(() => {
      const el = document.getElementById('notifList');
      if (el) el.innerHTML = '<div class="notif-dropdown-empty">Không thể tải thông báo.</div>';
    });
}

function filterDropdown(tab, btn) {
  notifActiveTab = tab;
  document.querySelectorAll('.notif-pill').forEach(p => p.classList.remove('active'));
  if (btn) btn.classList.add('active');
  renderDropdown(tab);
}

function renderDropdown(tab) {
  const el = document.getElementById('notifList');
  if (!el) return;

  const TYPE_GROUPS = {
    REMINDER:    t => t && t.startsWith('REMINDER'),
    PAYMENT:     t => t && (t.startsWith('PAYMENT') || t === 'BOOKING_CONFIRMED' || t === 'BOOKING_CANCELLED'),
    EXAM_RESULT: t => t && (t === 'EXAM_RESULT' || t === 'VACCINE_DUE'),
    CARE_TIP:    t => t && (t === 'CARE_TIP' || t === 'SUPPORT'),
  };

  const items = tab === 'ALL' ? notifAllData
      : notifAllData.filter(n => (TYPE_GROUPS[tab] || (() => true))(n.type));

  if (!items.length) {
    el.innerHTML = '<div class="notif-dropdown-empty">Không có thông báo.</div>';
    return;
  }

  el.innerHTML = items.slice(0, 15).map(n => {
    const tag    = n.actionUrl ? 'a' : 'div';
    const href   = n.actionUrl ? ` href="${CTX}${escHtml(n.actionUrl)}"` : '';
    const unread = !n.isRead ? ' unread' : '';
    const dot    = !n.isRead ? '<div class="notif-dropdown-dot"></div>' : '';
    const click  = n.actionUrl && !n.isRead
        ? ` onclick="markReadOnClick(${n.id})"` : '';
    return `<${tag}${href} class="notif-dropdown-item ${escHtml(n.typeColor)}${unread}"${click}>
      <div class="notif-dropdown-icon">${escHtml(n.icon || '🔔')}</div>
      <div class="notif-dropdown-body">
        <div class="notif-dropdown-title">${escHtml(n.title)}</div>
        <div class="notif-dropdown-text">${escHtml(n.body || '')}</div>
        <div class="notif-dropdown-time">${escHtml(n.relativeTime || '')}</div>
      </div>
      ${dot}
    </${tag}>`;
  }).join('');
}

function markReadOnClick(id) {
  fetch(CTX + '/notifications/mark-read', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' },
    body: 'id=' + id
  }).then(r => r.json()).then(d => updateBadge(d.unread)).catch(() => {});
  // Optimistically mark as read in local data
  notifAllData = notifAllData.map(n => n.id === id ? {...n, isRead: true} : n);
  renderDropdown(notifActiveTab);
}

function updateBadge(count) {
  const badge = document.querySelector('.notif-badge');
  if (count <= 0) { if (badge) badge.remove(); return; }
  if (badge) badge.textContent = count > 9 ? '9+' : count;
}

// Poll count every 60s to keep badge fresh
setInterval(() => {
  fetch(CTX + '/notifications/count', { headers: { 'Accept': 'application/json' } })
    .then(r => r.json())
    .then(d => updateBadge(d.unread))
    .catch(() => {});
}, 60_000);

// ─── Flash auto-dismiss ─────────────────────────────────────────────────────
document.querySelectorAll('.flash').forEach(el => {
  setTimeout(() => {
    el.style.transition = 'opacity .5s ease';
    el.style.opacity = '0';
    setTimeout(() => el.remove(), 500);
  }, 4000);
});

// ─── Helpers ────────────────────────────────────────────────────────────────
function initInlineUiIcons() {
  const icon = {
    check: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg>',
    warning: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 2 21h20L12 3z"/><path d="M12 9v4M12 17h.01"/></svg>',
    error: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/></svg>',
    user: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
    search: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>',
    calendar: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></svg>',
    inbox: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 4h16v16H4z"/><path d="M4 14h5l2 3h2l2-3h5"/></svg>',
    card: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 10h18M7 15h3"/></svg>',
    info: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/></svg>',
    bell: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/></svg>'
  };
  document.querySelectorAll('.alert-icon').forEach(el => {
    const alert = el.closest('.alert');
    el.innerHTML = alert && alert.classList.contains('alert-success') ? icon.check : alert && alert.classList.contains('alert-error') ? icon.error : icon.warning;
  });
  document.querySelectorAll('.input-icon').forEach(el => {
    const input = el.parentElement.querySelector('input');
    el.innerHTML = input && input.type === 'date' ? icon.calendar : input && /search|filter/i.test(`${input.name} ${input.id}`) ? icon.search : icon.user;
  });
  document.querySelectorAll('.empty-icon').forEach(el => { el.innerHTML = icon.inbox; });
  document.querySelectorAll('.pay-opt-icon').forEach(el => { el.innerHTML = icon.card; });
  document.querySelectorAll('.pay-qr-note-icon').forEach(el => { el.innerHTML = icon.info; });
  document.querySelectorAll('.result-icon--success').forEach(el => { el.innerHTML = icon.check; });
  document.querySelectorAll('.result-icon--pending').forEach(el => { el.innerHTML = icon.warning; });
  document.querySelectorAll('.notif-btn').forEach(el => { el.insertAdjacentHTML('afterbegin', icon.bell); });
}

initInlineUiIcons();

function escHtml(s) {
  if (!s) return '';
  return String(s)
    .replace(/&/g,'&amp;').replace(/</g,'&lt;')
    .replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}
