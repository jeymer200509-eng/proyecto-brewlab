<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageHeading" value="${recipe.name} (${recipe.method})"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="surface-card" aria-labelledby="recipe-header-heading">
  <div class="card-header">
    <div>
      <div class="cluster">
        <a class="btn btn-ghost btn-sm" href="${pageContext.request.contextPath}/app/recetas">← Volver a Recetas</a>
        <span class="badge badge-planned"><c:out value="${recipe.method}"/></span>
        <span class="badge badge-planned tabular">Ratio previsto <c:out value="${recipe.ratioDisplay}"/></span>
        <c:if test="${recipe.demo}"><span class="badge badge-demo">[DEMO]</span></c:if>
      </div>
      <h2 id="recipe-header-heading" style="margin-top:0.35rem;"><c:out value="${recipe.name}"/></h2>
      <p class="field-hint" style="margin-bottom:0;">
        Fórmula: <code class="num"><c:out value="${ratioFormula}"/></code> →
        <strong class="tabular"><c:out value="${recipe.waterDisplay}"/> ÷ <c:out value="${recipe.doseDisplay}"/> = <c:out value="${recipe.ratioDisplay}"/></strong>
      </p>
    </div>

    <div class="cluster">
      <c:if test="${not empty applicationScope.navPreparacionEnabled}">
        <a class="btn btn-caramel btn-sm"
           href="${pageContext.request.contextPath}/app/preparacion?recipeId=${fn:escapeXml(recipe.id)}">
          Ejecutar en Brew Assistant →
        </a>
      </c:if>
      <form method="post" action="${pageContext.request.contextPath}/app/recetas" style="display:inline;">
        <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
        <input type="hidden" name="action" value="duplicateRecipe">
        <input type="hidden" name="id" value="${fn:escapeXml(recipe.id)}">
        <button type="submit" class="btn btn-secondary btn-sm">Duplicar receta</button>
      </form>
      <form method="post" action="${pageContext.request.contextPath}/app/recetas" style="display:inline;">
        <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
        <input type="hidden" name="action" value="deleteRecipe">
        <input type="hidden" name="id" value="${fn:escapeXml(recipe.id)}">
        <button type="submit" class="btn btn-danger btn-sm">Eliminar</button>
      </form>
    </div>
  </div>

  <c:if test="${not empty coherenceWarnings}">
    <div class="alert alert-warn" role="status">
      <div>
        <strong>Avisos de coherencia de la receta (no bloquean pruebas manuales):</strong>
        <ul style="margin:0.35rem 0 0 1.1rem; padding:0;">
          <c:forEach var="w" items="${coherenceWarnings}">
            <li><c:out value="${w}"/></li>
          </c:forEach>
        </ul>
      </div>
    </div>
  </c:if>

  <details>
    <summary style="cursor:pointer; font-weight:600; font-size:0.9rem; color: var(--accent-caramel);">
      Editar parámetros previstos de la cabecera (dosis, agua, temperatura, molienda, equipo, agua)
    </summary>
    <form method="post" action="${pageContext.request.contextPath}/app/recetas" class="stack-md" style="margin-top:0.9rem;">
      <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
      <input type="hidden" name="action" value="saveRecipe">
      <input type="hidden" name="id" value="${fn:escapeXml(recipe.id)}">

      <div class="form-grid">
        <div class="field">
          <label class="field-label" for="edit-name">Nombre *</label>
          <input class="input" id="edit-name" name="name" type="text" required value="${fn:escapeXml(recipe.name)}">
        </div>
        <div class="field">
          <label class="field-label" for="edit-method">Método *</label>
          <input class="input" id="edit-method" name="method" type="text" required value="${fn:escapeXml(recipe.method)}">
        </div>
        <div class="field">
          <label class="field-label" for="edit-dose">Dosis prevista (g) *</label>
          <input class="input tabular" id="edit-dose" name="doseG" type="number" step="0.1" min="0.1" required value="${recipe.doseG}">
        </div>
        <div class="field">
          <label class="field-label" for="edit-water">Agua total prevista (g) *</label>
          <input class="input tabular" id="edit-water" name="waterG" type="number" step="1" min="1" required value="${recipe.waterG}">
        </div>
        <div class="field">
          <label class="field-label" for="edit-temp">Temperatura objetivo (°C)</label>
          <input class="input tabular" id="edit-temp" name="temperatureC" type="number" step="0.5" value="${recipe.temperatureC}">
        </div>
        <div class="field">
          <label class="field-label" for="edit-grind">Molienda prevista</label>
          <input class="input" id="edit-grind" name="grindSetting" type="text" value="${fn:escapeXml(recipe.grindSetting)}">
        </div>
        <div class="field">
          <label class="field-label" for="edit-tmin">Tiempo mín. (s)</label>
          <input class="input tabular" id="edit-tmin" name="targetTimeMinS" type="number" min="0" value="${recipe.targetTimeMinS}">
        </div>
        <div class="field">
          <label class="field-label" for="edit-tmax">Tiempo máx. (s)</label>
          <input class="input tabular" id="edit-tmax" name="targetTimeMaxS" type="number" min="0" value="${recipe.targetTimeMaxS}">
        </div>
        <div class="field">
          <label class="field-label" for="edit-lot">Lote de café asociado</label>
          <select class="input" id="edit-lot" name="coffeeLotId">
            <option value="">— Ninguno —</option>
            <c:forEach var="lot" items="${coffeeLots}">
              <option value="${fn:escapeXml(lot.id)}" ${recipe.coffeeLotId == lot.id ? 'selected' : ''}>
                <c:out value="${lot.code} — ${lot.name}"/>
              </option>
            </c:forEach>
          </select>
        </div>
        <div class="field">
          <label class="field-label" for="edit-wp">Perfil de agua previsto</label>
          <select class="input" id="edit-wp" name="waterProfileId">
            <option value="">— Ninguno —</option>
            <c:forEach var="wp" items="${waterProfiles}">
              <option value="${fn:escapeXml(wp.id)}" ${recipe.waterProfileId == wp.id ? 'selected' : ''}>
                <c:out value="${wp.name}"/> (<c:out value="${wp.tdsDisplay}"/>)
              </option>
            </c:forEach>
          </select>
        </div>
      </div>

      <c:if test="${not empty equipmentList}">
        <div class="field">
          <span class="field-label">Equipos previstos</span>
          <div class="cluster">
            <c:forEach var="eq" items="${equipmentList}">
              <c:set var="isChecked" value="false"/>
              <c:forEach var="selId" items="${recipe.equipmentIds}">
                <c:if test="${selId == eq.id}"><c:set var="isChecked" value="true"/></c:if>
              </c:forEach>
              <label class="cluster" style="font-size:0.84rem; background: var(--bg-subtle); padding:0.3rem 0.6rem; border-radius: var(--radius-sm); cursor:pointer;">
                <input type="checkbox" name="equipmentIds" value="${fn:escapeXml(eq.id)}" ${isChecked ? 'checked' : ''}>
                <span><c:out value="${eq.typeLabel}: ${eq.name}"/></span>
              </label>
            </c:forEach>
          </div>
        </div>
      </c:if>

      <div class="field">
        <label class="field-label" for="edit-notes">Notas</label>
        <textarea class="input" id="edit-notes" name="notes" rows="2"><c:out value="${recipe.notes}"/></textarea>
      </div>

      <button type="submit" class="btn btn-primary btn-sm">Guardar cabecera de receta</button>
    </form>
  </details>
