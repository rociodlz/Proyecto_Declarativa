# Contrato de integración Java ↔ SWI-Prolog

> **Estado: BORRADOR v1 — pendiente de aprobación por Henry, Erick y Rocío.**
> Ningún punto de este documento es definitivo hasta que los tres lo aprueben.
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

## 2. Ubicación de los archivos Prolog

Todos los archivos Prolog están en `base_conocimiento/`, en la raíz del repositorio.

| Archivo | Responsable | Estado |
|---|---|---|
| `bc_integrales.pl` | Henry | Existe (rama `base_conocimiento`) |
| `bc_derivadas.pl` | Henry | Existe (rama `base_conocimiento`) |
| `bc_simplificaciones.pl` | Henry | Existe (rama `base_conocimiento`) |
| `bc_simplificaciones_algebraicas.pl` | Henry | Existe (rama `base_conocimiento`) |
| `bc_evaluacion.pl` | Por acordar | Pendiente |
| `puente.pl` | Erick | Pendiente |

Los predicados originales de Henry **no se renombran ni se modifican** como parte de este contrato.

## 3. Predicados Prolog

| Predicado | Archivo | Estado | Descripción |
|---|---|---|---|
| `integral(+F, +X, -R)` | `bc_integrales.pl` | **Existe** | Integral de `F` respecto a `X`, simplificada. Falla si no hay regla aplicable. Para funciones simbólicas `u(x)`, `v(x)` devuelve un término que contiene `pendiente(_, _)`. |
| `integrar(+F, -P)` | `bc_evaluacion.pl` | **Pendiente** | Adaptador: llama a `integral(F, x, P)` y falla si `P` contiene `pendiente(_, _)`. |
| `evaluar_area(+P, +A, +B, -V)` | `bc_evaluacion.pl` | **Pendiente** | Integral definida `V = P(B) − P(A)`. Lanza un error de dominio cuando la evaluación numérica falla o cuando se detecta una singularidad conocida en `[A, B]` (ver sección 9). |
| `area_geometrica(+F, +P, +A, +B, -Ag)` | `bc_evaluacion.pl` | **Opcional** | Área geométrica `∫|F| dx` en `[A, B]`, dividiendo el intervalo en los cambios de signo de `F`. |

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
| `INTERNO` | Java o Prolog | Respuesta ausente o mal formada, o excepción inesperada. |

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
| v1 (borrador) | 15/11/2026 | Primera propuesta basada en el código real de `base_conocimiento` (`1de3224`). | Pendiente |
