<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageHeading" value="Laboratorio sensorial"/>
<c:set var="moduleKicker" value="06 / MAPA DE PERCEPCIÓN"/>
<c:set var="moduleDescription" value="Explore familias de sabores y registre intensidades. Su preferencia tiene su propio espacio."/>
<c:set var="moduleAnchor" value="sensory-workspace"/>
<c:set var="moduleAction" value="Evaluar una taza"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<details class="module-guidance"><summary>Guía de lectura y criterios del módulo</summary>
  <div class="card-header" style="margin-bottom: 0;">
    <div>
      <div class="field-hint">Módulos 5 y 6 · Sensory Lab &amp; Flavor Explorer</div>
      <h2 style="margin-top: 0.25rem;">Evaluación Sensorial y Explorador Jerárquico</h2>
      <p class="field-hint" style="margin-bottom: 0;">
        Registra atributos descriptivos orientativos, explora descriptores jerárquicos (<span class="mono">Familia → Subfamilia → Descriptor</span>)
        con intensidad y escala separadas, y conserva el agrado subjetivo en un bloque afectivo independiente.
      </p>
    </div>
    <div class="cluster">
      <span class="badge badge-measured" title="Esta evaluación simplificada de trabajo no sustituye ni representa el protocolo oficial CVA/SCA">
        Evaluación Interna de Laboratorio V1 (No es CVA oficial)
      </span>
      <span class="badge badge-planned mono"><c:out value="${taxonomyVersion}" /></span>
    </div>
  </div>
</details>

<div class="alert alert-info flash flash-info" role="note" style="margin-bottom: 1.25rem;">
  <div>
    <strong>Atribución y licencia del vocabulario:</strong>
    <c:out value="${taxonomyAttribution}" />. Los valores no evaluados se almacenan como <span class="mono">null</span> (<span class="mono">—</span>), nunca como <span class="mono">0</span>.
  </div>
</div>

