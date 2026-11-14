<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageHeading" value="Ficha de muestra"/>
<c:set var="moduleKicker" value="02 / TRAZABILIDAD"/>
<c:set var="moduleDescription" value="Origen, proceso y tostiones de un mismo café."/>
<c:set var="moduleAnchor" value="form-batch"/>
<c:set var="moduleAction" value="Registrar tostión"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="surface-card" aria-labelledby="ficha-lot-heading">
  <div class="card-header">
    <div>
      <div class="cluster">
        <a class="btn btn-ghost btn-sm" href="${pageContext.request.contextPath}/app/cafes">← Volver a Biblioteca</a>
        <code class="num"><c:out value="${lot.code}"/></code>
        <c:if test="${lot.demo}"><span class="badge badge-demo">[DEMO]</span></c:if>
        <c:if test="${lot.archived}"><span class="badge badge-warn">Archivado</span></c:if>
      </div>
      <h2 id="ficha-lot-heading" style="margin-top:0.35rem;"><c:out value="${lot.name}"/></h2>
    </div>

    <div class="cluster">
      <a class="btn btn-secondary btn-sm"
         href="${pageContext.request.contextPath}/app/cafes?editId=${fn:escapeXml(lot.id)}#form-cafe">
        Editar ficha del lote
      </a>
      <form method="post" action="${pageContext.request.contextPath}/app/cafes" style="display:inline;">
        <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
        <input type="hidden" name="id" value="${fn:escapeXml(lot.id)}">
        <input type="hidden" name="action" value="${lot.archived ? 'unarchiveLot' : 'archiveLot'}">
        <button type="submit" class="btn btn-ghost btn-sm">
          <c:out value="${lot.archived ? 'Restaurar lote' : 'Archivar lote'}"/>
        </button>
      </form>
    </div>
  </div>

  <div class="grid-4">
    <div>
      <div class="field-hint">Origen (País · Región · Municipio)</div>
      <div style="font-weight:600;"><c:out value="${lot.originDisplay}"/></div>
    </div>
    <div>
      <div class="field-hint">Finca / Productor</div>
      <div style="font-weight:600;">
        <c:out value="${not empty lot.farm ? lot.farm : '—'}"/> ·
        <c:out value="${not empty lot.producer ? lot.producer : '—'}"/>
      </div>
    </div>
    <div>
      <div class="field-hint">Especie / Variedad / Proceso</div>
      <div style="font-weight:600;">
        <c:out value="${not empty lot.species ? lot.species : '—'}"/> ·
        <c:out value="${not empty lot.variety ? lot.variety : '—'}"/> ·
        <c:out value="${not empty lot.process ? lot.process : '—'}"/>
      </div>
    </div>
    <div>
      <div class="field-hint">Altitud / Cosecha</div>
      <div style="font-weight:600;" class="tabular">
        <c:out value="${lot.altitudeDisplay}"/> · Cosecha: <c:out value="${not empty lot.harvest ? lot.harvest : '—'}"/>
      </div>
    </div>
    <div>
      <div class="field-hint">Humedad verde</div>
      <div style="font-weight:600;" class="tabular"><c:out value="${lot.moistureDisplay}"/></div>
    </div>
    <div>
      <div class="field-hint">Densidad verde</div>
      <div style="font-weight:600;" class="tabular"><c:out value="${lot.densityDisplay}"/></div>
    </div>
    <div>
      <div class="field-hint">Criba / Malla</div>
      <div style="font-weight:600;"><c:out value="${not empty lot.screenSize ? lot.screenSize : '—'}"/></div>
    </div>
    <div>
      <div class="field-hint">Recepción</div>
      <div style="font-weight:600;" class="tabular"><c:out value="${not empty lot.receivedOn ? lot.receivedOn : '—'}"/></div>
    </div>
  </div>

  <c:if test="${not empty lot.notes}">
    <p style="margin-top:0.85rem; margin-bottom:0; font-size:0.88rem; color: var(--text-secondary);">
      <strong>Notas:</strong> <c:out value="${lot.notes}"/>
    </p>
  </c:if>
</section>

