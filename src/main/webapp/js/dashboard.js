/* dashboard.js – PetClinic Staff Dashboard */

// ── Flash message auto-hide ───────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', function () {

    // Auto-dismiss alerts after 4 seconds
    document.querySelectorAll('.alert').forEach(function (el) {
        setTimeout(function () {
            el.style.transition = 'opacity 0.5s ease';
            el.style.opacity = '0';
            setTimeout(function () { el.remove(); }, 500);
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