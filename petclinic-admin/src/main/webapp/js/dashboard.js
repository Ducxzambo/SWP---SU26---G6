/* dashboard.js – PetClinic Staff Dashboard */

// ── Flash message auto-hide ───────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', function () {

    var icons = {
        check: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg>',
        warning: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 2 21h20L12 3z"/><path d="M12 9v4M12 17h.01"/></svg>',
        error: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/></svg>',
        user: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
        search: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>',
        calendar: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></svg>',
        inbox: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 4h16v16H4z"/><path d="M4 14h5l2 3h2l2-3h5"/></svg>',
        card: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 10h18M7 15h3"/></svg>',
        info: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/></svg>'
    };
    document.querySelectorAll('.alert-icon').forEach(function (el) {
        var alert = el.closest('.alert');
        el.innerHTML = alert && alert.classList.contains('alert-success') ? icons.check : alert && alert.classList.contains('alert-error') ? icons.error : icons.warning;
    });
    document.querySelectorAll('.input-icon').forEach(function (el) {
        var input = el.parentElement.querySelector('input');
        el.innerHTML = input && input.type === 'date' ? icons.calendar : input && /search|filter/i.test((input.name || '') + ' ' + (input.id || '')) ? icons.search : icons.user;
    });
    document.querySelectorAll('.empty-icon').forEach(function (el) { el.innerHTML = icons.inbox; });
    document.querySelectorAll('.pay-opt-icon').forEach(function (el) { el.innerHTML = icons.card; });
    document.querySelectorAll('.pay-qr-note-icon').forEach(function (el) { el.innerHTML = icons.info; });
    document.querySelectorAll('.result-icon--success').forEach(function (el) { el.innerHTML = icons.check; });
    document.querySelectorAll('.result-icon--pending').forEach(function (el) { el.innerHTML = icons.warning; });

    // Auto-dismiss alerts after 4 seconds
    document.querySelectorAll('.alert').forEach(function (el) {
        setTimeout(function () {
            el.style.transition = 'opacity 0.5s ease';
            el.style.opacity = '0';
            setTimeout(function () {
                el.remove();
            }, 500);
        }, 4000);
    });

    // Active nav highlight (in case server doesn't set it)
    var path = window.location.pathname;
    document.querySelectorAll('.nav-item').forEach(function (a) {
        if (a.getAttribute('href') && path.includes(a.getAttribute('href').split('?')[0])) {
            a.classList.add('active');
        }
    });

});

// ── Confirm helper ────────────────────────────────────────────────────────
function confirmAction(msg) {
    return confirm(msg || 'Bạn có chắc muốn thực hiện thao tác này?');
}
