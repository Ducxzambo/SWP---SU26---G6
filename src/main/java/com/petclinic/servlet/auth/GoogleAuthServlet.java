package com.petclinic.servlet.auth;

import com.petclinic.dao.CustomerDAO;
import com.petclinic.model.Customer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;  // Add org.json dependency

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Google OAuth 2.0 login flow.
 * <p>
 * Maven dependency:
 * <dependency>
 * <groupId>org.json</groupId>
 * <artifactId>json</artifactId>
 * <version>20240303</version>
 * </dependency>
 * <p>
 * Google Cloud Console setup:
 * 1. Create OAuth 2.0 Client ID (Web application)
 * 2. Add Authorized redirect URI: http://localhost:9999/petclinic/auth/google/callback
 * 3. Set env vars: GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
 */
@WebServlet(urlPatterns = {"/auth/google", "/auth/google/callback"})
public class GoogleAuthServlet extends HttpServlet {

    // ── Read from environment variables (never hard-code secrets) ────────────
    private static final String CLIENT_ID = System.getenv("GOOGLE_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("GOOGLE_CLIENT_SECRET");
    private static final String REDIRECT_URI = "http://localhost:9999/petclinic/auth/google/callback";
    private static final String SCOPE = "openid email profile";

    private final CustomerDAO customerDAO = new CustomerDAO();

    // ── GET /auth/google: redirect to Google consent screen ──────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getServletPath();

        if ("/auth/google".equals(path)) {
            // Generate random state for CSRF protection
            String state = java.util.UUID.randomUUID().toString();
            req.getSession(true).setAttribute("oauth_state", state);

            String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + URLEncoder.encode(CLIENT_ID, "UTF-8")
                    + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8")
                    + "&response_type=code"
                    + "&scope=" + URLEncoder.encode(SCOPE, "UTF-8")
                    + "&state=" + state
                    + "&access_type=offline";
            resp.sendRedirect(authUrl);
            return;
        }

        // ── GET /auth/google/callback: exchange code for token ─────────────
        String code = req.getParameter("code");
        String state = req.getParameter("state");
        HttpSession session = req.getSession(false);

        // CSRF check
        if (session == null || !state.equals(session.getAttribute("oauth_state"))) {
            resp.sendRedirect(req.getContextPath() + "/auth/login?error=oauth_state");
            return;
        }
        session.removeAttribute("oauth_state");

        if (code == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login?error=oauth_denied");
            return;
        }

        try {
            // Exchange auth code for access token
            String tokenResponse = exchangeCodeForToken(code);
            JSONObject tokenJson = new JSONObject(tokenResponse);
            String accessToken = tokenJson.getString("access_token");

            // Get user info from Google
            String userInfoResponse = fetchUserInfo(accessToken);
            JSONObject userInfo = new JSONObject(userInfoResponse);

            String email = userInfo.getString("email");
            String fullName = userInfo.optString("name", email);

            // Find or create customer
            Customer customer = customerDAO.findByEmail(email);
            if (customer == null) {
                // Auto-register Google users with a random unusable password
                String randomHash = com.petclinic.util.PasswordUtil.hashPassword(
                        java.util.UUID.randomUUID().toString());
                Customer newCustomer = new Customer(fullName, email, null, randomHash);
                int id = customerDAO.insert(newCustomer);
                customer = customerDAO.findById(id);
            }

            // Create session
            session = req.getSession(true);
            session.setAttribute("customer", customer);
            session.setMaxInactiveInterval(60 * 60 * 8);

            resp.sendRedirect(req.getContextPath() + "/");

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/auth/login?error=oauth_failed");
        }
    }

    // ── OAuth helpers ─────────────────────────────────────────────────────────

    private String exchangeCodeForToken(String code) throws Exception {
        URL url = new URL("https://oauth2.googleapis.com/token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String body = "code=" + URLEncoder.encode(code, "UTF-8")
                + "&client_id=" + URLEncoder.encode(CLIENT_ID, "UTF-8")
                + "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, "UTF-8")
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8")
                + "&grant_type=authorization_code";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    private String fetchUserInfo(String accessToken) throws Exception {
        URL url = new URL("https://www.googleapis.com/oauth2/v2/userinfo");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        return readResponse(conn);
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
