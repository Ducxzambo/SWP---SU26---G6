<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <title>404 - Not Found</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css" />
    <style>
        body { font-family: Arial, Helvetica, sans-serif; text-align: center; padding: 4rem; }
        .code { font-size: 6rem; font-weight: 700; color: #e74c3c; }
        .message { font-size: 1.25rem; margin-top: 1rem; }
        a { color: #3498db; text-decoration: none; }
    </style>
</head>
<body>
    <div class="container">
        <div class="code">404</div>
        <div class="message">The page you requested was not found.</div>
        <p><a href="${pageContext.request.contextPath}/">Return to home</a></p>
    </div>
</body>
</html>

