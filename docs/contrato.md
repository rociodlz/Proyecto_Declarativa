# Contrato de integración Java ↔ SWI-Prolog

> **Estado: v1 APROBADA** el 15/11/2026 por Henry, Erick y Rocío.
> **v1.1 PROPUESTA — pendiente de aprobación:** secciones 4.1, 12, 13, 14 y 15, y los cambios de las secciones 2, 3, 6, 7 y 9, marcados con 🟡.
> Los cambios posteriores se acuerdan entre los tres y se registran en la sección 11.

Convención de estados usada en todo el documento:

| Estado | Significado |
|---|---|
| **Existe** | Ya está implementado en el repositorio. |
| **Pendiente** | Necesario para cumplir el PDF oficial; aún no está implementado. |
| **Opcional** | Mejora deseable; el sistema funciona sin ella. |

---

## 1. Flujo general

```
PanelEntrada → ValidadorEntrada → NormalizadorExpresion → ServicioIntegrales
   → swipl base_conocimiento/puente.pl → ParserRespuesta
   → FormateadorResultado → PanelResultado
```

🟡 Propuesta v1.1 (sección 13): `NormalizadorExpresion` se invoca dentro de `ServicioProlog`, no desde el controlador.

## 2. Ubicación de los archivos Prolog

Todos los archivos Prolog están en `base_conocimiento/`, en la raíz del repositorio.

