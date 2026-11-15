package integrales.validacion;

import java.util.Objects;

/**
 * Indica que un dato escrito por el usuario está mal formado y no debe
 * enviarse a la base de conocimiento.
 *
 * <p>No representa errores de Prolog ({@code NO_SOPORTADA}, {@code DOMINIO}, etc.);
 * esos se devuelven dentro del resultado del servicio.</p>
 */
public class EntradaInvalidaException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Campo de la interfaz que contiene el dato inválido. */
    public enum Campo {
        EXPRESION,
        LIMITE_A,
        LIMITE_B
    }

    private final Campo campo;

    /**
     * @param campo   campo que contiene el error
     * @param mensaje mensaje en español, listo para mostrar al usuario
     */
    public EntradaInvalidaException(Campo campo, String mensaje) {
        super(mensaje);
        this.campo = Objects.requireNonNull(campo, "campo");
    }

    public Campo getCampo() {
        return campo;
    }
}
