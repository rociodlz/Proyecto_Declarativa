package integrales.ui;

import integrales.validacion.EntradaInvalidaException.Campo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.concurrent.atomic.AtomicInteger;

import static integrales.ui.UtilEDT.enEDT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelEntradaTest {

    private PanelEntrada panel;

    @BeforeEach
    void crear() throws Exception {
        enEDT(() -> panel = new PanelEntrada());
    }

    @Test
    @DisplayName("Escribe y lee los tres campos")
    void datos() throws Exception {
        enEDT(() -> {
            panel.setDatos("3*x^2", "0", "2");
            assertEquals("3*x^2", panel.getExpresion());
            assertEquals("0", panel.getLimiteA());
            assertEquals("2", panel.getLimiteB());
        });
    }

    @Test
    @DisplayName("Resolver ejecuta la acción registrada")
    void resolver() throws Exception {
        AtomicInteger veces = new AtomicInteger();
        enEDT(() -> {
            panel.alResolver(veces::incrementAndGet);
            panel.botonResolver().doClick();
        });
        assertEquals(1, veces.get());
    }

    @Test
    @DisplayName("Deshabilitado: los botones no responden")
    void deshabilitado() throws Exception {
        AtomicInteger veces = new AtomicInteger();
        enEDT(() -> {
            panel.alResolver(veces::incrementAndGet);
            panel.setHabilitado(false);
            assertFalse(panel.botonResolver().isEnabled());
            assertFalse(panel.botonLimpiar().isEnabled());
            assertFalse(panel.campo(Campo.EXPRESION).isEnabled());
            panel.botonResolver().doClick();
            panel.setHabilitado(true);
            assertTrue(panel.botonResolver().isEnabled());
        });
        assertEquals(0, veces.get());
    }

    @Test
    @DisplayName("Limpiar (acción por defecto) vacía los campos y quita errores")
    void limpiarPorDefecto() throws Exception {
        enEDT(() -> {
            panel.setDatos("x", "1", "abc");
            panel.mostrarErrorEntrada(Campo.LIMITE_B, "Error");
            panel.botonLimpiar().doClick();
            assertEquals("", panel.getExpresion());
            assertEquals("", panel.getLimiteA());
            assertEquals("", panel.getLimiteB());
            assertFalse(panel.errorVisible());
        });
    }

    @ParameterizedTest(name = "resalta {0}")
    @EnumSource(Campo.class)
    @DisplayName("El error resalta solo el campo indicado")
    void errorEnCampo(Campo campo) throws Exception {
        enEDT(() -> {
            panel.mostrarErrorEntrada(campo, "Mensaje de prueba");
            assertTrue(panel.errorVisible());
            assertEquals("Mensaje de prueba", panel.textoError());
            for (Campo otro : Campo.values()) {
                assertEquals(otro == campo, Estilo.tieneError(panel.campo(otro)), otro.name());
            }
        });
    }

    @Test
    @DisplayName("Editar un campo quita el error")
    void editarQuitaError() throws Exception {
        enEDT(() -> {
            panel.mostrarErrorEntrada(Campo.LIMITE_A, "Error");
            panel.campo(Campo.LIMITE_A).setText("1");
            assertFalse(panel.errorVisible());
            assertFalse(Estilo.tieneError(panel.campo(Campo.LIMITE_A)));
        });
    }

    @Test
    @DisplayName("Un error nuevo reemplaza al anterior")
    void errorReemplaza() throws Exception {
        enEDT(() -> {
            panel.mostrarErrorEntrada(Campo.EXPRESION, "Primero");
            panel.mostrarErrorEntrada(Campo.LIMITE_B, "Segundo");
            assertEquals("Segundo", panel.textoError());
            assertFalse(Estilo.tieneError(panel.campo(Campo.EXPRESION)));
            assertTrue(Estilo.tieneError(panel.campo(Campo.LIMITE_B)));
        });
    }

    @Test
    @DisplayName("Acciones nulas se rechazan")
    void accionesNulas() throws Exception {
        enEDT(() -> {
            assertThrows(NullPointerException.class, () -> panel.alResolver(null));
            assertThrows(NullPointerException.class, () -> panel.alLimpiar(null));
        });
    }

    @Test
    @DisplayName("Fuera del EDT se lanza IllegalStateException")
    void fueraDelEDT() {
        assertThrows(IllegalStateException.class, () -> panel.getExpresion());
        assertThrows(IllegalStateException.class, PanelEntrada::new);
    }
}
