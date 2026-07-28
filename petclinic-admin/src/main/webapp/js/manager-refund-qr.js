/**
 * manager-refund-qr.js — "Tạo yêu cầu hoàn tiền mới" form (create-form.jsp).
 *
 * Same job as refund.js's bank-info panel (customer-side cancel modal), but
 * wired to this page's own element IDs (mrf* prefix) instead of reusing
 * refund.js directly, since that file is tightly coupled to the cancel
 * modal's open/close lifecycle. The EMV/VietQR TLV-parsing algorithm itself
 * is copied as-is (pure functions, no DOM dependency) — see refund.js for
 * the original with more detailed comments on the tag structure.
 */
(function () {
  var form = document.getElementById('mrfForm');
  if (!form) return;

  var BANKS_API_URL = 'https://api.vietqr.io/v2/banks';
  var bankListCache = null;

  /* ============================================================
   * 1) DANH SÁCH NGÂN HÀNG
   * ============================================================ */
  function loadBankList() {
    var select = document.getElementById('mrfBankSelect');
    if (!select) return;
    select.innerHTML = '';
    select.appendChild(makeOption('', 'Đang tải danh sách ngân hàng...'));

    fetch(BANKS_API_URL)
      .then(function (r) { return r.json(); })
      .then(function (json) {
        var banks = (json && Array.isArray(json.data)) ? json.data : [];
        bankListCache = banks;
        select.innerHTML = '';
        select.appendChild(makeOption('', '-- Chọn ngân hàng --'));
        banks.forEach(function (b) {
          var opt = makeOption(b.code, (b.shortName || b.code) + ' — ' + b.name);
          opt.dataset.bin = b.bin || '';
          select.appendChild(opt);
        });
      })
      .catch(function () {
        select.innerHTML = '';
        select.appendChild(makeOption('', 'Không tải được danh sách ngân hàng — vui lòng thử lại'));
      });
  }

  function makeOption(value, label) {
    var opt = document.createElement('option');
    opt.value = value;
    opt.textContent = label;
    return opt;
  }

  /* ============================================================
   * 2) TOGGLE "Quét QR" / "Nhập thủ công"
   * ============================================================ */
  window.mrfSwitchMethod = function (method) {
    document.querySelectorAll('.refund-method-btn').forEach(function (b) {
      b.classList.toggle('active', b.dataset.method === method);
    });
    var qrBlock = document.getElementById('mrfQrBlock');
    if (qrBlock) qrBlock.style.display = method === 'qr' ? '' : 'none';
  };

  /* ============================================================
   * 3) TOGGLE "Hoàn tiền 100%"
   * ============================================================ */
  window.mrfToggleFullRefund = function (checked) {
    var amount = document.getElementById('mrfAmount');
    if (!amount) return;
    amount.disabled = checked;
    if (checked) amount.value = '';
    else amount.required = true;
  };

  /* ============================================================
   * 4) OPTION 1 — Chụp/tải ảnh mã QR → tự động điền form
   * ============================================================ */
  window.mrfHandleQrFile = function (file) {
    var statusEl = document.getElementById('mrfQrStatus');
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
          var canvas = document.getElementById('mrfQrCanvas');
          canvas.width = img.naturalWidth;
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
          setStatus(statusEl, 'Không tìm thấy mã QR trong ảnh. Vui lòng thử ảnh khác hoặc nhập thủ công.', 'error');
          return;
        }

        var info = extractBankInfoFromVietQR(code.data);
        if (!info) {
          setStatus(statusEl, 'Mã QR không đúng định dạng QR ngân hàng (VietQR). Vui lòng thử ảnh khác hoặc nhập thủ công.', 'error');
          return;
        }
        applyDecodedBankInfo(info, statusEl);
      };
      img.onerror = function () { setStatus(statusEl, 'Không thể đọc tệp ảnh này.', 'error'); };
      img.src = e.target.result;
    };
    reader.onerror = function () { setStatus(statusEl, 'Không thể đọc tệp ảnh này.', 'error'); };
    reader.readAsDataURL(file);
  };

  function applyDecodedBankInfo(info, statusEl) {
    var matched = null;
    if (bankListCache) {
      matched = bankListCache.filter(function (b) { return b.bin === info.bin; })[0] || null;
    }

    var select = document.getElementById('mrfBankSelect');
    if (select && matched) select.value = matched.code;

    var accInput = document.getElementById('mrfAccountNumber');
    if (accInput) accInput.value = info.accountNumber;

    var nameInput = document.getElementById('mrfAccountName');
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
  }

  function sanitizeAccountHolderName(rawName, matchedBank) {
    if (!rawName) return null;
    var name = rawName.trim().replace(/\s+/g, ' ');
    if (name.length < 3) return null;
    if (!/[A-Za-zÀ-ỹ]/.test(name)) return null;

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
   * 5) Parser EMVCo QR (chuẩn VietQR) — giống hệt refund.js
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
      if (value.length !== len) break;
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
   * Init
   * ============================================================ */
  loadBankList();
})();
