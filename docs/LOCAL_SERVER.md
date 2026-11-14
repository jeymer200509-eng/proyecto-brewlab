# Ejecutar BrewLab localmente

## Inicio recomendado: Docker Desktop (Windows) / Docker Engine + Compose (Linux)

Desde la raíz del repositorio:

```sh
docker compose up --build -d
```

Abrir http://localhost:8080 y crear un perfil. El servicio se enlaza solo a
127.0.0.1; no se publica en la red local. La primera compilación requiere internet.
Se ejecutan las pruebas Java antes de generar la aplicación. El contenedor usa
Java 21 y Tomcat 9 (javax.servlet), manteniendo la compatibilidad del bytecode Java 8.
No usar Tomcat 10/11 con esta base: requieren Jakarta y una migración separada.

También puede ejecutar `scripts/start-local.ps1` en PowerShell o
`sh scripts/start-local.sh` en Linux/macOS.

```sh
docker compose logs -f brewlab   # Ver arranque/errores
docker compose stop             # Detener conservando datos
docker compose start            # Volver a iniciar
docker compose up --build -d    # Reconstruir después de actualizar código
```

Los volúmenes `brewlab-data` y `brewlab-config` conservan la base SQL local y el
secreto JWT. No ejecute `docker compose down -v` si quiere conservar sus datos.
No hay usuarios predeterminados ni carga automática de datos ficticios.
Los datos [DEMO] se cargan solo desde la acción explícita de Configuración.

El modo inicial usa el fallback H2 persistente existente. Si necesita MySQL externo,
configure `WEB-INF/config/brewlab.properties` con la URL y credenciales de su
servidor; dentro de Docker, localhost es el propio contenedor. Conserve una copia
JSON desde Configuración antes de cambiar de motor: no hay migración automática.

## Tomcat existente / XAMPP

1. Use Java 21 para compilar y Tomcat 9 con Java 21 para ejecutar (o conserve su
   Tomcat 8.5 existente compatible con el proyecto, aunque es una línea antigua).
2. Ejecute `scripts/setup-local.ps1` para su instalación manual, si aún no tiene
   configuración. No use `-Force` sobre secretos existentes sin necesidad.
3. Compile: `mvnw.cmd test package` (Windows) o `sh mvnw test package` (Unix).
4. Detenga Tomcat y respalde `WEB-INF/data` y `WEB-INF/config` de su instalación.
5. Despliegue `target/brewlab.war` en `webapps`. Restaure su configuración/datos en
   el directorio expandido si sustituye una aplicación anterior. No borre el
   despliegue antiguo sin copia de seguridad.
6. Inicie Tomcat y abra http://localhost:8080/brewlab/app/inicio.

JSTL está incluido ahora en el WAR; no depende de bibliotecas añadidas a mano en XAMPP.
Las rutas JSP utilizan contextPath y funcionan en `/` o en `/brewlab`.

## Verificación reproducible

El workflow `Verify BrewLab` compila, ejecuta los tests Java y levanta un servidor
Docker aislado. `e2e/smoke.cjs` crea una cuenta de revisión, carga [DEMO] de forma
explícita y verifica navegación, fichas, recetas, rueda sensorial, comparador,
cronómetro, cierre de sesión y anchos 320/390/768/1440. Sus capturas y el WAR se
adjuntan como artefacto `brewlab-verification`. Use únicamente un servidor de
pruebas para ejecutar este script: añade una cuenta y datos de revisión.

La aplicación conserva Servlets/JSP, JWT, JDBC y MySQL/H2. Spring Boot, Spring
Security, Thymeleaf y PostgreSQL no forman parte de esta versión.
