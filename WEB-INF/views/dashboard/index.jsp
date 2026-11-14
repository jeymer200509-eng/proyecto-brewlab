<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/views/layout/immersion-header.jspf" %>

<div class="workspace-heading"><span class="eyebrow">ESTACIÓN 01 / CONTROL DEL LABORATORIO</span><span class="reference-date">Fecha de referencia <strong><c:out value="${todayFormatted}"/></strong></span></div>
<div class="control-desk">
  <section class="brew-station" aria-labelledby="station-heading">
    <div class="station-copy">
      <span class="eyebrow ion">OBSERVAR. PREPARAR. COMPRENDER.</span>
      <h1 id="station-heading">Cada café,<br>un descubrimiento.</h1>
      <p class="station-intro">Del origen a la taza. Un espacio para afinar su técnica y entender cada extracción.</p>
    </div>
    <figure class="extraction-figure">
      <svg class="extraction-diagram" viewBox="0 0 480 470" role="img" aria-labelledby="extraction-title extraction-desc">
        <title id="extraction-title">Esquema de extracción por filtrado</title>
        <desc id="extraction-desc">Ilustración de un filtro cónico sobre un recipiente. No representa mediciones ni el progreso de una sesión.</desc>
        <defs>
          <linearGradient id="filter-metal" x1="0" x2="1"><stop stop-color="#203039"/><stop offset=".45" stop-color="#52646c" stop-opacity=".5"/><stop offset=".7" stop-color="#112027"/><stop offset="1" stop-color="#52636a"/></linearGradient>
          <linearGradient id="vessel-glass" x1="0" x2="1"><stop stop-color="#92dbe0" stop-opacity=".13"/><stop offset=".5" stop-color="#92dbe0" stop-opacity=".02"/><stop offset="1" stop-color="#92dbe0" stop-opacity=".17"/></linearGradient>
          <linearGradient id="coffee-liquid" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#c68a52" stop-opacity=".65"/><stop offset="1" stop-color="#764226" stop-opacity=".14"/></linearGradient>
        </defs>
        <g fill="none" stroke="#36515c" stroke-width="1">
          <ellipse cx="240" cy="245" rx="185" ry="169" stroke-dasharray="2 9"/>
          <ellipse cx="240" cy="245" rx="163" ry="150" opacity=".45"/>
          <path d="M240 36v37M240 416v24M35 245h33M412 245h33M96 91l17 17M369 387l17 17"/>
          <path d="M103 167H55v-32M345 290h70v36M183 359h-65v27"/>
        </g>
        <g font-family="monospace" font-size="10" fill="#a3b3bc" letter-spacing="2"><text x="36" y="124">FILTRO</text><text x="369" y="344">EXTRACCIÓN</text><text x="64" y="407">RECOLECCIÓN</text></g>
        <ellipse cx="240" cy="399" rx="105" ry="13" fill="#000" opacity=".35"/>
        <path d="M196 280L176 367Q175 391 240 393Q305 391 304 367L284 280Z" fill="url(#vessel-glass)" stroke="#66858c"/>
        <path d="M294 305Q342 305 328 344Q321 358 302 355" fill="none" stroke="#66858c" stroke-width="3"/>
        <path d="M184 347Q240 358 296 347L301 369Q298 388 240 388Q182 388 180 369Z" fill="url(#coffee-liquid)"/>
        <ellipse cx="240" cy="347" rx="56" ry="9" fill="#ae7748" opacity=".3"/>
        <path d="M191 360v12M190 330h12M195 307h12" stroke="#a5c2c8" opacity=".5"/>
        <ellipse cx="240" cy="280" rx="44" ry="10" fill="#14232a" stroke="#66858c"/>
        <path d="M130 145L214 258Q240 277 266 258L350 145Z" fill="url(#filter-metal)" stroke="#8eafb7" stroke-width="1.2"/>
        <g fill="none" stroke="#8eafb7" opacity=".38"><path d="M149 150l75 102M179 151l51 103M209 151l28 105M240 153v105M271 151l-28 105M301 151l-51 103M331 150l-75 102"/></g>
        <ellipse cx="240" cy="145" rx="110" ry="26" fill="#1a2b32" stroke="#b1cbd0" stroke-width="1.5"/>
        <ellipse cx="240" cy="143" rx="96" ry="18" fill="#34291f" stroke="#8c6449"/>
        <ellipse cx="240" cy="139" rx="81" ry="10" fill="#ae8058" opacity=".16"/>
        <path d="M240 93v33" stroke="#85e4e8" stroke-width="2" stroke-linecap="round"/>
        <circle cx="240" cy="83" r="3" fill="#85e4e8"/>
        <path d="M240 265v37" stroke="#edb17c" stroke-dasharray="3 7" stroke-width="2"/>
        <circle cx="240" cy="315" r="3" fill="#edb17c"/>
      </svg>
      <figcaption>ESTUDIO DE EXTRACCIÓN <span>/</span> ESQUEMA ILUSTRATIVO</figcaption>
    </figure>
    <div class="session-console">
      <c:choose>
        <c:when test="${not empty activeBrewSession}">
          <div class="session-heading"><span class="eyebrow copper">SESIÓN ABIERTA</span><span class="session-state"><c:out value="${activeBrewSession.statusLabel}"/></span></div>
          <h2><c:out value="${activeBrewSession.plannedRecipeName}"/></h2>
          <p class="session-method"><c:out value="${activeBrewSession.plannedMethod}"/> · Parámetros previstos<c:if test="${activeBrewSession.demo}"> · [DEMO]</c:if></p>
          <dl class="planned-readings"><div><dt>Dosis</dt><dd><c:out value="${activeBrewSession.plannedDoseDisplay}"/></dd></div><div><dt>Agua</dt><dd><c:out value="${activeBrewSession.plannedWaterDisplay}"/></dd></div><div><dt>Temperatura</dt><dd><c:out value="${activeBrewSession.plannedTempDisplay}"/></dd></div></dl>
          <c:if test="${not empty applicationScope.navPreparacionEnabled}"><a class="btn btn-copper" href="${pageContext.request.contextPath}/app/preparacion?sessionId=${fn:escapeXml(activeBrewSession.id)}">Abrir sesión <span aria-hidden="true">↗</span></a></c:if>
        </c:when>
        <c:otherwise>
          <div class="session-heading"><span class="eyebrow copper">SU PRÓXIMA EXTRACCIÓN</span><span class="session-state">Sin sesión abierta</span></div>
          <h2>Una nueva pregunta.<br>Una nueva taza.</h2>
          <p>Elija un protocolo y registre su preparación paso a paso.</p>
          <c:if test="${not empty applicationScope.navPreparacionEnabled}"><a class="btn btn-copper" href="${pageContext.request.contextPath}/app/preparacion">Preparar café <span aria-hidden="true">↗</span></a></c:if>
          <c:if test="${not empty applicationScope.navEquiposEnabled}"><a class="quiet-link" href="${pageContext.request.contextPath}/app/recetas">Explorar recetas <span aria-hidden="true">→</span></a></c:if>
        </c:otherwise>
      </c:choose>
    </div>
    <div class="station-footnote"><span class="instrument-cross" aria-hidden="true">+</span> La precisión comienza con lo que observa.</div>
  </section>
  <aside class="lab-register" aria-labelledby="register-heading">
    <div class="register-heading"><span class="eyebrow">REGISTRO DEL LABORATORIO</span><h2 id="register-heading">Su trabajo,<br>en perspectiva.</h2><p>Totales registrados en su cuenta.</p></div>
    <dl class="register-readings">
      <div><dt>Lotes de café</dt><dd><c:out value="${dashboardStats.coffeeLotsCount}" default="—"/></dd></div>
      <div><dt>Lotes de tostión</dt><dd><c:out value="${dashboardStats.roastBatchesCount}" default="—"/></dd></div>
      <div><dt>Recetas</dt><dd><c:out value="${dashboardStats.recipesCount}" default="—"/></dd></div>
      <div class="reading-emphasis"><dt>Sesiones de preparación</dt><dd><c:out value="${dashboardStats.sessionsCount}" default="—"/></dd></div>
      <div><dt>Evaluaciones sensoriales</dt><dd><c:out value="${dashboardStats.assessmentsCount}" default="—"/></dd></div>
      <div><dt>Experimentos</dt><dd><c:out value="${dashboardStats.experimentsCount}" default="—"/></dd></div>
    </dl>
    <c:if test="${not empty applicationScope.navAnalisisEnabled}"><a class="register-link" href="${pageContext.request.contextPath}/app/analisis"><span>Consultar historial<br><small>Analizar y comparar sesiones</small></span><span aria-hidden="true">↗</span></a></c:if>
    <p class="register-note">Cada registro conserva su contexto. Los totales incluyen datos de demostración si los ha cargado.</p>
  </aside>
