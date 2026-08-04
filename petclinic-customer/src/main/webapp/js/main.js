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

// ─── Profile dropdown ───────────────────────────────────────────────────────
function toggleProfileMenu() {
  const panel = document.getElementById('navProfilePanel');
  if (!panel) return;
  const isOpen = panel.classList.toggle('open');
  if (isOpen) {
    setTimeout(() => document.addEventListener('click', closeProfileMenuOutside, { once: true }), 10);
  }
}

function closeProfileMenuOutside(e) {
  const panel = document.getElementById('navProfilePanel');
  const btn   = document.getElementById('navProfileBtn');
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
function escHtml(s) {
  if (!s) return '';
  return String(s)
    .replace(/&/g,'&amp;').replace(/</g,'&lt;')
    .replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}
