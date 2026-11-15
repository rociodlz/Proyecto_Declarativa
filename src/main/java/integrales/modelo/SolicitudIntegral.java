package integrales.modelo;

/**
 * Datos que el usuario ingresa: la función de {@code x} y los límites de integración.
 *
 * <p>Se permiten {@code a == b} (la integral es 0) y {@code a > b} (la integral sale negativa).
 *
 * @param expresion función de {@code x} tal como la escribió el usuario (ya validada)
 * @param a         límite inferior
 * @param b         límite superior
 */
public record SolicitudIntegral(String expresion, double a, double b) {
}
