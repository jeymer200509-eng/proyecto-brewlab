# BrewLab — Scientific Immersion

## Alcance y arquitectura comprobada
Base: d8660ee (main). Este repositorio contiene Java con release 8, Servlets javax,
JSP/JSTL, JDBC, MySQL/H2 y JWT propio. No contiene Spring Boot, Spring MVC,
Spring Security, PostgreSQL ni Thymeleaf. La fase visual no migra esas capas.
La aplicación se mantiene MVC de servidor, sin API REST.

El dashboard establece el lenguaje visual. La segunda entrega extiende los módulos
por solicitud expresa del usuario; la revisión reproducible se ejecuta en CI.
CSS aislado en scientific-immersion.css, layout reutilizable en immersion-header.jspf
/ immersion-footer.jspf. modules.css integra componentes y composiciones específicas para todas las vistas.

## Dirección de arte
Precisión científica, profundidad cinematográfica y café de especialidad.
El espacio principal es una mesa óptica: esquema de extracción, sesión y registro.
No utilizar un sidebar o una cuadrícula de tarjetas como composición universal.

| Token | Valor | Función |
|---|---|---|
| Void | #090e12 | Fondo profundo |
| Titanium | #141d24 | Superficies estructurales |
| Ion | #85e4e8 | Navegación, foco y estados técnicos |
| Copper | #edb17c | Acción de preparación |
| Frost | #eaf1f3 | Texto principal |
| Muted | #a3b3bc | Texto secundario |
| Line | #34454f | Divisores funcionales |
| Error | #ffaaa5 | Errores con texto e icono |

Tipografía: sistema operativo, sin descargas. Display 2.5–4.5rem, tracking
-0.045em; títulos 1.25–1.75rem; cuerpo 1rem/1.6; etiquetas 0.75rem/1.5.
Datos: ui-monospace, numerales tabulares. No animar números medidos.
Espaciado: 4, 8, 12, 16, 24, 32, 48, 64px; usar rem y layouts fluidos.
Superficies: grafito casi opaco, cristal solo en navegación; bordes superiores
suaves y un halo cian localizado detrás del esquema. No desenfoques anidados.
Iconografía: SVG original, stroke 1.5–2, geometría consistente; texto para cada
acción. Flechas indican navegación; ningún pictograma aparenta ser un control.

## Componentes y estados
- Cabecera horizontal: marca, navegación por módulos, cuenta y cierre POST con CSRF.
- Acción primaria cobre, secundaria de cristal, enlaces de registro con flecha.
- Registro: dl con cantidades del servidor y enlaces a sus módulos.
- Estado de sesión: preparada / en curso / en pausa; protocolo y parámetros
  etiquetados como previstos. Sin sesión: estado vacío y acceso al asistente.
- Error/aviso/confirmación: flash existente, semántica y texto conservados.
- Foco: contorno cian de 2px con separación; áreas interactivas mínimas 44px.
- Deshabilitado: conservar semántica nativa, sin enlaces falsos ni href="#".
- Datos ausentes: —; cero únicamente cuando el conteo real es cero.
- No confundir conteos históricos con tasas, calidad o telemetría en vivo.

## Movimiento e interacción
Apple Design del repositorio: feedback inmediato al presionar, continuidad espacial,
interrupción libre y materiales subordinados a la lectura. En esta fase no hay
arrastre ni gestos que requieran un motor de resortes. Transiciones breves de
color/borde y presión de 1px; no temporizadores ni bucles de animación nuevos.
Mantener el JS compartido de pointerdown y prevención de doble envío.
Reduced motion elimina desplazamiento; reduced transparency elimina cristal;
contrast more refuerza fondos/bordes. Sin fondos móviles ni gráficas inventadas.
El cronómetro del asistente no se modifica ni se duplica en el dashboard.

## Composición implementada por módulo
| Módulo | Composición | Interacción principal |
|---|---|---|
| Cafés | Archivo con índice y ficha de muestra | Buscar, filtrar y abrir trazabilidad |
| Equipos y agua | Banco de calibración dividido | Inventario y parámetros con unidades |
| Recetas | Índice de protocolos + secuencia | Editar y ordenar pasos |
| Asistente | Tiempo central + etapa + instrumentos | Iniciar, pausar, reanudar, registrar |
| Sensorial | Rueda central + inspector | Selección accesible y escala explícita |
| Comparador | Área de gráficos + tabla fuente | Seleccionar sesiones y comparar |
| Experimentos | Hipótesis + ensayos cronológicos | Cambiar una variable y registrar |

## Validación
Revisar 1440px, 768px, 390px y 320px; teclado, zoom y preferencias de accesibilidad.
Verificar estado vacío, sesión preparada/en curso/en pausa, nombres largos y
valores ausentes. Cada enlace conserva la ruta actual y las banderas de módulo.
No afirmar validación del backend si Maven o la aplicación no pueden ejecutarse.