</section>

<!-- PASOS ORDENADOS (RecipeStep) -->
<section class="surface-card" id="pasos-receta" aria-labelledby="steps-heading">
  <div class="card-header">
    <div>
      <h2 id="steps-heading">Pasos ordenados de preparación (RecipeStep)</h2>
      <p class="field-hint" style="margin-bottom:0;">
        Crea, edita, duplica, reordena (↑/↓) o elimina etapas. Cada paso define inicio objetivo, duración opcional, agua del paso y agua acumulada objetivo.
      </p>
    </div>
    <span class="badge badge-planned">Agua total prevista: <c:out value="${recipe.waterDisplay}"/></span>
  </div>

  <c:choose>
    <c:when test="${empty recipe.steps}">
      <div class="empty-state" style="margin-bottom:1rem;">
        <p style="margin:0; font-size:0.86rem; color: var(--text-secondary);">
          Esta receta aún no tiene pasos. Agrega el primer paso (ej. Preinfusión en 0:00) abajo.
        </p>
      </div>
    </c:when>
    <c:otherwise>
      <div class="table-wrap" style="margin-bottom:1rem;">
        <table class="data-table">
          <thead>
            <tr>
              <th class="num">#</th>
              <th class="num">Inicio / Duración</th>
              <th>Etapa e Instrucción</th>
              <th class="num">Agua del paso</th>
              <th class="num">Agua acumulada</th>
              <th class="num">Tolerancia</th>
              <th>Ordenar / Acciones</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="step" items="${recipe.steps}" varStatus="st">
              <tr>
                <td class="num"><strong><c:out value="${step.order}"/></strong></td>
                <td class="num">
                  <div><strong><c:out value="${step.startFormatted}"/></strong> (<c:out value="${step.startSeconds}"/> s)</div>
                  <div class="field-hint">Dur.: <c:out value="${step.durationFormatted}"/></div>
                </td>
                <td>
                  <div style="font-weight:600;"><c:out value="${step.title}"/></div>
                  <div style="font-size:0.84rem; color: var(--text-secondary);"><c:out value="${step.instruction}"/></div>
                </td>
                <td class="num"><c:out value="${step.waterDoseFormatted}"/></td>
                <td class="num"><strong><c:out value="${step.cumulativeWaterFormatted}"/></strong></td>
                <td class="num">
                  <c:choose>
                    <c:when test="${not empty step.toleranceSeconds}">±<c:out value="${step.toleranceSeconds}"/> s</c:when>
                    <c:otherwise>—</c:otherwise>
                  </c:choose>
                </td>
                <td>
                  <div class="cluster">
                    <c:if test="${not st.first}">
                      <form method="post" action="${pageContext.request.contextPath}/app/recetas" style="display:inline;">
                        <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
                        <input type="hidden" name="action" value="moveStepUp">
                        <input type="hidden" name="recipeId" value="${fn:escapeXml(recipe.id)}">
                        <input type="hidden" name="stepId" value="${fn:escapeXml(step.id)}">
                        <button type="submit" class="btn btn-secondary btn-sm" title="Subir paso">↑</button>
                      </form>
                    </c:if>
                    <c:if test="${not st.last}">
                      <form method="post" action="${pageContext.request.contextPath}/app/recetas" style="display:inline;">
                        <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
                        <input type="hidden" name="action" value="moveStepDown">
                        <input type="hidden" name="recipeId" value="${fn:escapeXml(recipe.id)}">
                        <input type="hidden" name="stepId" value="${fn:escapeXml(step.id)}">
                        <button type="submit" class="btn btn-secondary btn-sm" title="Bajar paso">↓</button>
                      </form>
                    </c:if>
                    <a class="btn btn-secondary btn-sm"
                       href="${pageContext.request.contextPath}/app/recetas/detalle?id=${fn:escapeXml(recipe.id)}&amp;editStepId=${fn:escapeXml(step.id)}#form-step">
                      Editar
                    </a>
                    <form method="post" action="${pageContext.request.contextPath}/app/recetas" style="display:inline;">
                      <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
                      <input type="hidden" name="action" value="duplicateStep">
                      <input type="hidden" name="recipeId" value="${fn:escapeXml(recipe.id)}">
                      <input type="hidden" name="stepId" value="${fn:escapeXml(step.id)}">
                      <button type="submit" class="btn btn-secondary btn-sm">Duplicar</button>
                    </form>
                    <form method="post" action="${pageContext.request.contextPath}/app/recetas" style="display:inline;">
                      <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
                      <input type="hidden" name="action" value="deleteStep">
                      <input type="hidden" name="recipeId" value="${fn:escapeXml(recipe.id)}">
                      <input type="hidden" name="stepId" value="${fn:escapeXml(step.id)}">
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

  <h3 id="form-step">
    <c:choose>
      <c:when test="${not empty editStep}">Editar paso: <c:out value="${editStep.title}"/></c:when>
      <c:otherwise>Agregar paso temporizado</c:otherwise>
    </c:choose>
  </h3>
  <form method="post" action="${pageContext.request.contextPath}/app/recetas" class="stack-md" data-once>
    <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
    <input type="hidden" name="action" value="saveStep">
    <input type="hidden" name="recipeId" value="${fn:escapeXml(recipe.id)}">
    <c:if test="${not empty editStep}">
      <input type="hidden" name="stepId" value="${fn:escapeXml(editStep.id)}">
    </c:if>

    <div class="form-grid">
      <div class="field">
        <label class="field-label" for="step-title">Título de la etapa *</label>
        <input class="input" id="step-title" name="title" type="text" required maxlength="140"
               value="${fn:escapeXml(editStep.title)}" placeholder="Ej. Preinfusión / Primer vertido">
        <c:if test="${not empty stepErrors.title}">
          <span class="field-error"><c:out value="${stepErrors.title}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="step-start">Inicio objetivo (segundos desde 00:00) *</label>
        <input class="input tabular" id="step-start" name="startSeconds" type="number" min="0" required
               value="${not empty editStep ? editStep.startSeconds : '0'}">
        <c:if test="${not empty stepErrors.startSeconds}">
          <span class="field-error"><c:out value="${stepErrors.startSeconds}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="step-dur">Duración objetivo (s, opcional)</label>
        <input class="input tabular" id="step-dur" name="durationSeconds" type="number" min="0"
               value="${editStep.durationSeconds}" placeholder="Ej. 30">
      </div>

      <div class="field">
        <label class="field-label" for="step-water">Agua de este vertido (g, opcional)</label>
        <input class="input tabular" id="step-water" name="waterDoseG" type="number" step="0.1" min="0"
               value="${editStep.waterDoseG}" placeholder="Ej. 45">
      </div>

      <div class="field">
        <label class="field-label" for="step-cum">Agua acumulada objetivo (g, opcional)</label>
        <input class="input tabular" id="step-cum" name="cumulativeWaterG" type="number" step="0.1" min="0"
               value="${editStep.cumulativeWaterG}" placeholder="Ej. 150">
      </div>

      <div class="field">
        <label class="field-label" for="step-tol">Tolerancia opcional (± segundos)</label>
        <input class="input tabular" id="step-tol" name="toleranceSeconds" type="number" min="0"
               value="${editStep.toleranceSeconds}" placeholder="Ej. 5">
      </div>
    </div>

    <div class="field">
      <label class="field-label" for="step-instr">Instrucción clara para el usuario *</label>
      <input class="input" id="step-instr" name="instruction" type="text" required
             value="${fn:escapeXml(editStep.instruction)}"
             placeholder="Ej. Verter en espiral desde el centro hasta alcanzar 150 g acumulados.">
      <c:if test="${not empty stepErrors.instruction}">
        <span class="field-error"><c:out value="${stepErrors.instruction}"/></span>
      </c:if>
    </div>

    <div class="cluster">
      <button type="submit" class="btn btn-primary btn-sm">Guardar paso</button>
      <c:if test="${not empty editStep}">
        <a class="btn btn-ghost btn-sm"
           href="${pageContext.request.contextPath}/app/recetas/detalle?id=${fn:escapeXml(recipe.id)}#pasos-receta">
          Cancelar edición de paso
        </a>
      </c:if>
    </div>
  </form>
</section>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