<form id="sensory-workspace" method="post" action="${pageContext.request.contextPath}/app/catacion" data-once="true">
  <input type="hidden" name="_csrf" value="${csrfToken}" />
  <input type="hidden" name="action" value="createAssessment" />

  <div class="sensory-workspace">
    <!-- COLUMNA IZQUIERDA: Vínculo de trazabilidad + Atributos Descriptivos + Bloque Afectivo Separado -->
    <div class="stack-md stack">
      <!-- 1. Trazabilidad y contexto de muestra -->
      <section class="surface-card card">
        <div class="card-header">
          <div>
            <span class="badge badge-measured">1 · Muestra y Trazabilidad</span>
            <h2 class="card-title" style="margin-top: 0.35rem;">Vínculo con Lote, Tostión y Sesión</h2>
          </div>
        </div>

        <div class="form-grid form-grid-2">
          <div class="field">
            <label class="field-label" for="evaluatorName">Catador / Evaluador *</label>
            <input class="input" id="evaluatorName" name="evaluatorName" type="text" required
                   value="<c:out value='${currentUser.displayName}' />" />
          </div>
          <div class="field">
            <label class="field-label" for="assessedOn">Fecha de evaluación *</label>
            <input class="input" id="assessedOn" name="assessedOn" type="date" required
                   value="<c:out value='${todayIso}' />" />
          </div>
        </div>

        <div class="field" style="margin-top: 0.75rem;">
          <label class="field-label" for="brewSessionId">Sesión de preparación vinculada (opcional)</label>
          <select class="input" id="brewSessionId" name="brewSessionId">
            <option value="">— Cata abierta / Sin sesión específica —</option>
            <c:forEach var="bs" items="${brewSessions}">
              <option value="${bs.id}" <c:if test="${bs.id == preselectedSessionId}">selected</c:if>>
                <c:out value="${bs.plannedMethod}" /> · <c:out value="${bs.plannedRecipeName}" />
                (<c:out value="${bs.statusLabel}" /> — Ratio real <c:out value="${bs.actualRatioDisplay}" />)
              </option>
            </c:forEach>
          </select>
        </div>

        <div class="form-grid form-grid-2" style="margin-top: 0.75rem;">
          <div class="field">
            <label class="field-label" for="coffeeLotId">Lote de café</label>
            <select class="input" id="coffeeLotId" name="coffeeLotId">
              <option value="">— Automático desde tueste/sesión o sin lote —</option>
              <c:forEach var="lot" items="${coffeeLots}">
                <option value="${lot.id}" <c:if test="${lot.id == preselectedLotId}">selected</c:if>>
                  [<c:out value="${lot.code}" />] <c:out value="${lot.name}" />
                </option>
              </c:forEach>
            </select>
          </div>
          <div class="field">
            <label class="field-label" for="roastBatchId">Lote de tostión</label>
            <select class="input" id="roastBatchId" name="roastBatchId">
              <option value="">— Opcional —</option>
              <c:forEach var="rb" items="${roastBatches}">
                <option value="${rb.id}" <c:if test="${rb.id == preselectedBatchId}">selected</c:if>>
                  [<c:out value="${rb.code}" />] <c:out value="${rb.roastLevel}" /> (<c:out value="${rb.roastDateDisplay}" />)
                </option>
              </c:forEach>
            </select>
          </div>
        </div>
      </section>

      <!-- 2. Bloque Descriptivo Orientativo -->
      <section class="surface-card card">
        <div class="card-header">
          <div>
            <span class="badge badge-measured">2 · Bloque Descriptivo</span>
            <h2 class="card-title" style="margin-top: 0.35rem;">Atributos Orientativos de Intensidad</h2>
            <p class="field-hint card-subtitle" style="margin-bottom: 0;">
              Evalúa la intensidad percibida en la escala elegida. Deja en blanco cualquier atributo no evaluado (<span class="mono">null</span>).
            </p>
          </div>
          <div class="field" style="min-width: 145px;">
            <label class="field-label" for="attributeScale">Escala explícita</label>
            <select class="input" id="attributeScale" name="attributeScale">
              <option value="1-5" <c:if test="${settings.defaultSensoryScale == '1-5'}">selected</c:if>>Escala 1 a 5</option>
              <option value="0-10" <c:if test="${settings.defaultSensoryScale == '0-10'}">selected</c:if>>Escala 0 a 10</option>
              <option value="1-15">Escala 1 a 15</option>
            </select>
          </div>
        </div>

        <div class="form-grid form-grid-2">
          <div class="field">
            <label class="field-label" for="fragranceAroma">Fragancia / Aroma</label>
            <input class="input" id="fragranceAroma" name="fragranceAroma" type="number" step="0.25" min="0" max="15" placeholder="No evaluado (—)" />
          </div>
          <div class="field">
            <label class="field-label" for="flavor">Sabor</label>
            <input class="input" id="flavor" name="flavor" type="number" step="0.25" min="0" max="15" placeholder="No evaluado (—)" />
          </div>
          <div class="field">
            <label class="field-label" for="acidity">Acidez</label>
            <input class="input" id="acidity" name="acidity" type="number" step="0.25" min="0" max="15" placeholder="No evaluado (—)" />
          </div>
          <div class="field">
            <label class="field-label" for="sweetness">Dulzor</label>
            <input class="input" id="sweetness" name="sweetness" type="number" step="0.25" min="0" max="15" placeholder="No evaluado (—)" />
          </div>
          <div class="field">
            <label class="field-label" for="bitterness">Amargor</label>
            <input class="input" id="bitterness" name="bitterness" type="number" step="0.25" min="0" max="15" placeholder="No evaluado (—)" />
          </div>
          <div class="field">
            <label class="field-label" for="bodyTexture">Cuerpo / Textura</label>
            <input class="input" id="bodyTexture" name="bodyTexture" type="number" step="0.25" min="0" max="15" placeholder="No evaluado (—)" />
          </div>
          <div class="field">
            <label class="field-label" for="aftertaste">Posgusto</label>
            <input class="input" id="aftertaste" name="aftertaste" type="number" step="0.25" min="0" max="15" placeholder="No evaluado (—)" />
          </div>
          <div class="field">
            <label class="field-label" for="balance">Balance estructural</label>
            <input class="input" id="balance" name="balance" type="number" step="0.25" min="0" max="15" placeholder="No evaluado (—)" />
          </div>
        </div>

        <div class="field" style="margin-top: 0.75rem;">
          <label class="field-label" for="descriptiveNotes">Notas descriptivas libres (evolución en taza de caliente a frío, textura, limpieza)</label>
          <textarea class="input" id="descriptiveNotes" name="descriptiveNotes" rows="3"
                    placeholder="Ej.: En caliente predomina jazmín y durazno; al enfriar a 45 °C emerge acidez málica jugosa y posgusto a panela..."></textarea>
        </div>
      </section>

      <!-- 3. Bloque Afectivo Estrictamente Separado -->
      <section class="surface-card card" style="border-color: rgba(155, 77, 150, 0.35);">
        <div class="card-header">
          <div>
            <span class="badge badge-preference badge-affective">3 · Bloque Afectivo Separado</span>
            <h2 class="card-title" style="margin-top: 0.35rem;">Preferencia y Agrado del Evaluador</h2>
            <p class="field-hint card-subtitle" style="margin-bottom: 0;">
              Separado del análisis descriptivo: mide cuánto te gustó esta taza y si repetirías la receta en este contexto.
            </p>
          </div>
        </div>

        <div class="form-grid form-grid-3">
          <div class="field">
            <label class="field-label" for="overallLiking">Agrado general</label>
            <input class="input" id="overallLiking" name="overallLiking" type="number" step="0.5" min="1" max="100"
                   placeholder="Opcional (—)" />
          </div>
          <div class="field">
            <label class="field-label" for="likingScale">Escala hedónica</label>
            <select class="input" id="likingScale" name="likingScale">
              <option value="1-9">Hedónica 1 a 9</option>
              <option value="1-5">Agrado 1 a 5</option>
              <option value="0-10">Agrado 0 a 10</option>
            </select>
          </div>
          <div class="field">
            <label class="field-label" for="wouldRepeat">¿Repetirías esta receta?</label>
            <select class="input" id="wouldRepeat" name="wouldRepeat">
              <option value="">— Sin definir —</option>
              <option value="YES">Sí, repetir igual</option>
              <option value="MAYBE">Repetir con ajustes</option>
              <option value="NO">No repetir</option>
            </select>
          </div>
        </div>

        <div class="form-grid form-grid-2" style="margin-top: 0.75rem;">
          <div class="field">
            <label class="field-label" for="affectiveContext">Contexto de consumo / Momento</label>
            <input class="input" id="affectiveContext" name="affectiveContext" type="text"
                   placeholder="Ej.: Taza matutina, calibración de barra, comparativa" />
          </div>
          <div class="field">
            <label class="field-label" for="affectiveNotes">Impresión subjetiva / Preferencia personal</label>
            <input class="input" id="affectiveNotes" name="affectiveNotes" type="text"
                   placeholder="Ej.: Muy placentera al templar; preferiría un punto más de cuerpo" />
          </div>
        </div>

        <div style="margin-top: 1.1rem; display: flex; justify-content: flex-end;">
          <button type="submit" class="btn btn-primary">Guardar evaluación descriptiva y afectiva</button>
        </div>
      </section>
    </div>

    <!-- COLUMNA DERECHA: Flavor Explorer (Rueda SVG original + Búsqueda + Lista accesible para teclado/lector) -->
    <div class="stack-md stack">
      <section class="surface-card card">
        <div class="card-header">
          <div>
            <span class="badge badge-planned">4 · Flavor Explorer Original</span>
            <h2 class="card-title" style="margin-top: 0.35rem;">Rueda Jerárquica Concéntrica y Lista Accesible</h2>
            <p class="field-hint card-subtitle" style="margin-bottom: 0;">
              Haz clic o pulsa <span class="mono">Enter</span> sobre un sector de <strong>Familia</strong> (anillo interior)
              o <strong>Subfamilia</strong> (anillo exterior), o usa el buscador y la lista accesible inferior.
            </p>
          </div>
          <div class="field" style="min-width: 140px;">
            <label class="field-label" for="descriptorScale">Escala de descriptor</label>
            <select class="input" id="descriptorScale" name="descriptorScale">
              <option value="1-5">Intensidad 1 a 5</option>
              <option value="1-3">Baja / Media / Alta (1-3)</option>
              <option value="0-10">Intensidad 0 a 10</option>
            </select>
          </div>
        </div>

        <!-- Rueda SVG concéntrica generada en servidor -->
        <div style="display: flex; flex-direction: column; align-items: center; margin-bottom: 1rem;">
          <svg id="brewlab-flavor-wheel" viewBox="0 0 520 520"
               role="group" aria-label="Rueda sensorial jerárquica original de BrewLab: Familias y Subfamilias"
               style="width: 100%; max-width: 430px; height: auto; user-select: none;">
            <!-- Anillo exterior: Subfamilias -->
            <g aria-label="Subfamilias aromáticas">
              <c:forEach var="sec" items="${subfamilySectors}">
                <g class="wheel-sector" role="button" tabindex="0"
                   data-filter-type="subfamily"
                   data-family="<c:out value='${sec.parentKey}' />"
                   data-subfamily="<c:out value='${sec.key}' />"
                   aria-label="Subfamilia ${sec.label} de ${sec.parentKey}"
                   style="cursor: pointer;">
                  <path d="${sec.pathData}" fill="${sec.colorHex}" fill-opacity="0.76"
                        stroke="#FAF7F2" stroke-width="1.5" />
                  <text x="${sec.labelX}" y="${sec.labelY}"
                        transform="rotate(${sec.labelRotation} ${sec.labelX} ${sec.labelY})"
                        text-anchor="middle" dominant-baseline="middle"
                        fill="#FFFFFF" font-size="9.5" font-weight="600"
                        style="pointer-events: none;">
                    <c:out value="${sec.shortLabel}" />
                  </text>
                </g>
              </c:forEach>
            </g>

            <!-- Anillo interior: Familias -->
            <g aria-label="Familias aromáticas principales">
              <c:forEach var="fsec" items="${familySectors}">
                <g class="wheel-sector" role="button" tabindex="0"
                   data-filter-type="family"
                   data-family="<c:out value='${fsec.key}' />"
                   aria-label="Familia ${fsec.label}"
                   style="cursor: pointer;">
                  <path d="${fsec.pathData}" fill="${fsec.colorHex}"
                        stroke="#FAF7F2" stroke-width="2" />
                  <text x="${fsec.labelX}" y="${fsec.labelY}"
                        transform="rotate(${fsec.labelRotation} ${fsec.labelX} ${fsec.labelY})"
                        text-anchor="middle" dominant-baseline="middle"
                        fill="#FFFFFF" font-size="10.5" font-weight="700"
                        style="pointer-events: none;">
                    <c:out value="${fsec.shortLabel}" />
                  </text>
                </g>
              </c:forEach>
            </g>

            <!-- Núcleo central para reiniciar filtro -->
            <g id="wheel-reset-center" role="button" tabindex="0"
               aria-label="Mostrar todas las familias y descriptores" style="cursor: pointer;">
              <circle cx="260" cy="260" r="62" fill="#231915" stroke="#FAF7F2" stroke-width="2" />
              <text x="260" y="253" text-anchor="middle" fill="#FAF7F2" font-size="11" font-weight="700">
                BREWLAB
              </text>
              <text x="260" y="269" text-anchor="middle" fill="#D5C7B8" font-size="9" font-weight="500">
                Ver todo (<c:out value="${fn:length(descriptors)}" />)
              </text>
            </g>
          </svg>
        </div>

        <!-- Buscador y estado de filtro activo -->
        <div class="form-grid form-grid-2" style="align-items: end; margin-bottom: 0.85rem;">
          <div class="field">
            <label class="field-label" for="flavor-search-input">Buscar por familia, subfamilia o descriptor (accesible por teclado)</label>
            <input class="input" id="flavor-search-input" type="search"
                   placeholder="Ej.: jazmín, cítricos, panela, cacao, mora..."
                   aria-controls="descriptor-accessible-list" />
          </div>
          <div style="display: flex; align-items: center; justify-content: space-between; gap: 0.5rem;">
            <span id="flavor-active-filter" class="badge badge-planned" aria-live="polite">
              Mostrando todas las familias (<c:out value="${fn:length(descriptors)}" />)
            </span>
            <button type="button" id="flavor-clear-filter-btn" class="btn btn-secondary btn-sm">
              Limpiar filtro
            </button>
          </div>
        </div>

        <!-- Alternativa en lista jerárquica accesible para teclado y lectores de pantalla -->
        <div id="descriptor-accessible-list" role="region"
             aria-label="Lista jerárquica accesible de descriptores sensoriales"
             style="max-height: 440px; overflow-y: auto; border: 1px solid var(--border-subtle); border-radius: var(--radius-md); padding: 0.65rem;">
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th style="width: 38px;">Sel.</th>
                  <th>Jerarquía (<span class="mono">Familia › Subfamilia › Descriptor</span>)</th>
                  <th style="width: 115px;">Intensidad</th>
                  <th style="width: 150px;">Nota específica</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="desc" items="${descriptors}">
                  <tr class="descriptor-row"
                      data-family="<c:out value='${desc.family}' />"
                      data-subfamily="<c:out value='${desc.subfamily}' />"
                      data-search="<c:out value='${fn:toLowerCase(desc.family)} ${fn:toLowerCase(desc.subfamily)} ${fn:toLowerCase(desc.term)}' />">
                    <td>
                      <input type="checkbox"
                             id="chk_${desc.id}"
                             name="descriptorIds"
                             value="${desc.id}"
                             aria-label="Seleccionar descriptor ${desc.term} (${desc.family}, ${desc.subfamily})" />
                    </td>
                    <td>
                      <label for="chk_${desc.id}" style="cursor: pointer; display: flex; align-items: center; gap: 0.45rem;">
                        <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${desc.colorHex}; flex-shrink: 0;"></span>
                        <span>
                          <span class="muted" style="font-size: 0.78rem;">
                            <c:out value="${desc.family}" /> › <c:out value="${desc.subfamily}" /> ›
                          </span>
                          <strong><c:out value="${desc.term}" /></strong>
                          <c:if test="${desc.custom}">
                            <span class="badge badge-estimated" style="margin-left: 0.25rem;">Personalizado</span>
                          </c:if>
                        </span>
                      </label>
                    </td>
                    <td>
                      <input class="input" type="number"
                             name="intensity_${desc.id}"
                             step="0.5" min="0.5" max="10"
                             placeholder="—"
                             aria-label="Intensidad para ${desc.term}"
                             style="padding: 0.3rem 0.45rem; font-size: 0.85rem;" />
                    </td>
                    <td>
                      <input class="input" type="text"
                             name="note_${desc.id}"
                             placeholder="Opcional"
                             aria-label="Nota para ${desc.term}"
                             style="padding: 0.3rem 0.45rem; font-size: 0.85rem;" />
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  </div>
</form>

