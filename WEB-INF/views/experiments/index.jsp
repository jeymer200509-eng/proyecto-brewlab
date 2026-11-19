<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageHeading" value="Cuaderno experimental"/>
<c:set var="moduleKicker" value="08 / HIPÓTESIS Y ENSAYOS"/>
<c:set var="moduleDescription" value="Cambie una variable. Observe, contraste y documente lo aprendido."/>
<c:set var="moduleAnchor" value="new-experiment"/>
<c:set var="moduleAction" value="Diseñar experimento"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<details class="module-guidance"><summary>Guía de lectura y criterios del módulo</summary>
  <div class="card-header" style="margin-bottom: 0;">
    <div>
      <div class="field-hint">Módulo 8 · Experiment Lab</div>
      <h2 style="margin-top: 0.25rem;">Laboratorio de Experimentación Controlada</h2>
      <p class="field-hint" style="margin-bottom: 0;">
        Formula preguntas e hipótesis, modifica <strong>una variable principal por ensayo</strong> manteniendo las demás
        constantes, vincula cada <span class="mono">ExperimentTrial</span> a una <span class="mono">BrewSession</span> real
        y registra tus conclusiones apoyado por sugerencias explicables basadas en reglas.
      </p>
    </div>
    <div class="cluster">
      <span class="badge badge-estimated">Motor de Reglas Explicable V1 (Hipótesis orientativas, nunca causas probadas)</span>
      <a class="btn btn-secondary btn-sm" href="${pageContext.request.contextPath}/app/analisis">← Volver a Analítica</a>
    </div>
  </div>
</details>