<section class="surface-card" aria-labelledby="batches-heading">
  <div class="card-header">
    <div>
      <h2 id="batches-heading">Lotes de tostión asociados (RoastBatch)</h2>
      <p class="field-hint" style="margin-bottom:0;">
        Los días desde tostión se calculan dinámicamente como diferencia calendario entre hoy (<code class="num"><c:out value="${todayIso}"/></code>) y la fecha del lote elegido. No existe una ventana universal de frescura.
      </p>
    </div>
  </div>

  <c:choose>
    <c:when test="${empty batchViews}">
      <div class="empty-state">
        <h3>Sin lotes de tostión registrados para este café</h3>
        <p style="font-size:0.86rem; color: var(--text-secondary);">
          Registra abajo el primer evento de tostión para poder calcular su edad y preparar recetas guiadas.
        </p>
      </div>
    </c:when>
    <c:otherwise>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Código de Tostión</th>
              <th class="num">Fecha de Tostión</th>
              <th class="num">Edad Calculada Hoy</th>
              <th>Nivel / Tostador</th>
              <th class="num">Merma / Agtron / Peso disp.</th>
              <th>Acciones del ciclo</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="bv" items="${batchViews}">
              <c:set var="b" value="${bv.batch}"/>
              <tr>
                <td>
                  <code class="num"><c:out value="${b.batchCode}"/></code>
                  <c:if test="${b.demo}"><span class="badge badge-demo">[DEMO]</span></c:if>
                  <c:if test="${b.archived}"><span class="badge badge-warn">Archivado</span></c:if>
                  <c:if test="${not empty b.roastNotes}">
                    <div class="field-hint"><c:out value="${b.roastNotes}"/></div>
                  </c:if>
                </td>
                <td class="num"><c:out value="${b.roastDate}"/></td>
                <td class="num">
                  <c:choose>
                    <c:when test="${bv.age.future}">
                      <span class="badge badge-warn">
                        Advertencia: fecha futura (<c:out value="${bv.age.days}"/> días)
                      </span>
                    </c:when>
                    <c:otherwise>
                      <span class="badge badge-measured tabular">
                        <c:out value="${bv.age.days}"/> días desde tostión
                      </span>
                    </c:otherwise>
                  </c:choose>
                </td>
                <td>
                  <div><strong><c:out value="${not empty b.roastLevel ? b.roastLevel : '—'}"/></strong></div>
                  <div class="field-hint">
                    <c:out value="${not empty b.roaster ? b.roaster : '—'}"/>
                    <c:if test="${not empty b.machine}"> · <c:out value="${b.machine}"/></c:if>
                  </div>
                </td>
                <td class="num">
                  <c:out value="${b.weightLossDisplay}"/> ·
                  Agtron: <c:out value="${b.agtronDisplay}"/> ·
                  Disp.: <c:out value="${b.availableWeightDisplay}"/>
                </td>
                <td>
                  <div class="cluster">
                    <c:if test="${not empty applicationScope.navPreparacionEnabled}">
                      <a class="btn btn-caramel btn-sm"
                         href="${pageContext.request.contextPath}/app/preparacion?coffeeLotId=${fn:escapeXml(lot.id)}&amp;roastBatchId=${fn:escapeXml(b.id)}">
                        Preparar
                      </a>
                    </c:if>
                    <c:if test="${not empty applicationScope.navCatacionEnabled}">
                      <a class="btn btn-sage btn-sm"
                         href="${pageContext.request.contextPath}/app/catacion?coffeeLotId=${fn:escapeXml(lot.id)}&amp;roastBatchId=${fn:escapeXml(b.id)}">
                        Catar
                      </a>
                    </c:if>
                    <a class="btn btn-secondary btn-sm"
                       href="${pageContext.request.contextPath}/app/cafes/lote?id=${fn:escapeXml(lot.id)}&amp;editBatchId=${fn:escapeXml(b.id)}#form-batch">
                      Editar
                    </a>
                    <form method="post" action="${pageContext.request.contextPath}/app/cafes" style="display:inline;">
                      <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
                      <input type="hidden" name="batchId" value="${fn:escapeXml(b.id)}">
                      <input type="hidden" name="action" value="${b.archived ? 'unarchiveBatch' : 'archiveBatch'}">
                      <button type="submit" class="btn btn-ghost btn-sm">
                        <c:out value="${b.archived ? 'Restaurar' : 'Archivar'}"/>
                      </button>
                    </form>
                  </div>
                </td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
    </c:otherwise>
  </c:choose>
</section>

