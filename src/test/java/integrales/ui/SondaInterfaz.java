package integrales.ui;

import integrales.validacion.EntradaInvalidaException.Campo;

import javax.swing.JButton;

/**
 * Puente solo para pruebas: permite que las pruebas de otros paquetes (por ejemplo, del
 * controlador) lean lo que muestran los paneles sin ampliar su API pública.
 * Todos los métodos deben llamarse desde el EDT.
 */
public final class SondaInterfaz {

    private SondaInterfaz() {
    }

    // --- PanelEntrada ----------------------------------------------------

    public static boolean errorEntradaVisible(PanelEntrada panel) {
        return panel.errorVisible();
    }

    public static String textoErrorEntrada(PanelEntrada panel) {
        return panel.textoError();
    }

    public static boolean campoConError(PanelEntrada panel, Campo campo) {
        return Estilo.tieneError(panel.campo(campo));
    }

    public static boolean campoHabilitado(PanelEntrada panel, Campo campo) {
        return panel.campo(campo).isEnabled();
    }

    public static JButton botonResolver(PanelEntrada panel) {
        return panel.botonResolver();
    }

    public static JButton botonLimpiar(PanelEntrada panel) {
        return panel.botonLimpiar();
    }

    // --- PanelResultado --------------------------------------------------

    public static boolean resultadoVisible(PanelResultado panel) {
        return panel.valoresVisibles();
    }

    public static boolean errorResultadoVisible(PanelResultado panel) {
        return panel.errorVisible();
    }

    public static String primitiva(PanelResultado panel) {
        return panel.textoPrimitiva();
    }

    public static String integralDefinida(PanelResultado panel) {
        return panel.textoIntegralDefinida();
    }

    public static String areaGeometrica(PanelResultado panel) {
        return panel.textoAreaGeometrica();
    }

    public static boolean notaVisible(PanelResultado panel) {
        return panel.notaVisible();
    }

    public static String nota(PanelResultado panel) {
        return panel.textoNota();
    }

    public static String tituloError(PanelResultado panel) {
        return panel.textoTituloError();
    }

    public static String detalleError(PanelResultado panel) {
        return panel.textoDetalle();
    }

    public static String sinValor() {
        return PanelResultado.SIN_VALOR;
    }

    public static String calculando() {
        return PanelResultado.CALCULANDO;
    }
}
