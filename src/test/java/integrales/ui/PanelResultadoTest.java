package integrales.ui;

import integrales.ui.FormateadorResultado.MensajeError;
import integrales.ui.FormateadorResultado.ResultadoFormateado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static integrales.ui.UtilEDT.enEDT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelResultadoTest {

    private static final ResultadoFormateado RESULTADO =
            new ResultadoFormateado("x^3 + C", "8", "8", null);
    private static final MensajeError ERROR =
            new MensajeError("Integral no soportada", "Mensaje", "detalle");

    private PanelResultado panel;

    @BeforeEach
    void crear() throws Exception {
        enEDT(() -> panel = new PanelResultado());
    }

    @Test
    @DisplayName("Estado inicial: sin valores ni error")
    void inicial() throws Exception {
        enEDT(() -> {
            assertTrue(panel.valoresVisibles());
            assertFalse(panel.errorVisible());
            assertEquals(PanelResultado.SIN_VALOR, panel.textoPrimitiva());
            assertEquals(PanelResultado.SIN_VALOR, panel.textoIntegralDefinida());
            assertEquals(PanelResultado.SIN_VALOR, panel.textoAreaGeometrica());
            assertFalse(panel.notaVisible());
        });
    }

    @Test
    @DisplayName("Muestra un resultado sin nota")
    void resultado() throws Exception {
        enEDT(() -> {
            panel.mostrarResultado(RESULTADO);
            assertEquals("x^3 + C", panel.textoPrimitiva());
            assertEquals("8", panel.textoIntegralDefinida());
            assertEquals("8", panel.textoAreaGeometrica());
            assertFalse(panel.notaVisible());
            assertNull(panel.tooltipArea());
        });
    }

    @Test
    @DisplayName("Muestra la nota y el aviso de área no disponible")
    void notaYAreaNoDisponible() throws Exception {
        enEDT(() -> {
            panel.mostrarResultado(new ResultadoFormateado("ln(|x|) + C", "0.693147",
                    FormateadorResultado.NO_DISPONIBLE, "Nota de prueba"));
            assertTrue(panel.notaVisible());
            assertEquals("Nota de prueba", panel.textoNota());
            assertEquals(FormateadorResultado.NO_DISPONIBLE, panel.textoAreaGeometrica());
            assertNotNull(panel.tooltipArea());
        });
    }

    @Test
    @DisplayName("Nunca se muestran a la vez un resultado y un error")
    void resultadoYErrorExcluyentes() throws Exception {
        enEDT(() -> {
            panel.mostrarResultado(RESULTADO);
            panel.mostrarError(ERROR);
            assertTrue(panel.errorVisible());
            assertFalse(panel.valoresVisibles());
            assertEquals("Integral no soportada", panel.textoTituloError());
            assertEquals("Mensaje", panel.textoMensajeError());

            panel.mostrarResultado(RESULTADO);
            assertTrue(panel.valoresVisibles());
            assertFalse(panel.errorVisible());
        });
    }

    @Test
    @DisplayName("El detalle técnico se oculta si no existe")
    void detalle() throws Exception {
        enEDT(() -> {
            panel.mostrarError(ERROR);
            assertTrue(panel.detalleVisible());
            assertTrue(panel.textoDetalle().contains("detalle"));
            panel.mostrarError(new MensajeError("Título", "Mensaje", null));
            assertFalse(panel.detalleVisible());
        });
    }

    @Test
    @DisplayName("Calculando y limpiar quitan el error y la nota")
    void calculandoYLimpiar() throws Exception {
        enEDT(() -> {
            panel.mostrarError(ERROR);
            panel.mostrarCalculando();
            assertFalse(panel.errorVisible());
            assertEquals(PanelResultado.CALCULANDO, panel.textoIntegralDefinida());

            panel.mostrarResultado(new ResultadoFormateado("x + C", "1", "1", "Nota"));
            panel.limpiar();
            assertEquals(PanelResultado.SIN_VALOR, panel.textoPrimitiva());
            assertFalse(panel.notaVisible());
        });
    }

    @Test
    @DisplayName("Fuera del EDT se lanza IllegalStateException")
    void fueraDelEDT() {
        assertThrows(IllegalStateException.class, () -> panel.limpiar());
        assertThrows(IllegalStateException.class, () -> panel.mostrarResultado(RESULTADO));
    }
}
