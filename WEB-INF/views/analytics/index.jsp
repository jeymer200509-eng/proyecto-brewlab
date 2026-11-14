<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageHeading" value="Historial, Analítica y Comparador de Sesiones"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="surface-card card" style="margin-bottom: 1rem;">
  <div class="card-header" style="margin-bottom: 0;">
    <div>
      <div class="field-hint">Módulo 7 · Analytics &amp; Comparison</div>
      <h2 style="margin-top: 0.25rem;">Historial, Analítica y Comparador de Sesiones</h2>
      <p class="field-hint" style="margin-bottom: 0;">
        Filtra preparaciones por café, tostión, método, receta, equipo y fechas; selecciona dos o más sesiones
        para contrastar parámetros previstos vs. medidos, perfil sensorial y advertencias metodológicas.
      </p>
    </div>
    <div class="cluster">
      <span class="badge badge-measured tabular mono">Muestra filtrada: N = <c:out value="${fn:length(filteredSessions)}" /> / <c:out value="${totalSessionCount}" /></span>
      <a class="btn btn-secondary btn-sm" href="${pageContext.request.contextPath}/app/experimentos">Ir a Experiment Lab →</a>
    </div>
  </div>
</section>

<!-- 1. Filtros multicriterio de historial -->
<section class="surface-card card" style="margin-bottom: 1.5rem;">
  <div class="card-header">
    <div>
      <span class="badge badge-planned">Filtro de Historial</span>
      <h2 class="card-title" style="margin-top: 0.35rem;">Acotar Sesiones por Trazabilidad y Parámetros</h2>
    </div>
    <a class="btn btn-secondary btn-sm" href="${pageContext.request.contextPath}/app/analisis">Limpiar filtros</a>
  </div>

  <form method="get" action="${pageContext.request.contextPath}/app/analisis">
    <div class="form-grid form-grid-4">
      <div class="field">
        <label class="field-label" for="filterLotId">Café (CoffeeLot)</label>
        <select class="input" id="filterLotId" name="coffeeLotId">
          <option value="">Todos los lotes</option>
          <c:forEach var="lot" items="${coffeeLots}">
            <option value="${lot.id}" <c:if test="${lot.id == filterLotId}">selected</c:if>>
              [<c:out value="${lot.code}" />] <c:out value="${lot.name}" />
            </option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label class="field-label" for="filterBatchId">Tostión (RoastBatch)</label>
        <select class="input" id="filterBatchId" name="roastBatchId">
          <option value="">Todos los tuestes</option>
          <c:forEach var="rb" items="${roastBatches}">
            <option value="${rb.id}" <c:if test="${rb.id == filterBatchId}">selected</c:if>>
              [<c:out value="${rb.code}" />] <c:out value="${rb.roastLevel}" /> (<c:out value="${rb.roastDateDisplay}" />)
            </option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label class="field-label" for="filterMethod">Método</label>
        <select class="input" id="filterMethod" name="method">
          <option value="">Todos los métodos</option>
          <c:forEach var="m" items="${availableMethods}">
            <option value="${m}" <c:if test="${m == filterMethod}">selected</c:if>><c:out value="${m}" /></option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label class="field-label" for="filterRecipeId">Receta</label>
        <select class="input" id="filterRecipeId" name="recipeId">
          <option value="">Todas las recetas</option>
          <c:forEach var="r" items="${recipes}">
            <option value="${r.id}" <c:if test="${r.id == filterRecipeId}">selected</c:if>>
              <c:out value="${r.name}" /> (<c:out value="${r.method}" />)
            </option>
          </c:forEach>
        </select>
      </div>
    </div>

    <div class="form-grid form-grid-4" style="margin-top: 0.75rem; align-items: end;">
      <div class="field">
        <label class="field-label" for="filterEquipmentId">Equipo utilizado</label>
        <select class="input" id="filterEquipmentId" name="equipmentId">
          <option value="">Cualquier equipo</option>
          <c:forEach var="equip" items="${equipmentList}">
            <option value="${equip.id}" <c:if test="${equip.id == filterEquipmentId}">selected</c:if>>
              <c:out value="${equip.typeLabel}" />: <c:out value="${equip.displayName}" />
            </option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label class="field-label" for="dateFrom">Desde fecha</label>
        <input class="input" id="dateFrom" name="dateFrom" type="date" value="<c:out value='${filterDateFrom}' />" />
      </div>

      <div class="field">
        <label class="field-label" for="dateTo">Hasta fecha</label>
        <input class="input" id="dateTo" name="dateTo" type="date" value="<c:out value='${filterDateTo}' />" />
      </div>

      <div>
        <button type="submit" class="btn btn-primary" style="width: 100%;">Aplicar filtros</button>
      </div>
    </div>
  </form>
