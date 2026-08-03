package com.petclinic.admin.servlet.manager.refunds;

import com.petclinic.backend.dto.ReceiptData;
import com.petclinic.backend.model.Refund;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.ReceiptService;
import com.petclinic.backend.service.RefundService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/manager/refunds/receipt")
public class RefundReceiptServlet extends HttpServlet {

    private final RefundService  refundService  = new RefundService();
    private final ReceiptService receiptService = new ReceiptService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        int refundId = parseId(req.getParameter("id"));
        try {
            Refund refund = refundService.getById(refundId);
            if (refund == null || !refund.isProcessed()) {
                req.getSession().setAttribute("flashError",
                        "Chỉ có thể xuất biên lai cho yêu cầu đã hoàn tất chuyển khoản.");
                resp.sendRedirect(req.getContextPath() + "/manager/refunds/detail?id=" + refundId);
                return;
            }

            ReceiptData receipt = receiptService.buildRefundReceipt(refund);

            if ("pdf".equalsIgnoreCase(req.getParameter("format"))) {
                byte[] pdf = receiptService.renderPdf(receipt);
                resp.setContentType("application/pdf");
                resp.setHeader("Content-Disposition", "attachment; filename=\"" + receipt.getInvoiceCode() + ".pdf\"");
                resp.setContentLength(pdf.length);
                resp.getOutputStream().write(pdf);
                return;
            }

            req.setAttribute("receipt", receipt);
            req.setAttribute("refundId", refundId);
            req.getRequestDispatcher("/WEB-INF/views/manager/refunds/receipt.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống: " + e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/manager/refunds/detail?id=" + refundId);
        }
    }

    private int parseId(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return -1; } }
}