<!-- Crear descriptor personalizado -->
<section class="surface-card card" style="margin-bottom: 1.5rem;">
  <div class="card-header">
    <div>
      <span class="badge badge-estimated">Vocabulario Ampliable</span>
      <h2 class="card-title" style="margin-top: 0.35rem;">Añadir Descriptor Personalizado a la Taxonomía</h2>
      <p class="field-hint card-subtitle" style="margin-bottom: 0;">
        Amplía el explorador con descriptores propios de tu región o laboratorio respetando la jerarquía <span class="mono">Familia → Subfamilia → Descriptor</span>.
      </p>
    </div>
  </div>
  <form method="post" action="${pageContext.request.contextPath}/app/catacion" data-once="true">
    <input type="hidden" name="_csrf" value="${csrfToken}" />
    <input type="hidden" name="action" value="createCustomDescriptor" />
    <div class="form-grid form-grid-4" style="align-items: end;">
      <div class="field">
        <label class="field-label" for="customFamily">Familia *</label>
        <input class="input" id="customFamily" name="family" type="text" required
               placeholder="Ej.: Frutal Luminoso" />
      </div>
      <div class="field">
        <label class="field-label" for="customSubfamily">Subfamilia *</label>
        <input class="input" id="customSubfamily" name="subfamily" type="text" required
               placeholder="Ej.: Frutas andinas" />
      </div>
      <div class="field">
        <label class="field-label" for="customTerm">Descriptor / Término *</label>
        <input class="input" id="customTerm" name="term" type="text" required
               placeholder="Ej.: Lulo / Naranjilla" />
      </div>
      <div style="display: flex; gap: 0.5rem; align-items: flex-end;">
        <div class="field" style="width: 90px;">
          <label class="field-label" for="customColor">Color</label>
          <input id="customColor" name="colorHex" type="color" value="#C86D2D" style="height: 38px; padding: 2px;" />
        </div>
        <button type="submit" class="btn btn-secondary">Crear descriptor</button>
      </div>
    </div>
  </form>