</div>
<section class="research-path" aria-labelledby="path-heading">
  <div class="path-heading"><h2 id="path-heading">Siga el hilo de su café.</h2><span class="eyebrow">DEL ORIGEN AL CRITERIO</span></div>
  <div class="path-steps">
    <c:if test="${not empty applicationScope.navCafesEnabled}"><a class="path-step" href="${pageContext.request.contextPath}/app/cafes"><span class="step-number">01 / MUESTRAS</span><h3>Conozca el origen <span aria-hidden="true">↗</span></h3><p>Lotes, procesos y tostiones. La identidad de cada café.</p></a></c:if>
    <c:if test="${not empty applicationScope.navCatacionEnabled}"><a class="path-step" href="${pageContext.request.contextPath}/app/catacion"><span class="step-number">02 / PERCEPCIÓN</span><h3>Explore lo sensorial <span aria-hidden="true">↗</span></h3><p>Describa aromas y sabores. Separe intensidad y preferencia.</p></a></c:if>
    <c:if test="${not empty applicationScope.navAnalisisEnabled}"><a class="path-step" href="${pageContext.request.contextPath}/app/experimentos"><span class="step-number">03 / INVESTIGACIÓN</span><h3>Ponga a prueba una idea <span aria-hidden="true">↗</span></h3><p>Una hipótesis, una variable y un nuevo aprendizaje.</p></a></c:if>
  </div>
</section>
<%@ include file="/WEB-INF/views/layout/immersion-footer.jspf" %>
