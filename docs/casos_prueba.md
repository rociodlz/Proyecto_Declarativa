# Casos de prueba

> **Estado:** casos propuestos. La columna **Resultado obtenido** se llena al probar el sistema completo.

Los valores esperados se calcularon de forma analítica y se comprobaron con integración numérica, redondeados a 6 decimales. Los límites con π se ingresan como decimales: π ≈ `3.141593`, π/2 ≈ `1.570796`, 2π ≈ `6.283185`.

- **Integral definida:** `P(b) − P(a)`; puede ser negativa.
- **Área geométrica:** `∫|f(x)| dx` en el intervalo; siempre ≥ 0. Es **opcional**: mientras no esté implementada, la interfaz no muestra un valor.

## 1. Tipos de integrales del PDF oficial

| # | Tipo PDF | f(x) | a | b | Primitiva esperada | Integral definida | Área geométrica | Resultado obtenido |
|---|---|---|---|---|---|---|---|---|
| 1 | a) k | `5` | 0 | 2 | `5*x` | 10.000000 | 10.000000 | |
| 2 | b) x | `x` | 0 | 2 | `x^2/2` | 2.000000 | 2.000000 | |
| 3 | c) k·x | `3*x` | 1 | 3 | `3*(x^2/2)` | 12.000000 | 12.000000 | |
| 4 | d) xⁿ | `x^3` | -1 | 2 | `x^4/4` | 3.750000 | 4.250000 | |
| 5 | d) k·xⁿ | `3*x^2` | 0 | 2 | `x^3` | 8.000000 | 8.000000 | |
| 6 | d) x⁻¹ | `x^(-1)` | 1 | 2 | `ln(abs(x))` | 0.693147 | 0.693147 | |
| 7 | d) x⁻² | `x^(-2)` | 1 | 2 | `-1/x` | 0.500000 | 0.500000 | |
| 8 | g) u + v | `x^2+x` | 0 | 1 | `x^3/3+x^2/2` | 0.833333 | 0.833333 | |
| 9 | h) u − v | `x^2-x` | 0 | 2 | `x^3/3-x^2/2` | 0.666667 | 1.000000 | |
| 10 | i) aˣ | `2^x` | 0 | 3 | `2^x/ln(2)` | 10.098865 | 10.098865 | |
| 11 | k) sin | `sin(x)` | 0 | π | `-cos(x)` | 2.000000 | 2.000000 | |
| 12 | k) sin | `sin(x)` | 0 | 2π | `-cos(x)` | 0.000000 | 4.000000 | |
| 13 | l) cos | `cos(x)` | 0 | π/2 | `sin(x)` | 1.000000 | 1.000000 | |
| 14 | m) tan | `tan(x)` | 0 | 1 | `-ln(abs(cos(x)))` | 0.615626 | 0.615626 | |
| 15 | n) asin | `asin(x)` | 0 | 1 | `x*asin(x)+sqrt(1-x^2)` | 0.570796 | 0.570796 | |
| 16 | o) acos | `acos(x)` | 0 | 1 | `x*acos(x)-sqrt(1-x^2)` | 1.000000 | 1.000000 | |
| 17 | p) atan | `atan(x)` | 0 | 1 | `x*atan(x)-ln(1+x^2)/2` | 0.438825 | 0.438825 | |
| 18 | q) sinh | `sinh(x)` | 0 | 1 | `cosh(x)` | 0.543081 | 0.543081 | |
| 19 | r) cosh | `cosh(x)` | 0 | 1 | `sinh(x)` | 1.175201 | 1.175201 | |
| 20 | s) tanh | `tanh(x)` | 0 | 1 | `ln(cosh(x))` | 0.433781 | 0.433781 | |
| 21 | t) asinh | `asinh(x)` | 0 | 1 | `x*asinh(x)-sqrt(x^2+1)` | 0.467160 | 0.467160 | |
| 22 | u) acosh | `acosh(x)` | 1 | 2 | `x*acosh(x)-sqrt(x-1)*sqrt(x+1)` | 0.901865 | 0.901865 | |
| 23 | v) atanh | `atanh(x)` | 0 | 0.5 | `x*atanh(x)+ln(1-x^2)/2` | 0.130812 | 0.130812 | |

La primitiva esperada es matemáticamente equivalente a la indicada; su forma exacta depende de la simplificación de la base de conocimiento.

## 2. Límites especiales

| # | f(x) | a | b | Integral definida | Área geométrica | Comportamiento esperado | Resultado obtenido |
|---|---|---|---|---|---|---|---|
| 24 | `x` | 1 | 1 | 0.000000 | 0.000000 | a = b es válido | |
| 25 | `x` | 2 | 0 | -2.000000 | 2.000000 | a > b es válido; se muestra nota de valor negativo | |

