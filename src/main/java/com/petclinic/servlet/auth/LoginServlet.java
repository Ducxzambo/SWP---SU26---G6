package com.petclinic.servlet.auth;

import com.petclinic.model.User;
import com.petclinic.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Optional;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    // vào GET --> forward đến login.jsp --> form
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Nếu đã đăng nhập --> redirect về dashboard
        if (req.getSession(false) != null
         && req.getSession(false).getAttribute("currentUser") != null) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }
        req.getRequestDispatcher("/views/auth/login.jsp").forward(req, resp);
    }

    // form từ login --> về POST xử lý đăng nhập
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        String email    = req.getParameter("email");
        String password = req.getParameter("password");
        String remember = req.getParameter("remember"); // checkbox

        Optional<User> result = authService.login(email, password);

        if (result.isEmpty()) {
            req.setAttribute("errorMsg", "Email hoặc mật khẩu không đúng.");
            req.setAttribute("emailVal", email); // giữ lại email để UX tốt hơn
            req.getRequestDispatcher("/views/auth/login.jsp").forward(req, resp);
            return;
        }

        User user = result.get();

        // Tạo session mới (chống session fixation)
        HttpSession oldSession = req.getSession(false);
        if (oldSession != null) oldSession.invalidate();
        HttpSession session = req.getSession(true);
        session.setAttribute("currentUser", user);
        session.setMaxInactiveInterval(30 * 60); // 30 phút

        // Remember me → cookie 7 ngày
        if ("on".equals(remember)) {
            Cookie cookie = new Cookie("rememberEmail", email);
            cookie.setMaxAge(7 * 24 * 3600);
            cookie.setPath(req.getContextPath());
            cookie.setHttpOnly(true);
            resp.addCookie(cookie);
        }

        // Redirect theo role
        String target = switch (user.getRoleName()) {
            case "Admin"        -> "/admin/dashboard";
            case "Veterinarian" -> "/vet/dashboard";
            case "Receptionist" -> "/reception/dashboard";
            default             -> "/home";
        };
        resp.sendRedirect(req.getContextPath() + target);
    }
}
