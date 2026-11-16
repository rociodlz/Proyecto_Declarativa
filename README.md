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

1. Instale el **JDK 17** o superior y compruebe con `java -version`.
2. Instale **SWI-Prolog** y compruebe con `swipl --version`:
   - **Windows:** instalador de <https://www.swi-prolog.org/download/stable>. Durante la instalación marque *Add swipl to the system PATH*. Si no lo hizo, el sistema también lo busca en `C:\Program Files\swipl\bin\swipl.exe`.
   - **Linux (Debian/Ubuntu):** `sudo apt install swi-prolog`.
   - **macOS:** `brew install swi-prolog`.
3. Compile el proyecto desde la carpeta raíz:
   - Windows: `mvnw.cmd clean package`
   - Linux / macOS: `./mvnw clean package`

   El ejecutable queda en `target/integrales-declarativa-1.0.0.jar`.

Si `swipl` está instalado en otra ruta, indíquela en `config.properties`:

```properties
# Vacía: se busca en el PATH y en las rutas típicas de instalación.
swipl.ruta=C:/Program Files/swipl/bin/swipl.exe
# Carpeta que contiene puente.pl y la base de conocimiento.
prolog.dir=base_conocimiento
# Tiempo máximo de una consulta, en milisegundos.
timeout.ms=5000
```

## Ejecución

Desde la carpeta raíz del proyecto:

- Windows: `ejecutar.bat`
- Linux / macOS: `./ejecutar.sh`

Los scripts buscan el `.jar` en su misma carpeta o en `target/`, y se ejecutan desde la carpeta del proyecto para encontrar `config.properties` y `base_conocimiento/`.

Sin argumentos se abre la interfaz gráfica. Si no se encuentra SWI-Prolog o falta algún archivo de `base_conocimiento/`, la ventana lo indica en una franja de aviso en la parte superior.

Opciones:

- `--mock`: usa respuestas simuladas, sin SWI-Prolog (útil para probar la interfaz). La ventana muestra un aviso de modo de prueba.
- `"<f(x)>" <a> <b>`: modo consola. Resuelve una integral sin abrir la interfaz y sirve para comprobar la instalación de Prolog. Ejemplo: `./ejecutar.sh "3*x^2" 0 2`.

Si aparece el error `PROLOG_NO_DISPONIBLE`, SWI-Prolog no está instalado o no se encontró: revise el paso 2 de la instalación o configure `swipl.ruta`.

## Uso

_Pendiente._

### Vista previa de la interfaz (desarrollo)

`VistaPreviaInterfaz` abre la ventana real con **datos simulados**, sin SWI-Prolog ni el servicio de integración. Es una herramienta de desarrollo: está en `src/test`, no se incluye en el JAR y no reemplaza a `integrales.Main`.

Desde la raíz del proyecto, en PowerShell (terminal integrada de VS Code):

```powershell
.\mvnw.cmd -q test-compile
& "$env:JAVA_HOME\bin\java.exe" -cp "target\classes;target\test-classes" integrales.ui.VistaPreviaInterfaz
```

Se usa `$env:JAVA_HOME\bin\java.exe` para garantizar Java 17 aunque el `java` del `PATH` sea otra versión.

- **Resolver** no calcula: simula una espera y muestra un caso de `docs/casos_prueba.md`, indicando que no corresponde a la expresión escrita.
- El menú **Vista previa** muestra cada estado de la interfaz: resultados, área no disponible, límites invertidos, error de entrada, errores de cálculo y estado de cálculo.
- Todos los valores se identifican en la ventana como simulados.