</section>

<!-- 2. Selección de sesiones en el historial -->
<section class="surface-card card" style="margin-bottom: 1.5rem;">
  <form method="get" action="${pageContext.request.contextPath}/app/analisis">
    <input type="hidden" name="coffeeLotId" value="<c:out value='${filterLotId}' />" />
    <input type="hidden" name="roastBatchId" value="<c:out value='${filterBatchId}' />" />
    <input type="hidden" name="method" value="<c:out value='${filterMethod}' />" />
    <input type="hidden" name="recipeId" value="<c:out value='${filterRecipeId}' />" />
    <input type="hidden" name="equipmentId" value="<c:out value='${filterEquipmentId}' />" />
    <input type="hidden" name="dateFrom" value="<c:out value='${filterDateFrom}' />" />
    <input type="hidden" name="dateTo" value="<c:out value='${filterDateTo}' />" />

    <div class="card-header">
      <div>
        <span class="badge badge-measured">Selección Comparativa</span>
        <h2 class="card-title" style="margin-top: 0.35rem;">
          Sesiones Encontradas (<span class="tabular mono">N = <c:out value="${fn:length(filteredSessions)}" /></span>)
        </h2>
        <p class="field-hint card-subtitle" style="margin-bottom: 0;">
          Marca dos o más sesiones en la primera columna y pulsa <strong>«Comparar sesiones seleccionadas»</strong>.
        </p>
      </div>
      <button type="submit" class="btn btn-primary">Comparar sesiones seleccionadas</button>
    </div>

    <c:choose>
      <c:when test="${empty filteredSessions}">
        <div class="empty-state">
          <p>No hay sesiones que coincidan con los criterios actuales.</p>
          <a class="btn btn-secondary btn-sm" href="${pageContext.request.contextPath}/app/preparacion?new=true">Ejecutar nueva sesión</a>
        </div>
      </c:when>
      <c:otherwise>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th style="width: 44px;">Comp.</th>
                <th>Receta / Método</th>
                <th>Café y Tostión</th>
                <th>Estado</th>
                <th>Previsto (<span class="mono">g · 1:x · °C</span>)</th>
                <th>Medido Real (<span class="mono">g · 1:x · °C · s</span>)</th>
                <th>Evaluación Sensorial</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="s" items="${filteredSessions}">
                <c:set var="sLot" value="${lotMap[s.coffeeLotId]}" />
                <c:set var="sBatch" value="${batchMap[s.roastBatchId]}" />
                <c:set var="sAssess" value="${latestAssessmentBySession[s.id]}" />
                <c:set var="isChecked" value="${false}" />
                <c:forEach var="cid" items="${selectedCompareIds}">
                  <c:if test="${cid == s.id}">
                    <c:set var="isChecked" value="${true}" />
                  </c:if>
                </c:forEach>
                <tr>
                  <td>
                    <input type="checkbox" name="compareId" value="${s.id}"
                           aria-label="Seleccionar sesión ${s.plannedRecipeName} para comparar"
                           <c:if test="${isChecked}">checked</c:if> />
                  </td>
                  <td>
                    <strong><c:out value="${s.plannedRecipeName}" /></strong>
                    <div class="muted" style="font-size: 0.8rem;"><c:out value="${s.plannedMethod}" /></div>
                  </td>
                  <td>
                    <c:choose>
                      <c:when test="${not empty sLot}">
                        [<c:out value="${sLot.code}" />] <c:out value="${sLot.name}" />
                      </c:when>
                      <c:otherwise><span class="mono muted">—</span></c:otherwise>
                    </c:choose>
                    <c:if test="${not empty sBatch}">
                      <div class="muted" style="font-size: 0.78rem;">
                        Tueste <c:out value="${sBatch.code}" /> (<c:out value="${sBatch.roastLevel}" />)
                      </div>
                    </c:if>
                  </td>
                  <td><span class="badge badge-measured"><c:out value="${s.statusLabel}" /></span></td>
                  <td class="num">
                    <c:out value="${s.plannedDoseGDisplay}" /> / <c:out value="${s.plannedWaterGDisplay}" />
                    (<c:out value="${s.plannedRatioDisplay}" />) · <c:out value="${s.plannedTemperatureCDisplay}" />
                  </td>
                  <td class="num">
                    <c:out value="${s.actualDoseGDisplay}" /> / <c:out value="${s.actualWaterGDisplay}" />
                    (<c:out value="${s.actualRatioDisplay}" />) · <c:out value="${s.actualTemperatureCDisplay}" />
                    · <strong><c:out value="${s.totalTimeDisplay}" /></strong>
                  </td>
                  <td>
                    <c:choose>
                      <c:when test="${not empty sAssess}">
                        <span class="badge badge-measured">
                          Sabor <c:out value="${sAssess.flavorDisplay}" /> · Balance <c:out value="${sAssess.balanceDisplay}" />
                          (<c:out value="${sAssess.attributeScale}" />)
                        </span>
                      </c:when>
                      <c:otherwise>
                        <a class="btn btn-secondary btn-sm" href="${pageContext.request.contextPath}/app/catacion?sessionId=${s.id}">
                          Evaluar taza
                        </a>
                      </c:otherwise>
                    </c:choose>
                  </td>
                  <td>
                    <a class="btn btn-secondary btn-sm" href="${pageContext.request.contextPath}/app/preparacion?sessionId=${s.id}">
                      Ver sesión
                    </a>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
      </c:otherwise>
    </c:choose>
  </form>
