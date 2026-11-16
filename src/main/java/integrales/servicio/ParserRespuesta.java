package integrales.servicio;

import integrales.modelo.ResultadoIntegral;

import java.util.Set;

/**
 * Interpreta la salida estándar de {@code puente.pl} ({@code docs/contrato.md}, sección 6).
 *
 * <pre>
 * OK|&lt;primitiva&gt;|&lt;integral_definida&gt;|&lt;area_geometrica&gt;   (area_geometrica es "-" si no se calculó)
 * ERROR|&lt;CODIGO&gt;|&lt;detalle&gt;
 * </pre>
 *
 * <p>Toma la primera línea que empieza con {@code OK|} o {@code ERROR|} e ignora las demás.
 * Si no hay ninguna, o está mal formada, devuelve un error {@code INTERNO}. Nunca lanza excepciones.
 */
public final class ParserRespuesta {

    private static final Set<String> CODIGOS = Set.of(
            ResultadoIntegral.SINTAXIS,
            ResultadoIntegral.NO_SOPORTADA,
            ResultadoIntegral.DOMINIO,
            ResultadoIntegral.PROLOG_NO_DISPONIBLE,
            ResultadoIntegral.TIEMPO_AGOTADO,
            ResultadoIntegral.INTERNO);

    public ResultadoIntegral parsear(String salida) {
        if (salida == null || salida.isBlank()) {
            return interno("Prolog no devolvió ninguna respuesta");
        }
        for (String linea : salida.split("\\R")) {
            String l = linea.strip();
            if (l.startsWith("OK|")) {
                return parsearOk(l);
            }
            if (l.startsWith("ERROR|")) {
                return parsearError(l);
            }
        }
        return interno("La respuesta de Prolog no contiene una línea OK| ni ERROR|");
    }

    private static ResultadoIntegral parsearOk(String linea) {
        String[] partes = linea.split("\\|", -1);
        if (partes.length != 4) {
            return interno("Respuesta OK mal formada: " + linea);
        }
        String primitiva = partes[1].strip();
        Double integral = numero(partes[2]);
        String textoArea = partes[3].strip();
        Double area = textoArea.equals("-") ? null : numero(textoArea);
        if (primitiva.isEmpty() || integral == null || (area == null && !textoArea.equals("-"))) {
            return interno("Respuesta OK mal formada: " + linea);
        }
        return ResultadoIntegral.exito(primitiva, integral, area);
    }

    private static ResultadoIntegral parsearError(String linea) {
        String[] partes = linea.split("\\|", 3);
        String codigo = partes.length > 1 ? partes[1].strip() : "";
        if (!CODIGOS.contains(codigo)) {
            return interno("Código de error desconocido en la respuesta: " + linea);
        }
        String detalle = partes.length > 2 ? partes[2].strip() : "";
        return ResultadoIntegral.error(codigo, detalle);
    }

    /** Número finito, o {@code null} si el texto no es un número válido. */
    private static Double numero(String texto) {
        try {
            double v = Double.parseDouble(texto.strip());
            return Double.isFinite(v) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static ResultadoIntegral interno(String detalle) {
        return ResultadoIntegral.error(ResultadoIntegral.INTERNO, detalle);
    }
}
