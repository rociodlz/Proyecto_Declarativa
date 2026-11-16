package integrales.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import integrales.modelo.ResultadoIntegral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ParserRespuestaTest {

    private final ParserRespuesta parser = new ParserRespuesta();

    @Test
    void okConAreaGeometrica() {
        ResultadoIntegral r = parser.parsear("OK|x^3|8.0|8.0\n");
        assertTrue(r.exito());
        assertEquals("x^3", r.primitiva());
        assertEquals(8.0, r.integralDefinida());
        assertEquals(8.0, r.areaGeometrica());
        assertNull(r.codigoError());
        assertNull(r.detalle());
    }

    @Test
    void okSinAreaGeometrica() {
        ResultadoIntegral r = parser.parsear("OK|-cos(x)|1.99999999999994|-");
        assertTrue(r.exito());
        assertEquals("-cos(x)", r.primitiva());
        assertEquals(1.99999999999994, r.integralDefinida());
        assertNull(r.areaGeometrica());
    }

    @Test
    void okConNumerosEnteroNegativoYCientifico() {
        assertEquals(8.0, parser.parsear("OK|x^3|8|-").integralDefinida());
        assertEquals(-2.0, parser.parsear("OK|x^2/2|-2.0|2.0").integralDefinida());
        assertEquals(1.0e10, parser.parsear("OK|x|1.0e10|-").integralDefinida());
    }

    @Test
    void primitivaConEspaciosYOperadores() {
        ResultadoIntegral r = parser.parsear("OK|2* -cos(x)|4.0|-");
        assertEquals("2* -cos(x)", r.primitiva());
    }

    @Test
    void errorConDetalle() {
        ResultadoIntegral r = parser.parsear("ERROR|NO_SOPORTADA|No hay una regla de integración para x*sin(x)");
        assertFalse(r.exito());
        assertEquals(ResultadoIntegral.NO_SOPORTADA, r.codigoError());
        assertEquals("No hay una regla de integración para x*sin(x)", r.detalle());
        assertNull(r.primitiva());
        assertNull(r.integralDefinida());
        assertNull(r.areaGeometrica());
    }

    @Test
    void errorSinDetalle() {
        ResultadoIntegral r = parser.parsear("ERROR|DOMINIO");
        assertEquals(ResultadoIntegral.DOMINIO, r.codigoError());
        assertEquals("", r.detalle());
    }

    @Test
    void detalleConBarraSeConserva() {
        assertEquals("a|b", parser.parsear("ERROR|INTERNO|a|b").detalle());
    }

    @Test
    void tomaLaPrimeraLineaValidaEIgnoraLasDemas() {
        String salida = "Warning: algo\r\n% cargando...\r\nERROR|SINTAXIS|x^\r\nOK|x|1.0|-\r\n";
        ResultadoIntegral r = parser.parsear(salida);
        assertEquals(ResultadoIntegral.SINTAXIS, r.codigoError());
        assertEquals("x^", r.detalle());
    }

    @Test
    void lineaConEspaciosAlrededor() {
        assertTrue(parser.parsear("   OK|x|1.0|-   ").exito());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "hola",
            "ok|x|1|-",
            "OK|x|1",
            "OK|x|1|-|extra",
            "OK||1|-",
            "OK|x|abc|-",
            "OK|x|1|abc",
            "OK|x|NaN|-",
            "OK|x|Infinity|-",
            "OK|x|1.0Inf|-",
            "ERROR|",
            "ERROR|DESCONOCIDO|algo",
    })
    void respuestasMalFormadasSonInterno(String salida) {
        ResultadoIntegral r = parser.parsear(salida);
        assertFalse(r.exito());
        assertEquals(ResultadoIntegral.INTERNO, r.codigoError());
    }

    @Test
    void nuloEsInterno() {
        assertEquals(ResultadoIntegral.INTERNO, parser.parsear(null).codigoError());
    }

    @Test
    void reconoceTodosLosCodigosDelContrato() {
        for (String codigo : new String[] {"SINTAXIS", "NO_SOPORTADA", "DOMINIO",
                "PROLOG_NO_DISPONIBLE", "TIEMPO_AGOTADO", "INTERNO"}) {
            assertEquals(codigo, parser.parsear("ERROR|" + codigo + "|d").codigoError());
        }
    }
}
