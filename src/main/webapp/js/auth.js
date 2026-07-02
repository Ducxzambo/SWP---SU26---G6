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
  btn.textContent = isText ? '👁' : '🙈';
}

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
