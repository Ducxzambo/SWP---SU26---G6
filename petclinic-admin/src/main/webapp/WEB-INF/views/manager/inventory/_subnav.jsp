<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<nav class="subnav">
    <a href="${pageContext.request.contextPath}/admin/inventory"
       class="subnav-item ${activeTab == 'list' ? 'active' : ''}">Tồn kho</a>
    <a href="${pageContext.request.contextPath}/admin/inventory/stock-in"
       class="subnav-item ${activeTab == 'stockIn' ? 'active' : ''}">Nhập kho</a>
    <a href="${pageContext.request.contextPath}/admin/inventory/stock-out"
       class="subnav-item ${activeTab == 'stockOut' ? 'active' : ''}">Xuất kho</a>
    <a href="${pageContext.request.contextPath}/admin/inventory/thresholds"
       class="subnav-item ${activeTab == 'thresholds' ? 'active' : ''}">Ngưỡng cảnh báo</a>
    <a href="${pageContext.request.contextPath}/admin/inventory/transactions"
       class="subnav-item ${activeTab == 'transactions' ? 'active' : ''}">Lịch sử giao dịch</a>
    <a href="${pageContext.request.contextPath}/admin/inventory/receipts"
       class="subnav-item ${activeTab == 'receipts' ? 'active' : ''}">Phiếu nhập</a>
    <a href="${pageContext.request.contextPath}/admin/inventory/report"
       class="subnav-item ${activeTab == 'report' ? 'active' : ''}">Báo cáo</a>
</nav>
<style>
main:has(.stock-out-form)>.card{max-width:none!important;width:100%}main:has(.stock-out-form){max-width:none!important;padding-right:48px}main:has(.stock-out-form) .card-body{padding:30px}main:has(.stock-out-form) .stock-out-form{padding:30px;background:linear-gradient(145deg,#f7fcfb,#eff8f5);border-radius:16px}main:has(.stock-out-form) .form-row{gap:22px}main:has(.stock-out-form) .form-group{padding:20px;border-radius:13px}main:has(.stock-out-form) .form-label{font-size:16px}main:has(.stock-out-form) .form-control{min-height:52px}main:has(.stock-out-form) .stock-out-notice{margin-bottom:24px;padding:18px 20px;font-size:15px}main:has(.stock-out-form) .stock-out-actions{margin-top:26px;padding-top:23px}main:has(.stock-out-form) .stock-out-actions .btn{min-height:48px;padding:0 24px;border-radius:10px}@media(max-width:900px){main:has(.stock-out-form){padding-right:20px}main:has(.stock-out-form) .card-body{padding:20px}main:has(.stock-out-form) .stock-out-form{padding:18px}}
.stock-confirm{display:none;position:fixed;inset:0;z-index:3000;align-items:center;justify-content:center;padding:20px;background:rgba(3,27,24,.62);backdrop-filter:blur(5px)}.stock-confirm.open{display:flex}.stock-confirm-box{width:min(410px,100%);overflow:hidden;border-radius:18px;background:#fff;box-shadow:0 28px 75px rgba(0,0,0,.35);animation:stock-pop .2s ease}.stock-confirm-head{padding:21px 24px;background:linear-gradient(135deg,#06453d,#11937f);color:#fff;font-weight:750;font-size:20px}.stock-confirm-body{padding:24px;color:#355d57;line-height:1.55}.stock-confirm-icon{display:grid;place-items:center;width:48px;height:48px;margin:-47px 0 15px;border-radius:50%;background:#fff1d8;color:#c98000;font-size:24px;box-shadow:0 4px 13px rgba(0,0,0,.12)}.stock-confirm-actions{display:flex;justify-content:flex-end;gap:10px;margin-top:22px}.stock-confirm-actions button{border:0;border-radius:9px;padding:11px 16px;font-weight:700;cursor:pointer}.stock-cancel{background:#edf3f2;color:#41645e}.stock-accept{background:#087d6b;color:#fff}@keyframes stock-pop{from{opacity:0;transform:translateY(12px) scale(.97)}to{opacity:1;transform:none}}
</style>
<script>
document.addEventListener('DOMContentLoaded',function(){var form=document.querySelector('.stock-out-form');if(!form)return;window.confirm=function(){return true;};var modal=document.createElement('div');modal.className='stock-confirm';modal.innerHTML='<div class="stock-confirm-box"><div class="stock-confirm-head">Xác nhận xuất kho</div><div class="stock-confirm-body"><div class="stock-confirm-icon">!</div>Bạn có chắc muốn lưu giao dịch xuất kho này? Số lượng tồn sẽ được cập nhật ngay.<div class="stock-confirm-actions"><button type="button" class="stock-cancel">Hủy</button><button type="button" class="stock-accept">Xác nhận xuất kho</button></div></div></div>';document.body.appendChild(modal);form.addEventListener('submit',function(e){if(!form.dataset.confirmed){e.preventDefault();modal.classList.add('open');}});modal.querySelector('.stock-cancel').onclick=function(){modal.classList.remove('open');};modal.querySelector('.stock-accept').onclick=function(){form.dataset.confirmed='1';modal.classList.remove('open');form.requestSubmit();};modal.onclick=function(e){if(e.target===modal)modal.classList.remove('open');};});
</script>