<!-- 1. Crear nuevo Experimento -->
<section id="new-experiment" class="surface-card card hypothesis-workbench" style="margin-bottom: 1.5rem;">
  <div class="card-header">
    <div>
      <span class="badge badge-planned">Diseño Experimental</span>
      <h2 class="card-title" style="margin-top: 0.35rem;">Nuevo Experimento (Aislamiento de una Variable Principal)</h2>
      <p class="field-hint card-subtitle" style="margin-bottom: 0;">
        Recomendación metodológica: cambia una sola variable principal por ensayo (p. ej. molienda o temperatura) y conserva constantes lote, tueste, dosis y agua.
      </p>
    </div>
  </div>

  <form method="post" action="${pageContext.request.contextPath}/app/experimentos" data-once="true">
    <input type="hidden" name="_csrf" value="${csrfToken}" />
    <input type="hidden" name="action" value="createExperiment" />

    <div class="form-grid form-grid-3">
      <div class="field">
        <label class="field-label" for="expTitle">Título del experimento *</label>
        <input class="input" id="expTitle" name="title" type="text" required
               placeholder="Ej.: Efecto de molienda (20 vs 24 clics) en V60" />
      </div>
      <div class="field">
        <label class="field-label" for="expLotId">Lote de café</label>
        <select class="input" id="expLotId" name="coffeeLotId">
          <option value="">— Opcional —</option>
          <c:forEach var="lot" items="${coffeeLots}">
            <option value="${lot.id}">[<c:out value="${lot.code}" />] <c:out value="${lot.name}" /></option>
          </c:forEach>
        </select>
      </div>
      <div class="field">
        <label class="field-label" for="expBatchId">Lote de tostión</label>
        <select class="input" id="expBatchId" name="roastBatchId">
          <option value="">— Opcional —</option>
          <c:forEach var="rb" items="${roastBatches}">
            <option value="${rb.id}">[<c:out value="${rb.code}" />] <c:out value="${rb.roastLevel}" /> (<c:out value="${rb.roastDateDisplay}" />)</option>
          </c:forEach>
        </select>
      </div>
    </div>

    <div class="form-grid form-grid-2" style="margin-top: 0.75rem;">
      <div class="field">
        <label class="field-label" for="expQuestion">Pregunta de investigación *</label>
        <input class="input" id="expQuestion" name="question" type="text" required
               placeholder="Ej.: ¿Cómo afecta abrir 3 clics la molienda al dulzor y tiempo de drenaje?" />
      </div>
      <div class="field">
        <label class="field-label" for="expObjective">Objetivo *</label>
        <input class="input" id="expObjective" name="objective" type="text" required
               placeholder="Ej.: Reducir la sequedad final manteniendo la claridad floral" />
      </div>
    </div>

    <div class="form-grid form-grid-3" style="margin-top: 0.75rem;">
      <div class="field">
        <label class="field-label" for="expHypothesis">Hipótesis inicial *</label>
        <input class="input" id="expHypothesis" name="hypothesis" type="text" required
               placeholder="Ej.: Con 24 clics el drenaje bajará ~15 s y disminuirá el amargor" />
      </div>
      <div class="field">
        <label class="field-label" for="expModVar">Variable principal modificada (1 recomendada) *</label>
        <select class="input" id="expModVar" name="modifiedVariable" required>
          <option value="Molienda (Grind Setting)">Molienda (Grind Setting)</option>
          <option value="Temperatura del agua (°C)">Temperatura del agua (°C)</option>
          <option value="Ratio agua:café (1:x)">Ratio agua:café (1:x)</option>
          <option value="Estructura de vertidos / Pulsos">Estructura de vertidos / Pulsos</option>
          <option value="Perfil mineral del agua">Perfil mineral del agua</option>
          <option value="Método / Dripper">Método / Dripper</option>
        </select>
      </div>
      <div class="field">
        <label class="field-label" for="expCtrlVars">Variables controladas (constantes) *</label>
        <input class="input" id="expCtrlVars" name="controlledVariables" type="text" required
               value="Mismo CoffeeLot, mismo RoastBatch, dosis 15 g, agua 250 g, temperatura 93 °C" />
      </div>
    </div>

    <!-- Vincular opcionalmente 2 ensayos iniciales -->
    <div class="form-grid form-grid-4" style="margin-top: 0.85rem; align-items: end;">
      <div class="field">
        <label class="field-label" for="trialSessionA">Ensayo A (Sesión de referencia)</label>
        <select class="input" id="trialSessionA" name="trialSessionA">
          <option value="">— Vincular después —</option>
          <c:forEach var="bs" items="${brewSessions}">
            <option value="${bs.id}">
              <c:out value="${bs.plannedMethod}" /> · <c:out value="${bs.plannedRecipeName}" /> (<c:out value="${bs.actualGrindDisplay}" />, <c:out value="${bs.totalTimeDisplay}" />)
            </option>
          </c:forEach>
        </select>
      </div>
      <div class="field">
        <label class="field-label" for="trialValueA">Valor en Ensayo A</label>
        <input class="input" id="trialValueA" name="trialValueA" type="text" placeholder="Ej.: 21 clics" />
      </div>
      <div class="field">
        <label class="field-label" for="trialSessionB">Ensayo B (Sesión con variación)</label>
        <select class="input" id="trialSessionB" name="trialSessionB">
          <option value="">— Vincular después —</option>
          <c:forEach var="bs" items="${brewSessions}">
            <option value="${bs.id}">
              <c:out value="${bs.plannedMethod}" /> · <c:out value="${bs.plannedRecipeName}" /> (<c:out value="${bs.actualGrindDisplay}" />, <c:out value="${bs.totalTimeDisplay}" />)
            </option>
          </c:forEach>
        </select>
      </div>
      <div class="field">
        <label class="field-label" for="trialValueB">Valor en Ensayo B</label>
        <input class="input" id="trialValueB" name="trialValueB" type="text" placeholder="Ej.: 24 clics" />
      </div>
    </div>

    <div style="margin-top: 1rem; display: flex; justify-content: flex-end;">
      <button type="submit" class="btn btn-primary">Crear experimento</button>
    </div>
  </form>
</section>

