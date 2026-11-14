<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageHeading" value="Biblioteca de cafés"/>
<c:set var="moduleKicker" value="02 / ARCHIVO DE MUESTRAS"/>
<c:set var="moduleDescription" value="Cada origen tiene una historia. Registre sus lotes y siga la evolución de cada tostión."/>
<c:set var="moduleAnchor" value="form-cafe"/>
<c:set var="moduleAction" value="Registrar muestra"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="surface-card" aria-labelledby="busqueda-cafes-heading">
  <div class="card-header">
    <div>
      <h2 id="busqueda-cafes-heading">Lotes de café registrados</h2>
      <p class="field-hint" style="margin-bottom:0;">
        La identidad del café verde (<code>CoffeeLot</code>) está separada de cada evento de tostión (<code>RoastBatch</code>). Los días desde tostión se calculan respecto a hoy (<span class="tabular"><c:out value="${todayFormatted}"/></span>).
      </p>
    </div>
    <form method="get" action="${pageContext.request.contextPath}/app/cafes" class="cluster">
      <label class="sr-only" for="search-q">Buscar por código, nombre, origen, variedad o proceso</label>
      <input class="input" id="search-q" name="q" type="search" style="width:16rem;"
             value="${fn:escapeXml(query)}" placeholder="Buscar origen, variedad, código…">
      <label class="cluster" style="font-size:0.82rem; cursor:pointer;">
        <input type="checkbox" name="archived" value="true" ${showArchived ? 'checked' : ''} onchange="this.form.submit()">
        <span>Incluir archivados</span>
      </label>
      <button type="submit" class="btn btn-secondary btn-sm">Filtrar</button>
      <c:if test="${not empty query or showArchived}">
        <a class="btn btn-ghost btn-sm" href="${pageContext.request.contextPath}/app/cafes">Limpiar</a>
      </c:if>
    </form>
  </div>

  <c:choose>
    <c:when test="${empty lots}">
      <div class="empty-state">
        <h3>Aún no hay lotes de café que coincidan</h3>
        <p style="color: var(--text-secondary); font-size:0.88rem;">
          Registra un lote de café abajo. Los campos desconocidos pueden dejarse en blanco y se conservarán como <code>null</code> (nunca como cero).
        </p>
      </div>
    </c:when>
    <c:otherwise>
      <div class="sample-archive">


            <c:forEach var="lot" items="${lots}">
              <article class="sample-file">
                <div class="record-cell record-cell-1"><span class="record-label">Identidad de muestra</span>
                  <div>
                    <code class="num"><c:out value="${lot.code}"/></code>
                    <c:if test="${lot.demo}">
                      <span class="badge badge-demo">[DEMO]</span>
                    </c:if>
                    <c:if test="${lot.archived}">
                      <span class="badge badge-warn">Archivado</span>
                    </c:if>
                  </div>
                  <a href="${pageContext.request.contextPath}/app/cafes/lote?id=${fn:escapeXml(lot.id)}"
                     style="font-weight:650; color: var(--text-primary); text-decoration:underline;">
                    <c:out value="${lot.name}"/>
                  </a>
                </div>
                <div class="record-cell record-cell-2"><span class="record-label">Origen y finca</span>
                  <div><c:out value="${lot.originDisplay}"/></div>
                  <div class="field-hint">
                    Finca: <c:out value="${not empty lot.farm ? lot.farm : '—'}"/> ·
                    Prod.: <c:out value="${not empty lot.producer ? lot.producer : '—'}"/>
                  </div>
                </div>
                <div class="record-cell record-cell-3"><span class="record-label">Variedad y proceso</span>
                  <div><c:out value="${not empty lot.variety ? lot.variety : '—'}"/></div>
                  <div class="field-hint">Proceso: <c:out value="${not empty lot.process ? lot.process : '—'}"/></div>
                </div>
                <div class="record-cell record-cell-4"><span class="record-label">Altitud</span><c:out value="${lot.altitudeDisplay}"/></div>
                <div class="record-cell record-cell-5"><span class="record-label">Tostiones y edad</span>
                  <c:set var="bList" value="${batchesByLot[lot.id]}"/>
                  <c:choose>
                    <c:when test="${empty bList}">
                      <span class="field-hint">Sin lotes de tostión asociados</span>
                    </c:when>
                    <c:otherwise>
                      <div class="stack-sm">
                        <c:forEach var="bv" items="${bList}">
                          <div class="cluster" style="font-size:0.82rem;">
                            <code class="num"><c:out value="${bv.batch.batchCode}"/></code>
                            <span class="tabular">(<c:out value="${bv.batch.roastDate}"/>)</span>
                            <c:choose>
                              <c:when test="${bv.age.future}">
                                <span class="badge badge-warn">Fecha futura (<c:out value="${bv.age.days}"/> d)</span>
                              </c:when>
                              <c:otherwise>
                                <span class="badge badge-measured tabular"><c:out value="${bv.age.days}"/> días desde tostión</span>
                              </c:otherwise>
                            </c:choose>
                          </div>
                        </c:forEach>
                      </div>
                    </c:otherwise>
                  </c:choose>
                </div>
                <div class="record-cell record-cell-6"><span class="record-label">Acciones</span>
                  <div class="cluster">
                    <a class="btn btn-primary btn-sm"
                       href="${pageContext.request.contextPath}/app/cafes/lote?id=${fn:escapeXml(lot.id)}">
                      Abrir ficha / Tostiones
                    </a>
                    <a class="btn btn-secondary btn-sm"
                       href="${pageContext.request.contextPath}/app/cafes?editId=${fn:escapeXml(lot.id)}#form-cafe">
                      Editar
                    </a>
                  </div>
                </div>
              </article>
            </c:forEach>


      </div>
    </c:otherwise>
  </c:choose>