<section class="surface-card" id="form-batch" aria-labelledby="nuevo-batch-heading">
  <div class="card-header">
    <div>
      <h2 id="nuevo-batch-heading">
        <c:choose>
          <c:when test="${not empty editBatch and not empty editBatch.id}">
            Editar lote de tostión: <c:out value="${editBatch.batchCode}"/>
          </c:when>
          <c:otherwise>Registrar nuevo lote de tostión (RoastBatch) para <c:out value="${lot.code}"/></c:otherwise>
        </c:choose>
      </h2>
      <p class="field-hint" style="margin-bottom:0;">
        Un mismo lote de café admite múltiples lotes de tostión con fechas y perfiles distintos.
      </p>
    </div>
    <c:if test="${not empty editBatch}">
      <a class="btn btn-ghost btn-sm" href="${pageContext.request.contextPath}/app/cafes/lote?id=${fn:escapeXml(lot.id)}">Cancelar edición</a>
    </c:if>
  </div>

  <form method="post" action="${pageContext.request.contextPath}/app/cafes" class="stack-md" data-once>
    <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
    <input type="hidden" name="action" value="saveBatch">
    <input type="hidden" name="coffeeLotId" value="${fn:escapeXml(lot.id)}">
    <c:if test="${not empty editBatch}">
      <input type="hidden" name="batchId" value="${fn:escapeXml(editBatch.id)}">
    </c:if>

    <div class="form-grid">
      <div class="field">
        <label class="field-label" for="batch-code">Código del lote de tostión *</label>
        <input class="input" id="batch-code" name="batchCode" type="text" required maxlength="64"
               value="${fn:escapeXml(editBatch.batchCode)}" placeholder="Ej. T-2026-11-A">
        <c:if test="${not empty batchErrors.batchCode}">
          <span class="field-error"><c:out value="${batchErrors.batchCode}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="batch-date">Fecha de tostión (calendario local) *</label>
        <input class="input tabular" id="batch-date" name="roastDate" type="date" required
               value="${not empty editBatch.roastDate ? editBatch.roastDate : todayIso}">
        <c:if test="${not empty batchErrors.roastDate}">
          <span class="field-error"><c:out value="${batchErrors.roastDate}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="batch-level">Nivel de tostión (descriptivo)</label>
        <input class="input" id="batch-level" name="roastLevel" type="text" maxlength="60"
               value="${fn:escapeXml(editBatch.roastLevel)}" placeholder="Ej. Media-clara, Clara, Media">
      </div>

      <div class="field">
        <label class="field-label" for="batch-roaster">Tostador</label>
        <input class="input" id="batch-roaster" name="roaster" type="text" maxlength="140"
               value="${fn:escapeXml(editBatch.roaster)}">
      </div>

      <div class="field">
        <label class="field-label" for="batch-machine">Tostadora / Máquina</label>
        <input class="input" id="batch-machine" name="machine" type="text" maxlength="140"
               value="${fn:escapeXml(editBatch.machine)}" placeholder="Opcional, ej. Ikawa Pro / Loring">
      </div>

      <div class="field">
        <label class="field-label" for="batch-loss">Pérdida de masa (%)</label>
        <input class="input tabular" id="batch-loss" name="weightLossPct" type="number" step="0.1" min="0" max="99.9"
               value="${editBatch.weightLossPct}" placeholder="Opcional, ej. 13.4">
        <c:if test="${not empty batchErrors.weightLossPct}">
          <span class="field-error"><c:out value="${batchErrors.weightLossPct}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="batch-agtron">Color Agtron / instrumental</label>
        <input class="input tabular" id="batch-agtron" name="agtron" type="number" step="0.1" min="0.1"
               value="${editBatch.agtron}" placeholder="Opcional, ej. 68.5">
        <c:if test="${not empty batchErrors.agtron}">
          <span class="field-error"><c:out value="${batchErrors.agtron}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="batch-weight">Peso disponible (g)</label>
        <input class="input tabular" id="batch-weight" name="availableWeightG" type="number" step="0.1" min="0"
               value="${editBatch.availableWeightG}" placeholder="Opcional, ej. 250">
        <c:if test="${not empty batchErrors.availableWeightG}">
          <span class="field-error"><c:out value="${batchErrors.availableWeightG}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="batch-opened">Fecha de apertura del empaque</label>
        <input class="input tabular" id="batch-opened" name="openedOn" type="date"
               value="${editBatch.openedOn}">
      </div>

      <div class="field">
        <label class="field-label" for="batch-pref">Fecha de consumo preferente (usuario)</label>
        <input class="input tabular" id="batch-pref" name="preferredByDate" type="date"
               value="${editBatch.preferredByDate}">
      </div>
    </div>

    <div class="field">
      <label class="field-label" for="batch-notes">Notas del perfil de tostión</label>
      <textarea class="input" id="batch-notes" name="roastNotes" rows="2"
                placeholder="Ej. Desarrollo 1:18 (14,2 %), caída a 204 °C."><c:out value="${editBatch.roastNotes}"/></textarea>
    </div>

    <div>
      <button type="submit" class="btn btn-primary">Guardar lote de tostión</button>
    </div>
  </form>
</section>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