<!-- 2. Lista de Experimentos registrados -->
<section class="experiment-timeline stack-md stack">
  <c:choose>
    <c:when test="${empty experiments}">
      <div class="surface-card card empty-state">
        <p>Aún no has creado experimentos. Diseña tu primer experimento en el formulario superior y vincula sesiones reales.</p>
      </div>
    </c:when>
    <c:otherwise>
      <c:forEach var="exp" items="${experiments}">
        <c:set var="expComp" value="${comparisonByExperiment[exp.id]}" />
        <c:set var="expSuggs" value="${suggestionsByExperiment[exp.id]}" />
        <article class="surface-card card experiment-entry">
          <div class="card-header">
            <div>
              <div class="cluster" style="margin-bottom: 0.35rem;">
                <span class="badge ${exp.status == 'CONCLUDED' ? 'badge-measured' : 'badge-planned'}">
                  <c:out value="${exp.statusLabel}" />
                </span>
                <span class="badge badge-planned">Variable modificada: <c:out value="${exp.modifiedVariable}" /></span>
                <span class="badge badge-measured">Controladas: <c:out value="${exp.controlledVariables}" /></span>
              </div>
              <h2 class="card-title"><c:out value="${exp.title}" /></h2>
              <p class="field-hint card-subtitle" style="margin-bottom: 0;">
                <strong>Pregunta:</strong> <c:out value="${exp.question}" /> ·
                <strong>Objetivo:</strong> <c:out value="${exp.objective}" /> ·
                <strong>Hipótesis:</strong> <c:out value="${exp.hypothesis}" />
              </p>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/app/experimentos" style="margin: 0;">
              <input type="hidden" name="_csrf" value="${csrfToken}" />
              <input type="hidden" name="action" value="deleteExperiment" />
              <input type="hidden" name="experimentId" value="${exp.id}" />
              <button type="submit" class="btn btn-secondary btn-sm">Eliminar</button>
            </form>
          </div>

          <!-- Advertencia si los ensayos cambiaron múltiples variables simultáneamente -->
          <c:if test="${not empty expComp and fn:length(expComp.changedVariables) > 1}">
            <div class="alert alert-warn flash flash-warn" role="status" style="margin-bottom: 0.85rem;">
              <div>
                <strong>Aviso de control experimental:</strong> Entre los ensayos vinculados cambiaron
                <strong><c:out value="${fn:length(expComp.changedVariables)}" /> variables</strong>
                (<c:forEach var="cv" items="${expComp.changedVariables}" varStatus="cvs"><c:out value="${cv}" /><c:if test="${not cvs.last}">, </c:if></c:forEach>).
                Recuerda mantener fijas las variables controladas para aislar el efecto de <em><c:out value="${exp.modifiedVariable}" /></em>.
              </div>
            </div>
          </c:if>

          <!-- Tabla de Ensayos (ExperimentTrial) vinculados a BrewSession -->
          <div class="table-wrap" style="margin-bottom: 1rem;">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Orden</th>
                  <th>Etiqueta del Ensayo</th>
                  <th>Valor de Variable Modificada</th>
                  <th>BrewSession Vinculada</th>
                  <th>Medido Real (<span class="mono">Ratio · °C · Molienda · Tiempo</span>)</th>
                  <th>Resultado Sensorial / Afectivo</th>
                </tr>
              </thead>
              <tbody>
                <c:choose>
                  <c:when test="${empty exp.trials}">
                    <tr>
                      <td colspan="6" class="muted">Sin ensayos vinculados todavía. Añade una sesión abajo.</td>
                    </tr>
                  </c:when>
                  <c:otherwise>
                    <c:forEach var="tr" items="${exp.trials}">
                      <c:set var="trSession" value="${sessionMap[tr.brewSessionId]}" />
                      <c:set var="trAssess" value="${latestAssessmentBySession[tr.brewSessionId]}" />
                      <tr>
                        <td class="num">#<c:out value="${tr.trialOrder}" /></td>
                        <td><strong><c:out value="${tr.label}" /></strong></td>
                        <td><span class="badge badge-planned"><c:out value="${tr.modifiedValue != null ? tr.modifiedValue : '—'}" /></span></td>
                        <td>
                          <c:choose>
                            <c:when test="${not empty trSession}">
                              <a href="${pageContext.request.contextPath}/app/preparacion?sessionId=${trSession.id}">
                                <c:out value="${trSession.plannedRecipeName}" /> (<c:out value="${trSession.plannedMethod}" />)
                              </a>
                            </c:when>
                            <c:otherwise><span class="mono muted">—</span></c:otherwise>
                          </c:choose>
                        </td>
                        <td class="num">
                          <c:if test="${not empty trSession}">
                            <c:out value="${trSession.actualRatioDisplay}" /> ·
                            <c:out value="${trSession.actualTemperatureCDisplay}" /> ·
                            <c:out value="${trSession.actualGrindDisplay}" /> ·
                            <strong><c:out value="${trSession.totalTimeDisplay}" /></strong>
                          </c:if>
                        </td>
                        <td>
                          <c:choose>
                            <c:when test="${not empty trAssess}">
                              Sabor <span class="mono"><c:out value="${trAssess.flavorDisplay}" /></span> ·
                              Acidez <span class="mono"><c:out value="${trAssess.acidityDisplay}" /></span> ·
                              Dulzor <span class="mono"><c:out value="${trAssess.sweetnessDisplay}" /></span> ·
                              Balance <strong class="mono"><c:out value="${trAssess.balanceDisplay}" /></strong>
                              <c:if test="${not empty trAssess.preferenceRecord}">
                                <span class="badge badge-preference badge-affective" style="margin-left: 4px;">
                                  Agrado <c:out value="${trAssess.preferenceRecord.overallLikingDisplay}" />
                                </span>
                              </c:if>
                            </c:when>
                            <c:otherwise><span class="mono muted">—</span></c:otherwise>
                          </c:choose>
                        </td>
                      </tr>
                    </c:forEach>
                  </c:otherwise>
                </c:choose>
              </tbody>
            </table>
          </div>

          <!-- Sugerencia de siguiente hipótesis basada en reglas -->
          <c:if test="${not empty expSuggs}">
            <div style="padding: 0.75rem 0.95rem; border-radius: var(--radius-sm); background: rgba(196, 125, 43, 0.09); border-left: 3px solid #C47D2B; margin-bottom: 1rem;">
              <div class="cluster" style="margin-bottom: 0.25rem;">
                <span class="badge badge-estimated">Sugerencia Basada en Reglas (No es causa probada)</span>
              </div>
              <c:forEach var="sg" items="${expSuggs}">
                <div style="font-size: 0.86rem; margin-top: 0.25rem;">
                  <strong class="mono">[<c:out value="${sg.ruleCode}" />] <c:out value="${sg.title}" />:</strong>
                  <c:out value="${sg.observation}" />
                  <em><c:out value="${sg.suggestedHypothesis}" /></em>
                  → <strong>Siguiente paso sugerido:</strong> <c:out value="${sg.suggestedNextAction}" />
                </div>
              </c:forEach>
            </div>
          </c:if>

          <!-- Formularios de acción: Agregar Ensayo + Registrar Conclusión del Usuario -->
          <div class="grid-2" style="align-items: start;">
            <form method="post" action="${pageContext.request.contextPath}/app/experimentos" data-once="true">
              <input type="hidden" name="_csrf" value="${csrfToken}" />
              <input type="hidden" name="action" value="addTrial" />
              <input type="hidden" name="experimentId" value="${exp.id}" />
              <strong style="font-size: 0.86rem; display: block; margin-bottom: 0.45rem;">Vincular nuevo ensayo (ExperimentTrial)</strong>
              <div class="form-grid form-grid-3" style="align-items: end;">
                <div class="field">
                  <label class="field-label">Sesión *</label>
                  <select class="input" name="brewSessionId" required>
                    <option value="">Selecciona sesión...</option>
                    <c:forEach var="bs" items="${brewSessions}">
                      <option value="${bs.id}">
                        <c:out value="${bs.plannedMethod}" /> · <c:out value="${bs.plannedRecipeName}" /> (<c:out value="${bs.totalTimeDisplay}" />)
                      </option>
                    </c:forEach>
                  </select>
                </div>
                <div class="field">
                  <label class="field-label">Etiqueta</label>
                  <input class="input" name="label" type="text" placeholder="Ej.: Ensayo C" />
                </div>
                <div class="field">
                  <label class="field-label">Valor modificado</label>
                  <input class="input" name="modifiedValue" type="text" placeholder="Ej.: 23 clics" />
                </div>
              </div>
              <div style="margin-top: 0.5rem;">
                <button type="submit" class="btn btn-secondary btn-sm">Añadir ensayo al experimento</button>
              </div>
            </form>

            <form method="post" action="${pageContext.request.contextPath}/app/experimentos" data-once="true">
              <input type="hidden" name="_csrf" value="${csrfToken}" />
              <input type="hidden" name="action" value="concludeExperiment" />
              <input type="hidden" name="experimentId" value="${exp.id}" />
              <strong style="font-size: 0.86rem; display: block; margin-bottom: 0.45rem;">Conclusión del usuario y estado</strong>
              <div class="field">
                <textarea class="input" name="userConclusion" rows="2"
                          placeholder="Registra tu conclusión tras comparar los ensayos..."><c:out value="${exp.userConclusion}" /></textarea>
              </div>
              <div style="display: flex; gap: 0.5rem; align-items: center; justify-content: flex-end; margin-top: 0.5rem;">
                <select class="input" name="status" style="width: auto;">
                  <option value="CONCLUDED" <c:if test="${exp.status == 'CONCLUDED'}">selected</c:if>>Marcar como Concluido</option>
                  <option value="OPEN" <c:if test="${exp.status == 'OPEN'}">selected</c:if>>Mantener En curso</option>
                </select>
                <button type="submit" class="btn btn-primary btn-sm">Guardar conclusión</button>
              </div>
            </form>
          </div>
        </article>
      </c:forEach>
    </c:otherwise>
  </c:choose>
</section>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
