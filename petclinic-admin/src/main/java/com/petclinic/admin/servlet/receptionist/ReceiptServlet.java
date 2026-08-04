// petclinic-admin/src/main/java/com/petclinic/admin/servlet/receptionist/ReceiptServlet.java
package com.petclinic.admin.servlet.receptionist;

import com.petclinic.backend.dto.ReceiptData;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.ReceiptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;

@WebServlet("/receptionist/invoice/receipt")
public class ReceiptServlet extends HttpServlet {

    private final ReceiptService receiptService = new ReceiptService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        Staff staff = session != null ? (Staff) session.getAttribute("staff") : null;
        if (staff == null || !"Receptionist".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login"); return;
        }

        int invoiceId = parseId(req.getParameter("invoiceId"));
        String from = "history".equals(req.getParameter("from")) ? "history" : "checkin";
        if (invoiceId <= 0) { resp.sendRedirect(req.getContextPath() + "/receptionist/" + from); return; }

        try {
            ReceiptData receipt = receiptService.buildReceiptForDownload(invoiceId);
            if (receipt == null) {
                session.setAttribute("flashError", "Hóa đơn #" + invoiceId + " chưa được thanh toán.");
                resp.sendRedirect(req.getContextPath() + "/receptionist/invoice?invoiceId=" + invoiceId + "&from=" + from);
                return;
            }

            if ("pdf".equalsIgnoreCase(req.getParameter("format"))) {
                byte[] pdf = receiptService.renderPdf(receipt);
                resp.setContentType("application/pdf");
                resp.setHeader("Content-Disposition", "attachment; filename=\"" + receipt.getInvoiceCode() + ".pdf\"");
                resp.setContentLength(pdf.length);
                resp.getOutputStream().write(pdf);
                return;
            }

            req.setAttribute("receipt", receipt);
            req.setAttribute("from", from);
            req.getRequestDispatcher("/WEB-INF/views/receptionist/receipt.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Lỗi hệ thống: " + e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/receptionist/" + from);
        }
    }


    private int parseId(String s) {
        if (s == null || s.isBlank()) return -1;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return -1; }
    }
}