</section>

<section class="surface-card" id="form-cafe" aria-labelledby="nuevo-cafe-heading">
  <div class="card-header">
    <div>
      <h2 id="nuevo-cafe-heading">
        <c:choose>
          <c:when test="${not empty editLot and not empty editLot.id}">Editar lote de café: <c:out value="${editLot.name}"/></c:when>
          <c:otherwise>Registrar nuevo lote de café (CoffeeLot)</c:otherwise>
        </c:choose>
      </h2>
      <p class="field-hint" style="margin-bottom:0;">
        Solo el código y el nombre son obligatorios. Cualquier campo desconocido se conserva vacío (<code>null</code>).
      </p>
    </div>
    <c:if test="${not empty editLot}">
      <a class="btn btn-ghost btn-sm" href="${pageContext.request.contextPath}/app/cafes">Cancelar edición</a>
    </c:if>
  </div>

  <form method="post" action="${pageContext.request.contextPath}/app/cafes" class="stack-md" data-once>
    <input type="hidden" name="_csrf" value="${fn:escapeXml(csrfToken)}">
    <input type="hidden" name="action" value="saveLot">
    <c:if test="${not empty editLot}">
      <input type="hidden" name="id" value="${fn:escapeXml(editLot.id)}">
    </c:if>

    <div class="form-grid">
      <div class="field">
        <label class="field-label" for="lot-code">Código interno / muestra *</label>
        <input class="input" id="lot-code" name="code" type="text" required maxlength="64"
               value="${fn:escapeXml(editLot.code)}" placeholder="Ej. COL-HUI-01">
        <c:if test="${not empty lotErrors.code}">
          <span class="field-error"><c:out value="${lotErrors.code}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="lot-name">Nombre o denominación *</label>
        <input class="input" id="lot-name" name="name" type="text" required maxlength="180"
               value="${fn:escapeXml(editLot.name)}" placeholder="Ej. Finca El Mirador — Pink Bourbon">
        <c:if test="${not empty lotErrors.name}">
          <span class="field-error"><c:out value="${lotErrors.name}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="lot-country">País</label>
        <input class="input" id="lot-country" name="country" type="text" maxlength="100"
               value="${fn:escapeXml(editLot.country)}" placeholder="Ej. Colombia">
      </div>

      <div class="field">
        <label class="field-label" for="lot-region">Departamento / Región</label>
        <input class="input" id="lot-region" name="region" type="text" maxlength="100"
               value="${fn:escapeXml(editLot.region)}" placeholder="Ej. Huila">
      </div>

      <div class="field">
        <label class="field-label" for="lot-municipality">Municipio</label>
        <input class="input" id="lot-municipality" name="municipality" type="text" maxlength="100"
               value="${fn:escapeXml(editLot.municipality)}" placeholder="Ej. Palestina">
      </div>

      <div class="field">
        <label class="field-label" for="lot-farm">Finca</label>
        <input class="input" id="lot-farm" name="farm" type="text" maxlength="140"
               value="${fn:escapeXml(editLot.farm)}" placeholder="Ej. El Mirador">
      </div>

      <div class="field">
        <label class="field-label" for="lot-producer">Productor</label>
        <input class="input" id="lot-producer" name="producer" type="text" maxlength="140"
               value="${fn:escapeXml(editLot.producer)}">
      </div>

      <div class="field">
        <label class="field-label" for="lot-species">Especie</label>
        <input class="input" id="lot-species" name="species" type="text" maxlength="80"
               value="${fn:escapeXml(editLot.species)}" placeholder="Ej. Coffea arabica">
      </div>

      <div class="field">
        <label class="field-label" for="lot-variety">Variedad</label>
        <input class="input" id="lot-variety" name="variety" type="text" maxlength="140"
               value="${fn:escapeXml(editLot.variety)}" placeholder="Ej. Pink Bourbon, Caturra, Geisha">
      </div>

      <div class="field">
        <label class="field-label" for="lot-process">Proceso</label>
        <input class="input" id="lot-process" name="process" type="text" maxlength="120"
               value="${fn:escapeXml(editLot.process)}" placeholder="Ej. Lavado, Natural, Honey">
      </div>

      <div class="field">
        <label class="field-label" for="lot-alt-min">Altitud mínima (m s. n. m.)</label>
        <input class="input tabular" id="lot-alt-min" name="altitudeMin" type="number" min="0" max="3500"
               value="${editLot.altitudeMin}">
        <c:if test="${not empty lotErrors.altitudeMin}">
          <span class="field-error"><c:out value="${lotErrors.altitudeMin}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="lot-alt-max">Altitud máxima (m s. n. m.)</label>
        <input class="input tabular" id="lot-alt-max" name="altitudeMax" type="number" min="0" max="3500"
               value="${editLot.altitudeMax}">
        <c:if test="${not empty lotErrors.altitudeMax}">
          <span class="field-error"><c:out value="${lotErrors.altitudeMax}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="lot-harvest">Cosecha / Año</label>
        <input class="input" id="lot-harvest" name="harvest" type="text" maxlength="40"
               value="${fn:escapeXml(editLot.harvest)}" placeholder="Ej. 2026-A">
      </div>

      <div class="field">
        <label class="field-label" for="lot-received">Fecha de recepción</label>
        <input class="input tabular" id="lot-received" name="receivedOn" type="date"
               value="${editLot.receivedOn}">
        <c:if test="${not empty lotErrors.receivedOn}">
          <span class="field-error"><c:out value="${lotErrors.receivedOn}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="lot-moisture">Humedad café verde (%)</label>
        <input class="input tabular" id="lot-moisture" name="moisturePct" type="number" step="0.1" min="0" max="40"
               value="${editLot.moisturePct}" placeholder="Opcional, ej. 10.8">
        <c:if test="${not empty lotErrors.moisturePct}">
          <span class="field-error"><c:out value="${lotErrors.moisturePct}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="lot-density">Densidad café verde (g/L)</label>
        <input class="input tabular" id="lot-density" name="density" type="number" step="1" min="1"
               value="${editLot.density}" placeholder="Opcional, ej. 710">
        <c:if test="${not empty lotErrors.density}">
          <span class="field-error"><c:out value="${lotErrors.density}"/></span>
        </c:if>
      </div>

      <div class="field">
        <label class="field-label" for="lot-screen">Tamaño de criba / malla</label>
        <input class="input" id="lot-screen" name="screenSize" type="text" maxlength="40"
               value="${fn:escapeXml(editLot.screenSize)}" placeholder="Opcional, ej. Malla 16-18">
      </div>
    </div>

    <div class="field">
      <label class="field-label" for="lot-notes">Notas de origen y trazabilidad</label>
      <textarea class="input" id="lot-notes" name="notes" rows="2"><c:out value="${editLot.notes}"/></textarea>
    </div>

    <div class="cluster">
      <button type="submit" class="btn btn-primary">Guardar lote de café</button>
    </div>
  </form>
</section>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
