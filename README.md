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

Los scripts usan el `java` de `JAVA_HOME` si está definido; si no, el del `PATH`.

Si aparece el error `PROLOG_NO_DISPONIBLE`, SWI-Prolog no está instalado o no se encontró: revise el paso 2 de la instalación o configure `swipl.ruta`. Si aparece `INTERNO` con «No se encontró …» o «La base de conocimiento está incompleta», falta o está dañado un archivo de `base_conocimiento/`: deben estar `puente.pl`, `bc_evaluacion.pl`, `bc_integrales.pl`, `bc_derivadas.pl`, `bc_simplificaciones.pl` y `bc_simplificaciones_algebraicas.pl`.

### Limitaciones del cálculo

- La integral definida usa la regla de Barrow, `P(b) − P(a)`. Si la función no es continua en `[a, b]`, la respuesta es «Fuera del dominio».
- El área geométrica (`∫|f(x)| dx`) es siempre ≥ 0, también si `a > b`. Si no se puede calcular de forma confiable (por ejemplo, en intervalos muy largos con más de 200000 cambios de signo), se muestra «No disponible» en lugar de un valor dudoso. Detalles en `docs/contrato.md`, secciones 9.1 y 9.2.

## Uso

### 1. Abrir la aplicación

Ejecute `ejecutar.bat` (Windows) o `./ejecutar.sh` (Linux / macOS) sin argumentos, como se indica en [Ejecución](#ejecución). Se abre la ventana **Sistema de Integrales**, con dos tarjetas: **Datos de la integral** y **Resultado**.

Si aparece una franja de aviso en la parte superior, lea su mensaje antes de continuar: indica que no se encontró SWI-Prolog, que la ruta configurada en `swipl.ruta` no existe, que falta un archivo de `base_conocimiento/` o que la aplicación está en modo de prueba (`--mock`). La ventana se abre igualmente.

### 2. Escribir la función f(x)

Escriba la función en el campo **f(x) =** usando la variable `x`:

- Operadores: `+`, `-`, `*`, `/` y `^` (potencia). Ejemplos: `3*x^2`, `x^-1`, `2^x`, `sin(x)+cos(x)`.
- Funciones: `sin` (o `sen`), `cos`, `tan`, `asin`, `acos`, `atan`, `sinh`, `cosh`, `tanh`, `asinh`, `acosh`, `atanh`. También se aceptan `arcsin`, `arccos`, `arctan`, `arcsinh`, `arccosh` y `arctanh`. Las funciones trigonométricas usan radianes.
- Se aceptan mayúsculas y la multiplicación implícita: `3x` se interpreta como `3*x`, y `Sen(X)` como `sin(x)`.
- Los decimales se escriben con punto: `0.5`, no `0,5`.

El menú **Ayuda > Sintaxis** (tecla **F1**) muestra este resumen dentro de la aplicación. Distingue cómo se escribe la función, qué se puede integrar, qué funciones se escriben pero no se pueden integrar y las limitaciones (ver [Limitaciones](#limitaciones)).

### 3. Ingresar los límites a y b

Escriba el límite inferior en **a =** y el superior en **b =**. Deben ser números entre −1000000 y 1000000, con punto decimal (por ejemplo `-2.5` o `3.14159`); no se aceptan expresiones como `pi`. Se permite `a = b` (la integral vale 0) y `a > b` (la integral definida cambia de signo).

### 4. Resolver

Pulse **Resolver**, la tecla **Enter** o **Alt+R**.

Primero se valida lo escrito. Si hay un error, el campo afectado se marca en rojo, debajo aparece un mensaje que lo explica y no se consulta a SWI-Prolog.

Si los datos son válidos, la tarjeta Resultado muestra «Calculando…» y en la barra de estado inferior aparece una barra de actividad. Mientras tanto, los campos y los botones quedan desactivados, para que no se inicien dos cálculos a la vez; la ventana sigue respondiendo. Si SWI-Prolog no responde dentro del tiempo configurado en `config.properties` (`timeout.ms`, 5 segundos por defecto), se muestra «Tiempo agotado»; como protección adicional, la interfaz cancela cualquier cálculo que supere los 15 segundos.

### 5. Interpretar el resultado

| Bloque | Qué muestra |
|---|---|
| **Integral indefinida** | La primitiva `F(x) + C` que devolvió la base de conocimiento, simplificada con reglas algebraicas básicas (por ejemplo `x^3 + C` para `3*x^2`, o `-1/x + C` para `x^-2`). `\|x\|` es el valor absoluto, `ln` el logaritmo natural y `sqrt` la raíz cuadrada. |
| **Integral definida** | `F(b) − F(a)`, con signo: es negativa cuando predomina la parte de la función que está bajo el eje x. |
| **Área geométrica** | `∫\|f(x)\| dx` en el intervalo: el área entre la curva y el eje x, siempre ≥ 0. Es un resultado numérico que puede ser aproximado. Cuando el sistema no puede calcularla con suficiente confiabilidad (por ejemplo, en intervalos muy largos), muestra **«No disponible»**. |

Los números se muestran con hasta 6 decimales, y en notación científica a partir de 10⁹ (por ejemplo `2.5e+23`). Debajo de los valores pueden aparecer notas: límites iguales, límites invertidos, integral negativa o área no disponible.

### 6. Limpiar

Pulse **Limpiar** (**Alt+L**) para borrar la función, los límites y el resultado, y empezar de nuevo. Mientras se calcula, Limpiar está desactivado.

### Ejemplo

| Campo | Valor |
|---|---|
| f(x) | `3*x^2` |
| a | `0` |
| b | `2` |

Al pulsar Resolver:

- **Integral indefinida:** `x^3 + C`. La forma exacta en que se muestra depende de la simplificación de la base de conocimiento.
- **Integral definida:** `8`, porque `2³ − 0³ = 8`.
- **Área geométrica:** `8`, igual a la integral, porque `3x²` no es negativa en `[0, 2]`.

Con `x^3-x` entre `-2` y `2`, en cambio, la integral definida es `0` y el área geométrica es `5`: las partes por encima y por debajo del eje x se compensan en la integral, pero se suman en el área.

### Mensajes de error

**Errores de entrada:** aparecen en la tarjeta *Datos de la integral*, con el campo afectado marcado en rojo. Por ejemplo, una expresión vacía o incompleta (`3*x^`), paréntesis sin cerrar, una función sin paréntesis (`sin x`), una variable distinta de `x` o un límite que no es un número. Corrija el dato y pulse Resolver otra vez.

**Errores de cálculo:** aparecen en la tarjeta *Resultado*, con un título, una explicación y, cuando está disponible, un «Detalle técnico» que ayuda a diagnosticar el problema.

| Título | Significado |
|---|---|
| Integral no soportada | La base de conocimiento no tiene una regla para esa expresión (ver [Limitaciones](#limitaciones)). |
| Fuera del dominio | La primitiva no se puede evaluar en todo `[a, b]`: la función no está definida en algún punto o tiene una singularidad (por ejemplo `x^-1` entre `-1` y `1`, o `tan(x)` entre `0` y `2`), o el valor es demasiado grande para calcularse (por ejemplo `2^x` entre `0` y `1000000`). El detalle técnico indica cuál de los dos casos es. Pruebe con otros límites. |
| Expresión no reconocida | SWI-Prolog no pudo leer la expresión. |
| SWI-Prolog no disponible | No se encontró `swipl`. Revise la [Instalación](#instalación) o `swipl.ruta` en `config.properties`. |
| Tiempo agotado | El cálculo tardó más de lo permitido y se canceló. |
| Error interno | Falló algo inesperado; por ejemplo, falta o está dañado un archivo de `base_conocimiento/`. El detalle técnico indica la causa. |

### Modo de prueba (`--mock`)

`ejecutar.bat --mock` (o `./ejecutar.sh --mock`) abre la interfaz **sin SWI-Prolog**. Sirve solo para probar la interfaz: **los resultados son simulados y no corresponden a la función escrita**. La ventana lo indica con una franja de aviso.

- `x*sin(x)` responde «Integral no soportada».
- `x^-1` responde «Fuera del dominio» si el 0 está en el intervalo.
- `x^5` tarda 3 segundos, para observar el estado «Calculando…».
- Cualquier otra expresión responde como si fuera `3*x^2`.

En el modo consola (`--mock "<f(x)>" a b`) el resultado se imprime sin indicar que es simulado. No use `--mock` para obtener resultados reales.

### Limitaciones

Además de las [limitaciones del cálculo](#limitaciones-del-cálculo):

- La base de conocimiento integra constantes numéricas, `x`, `x^n` (con `n` entero, incluido `x^-1`), `a^x` con `a` numérico (también `e^x` y `exp(x)`), y `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `sinh`, `cosh`, `tanh`, `asinh`, `acosh` y `atanh` con argumento exactamente `x`. También integra sumas y restas de estas funciones y su producto por una constante escrita a la izquierda, por ejemplo `3*x`, `3*sin(x)` o `2*x^2+3*cos(x)`.
- No integra, y responde «Integral no soportada»: la constante escrita a la derecha (`sin(x)*3`), productos de funciones (`x*sin(x)`), funciones de un argumento distinto de `x` (`sin(2*x)`, `e^(2*x)`), `ln(x)`, `x^0.5`, `1/x`, `x/2` ni `-x`. La validación acepta `ln`, `log` y la constante `e` sola en la expresión, pero la base de conocimiento no tiene reglas para integrarlas. La lista completa está en `docs/contrato.md`, sección 3.
- `sqrt` no se acepta en la expresión.
- La primitiva se simplifica con reglas algebraicas básicas: constantes que multiplican fracciones o sumas, signos y exponentes enteros negativos (`x^3` en lugar de `3*(x^3/3)`, `-1/x` en lugar de `x^ -1/ -1`). No se aplican identidades trigonométricas ni se factoriza, y las raíces se muestran como `sqrt(...)`.

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