## 3. Dominio y singularidades (se espera `DOMINIO`)

| # | f(x) | a | b | Motivo | Resultado obtenido |
|---|---|---|---|---|---|
| 26 | `x^(-1)` | -1 | 1 | Singularidad en x = 0 | |
| 27 | `x^(-2)` | -1 | 1 | Singularidad en x = 0 | |
| 28 | `tan(x)` | 0 | 2 | Singularidad en x = π/2 | |
| 29 | `asin(x)` | 0 | 2 | Fuera del dominio [−1, 1] | |
| 30 | `acosh(x)` | 0 | 2 | Fuera del dominio x ≥ 1 | |
| 31 | `atanh(x)` | 0 | 1 | atanh(1) no está definida | |

## 4. Integrales no soportadas actualmente (se espera `NO_SOPORTADA`)

Estado verificado el 15/11/2026 sobre el commit `1de3224` de `base_conocimiento`. Si se agregan reglas, el resultado esperado cambia.

| # | f(x) | Motivo | Resultado obtenido |
|---|---|---|---|
| 32 | `e^x` | `e` no se trata como constante numérica | |
| 33 | `x*3` | La constante debe ir a la izquierda | |
| 34 | `-x` | No hay regla para la negación | |
| 35 | `x/2` | No hay regla para la división | |
| 36 | `sin(2*x)` | No hay regla de la cadena | |

El caso 37 (`u(x)`) se movió a la sección 5: la interfaz rechaza `u` y `v` antes de llamar a Prolog.

## 5. Validación de la entrada (la interfaz rechaza antes de llamar a Prolog)

Reglas E1–E14 y L1–L5 de `docs/contrato.md`, sección 12. Estos casos también están cubiertos por `ValidadorEntradaTest`; la columna **Resultado obtenido** se refiere a la prueba manual en la interfaz.

| # | Entrada | Campo | Regla | Mensaje esperado (resumen) | Resultado obtenido |
|---|---|---|---|---|---|
| 37 | `u(x)` | f(x) | E6 | Función genérica: escriba la función concreta | |
| 38 | _(vacío)_ | f(x) | E1 | La expresión está vacía | |
| 39 | `3*x^` | f(x) | E13 | Expresión incompleta | |
| 40 | `(x+1` | f(x) | E9 | Paréntesis no balanceados | |
| 41 | `sin()` | f(x) | E10 | Paréntesis vacíos | |
| 42 | `x**2` | f(x) | E11 | Operadores consecutivos | |
| 43 | `y+1` | f(x) | E6 | Solo se permite la variable x | |
| 44 | `foo(x)` | f(x) | E6 | Función no permitida | |
| 45 | `1,5` | a | L2 | Use punto decimal | |
| 46 | `abc` | b | L3 | El límite b debe ser un número | |
| 47 | `1e7` | a | L5 | Debe estar entre -1000000 y 1000000 (límite provisional) | |
| 48 | 201 caracteres | f(x) | E2 | El máximo es 200 | |
| 52 | `sinx` | f(x) | E7 | ¿Quiso escribir sin(x)? | |
| 53 | `sin x` | f(x) | E8 | La función sin debe ir seguida de "(" | |
| 54 | `2 3` | f(x) | E14 | Falta un operador | |

### 5.1 Entradas que la validación acepta (las transforma el normalizador)

| # | Entrada | Forma normalizada esperada | Resultado obtenido |
|---|---|---|---|
| 55 | `3x^2` | `3*x^2` | |
| 56 | `2*-x` | `2*(-x)` | |
| 57 | `x^-1` | `x^(-1)` | |
| 58 | `.5*x` | `0.5*x` | |
| 59 | `Sen(X)` | `sin(x)` | |
| 60 | `arcsinh(x)` | `asinh(x)` (pendiente de confirmar por Erick) | |
| 61 | `(x+1)(x-1)` | `(x+1)*(x-1)` | |

## 5.2 Revisión visual de la interfaz (vista previa)

Se realiza con `VistaPreviaInterfaz` (ver README). Los datos son simulados.

