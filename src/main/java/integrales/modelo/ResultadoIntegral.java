package integrales.modelo;

/**
 * Respuesta del servicio de integrales (ver {@code docs/contrato.md}, sección 8).
 *
 * <ul>
 *   <li>Si {@code exito} es {@code true}: {@code primitiva} e {@code integralDefinida} tienen valor,
 *       {@code codigoError} y {@code detalle} son {@code null}, y {@code areaGeometrica} puede ser
 *       {@code null} (no se calculó).</li>
 *   <li>Si {@code exito} es {@code false}: {@code codigoError} tiene valor y los campos numéricos
 *       son {@code null}.</li>
 * </ul>
 *
 * <p>Use las fábricas {@link #exito(String, double, Double)} y {@link #error(String, String)}.
 */
public record ResultadoIntegral(boolean exito,
                                String primitiva,
                                Double integralDefinida,
                                Double areaGeometrica,
                                String codigoError,
                                String detalle) {

    /** {@code term_string/2} no pudo leer la expresión. Lo genera Prolog. */
    public static final String SINTAXIS = "SINTAXIS";
    /** {@code integrar/2} falló: no hay regla para la expresión. Lo genera Prolog. */
    public static final String NO_SOPORTADA = "NO_SOPORTADA";
    /** La evaluación numérica falló o hay una singularidad en [a, b]. Lo genera Prolog. */
    public static final String DOMINIO = "DOMINIO";
    /** No se encontró el ejecutable {@code swipl}. Lo genera Java. */
    public static final String PROLOG_NO_DISPONIBLE = "PROLOG_NO_DISPONIBLE";
    /** La consulta a Prolog superó el tiempo máximo. Lo genera Java. */
    public static final String TIEMPO_AGOTADO = "TIEMPO_AGOTADO";
    /** Respuesta ausente o mal formada, o excepción inesperada. Java o Prolog. */
    public static final String INTERNO = "INTERNO";

    /** Resultado exitoso. {@code areaGeometrica} es {@code null} si no se calculó. */
    public static ResultadoIntegral exito(String primitiva, double integralDefinida, Double areaGeometrica) {
        return new ResultadoIntegral(true, primitiva, integralDefinida, areaGeometrica, null, null);
    }

    /** Resultado con error. {@code codigoError} es uno de los códigos de esta clase. */
    public static ResultadoIntegral error(String codigoError, String detalle) {
        return new ResultadoIntegral(false, null, null, null, codigoError, detalle);
    }
}
