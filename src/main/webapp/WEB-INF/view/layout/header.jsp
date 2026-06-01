<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><c:out value="${pageTitle}" default="promptmark"/></title>

  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Space+Grotesk:wght@500;700&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet">

  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/tokens.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">

  <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r170/three.min.js" defer></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/gsap/3.12.5/gsap.min.js" defer></script>
  <script src="${pageContext.request.contextPath}/assets/js/effects.js" defer></script>
</head>
<body>
<c:import url="/WEB-INF/view/layout/nav.jsp"/>
<main class="container">
