package com.petclinic.admin.servlet.manager.shift;

import com.petclinic.backend.dao.ShiftCapacityDAO;
import com.petclinic.backend.model.ShiftCapacity;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.BookingService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@WebServlet("/manager/capacity")
public class ShiftCapacityServlet extends HttpServlet {

    private final ShiftCapacityDAO capacityDAO = new ShiftCapacityDAO();
    private static final int[] SHIFTS = {1, 2, 3, 4};

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            LocalDate from = LocalDate.now();
            LocalDate to   = from.plusDays(BookingService.DAYS_AHEAD - 1);

            Map<LocalDate, Map<Integer, ShiftCapacity>> existing = capacityDAO.findByDateRange(from, to);

            req.setAttribute("fromDate", from);
            req.setAttribute("toDate", to);
            req.setAttribute("shifts", SHIFTS);
            req.setAttribute("existing", existing);
            req.setAttribute("daysAhead", BookingService.DAYS_AHEAD);

            req.getRequestDispatcher("/WEB-INF/views/manager/shift/capacity.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được dữ liệu sức chứa: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/shift/capacity.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        String action = req.getParameter("action");
        if ("bulkApply".equals(action)) {
            handleBulkApply(req, resp, manager);
        } else {
            handleSaveGrid(req, resp, manager);
        }
    }

    // Lưu từng ô riêng lẻ (bảng chi tiết)
    private void handleSaveGrid(HttpServletRequest req, HttpServletResponse resp, Staff manager)
            throws IOException {
        try {
            LocalDate from = LocalDate.now();
            LocalDate to   = from.plusDays(BookingService.DAYS_AHEAD - 1);

            int saved = 0;
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                for (int shift : SHIFTS) {
                    String groomParam = req.getParameter("groomCap_" + d + "_" + shift);
                    String vetParam   = req.getParameter("vetCap_" + d + "_" + shift);
                    if (groomParam == null && vetParam == null) continue;

                    int groomCap = parseNonNegativeInt(groomParam);
                    int vetCap   = parseNonNegativeInt(vetParam);
                    capacityDAO.upsert(d, shift, groomCap, vetCap, manager.getStaffID());
                    saved++;
                }
            }
            req.getSession().setAttribute("flashSuccess", "Đã cập nhật sức chứa cho " + saved + " ca.");
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống khi lưu sức chứa: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/manager/capacity");
    }

    // Áp cùng 1 cặp GroomCap/VetCap cho NHIỀU ngày x NHIỀU ca cùng lúc
    private void handleBulkApply(HttpServletRequest req, HttpServletResponse resp, Staff manager)
            throws IOException {
        try {
            LocalDate windowStart = LocalDate.now();
            LocalDate windowEnd   = windowStart.plusDays(BookingService.DAYS_AHEAD - 1);

            LocalDate bulkFrom = parseDate(req.getParameter("bulkFrom"));
            LocalDate bulkTo   = parseDate(req.getParameter("bulkTo"));
            if (bulkFrom == null || bulkTo == null) {
                req.getSession().setAttribute("flashError", "Vui lòng chọn đầy đủ khoảng ngày áp dụng.");
                resp.sendRedirect(req.getContextPath() + "/manager/capacity");
                return;
            }

            // Kẹp khoảng ngày vào đúng cửa sổ cho phép chỉnh sửa
            LocalDate effectiveFrom = bulkFrom.isBefore(windowStart) ? windowStart : bulkFrom;
            LocalDate effectiveTo   = bulkTo.isAfter(windowEnd) ? windowEnd : bulkTo;

            if (effectiveFrom.isAfter(effectiveTo)) {
                req.getSession().setAttribute("flashError",
                        "Khoảng ngày không hợp lệ hoặc nằm ngoài phạm vi cho phép chỉnh sửa ("
                                + windowStart + " → " + windowEnd + ").");
                resp.sendRedirect(req.getContextPath() + "/manager/capacity");
                return;
            }

            String[] shiftParams = req.getParameterValues("shifts");
            if (shiftParams == null || shiftParams.length == 0) {
                req.getSession().setAttribute("flashError", "Vui lòng chọn ít nhất 1 ca để áp dụng.");
                resp.sendRedirect(req.getContextPath() + "/manager/capacity");
                return;
            }

            int groomCap = parseNonNegativeInt(req.getParameter("bulkGroomCap"));
            int vetCap   = parseNonNegativeInt(req.getParameter("bulkVetCap"));

            int applied = 0;
            for (LocalDate d = effectiveFrom; !d.isAfter(effectiveTo); d = d.plusDays(1)) {
                for (String shiftStr : shiftParams) {
                    try {
                        int shift = Integer.parseInt(shiftStr.trim());
                        if (shift < 1 || shift > 4) continue;
                        capacityDAO.upsert(d, shift, groomCap, vetCap, manager.getStaffID());
                        applied++;
                    } catch (NumberFormatException ignored) {}
                }
            }

            req.getSession().setAttribute("flashSuccess",
                    "Đã áp dụng sức chứa hàng loạt (Groomer=" + groomCap + ", Vet=" + vetCap + ") cho "
                            + applied + " ca, từ " + effectiveFrom + " đến " + effectiveTo + ".");
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống khi áp dụng hàng loạt: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/manager/capacity");
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (DateTimeParseException e) { return null; }
    }

    private int parseNonNegativeInt(String s) {
        try {
            int v = Integer.parseInt(s.trim());
            return Math.max(0, v);
        } catch (Exception e) {
            return 0;
        }
    }
}