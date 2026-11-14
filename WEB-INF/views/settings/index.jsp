<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageHeading" value="Configuración y Respaldo del Laboratorio"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<div class="grid-2">
  <section class="surface-card" aria-labelledby="prefs-heading">
    <div class="card-header">
      <div>
        <h2 id="prefs-heading">Preferencias de estación de trabajo</h2>
        <p class="field-hint" style="margin-bottom:0;">
          Última modificación guardada: <strong class="tabular"><c:out value="${lastSavedFormatted}"/></strong>
        </p>
      </div>
      <span class="badge badge-preference">Preferencia de usuario</span>
    </div>

    <form method="post" action="${pageContext.request.contextPath}/app/ajustes" class="stack-md">
      <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">

      <div class="field">
        <label class="cluster" style="cursor:pointer;">
          <input type="checkbox" name="soundEnabled" value="true" ${settings.soundEnabled ? 'checked' : ''}>
          <span><strong>Aviso acústico opcional en pasos del cronómetro</strong></span>
        </label>
        <span class="field-hint">La interfaz nunca depende exclusivamente del sonido; siempre muestra alertas visuales.</span>
      </div>

      <div class="field">
        <label class="cluster" style="cursor:pointer;">
          <input type="checkbox" name="keepScreenAwake" value="true" ${settings.keepScreenAwake ? 'checked' : ''}>
          <span><strong>Intentar mantener la pantalla activa durante la preparación (Wake Lock API)</strong></span>
        </label>
        <span class="field-hint">Si el navegador no soporta Wake Lock, el cronómetro sigue funcionando mediante cálculo por timestamps.</span>
      </div>

      <div class="field">
        <label class="field-label" for="timerTheme">Tema visual del cronómetro (Brew Assistant)</label>
        <select class="input" id="timerTheme" name="timerTheme">
          <option value="auto" ${settings.timerThemeCode == 'auto' ? 'selected' : ''}>Automático (crema de laboratorio)</option>
          <option value="claro" ${settings.timerThemeCode == 'claro' ? 'selected' : ''}>Claro de alto contraste</option>
          <option value="oscuro" ${settings.timerThemeCode == 'oscuro' ? 'selected' : ''}>Modo oscuro (espresso nocturno)</option>
        </select>
      </div>

      <div>
        <button type="submit" class="btn btn-primary">Guardar preferencias</button>
      </div>
    </form>
  </section>

  <section class="surface-card" aria-labelledby="infra-heading">
    <div class="card-header">
      <h2 id="infra-heading">Estado de persistencia y seguridad</h2>
    </div>
    <dl class="stack-sm" style="margin:0; font-size:0.88rem;">
      <div>
        <dt class="field-hint">Unidades por defecto (V1)</dt>
        <dd style="margin:0; font-weight:600;">Masa: gramos (g) · Temperatura: grados Celsius (°C) · Ratio: 1:x · Tiempo: s / mm:ss</dd>
      </div>
      <div>
        <dt class="field-hint">Motor de base de datos relacional activo</dt>
        <dd style="margin:0; font-weight:600;"><c:out value="${dbEngineLabel}"/> (<code class="num"><c:out value="${dbUrlSummary}"/></code>)</dd>
      </div>
      <div>
        <dt class="field-hint">Versión del esquema SQL</dt>
        <dd style="margin:0; font-weight:600;" class="tabular">V<c:out value="${schemaVersion}"/></dd>
      </div>
      <div>
        <dt class="field-hint">Autenticación activa</dt>
        <dd style="margin:0; font-weight:600;">
          JWT HS256 en cookie HttpOnly (usuario: <c:out value="${currentUser.email}"/>, versión de token #<span class="tabular"><c:out value="${currentUser.tokenVersion}"/></span>)
        </dd>
      </div>
    </dl>
  </section>
</div>

<c:if test="${not empty backupSectionEnabled}">
  <%@ include file="/WEB-INF/views/settings/backup_section.jspf" %>
</c:if>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
