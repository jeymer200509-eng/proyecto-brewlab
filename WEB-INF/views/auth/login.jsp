<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Iniciar sesión — BrewLab</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/brewlab.css">
</head>
<body>
<main class="auth-layout">
  <section class="auth-card" aria-labelledby="login-heading">
    <div class="brand-block" style="border-bottom-color: var(--border-hairline); padding-left:0; padding-right:0;">
      <span class="brand-mark" aria-hidden="true">BL</span>
      <div>
        <div class="brand-title" style="color: var(--text-primary);">BrewLab</div>
        <div class="brand-subtitle" style="color: var(--text-muted);">Laboratorio Digital de Café de Especialidad</div>
      </div>
    </div>

    <h1 id="login-heading" style="font-size: 1.35rem; margin-bottom: 0.35rem;">Acceso al laboratorio</h1>
    <p style="font-size: 0.86rem; color: var(--text-muted); margin-bottom: 1.1rem;">
      Autenticación mediante token JWT firmado (HS256) en cookie segura HttpOnly.
    </p>

    <c:if test="${not empty flashMessage}">
      <div class="alert alert-${flashType == 'error' ? 'error' : 'info'}" role="status">
        <span><c:out value="${flashMessage}"/></span>
      </div>
    </c:if>

    <c:if test="${not empty errors.general}">
      <div class="alert alert-error" role="alert">
        <span><c:out value="${errors.general}"/></span>
      </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/auth/login" class="stack-md" data-once>
      <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">

      <div class="field">
        <label class="field-label" for="login-email">Correo electrónico</label>
        <input class="input" id="login-email" name="email" type="email" autocomplete="email" required
               value="${fn:escapeXml(formEmail)}">
        <c:if test="${not empty errors.email}">
          <span class="field-error"><c:out value="${errors.email}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="login-password">Contraseña</label>
        <input class="input" id="login-password" name="password" type="password" autocomplete="current-password" required>
        <c:if test="${not empty errors.password}">
          <span class="field-error"><c:out value="${errors.password}"/></span>
        </c:if>
      </div>

      <button type="submit" class="btn btn-primary btn-lg" style="width: 100%;">Entrar al laboratorio</button>
    </form>

    <p style="font-size: 0.84rem; color: var(--text-secondary); margin-top: 1.15rem; margin-bottom: 0; text-align: center;">
      ¿Primera vez en este equipo?
      <a href="${pageContext.request.contextPath}/auth/registro" style="color: var(--accent-caramel); font-weight: 600;">
        Crear perfil de catador/barista
      </a>
    </p>
  </section>
</main>
<script src="${pageContext.request.contextPath}/assets/js/brewlab.js" defer></script>
</body>
</html>
