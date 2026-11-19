# Interfaz Scientific Immersion — estado de entrega

## Alcance
Dashboard, biblioteca y ficha de café, equipos/agua, catálogo y editor de recetas,
asistente, laboratorio sensorial, comparador, experimentos, ajustes/respaldo y autenticación.

## Conservado
Modelos, repositorios, rutas, JWT, CSRF, formularios de negocio y scripts de
cronómetro, ratio y rueda sensorial. No se implementa API REST.

## Verificación
- Revisión estática de formularios, acciones, includes y scripts.
- Workflow Verify BrewLab: pruebas Java, WAR, arranque Tomcat y recorridos de navegador.
- El resultado real del workflow y sus capturas son la autoridad de validación.
- La revisión visual del dashboard no pudo ejecutarse en el entorno inicial por
  restricciones de Chromium y Maven; se traslada al servidor aislado de CI.

## Ejecución
Consulte LOCAL_SERVER.md para Docker y Tomcat manual.
La extensión a todos los módulos fue solicitada después de la primera entrega.

## Resultado comprobado
La ejecución de GitHub Actions 43564640176 pasó: pruebas Java, WAR, arranque
Docker/Tomcat y recorrido de navegador. Se generaron capturas de los nueve módulos
a 320, 390, 768 y 1440px, estados del dashboard y comparación de sesiones.
Se comprobó pausa/recarga/reanudación, rueda por teclado, búsqueda y cierre de sesión.
La revisión visual detectó un fondo claro residual en el contenedor del gráfico;
se sustituye por la superficie oscura del sistema en el ajuste final.
