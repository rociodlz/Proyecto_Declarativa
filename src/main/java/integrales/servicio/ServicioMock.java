package integrales.servicio;

import integrales.modelo.ResultadoIntegral;
import integrales.modelo.SolicitudIntegral;

/**
 * Servicio de prueba con respuestas fijas, para desarrollar la interfaz sin SWI-Prolog
 * ({@code java -jar ... --mock}).
 *
 * <p>Según la expresión normalizada:
 * <ul>
 *   <li>{@value #EXPRESION_NO_SOPORTADA}: error {@code NO_SOPORTADA}.</li>
 *   <li>{@value #EXPRESION_DOMINIO}: error {@code DOMINIO} si 0 está en [a, b]; si no, éxito con
 *       {@code ln(abs(x))}.</li>
 *   <li>{@value #EXPRESION_LENTA}: tarda 3 s y responde {@code x^6/6}, sin área geométrica
 *       (sirve para comprobar que el botón Resolver se desactiva mientras calcula).</li>
 *   <li>Cualquier otra: la respuesta de {@code 3*x^2}, es decir, primitiva {@code x^3} e integral
 *       {@code b³ − a³}.</li>
 * </ul>
 */
public final class ServicioMock implements ServicioIntegrales {

    public static final String EXPRESION_NO_SOPORTADA = "x*sin(x)";
    public static final String EXPRESION_DOMINIO = "x^(-1)";
    public static final String EXPRESION_LENTA = "x^5";

    private final long demoraMs;
    private final NormalizadorExpresion normalizador = new NormalizadorExpresion();

    public ServicioMock() {
        this(3000);
    }

    /** @param demoraMs cuánto tarda la respuesta de {@value #EXPRESION_LENTA} */
    public ServicioMock(long demoraMs) {
        this.demoraMs = demoraMs;
    }

    @Override
    public ResultadoIntegral resolver(SolicitudIntegral solicitud) {
        double a = solicitud.a();
        double b = solicitud.b();
        switch (normalizador.normalizar(solicitud.expresion())) {
            case EXPRESION_NO_SOPORTADA:
                return ResultadoIntegral.error(ResultadoIntegral.NO_SOPORTADA,
                        "No hay una regla de integración para x*sin(x) (respuesta simulada)");
            case EXPRESION_DOMINIO:
                if (Math.min(a, b) <= 0 && 0 <= Math.max(a, b)) {
                    return ResultadoIntegral.error(ResultadoIntegral.DOMINIO,
                            "Singularidad en x = 0 dentro del intervalo (respuesta simulada)");
                }
                double ln = Math.log(Math.abs(b)) - Math.log(Math.abs(a));
                return ResultadoIntegral.exito("ln(abs(x))", ln, Math.abs(ln));
            case EXPRESION_LENTA:
                try {
                    Thread.sleep(demoraMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return ResultadoIntegral.error(ResultadoIntegral.INTERNO, "Consulta interrumpida");
                }
                return ResultadoIntegral.exito("x^6/6", (Math.pow(b, 6) - Math.pow(a, 6)) / 6, null);
            default:
                double integral = Math.pow(b, 3) - Math.pow(a, 3);
                return ResultadoIntegral.exito("x^3", integral, Math.abs(integral));
        }
    }
}
