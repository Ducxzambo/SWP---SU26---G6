(function () {
    const root = document.getElementById('apptCalendarRoot');
    if (!root) return;

    const CTX = window.APP_CTX || '';
    const RAW_EVENTS = window.APPT_CALENDAR_DATA || [];

    const STATUS_CLASS = {
        Pending: 'db-status-pending', Confirmed: 'db-status-confirmed',
        InProgress: 'db-status-inprogress', Done: 'db-status-done',
        Cancelled: 'db-status-cancelled', NoShow: 'db-status-noshow'
    };

    const eventsByDate = {};
    RAW_EVENTS.forEach(function (e) {
        (eventsByDate[e.date] = eventsByDate[e.date] || []).push(e);
    });
    Object.values(eventsByDate).forEach(function (list) {
        list.sort(function (a, b) { return (a.startTime || '').localeCompare(b.startTime || ''); });
    });

    const DOW = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
    const MONTH_NAMES = ['Tháng 1','Tháng 2','Tháng 3','Tháng 4','Tháng 5','Tháng 6',
        'Tháng 7','Tháng 8','Tháng 9','Tháng 10','Tháng 11','Tháng 12'];

    let mode = 'month';
    let cursor = new Date();
    cursor.setHours(0, 0, 0, 0);

    function pad(n) { return n < 10 ? '0' + n : '' + n; }
    function fmtKey(d) { return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()); }

    function render() {
        root.innerHTML =
            '<div class="cal-toolbar">' +
            '<div class="cal-nav">' +
            '<button type="button" class="cal-btn" id="calPrev">‹</button>' +
            '<button type="button" class="cal-btn cal-today" id="calToday">Hôm nay</button>' +
            '<button type="button" class="cal-btn" id="calNext">›</button>' +
            '<span class="cal-label" id="calLabel"></span>' +
            '</div>' +
            '<div class="cal-view-switch">' +
            '<button type="button" class="cal-mode-btn" data-mode="month">Tháng</button>' +
            '<button type="button" class="cal-mode-btn" data-mode="week">Tuần</button>' +
            '<button type="button" class="cal-mode-btn" data-mode="day">Ngày</button>' +
            '</div>' +
            '</div>' +
            '<div id="calBody" class="cal-body"></div>';

        root.querySelectorAll('.cal-mode-btn').forEach(function (btn) {
            btn.classList.toggle('active', btn.dataset.mode === mode);
            btn.onclick = function () { mode = btn.dataset.mode; render(); };
        });
        document.getElementById('calPrev').onclick = function () { step(-1); };
        document.getElementById('calNext').onclick = function () { step(1); };
        document.getElementById('calToday').onclick = function () {
            cursor = new Date(); cursor.setHours(0, 0, 0, 0); render();
        };

        if (mode === 'month') renderMonth();
        else if (mode === 'week') renderWeek();
        else renderDay();
    }

    function step(dir) {
        if (mode === 'month') cursor.setMonth(cursor.getMonth() + dir);
        else if (mode === 'week') cursor.setDate(cursor.getDate() + dir * 7);
        else cursor.setDate(cursor.getDate() + dir);
        render();
    }

    function renderMonth() {
        document.getElementById('calLabel').textContent = MONTH_NAMES[cursor.getMonth()] + ' ' + cursor.getFullYear();
        const body = document.getElementById('calBody');

        const first = new Date(cursor.getFullYear(), cursor.getMonth(), 1);
        const gridStart = new Date(first);
        gridStart.setDate(first.getDate() - first.getDay());

        let html = '<div class="cal-grid cal-grid-head">';
        DOW.forEach(function (d) { html += '<div class="cal-grid-dow">' + d + '</div>'; });
        html += '</div><div class="cal-grid cal-grid-body">';

        const todayKey = fmtKey(new Date());
        for (let i = 0; i < 42; i++) {
            const d = new Date(gridStart);
            d.setDate(gridStart.getDate() + i);
            const key = fmtKey(d);
            const inMonth = d.getMonth() === cursor.getMonth();
            const evts = eventsByDate[key] || [];
            html += '<div class="cal-cell' + (inMonth ? '' : ' cal-cell-out') + (key === todayKey ? ' cal-cell-today' : '') + '" data-date="' + key + '">';
            html += '<div class="cal-cell-day">' + d.getDate() + '</div>';
            evts.slice(0, 3).forEach(function (e) {
                html += '<a href="' + CTX + '/appointments/detail?id=' + e.id + '" class="cal-chip ' + (STATUS_CLASS[e.status] || '') + '">' +
                    (e.startTime || '') + ' ' + escHtml(e.service) + '</a>';
            });
            if (evts.length > 3) html += '<div class="cal-more">+' + (evts.length - 3) + ' khác</div>';
            html += '</div>';
        }
        html += '</div>';
        body.innerHTML = html;

        body.querySelectorAll('.cal-cell').forEach(function (cell) {
            cell.addEventListener('click', function (ev) {
                if (ev.target.closest('.cal-chip')) return;
                const parts = cell.dataset.date.split('-').map(Number);
                cursor = new Date(parts[0], parts[1] - 1, parts[2]);
                mode = 'day';
                render();
            });
        });
    }

    function renderWeek() {
        const weekStart = new Date(cursor);
        weekStart.setDate(cursor.getDate() - cursor.getDay());
        const weekEnd = new Date(weekStart); weekEnd.setDate(weekStart.getDate() + 6);

        document.getElementById('calLabel').textContent =
            pad(weekStart.getDate()) + '/' + pad(weekStart.getMonth() + 1) + ' – ' +
            pad(weekEnd.getDate()) + '/' + pad(weekEnd.getMonth() + 1) + '/' + weekEnd.getFullYear();

        const body = document.getElementById('calBody');
        let html = '<div class="cal-week-grid">';
        const todayKey = fmtKey(new Date());
        for (let i = 0; i < 7; i++) {
            const d = new Date(weekStart); d.setDate(weekStart.getDate() + i);
            const key = fmtKey(d);
            const evts = eventsByDate[key] || [];
            html += '<div class="cal-week-col' + (key === todayKey ? ' cal-cell-today' : '') + '">';
            html += '<div class="cal-week-col-head">' + DOW[d.getDay()] + '<br><span>' + pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '</span></div>';
            if (!evts.length) html += '<div class="cal-week-empty">—</div>';
            evts.forEach(function (e) {
                html += '<a href="' + CTX + '/appointments/detail?id=' + e.id + '" class="cal-week-item ' + (STATUS_CLASS[e.status] || '') + '">' +
                    '<strong>' + (e.startTime || '') + '</strong><br>' + escHtml(e.service) +
                    (e.pet ? '<br><span class="cal-week-pet">' + escHtml(e.pet) + '</span>' : '') + '</a>';
            });
            html += '</div>';
        }
        html += '</div>';
        body.innerHTML = html;
    }

    function renderDay() {
        const key = fmtKey(cursor);
        document.getElementById('calLabel').textContent =
            DOW[cursor.getDay()] + ', ' + pad(cursor.getDate()) + '/' + pad(cursor.getMonth() + 1) + '/' + cursor.getFullYear();
        const evts = eventsByDate[key] || [];
        const body = document.getElementById('calBody');
        if (!evts.length) {
            body.innerHTML = '<div class="cal-day-empty">Không có lịch hẹn nào trong ngày này.</div>';
            return;
        }
        let html = '<div class="cal-day-list">';
        evts.forEach(function (e) {
            html += '<a href="' + CTX + '/appointments/detail?id=' + e.id + '" class="cal-day-item">' +
                '<div class="cal-day-time">' + (e.startTime || '') + ' – ' + (e.endTime || '') + '</div>' +
                '<div class="cal-day-body"><strong>' + escHtml(e.service) + '</strong>' +
                (e.pet ? '<span>' + escHtml(e.pet) + '</span>' : '') + '</div>' +
                '<span class="db-recent-status ' + (STATUS_CLASS[e.status] || '') + '">' + escHtml(e.status) + '</span>' +
                '</a>';
        });
        html += '</div>';
        body.innerHTML = html;
    }

    function escHtml(s) {
        if (!s) return '';
        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    render();
})();