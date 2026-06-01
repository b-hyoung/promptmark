<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    // True landing page: forward (not redirect) so URL stays `/promptmark/`.
    request.getRequestDispatcher("/app/home/index").forward(request, response);
%>