| Archivo | Responsable | Estado |
|---|---|---|
| `bc_integrales.pl` | Henry | Existe (rama `base_conocimiento`) |
| `bc_derivadas.pl` | Henry | Existe (rama `base_conocimiento`) |
| `bc_simplificaciones.pl` | Henry | Existe (rama `base_conocimiento`) |
| `bc_simplificaciones_algebraicas.pl` | Henry | Existe (rama `base_conocimiento`) |
| `bc_evaluacion.pl` | Por acordar (🟡 propuesta de Erick en el PR #7, pendiente de que Henry la acepte o la reemplace) | Pendiente |
| `puente.pl` | Erick | Existe |

Los predicados originales de Henry **no se renombran ni se modifican** como parte de este contrato.

🟡 Los seis archivos son obligatorios. `puente.pl` comprueba que existan antes de cargar, que la carga no tenga errores y que existan `integral/3`, `integrar/2` y `evaluar_area/4`; si no, responde `INTERNO` (nunca `NO_SOPORTADA`). `Main` comprueba la misma lista (`ConfiguracionProlog.ARCHIVOS_PROLOG`) al abrir la ventana y muestra un aviso si falta alguno.

## 3. Predicados Prolog

| Predicado | Archivo | Estado | Descripción |
|---|---|---|---|
| `integral(+F, +X, -R)` | `bc_integrales.pl` | **Existe** | Integral de `F` respecto a `X`, simplificada. Falla si no hay regla aplicable. Para funciones simbólicas `u(x)`, `v(x)` devuelve un término que contiene `pendiente(_, _)`. |
| `integrar(+F, -P)` | `bc_evaluacion.pl` | **Pendiente** (🟡 propuesta en el PR #7) | Adaptador: llama a `integral(F, x, P)` y falla si `P` contiene `pendiente(_, _)`. 🟡 Si falta un predicado de la base (`existence_error`), lanza el error en vez de fallar. |
| `evaluar_area(+P, +A, +B, -V)` | `bc_evaluacion.pl` | **Pendiente** (🟡 propuesta en el PR #7) | Integral definida `V = P(B) − P(A)`. Lanza un error de dominio cuando la evaluación numérica falla o cuando detecta una singularidad en `[A, B]` (ver sección 9). 🟡 El error de dominio propio es `error(dominio(Motivo), _)`; `puente.pl` lo convierte en `DOMINIO`. |
| `area_geometrica(+F, +P, +A, +B, -Ag)` | `bc_evaluacion.pl` | **Opcional** (🟡 propuesta en el PR #7) | Área geométrica `∫|F| dx` en `[min(A,B), max(A,B)]`, siempre ≥ 0, dividiendo el intervalo en los cambios de signo de `F`. 🟡 Falla si no puede obtenerlos de forma confiable (sección 9.2). |

Los estados de estas tres filas pasarán a **Existe** cuando Henry confirme la implementación de `bc_evaluacion.pl`.

### Notación de la base de conocimiento (según `base_conocimiento/README.txt`)

| Notación | Significado |
|---|---|
| `ln(F)` | Logaritmo natural |
| `log(C, F)` | Logaritmo de `F` en base `C` |
| `raiz(F, N)` | Raíz `N`-ésima de `F` |
| `abs(F)` | Valor absoluto |
| `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `sinh`, `cosh`, `tanh`, `asinh`, `acosh`, `atanh` | Funciones trigonométricas e hiperbólicas, en radianes |
| `C^F` | Exponencial de base constante `C` |

### Cobertura actual de `integral/3` (verificada el 15/11/2026 sobre el commit `1de3224`)

| Tipo del PDF | Estado |
|---|---|
| a) `k`, b) `x`, c) `k*x`, d) `x^n` (n entero, incluido `x^(-1)`) | Existe |
| g) `u + v`, h) `u − v` | Existe |
| i) `a^x` con `a` numérico | Existe |
| k)–v) sin, cos, tan, asin, acos, atan, sinh, cosh, tanh, asinh, acosh, atanh **con argumento exactamente `x`** | Existe |
| e) `u`, f) `k*u`, j) `a^u` con `u` simbólica | Devuelve `pendiente(...)`: se trata como no soportada |

Limitaciones conocidas de `integral/3` (devuelve `false`): `x*k` (constante a la derecha), `-f` (negación), `f/k`, `1/x`, `e^x`, `exp(x)`, `ln(x)`, `sqrt(x)`, `x^0.5`, `sin(2*x)` (no hay regla de la cadena). Si se agregan reglas, se coordinará con Henry y se actualizará esta tabla.

## 4. Expresión normalizada (la produce Java, la consume Prolog)

| Regla | Ejemplo |
|---|---|
| Variable única `x` | — |
| Minúsculas, sin espacios | `Sin( X )` → `sin(x)` |
| Multiplicación explícita | `3x` → `3*x`, `2(x+1)` → `2*(x+1)` |
| Nombres en español | `sen` → `sin` |
| Logaritmo natural como `ln` | `log(x)` → `ln(x)` |
| Exponente negativo entre paréntesis | `x^-1` → `x^(-1)` (sin paréntesis es error de sintaxis en Prolog) |
| Punto decimal | `0.5` |

### 4.1 🟡 Propuesta v1.1: reglas adicionales de normalización (pendiente de confirmación de Erick)

La validación de la interfaz (sección 12) acepta las formas siguientes. Se comprobó con SWI-Prolog 10.1.13 que **Prolog no puede leerlas tal como se escriben** (`syntax_error(operator_expected)`), por lo que el normalizador debe transformarlas:

| Forma aceptada por la validación | Problema en Prolog | Normalización propuesta |
|---|---|---|
| `2*-x`, `x/-2`, `x+-1`, `x--1`, `2^-x`, `x^-1` | Prolog lee `*-`, `/-`, `+-`, `--`, `^-` como un solo operador desconocido | Encerrar el negativo entre paréntesis: `2*(-x)`, `x/(-2)`, `x+(-1)`, `x-(-1)`, `2^(-x)`, `x^(-1)` |
| `.5` | Un número no puede empezar con punto | `0.5` |
| `2.` | Un número no puede terminar con punto | `2` o `2.0` |
| `2sin(x)`, `3e^x` | Multiplicación implícita con función o constante | `2*sin(x)`, `3*e^x` |
| `x(x+1)`, `(x+1)(x-1)`, `(x+1)x`, `x sin(x)` | Multiplicación implícita sin número | `x*(x+1)`, `(x+1)*(x-1)`, `(x+1)*x`, `x*sin(x)` |
| `arcsin`, `arccos`, `arctan` | Nombres del PDF que Prolog no conoce | `asin`, `acos`, `atan` |
| `arcsinh`, `arccosh`, `arctanh`, `sh` | Nombres del PDF que Prolog no conoce | `asinh`, `acosh`, `atanh`, `sinh` |

Formas que Prolog sí lee sin cambios: `-x`, `(-x+1)`, `-sin(x)`, `x^2.5`, `0.5*x`.

## 5. Invocación

```
swipl base_conocimiento/puente.pl "<expresion_normalizada>" <a> <b>
```

- `a` y `b` se escriben con punto decimal (`Locale.ROOT`).
- Java pasa los argumentos como lista (`ProcessBuilder`), sin usar un shell.
- `puente.pl` carga los archivos de `base_conocimiento/` con rutas relativas a su propia ubicación.

## 6. Respuesta

`puente.pl` escribe en la salida estándar una línea con alguno de estos formatos:

```
OK|<primitiva>|<integral_definida>|<area_geometrica>
ERROR|<CODIGO>|<detalle>
```

- `<area_geometrica>` es `-` cuando no se calculó (por ejemplo, mientras `area_geometrica/5` no exista). **Nunca se reemplaza por un valor aproximado o inventado.**
- 🟡 `<area_geometrica>` es siempre ≥ 0, también cuando `a > b`; también es `-` cuando no se puede calcular de forma confiable (sección 9.2).
- Java toma la primera línea que empieza con `OK|` o `ERROR|`.
- Las advertencias de SWI-Prolog van a la salida de error y se ignoran.

## 7. Códigos de error

| Código | Origen | Cuándo |
|---|---|---|
| `SINTAXIS` | Prolog | `term_string/2` no puede leer la expresión. |
| `NO_SOPORTADA` | Prolog | `integrar/2` falla. |
| `DOMINIO` | Prolog | La evaluación numérica falla o se detecta una singularidad conocida en `[a, b]`. |
| `PROLOG_NO_DISPONIBLE` | Java | No se encuentra el ejecutable `swipl`. |
| `TIEMPO_AGOTADO` | Java | La consulta tarda más de 5 segundos. |
| `INTERNO` | Java o Prolog | Respuesta ausente o mal formada, o excepción inesperada. 🟡 También cuando falta un archivo o un predicado de la base de conocimiento, o hay un error al cargarla. |

Los errores de validación de la interfaz no son códigos de este contrato: la interfaz los muestra antes de llamar al servicio.

## 8. Modelo y servicio Java (propuesta, implementación de Erick)

```java
// paquete integrales.modelo
public record SolicitudIntegral(String expresion, double a, double b) {}

public record ResultadoIntegral(boolean exito,
                                String primitiva,
                                Double integralDefinida,
                                Double areaGeometrica,   // null si no se calculó
                                String codigoError,
                                String detalle) {}

// paquete integrales.servicio
public interface ServicioIntegrales {
    /** Nunca lanza excepciones: los errores se devuelven dentro del resultado. */
    ResultadoIntegral resolver(SolicitudIntegral solicitud);
}
```

- Si `exito` es `true`: `primitiva` e `integralDefinida` tienen valor, `codigoError` y `detalle` son `null`, y `areaGeometrica` puede ser `null`.
- Si `exito` es `false`: `codigoError` tiene valor y los campos numéricos son `null`.
- El controlador llama a `resolver` dentro de un `SwingWorker`.

## 9. Dominio y singularidades: alcance y limitaciones

La integral definida se calcula con la regla de Barrow, `P(b) − P(a)`. Esta regla **solo es válida si la función es continua en `[a, b]`**.

Alcance propuesto para `evaluar_area/4`:

1. Si la evaluación numérica de `P(a)` o `P(b)` falla (por ejemplo `ln(0)`, `acosh(0.5)`, `atanh(1)`), devuelve `DOMINIO`.
2. Detecta las singularidades **de los casos conocidos** de la base de conocimiento:
   - `x^n` con `n` negativo cuando `0 ∈ [a, b]`.
   - `tan(x)` cuando `[a, b]` contiene algún `π/2 + kπ`.
   - `asin`, `acos` fuera de `[−1, 1]`; `acosh` con `x < 1`; `atanh` con `|x| ≥ 1`.

**Limitación:** esta detección **no garantiza encontrar todas las singularidades** de cualquier expresión. Se limita a los tipos anteriores. Una expresión que combine funciones de forma no prevista podría producir un resultado numérico incorrecto sin error. Los casos que se prueben se documentarán en `docs/casos_prueba.md`.

### 9.1 🟡 Propuesta v1.1: cómo se detectan las singularidades (`bc_evaluacion.pl`, PR #7)

En la primitiva `P` se buscan las subexpresiones que dejan de estar definidas donde otra expresión `G` se anula: `B^n` con `n < 0` (`G = B`), `F/D` con `D` no numérico (`G = D`), `ln(H)` y `log(C, H)` (`G = H`) y `tan(H)` (`G = cos(H)`). Si `G` tiene un cero o un polo en `[a, b]`, incluidos los extremos, la respuesta es `DOMINIO`.

Los ceros de `G` se calculan **de forma analítica**, sin muestreo, cuando `G` es:

- un polinomio en `x` (raíces reales exactas): cubre `x^(-1)`, `x^(-2)` y `ln(1-x^2)` de `atanh`, con los puntos ±1;
- `sin`, `cos` o `tan` de un argumento lineal: todos los `kπ` y `π/2 + kπ` del intervalo, sin importar su longitud (`tan(x)` de 0 a 4000π da `DOMINIO`);
- productos, cocientes, potencias y funciones con un único cero de las anteriores.

Las restricciones de dominio de `asin`, `acos`, `acosh`, `atanh`, `ln` y las raíces pares se detectan al evaluar `P` en los extremos, porque sus argumentos en la base de conocimiento son monótonos (`x`, `1-x^2` en `[0, 1]`, …).

Si `G` no es reconocible, se muestrea con una separación de 0.05 como máximo (hasta 40000 puntos). Si el intervalo es demasiado largo para eso, se responde `DOMINIO` con el motivo «no se puede verificar…»: no se adivina. Con las reglas actuales de `integral/3` este caso no ocurre.

### 9.2 🟡 Propuesta v1.1: área geométrica y sus limitaciones

`area_geometrica/5` divide `[min(a,b), max(a,b)]` en los puntos donde `f` puede cambiar de signo y suma `|P(xᵢ₊₁) − P(xᵢ)|`. El resultado es siempre ≥ 0.

- Los puntos se calculan de forma analítica con las mismas reglas de 9.1, aplicadas a `f`. Son exactos salvo por el redondeo de punto flotante (por ejemplo, `sin(x)` de 0 a 7000 da 4456.137987).
- Si `f` no es reconocible (por ejemplo `sin(x) + 0.5`), se muestrea con una separación de 0.05 como máximo, dos veces y con cantidades de puntos no proporcionales. El resultado solo se acepta si las dos cuentas de cambios de signo coinciden y no son demasiadas.
- **No se informa el área (`-`)** cuando hay más de 200000 cambios de signo, cuando el intervalo es demasiado largo para muestrearlo (más de 2000 unidades) o cuando las dos cuentas no coinciden.
- **Limitación:** el muestreo puede no ver dos cambios de signo muy próximos entre sí (a menos de 0.05). Con las funciones que admite hoy `integral/3` (argumento exactamente `x`), esto solo afecta a sumas de funciones con raíces dobles o casi dobles.

## 10. Responsabilidades

| Integrante | Responsabilidad |
|---|---|
| Henry | Reglas de integración, simplificación y evaluación (`base_conocimiento/`). |
| Erick | Modelo, servicio, normalización, `puente.pl`, parser de respuesta, `Main`, ejecución de `swipl`. |
| Rocío | Validación del texto ingresado, interfaz Swing, controlador, formateo de resultados, documentación. |

Formateo en la interfaz (Rocío): agregar `+ C`, mostrar `raiz(F, 2)` como `sqrt(F)`, redondear a 6 decimales y traducir los códigos de error a mensajes en español.

Las pruebas que necesitan `swipl` deben **omitirse** (no fallar) cuando `swipl` no está disponible, para que `mvnw clean verify` funcione en cualquier máquina.

## 11. Historial de cambios

| Versión | Fecha | Cambio | Aprobado por |
|---|---|---|---|
| v1 | 15/11/2026 | Primera versión basada en el código real de `base_conocimiento` (`1de3224`). | Henry, Erick y Rocío |
| v1.1 🟡 | 15/11/2026 | Normalización adicional (4.1), validación de la entrada (12), responsable de normalizar (13), integración de la interfaz con `Main` (14) y controlador (15). | Pendiente |
| v1.1 🟡 | 16/11/2026 | Archivos obligatorios y error `INTERNO` por base incompleta (2, 7); estados de `bc_evaluacion.pl` y `error(dominio(Motivo), _)` (3); área siempre ≥ 0 o `-` si no es confiable (6, 9.2); detección analítica de singularidades (9.1). | Pendiente |

## 12. 🟡 Propuesta v1.1: validación de la entrada (implementada por Rocío)

`integrales.validacion.ValidadorEntrada` revisa el **texto crudo** antes de enviarlo. **No normaliza, no construye un árbol de la expresión y no comprueba el dominio matemático.** Recorre el texto una vez, dividiéndolo en piezas (números, palabras, operadores y paréntesis), y revisa cada pieza respecto de la anterior.

### 12.1 Expresión

| Regla | Se rechaza, por ejemplo |
|---|---|
| E1. No vacía | `""`, `"   "` |
| E2. Máximo 200 caracteres (sin contar espacios exteriores) | 201 caracteres |
| E3. Solo dígitos, letras, espacios y `+ - * / ^ ( ) .` | `x=2`, `√x`, `x²` |
| E4. La coma tiene un mensaje propio (use punto decimal) | `1,5*x` |
| E5. Números con a lo sumo un punto | `1..2`, `1.2.3`, `.` |
| E6. Palabras permitidas: `x`, `e` y las funciones de la lista blanca (sin distinguir mayúsculas). `u` y `v` se rechazan | `y+1`, `foo(x)`, `sqrt(x)`, `u(x)` |
| E7. Palabras pegadas: mensaje con sugerencia | `sinx` → "¿Quiso escribir sin(x)?" |
| E8. Toda función va seguida de `(` | `sin x`, `ln^2` |
| E9. Paréntesis balanceados | `(x+1`, `x+1)` |
| E10. Sin paréntesis vacíos | `sin()`, `()` |
| E11. Sin operadores consecutivos, salvo un `-` unario después de un operador | `x**2`, `x+*2`, `--x` |
| E12. No empieza (ni sigue a `(`) con `+ * / ^`; el `-` inicial sí se permite | `*x`, `(+x)` |
| E13. No termina con operador ni hay operador antes de `)` | `3*x^`, `(x+)` |
| E14. Un número no puede seguir sin operador a otro valor (sería ambiguo) | `2 3`, `x2`, `(x+1)2` |

**Lista blanca de funciones:** `sin, sen, cos, tan, asin, acos, atan, sinh, cosh, tanh, asinh, acosh, atanh, exp, log, ln` y los alias del PDF `arcsin, arccos, arctan, arcsinh, arccosh, arctanh, sh` (su normalización está pendiente de confirmar por Erick, sección 4.1).

**Se aceptan** (Erick las normaliza, sección 4.1): signo negativo unario (`-x`, `2*-x`, `x/-2`, `2^-x`), multiplicación implícita (`3x`, `2(x+1)`, `2sin(x)`, `x(x+1)`, `(x+1)(x-1)`) y decimales `.5` y `2.`.

### 12.2 Límites a y b

| Regla | Se rechaza, por ejemplo |
|---|---|
| L1. Obligatorio | `""` |
| L2. La coma tiene un mensaje propio | `1,5` |
| L3. Formato `[+-]dígitos[.dígitos][e±dígitos]` (también `.5` y `2.`) | `abc`, `NaN`, `Infinity`, `0x10`, `2d`, `pi` |
| L4. Finito | `1e400` |
| L5. \|valor\| ≤ 1 000 000 | `1e7` |

- **L5 es una decisión provisional del proyecto, no un requisito del PDF.** Evita desbordamientos al evaluar potencias y exponenciales, y puede ajustarse.
- **a = b y a > b son válidos.** El PDF no exige a < b, y ∫ₐᵇ f = −∫ᵦᵃ f está bien definida. La interfaz muestra una nota cuando el resultado es negativo.

### 12.3 Lo que la validación no hace

| Responsable | Tarea |
|---|---|
| Erick (`NormalizadorExpresion`) | Transformar el texto validado a la forma que lee Prolog (secciones 4 y 4.1). |
| Henry (base de conocimiento) | Decidir si la integral está soportada (`NO_SOPORTADA`), el dominio y las singularidades (`DOMINIO`), y los errores de sintaxis que se escapen (`SINTAXIS`). |

Por ejemplo, `x^(-1)` con a = −1 y b = 1 pasa la validación porque está bien escrita; la base de conocimiento debe responder `DOMINIO`.

## 13. 🟡 Propuesta v1.1: responsable de normalizar

`ServicioProlog` llama internamente a `NormalizadorExpresion` antes de invocar `swipl`. Así:

- El controlador construye `SolicitudIntegral` con el texto validado **sin normalizar**.
- `ServicioMock` no necesita normalizar.
- El controlador solo depende de la interfaz `ServicioIntegrales`.

## 14. 🟡 Propuesta v1.1: integración de la interfaz con `Main`

### 14.1 Implementado (Rocío, paquete `integrales.ui`)

| Clase | API pública |
|---|---|
| `VentanaPrincipal` | `static instalarAspecto()`, `VentanaPrincipal()`, `getPanelEntrada()`, `getPanelResultado()`, `setCalculando(boolean)`, `setEstado(String)`, `mostrarAviso(String)` |
| `PanelEntrada` | `getExpresion()`, `getLimiteA()`, `getLimiteB()`, `setDatos(...)`, `alResolver(Runnable)`, `alLimpiar(Runnable)`, `mostrarErrorEntrada(Campo, String)`, `quitarErrores()`, `setHabilitado(boolean)`, `limpiar()` |
| `PanelResultado` | `mostrarResultado(ResultadoFormateado)`, `mostrarError(MensajeError)`, `mostrarCalculando()`, `limpiar()` |
| `FormateadorResultado` | `formatearExito(primitiva, integralDefinida, areaGeometrica, a, b)`, `formatearError(codigo, detalle)`, `formatearPrimitiva(String)`, `formatearNumero(double)` |

- El paquete `ui` **no depende** de `modelo` ni de `servicio`: recibe valores simples. El controlador hace la conversión desde `ResultadoIntegral`.
- Todos los métodos públicos de la ventana y los paneles deben llamarse desde el EDT; fuera de él lanzan `IllegalStateException`.
- El formateador muestra `raiz(F,2)` como `sqrt(F)`, `raiz(F,N)` como `(F^(1/N))` y `abs(F)` como `|F|`, solo cuando la transformación conserva el significado; si no, mantiene la forma original. Conserva los espacios que escribe Prolog (`2* -cos(x)`). El área `null` se muestra como "No disponible".

### 14.2 Propuesta para el `Main` de Erick (pendiente de confirmar)

```java
SwingUtilities.invokeLater(() -> {
    VentanaPrincipal.instalarAspecto();
    VentanaPrincipal ventana = new VentanaPrincipal();
    new ControladorIntegral(ventana, servicio);   // integrales.controlador
    ventana.setVisible(true);
});
```

`ControladorIntegral` ya está implementado (sección 15): crea internamente `ValidadorEntrada` y `FormateadorResultado`, y llama al servicio dentro de un `SwingWorker`, nunca en el EDT. Con `--mock` se recomienda además `ventana.mostrarAviso(...)` para indicar que las respuestas son simuladas.

### 14.3 Herramienta de desarrollo

`src/test/java/integrales/ui/VistaPreviaInterfaz.java` abre la ventana con datos simulados. No forma parte de la aplicación, no entra en el JAR y no reemplaza a `Main` (ver README).

## 15. 🟡 Propuesta v1.1: controlador (implementado por Rocío)

`integrales.controlador.ControladorIntegral` conecta la interfaz con `ValidadorEntrada` y con `ServicioIntegrales`. Usa directamente las clases de Erick; no hay interfaces ni adaptadores intermedios.

### 15.1 API pública

| Método | Descripción |
|---|---|
| `ControladorIntegral(VentanaPrincipal ventana, ServicioIntegrales servicio)` | Registra Resolver y Limpiar en `PanelEntrada`. Argumentos obligatorios. Debe llamarse en el EDT. |
| `resolver()` | Equivale a pulsar Resolver (EDT). |
| `limpiar()` | Equivale a pulsar Limpiar (EDT). |

Fuera del EDT, los tres lanzan `IllegalStateException`.

### 15.2 Flujo de Resolver

1. Si ya hay un cálculo en curso, no hace nada.
2. Valida f(x), a y b, en ese orden. Ante el primer error: lo muestra en su campo, borra el resultado anterior y **no llama al servicio**.
3. Crea `SolicitudIntegral` con la expresión validada **sin normalizar** (solo sin espacios exteriores; normaliza `ServicioProlog`, sección 13) y los límites como `double`. a = b y a > b se envían sin cambios.
4. Activa el estado de carga (`setCalculando(true)`, "Calculando…") y llama a `servicio.resolver(...)` en un `SwingWorker`.
5. Al terminar, en el EDT: desactiva el estado de carga y muestra el resultado con `FormateadorResultado`.

### 15.3 Respuestas y errores

| Respuesta del servicio | Se muestra |
|---|---|
| `exito = true` con integral definida finita | Primitiva, integral definida y área (o "No disponible") |
| `exito = false` | El mensaje del código de error y su detalle |
| `null` | `INTERNO`: "El servicio no devolvió ningún resultado." |
| `exito = true` sin integral definida, con valor no finito o primitiva vacía | `INTERNO`: respuesta no válida (no se inventan valores) |
| El servicio lanza una excepción (no debería, según la sección 8) | `INTERNO`, con la excepción en el detalle técnico |
| El hilo se interrumpe | `INTERNO`: "La consulta fue interrumpida." |
| El servicio no responde en 15 s | `TIEMPO_AGOTADO` (sección 15.5) |

En todos los casos la interfaz se rehabilita.

### 15.4 Limpiar, cálculos simultáneos y respuestas obsoletas

- Mientras se calcula, la ventana desactiva Resolver, **Limpiar** y los campos; no puede haber dos cálculos a la vez. El tiempo máximo de seguridad (15.5) garantiza que la interfaz siempre se recupera.
- Cada cálculo recibe un **número de operación**. `limpiar()`, el tiempo agotado y el cierre de la ventana lo invalidan, detienen el temporizador y piden la cancelación del `SwingWorker`. Una respuesta, o el aviso de un temporizador, solo tiene efecto si su número sigue siendo el actual: **un resultado anterior nunca reaparece después de limpiar ni sobrescribe el mensaje de tiempo agotado**.

### 15.5 Tiempo máximo de seguridad

- Cada cálculo arranca un `javax.swing.Timer` de un solo disparo (se ejecuta en el EDT) de **15 s** (`ControladorIntegral.TIEMPO_MAXIMO`). Es mayor que el límite del servicio (`timeout.ms`, 5 s) más el arranque de SWI-Prolog, así que normalmente responde antes el servicio.
- Si se cumple el tiempo y la operación sigue activa: se invalida, se pide `cancel(true)`, se rehabilita la interfaz y se muestra `TIEMPO_AGOTADO` ("El servicio no respondió en 15 s; el cálculo se canceló.").
- El controlador **no depende** de que el servicio respete su propio límite ni de que `cancel(true)` lo detenga: si el servicio sigue bloqueado, su hilo continúa, pero su respuesta se descarta.
- El temporizador se detiene cuando el cálculo termina, al limpiar y al cerrar la ventana. Cada temporizador conoce su operación, así que uno antiguo no puede afectar a un cálculo nuevo.
- Para las pruebas existe un constructor de paquete `ControladorIntegral(ventana, servicio, Duration)`.

### 15.6 Pendiente

- `Main.iniciarInterfaz` (Erick) debe crear el controlador como en la sección 14.2.
