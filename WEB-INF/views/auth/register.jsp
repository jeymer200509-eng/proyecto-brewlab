<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Crear cuenta — BrewLab</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/brewlab.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/scientific-immersion.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/modules.css">
</head>
<body class="immersion module-auth">
<main class="auth-layout">
  <div class="auth-intro"><span class="eyebrow ion">BREWLAB / SCIENTIFIC IMMERSION</span><p class="auth-statement">El café como<br>campo de estudio.</p><p>Observe el origen. Afine la extracción.<br>Descubra lo que revela cada taza.</p><div class="auth-orbit" aria-hidden="true"><span>ORIGEN</span><span>EXTRACCIÓN</span><span>PERCEPCIÓN</span></div></div>
  <section class="auth-card" aria-labelledby="register-heading">
    <div class="brand-block" style="border-bottom-color: var(--border-hairline); padding-left:0; padding-right:0;">
      <span class="brand-mark" aria-hidden="true">BL</span>
      <div>
        <div class="brand-title" style="color: var(--text-primary);">BrewLab</div>
        <div class="brand-subtitle" style="color: var(--text-muted);">Registro de Evaluador / Barista</div>
      </div>
    </div>

    <h1 id="register-heading" style="font-size: 1.35rem; margin-bottom: 0.35rem;">Crear perfil de trabajo</h1>
    <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1.1rem;">
      Tu nombre se usará como evaluador por defecto en las fichas sensoriales.
    </p>

    <form method="post" action="${pageContext.request.contextPath}/auth/registro" class="stack-md" data-once>
      <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">

      <div class="field">
        <label class="field-label" for="reg-name">Nombre o alias de evaluador</label>
        <input class="input" id="reg-name" name="displayName" type="text" required minlength="2" maxlength="120"
               value="${fn:escapeXml(formDisplayName)}" placeholder="Ej. Valentina Ríos">
        <c:if test="${not empty errors.displayName}">
          <span class="field-error"><c:out value="${errors.displayName}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="reg-email">Correo electrónico</label>
        <input class="input" id="reg-email" name="email" type="email" autocomplete="email" required
               value="${fn:escapeXml(formEmail)}" placeholder="barista@ejemplo.com">
        <c:if test="${not empty errors.email}">
          <span class="field-error"><c:out value="${errors.email}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="reg-password">Contraseña (mínimo 8 caracteres)</label>
        <input class="input" id="reg-password" name="password" type="password" autocomplete="new-password" required minlength="8">
        <c:if test="${not empty errors.password}">
          <span class="field-error"><c:out value="${errors.password}"/></span>
        </c:if>
      </div>

      <button type="submit" class="btn btn-primary btn-lg" style="width: 100%;">Crear cuenta e iniciar</button>
    </form>

    <p style="font-size: 0.84rem; color: var(--text-secondary); margin-top: 1.15rem; margin-bottom: 0; text-align: center;">
      ¿Ya tienes cuenta?
      <a href="${pageContext.request.contextPath}/auth/login" style="color: var(--accent-caramel); font-weight: 600;">
        Iniciar sesión
      </a>
    </p>
  </section>
</main>
<script src="${pageContext.request.contextPath}/assets/js/brewlab.js" defer></script>
</body>
</html>
