/* auth.js – PetClinic Authentication Pages */

// ── Password strength meter ──────────────────────────────────────────────
function initPasswordStrength(inputId, barId, labelId, reqListId) {
  const input  = document.getElementById(inputId);
  const bar    = document.getElementById(barId);
  const label  = document.getElementById(labelId);
  const list   = reqListId ? document.getElementById(reqListId) : null;
  if (!input || !bar) return;

  const rules = [
    { re: /.{6,}/,       text: '6+ ký tự',       id: 'req-len'  },
    { re: /[A-Z]/,       text: 'Chữ in hoa',      id: 'req-upper'},
    { re: /[a-z]/,       text: 'Chữ thường',      id: 'req-lower'},
    { re: /[0-9]/,       text: 'Chữ số',          id: 'req-digit'},
    { re: /[^A-Za-z0-9]/,text: 'Ký tự đặc biệt', id: 'req-spec' },
  ];

  const colors = ['#c0392b','#e67e22','#f1c40f','#2ecc71','#27ae60'];
  const labels = ['Rất yếu','Yếu','Trung bình','Mạnh','Rất mạnh'];

  input.addEventListener('input', () => {
    const val = input.value;
    let score = rules.filter(r => r.re.test(val)).length;

    bar.style.width     = (score / rules.length * 100) + '%';
    bar.style.background = colors[Math.max(0, score - 1)] || colors[0];
    if (label) label.textContent = val ? labels[Math.max(0, score - 1)] : '';

    if (list) {
      rules.forEach(r => {
        const li = list.querySelector('[data-req="' + r.id + '"]');
        if (li) li.classList.toggle('met', r.re.test(val));
      });
    }
  });
}

// ── Toggle password visibility ───────────────────────────────────────────
function togglePassword(inputId, btnId) {
  const input = document.getElementById(inputId);
  const btn   = document.getElementById(btnId);
  if (!input || !btn) return;
  const isText = input.type === 'text';
  input.type   = isText ? 'password' : 'text';
  btn.setAttribute('aria-label', isText ? 'Hiện mật khẩu' : 'Ẩn mật khẩu');
  btn.setAttribute('title', isText ? 'Hiện mật khẩu' : 'Ẩn mật khẩu');
  btn.innerHTML = isText
    ? '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>'
    : '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 3l18 18"/><path d="M10.6 5.1A10.9 10.9 0 0 1 12 5c7 0 11 7 11 7a19.1 19.1 0 0 1-3.1 3.8M6.2 6.2C3.1 8.2 1 12 1 12s4 7 11 7c1.4 0 2.7-.3 3.8-.8"/><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2"/></svg>';
}

function initInlineAuthIcons() {
  const icons = {
    paw: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="7" cy="8" r="2.2"/><circle cx="12" cy="5.5" r="2.2"/><circle cx="17" cy="8" r="2.2"/><path d="M12 11c-3.2 0-5.5 2.4-5.5 5 0 2 1.5 3.5 3.4 2.7l2.1-.9 2.1.9c1.9.8 3.4-.7 3.4-2.7 0-2.6-2.3-5-5.5-5z"/></svg>',
    user: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
    mail: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></svg>',
    lock: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></svg>',
    check: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg>',
    warning: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 2 21h20L12 3z"/><path d="M12 9v4M12 17h.01"/></svg>',
    error: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/></svg>',
    eye: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>'
  };
  document.querySelectorAll('.paw').forEach(el => { el.innerHTML = icons.paw; });
  document.querySelectorAll('.alert-icon').forEach(el => {
    const parent = el.closest('.alert');
    el.innerHTML = parent && parent.classList.contains('alert-success') ? icons.check : parent && parent.classList.contains('alert-error') ? icons.error : icons.warning;
  });
  document.querySelectorAll('.input-icon').forEach(el => {
    const input = el.parentElement.querySelector('input');
    const type = input ? input.type : '';
    const name = input ? `${input.name} ${input.id}`.toLowerCase() : '';
    el.innerHTML = type === 'password' ? icons.lock : type === 'email' || name.includes('email') ? icons.mail : icons.user;
  });
  document.querySelectorAll('.toggle-pwd').forEach(el => { el.innerHTML = icons.eye; el.setAttribute('aria-label', 'Hiện mật khẩu'); });
}

if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initInlineAuthIcons);
else initInlineAuthIcons();

// ── OTP input: auto-advance between digit boxes ──────────────────────────
function initOtpInputs(groupId, hiddenId, onChange) {
  const group  = document.getElementById(groupId);
  const hidden = document.getElementById(hiddenId);
  if (!group || !hidden) return;

  const digits = group.querySelectorAll('.otp-digit');

  digits.forEach((input, idx) => {
    input.setAttribute('maxlength', 1);
    input.setAttribute('inputmode', 'numeric');
    input.setAttribute('pattern',   '[0-9]');

    input.addEventListener('input', e => {
      const val = e.target.value.replace(/\D/g, '');
      e.target.value = val.slice(-1);
      if (val && idx < digits.length - 1) digits[idx + 1].focus();
      syncHidden();
    });

    input.addEventListener('keydown', e => {
      if (e.key === 'Backspace' && !input.value && idx > 0) {
        digits[idx - 1].focus();
        digits[idx - 1].value = '';
        syncHidden();
      }
    });

    input.addEventListener('paste', e => {
      e.preventDefault();
      const text = (e.clipboardData || window.clipboardData).getData('text').replace(/\D/g, '');
      text.split('').forEach((ch, i) => {
        if (digits[i]) digits[i].value = ch;
      });
      const next = Math.min(text.length, digits.length - 1);
      digits[next].focus();
      syncHidden();
    });
  });

  function syncHidden() {
    hidden.value = Array.from(digits).map(d => d.value).join('');
    // Thông báo cho caller biết giá trị đã thay đổi
    if (typeof onChange === 'function') onChange(hidden.value);
  }
}

// ── Resend OTP countdown ─────────────────────────────────────────────────
function initResendCountdown(timerId, linkId, seconds) {
  const timerEl = document.getElementById(timerId);
  const linkEl  = document.getElementById(linkId);
  if (!timerEl || !linkEl) return;

  linkEl.style.display = 'none';
  let remaining = seconds;

  const interval = setInterval(() => {
    remaining--;
    timerEl.textContent = remaining + 's';
    if (remaining <= 0) {
      clearInterval(interval);
      timerEl.closest('.resend-wrap').innerHTML =
        '<a id="' + linkId + '" onclick="resendOtp(this)">Gửi lại mã</a>';
    }
  }, 1000);
}

// ── Resend OTP via fetch ─────────────────────────────────────────────────
async function resendOtp(link, url) {
  link.textContent = 'Đang gửi…';
  link.style.pointerEvents = 'none';
  try {
    const res  = await fetch(url || link.dataset.url, { method: 'POST' });
    const json = await res.json();
    if (json.ok) {
      link.closest('.resend-wrap').innerHTML =
        '✓ Đã gửi lại. Hãy kiểm tra hộp thư / điện thoại của bạn.';
    } else {
      link.textContent = 'Thử lại';
      link.style.pointerEvents = '';
    }
  } catch {
    link.textContent = 'Lỗi kết nối – thử lại';
    link.style.pointerEvents = '';
  }
}

// ── Phone formatting (Vietnamese) ────────────────────────────────────────
function formatVnPhone(input) {
  input.addEventListener('input', () => {
    let v = input.value.replace(/\D/g, '');
    if (v.startsWith('0') && v.length > 10) v = v.slice(0, 10);
    input.value = v;
  });
}
