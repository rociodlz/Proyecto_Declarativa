package integrales.ui;

import integrales.ui.FormateadorResultado.MensajeError;
import integrales.ui.FormateadorResultado.ResultadoFormateado;
import integrales.validacion.EntradaInvalidaException;
import integrales.validacion.EntradaInvalidaException.Campo;
import integrales.validacion.ValidadorEntrada;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.KeyEvent;

/**
 * Herramienta de desarrollo: abre la ventana real con datos simulados para
 * revisar la interfaz sin el controlador, sin el servicio y sin SWI-Prolog.
 *
 * <p>No forma parte de la aplicación: está en {@code src/test}, no entra en el
 * JAR y no reemplaza a {@code integrales.Main}. No calcula nada: los valores
 * que muestra se copiaron de {@code docs/casos_prueba.md} y siempre se
 * identifican como simulados.</p>
 *
 * <p>Ejecución (PowerShell, desde la raíz del proyecto):</p>
 * <pre>
 * .\mvnw.cmd -q test-compile
 * &amp; "$env:JAVA_HOME\bin\java.exe" -cp "target\classes;target\test-classes" integrales.ui.VistaPreviaInterfaz
 * </pre>
 */
public final class VistaPreviaInterfaz {

    static final String AVISO = "VISTA PREVIA DE DESARROLLO — Los resultados de esta ventana son datos "
            + "simulados copiados de docs/casos_prueba.md. No fueron calculados por la base de conocimiento.";

    private static final int ESPERA_RESOLVER_MS = 1200;
    private static final int ESPERA_CALCULANDO_MS = 3000;

    /** Caso de demostración tomado de docs/casos_prueba.md. */
    private record Caso(int numero, String expresion, String a, String b,
                        String primitivaProlog, double integralDefinida, Double areaGeometrica) {
        String descripcion() {
            return "f(x) = " + expresion + ", a = " + a + ", b = " + b;
        }
    }

    // Primitivas con la forma que devuelve la base de conocimiento; valores redondeados de casos_prueba.md.
    private static final Caso CASO_5 = new Caso(5, "3*x^2", "0", "2", "x^3", 8.0, 8.0);
    private static final Caso CASO_15 = new Caso(15, "asin(x)", "0", "1",
            "x*asin(x)+raiz(1-x^2,2)", 0.570796, 0.570796);
    private static final Caso CASO_9 = new Caso(9, "x^2-x", "0", "2", "x^3/3-x^2/2", 0.666667, 1.0);
    private static final Caso CASO_6_SIN_AREA = new Caso(6, "x^(-1)", "1", "2", "ln(abs(x))", 0.693147, null);
    private static final Caso CASO_25 = new Caso(25, "x", "2", "0", "x^2/2", -2.0, 2.0);

    private final VentanaPrincipal ventana;
    private final PanelEntrada entrada;
    private final PanelResultado resultado;
    private final FormateadorResultado formateador = new FormateadorResultado();
    private Timer temporizador;

