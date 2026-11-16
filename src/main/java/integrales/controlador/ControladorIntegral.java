package integrales.controlador;

import integrales.modelo.ResultadoIntegral;
import integrales.modelo.SolicitudIntegral;
import integrales.servicio.ServicioIntegrales;
import integrales.ui.FormateadorResultado;
import integrales.ui.PanelEntrada;
import integrales.ui.PanelResultado;
import integrales.ui.VentanaPrincipal;
import integrales.validacion.EntradaInvalidaException;
import integrales.validacion.EntradaInvalidaException.Campo;
import integrales.validacion.ValidadorEntrada;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Conecta la interfaz con la validación y con el servicio de integrales.
 *
 * <ul>
 *   <li><b>Resolver:</b> valida f(x), a y b; si hay un error, lo muestra en su campo y no llama
 *       al servicio. Si la entrada es válida, llama al servicio en un {@link SwingWorker} (nunca
 *       en el EDT) y muestra el resultado o el error con {@link FormateadorResultado}.</li>
 *   <li><b>Limpiar:</b> borra los datos y el resultado. Si hubiera un cálculo en curso, lo
 *       cancela y su respuesta se descarta.</li>
 *   <li><b>Tiempo máximo:</b> si el servicio no responde en {@link #TIEMPO_MAXIMO}, el cálculo se
 *       abandona, la interfaz se rehabilita y se muestra {@code TIEMPO_AGOTADO}. No depende de que
 *       el servicio respete su propio límite ni de que {@code cancel(true)} lo detenga.</li>
 * </ul>
 *
 * <p>Cada cálculo recibe un número de operación. La respuesta del servicio y el aviso del
 * temporizador solo tienen efecto si su número sigue siendo el actual; así un evento antiguo
 * nunca modifica el estado de una operación más reciente. Mientras se calcula, la ventana
 * desactiva Resolver, Limpiar y los campos, por lo que no puede haber dos cálculos a la vez.</p>
 *
 * <p>Debe crearse y usarse desde el hilo de eventos de Swing (EDT).</p>
 */
public final class ControladorIntegral {

    /** Tiempo máximo de seguridad de cada cálculo. Mayor que el del servicio (5 s) más el arranque de Prolog. */
    public static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(15);

    static final String ESTADO_ENTRADA_INVALIDA = "Revise los datos ingresados.";
    static final String ESTADO_COMPLETADO = "Cálculo completado.";
    static final String ESTADO_ERROR = "No se pudo completar el cálculo.";

    private final VentanaPrincipal ventana;
    private final PanelEntrada entrada;
    private final PanelResultado resultado;
    private final ServicioIntegrales servicio;
    private final Duration tiempoMaximo;
    private final ValidadorEntrada validador = new ValidadorEntrada();
    private final FormateadorResultado formateador = new FormateadorResultado();

    // Solo se leen y modifican en el EDT: no necesitan sincronización.
    private int operacionActual;
    private SwingWorker<ResultadoIntegral, Void> trabajo;
    private Timer temporizador;

    /**
     * Registra las acciones de Resolver y Limpiar en la ventana. Usa {@link #TIEMPO_MAXIMO}.
     *
     * @throws IllegalStateException si no se llama desde el EDT
     */
    public ControladorIntegral(VentanaPrincipal ventana, ServicioIntegrales servicio) {
        this(ventana, servicio, TIEMPO_MAXIMO);
    }

    /** Solo para pruebas: permite un tiempo máximo corto. */
    ControladorIntegral(VentanaPrincipal ventana, ServicioIntegrales servicio, Duration tiempoMaximo) {
        verificarEDT();
        this.ventana = Objects.requireNonNull(ventana, "ventana");
        this.servicio = Objects.requireNonNull(servicio, "servicio");
        this.tiempoMaximo = Objects.requireNonNull(tiempoMaximo, "tiempoMaximo");
        if (tiempoMaximo.isNegative() || tiempoMaximo.isZero()) {
            throw new IllegalArgumentException("El tiempo máximo debe ser positivo: " + tiempoMaximo);
        }
        this.entrada = ventana.getPanelEntrada();
        this.resultado = ventana.getPanelResultado();

        entrada.alResolver(this::resolver);
        entrada.alLimpiar(this::limpiar);
        ventana.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // Un cálculo que termine después de cerrar la ventana no debe tocarla.
                cancelarCalculo();
            }
        });
    }

    /** Equivale a pulsar Resolver. */
    public void resolver() {
        verificarEDT();
        if (trabajo != null) {
            return; // Ya hay un cálculo en curso.
        }
        entrada.quitarErrores();

        SolicitudIntegral solicitud;
        try {
            String expresion = validador.validarExpresion(entrada.getExpresion());
            double a = validador.validarLimite(entrada.getLimiteA(), Campo.LIMITE_A);
            double b = validador.validarLimite(entrada.getLimiteB(), Campo.LIMITE_B);
            solicitud = new SolicitudIntegral(expresion, a, b);
        } catch (EntradaInvalidaException e) {
            resultado.limpiar(); // Nunca queda un resultado anterior junto a un error.
            entrada.mostrarErrorEntrada(e.getCampo(), e.getMessage());
            ventana.setEstado(ESTADO_ENTRADA_INVALIDA);
            return;
        }

        int operacion = ++operacionActual;
        ventana.setCalculando(true);
        resultado.mostrarCalculando();
        trabajo = new SwingWorker<>() {
            @Override
            protected ResultadoIntegral doInBackground() {
                return servicio.resolver(solicitud); // Hilo secundario.
            }

            @Override
            protected void done() {
                terminar(operacion, solicitud, this); // De vuelta en el EDT.
            }
        };
        // javax.swing.Timer ejecuta su acción en el EDT. Cada temporizador conoce su operación,
        // así que uno antiguo no puede afectar a un cálculo nuevo.
        temporizador = new Timer(Math.toIntExact(Math.max(1, tiempoMaximo.toMillis())),
                e -> agotarTiempo(operacion));
        temporizador.setRepeats(false);
        temporizador.start();
        trabajo.execute();
    }

    /** Equivale a pulsar Limpiar. Si hay un cálculo en curso, lo cancela y descarta su respuesta. */
    public void limpiar() {
        verificarEDT();
        cancelarCalculo();
        ventana.setCalculando(false);
        entrada.limpiar();
        resultado.limpiar();
    }

    // ------------------------------------------------------------------

    /**
     * Invalida la operación actual, detiene su temporizador y pide la cancelación del trabajo.
     * {@code cancel(true)} solo interrumpe el hilo: si el servicio no responde a la interrupción,
     * sigue ejecutándose, pero su respuesta se descartará por el número de operación.
     */
    private void cancelarCalculo() {
        operacionActual++;
        detenerTemporizador();
        if (trabajo != null) {
            trabajo.cancel(true);
            trabajo = null;
        }
    }

    private void detenerTemporizador() {
        if (temporizador != null) {
            temporizador.stop();
            temporizador = null;
        }
    }

    private void agotarTiempo(int operacion) {
        if (operacion != operacionActual) {
            return; // Temporizador de una operación que ya terminó o se descartó.
        }
        cancelarCalculo();
        ventana.setCalculando(false);
        resultado.mostrarError(formateador.formatearError(ResultadoIntegral.TIEMPO_AGOTADO,
                "El servicio no respondió en " + describir(tiempoMaximo) + "; el cálculo se canceló."));
        ventana.setEstado(ESTADO_ERROR);
    }

    private void terminar(int operacion, SolicitudIntegral solicitud,
                          SwingWorker<ResultadoIntegral, Void> terminado) {
        if (operacion != operacionActual) {
            return; // Respuesta obsoleta: se limpió, se agotó el tiempo o se cerró la ventana.
        }
        trabajo = null;
        detenerTemporizador();
        ventana.setCalculando(false);

        ResultadoIntegral respuesta;
        try {
            respuesta = terminado.get(); // No bloquea: el trabajo ya terminó.
        } catch (CancellationException e) {
            // No debería ocurrir (toda cancelación invalida antes la operación), pero si ocurre
            // la interfaz no se queda en "Calculando…".
            resultado.limpiar();
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            mostrarErrorInterno("La consulta fue interrumpida.");
            return;
        } catch (ExecutionException e) {
            mostrarErrorInterno("Error inesperado del servicio: " + e.getCause());
            return;
        }
        mostrar(respuesta, solicitud);
    }

    private void mostrar(ResultadoIntegral respuesta, SolicitudIntegral solicitud) {
        if (respuesta == null) {
            mostrarErrorInterno("El servicio no devolvió ningún resultado.");
            return;
        }
        if (!respuesta.exito()) {
            resultado.mostrarError(formateador.formatearError(respuesta.codigoError(), respuesta.detalle()));
            ventana.setEstado(ESTADO_ERROR);
            return;
        }
        if (respuesta.integralDefinida() == null) {
            mostrarErrorInterno("El servicio indicó éxito pero no envió la integral definida.");
            return;
        }
        try {
            resultado.mostrarResultado(formateador.formatearExito(respuesta.primitiva(),
                    respuesta.integralDefinida(), respuesta.areaGeometrica(), solicitud.a(), solicitud.b()));
            ventana.setEstado(ESTADO_COMPLETADO);
        } catch (RuntimeException e) {
            // Primitiva vacía o valor no finito: el formateador no inventa un resultado.
            mostrarErrorInterno("Respuesta del servicio no válida: " + e.getMessage());
        }
    }

    private void mostrarErrorInterno(String detalle) {
        resultado.mostrarError(formateador.formatearError(ResultadoIntegral.INTERNO, detalle));
        ventana.setEstado(ESTADO_ERROR);
    }

    private static String describir(Duration duracion) {
        long ms = duracion.toMillis();
        return ms % 1000 == 0 ? (ms / 1000) + " s" : ms + " ms";
    }

    private static void verificarEDT() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "El controlador debe usarse desde el hilo de eventos de Swing (EDT).");
        }
    }

    // --- Solo para pruebas ----------------------------------------------

    /** Cálculo en curso, o {@code null}. Permite a las pruebas esperar su fin sin pausas. */
    SwingWorker<ResultadoIntegral, Void> trabajoEnCurso() {
        return trabajo;
    }

    /** Temporizador de seguridad del cálculo en curso, o {@code null}. */
    Timer temporizadorEnCurso() {
        return temporizador;
    }
}
