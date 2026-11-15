package integrales;

import integrales.modelo.ResultadoIntegral;
import integrales.modelo.SolicitudIntegral;
import integrales.servicio.ConfiguracionProlog;
import integrales.servicio.ServicioIntegrales;
import integrales.servicio.ServicioMock;
import integrales.servicio.ServicioProlog;

import java.util.ArrayList;
import java.util.List;

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

        ServicioIntegrales servicio = mock
                ? new ServicioMock()
                : new ServicioProlog(ConfiguracionProlog.cargar());

        if (posicionales.isEmpty()) {
            iniciarInterfaz(servicio);
        } else if (posicionales.size() == 3) {
            System.exit(resolverEnConsola(servicio, posicionales));
        } else {
            imprimirUso();
            System.exit(2);
        }
    }

    private static void iniciarInterfaz(ServicioIntegrales servicio) {
        // TODO: conectar la interfaz cuando exista ControladorIntegral (contrato, sección 14.2).
        System.err.println("La interfaz gráfica todavía no está integrada. Use el modo consola:");
        imprimirUso();
        System.exit(2);
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
