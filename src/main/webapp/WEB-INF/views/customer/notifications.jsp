<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Thông báo – PetClinic</title>
  <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/views/common/header.jsp" %>

<div style="max-width:760px;margin:40px auto;padding:0 24px;">

  <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:24px;">
    <h1 style="font-family:'Playfair Display',serif;font-size:26px;color:var(--green-900);">
      🔔 Thông báo
      <c:if test="${unreadCount > 0}">
        <span style="background:var(--green-500);color:#fff;border-radius:12px;
              padding:2px 10px;font-size:14px;font-family:'DM Sans',sans-serif;
              font-weight:600;vertical-align:middle;">${unreadCount} mới</span>
      </c:if>
    </h1>
    <c:if test="${unreadCount > 0}">
      <form action="${ctx}/notifications/mark-read" method="post">
        <button type="submit"
                style="padding:8px 16px;border:1.5px solid var(--border);border-radius:8px;
                       background:#fff;font-size:13.5px;cursor:pointer;color:var(--text-mid);
                       transition:var(--transition);">
          ✓ Đánh dấu tất cả đã đọc
        </button>
      </form>
    </c:if>
  </div>

  <c:choose>
    <c:when test="${empty notifications}">
      <div style="text-align:center;padding:60px 0;color:var(--warm-gray);">
        <div style="font-size:48px;margin-bottom:16px;">🔕</div>
        <p style="font-size:15px;">Bạn chưa có thông báo nào.</p>
      </div>
    </c:when>
    <c:otherwise>
      <c:forEach var="n" items="${notifications}">
        <div style="background:#fff;border:1.5px solid ${n.read ? 'var(--border)' : 'var(--green-400)'};
                    border-radius:12px;padding:18px 20px;margin-bottom:12px;
                    ${n.read ? '' : 'border-left-width:4px;'}
                    transition:var(--transition);">
          <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:12px;">
            <div style="flex:1;">
              <div style="font-weight:600;font-size:15px;margin-bottom:5px;
                          color:${n.read ? 'var(--text-dark)' : 'var(--green-900)'};">
                <c:if test="${!n.read}">
                  <span style="display:inline-block;width:8px;height:8px;background:var(--green-500);
                                border-radius:50%;margin-right:6px;vertical-align:middle;"></span>
                </c:if>
                ${n.title}
              </div>
              <div style="font-size:14px;color:var(--text-mid);line-height:1.6;">${n.body}</div>
            </div>
            <div style="font-size:12px;color:var(--warm-gray);white-space:nowrap;flex-shrink:0;">
              <c:if test="${n.createdAt != null}">
                <fmt:formatDate value="${n.createdAt}" pattern="HH:mm dd/MM/yyyy" type="both"/>
              </c:if>
            </div>
          </div>
        </div>
      </c:forEach>
    </c:otherwise>
  </c:choose>

</div>

<%@ include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>
