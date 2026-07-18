package com.petclinic.servlet.home;

import com.petclinic.dao.*;
import com.petclinic.model.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

/**
 * URL map:
 *   GET /services                      — danh sách toàn bộ dịch vụ theo nhóm
 *   GET /services?category=ID          — như trên, tự cuộn tới nhóm ID khi tải trang
 *   GET /services?category=ID&service=ID — tự cuộn + highlight đúng dịch vụ
 */
@WebServlet(urlPatterns = {"/services"})
public class ServicesServlet extends HttpServlet {

    private final ServiceDAO      serviceDAO      = new ServiceDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            List<ServiceCategory> categories = serviceDAO.findAllCategoriesWithServices();

            req.setAttribute("categories",    categories);
            req.setAttribute("navCategories", categories);
            req.setAttribute("focusCategory", parseId(req.getParameter("category")));
            req.setAttribute("focusService",  parseId(req.getParameter("service")));

            attachCustomerContext(req);

            req.getRequestDispatcher("/WEB-INF/views/guest/services.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    /** Nạp số thông báo chưa đọc cho header nếu khách đã đăng nhập. */
    private void attachCustomerContext(HttpServletRequest req) throws Exception {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("customer") != null) {
            Customer c = (Customer) session.getAttribute("customer");
            req.setAttribute("unreadCount", notificationDAO.countUnread(c.getCustomerID()));
        }
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return 0; }
    }
}
