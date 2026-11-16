package integrales;

import integrales.controlador.ControladorIntegral;
import integrales.modelo.ResultadoIntegral;
import integrales.modelo.SolicitudIntegral;
import integrales.servicio.ConfiguracionProlog;
import integrales.servicio.ServicioIntegrales;
import integrales.servicio.ServicioMock;
import integrales.servicio.ServicioProlog;
import integrales.ui.VentanaPrincipal;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada.
 *
 * <pre>
 * java -jar integrales-declarativa-1.0.0.jar                     interfaz gráfica con SWI-Prolog
 * java -jar integrales-declarativa-1.0.0.jar --mock              interfaz gráfica con respuestas simuladas
 * java -jar integrales-declarativa-1.0.0.jar [--mock] "f" a b    consola: resuelve una integral y termina
 * </pre>
 *
 * <p>El modo consola no valida la entrada; sirve para probar la integración con Prolog sin la interfaz.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        boolean mock = false;
        List<String> posicionales = new ArrayList<>();
        for (String arg : args) {
            if (arg.equals("--mock")) {
                mock = true;
            } else if (arg.equals("--ayuda") || arg.equals("--help")) {
                imprimirUso();
                return;
            } else {
                posicionales.add(arg);
            }
        }

        ConfiguracionProlog configuracion = mock ? null : ConfiguracionProlog.cargar();
        ServicioIntegrales servicio = mock ? new ServicioMock() : new ServicioProlog(configuracion);

        if (posicionales.isEmpty()) {
            iniciarInterfaz(servicio, avisoInicial(configuracion));
        } else if (posicionales.size() == 3) {
            System.exit(resolverEnConsola(servicio, posicionales));
        } else {
            imprimirUso();
            System.exit(2);
        }
    }

    /** Abre la ventana (contrato, sección 14.2). */
    private static void iniciarInterfaz(ServicioIntegrales servicio, String aviso) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("No hay entorno gráfico disponible. Use el modo consola:");
            imprimirUso();
            System.exit(2);
        }
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal.instalarAspecto();
            VentanaPrincipal ventana = new VentanaPrincipal();
            new ControladorIntegral(ventana, servicio);
            ventana.mostrarAviso(aviso);
            ventana.setVisible(true);
        });
    }

    /**
     * Aviso que se muestra en la franja superior de la ventana, o {@code null} si no hace falta.
     * Avisa del modo simulado y de los problemas de instalación que se pueden detectar antes de
     * resolver, para que el usuario no los descubra recién al pulsar Resolver.
     *
     * @param configuracion {@code null} en modo {@code --mock}
     */
    static String avisoInicial(ConfiguracionProlog configuracion) {
        if (configuracion == null) {
            return "Modo de prueba (--mock): las respuestas son simuladas y no se consulta SWI-Prolog.";
        }
        if (configuracion.swipl().isEmpty()) {
            return "No se encontró SWI-Prolog (swipl). Instálelo o indique su ruta en "
                    + ConfiguracionProlog.ARCHIVO + " (swipl.ruta).";
        }
        Path swipl = configuracion.swipl().get();
        // Una ruta con carpeta que no existe es un error de configuración. Un nombre solo
        // (por ejemplo "swipl") se busca en el PATH al ejecutarlo, así que no se comprueba aquí.
        if (swipl.getParent() != null && !Files.isRegularFile(swipl)) {
            return "La ruta de SWI-Prolog configurada no existe: " + swipl + ". Corrija swipl.ruta en "
                    + ConfiguracionProlog.ARCHIVO + " o déjela vacía para buscarlo automáticamente.";
        }
        return configuracion.archivoFaltante()
                .map(archivo -> "Falta " + archivo + " en " + configuracion.directorioProlog()
                        + ". Revise prolog.dir en " + ConfiguracionProlog.ARCHIVO + ".")
                .orElse(null);
    }

    private static int resolverEnConsola(ServicioIntegrales servicio, List<String> args) {
        double a;
        double b;
        try {
            a = Double.parseDouble(args.get(1));
            b = Double.parseDouble(args.get(2));
        } catch (NumberFormatException e) {
            System.err.println("Los límites deben ser números con punto decimal: " + e.getMessage());
            return 2;
        }
        ResultadoIntegral r = servicio.resolver(new SolicitudIntegral(args.get(0), a, b));
        if (r.exito()) {
            System.out.println("Primitiva:          " + r.primitiva() + " + C");
            System.out.println("Integral definida:  " + r.integralDefinida());
            System.out.println("Área geométrica:    "
                    + (r.areaGeometrica() == null ? "(no calculada)" : r.areaGeometrica()));
            return 0;
        }
        System.out.println("Error " + r.codigoError() + ": " + r.detalle());
        return 1;
    }

    private static void imprimirUso() {
        System.err.println("""
                Uso:
                  java -jar integrales-declarativa-1.0.0.jar                     interfaz gráfica con SWI-Prolog
                  java -jar integrales-declarativa-1.0.0.jar --mock              interfaz gráfica con respuestas simuladas
                  java -jar integrales-declarativa-1.0.0.jar [--mock] "f" a b    consola, p. ej.: "3*x^2" 0 2""");
    }
}
