/**
 * refund.js — "Yêu cầu hoàn tiền" panel trong Cancel modal
 * (appointment-detail.jsp, chỉ render khi appointment đang Confirmed).
 *
 * Cả 2 lựa chọn (quét QR / nhập thủ công) đều ghi vào CÙNG 3 field:
 *   #refundBankSelect (name=bankCode), #refundAccountNumber (name=accountNumber),
 *   #refundAccountName (name=accountName) — quét QR chỉ là một cách tự động
 *   điền nhanh cho đúng 3 field thủ công đó, không phải 1 form riêng.
 *
 * Phụ thuộc: jsQR (biến global window.jsQR, load qua CDN trong JSP) để đọc
 * mã QR từ ảnh; gọi trực tiếp API công khai của VietQR để đổ danh sách
 * ngân hàng vào <select>.
 */
(function () {
  var refundPanel = document.getElementById('refundPanel');
  if (!refundPanel) return; // trang không có checkbox "Yêu cầu hoàn tiền" (appt không Confirmed)

  var BANKS_API_URL = 'https://api.vietqr.io/v2/banks';

  var bankListCache   = null;
  var bankListLoading = false;

  /* ============================================================
   * 1) DANH SÁCH NGÂN HÀNG (Option 2 — dropdown)
   * ============================================================ */
  function ensureBankListLoaded() {
    if (bankListCache || bankListLoading) return;
    bankListLoading = true;

    var select = document.getElementById('refundBankSelect');
    if (select) {
      select.innerHTML = '';
      select.appendChild(makeOption('', 'Đang tải danh sách ngân hàng...'));
    }

    fetch(BANKS_API_URL)
      .then(function (r) { return r.json(); })
      .then(function (json) {
        var banks = (json && Array.isArray(json.data)) ? json.data : [];
        bankListCache = banks;
        renderBankOptions(banks);
      })
      .catch(function () {
        if (select) {
          select.innerHTML = '';
          select.appendChild(makeOption('', 'Không tải được danh sách ngân hàng — vui lòng thử lại'));
        }
      })
      .finally(function () { bankListLoading = false; });
  }

  function renderBankOptions(banks) {
    var select = document.getElementById('refundBankSelect');
    if (!select) return;
    select.innerHTML = '';
    select.appendChild(makeOption('', '-- Chọn ngân hàng --'));
    banks.forEach(function (b) {
      var opt = makeOption(b.code, (b.shortName || b.code) + ' — ' + b.name);
      opt.dataset.bin = b.bin || '';
      select.appendChild(opt);
    });
  }

  function makeOption(value, label) {
    var opt = document.createElement('option');
    opt.value = value;
    opt.textContent = label;
    return opt;
  }

  /* ============================================================
   * 2) TOGGLE PANEL + CHUYỂN TAB Option 1 / Option 2
   * ============================================================ */
  function toggleRefundPanel(checked) {
    refundPanel.classList.toggle('open', !!checked);
    if (checked) ensureBankListLoaded();
    updateConfirmButtonState();
  }

  function switchRefundMethod(method) {
    document.querySelectorAll('.refund-method-btn').forEach(function (b) {
      b.classList.toggle('active', b.dataset.method === method);
    });
    var qrBlock = document.getElementById('refundQrBlock');
    if (qrBlock) qrBlock.style.display = method === 'qr' ? '' : 'none';
  }

  function resetRefundPanel() {
    var cb = document.getElementById('refundRequested');
    if (cb) cb.checked = false;
    refundPanel.classList.remove('open');

    var bank = document.getElementById('refundBankSelect');
    if (bank) bank.value = '';
    var accNo = document.getElementById('refundAccountNumber');
    if (accNo) accNo.value = '';
    var accName = document.getElementById('refundAccountName');
    if (accName) accName.value = '';
    var fileInput = document.getElementById('refundQrFile');
    if (fileInput) fileInput.value = '';
    var statusEl = document.getElementById('refundQrStatus');
    if (statusEl) { statusEl.textContent = ''; statusEl.className = 'refund-qr-status'; }

    switchRefundMethod('qr');
  }

  /* ============================================================
   * 3) OPTION 1 — Chụp/tải ảnh mã QR → tự động điền form
   * ============================================================ */
  function handleRefundQrFile(file) {
    var statusEl = document.getElementById('refundQrStatus');
    if (!file || !statusEl) return;

    setStatus(statusEl, 'Đang xử lý ảnh...', '');

    if (typeof window.jsQR !== 'function') {
      setStatus(statusEl, 'Không tải được thư viện đọc mã QR. Vui lòng nhập thủ công.', 'error');
      return;
    }

    var reader = new FileReader();
    reader.onload = function (e) {
      var img = new Image();
      img.onload = function () {
        var imageData;
        try {
          var canvas = document.getElementById('refundQrCanvas');
          canvas.width  = img.naturalWidth;
          canvas.height = img.naturalHeight;
          var ctx = canvas.getContext('2d');
          ctx.drawImage(img, 0, 0);
          imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        } catch (err) {
          setStatus(statusEl, 'Không thể xử lý ảnh này. Vui lòng thử ảnh khác.', 'error');
          return;
        }

        var code = window.jsQR(imageData.data, imageData.width, imageData.height);
        if (!code || !code.data) {
          setStatus(statusEl, 'Không tìm thấy mã QR trong ảnh. Vui lòng chụp rõ nét hơn hoặc chuyển sang nhập thủ công.', 'error');
          return;
        }

        var info = extractBankInfoFromVietQR(code.data);
        if (!info) {
          setStatus(statusEl, 'Mã QR không đúng định dạng QR ngân hàng (VietQR). Vui lòng thử ảnh khác hoặc nhập thủ công.', 'error');
          return;
        }
        applyDecodedBankInfo(info, statusEl);
      };
      img.onerror = function () {
        setStatus(statusEl, 'Không thể đọc tệp ảnh này.', 'error');
      };
      img.src = e.target.result;
    };
    reader.onerror = function () {
      setStatus(statusEl, 'Không thể đọc tệp ảnh này.', 'error');
    };
    reader.readAsDataURL(file);
  }

  function applyDecodedBankInfo(info, statusEl) {
    var matched = null;
    if (bankListCache) {
      matched = bankListCache.filter(function (b) { return b.bin === info.bin; })[0] || null;
    }

    var select = document.getElementById('refundBankSelect');
    if (select && matched) select.value = matched.code;

    var accInput = document.getElementById('refundAccountNumber');
    if (accInput) accInput.value = info.accountNumber;

    // Tên chủ tài khoản: chỉ điền nếu tag 59 chứa 1 cái tên hợp lệ (xem
    // sanitizeAccountHolderName) VÀ ô đang trống (không ghi đè cái khách đã
    // tự gõ). Nếu tag 59 trống/không hợp lệ → để nguyên ô trống, khách tự gõ.
    var nameInput = document.getElementById('refundAccountName');
    var candidateName = sanitizeAccountHolderName(info.merchantName, matched);
    var nameAutoFilled = false;
    if (nameInput && candidateName && !nameInput.value.trim()) {
      nameInput.value = candidateName;
      nameAutoFilled = true;
    }

    var bankLabel = matched ? (matched.shortName || matched.code) : null;
    var msg = bankLabel
        ? '✓ Đã nhận diện ' + bankLabel + ' — STK ' + info.accountNumber + '.'
        : '✓ Đã đọc được số tài khoản ' + info.accountNumber + ', chưa xác định được ngân hàng — vui lòng chọn thủ công.';
    if (nameAutoFilled) {
      msg += ' Vui lòng kiểm tra lại tên chủ tài khoản.';
    } else if (!candidateName) {
      msg += ' Mã QR không có sẵn tên chủ tài khoản, vui lòng tự nhập.';
    }
    setStatus(statusEl, msg, 'success');
    updateConfirmButtonState();
  }

  /**
   * Tag 59 (Merchant Name) trong QR EMVCo KHÔNG được đảm bảo là tên chủ tài
   * khoản — có thể trống, có thể là chuỗi generic của ngân hàng/hệ thống QR
   * (vd "VIETQR", "NAPAS") thay vì tên khách. Chỉ coi là tên hợp lệ khi:
   *   - không rỗng, đủ dài tối thiểu, có chứa chữ cái (không phải toàn số)
   *   - KHÔNG trùng 1 trong các chuỗi generic thường gặp
   *   - KHÔNG trùng tên/tên viết tắt của chính ngân hàng vừa nhận diện được
   * Không hợp lệ → trả về null, KHÔNG điền gì cả, để khách tự gõ tay.
   */
  function sanitizeAccountHolderName(rawName, matchedBank) {
    if (!rawName) return null;
    var name = rawName.trim().replace(/\s+/g, ' ');
    if (name.length < 3) return null;
    if (!/[A-Za-zÀ-ỹ]/.test(name)) return null; // toàn số/ký tự đặc biệt

    var upper = name.toUpperCase();
    var GENERIC = ['VIETQR', 'VIET QR', 'NAPAS', 'NAPAS247', 'NAPAS 247',
                   'QRCODE', 'QR CODE', 'MERCHANT', 'MERCHANT NAME',
                   'BANK', 'PAYMENT', 'THANH TOAN', 'CHUYEN TIEN'];
    if (GENERIC.indexOf(upper) !== -1) return null;

    if (matchedBank) {
      var bankNames = [matchedBank.shortName, matchedBank.name, matchedBank.code]
          .filter(Boolean)
          .map(function (s) { return s.toUpperCase(); });
      if (bankNames.indexOf(upper) !== -1) return null;
    }

    return name;
  }

  function setStatus(el, text, kind) {
    el.textContent = text;
    el.className = 'refund-qr-status' + (kind ? ' ' + kind : '');
  }

  /* ============================================================
   * 4) Parser EMVCo QR (chuẩn VietQR) — tag-length-value đệ quy
   *    Tag gốc quan trọng: 38 = Merchant Account Info (NAPAS/VietQR),
   *    59 = Merchant Name. Bên trong tag 38: sub-tag chứa {00: bank BIN
   *    6 số, 01: số tài khoản}.
   * ============================================================ */
  function parseEmvTlv(payload) {
    var root = {};
    var i = 0;
    while (i + 4 <= payload.length) {
      var tag = payload.substring(i, i + 2);
      var lenStr = payload.substring(i + 2, i + 4);
      var len = parseInt(lenStr, 10);
      if (!/^\d{2}$/.test(tag) || isNaN(len)) break;
      var value = payload.substring(i + 4, i + 4 + len);
      if (value.length !== len) break; // dữ liệu bị cắt/hỏng
      root[tag] = value;
      i += 4 + len;
    }
    return root;
  }

  function extractBankInfoFromVietQR(payload) {
    try {
      var root = parseEmvTlv(payload);
      var candidateTags = ['38', '39', '40', '41', '42', '43', '44', '45'];
      for (var t = 0; t < candidateTags.length; t++) {
        var raw = root[candidateTags[t]];
        if (!raw) continue;
        var sub = parseEmvTlv(raw);
        var keys = Object.keys(sub);
        for (var k = 0; k < keys.length; k++) {
          var val = sub[keys[k]];
          if (!val || val.length < 6) continue;
          var inner = parseEmvTlv(val);
          var bin = inner['00'];
          var acc = inner['01'];
          if (bin && /^\d{6}$/.test(bin) && acc) {
            return { bin: bin, accountNumber: acc, merchantName: root['59'] || '' };
          }
        }
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  /* ============================================================
   * 5) VALIDATE trước khi cho bấm "Xác nhận huỷ"
   * ============================================================ */
  function updateConfirmButtonState() {
    var btn = document.getElementById('btnConfirmCancel');
    var confirmCb = document.getElementById('confirmCheck');
    if (!btn || !confirmCb) return;

    var ok = confirmCb.checked;
    var refundCb = document.getElementById('refundRequested');
    if (refundCb && refundCb.checked) {
      var bank    = document.getElementById('refundBankSelect');
      var accNo   = document.getElementById('refundAccountNumber');
      var accName = document.getElementById('refundAccountName');
      var filled = !!(bank && bank.value && accNo && accNo.value.trim() && accName && accName.value.trim());
      ok = ok && filled;
    }
    btn.disabled = !ok;
  }

  // Expose cho inline onclick/onchange trong JSP + cho appointment.js gọi khi đóng modal
  window.toggleRefundPanel      = toggleRefundPanel;
  window.switchRefundMethod     = switchRefundMethod;
  window.handleRefundQrFile     = handleRefundQrFile;
  window.updateConfirmButtonState = updateConfirmButtonState;
  window.resetRefundPanel       = resetRefundPanel;
})();
