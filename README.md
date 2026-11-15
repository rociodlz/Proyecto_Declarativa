# Proyecto_Declarativa
Sistema para la resolución de integrales y el cálculo de áreas bajo la curva, desarrollado con Java y SWI-Prolog.

Proyecto Final de Programación Declarativa — Ciclo 02/2026.

El usuario escribe una función de `x` y los límites `a` y `b` en una interfaz Java Swing. La interfaz envía la expresión a una base de conocimiento en SWI-Prolog, que obtiene la primitiva y el valor de la integral definida. El resultado se muestra en la misma interfaz.

## Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 17 | `JAVA_HOME` debe apuntar al JDK 17, o `java` 17 debe estar en el `PATH`. |
| SWI-Prolog | 9.x o superior | Probado con 10.1.13. |
| Maven | No es necesario instalarlo | El proyecto incluye Maven Wrapper (`mvnw`, `mvnw.cmd`). |

La primera ejecución del wrapper descarga Maven 3.9.16 en `~/.m2/wrapper/`, por lo que requiere conexión a Internet.

## Compilar y probar

Windows:

```bat
mvnw.cmd clean verify
```

Linux / macOS:

```sh
./mvnw clean verify
```

## Estructura del proyecto

```
├── pom.xml                      Configuración Maven (Java 17, JUnit 5, UTF-8)
├── mvnw, mvnw.cmd, .mvn/        Maven Wrapper
├── base_conocimiento/           Base de conocimiento en SWI-Prolog
├── docs/
│   ├── contrato.md              Contrato de integración Java ↔ Prolog
│   └── casos_prueba.md          Casos de prueba y resultados
└── src/
    ├── main/java/integrales/
    │   ├── modelo/              Solicitud y resultado
    │   ├── servicio/            Comunicación con SWI-Prolog
    │   ├── validacion/          Validación de la entrada del usuario
    │   ├── controlador/         Coordinación entre la interfaz y el servicio
    │   └── ui/                  Interfaz gráfica Java Swing
    └── test/java/integrales/    Pruebas unitarias (JUnit 5)
```

## Instalación

_Pendiente._

## Ejecución

_Pendiente._

## Uso

_Pendiente._
