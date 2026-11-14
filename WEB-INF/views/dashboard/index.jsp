<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageHeading" value="Panel de Laboratorio"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="surface-card" aria-labelledby="ciclo-heading">
  <div class="card-header">
    <div>
      <h2 id="ciclo-heading">Estación de trabajo — Ciclo de aprendizaje del café</h2>
      <p style="color: var(--text-secondary); font-size: 0.9rem; margin-bottom: 0;">
        Fecha local de referencia: <strong class="tabular"><c:out value="${todayFormatted}"/></strong> ·
        Motor: <strong><c:out value="${dbEngineLabel}"/></strong> (esquema v<span class="tabular"><c:out value="${schemaVersion}"/></span>)
      </p>
    </div>
    <div class="cluster">
      <a class="btn btn-secondary btn-sm" href="${pageContext.request.contextPath}/app/ajustes">Configuración del laboratorio</a>
    </div>
  </div>

  <div class="grid-3">
    <div class="surface-card" style="background: var(--bg-subtle);">
      <h3>1. Trazabilidad y Lotes</h3>
      <p style="font-size: 0.86rem; color: var(--text-secondary);">
        Separa la identidad del café verde (<code>CoffeeLot</code>) de cada evento de tostión (<code>RoastBatch</code>) y calcula los días desde tostión dinámicamente.
      </p>
      <c:if test="${not empty applicationScope.navCafesEnabled}">
        <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/app/cafes">Abrir Biblioteca de Cafés</a>
      </c:if>
    </div>

    <div class="surface-card" style="background: var(--bg-subtle);">
      <h3>2. Recetas y Preparación Guiada</h3>
      <p style="font-size: 0.86rem; color: var(--text-secondary);">
        Fórmula determinista: <code class="num"><c:out value="${sampleRatioFormula}"/></code>. Cronómetro recuperable basado en marcas temporales reales.
      </p>
      <c:if test="${not empty applicationScope.navPreparacionEnabled}">
        <div class="cluster">
          <a class="btn btn-secondary btn-sm" href="${pageContext.request.contextPath}/app/recetas">Recetas</a>
          <a class="btn btn-caramel btn-sm" href="${pageContext.request.contextPath}/app/preparacion">Iniciar Preparación</a>
        </div>
      </c:if>
    </div>

    <div class="surface-card" style="background: var(--bg-subtle);">
      <h3>3. Evaluación Sensorial y Comparación</h3>
      <p style="font-size: 0.86rem; color: var(--text-secondary);">
        Taxonomía jerárquica propia, separación estricta entre descripción, intensidad y preferencia afectiva, y comparador honesto de sesiones.
      </p>
      <c:if test="${not empty applicationScope.navCatacionEnabled}">
        <div class="cluster">
          <a class="btn btn-sage btn-sm" href="${pageContext.request.contextPath}/app/catacion">Sensory Lab</a>
          <a class="btn btn-secondary btn-sm" href="${pageContext.request.contextPath}/app/analisis">Comparar Sesiones</a>
        </div>
      </c:if>
    </div>
  </div>
</section>

<c:if test="${not empty dashboardStats}">
  <section class="surface-card" aria-labelledby="resumen-heading">
    <div class="card-header">
      <h2 id="resumen-heading">Estado actual del laboratorio</h2>
      <c:if test="${not empty activeBrewSession}">
        <a class="btn btn-caramel btn-sm"
           href="${pageContext.request.contextPath}/app/preparacion?sessionId=${fn:escapeXml(activeBrewSession.id)}">
          Reanudar sesión activa (<c:out value="${activeBrewSession.status}"/>)
        </a>
      </c:if>
    </div>
    <div class="grid-4">
      <div class="surface-card">
        <div class="field-hint">Lotes de café</div>
        <div class="display-title tabular"><c:out value="${dashboardStats.coffeeLotsCount}"/></div>
      </div>
      <div class="surface-card">
        <div class="field-hint">Lotes de tostión</div>
        <div class="display-title tabular"><c:out value="${dashboardStats.roastBatchesCount}"/></div>
      </div>
      <div class="surface-card">
        <div class="field-hint">Recetas activas</div>
        <div class="display-title tabular"><c:out value="${dashboardStats.recipesCount}"/></div>
      </div>
      <div class="surface-card">
        <div class="field-hint">Sesiones / Cataciones</div>
        <div class="display-title tabular">
          <c:out value="${dashboardStats.sessionsCount}"/> / <c:out value="${dashboardStats.assessmentsCount}"/>
        </div>
      </div>
    </div>
  </section>
</c:if>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
