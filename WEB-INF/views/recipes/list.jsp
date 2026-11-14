<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageHeading" value="Constructor de Recetas y Métodos (Recipe Builder)"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="surface-card" aria-labelledby="recipes-list-heading">
  <div class="card-header">
    <div>
      <h2 id="recipes-list-heading">Recetas reutilizables</h2>
      <p class="field-hint" style="margin-bottom:0;">
        Fórmula aplicada: <code class="num"><c:out value="${ratioFormula}"/></code>. Los parámetros de una receta son valores <strong>previstos</strong>; los valores reales se registran en cada sesión.
      </p>
    </div>
    <form method="post" action="${pageContext.request.contextPath}/app/recetas" data-once>
      <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
      <input type="hidden" name="action" value="createV60Example">
      <button type="submit" class="btn btn-secondary btn-sm">
        + Crear plantilla ilustrativa V60 (15 g / 250 g → 1:16,7)
      </button>
    </form>
  </div>

  <c:choose>
    <c:when test="${empty recipes}">
      <div class="empty-state">
        <h3>Sin recetas registradas todavía</h3>
        <p style="font-size:0.86rem; color: var(--text-secondary);">
          Crea una receta personalizada abajo o carga la plantilla ilustrativa V60 como punto de partida editable.
        </p>
      </div>
    </c:when>
    <c:otherwise>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Receta / Método</th>
              <th class="num">Dosis / Agua</th>
              <th class="num">Ratio (1:x)</th>
              <th class="num">Temp. / Tiempo</th>
              <th>Pasos y Coherencia</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="r" items="${recipes}">
              <c:set var="wList" value="${warningsByRecipe[r.id]}"/>
              <tr>
                <td>
                  <div>
                    <span class="badge badge-planned"><c:out value="${r.method}"/></span>
                    <span class="field-hint tabular">v<c:out value="${r.version}"/></span>
                    <c:if test="${r.demo}"><span class="badge badge-demo">[DEMO]</span></c:if>
                  </div>
                  <a href="${pageContext.request.contextPath}/app/recetas/detalle?id=${fn:escapeXml(r.id)}"
                     style="font-weight:650; color: var(--text-primary); text-decoration:underline;">
                    <c:out value="${r.name}"/>
                  </a>
                </td>
                <td class="num"><c:out value="${r.doseDisplay}"/> / <c:out value="${r.waterDisplay}"/></td>
                <td class="num">
                  <span class="badge badge-planned tabular"><c:out value="${r.ratioDisplay}"/></span>
                </td>
                <td class="num">
                  <c:out value="${r.temperatureDisplay}"/> · <c:out value="${r.targetTimeDisplay}"/>
                </td>
                <td>
                  <div class="tabular"><strong><c:out value="${fn:length(r.steps)}"/></strong> pasos definidos</div>
                  <c:if test="${not empty wList}">
                    <span class="badge badge-warn"><c:out value="${fn:length(wList)}"/> aviso(s)</span>
                  </c:if>
                </td>
                <td>
                  <div class="cluster">
                    <a class="btn btn-primary btn-sm"
                       href="${pageContext.request.contextPath}/app/recetas/detalle?id=${fn:escapeXml(r.id)}">
                      Editar / Pasos
                    </a>
                    <c:if test="${not empty applicationScope.navPreparacionEnabled}">
                      <a class="btn btn-caramel btn-sm"
                         href="${pageContext.request.contextPath}/app/preparacion?recipeId=${fn:escapeXml(r.id)}">
                        Preparar
                      </a>
                    </c:if>
                    <form method="post" action="${pageContext.request.contextPath}/app/recetas" style="display:inline;">
                      <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
                      <input type="hidden" name="action" value="duplicateRecipe">
                      <input type="hidden" name="id" value="${fn:escapeXml(r.id)}">
                      <button type="submit" class="btn btn-secondary btn-sm">Duplicar</button>
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

