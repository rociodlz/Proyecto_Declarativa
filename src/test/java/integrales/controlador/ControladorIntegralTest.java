package integrales.controlador;

import integrales.modelo.ResultadoIntegral;
import integrales.modelo.SolicitudIntegral;
import integrales.servicio.ServicioIntegrales;
import integrales.ui.PanelEntrada;
import integrales.ui.PanelResultado;
import integrales.ui.SondaInterfaz;
import integrales.ui.VentanaPrincipal;
import integrales.validacion.EntradaInvalidaException.Campo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Pruebas del controlador con la ventana real (sin mostrarla) y servicios simulados con lambdas.
 * Los cálculos se sincronizan con {@link CountDownLatch}, sin pausas arbitrarias.
 */
class ControladorIntegralTest {

    private static final long ESPERA_MAXIMA_S = 5;

    private VentanaPrincipal ventana;
    private ControladorIntegral controlador;

    @BeforeEach
    void requierePantalla() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Se necesita entorno gráfico para crear la ventana");
    }

    @AfterEach
    void cerrar() throws Exception {
        if (ventana != null) {
            enEDT(() -> ventana.dispose());
        }
    }

    // ==================================================================
    // Construcción y conexión de botones
    // ==================================================================

    @Test
    @DisplayName("El constructor fuera del EDT lanza IllegalStateException")
    void constructorFueraDelEdt() throws Exception {
        ventana = leer(VentanaPrincipal::new);
        assertThrows(IllegalStateException.class,
                () -> new ControladorIntegral(ventana, s -> ResultadoIntegral.exito("x", 0, 0.0)));
    }

    @Test
    @DisplayName("Argumentos nulos se rechazan")
    void argumentosNulos() throws Exception {
        ventana = leer(VentanaPrincipal::new);
        enEDT(() -> {
            assertThrows(NullPointerException.class, () -> new ControladorIntegral(null, s -> null));
            assertThrows(NullPointerException.class, () -> new ControladorIntegral(ventana, null));
        });
    }

    @Test
    @DisplayName("Los botones Resolver y Limpiar de la ventana ejecutan el controlador")
    void botonesConectados() throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        crear(s -> {
            llamadas.incrementAndGet();
            return ResultadoIntegral.exito("x^3", 8, 8.0);
        });
        escribir("3*x^2", "0", "2");

        CountDownLatch fin = leer(() -> {
            ventana.getRootPane().getDefaultButton().doClick();
            return observarFin();
        });
        esperar(fin);
        assertEquals(1, llamadas.get());
        enEDT(() -> assertEquals("x^3 + C", SondaInterfaz.primitiva(resultado())));

        enEDT(() -> {
            SondaInterfaz.botonLimpiar(entrada()).doClick();
            assertEquals("", entrada().getExpresion());
            assertEquals(SondaInterfaz.sinValor(), SondaInterfaz.primitiva(resultado()));
        });
    }

    // ==================================================================
    // Validación
    // ==================================================================

    @ParameterizedTest(name = "f(x)=[{0}] a=[{1}] b=[{2}] → error en {3}")
    @CsvSource({
            "'',      0,   1,   EXPRESION",
            "3*x^,    0,   1,   EXPRESION",
            "y+1,     0,   1,   EXPRESION",
            "u(x),    0,   1,   EXPRESION",
            "x,       abc, 1,   LIMITE_A",
            "x,       1e7, 1,   LIMITE_A",
            "x,       0,   1;5, LIMITE_B",
            "x,       0,   '',  LIMITE_B",
            "y,       abc, abc, EXPRESION"})
    @DisplayName("Entrada inválida: error en el campo correcto y el servicio no se llama")
    void entradaInvalida(String expresion, String a, String b, Campo campo) throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        crear(s -> {
            llamadas.incrementAndGet();
            return ResultadoIntegral.exito("x", 0, 0.0);
        });
        // En CsvSource la coma separa columnas; "1;5" representa la coma decimal "1,5".
        escribir(expresion, a, b.replace(';', ','));

        enEDT(() -> {
            controlador.resolver();
            assertNull(controlador.trabajoEnCurso(), "No debe iniciarse ningún cálculo");
            assertTrue(SondaInterfaz.errorEntradaVisible(entrada()));
            for (Campo c : Campo.values()) {
                assertEquals(c == campo, SondaInterfaz.campoConError(entrada(), c), c.name());
            }
            assertFalse(ventana.isCalculando());
            assertEquals(SondaInterfaz.sinValor(), SondaInterfaz.primitiva(resultado()));
        });
        assertEquals(0, llamadas.get());
    }

    @Test
    @DisplayName("Un error de entrada borra el resultado anterior")
    void errorDeEntradaBorraResultadoAnterior() throws Exception {
        crear(s -> ResultadoIntegral.exito("x^3", 8, 8.0));
        escribir("3*x^2", "0", "2");
        esperar(resolver());
        enEDT(() -> assertEquals("x^3 + C", SondaInterfaz.primitiva(resultado())));

        escribir("3*x^2", "0", "abc");
        enEDT(() -> {
            controlador.resolver();
            assertTrue(SondaInterfaz.campoConError(entrada(), Campo.LIMITE_B));
            assertEquals(SondaInterfaz.sinValor(), SondaInterfaz.primitiva(resultado()));
            assertFalse(SondaInterfaz.errorResultadoVisible(resultado()));
        });
    }

    @Test
    @DisplayName("La expresión llega validada y sin normalizar; los límites como double")
    void solicitudEnviada() throws Exception {
        AtomicReference<SolicitudIntegral> recibida = new AtomicReference<>();
        crear(s -> {
            recibida.set(s);
            return ResultadoIntegral.exito("x", 0, 0.0);
        });
        escribir("  Sen( X )  ", " 0 ", "3.5");
        esperar(resolver());
        assertEquals(new SolicitudIntegral("Sen( X )", 0.0, 3.5), recibida.get());
    }

    @ParameterizedTest(name = "a = {0}, b = {1}")
    @CsvSource({"2, 0, invertidos", "1, 1, iguales"})
    @DisplayName("Límites invertidos e iguales se envían al servicio y se muestra la nota")
    void limitesInvertidosEIguales(String a, String b, String nota) throws Exception {
        AtomicReference<SolicitudIntegral> recibida = new AtomicReference<>();
        crear(s -> {
            recibida.set(s);
            double valor = (s.b() * s.b() - s.a() * s.a()) / 2;
            return ResultadoIntegral.exito("x^2/2", valor, Math.abs(valor));
        });
        escribir("x", a, b);
        esperar(resolver());
        assertEquals(Double.parseDouble(a), recibida.get().a());
        assertEquals(Double.parseDouble(b), recibida.get().b());
        enEDT(() -> {
            assertTrue(SondaInterfaz.resultadoVisible(resultado()));
            assertTrue(SondaInterfaz.notaVisible(resultado()));
            assertTrue(SondaInterfaz.nota(resultado()).contains(nota));
        });
    }

    // ==================================================================
    // Respuestas del servicio
    // ==================================================================

    @Test
    @DisplayName("Resultado exitoso: primitiva, integral definida y área formateadas")
    void resultadoExitoso() throws Exception {
        crear(s -> ResultadoIntegral.exito("x*asin(x)+raiz(1-x^2,2)",
                0.5707963267948966, 0.5707963267948966));
        escribir("asin(x)", "0", "1");
        esperar(resolver());
        enEDT(() -> {
            assertTrue(SondaInterfaz.resultadoVisible(resultado()));
            assertFalse(SondaInterfaz.errorResultadoVisible(resultado()));
            assertEquals("x*asin(x)+sqrt(1-x^2) + C", SondaInterfaz.primitiva(resultado()));
            assertEquals("0.570796", SondaInterfaz.integralDefinida(resultado()));
            assertEquals("0.570796", SondaInterfaz.areaGeometrica(resultado()));
            assertFalse(SondaInterfaz.notaVisible(resultado()));
        });
    }

    @Test
    @DisplayName("Área geométrica no calculada: se muestra 'No disponible'")
    void areaNoDisponible() throws Exception {
        crear(s -> ResultadoIntegral.exito("ln(abs(x))", Math.log(2), null));
        escribir("x^(-1)", "1", "2");
        esperar(resolver());
        enEDT(() -> {
            assertEquals("ln(|x|) + C", SondaInterfaz.primitiva(resultado()));
            assertEquals("0.693147", SondaInterfaz.integralDefinida(resultado()));
            assertEquals("No disponible", SondaInterfaz.areaGeometrica(resultado()));
        });
    }

    @Test
    @DisplayName("Error del servicio: se muestra su mensaje y no hay valores visibles")
    void errorDelServicio() throws Exception {
        crear(s -> ResultadoIntegral.error(ResultadoIntegral.NO_SOPORTADA, "sin regla para sin(2*x)"));
        escribir("sin(2*x)", "0", "1");
        esperar(resolver());
        enEDT(() -> {
            assertTrue(SondaInterfaz.errorResultadoVisible(resultado()));
            assertFalse(SondaInterfaz.resultadoVisible(resultado()));
            assertEquals("Integral no soportada", SondaInterfaz.tituloError(resultado()));
            assertTrue(SondaInterfaz.detalleError(resultado()).contains("sin regla para sin(2*x)"));
            assertFalse(ventana.isCalculando());
        });
    }

    @Test
    @DisplayName("Excepción inesperada: error interno y la interfaz se recupera")
    void excepcionInesperada() throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        crear(s -> {
            if (llamadas.incrementAndGet() == 1) {
                throw new IllegalStateException("fallo simulado");
            }
            return ResultadoIntegral.exito("x^3", 8, 8.0);
        });
        escribir("3*x^2", "0", "2");
        esperar(resolver());
        enEDT(() -> {
            assertEquals("Error interno", SondaInterfaz.tituloError(resultado()));
            assertTrue(SondaInterfaz.detalleError(resultado()).contains("fallo simulado"));
            assertFalse(ventana.isCalculando());
            assertTrue(SondaInterfaz.botonResolver(entrada()).isEnabled());
        });

        esperar(resolver()); // Se puede volver a calcular.
        enEDT(() -> assertEquals("x^3 + C", SondaInterfaz.primitiva(resultado())));
        assertEquals(2, llamadas.get());
    }

    @Test
    @DisplayName("Resultado nulo: error interno")
    void resultadoNulo() throws Exception {
        crear(s -> null);
        escribir("x", "0", "1");
        esperar(resolver());
        enEDT(() -> {
            assertEquals("Error interno", SondaInterfaz.tituloError(resultado()));
            assertTrue(SondaInterfaz.detalleError(resultado()).contains("no devolvió"));
            assertFalse(ventana.isCalculando());
        });
    }

    @Test
    @DisplayName("Éxito sin integral definida o con valor no finito: error interno, sin inventar valores")
    void exitoMalFormado() throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        crear(s -> llamadas.incrementAndGet() == 1
                ? new ResultadoIntegral(true, "x", null, null, null, null)
                : ResultadoIntegral.exito("x", Double.NaN, null));
        escribir("x", "0", "1");

        esperar(resolver());
        enEDT(() -> assertEquals("Error interno", SondaInterfaz.tituloError(resultado())));
        esperar(resolver());
        enEDT(() -> {
            assertEquals("Error interno", SondaInterfaz.tituloError(resultado()));
            assertFalse(SondaInterfaz.resultadoVisible(resultado()));
            assertFalse(ventana.isCalculando());
            assertTrue(SondaInterfaz.botonResolver(entrada()).isEnabled());
            assertTrue(SondaInterfaz.campoHabilitado(entrada(), Campo.EXPRESION));
        });
    }

    // ==================================================================
    // Hilos, estado de carga y concurrencia
    // ==================================================================

    @Test
    @DisplayName("El servicio se ejecuta fuera del EDT")
    void servicioFueraDelEdt() throws Exception {
        AtomicBoolean enEdt = new AtomicBoolean(true);
        crear(s -> {
            enEdt.set(SwingUtilities.isEventDispatchThread());
            return ResultadoIntegral.exito("x", 0, 0.0);
        });
        escribir("x", "0", "1");
        esperar(resolver());
        assertFalse(enEdt.get());
    }

    @Test
    @DisplayName("Estado de carga mientras calcula y recuperación al terminar")
    void estadoDeCargaYRecuperacion() throws Exception {
        CountDownLatch liberar = new CountDownLatch(1);
        crear(s -> {
            esperarSinInterrupcion(liberar);
            return ResultadoIntegral.exito("x^3", 8, 8.0);
        });
        escribir("3*x^2", "0", "2");
        CountDownLatch fin = resolver();

        enEDT(() -> {
            assertTrue(ventana.isCalculando());
            assertFalse(SondaInterfaz.botonResolver(entrada()).isEnabled());
            assertFalse(SondaInterfaz.botonLimpiar(entrada()).isEnabled());
            assertFalse(SondaInterfaz.campoHabilitado(entrada(), Campo.EXPRESION));
            assertEquals(SondaInterfaz.calculando(), SondaInterfaz.primitiva(resultado()));
        });

        liberar.countDown();
        esperar(fin);
        enEDT(() -> {
            assertFalse(ventana.isCalculando());
            assertNull(controlador.trabajoEnCurso());
            assertTrue(SondaInterfaz.botonResolver(entrada()).isEnabled());
            assertTrue(SondaInterfaz.botonLimpiar(entrada()).isEnabled());
            assertTrue(SondaInterfaz.campoHabilitado(entrada(), Campo.EXPRESION));
            assertEquals("x^3 + C", SondaInterfaz.primitiva(resultado()));
        });
    }

    @Test
    @DisplayName("No se inician cálculos simultáneos")
    void sinCalculosSimultaneos() throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        CountDownLatch liberar = new CountDownLatch(1);
        crear(s -> {
            llamadas.incrementAndGet();
            esperarSinInterrupcion(liberar);
            return ResultadoIntegral.exito("x^3", 8, 8.0);
        });
        escribir("3*x^2", "0", "2");
        CountDownLatch fin = resolver();

        enEDT(() -> {
            SwingWorker<?, ?> primero = controlador.trabajoEnCurso();
            controlador.resolver();                                   // llamada directa
            ventana.getRootPane().getDefaultButton().doClick();       // Enter / botón (desactivado)
            assertSame(primero, controlador.trabajoEnCurso());
        });

        liberar.countDown();
        esperar(fin);
        assertEquals(1, llamadas.get());
    }

    @Test
    @DisplayName("Limpiar durante un cálculo: la interfaz queda limpia y la respuesta antigua se descarta")
    void limpiarDuranteCalculo() throws Exception {
        CountDownLatch primeraIniciada = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch primeraTerminada = new CountDownLatch(1);
        // Las consultas se distinguen por la expresión, no por el orden de llegada.
        crear(s -> {
            if (s.expresion().equals("3*x^2")) {
                primeraIniciada.countDown();
                try {
                    esperarSinInterrupcion(liberar);
                    return ResultadoIntegral.exito("respuesta_antigua", 1, 1.0);
                } finally {
                    primeraTerminada.countDown();
                }
            }
            return ResultadoIntegral.exito("x^2/2", 2, 2.0);
        });
        escribir("3*x^2", "0", "2");
        resolver();
        // Se limpia cuando el servicio ya está trabajando: es el caso que debe descartarse.
        assertTrue(primeraIniciada.await(ESPERA_MAXIMA_S, TimeUnit.SECONDS));

        enEDT(() -> {
            controlador.limpiar();
            assertFalse(ventana.isCalculando());
            assertNull(controlador.trabajoEnCurso());
            assertEquals("", entrada().getExpresion());
            assertEquals(SondaInterfaz.sinValor(), SondaInterfaz.primitiva(resultado()));
            assertTrue(SondaInterfaz.botonResolver(entrada()).isEnabled());
        });

        // La primera consulta termina después de limpiar: su respuesta no debe aparecer.
        liberar.countDown();
        assertTrue(primeraTerminada.await(ESPERA_MAXIMA_S, TimeUnit.SECONDS));
        enEDT(() -> { });   // procesa cualquier evento pendiente en el EDT
        enEDT(() -> {
            assertEquals(SondaInterfaz.sinValor(), SondaInterfaz.primitiva(resultado()));
            assertFalse(SondaInterfaz.errorResultadoVisible(resultado()));
            assertFalse(ventana.isCalculando());
        });

        // Un cálculo nuevo muestra su propio resultado, nunca el antiguo.
        escribir("x", "0", "2");
        esperar(resolver());
        enEDT(() -> assertEquals("x^2/2 + C", SondaInterfaz.primitiva(resultado())));
    }

    @Test
    @DisplayName("Limpiar justo después de Resolver (antes de que el servicio empiece) no produce resultados")
    void limpiarAntesDeQueEmpieceElServicio() throws Exception {
        CountDownLatch servicioTermino = new CountDownLatch(1);
        crear(s -> {
            try {
                return ResultadoIntegral.exito("respuesta_antigua", 1, 1.0);
            } finally {
                servicioTermino.countDown();
            }
        });
        escribir("3*x^2", "0", "2");

        SwingWorker<?, ?> trabajo = leer(() -> {
            // Ambas acciones en la misma tarea del EDT: done() no puede ejecutarse entre ellas.
            controlador.resolver();
            SwingWorker<?, ?> iniciado = controlador.trabajoEnCurso();
            controlador.limpiar();
            return iniciado;
        });
        assertTrue(trabajo.isCancelled());

        // Según el planificador, el servicio pudo no ejecutarse nunca o ejecutarse igualmente:
        // en ambos casos su respuesta debe descartarse. La espera es solo una cota, no una pausa.
        servicioTermino.await(1, TimeUnit.SECONDS);
        enEDT(() -> { });
        enEDT(() -> {
            assertEquals(SondaInterfaz.sinValor(), SondaInterfaz.primitiva(resultado()));
            assertFalse(SondaInterfaz.errorResultadoVisible(resultado()));
            assertFalse(ventana.isCalculando());
            assertNull(controlador.trabajoEnCurso());
        });
    }

    // ==================================================================
    // Tiempo máximo de seguridad
    // ==================================================================

    @Test
    @DisplayName("Tiempo agotado: error TIEMPO_AGOTADO, interfaz habilitada, respuesta tardía descartada y nuevo cálculo posible")
    void tiempoAgotado() throws Exception {
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch lentaTermino = new CountDownLatch(1);
        crear(s -> {
            if (s.expresion().equals("3*x^2")) {
                // Servicio que ignora la interrupción: cancel(true) no lo detiene.
                try {
                    esperarIgnorandoInterrupciones(liberar);
                    return ResultadoIntegral.exito("respuesta_tardia", 1, 1.0);
                } finally {
                    lentaTermino.countDown();
                }
            }
            return ResultadoIntegral.exito("x^2/2", 2, 2.0);
        }, Duration.ofMillis(200));
        escribir("3*x^2", "0", "2");

        // No se espera DONE para detectar el tiempo agotado: al cancelar un trabajo cuyo
        // doInBackground sigue en curso, JDK 17 avisa DONE enseguida, pero JDK 21 lo hace recién
        // cuando doInBackground termina. Se espera lo que ve el usuario.
        CountDownLatch finLenta = resolver();
        esperarHasta(() -> "Tiempo agotado".equals(SondaInterfaz.tituloError(resultado()))
                && !ventana.isCalculando());
        enEDT(() -> {
            assertTrue(SondaInterfaz.errorResultadoVisible(resultado()));
            assertEquals("Tiempo agotado", SondaInterfaz.tituloError(resultado()));
            assertTrue(SondaInterfaz.detalleError(resultado()).contains("200 ms"));
            assertFalse(ventana.isCalculando());
            assertNull(controlador.trabajoEnCurso());
            assertNull(controlador.temporizadorEnCurso());
            assertTrue(SondaInterfaz.botonResolver(entrada()).isEnabled());
            assertTrue(SondaInterfaz.botonLimpiar(entrada()).isEnabled());
            assertTrue(SondaInterfaz.campoHabilitado(entrada(), Campo.EXPRESION));
        });

        // La respuesta tardía llega después: no debe cambiar la pantalla.
        liberar.countDown();
        assertTrue(lentaTermino.await(ESPERA_MAXIMA_S, TimeUnit.SECONDS));
        // SwingWorker avisa DONE en el EDT justo después de ejecutar done(); con este latch,
        // la respuesta tardía ya pasó por el controlador (y se descartó) en JDK 17 y en JDK 21.
        esperar(finLenta);
        enEDT(() -> { });
        enEDT(() -> {
            assertEquals("Tiempo agotado", SondaInterfaz.tituloError(resultado()));
            assertFalse(SondaInterfaz.resultadoVisible(resultado()));
        });

        // Se puede calcular de nuevo.
        escribir("x", "0", "2");
        esperar(resolver());
        enEDT(() -> {
            assertTrue(SondaInterfaz.resultadoVisible(resultado()));
            assertEquals("x^2/2 + C", SondaInterfaz.primitiva(resultado()));
        });
    }

    @Test
    @DisplayName("El temporizador de una operación anterior no afecta a una nueva")
    void temporizadorAntiguoNoAfectaOperacionNueva() throws Exception {
        CountDownLatch liberar = new CountDownLatch(1);
        crear(s -> {
            esperarSinInterrupcion(liberar);
            return ResultadoIntegral.exito("x^3", 8, 8.0);
        });
        escribir("3*x^2", "0", "2");

        CountDownLatch finNueva = leer(() -> {
            controlador.resolver();
            Timer antiguo = controlador.temporizadorEnCurso();
            controlador.limpiar();
            assertFalse(antiguo.isRunning(), "Limpiar debe detener el temporizador");

            entrada().setDatos("3*x^2", "0", "2");
            controlador.resolver();
            CountDownLatch fin = observarFin();
            // Se simula que el temporizador antiguo se dispara tarde, durante el cálculo nuevo.
            for (ActionListener accion : antiguo.getActionListeners()) {
                accion.actionPerformed(new ActionEvent(antiguo, ActionEvent.ACTION_PERFORMED, null));
            }
            assertTrue(ventana.isCalculando(), "El cálculo nuevo debe seguir activo");
            assertNotNull(controlador.trabajoEnCurso());
            assertTrue(controlador.temporizadorEnCurso().isRunning());
            assertEquals(SondaInterfaz.calculando(), SondaInterfaz.primitiva(resultado()));
            return fin;
        });

        liberar.countDown();
        esperar(finNueva);
        enEDT(() -> {
            assertEquals("x^3 + C", SondaInterfaz.primitiva(resultado()));
            assertNull(controlador.temporizadorEnCurso());
        });
    }

    @Test
    @DisplayName("Cerrar la ventana durante un cálculo cancela la operación pendiente")
    void cerrarVentanaDuranteCalculo() throws Exception {
        CountDownLatch iniciado = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch termino = new CountDownLatch(1);
        crear(s -> {
            iniciado.countDown();
            try {
                esperarIgnorandoInterrupciones(liberar);
                return ResultadoIntegral.exito("x^3", 8, 8.0);
            } finally {
                termino.countDown();
            }
        });
        escribir("3*x^2", "0", "2");
        resolver();
        assertTrue(iniciado.await(ESPERA_MAXIMA_S, TimeUnit.SECONDS));

        SwingWorker<?, ?> trabajo = leer(() -> controlador.trabajoEnCurso());
        Timer temporizador = leer(() -> controlador.temporizadorEnCurso());
        // Swing solo envía WINDOW_CLOSED al cerrar una ventana "displayable" (como la que se ve en
        // la aplicación). addNotify() la prepara así sin mostrarla en pantalla.
        enEDT(() -> ventana.addNotify());
        enEDT(() -> ventana.dispose());
        enEDT(() -> { }); // procesa el evento WINDOW_CLOSED

        enEDT(() -> {
            assertTrue(trabajo.isCancelled());
            assertNull(controlador.trabajoEnCurso());
            assertFalse(temporizador.isRunning());
            assertNull(controlador.temporizadorEnCurso());
        });

        // El servicio termina después del cierre sin provocar errores.
        liberar.countDown();
        assertTrue(termino.await(ESPERA_MAXIMA_S, TimeUnit.SECONDS));
        enEDT(() -> { });
        enEDT(() -> assertNull(controlador.trabajoEnCurso()));
    }

    @Test
    @DisplayName("El tiempo máximo debe ser positivo")
    void tiempoMaximoInvalido() throws Exception {
        ventana = leer(VentanaPrincipal::new);
        enEDT(() -> {
            assertThrows(IllegalArgumentException.class,
                    () -> new ControladorIntegral(ventana, s -> null, Duration.ZERO));
            assertThrows(IllegalArgumentException.class,
                    () -> new ControladorIntegral(ventana, s -> null, Duration.ofMillis(-1)));
            assertThrows(NullPointerException.class,
                    () -> new ControladorIntegral(ventana, s -> null, null));
        });
    }

    // ==================================================================
    // Utilidades
    // ==================================================================

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }

    private void crear(ServicioIntegrales servicio) throws Exception {
        enEDT(() -> {
            ventana = new VentanaPrincipal();
            controlador = new ControladorIntegral(ventana, servicio);
        });
    }

    private void crear(ServicioIntegrales servicio, Duration tiempoMaximo) throws Exception {
        enEDT(() -> {
            ventana = new VentanaPrincipal();
            controlador = new ControladorIntegral(ventana, servicio, tiempoMaximo);
        });
    }

    private void escribir(String expresion, String a, String b) throws Exception {
        enEDT(() -> entrada().setDatos(expresion, a, b));
    }

    private PanelEntrada entrada() {
        return ventana.getPanelEntrada();
    }

    private PanelResultado resultado() {
        return ventana.getPanelResultado();
    }

    /** Pulsa Resolver y devuelve un latch que se libera cuando el cálculo terminó y la interfaz se actualizó. */
    private CountDownLatch resolver() throws Exception {
        return leer(() -> {
            controlador.resolver();
            CountDownLatch fin = observarFin();
            assertNotNull(fin, "Se esperaba que se iniciara un cálculo");
            return fin;
        });
    }

    /**
     * Debe llamarse en el EDT, justo después de iniciar el cálculo. SwingWorker dispara
     * state = DONE en el EDT después de ejecutar done(), así que el latch se libera cuando el
     * controlador ya actualizó la interfaz.
     */
    private CountDownLatch observarFin() {
        SwingWorker<?, ?> trabajo = controlador.trabajoEnCurso();
        if (trabajo == null) {
            return null;
        }
        CountDownLatch fin = new CountDownLatch(1);
        trabajo.addPropertyChangeListener(e -> {
            if ("state".equals(e.getPropertyName()) && e.getNewValue() == SwingWorker.StateValue.DONE) {
                fin.countDown();
            }
        });
        return fin;
    }

    private static void esperar(CountDownLatch fin) throws InterruptedException {
        assertTrue(fin.await(ESPERA_MAXIMA_S, TimeUnit.SECONDS), "El cálculo no terminó a tiempo");
    }

    /**
     * Espera hasta que la condición se cumpla, evaluándola siempre en el EDT. Falla si no se cumple
     * en {@link #ESPERA_MAXIMA_S}. Entre comprobaciones hace una pausa corta con
     * {@link LockSupport#parkNanos} para no ocupar el EDT con consultas continuas: no hay ningún
     * evento que avise de un cambio en la interfaz.
     */
    private static void esperarHasta(Callable<Boolean> condicion) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(ESPERA_MAXIMA_S);
        while (!leer(condicion)) {
            assertTrue(System.nanoTime() < limite, "La interfaz no llegó al estado esperado a tiempo");
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
    }

    /** Para servicios simulados: espera el latch; si el hilo se interrumpe (cancelación), continúa. */
    private static void esperarSinInterrupcion(CountDownLatch latch) {
        try {
            latch.await(ESPERA_MAXIMA_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Para simular un servicio bloqueado que no responde a cancel(true): sigue esperando aunque
     * lo interrumpan. Tiene una cota para no dejar hilos colgados si la prueba falla.
     */
    private static void esperarIgnorandoInterrupciones(CountDownLatch latch) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(ESPERA_MAXIMA_S);
        boolean interrumpido = false;
        while (latch.getCount() > 0 && System.nanoTime() < limite) {
            try {
                latch.await(limite - System.nanoTime(), TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                interrumpido = true;
            }
        }
        if (interrumpido) {
            Thread.currentThread().interrupt();
        }
    }

    private static void enEDT(Accion accion) throws Exception {
        leer(() -> {
            accion.ejecutar();
            return null;
        });
    }

    private static <T> T leer(Callable<T> tarea) throws Exception {
        AtomicReference<T> valor = new AtomicReference<>();
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                valor.set(tarea.call());
            } catch (Throwable t) {
                fallo.set(t);
            }
        });
        if (fallo.get() instanceof Exception e) {
            throw e;
        }
        if (fallo.get() instanceof Error e) {
            throw e;
        }
        return valor.get();
    }
}