    private VistaPreviaInterfaz() {
        VentanaPrincipal.instalarAspecto();
        ventana = new VentanaPrincipal();
        ventana.setTitle(VentanaPrincipal.TITULO_VENTANA + " (vista previa)");
        ventana.mostrarAviso(AVISO);
        entrada = ventana.getPanelEntrada();
        resultado = ventana.getPanelResultado();

        entrada.alResolver(this::simularResolver);
        entrada.alLimpiar(this::limpiar);
        ventana.getJMenuBar().add(crearMenuVistaPrevia());
        ventana.setEstado("Vista previa: escriba datos y pulse Resolver, o use el menú Vista previa.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VistaPreviaInterfaz().ventana.setVisible(true));
    }

    // ------------------------------------------------------------------

    private JMenu crearMenuVistaPrevia() {
        JMenu menu = new JMenu("Vista previa");
        menu.setMnemonic(KeyEvent.VK_V);
        agregar(menu, "Resultado simulado: 3*x^2 en [0, 2] (caso 5)",
                () -> mostrarCaso(CASO_5, true));
        agregar(menu, "Resultado con raíz: asin(x) en [0, 1] (caso 15)",
                () -> mostrarCaso(CASO_15, true));
        agregar(menu, "Integral y área distintas: x^2-x en [0, 2] (caso 9)",
                () -> mostrarCaso(CASO_9, true));
        agregar(menu, "Área no disponible: x^(-1) en [1, 2]",
                () -> mostrarCaso(CASO_6_SIN_AREA, true));
        agregar(menu, "Límites invertidos: x en [2, 0] (caso 25)",
                () -> mostrarCaso(CASO_25, true));
        menu.addSeparator();
        agregar(menu, "Error de entrada: límite b = abc", this::mostrarErrorEntrada);
        agregar(menu, "Error NO_SOPORTADA: sin(2*x) (caso 36)",
                () -> mostrarErrorCalculo("sin(2*x)", "0", "1", "NO_SOPORTADA"));
        agregar(menu, "Error DOMINIO: x^(-1) en [-1, 1] (caso 26)",
                () -> mostrarErrorCalculo("x^(-1)", "-1", "1", "DOMINIO"));
        agregar(menu, "Error PROLOG_NO_DISPONIBLE (caso 49)",
                () -> mostrarErrorCalculo("3*x^2", "0", "2", "PROLOG_NO_DISPONIBLE"));
        menu.addSeparator();
        agregar(menu, "Estado calculando (3 s)", this::simularCalculando);
        agregar(menu, "Limpiar", this::limpiar);
        return menu;
    }

    private static void agregar(JMenu menu, String texto, Runnable accion) {
        JMenuItem item = new JMenuItem(texto);
        item.addActionListener(e -> accion.run());
        menu.add(item);
    }

    // --- Acciones ------------------------------------------------------

    /** Resolver no calcula: simula la espera y muestra el caso 5, avisando que no es la expresión escrita. */
    private void simularResolver() {
        iniciarCalculo(ESPERA_RESOLVER_MS, () -> {
            ResultadoFormateado r = formatear(CASO_5);
            resultado.mostrarResultado(new ResultadoFormateado(r.primitiva(), r.integralDefinida(),
                    r.areaGeometrica(), "Dato simulado: la vista previa no calcula la expresión escrita. "
                    + "Se muestra el caso 5 de docs/casos_prueba.md (" + CASO_5.descripcion() + ")."
                    + (r.nota() == null ? "" : "\n" + r.nota())));
            ventana.setEstado("Vista previa: resultado simulado (caso 5).");
        });
    }

    private void simularCalculando() {
        iniciarCalculo(ESPERA_CALCULANDO_MS, () -> {
            resultado.limpiar();
            ventana.setEstado("Vista previa: fin del cálculo simulado.");
        });
    }

    private void iniciarCalculo(int milisegundos, Runnable alTerminar) {
        detenerTemporizador();
        entrada.quitarErrores();
        ventana.setCalculando(true);
        ventana.setEstado("Calculando… (simulado)");
        resultado.mostrarCalculando();
        // javax.swing.Timer ejecuta la acción en el EDT y no bloquea la interfaz.
        temporizador = new Timer(milisegundos, e -> {
            ventana.setCalculando(false);
            alTerminar.run();
        });
        temporizador.setRepeats(false);
        temporizador.start();
    }

    private void mostrarCaso(Caso caso, boolean escribirDatos) {
        detenerTemporizador();
        ventana.setCalculando(false);
        if (escribirDatos) {
            entrada.setDatos(caso.expresion(), caso.a(), caso.b());
        }
        entrada.quitarErrores();
        ResultadoFormateado r = formatear(caso);
        String aviso = "Dato simulado (caso " + caso.numero() + " de docs/casos_prueba.md); no fue calculado.";
        if (caso.areaGeometrica() == null) {
            aviso += " El área geométrica se omitió a propósito para mostrar el estado «No disponible».";
        }
        resultado.mostrarResultado(new ResultadoFormateado(r.primitiva(), r.integralDefinida(),
                r.areaGeometrica(), aviso + (r.nota() == null ? "" : "\n" + r.nota())));
        ventana.setEstado("Vista previa: resultado simulado (caso " + caso.numero() + ").");
    }

    private void mostrarErrorEntrada() {
        detenerTemporizador();
        ventana.setCalculando(false);
        entrada.setDatos("3*x^2", "0", "abc");
        resultado.limpiar();
        try {
            new ValidadorEntrada().validarLimite("abc", Campo.LIMITE_B);
        } catch (EntradaInvalidaException e) {
            entrada.mostrarErrorEntrada(e.getCampo(), e.getMessage());
        }
        ventana.setEstado("Vista previa: error de entrada (mensaje real del validador).");
    }

    private void mostrarErrorCalculo(String expresion, String a, String b, String codigo) {
        detenerTemporizador();
        ventana.setCalculando(false);
        entrada.setDatos(expresion, a, b);
        entrada.quitarErrores();
        MensajeError error = formateador.formatearError(codigo,
                "Error simulado para la vista previa; no proviene de SWI-Prolog.");
        resultado.mostrarError(error);
        ventana.setEstado("Vista previa: error simulado " + codigo + ".");
    }

    private void limpiar() {
        detenerTemporizador();
        ventana.setCalculando(false);
        entrada.limpiar();
        resultado.limpiar();
        ventana.setEstado("Vista previa: datos borrados.");
    }

    private ResultadoFormateado formatear(Caso caso) {
        return formateador.formatearExito(caso.primitivaProlog(), caso.integralDefinida(),
                caso.areaGeometrica(), Double.parseDouble(caso.a()), Double.parseDouble(caso.b()));
    }

    private void detenerTemporizador() {
        if (temporizador != null) {
            temporizador.stop();
            temporizador = null;
        }
    }
}