<section class="surface-card" aria-labelledby="new-recipe-heading">
  <div class="card-header">
    <div>
      <h2 id="new-recipe-heading">Crear nueva receta</h2>
      <p class="field-hint" style="margin-bottom:0;">
        Introduce dosis y agua total en gramos; el ratio 1:x se calcula automáticamente y se valida en el servidor.
      </p>
    </div>
    <span class="badge badge-planned">Parámetros previstos</span>
  </div>

  <form method="post" action="${pageContext.request.contextPath}/app/recetas" class="stack-md" data-once>
    <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
    <input type="hidden" name="action" value="saveRecipe">

    <div class="form-grid">
      <div class="field">
        <label class="field-label" for="rec-name">Nombre de la receta *</label>
        <input class="input" id="rec-name" name="name" type="text" required maxlength="180"
               value="${fn:escapeXml(editRecipe.name)}" placeholder="Ej. V60 Floral 3 Vertidos">
        <c:if test="${not empty recipeErrors.name}">
          <span class="field-error"><c:out value="${recipeErrors.name}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="rec-method">Método *</label>
        <input class="input" id="rec-method" name="method" type="text" required maxlength="80"
               list="method-suggestions" value="${fn:escapeXml(not empty editRecipe.method ? editRecipe.method : 'V60')}">
        <datalist id="method-suggestions">
          <option value="V60"></option>
          <option value="Origami"></option>
          <option value="Kalita Wave"></option>
          <option value="Chemex"></option>
          <option value="AeroPress"></option>
          <option value="Prensa Francesa"></option>
          <option value="Clever Dripper"></option>
          <option value="Espresso"></option>
        </datalist>
        <c:if test="${not empty recipeErrors.method}">
          <span class="field-error"><c:out value="${recipeErrors.method}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="rec-dose">Dosis de café (g) *</label>
        <input class="input tabular" id="rec-dose" name="doseG" type="number" step="0.1" min="0.1" required
               value="${not empty editRecipe.doseG ? editRecipe.doseG : '15'}">
        <c:if test="${not empty recipeErrors.doseG}">
          <span class="field-error"><c:out value="${recipeErrors.doseG}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="rec-water">Agua total (g) *</label>
        <input class="input tabular" id="rec-water" name="waterG" type="number" step="1" min="1" required
               value="${not empty editRecipe.waterG ? editRecipe.waterG : '250'}">
        <c:if test="${not empty recipeErrors.waterG}">
          <span class="field-error"><c:out value="${recipeErrors.waterG}"/></span>
        </c:if>
      </div>

      <div class="field">
        <span class="field-label">Ratio calculado (agua ÷ dosis)</span>
        <div class="input tabular" id="live-ratio-preview" style="background: var(--bg-subtle); font-weight:700;">
          1:16,7
        </div>
        <span class="field-hint">No confundir con rendimiento de extracción (EY).</span>
      </div>

      <div class="field">
        <label class="field-label" for="rec-temp">Temperatura objetivo (°C)</label>
        <input class="input tabular" id="rec-temp" name="temperatureC" type="number" step="0.5" min="1" max="105"
               value="${editRecipe.temperatureC}" placeholder="Ej. 93">
        <c:if test="${not empty recipeErrors.temperatureC}">
          <span class="field-error"><c:out value="${recipeErrors.temperatureC}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="rec-grind">Molienda prevista</label>
        <input class="input" id="rec-grind" name="grindSetting" type="text" maxlength="120"
               value="${fn:escapeXml(editRecipe.grindSetting)}" placeholder="Ej. 24 clics Comandante C40">
      </div>

      <div class="field">
        <label class="field-label" for="rec-tmin">Tiempo objetivo mín. (segundos)</label>
        <input class="input tabular" id="rec-tmin" name="targetTimeMinS" type="number" min="0"
               value="${editRecipe.targetTimeMinS}" placeholder="Ej. 165 (2:45)">
      </div>

      <div class="field">
        <label class="field-label" for="rec-tmax">Tiempo objetivo máx. (segundos)</label>
        <input class="input tabular" id="rec-tmax" name="targetTimeMaxS" type="number" min="0"
               value="${editRecipe.targetTimeMaxS}" placeholder="Ej. 195 (3:15)">
        <c:if test="${not empty recipeErrors.targetTimeMaxS}">
          <span class="field-error"><c:out value="${recipeErrors.targetTimeMaxS}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="rec-lot">Lote de café asociado (opcional)</label>
        <select class="input" id="rec-lot" name="coffeeLotId">
          <option value="">— Sin asociar a un café fijo —</option>
          <c:forEach var="lot" items="${coffeeLots}">
            <option value="${fn:escapeXml(lot.id)}" ${editRecipe.coffeeLotId == lot.id ? 'selected' : ''}>
              <c:out value="${lot.code} — ${lot.name}"/>
            </option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label class="field-label" for="rec-water-prof">Perfil de agua previsto (opcional)</label>
        <select class="input" id="rec-water-prof" name="waterProfileId">
          <option value="">— Sin perfil específico —</option>
          <c:forEach var="wp" items="${waterProfiles}">
            <option value="${fn:escapeXml(wp.id)}" ${editRecipe.waterProfileId == wp.id ? 'selected' : ''}>
              <c:out value="${wp.name}"/> (<c:out value="${wp.tdsDisplay}"/>)
            </option>
          </c:forEach>
        </select>
      </div>
    </div>

    <c:if test="${not empty equipmentList}">
      <div class="field">
        <span class="field-label">Equipos previstos (selecciona los que apliquen)</span>
        <div class="cluster">
          <c:forEach var="eq" items="${equipmentList}">
            <label class="cluster" style="font-size:0.84rem; background: var(--bg-subtle); padding:0.3rem 0.6rem; border-radius: var(--radius-sm); cursor:pointer;">
              <input type="checkbox" name="equipmentIds" value="${fn:escapeXml(eq.id)}">
              <span><c:out value="${eq.typeLabel}: ${eq.name}"/></span>
            </label>
          </c:forEach>
        </div>
      </div>
    </c:if>

    <div class="field">
      <label class="field-label" for="rec-notes">Notas e intención sensorial de la receta</label>
      <textarea class="input" id="rec-notes" name="notes" rows="2"><c:out value="${editRecipe.notes}"/></textarea>
    </div>

    <div>
      <button type="submit" class="btn btn-primary">Crear receta y configurar pasos →</button>
    </div>
  </form>
</section>

<script>
(function () {
  var doseInput = document.getElementById('rec-dose');
  var waterInput = document.getElementById('rec-water');
  var preview = document.getElementById('live-ratio-preview');
  if (!doseInput || !waterInput || !preview) return;
  function updateRatio() {
    var d = parseFloat(doseInput.value);
    var w = parseFloat(waterInput.value);
    if (isNaN(d) || isNaN(w)) {
      preview.textContent = '— (Completa dosis y agua)';
    } else if (d <= 0) {
      preview.textContent = 'Bloqueado (dosis > 0 g requerida)';
    } else if (w < 0) {
      preview.textContent = 'Agua inválida';
    } else {
      var r = (w / d).toFixed(1).replace('.', ',');
      preview.textContent = '1:' + r;
    }
  }
  doseInput.addEventListener('input', updateRatio);
  waterInput.addEventListener('input', updateRatio);
  updateRatio();
})();
</script>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
