package integrales.servicio;

import integrales.modelo.ResultadoIntegral;
import integrales.modelo.SolicitudIntegral;

/** Resuelve una integral: primitiva e integral definida en [a, b]. */
public interface ServicioIntegrales {

    /**
     * Nunca lanza excepciones: los errores se devuelven dentro del resultado.
     * Puede tardar (llama a un proceso externo), así que debe invocarse fuera del hilo de Swing.
     */
    ResultadoIntegral resolver(SolicitudIntegral solicitud);
}