</section>

<!-- 3. Comparador lado a lado + Gráfico SVG con ejes/unidades/leyenda/fuente tabular -->
<c:if test="${comparison.sampleSize >= 2}">
  <section class="surface-card card" style="margin-bottom: 1.5rem;">
    <div class="card-header">
      <div>
        <span class="badge badge-measured">Comparación Multisesión Activa</span>
        <h2 class="card-title" style="margin-top: 0.35rem;">
          Contraste de Parámetros Previstos, Reales, Sensoriales y Afectivos
        </h2>
        <p class="field-hint card-subtitle" style="margin-bottom: 0;">
          Tamaño de muestra comparada: <strong class="tabular mono">N = <c:out value="${comparison.sampleSize}" /> sesiones</strong>.
          Unidades explícitas: masa en <span class="mono">g</span>, temperatura en <span class="mono">°C</span>, tiempo en <span class="mono">m:ss</span>, ratio como <span class="mono">1:x</span>.
          Datos faltantes o no medidos representados como <span class="mono">—</span> (nunca <span class="mono">0</span>).
        </p>
      </div>
    </div>

    <!-- Advertencias metodológicas de contexto y no-causalidad -->
    <c:if test="${not empty comparison.contextWarnings}">
      <div class="stack-sm stack" style="margin-bottom: 1.15rem;">
        <c:forEach var="warn" items="${comparison.contextWarnings}">
          <div class="alert alert-warn flash flash-warn" role="status" style="margin-bottom: 0;">
            <div><strong>Advertencia de comparabilidad:</strong> <c:out value="${warn}" /></div>
          </div>
        </c:forEach>
      </div>
    </c:if>

    <!-- Leyenda de sesiones comparadas -->
    <div class="cluster" style="margin-bottom: 1rem;">
      <c:forEach var="item" items="${comparison.items}" varStatus="st">
        <span class="badge" style="border: 1px solid ${item.colorHex}; background: rgba(35,25,21,0.04);">
          <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${item.colorHex};margin-right:5px;"></span>
          <strong>S<c:out value="${st.count}" />:</strong>
          <c:out value="${item.session.plannedRecipeName}" /> (<c:out value="${item.session.plannedMethod}" />,
          Ratio real <c:out value="${item.session.actualRatioDisplay}" />, Tiempo <c:out value="${item.session.totalTimeDisplay}" />)
        </span>
      </c:forEach>
    </div>

    <!-- Gráfico SVG comparativo con ejes, unidad, tamaño de muestra y leyenda -->
    <div style="border: 1px solid var(--border-subtle); border-radius: var(--radius-md); padding: 1rem; background: rgba(255,255,255,0.65); margin-bottom: 1.25rem;">
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 0.5rem;">
        <strong style="font-size: 0.92rem;">
          Gráfico SVG — Perfil Sensorial Descriptivo Comparado (Eje Y: Intensidad en escala 0 a <c:out value="${comparison.sensoryChartMax}" /> pts · Muestra N = <c:out value="${comparison.sampleSize}" />)
        </strong>
        <span class="mono muted" style="font-size: 0.78rem;">Fuente: tabla comparativa inferior · Null = Sin barra (—)</span>
      </div>

      <svg viewBox="0 0 760 220" role="img"
           aria-label="Gráfico comparativo de atributos sensoriales con ejes, unidades y leyenda"
           style="width: 100%; height: auto; max-height: 240px;">
        <!-- Líneas guía horizontales y etiquetas del eje Y -->
        <line x1="56" y1="25" x2="740" y2="25" stroke="#E2D9CC" stroke-dasharray="3 3" />
        <text x="48" y="29" text-anchor="end" font-size="10" fill="#6D5D53"><c:out value="${comparison.sensoryChartMax}" /> pts</text>

        <line x1="56" y1="95" x2="740" y2="95" stroke="#E2D9CC" stroke-dasharray="3 3" />
        <text x="48" y="99" text-anchor="end" font-size="10" fill="#6D5D53">Media</text>

        <line x1="56" y1="165" x2="740" y2="165" stroke="#231915" stroke-width="1.2" />
        <text x="48" y="169" text-anchor="end" font-size="10" fill="#231915">0 pts</text>

        <!-- 8 grupos de atributos en el eje X -->
        <c:set var="attrNames" value="${fn:split('Fragancia,Sabor,Acidez,Dulzor,Amargor,Cuerpo,Posgusto,Balance', ',')}" />
        <c:forEach var="attrLabel" items="${attrNames}" varStatus="attrIdx">
          <c:set var="groupX" value="${72 + attrIdx.index * 83}" />
          <text x="${groupX + 26}" y="186" text-anchor="middle" font-size="10.5" font-weight="600" fill="#231915">
            <c:out value="${attrLabel}" />
          </text>

          <c:forEach var="item" items="${comparison.items}" varStatus="sIdx">
            <c:if test="${sIdx.index < 4}">
              <c:set var="barX" value="${groupX + sIdx.index * 14}" />
              <c:set var="val" value="${null}" />
              <c:if test="${not empty item.assessment}">
                <c:choose>
                  <c:when test="${attrIdx.index == 0}"><c:set var="val" value="${item.assessment.fragranceAroma}" /></c:when>
                  <c:when test="${attrIdx.index == 1}"><c:set var="val" value="${item.assessment.flavor}" /></c:when>
                  <c:when test="${attrIdx.index == 2}"><c:set var="val" value="${item.assessment.acidity}" /></c:when>
                  <c:when test="${attrIdx.index == 3}"><c:set var="val" value="${item.assessment.sweetness}" /></c:when>
                  <c:when test="${attrIdx.index == 4}"><c:set var="val" value="${item.assessment.bitterness}" /></c:when>
                  <c:when test="${attrIdx.index == 5}"><c:set var="val" value="${item.assessment.bodyTexture}" /></c:when>
                  <c:when test="${attrIdx.index == 6}"><c:set var="val" value="${item.assessment.aftertaste}" /></c:when>
                  <c:when test="${attrIdx.index == 7}"><c:set var="val" value="${item.assessment.balance}" /></c:when>
                </c:choose>
              </c:if>
              <c:choose>
                <c:when test="${not empty val}">
                  <c:set var="barH" value="${item.getSvgBarHeight(val, comparison.sensoryChartMax)}" />
                  <rect x="${barX}" y="${165 - barH}" width="11" height="${barH}"
                        fill="${item.colorHex}" rx="2">
                    <title>S${sIdx.count} ${attrLabel}: ${val}</title>
                  </rect>
                </c:when>
                <c:otherwise>
                  <text x="${barX + 5}" y="161" text-anchor="middle" font-size="9" fill="#8C7A6B">—</text>
                </c:otherwise>
              </c:choose>
            </c:if>
          </c:forEach>
        </c:forEach>
      </svg>
    </div>

    <!-- Fuente tabular completa lado a lado -->
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>Dimensión / Parámetro (Unidad)</th>
            <th>Tipo de Dato</th>
            <c:forEach var="item" items="${comparison.items}" varStatus="st">
              <th>
                <span style="display:inline-block;width:9px;height:9px;border-radius:50%;background:${item.colorHex};margin-right:4px;"></span>
                S<c:out value="${st.count}" /> · <c:out value="${item.session.plannedRecipeName}" />
              </th>
            </c:forEach>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><strong>Lote de café (CoffeeLot)</strong></td>
            <td><span class="badge badge-measured">Contexto</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <c:set var="cLot" value="${lotMap[item.session.coffeeLotId]}" />
              <td>
                <c:choose>
                  <c:when test="${not empty cLot}">[<c:out value="${cLot.code}" />] <c:out value="${cLot.name}" /></c:when>
                  <c:otherwise><span class="mono muted">—</span></c:otherwise>
                </c:choose>
              </td>
            </c:forEach>
          </tr>
          <tr>
            <td><strong>Lote de tostión (RoastBatch)</strong></td>
            <td><span class="badge badge-measured">Contexto</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <c:set var="cBatch" value="${batchMap[item.session.roastBatchId]}" />
              <td>
                <c:choose>
                  <c:when test="${not empty cBatch}">[<c:out value="${cBatch.code}" />] <c:out value="${cBatch.roastLevel}" /> (<c:out value="${cBatch.roastDateDisplay}" />)</c:when>
                  <c:otherwise><span class="mono muted">—</span></c:otherwise>
                </c:choose>
              </td>
            </c:forEach>
          </tr>
          <tr>
            <td><strong>Dosis prevista vs. medida (g)</strong></td>
            <td><span class="badge badge-planned">Previsto</span> / <span class="badge badge-measured">Medido</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <td class="num">
                <span class="muted"><c:out value="${item.session.plannedDoseGDisplay}" /></span> →
                <strong><c:out value="${item.session.actualDoseGDisplay}" /></strong>
              </td>
            </c:forEach>
          </tr>
          <tr>
            <td><strong>Agua total prevista vs. medida (g)</strong></td>
            <td><span class="badge badge-planned">Previsto</span> / <span class="badge badge-measured">Medido</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <td class="num">
                <span class="muted"><c:out value="${item.session.plannedWaterGDisplay}" /></span> →
                <strong><c:out value="${item.session.actualWaterGDisplay}" /></strong>
              </td>
            </c:forEach>
          </tr>
          <tr>
            <td><strong>Ratio previsto vs. real (1:x)</strong></td>
            <td><span class="badge badge-planned">Previsto</span> / <span class="badge badge-measured">Medido</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <td class="num">
                <span class="muted"><c:out value="${item.session.plannedRatioDisplay}" /></span> →
                <strong><c:out value="${item.session.actualRatioDisplay}" /></strong>
              </td>
            </c:forEach>
          </tr>
          <tr>
            <td><strong>Temperatura prevista vs. medida (°C)</strong></td>
            <td><span class="badge badge-planned">Previsto</span> / <span class="badge badge-measured">Medido</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <td class="num">
                <span class="muted"><c:out value="${item.session.plannedTemperatureCDisplay}" /></span> →
                <strong><c:out value="${item.session.actualTemperatureCDisplay}" /></strong>
              </td>
            </c:forEach>
          </tr>
          <tr>
            <td><strong>Molienda prevista vs. real</strong></td>
            <td><span class="badge badge-planned">Previsto</span> / <span class="badge badge-measured">Medido</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <td>
                <span class="muted"><c:out value="${item.session.plannedGrindDisplay}" /></span> →
                <strong><c:out value="${item.session.actualGrindDisplay}" /></strong>
              </td>
            </c:forEach>
          </tr>
          <tr>
            <td><strong>Tiempo objetivo vs. total medido (m:ss)</strong></td>
            <td><span class="badge badge-planned">Previsto</span> / <span class="badge badge-measured">Medido</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <td class="num">
                <span class="muted"><c:out value="${item.session.plannedTargetTimeDisplay}" /></span> →
                <strong><c:out value="${item.session.totalTimeDisplay}" /></strong>
              </td>
            </c:forEach>
          </tr>
          <tr>
            <td><strong>Peso final de bebida (g)</strong></td>
            <td><span class="badge badge-measured">Medido</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <td class="num"><c:out value="${item.session.finalBeverageWeightGDisplay}" /></td>
            </c:forEach>
          </tr>
          <tr>
            <td><strong>Atributos Descriptivos (Escala explícita)</strong></td>
            <td><span class="badge badge-measured">Descriptivo</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <td class="num">
                <c:choose>
                  <c:when test="${not empty item.assessment}">
                    <div style="font-size: 0.82rem;">
                      Escala <strong><c:out value="${item.assessment.attributeScale}" /></strong><br/>
                      Aroma: <c:out value="${item.assessment.fragranceAromaDisplay}" /> ·
                      Sabor: <c:out value="${item.assessment.flavorDisplay}" /> ·
                      Acidez: <c:out value="${item.assessment.acidityDisplay}" /><br/>
                      Dulzor: <c:out value="${item.assessment.sweetnessDisplay}" /> ·
                      Amargor: <c:out value="${item.assessment.bitternessDisplay}" /> ·
                      Cuerpo: <c:out value="${item.assessment.bodyTextureDisplay}" /><br/>
                      Posgusto: <c:out value="${item.assessment.aftertasteDisplay}" /> ·
                      Balance: <strong><c:out value="${item.assessment.balanceDisplay}" /></strong>
                    </div>
                  </c:when>
                  <c:otherwise><span class="mono muted">— (Sin evaluación)</span></c:otherwise>
                </c:choose>
              </td>
            </c:forEach>
          </tr>
          <tr>
            <td><strong>Preferencia / Agrado (Bloque Afectivo Separado)</strong></td>
            <td><span class="badge badge-preference badge-affective">Afectivo</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <td>
                <c:choose>
                  <c:when test="${not empty item.assessment and not empty item.assessment.preferenceRecord}">
                    Agrado: <strong class="tabular mono"><c:out value="${item.assessment.preferenceRecord.overallLikingDisplay}" /></strong>
                    · <c:out value="${item.assessment.preferenceRecord.wouldRepeatDisplay}" />
                  </c:when>
                  <c:otherwise><span class="mono muted">—</span></c:otherwise>
                </c:choose>
              </td>
            </c:forEach>
          </tr>
          <tr>
            <td><strong>Observaciones de preparación y cata</strong></td>
            <td><span class="badge badge-measured">Observación</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <td style="font-size: 0.84rem;">
                <c:out value="${item.session.notes != null ? item.session.notes : '—'}" />
                <c:if test="${not empty item.assessment and not empty item.assessment.descriptiveNotes}">
                  <div class="muted" style="margin-top: 0.25rem;">
                    Cata: <c:out value="${item.assessment.descriptiveNotes}" />
                  </div>
                </c:if>
              </td>
            </c:forEach>
          </tr>
          <tr>
            <td><strong>Hipótesis sugeridas por motor de reglas (No causalidad probada)</strong></td>
            <td><span class="badge badge-estimated">Sugerencia basada en reglas</span></td>
            <c:forEach var="item" items="${comparison.items}">
              <td style="font-size: 0.82rem;">
                <c:forEach var="sug" items="${suggestionsBySession[item.session.id]}">
                  <div style="margin-bottom: 0.35rem;">
                    <strong class="mono">[<c:out value="${sug.ruleCode}" />]</strong>
                    <c:out value="${sug.title}" />:
                    <span class="muted"><c:out value="${sug.suggestedNextAction}" /></span>
                  </div>
                </c:forEach>
              </td>
            </c:forEach>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</c:if>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
