<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageHeading" value="Equipos y Perfiles de Agua (Equipment & Water Lab)"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<div class="grid-2">
  <!-- EQUIPOS -->
  <section class="surface-card" aria-labelledby="eq-heading">
    <div class="card-header">
      <div>
        <h2 id="eq-heading">Equipos e instrumentos</h2>
        <p class="field-hint" style="margin-bottom:0;">
          Molinos, drippers/métodos, filtros, hervidores, balanzas y refractómetros.
        </p>
      </div>
    </div>

    <c:choose>
      <c:when test="${empty equipmentList}">
        <div class="empty-state" style="margin-bottom:1rem;">
          <p style="margin:0; font-size:0.86rem; color: var(--text-secondary);">
            Aún no hay equipos registrados. Registra tu molino o método abajo para vincularlo a tus recetas y sesiones.
          </p>
        </div>
      </c:when>
      <c:otherwise>
        <div class="table-wrap" style="margin-bottom:1rem;">
          <table class="data-table">
            <thead>
              <tr>
                <th>Tipo</th>
                <th>Nombre / Marca y Modelo</th>
                <th>Escala / Detalles</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="eq" items="${equipmentList}">
                <tr>
                  <td>
                    <span class="badge badge-planned"><c:out value="${eq.typeLabel}"/></span>
                    <c:if test="${eq.demo}"><span class="badge badge-demo">[DEMO]</span></c:if>
                  </td>
                  <td>
                    <div style="font-weight:600;"><c:out value="${eq.name}"/></div>
                    <div class="field-hint">
                      <c:out value="${not empty eq.brand ? eq.brand : '—'}"/> ·
                      <c:out value="${not empty eq.model ? eq.model : '—'}"/>
                    </div>
                  </td>
                  <td>
                    <div><c:out value="${not empty eq.settingsScale ? eq.settingsScale : '—'}"/></div>
                    <c:if test="${not empty eq.notes}">
                      <div class="field-hint"><c:out value="${eq.notes}"/></div>
                    </c:if>
                  </td>
                  <td>
                    <div class="cluster">
                      <a class="btn btn-secondary btn-sm"
                         href="${pageContext.request.contextPath}/app/equipos?editEquipmentId=${fn:escapeXml(eq.id)}#form-eq">
                        Editar
                      </a>
                      <form method="post" action="${pageContext.request.contextPath}/app/equipos" style="display:inline;">
                        <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
                        <input type="hidden" name="action" value="deleteEquipment">
                        <input type="hidden" name="equipmentId" value="${fn:escapeXml(eq.id)}">
                        <button type="submit" class="btn btn-ghost btn-sm">Eliminar</button>
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

    <h3 id="form-eq">
      <c:choose>
        <c:when test="${not empty editEquipment and not empty editEquipment.id}">Editar equipo: <c:out value="${editEquipment.name}"/></c:when>
        <c:otherwise>Registrar equipo</c:otherwise>
      </c:choose>
    </h3>
    <form method="post" action="${pageContext.request.contextPath}/app/equipos" class="stack-md" data-once>
      <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
      <input type="hidden" name="action" value="saveEquipment">
      <c:if test="${not empty editEquipment}">
        <input type="hidden" name="equipmentId" value="${fn:escapeXml(editEquipment.id)}">
      </c:if>

      <div class="form-grid">
        <div class="field">
          <label class="field-label" for="eq-type">Tipo de equipo *</label>
          <select class="input" id="eq-type" name="type" required>
            <option value="molino" ${editEquipment.type == 'molino' ? 'selected' : ''}>Molino</option>
            <option value="dripper" ${editEquipment.type == 'dripper' ? 'selected' : ''}>Dripper / Método</option>
            <option value="filtro" ${editEquipment.type == 'filtro' ? 'selected' : ''}>Filtro</option>
            <option value="hervidor" ${editEquipment.type == 'hervidor' ? 'selected' : ''}>Hervidor</option>
            <option value="balanza" ${editEquipment.type == 'balanza' ? 'selected' : ''}>Balanza</option>
            <option value="refractometro" ${editEquipment.type == 'refractometro' ? 'selected' : ''}>Refractómetro</option>
            <option value="otro" ${editEquipment.type == 'otro' ? 'selected' : ''}>Otro</option>
          </select>
        </div>

        <div class="field">
          <label class="field-label" for="eq-name">Nombre identificativo *</label>
          <input class="input" id="eq-name" name="name" type="text" required maxlength="160"
                 value="${fn:escapeXml(editEquipment.name)}" placeholder="Ej. Comandante C40 MK4">
          <c:if test="${not empty eqErrors.name}">
            <span class="field-error"><c:out value="${eqErrors.name}"/></span>
          </c:if>
        </div>

        <div class="field">
          <label class="field-label" for="eq-brand">Marca</label>
          <input class="input" id="eq-brand" name="brand" type="text" maxlength="100"
                 value="${fn:escapeXml(editEquipment.brand)}" placeholder="Ej. Comandante / Hario">
        </div>

        <div class="field">
          <label class="field-label" for="eq-model">Modelo</label>
          <input class="input" id="eq-model" name="model" type="text" maxlength="120"
                 value="${fn:escapeXml(editEquipment.model)}" placeholder="Ej. V60 02 Cerámica">
        </div>

        <div class="field" style="grid-column: 1 / -1;">
          <label class="field-label" for="eq-scale">Escala de molienda / precisión / material</label>
          <input class="input" id="eq-scale" name="settingsScale" type="text" maxlength="140"
                 value="${fn:escapeXml(editEquipment.settingsScale)}" placeholder="Ej. Clics 1–40 (eje estándar) / Precisión 0,1 g">
        </div>
      </div>

      <div class="field">
        <label class="field-label" for="eq-notes">Notas del equipo</label>
        <textarea class="input" id="eq-notes" name="notes" rows="2"><c:out value="${editEquipment.notes}"/></textarea>
      </div>

      <div class="cluster">
        <button type="submit" class="btn btn-primary btn-sm">Guardar equipo</button>
        <c:if test="${not empty editEquipment}">
          <a class="btn btn-ghost btn-sm" href="${pageContext.request.contextPath}/app/equipos">Cancelar</a>
        </c:if>
      </div>
    </form>
  </section>

  <!-- PERFILES DE AGUA -->
  <section class="surface-card" aria-labelledby="water-heading">
    <div class="card-header">
      <div>
        <h2 id="water-heading">Perfiles de agua (WaterProfile)</h2>
        <p class="field-hint" style="margin-bottom:0;">
          Registra solo los parámetros medidos o conocidos. La ausencia de dato se conserva como <code>null</code> (—), nunca como 0.
        </p>
      </div>
    </div>

    <c:choose>
      <c:when test="${empty waterProfiles}">
        <div class="empty-state" style="margin-bottom:1rem;">
          <p style="margin:0; font-size:0.86rem; color: var(--text-secondary);">
            Sin perfiles de agua registrados. Puedes registrar agua filtrada incluso si desconoces su dureza o alcalinidad.
          </p>
        </div>
      </c:when>
      <c:otherwise>
        <div class="table-wrap" style="margin-bottom:1rem;">
          <table class="data-table">
            <thead>
              <tr>
                <th>Perfil / Fuente</th>
                <th class="num">TDS</th>
                <th class="num">Dureza / Alcalinidad</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="wp" items="${waterProfiles}">
                <tr>
                  <td>
                    <div style="font-weight:600;">
                      <c:out value="${wp.name}"/>
                      <c:if test="${wp.demo}"><span class="badge badge-demo">[DEMO]</span></c:if>
                    </div>
                    <div class="field-hint">
                      Fuente: <c:out value="${not empty wp.source ? wp.source : '—'}"/>
                      <c:if test="${not empty wp.minerals}"> · <c:out value="${wp.minerals}"/></c:if>
                    </div>
                  </td>
                  <td class="num"><c:out value="${wp.tdsDisplay}"/></td>
                  <td class="num">
                    GH: <c:out value="${wp.hardnessDisplay}"/><br>
                    KH: <c:out value="${wp.alkalinityDisplay}"/>
                  </td>
                  <td>
                    <div class="cluster">
                      <a class="btn btn-secondary btn-sm"
                         href="${pageContext.request.contextPath}/app/equipos?editWaterId=${fn:escapeXml(wp.id)}#form-water">
                        Editar
                      </a>
                      <form method="post" action="${pageContext.request.contextPath}/app/equipos" style="display:inline;">
                        <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
                        <input type="hidden" name="action" value="deleteWater">
                        <input type="hidden" name="waterId" value="${fn:escapeXml(wp.id)}">
                        <button type="submit" class="btn btn-ghost btn-sm">Eliminar</button>
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

    <h3 id="form-water">
      <c:choose>
        <c:when test="${not empty editWater and not empty editWater.id}">Editar perfil de agua: <c:out value="${editWater.name}"/></c:when>
        <c:otherwise>Registrar perfil de agua</c:otherwise>
      </c:choose>
    </h3>
    <form method="post" action="${pageContext.request.contextPath}/app/equipos" class="stack-md" data-once>
      <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
      <input type="hidden" name="action" value="saveWater">
      <c:if test="${not empty editWater}">
        <input type="hidden" name="waterId" value="${fn:escapeXml(editWater.id)}">
      </c:if>

      <div class="form-grid">
        <div class="field">
          <label class="field-label" for="wp-name">Nombre del perfil *</label>
          <input class="input" id="wp-name" name="name" type="text" required maxlength="160"
                 value="${fn:escapeXml(editWater.name)}" placeholder="Ej. Agua filtrada laboratorio">
          <c:if test="${not empty waterErrors.name}">
            <span class="field-error"><c:out value="${waterErrors.name}"/></span>
          </c:if>
        </div>

        <div class="field">
          <label class="field-label" for="wp-source">Fuente</label>
          <input class="input" id="wp-source" name="source" type="text" maxlength="140"
                 value="${fn:escapeXml(editWater.source)}" placeholder="Ej. Ósmosis + remineralización">
        </div>

        <div class="field">
          <label class="field-label" for="wp-tds">TDS medido (mg/L o ppm)</label>
          <input class="input tabular" id="wp-tds" name="tdsPpm" type="number" step="0.1" min="0"
                 value="${editWater.tdsPpm}" placeholder="Opcional, ej. 85">
          <c:if test="${not empty waterErrors.tdsPpm}">
            <span class="field-error"><c:out value="${waterErrors.tdsPpm}"/></span>
          </c:if>
        </div>

        <div class="field">
          <label class="field-label" for="wp-gh">Dureza total GH (mg/L CaCO₃)</label>
          <input class="input tabular" id="wp-gh" name="hardnessGh" type="number" step="0.1" min="0"
                 value="${editWater.hardnessGh}" placeholder="Opcional, ej. 55">
        </div>

        <div class="field">
          <label class="field-label" for="wp-kh">Alcalinidad KH (mg/L CaCO₃)</label>
          <input class="input tabular" id="wp-kh" name="alkalinityKh" type="number" step="0.1" min="0"
                 value="${editWater.alkalinityKh}" placeholder="Opcional, ej. 30">
        </div>

        <div class="field">
          <label class="field-label" for="wp-date">Fecha de medición</label>
          <input class="input tabular" id="wp-date" name="measuredAt" type="date"
                 value="${editWater.measuredAt}">
        </div>

        <div class="field" style="grid-column: 1 / -1;">
          <label class="field-label" for="wp-minerals">Minerales opcionales (Mg, Ca, Na, HCO₃)</label>
          <input class="input" id="wp-minerals" name="minerals" type="text" maxlength="240"
                 value="${fn:escapeXml(editWater.minerals)}" placeholder="Opcional, ej. Mg 12 mg/L, Ca 10 mg/L">
        </div>
      </div>

      <div class="cluster">
        <button type="submit" class="btn btn-primary btn-sm">Guardar perfil de agua</button>
        <c:if test="${not empty editWater}">
          <a class="btn btn-ghost btn-sm" href="${pageContext.request.contextPath}/app/equipos">Cancelar</a>
        </c:if>
      </div>
    </form>
  </section>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