</section>

<!-- Historial de evaluaciones sensoriales registradas -->
<section class="surface-card card">
  <div class="card-header">
    <div>
      <span class="badge badge-measured">Registro Histórico</span>
      <h2 class="card-title" style="margin-top: 0.35rem;">
        Evaluaciones Sensoriales Registradas (<c:out value="${fn:length(assessments)}" />)
      </h2>
    </div>
  </div>

  <c:choose>
    <c:when test="${empty assessments}">
      <div class="empty-state">
        <p>Aún no hay evaluaciones sensoriales registradas. Completa el formulario superior tras una sesión de preparación o cata.</p>
      </div>
    </c:when>
    <c:otherwise>
      <div class="stack-md stack">
        <c:forEach var="assess" items="${assessments}">
          <c:set var="asLot" value="${lotMap[assess.coffeeLotId]}" />
          <c:set var="asBatch" value="${batchMap[assess.roastBatchId]}" />
          <c:set var="asSession" value="${sessionMap[assess.brewSessionId]}" />
          <article class="surface-card card" style="background: var(--bg-subtle);">
            <div class="card-header">
              <div>
                <div class="cluster" style="margin-bottom: 0.3rem;">
                  <span class="badge badge-measured">Escala descriptiva: <c:out value="${assess.attributeScale}" /></span>
                  <span class="field-hint mono"><c:out value="${assess.assessedOnDisplay}" /> · Evaluador: <c:out value="${assess.evaluatorName}" /></span>
                  <c:if test="${not empty asLot}">
                    <span class="badge badge-planned">[<c:out value="${asLot.code}" />] <c:out value="${asLot.name}" /></span>
                  </c:if>
                  <c:if test="${not empty asBatch}">
                    <span class="badge badge-planned">Tueste <c:out value="${asBatch.code}" /> (<c:out value="${asBatch.roastLevel}" />)</span>
                  </c:if>
                  <c:if test="${not empty asSession}">
                    <a class="badge badge-estimated" href="${pageContext.request.contextPath}/app/preparacion?sessionId=${asSession.id}">
                      Sesión: <c:out value="${asSession.plannedMethod}" /> (<c:out value="${asSession.actualRatioDisplay}" />)
                    </a>
                  </c:if>
                </div>
              </div>
              <form method="post" action="${pageContext.request.contextPath}/app/catacion" style="margin: 0;">
                <input type="hidden" name="_csrf" value="${csrfToken}" />
                <input type="hidden" name="action" value="deleteAssessment" />
                <input type="hidden" name="assessmentId" value="${assess.id}" />
                <button type="submit" class="btn btn-secondary btn-sm">Eliminar</button>
              </form>
            </div>

            <!-- Tabla de atributos descriptivos (null = —) -->
            <div class="table-wrap" style="margin-bottom: 0.75rem;">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Fragancia/Aroma</th>
                    <th>Sabor</th>
                    <th>Acidez</th>
                    <th>Dulzor</th>
                    <th>Amargor</th>
                    <th>Cuerpo/Textura</th>
                    <th>Posgusto</th>
                    <th>Balance</th>
                  </tr>
                </thead>
                <tbody class="num">
                  <tr>
                    <td><c:out value="${assess.fragranceAromaDisplay}" /></td>
                    <td><c:out value="${assess.flavorDisplay}" /></td>
                    <td><c:out value="${assess.acidityDisplay}" /></td>
                    <td><c:out value="${assess.sweetnessDisplay}" /></td>
                    <td><c:out value="${assess.bitternessDisplay}" /></td>
                    <td><c:out value="${assess.bodyTextureDisplay}" /></td>
                    <td><c:out value="${assess.aftertasteDisplay}" /></td>
                    <td><c:out value="${assess.balanceDisplay}" /></td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- Descriptores jerárquicos con intensidad y escala por separado -->
            <c:if test="${not empty assess.descriptors}">
              <div style="margin-bottom: 0.65rem;">
                <strong style="font-size: 0.84rem;">Descriptores percibidos:</strong>
                <div class="cluster" style="margin-top: 0.35rem;">
                  <c:forEach var="ad" items="${assess.descriptors}">
                    <span class="badge" style="background: rgba(35,25,21,0.06); border: 1px solid ${ad.descriptor.colorHex};">
                      <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${ad.descriptor.colorHex};margin-right:4px;"></span>
                      <c:out value="${ad.descriptor.hierarchyLabel}" />
                      <span class="mono" style="margin-left: 4px;">(<c:out value="${ad.intensityDisplay}" />)</span>
                    </span>
                  </c:forEach>
                </div>
              </div>
            </c:if>

            <c:if test="${not empty assess.descriptiveNotes}">
              <p style="margin: 0.35rem 0; font-size: 0.9rem;">
                <strong>Notas descriptivas:</strong> <c:out value="${assess.descriptiveNotes}" />
              </p>
            </c:if>

            <!-- Bloque afectivo separado -->
            <c:if test="${not empty assess.preferenceRecord}">
              <div style="margin-top: 0.65rem; padding: 0.65rem 0.85rem; border-radius: var(--radius-sm); background: rgba(155, 77, 150, 0.08); border-left: 3px solid #9B4D96;">
                <span class="badge badge-preference badge-affective">Registro Afectivo Separado</span>
                <span class="tabular mono" style="margin-left: 0.5rem;">
                  Agrado: <strong><c:out value="${assess.preferenceRecord.overallLikingDisplay}" /></strong>
                  · Repetir: <strong><c:out value="${assess.preferenceRecord.wouldRepeatDisplay}" /></strong>
                </span>
                <c:if test="${not empty assess.preferenceRecord.affectiveContext}">
                  <span class="field-hint muted"> · Contexto: <c:out value="${assess.preferenceRecord.affectiveContext}" /></span>
                </c:if>
                <c:if test="${not empty assess.preferenceRecord.affectiveNotes}">
                  <div style="margin-top: 0.25rem; font-size: 0.88rem;">
                    <c:out value="${assess.preferenceRecord.affectiveNotes}" />
                  </div>
                </c:if>
              </div>
            </c:if>
          </article>
        </c:forEach>
      </div>
    </c:otherwise>
  </c:choose>
