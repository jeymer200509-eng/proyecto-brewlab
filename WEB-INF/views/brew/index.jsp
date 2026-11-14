<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageHeading" value="Asistente de Preparación (Brew Assistant)"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<c:if test="${not empty brewSession}">
  <section class="timer-stage theme-${settings.timerThemeCode}" id="brew-session-stage"
           data-session-id="${fn:escapeXml(brewSession.id)}"
           data-status="${fn:escapeXml(brewSession.status)}"
           data-started-ms="${brewSession.startedAtEpochMs}"
           data-paused-ms="${brewSession.pausedAtEpochMs}"
           data-accum-pause-ms="${brewSession.accumulatedPauseMs}"
           data-server-now-ms="${serverNowEpochMs}"
           data-sound-enabled="${settings.soundEnabled}"
           data-wake-lock="${settings.keepScreenAwake}"
           aria-labelledby="timer-heading"
           style="margin-bottom: 1.25rem;">

    <div class="card-header">
      <div>
        <div class="cluster">
          <span class="badge badge-planned"><c:out value="${brewSession.plannedMethod}"/></span>
          <span class="badge badge-measured" id="session-status-badge">Estado: <c:out value="${brewSession.status}"/></span>
          <c:if test="${not empty sessionLot}">
            <span class="badge badge-planned"><c:out value="${sessionLot.code} — ${sessionLot.name}"/></span>
          </c:if>
          <c:if test="${not empty sessionBatch}">
            <span class="badge badge-measured tabular">
              Tostión <c:out value="${sessionBatch.batchCode}"/> (<c:out value="${sessionBatchAge.days}"/> d)
            </span>
          </c:if>
        </div>
        <h2 id="timer-heading" style="margin-top:0.4rem;">
          <c:out value="${brewSession.plannedRecipeName}"/>
        </h2>
      </div>
      <div class="cluster">
        <a class="btn btn-secondary btn-sm" href="${pageContext.request.contextPath}/app/preparacion?new=true">
          + Nueva preparación
        </a>
        <c:if test="${brewSession.completed and not empty applicationScope.navCatacionEnabled}">
          <a class="btn btn-sage btn-sm"
             href="${pageContext.request.contextPath}/app/catacion?brewSessionId=${fn:escapeXml(brewSession.id)}&amp;coffeeLotId=${fn:escapeXml(brewSession.coffeeLotId)}&amp;roastBatchId=${fn:escapeXml(brewSession.roastBatchId)}">
            Evaluar taza en Sensory Lab →
          </a>
        </c:if>
      </div>
    </div>

    <!-- CRONÓMETRO PROMINENTE CON DÍGITOS TABULARES -->
    <div class="grid-2" style="align-items: center; margin: 1rem 0;">
      <div>
        <div class="field-hint">Tiempo neto transcurrido (reconstruido por marcas temporales)</div>
        <div class="timer-digits tabular" id="brew-timer-display" aria-live="off">
          <c:out value="${elapsedFormatted}"/>
        </div>
        <div class="field-hint" style="margin-top:0.35rem;">
          Previsto: <strong class="tabular"><c:out value="${brewSession.plannedDoseDisplay}"/> / <c:out value="${brewSession.plannedWaterDisplay}"/> (<c:out value="${brewSession.plannedRatioDisplay}"/>)</strong>
          · Temp. objetivo: <strong class="tabular"><c:out value="${brewSession.plannedTempDisplay}"/></strong>
        </div>
      </div>

      <!-- CONTROLES GRANDES DE CRONÓMETRO -->
      <div class="stack-sm">
        <c:choose>
          <c:when test="${brewSession.status == 'PREPARED'}">
            <form method="post" action="${pageContext.request.contextPath}/app/preparacion" data-once>
              <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
              <input type="hidden" name="action" value="start">
              <input type="hidden" name="sessionId" value="${fn:escapeXml(brewSession.id)}">
              <input type="hidden" name="idempotencyKey" value="START-${fn:escapeXml(actionNonce)}">
              <button type="submit" class="btn btn-caramel btn-lg" style="width:100%;">
                Iniciar cronómetro y preparación
              </button>
            </form>
          </c:when>

          <c:when test="${brewSession.status == 'ACTIVE'}">
            <form method="post" action="${pageContext.request.contextPath}/app/preparacion" data-once>
              <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
              <input type="hidden" name="action" value="pause">
              <input type="hidden" name="sessionId" value="${fn:escapeXml(brewSession.id)}">
              <input type="hidden" name="idempotencyKey" value="PAUSE-${fn:escapeXml(actionNonce)}">
              <button type="submit" class="btn btn-secondary btn-lg" style="width:100%;">
                Pausar cronómetro (la pausa no suma tiempo)
              </button>
            </form>
          </c:when>

          <c:when test="${brewSession.status == 'PAUSED'}">
            <form method="post" action="${pageContext.request.contextPath}/app/preparacion" data-once>
              <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
              <input type="hidden" name="action" value="resume">
              <input type="hidden" name="sessionId" value="${fn:escapeXml(brewSession.id)}">
              <input type="hidden" name="idempotencyKey" value="RESUME-${fn:escapeXml(actionNonce)}">
              <button type="submit" class="btn btn-caramel btn-lg" style="width:100%;">
                Reanudar cronómetro
              </button>
            </form>
          </c:when>
        </c:choose>
      </div>
    </div>

    <!-- ETAPA ACTUAL Y SIGUIENTE ETAPA -->
    <c:if test="${brewSession.inProgress}">
      <div class="grid-2" style="margin-top: 1rem;">
        <div class="surface-card" style="color: var(--text-primary);">
          <c:choose>
            <c:when test="${not empty currentStep}">
              <div class="cluster" style="justify-content: space-between;">
                <span class="badge badge-planned">Etapa actual #<c:out value="${currentStep.order}"/></span>
                <span class="tabular" style="font-weight:600;">
                  Inicio objetivo: <c:out value="${currentStep.startFormatted}"/>
                </span>
              </div>
              <h3 style="margin-top:0.45rem; font-size:1.15rem;"><c:out value="${currentStep.title}"/></h3>
              <p style="font-size:0.94rem; color: var(--text-secondary);"><c:out value="${currentStep.instruction}"/></p>
              <div class="cluster" style="margin-bottom:0.85rem;">
                <span class="badge badge-planned tabular">Vertido paso: <c:out value="${currentStep.waterDoseFormatted}"/></span>
                <span class="badge badge-planned tabular">Acumulado previsto: <c:out value="${currentStep.cumulativeWaterFormatted}"/></span>
              </div>

              <!-- Completar etapa actual -->
              <form method="post" action="${pageContext.request.contextPath}/app/preparacion" class="stack-sm" data-once>
                <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
                <input type="hidden" name="action" value="completeStep">
                <input type="hidden" name="sessionId" value="${fn:escapeXml(brewSession.id)}">
                <input type="hidden" name="idempotencyKey" value="STEP-${currentStep.order}-${fn:escapeXml(actionNonce)}">

                <div class="form-grid">
                  <div class="field">
                    <label class="field-label" for="step-actual-water">Agua acumulada real medida (g)</label>
                    <input class="input tabular" id="step-actual-water" name="stepActualWaterG" type="number" step="0.1" min="0"
                           value="${not empty currentStep.cumulativeWaterG ? currentStep.cumulativeWaterG : currentStep.waterDoseG}">
                  </div>
                  <div class="field">
                    <label class="field-label" for="step-notes">Observación del paso (opcional)</label>
                    <input class="input" id="step-notes" name="stepNotes" type="text" placeholder="Ej. Flujo rápido">
                  </div>
                </div>
                <button type="submit" class="btn btn-sage">
                  ✓ Marcar etapa #<c:out value="${currentStep.order}"/> completada
                </button>
              </form>

              <!-- Omitir etapa con confirmación explícita -->
              <form method="post" action="${pageContext.request.contextPath}/app/preparacion"
                    class="cluster" style="margin-top:0.65rem; padding-top:0.65rem; border-top:1px solid var(--border-hairline);" data-once>
                <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
                <input type="hidden" name="action" value="skipStep">
                <input type="hidden" name="sessionId" value="${fn:escapeXml(brewSession.id)}">
                <input type="hidden" name="idempotencyKey" value="SKIP-${currentStep.order}-${fn:escapeXml(actionNonce)}">
                <label class="cluster" style="font-size:0.8rem; cursor:pointer;">
                  <input type="checkbox" name="confirmSkip" value="true" required>
                  <span>Confirmo omitir esta etapa</span>
                </label>
                <button type="submit" class="btn btn-ghost btn-sm">Saltar etapa</button>
              </form>
            </c:when>
            <c:otherwise>
              <div class="alert alert-success" style="margin-bottom:0;">
                <span>Todas las etapas programadas de la receta se han completado. Registra el cierre de la preparación al terminar el drenaje.</span>
              </div>
            </c:otherwise>
          </c:choose>

          <c:if test="${not empty nextStep}">
            <div style="margin-top:0.85rem; padding-top:0.75rem; border-top:1px solid var(--border-hairline); font-size:0.84rem; color: var(--text-secondary);">
              <strong>Siguiente etapa (#<c:out value="${nextStep.order}"/> en <span class="tabular"><c:out value="${nextStep.startFormatted}"/></span>):</strong>
              <c:out value="${nextStep.title}"/> — Acumulado previsto: <span class="tabular"><c:out value="${nextStep.cumulativeWaterFormatted}"/></span>
            </div>
          </c:if>
        </div>

        <!-- FINALIZAR O CANCELAR SESIÓN -->
        <div class="surface-card" style="color: var(--text-primary);">
          <h3>Finalizar preparación y guardar valores reales</h3>
          <p class="field-hint">
            Distingue los parámetros previstos de la receta frente a lo que realmente ocurrió en taza.
          </p>
          <form method="post" action="${pageContext.request.contextPath}/app/preparacion" class="stack-sm" data-once>
            <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
            <input type="hidden" name="action" value="completeSession">
            <input type="hidden" name="sessionId" value="${fn:escapeXml(brewSession.id)}">
            <input type="hidden" name="idempotencyKey" value="FINISH-${fn:escapeXml(actionNonce)}">

            <div class="form-grid">
              <div class="field">
                <label class="field-label" for="fin-dose">Dosis real medida (g)</label>
                <input class="input tabular" id="fin-dose" name="actualDoseG" type="number" step="0.1" min="0.1"
                       value="${not empty brewSession.actualDoseG ? brewSession.actualDoseG : brewSession.plannedDoseG}">
              </div>
              <div class="field">
                <label class="field-label" for="fin-water">Agua total real vertida (g)</label>
                <input class="input tabular" id="fin-water" name="actualWaterG" type="number" step="0.5" min="1"
                       value="${not empty brewSession.actualWaterG ? brewSession.actualWaterG : brewSession.plannedWaterG}">
              </div>
              <div class="field">
                <label class="field-label" for="fin-temp">Temperatura real (°C)</label>
                <input class="input tabular" id="fin-temp" name="actualTemperatureC" type="number" step="0.5"
                       value="${not empty brewSession.actualTemperatureC ? brewSession.actualTemperatureC : brewSession.plannedTemperatureC}">
              </div>
              <div class="field">
                <label class="field-label" for="fin-grind">Molienda real usada</label>
                <input class="input" id="fin-grind" name="actualGrindSetting" type="text"
                       value="${fn:escapeXml(not empty brewSession.actualGrindSetting ? brewSession.actualGrindSetting : brewSession.plannedGrindSetting)}">
              </div>
              <div class="field">
                <label class="field-label" for="fin-time">Tiempo total (s, vacío = usar cronómetro)</label>
                <input class="input tabular" id="fin-time" name="totalTimeSeconds" type="number" min="1"
                       placeholder="Automático desde cronómetro">
              </div>
              <div class="field">
                <label class="field-label" for="fin-bev">Peso final de bebida en taza (g, opcional)</label>
                <input class="input tabular" id="fin-bev" name="finalBeverageWeightG" type="number" step="0.5" min="0"
                       placeholder="No medido = vacío">
              </div>
            </div>

            <div class="field">
              <label class="field-label" for="fin-notes">Observaciones de la extracción</label>
              <textarea class="input" id="fin-notes" name="notes" rows="2"
                        placeholder="Ej. Cama plana, aroma floral intenso durante preinfusión."><c:out value="${brewSession.notes}"/></textarea>
            </div>

            <button type="submit" class="btn btn-primary">Finalizar y guardar sesión real</button>
          </form>

          <form method="post" action="${pageContext.request.contextPath}/app/preparacion"
                class="cluster" style="margin-top:0.75rem; padding-top:0.75rem; border-top:1px solid var(--border-hairline);" data-once>
            <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
            <input type="hidden" name="action" value="cancelSession">
            <input type="hidden" name="sessionId" value="${fn:escapeXml(brewSession.id)}">
            <input type="hidden" name="idempotencyKey" value="CANCEL-${fn:escapeXml(actionNonce)}">
            <input class="input" name="cancelReason" type="text" style="width:13rem;" placeholder="Motivo de cancelación">
            <button type="submit" class="btn btn-danger btn-sm">Cancelar preparación</button>
          </form>
        </div>
      </div>
    </c:if>

    <!-- RESUMEN DE SESIÓN COMPLETADA: PREVISTO VS MEDIDO + EVENTOS -->
    <div class="surface-card" style="margin-top:1rem; color: var(--text-primary);">
      <div class="card-header">
        <h3>Comparativa de la sesión: Parámetros previstos (instantánea) vs. Valores reales medidos</h3>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Parámetro</th>
              <th class="num"><span class="badge badge-planned">Previsto (Receta al iniciar)</span></th>
              <th class="num"><span class="badge badge-measured">Medido (Real en sesión)</span></th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Dosis de café</td>
              <td class="num"><c:out value="${brewSession.plannedDoseDisplay}"/></td>
              <td class="num"><strong><c:out value="${brewSession.actualDoseDisplay}"/></strong></td>
            </tr>
            <tr>
              <td>Agua total</td>
              <td class="num"><c:out value="${brewSession.plannedWaterDisplay}"/></td>
              <td class="num"><strong><c:out value="${brewSession.actualWaterDisplay}"/></strong></td>
            </tr>
            <tr>
              <td>Ratio (1:x = agua ÷ dosis)</td>
              <td class="num"><c:out value="${brewSession.plannedRatioDisplay}"/></td>
              <td class="num"><strong><c:out value="${brewSession.actualRatioDisplay}"/></strong></td>
            </tr>
            <tr>
              <td>Temperatura (°C)</td>
              <td class="num"><c:out value="${brewSession.plannedTempDisplay}"/></td>
              <td class="num"><strong><c:out value="${brewSession.actualTempDisplay}"/></strong></td>
            </tr>
            <tr>
              <td>Molienda</td>
              <td><c:out value="${not empty brewSession.plannedGrindSetting ? brewSession.plannedGrindSetting : '—'}"/></td>
              <td><strong><c:out value="${not empty brewSession.actualGrindSetting ? brewSession.actualGrindSetting : '—'}"/></strong></td>
            </tr>
            <tr>
              <td>Tiempo total / Peso bebida</td>
              <td class="num">
                <c:out value="${not empty brewSession.plannedTargetTimeMinS ? brewSession.plannedTargetTimeMinS : '—'}"/> s
              </td>
              <td class="num">
                <strong><c:out value="${brewSession.totalTimeDisplay}"/></strong>
                (Bebida: <c:out value="${brewSession.finalBeverageDisplay}"/>)
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <c:if test="${not empty brewSession.events}">
        <h4 style="margin-top:1rem; margin-bottom:0.4rem;">Registro inmutable de eventos (BrewEvent)</h4>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th class="num">Tiempo real</th>
                <th>Evento</th>
                <th class="num">Objetivo (g)</th>
                <th class="num">Real (g)</th>
                <th>Notas</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="ev" items="${brewSession.events}">
                <tr>
                  <td class="num"><strong><c:out value="${ev.elapsedFormatted}"/></strong></td>
                  <td>
                    <c:out value="${ev.typeLabel}"/>
                    <c:if test="${not empty ev.stepOrder}"> (Paso #<c:out value="${ev.stepOrder}"/>)</c:if>
                  </td>
                  <td class="num"><c:out value="${ev.targetFormatted}"/></td>
                  <td class="num"><c:out value="${ev.actualFormatted}"/></td>
                  <td><c:out value="${not empty ev.notes ? ev.notes : '—'}"/></td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
      </c:if>
    </div>
  </section>
</c:if>

<!-- CONFIGURAR NUEVA SESIÓN DESDE RECETA + CAFÉ/TOSTIÓN -->
<section class="surface-card" aria-labelledby="setup-session-heading">
  <div class="card-header">
    <div>
      <h2 id="setup-session-heading">Configurar nueva sesión guiada</h2>
      <p class="field-hint" style="margin-bottom:0;">
        Selecciona la receta y el lote de café/tostión. Se guardará una instantánea completa de los pasos de la receta al crear la sesión.
      </p>
    </div>
  </div>

  <c:choose>
    <c:when test="${empty recipes}">
      <div class="empty-state">
        <p style="margin-bottom:0.75rem;">Necesitas al menos una receta para iniciar el asistente guiado.</p>
        <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/app/recetas">Ir a Recetas y Métodos</a>
      </div>
    </c:when>
    <c:otherwise>
      <form method="post" action="${pageContext.request.contextPath}/app/preparacion" class="stack-md" data-once>
        <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
        <input type="hidden" name="action" value="createSession">

        <div class="form-grid">
          <div class="field">
            <label class="field-label" for="prep-recipe">Receta base *</label>
            <select class="input" id="prep-recipe" name="recipeId" required>
              <c:forEach var="r" items="${recipes}">
                <option value="${fn:escapeXml(r.id)}" ${preselectedRecipeId == r.id ? 'selected' : ''}>
                  <c:out value="${r.name} (${r.method} · ${r.doseDisplay}/${r.waterDisplay} · ${r.ratioDisplay})"/>
                </option>
              </c:forEach>
            </select>
          </div>

          <div class="field">
            <label class="field-label" for="prep-lot">Lote de café (CoffeeLot)</label>
            <select class="input" id="prep-lot" name="coffeeLotId">
              <option value="">— Usar el de la receta / ninguno —</option>
              <c:forEach var="lot" items="${coffeeLots}">
                <option value="${fn:escapeXml(lot.id)}" ${preselectedLotId == lot.id ? 'selected' : ''}>
                  <c:out value="${lot.code} — ${lot.name}"/>
                </option>
              </c:forEach>
            </select>
          </div>

          <div class="field">
            <label class="field-label" for="prep-batch">Lote de tostión (RoastBatch)</label>
            <select class="input" id="prep-batch" name="roastBatchId">
              <option value="">— Seleccionar lote de tostión —</option>
              <c:forEach var="b" items="${roastBatches}">
                <option value="${fn:escapeXml(b.id)}" ${preselectedBatchId == b.id ? 'selected' : ''}>
                  <c:out value="${b.batchCode} (${b.roastDate} · ${not empty b.roastLevel ? b.roastLevel : 'Sin nivel'})"/>
                </option>
              </c:forEach>
            </select>
          </div>

          <div class="field">
            <label class="field-label" for="prep-dose">Dosis inicial real (g, opcional)</label>
            <input class="input tabular" id="prep-dose" name="actualDoseG" type="number" step="0.1" min="0.1"
                   placeholder="Por defecto usa la prevista">
          </div>

          <div class="field">
            <label class="field-label" for="prep-temp">Temperatura inicial real (°C, opcional)</label>
            <input class="input tabular" id="prep-temp" name="actualTemperatureC" type="number" step="0.5"
                   placeholder="Por defecto usa la prevista">
          </div>

          <div class="field">
            <label class="field-label" for="prep-grind">Molienda inicial real (opcional)</label>
            <input class="input" id="prep-grind" name="actualGrindSetting" type="text"
                   placeholder="Por defecto usa la prevista">
          </div>
        </div>

        <label class="cluster" style="cursor:pointer; font-size:0.88rem;">
          <input type="checkbox" name="startImmediately" value="true" checked>
          <span><strong>Iniciar el cronómetro inmediatamente al crear la sesión</strong></span>
        </label>

        <div>
          <button type="submit" class="btn btn-caramel">Abrir sesión en el Asistente →</button>
        </div>
      </form>
    </c:otherwise>
  </c:choose>
</section>

<!-- HISTORIAL RECIENTE DE SESIONES -->
<c:if test="${not empty sessionHistory}">
  <section class="surface-card" aria-labelledby="hist-heading">
    <div class="card-header">
      <h2 id="hist-heading">Sesiones registradas</h2>
      <c:if test="${not empty applicationScope.navAnalisisEnabled}">
        <a class="btn btn-secondary btn-sm" href="${pageContext.request.contextPath}/app/analisis">
          Abrir comparador lado a lado →
        </a>
      </c:if>
    </div>
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>Receta / Método</th>
            <th>Café / Tostión</th>
            <th>Estado</th>
            <th class="num">Previsto vs. Real</th>
            <th class="num">Tiempo</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="s" items="${sessionHistory}">
            <tr>
              <td>
                <div style="font-weight:600;">
                  <c:out value="${s.plannedRecipeName}"/>
                  <c:if test="${s.demo}"><span class="badge badge-demo">[DEMO]</span></c:if>
                </div>
                <div class="field-hint"><c:out value="${s.plannedMethod}"/></div>
              </td>
              <td>
                <c:set var="sLot" value="${lotMap[s.coffeeLotId]}"/>
                <c:set var="sBatch" value="${batchMap[s.roastBatchId]}"/>
                <div><c:out value="${not empty sLot ? sLot.name : '—'}"/></div>
                <div class="field-hint"><c:out value="${not empty sBatch ? sBatch.batchCode : '—'}"/></div>
              </td>
              <td><span class="badge badge-measured"><c:out value="${s.status}"/></span></td>
              <td class="num">
                Prev.: <c:out value="${s.plannedDoseDisplay}"/> / <c:out value="${s.plannedWaterDisplay}"/> (<c:out value="${s.plannedRatioDisplay}"/>)<br>
                Real: <strong><c:out value="${s.actualDoseDisplay}"/> / <c:out value="${s.actualWaterDisplay}"/> (<c:out value="${s.actualRatioDisplay}"/>)</strong>
              </td>
              <td class="num"><c:out value="${s.totalTimeDisplay}"/></td>
              <td>
                <div class="cluster">
                  <a class="btn btn-secondary btn-sm"
                     href="${pageContext.request.contextPath}/app/preparacion?sessionId=${fn:escapeXml(s.id)}">
                    Ver / Reanudar
                  </a>
                  <c:if test="${not empty applicationScope.navCatacionEnabled}">
                    <a class="btn btn-sage btn-sm"
                       href="${pageContext.request.contextPath}/app/catacion?brewSessionId=${fn:escapeXml(s.id)}&amp;coffeeLotId=${fn:escapeXml(s.coffeeLotId)}&amp;roastBatchId=${fn:escapeXml(s.roastBatchId)}">
                      Catar
                    </a>
                  </c:if>
                </div>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </section>
</c:if>

<script>
(function () {
  var stage = document.getElementById('brew-session-stage');
  var display = document.getElementById('brew-timer-display');
  if (!stage || !display) return;

  var status = stage.getAttribute('data-status');
  var startedMs = parseInt(stage.getAttribute('data-started-ms') || '0', 10);
  var accumPauseMs = parseInt(stage.getAttribute('data-accum-pause-ms') || '0', 10);
  var serverNowMs = parseInt(stage.getAttribute('data-server-now-ms') || '0', 10);
  var clientOffsetMs = serverNowMs > 0 ? (Date.now() - serverNowMs) : 0;

  function formatDuration(totalSeconds) {
    var s = Math.max(0, Math.round(totalSeconds));
    var m = Math.floor(s / 60);
    var rem = s % 60;
    return m + ':' + (rem < 10 ? '0' + rem : rem);
  }

  if (status === 'ACTIVE' && startedMs > 0) {
    function tick() {
      var correctedNow = Date.now() - clientOffsetMs;
      var elapsedMs = Math.max(0, correctedNow - startedMs - accumPauseMs);
      display.textContent = formatDuration(elapsedMs / 1000);
      requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  }

  // Wake Lock opcional con fallback silencioso si el navegador no lo soporta
  if (status === 'ACTIVE' && stage.getAttribute('data-wake-lock') === 'true' && navigator.wakeLock) {
    try {
      navigator.wakeLock.request('screen').catch(function () {});
    } catch (e) {}
  }
})();
</script>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
