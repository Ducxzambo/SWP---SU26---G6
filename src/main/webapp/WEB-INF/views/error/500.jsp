<%@ page isErrorPage="true" contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <title>500 - Internal Server Error</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css" />
    <style>
        body { font-family: Arial, Helvetica, sans-serif; text-align: center; padding: 4rem; }
        .code { font-size: 6rem; font-weight: 700; color: #c0392b; }
        .message { font-size: 1.25rem; margin-top: 1rem; }
        pre { text-align: left; display: inline-block; margin-top: 1rem; max-width: 80%; overflow: auto; background: #f8f8f8; padding: 1rem; border-radius: 4px; }
        a { color: #3498db; text-decoration: none; }
    </style>
</head>
<body>
    <div class="container">
        <div class="code">500</div>
        <div class="message">An internal server error occurred.</div>
        <p><a href="${pageContext.request.contextPath}/">Return to home</a></p>
        <%-- Optionally show stack trace in dev (kept minimal for production safety) --%>
        <%!
            // Minimal HTML escaper to avoid adding external dependencies
            public String escapeHtml(String s) {
                if (s == null) return "";
                return s.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#39;");
            }
        %>
        <%
            if (exception != null) {
        %>
            <pre><%= escapeHtml(exception.toString()) %></pre>
        <%
            }
        %>
    </div>
</body>
</html>