| # | Acción | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 62 | Abrir la vista previa | Ventana con la franja ámbar "VISTA PREVIA DE DESARROLLO" | |
| 63 | Escribir en f(x), a y b; pulsar Resolver (o Enter) | Estado "Calculando…", campos y botones desactivados; luego el resultado simulado del caso 5 con la nota de dato simulado | |
| 64 | Pulsar Limpiar (o Alt+L) | Campos y resultado vacíos; foco en f(x) | |
| 65 | Menú Vista previa > Resultado con raíz | Primitiva `x*asin(x)+sqrt(1-x^2) + C`; valores 0.570796 | |
| 66 | Menú Vista previa > Área no disponible | Área "No disponible" con tooltip; nota explicativa | |
| 67 | Menú Vista previa > Límites invertidos | Integral -2, área 2, nota de límites invertidos | |
| 68 | Menú Vista previa > Error de entrada | Campo b con borde rojo y mensaje; sin resultado visible | |
| 69 | Editar el campo b después del error | Desaparecen el borde rojo y el mensaje | |
| 70 | Menú Vista previa > Error NO_SOPORTADA | Recuadro rojo con título y detalle técnico; sin valores visibles | |
| 71 | Seleccionar la primitiva con el ratón y copiar (Ctrl+C) | El texto se copia | |
| 72 | Redimensionar al mínimo (720×560) y al máximo | Sin cortes; la primitiva larga se ajusta en varias líneas; aparece barra de desplazamiento vertical si hace falta | |
| 73 | Ayuda > Sintaxis (F1) y Ayuda > Acerca de | Diálogos en español | |

## 5.3 Controlador

### Pruebas automáticas (`ControladorIntegralTest`)

Usan la ventana real (sin mostrarla) y servicios simulados con lambdas, sincronizados con `CountDownLatch`, sin pausas. Se omiten si no hay entorno gráfico.

| Aspecto | Qué se comprueba |
|---|---|
| Construcción | Fuera del EDT lanza `IllegalStateException`; argumentos nulos se rechazan |
| Botones | Resolver (botón por defecto / Enter) y Limpiar ejecutan el controlador |
| Validación (9 casos) | f(x) vacía, incompleta, con `y` o con `u(x)`; a = `abc` o `1e7`; b = `1,5` o vacío; f(x) y límites inválidos a la vez → error solo en el primer campo; **el servicio no se llama** |
| Resultado anterior | Un error de entrada borra el resultado que se mostraba |
| Solicitud | La expresión llega sin espacios exteriores y sin normalizar (`Sen( X )`); límites como `double` |
| Límites | a > b y a = b llegan al servicio; se muestra la nota correspondiente |
| Éxito | Primitiva `x*asin(x)+sqrt(1-x^2) + C`, `0.570796`; área `null` → "No disponible" |
| Errores | `NO_SOPORTADA` muestra su título y detalle; excepción, `null`, éxito sin integral o con `NaN` → "Error interno" y la interfaz se rehabilita |
| Hilos | El servicio se ejecuta fuera del EDT |
| Carga | Mientras calcula: Resolver, Limpiar y campos desactivados, "Calculando…"; al terminar se rehabilitan |
| Concurrencia | Un segundo Resolver durante un cálculo se ignora (el servicio se llama una vez) |
| Limpiar durante un cálculo | La interfaz queda limpia, la respuesta antigua se descarta y un cálculo nuevo muestra su propio resultado |
| Limpiar antes de que empiece el servicio | Resolver y Limpiar en la misma tarea del EDT: el trabajo queda cancelado y no aparece ningún resultado posterior |
| Tiempo agotado (200 ms en la prueba) | Con un servicio bloqueado que **ignora la interrupción**: "Tiempo agotado", interfaz habilitada, la respuesta tardía no cambia la pantalla y se puede iniciar un cálculo nuevo |
| Temporizador antiguo | Limpiar detiene el temporizador; si el de una operación anterior se disparara durante un cálculo nuevo, no lo afecta |
| Cerrar la ventana | Con un cálculo en curso: el trabajo se cancela y el temporizador se detiene; la respuesta posterior del servicio no provoca errores |
| Tiempo máximo inválido | Cero, negativo o nulo se rechazan |

Las pruebas del controlador se repitieron 30 veces seguidas sin fallos, para descartar condiciones de carrera.

### Revisión manual (cuando `Main` conecte el controlador)

| # | Acción | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 74 | `ejecutar.bat --mock`, escribir `3*x^2`, a = 0, b = 2, Enter | "Calculando…" y luego primitiva `x^3 + C`, integral 8, área 8 | |
| 75 | f(x) = `x^5` (respuesta lenta del mock, 3 s) | Campos y botones desactivados durante el cálculo; luego `x^6/6 + C` y área "No disponible" | |
| 76 | f(x) = `x*sin(x)` | Error "Integral no soportada" | |
| 77 | f(x) = `x^(-1)`, a = -1, b = 1 | Error "Fuera del dominio" | |
| 78 | b = `abc` | Error en el campo b; el mock no se consulta | |

## 6. Errores del entorno

| # | Situación | Código | Resultado obtenido |
|---|---|---|---|
| 49 | `swipl` no encontrado | `PROLOG_NO_DISPONIBLE` | |
| 50 | La consulta a Prolog tarda más de 5 s | `TIEMPO_AGOTADO` | |
| 51 | Mock lento (3 s): el botón Resolver se desactiva mientras calcula | — | |