</section>

<script>
(function () {
  var searchInput = document.getElementById('flavor-search-input');
  var filterBadge = document.getElementById('flavor-active-filter');
  var clearBtn = document.getElementById('flavor-clear-filter-btn');
  var resetCenter = document.getElementById('wheel-reset-center');
  var rows = Array.prototype.slice.call(document.querySelectorAll('.descriptor-row'));
  var sectors = Array.prototype.slice.call(document.querySelectorAll('.wheel-sector'));

  var activeFamily = '';
  var activeSubfamily = '';

  function applyFilter() {
    var query = (searchInput && searchInput.value ? searchInput.value : '').trim().toLowerCase();
    var visibleCount = 0;

    rows.forEach(function (row) {
      var fam = row.getAttribute('data-family') || '';
      var sub = row.getAttribute('data-subfamily') || '';
      var haystack = row.getAttribute('data-search') || '';

      var matchFamily = !activeFamily || fam === activeFamily;
      var matchSub = !activeSubfamily || sub === activeSubfamily;
      var matchQuery = !query || haystack.indexOf(query) !== -1;

      var show = matchFamily && matchSub && matchQuery;
      row.style.display = show ? '' : 'none';
      if (show) visibleCount++;
    });

    if (filterBadge) {
      if (activeSubfamily) {
        filterBadge.textContent = activeFamily + ' › ' + activeSubfamily + ' (' + visibleCount + ')';
      } else if (activeFamily) {
        filterBadge.textContent = 'Familia: ' + activeFamily + ' (' + visibleCount + ')';
      } else if (query) {
        filterBadge.textContent = 'Búsqueda: «' + query + '» (' + visibleCount + ')';
      } else {
        filterBadge.textContent = 'Mostrando todas las familias (' + visibleCount + ')';
      }
    }
  }

  function handleSectorTrigger(el) {
    var type = el.getAttribute('data-filter-type');
    var fam = el.getAttribute('data-family') || '';
    var sub = el.getAttribute('data-subfamily') || '';
    if (type === 'subfamily') {
      if (activeFamily === fam && activeSubfamily === sub) {
        activeFamily = '';
        activeSubfamily = '';
      } else {
        activeFamily = fam;
        activeSubfamily = sub;
      }
    } else {
      if (activeFamily === fam && !activeSubfamily) {
        activeFamily = '';
      } else {
        activeFamily = fam;
        activeSubfamily = '';
      }
    }
    applyFilter();
  }

  sectors.forEach(function (sec) {
    sec.addEventListener('click', function () { handleSectorTrigger(sec); });
    sec.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        handleSectorTrigger(sec);
      }
    });
  });

  function clearFilters() {
    activeFamily = '';
    activeSubfamily = '';
    if (searchInput) searchInput.value = '';
    applyFilter();
  }

  if (searchInput) searchInput.addEventListener('input', applyFilter);
  if (clearBtn) clearBtn.addEventListener('click', clearFilters);
  if (resetCenter) {
    resetCenter.addEventListener('click', clearFilters);
    resetCenter.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        clearFilters();
      }
    });
  }
})();
</script>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
