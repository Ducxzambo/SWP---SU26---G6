/* Shared customer UI behavior. */

function toggleProfileMenu() {
  const panel = document.getElementById('navProfilePanel');
  if (!panel) return;
  const isOpen = panel.classList.toggle('open');
  if (isOpen) setTimeout(() => document.addEventListener('click', closeProfileMenuOutside, { once: true }), 10);
}

function closeProfileMenuOutside(event) {
  const panel = document.getElementById('navProfilePanel');
  const button = document.getElementById('navProfileBtn');
  if (panel && button && !panel.contains(event.target) && !button.contains(event.target)) panel.classList.remove('open');
}

function initInlineUiIcons() {
  const icon = {
    check: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg>',
    warning: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 2 21h20L12 3z"/><path d="M12 9v4M12 17h.01"/></svg>',
    error: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/></svg>',
    user: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
    search: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>',
    calendar: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></svg>',
    inbox: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 4h16v16H4z"/><path d="M4 14h5l2 3h2l2-3h5"/></svg>',
    paw: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="7" cy="8" r="2"/><circle cx="12" cy="5" r="2"/><circle cx="17" cy="8" r="2"/><path d="M12 11c-3 0-5 2-5 5 0 2 1 3 3 2l2-1 2 1c2 1 3 0 3-2 0-3-2-5-5-5z"/></svg>',
    lock: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></svg>',
    card: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 10h18M7 15h3"/></svg>',
    info: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/></svg>'
  };
  document.querySelectorAll('.alert-icon').forEach(el => {
    const alert = el.closest('.alert');
    el.innerHTML = alert && alert.classList.contains('alert-success') ? icon.check : alert && alert.classList.contains('alert-error') ? icon.error : icon.warning;
  });
  document.querySelectorAll('.input-icon').forEach(el => {
    const input = el.parentElement.querySelector('input');
    el.innerHTML = input && input.type === 'date' ? icon.calendar : input && /search|filter/i.test(`${input.name} ${input.id}`) ? icon.search : icon.user;
  });
  document.querySelectorAll('.empty-icon, .appt-empty-icon, .review-empty-icon').forEach(el => { el.innerHTML = icon.inbox; });
  document.querySelectorAll('.pets-empty-icon, .db-quick-icon').forEach(el => { el.innerHTML = icon.paw; });
  document.querySelectorAll('.db-recent-avatar').forEach(el => { el.innerHTML = icon.paw; });
  document.querySelectorAll('.account-badge-locked').forEach(el => { el.insertAdjacentHTML('afterbegin', icon.lock + ' '); });
  document.querySelectorAll('.pay-opt-icon').forEach(el => { el.innerHTML = icon.card; });
  document.querySelectorAll('.pay-qr-note-icon, .modal-icon').forEach(el => { el.innerHTML = icon.info; });
  document.querySelectorAll('.result-icon--success').forEach(el => { el.innerHTML = icon.check; });
  document.querySelectorAll('.result-icon--pending').forEach(el => { el.innerHTML = icon.warning; });
  document.querySelectorAll('.pet-search-icon').forEach(el => { el.innerHTML = icon.search; });
}

initInlineUiIcons();
document.querySelectorAll('.flash').forEach(el => setTimeout(() => {
  el.style.transition = 'opacity .5s ease';
  el.style.opacity = '0';
  setTimeout(() => el.remove(), 500);
}, 4000));
