package integrales.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import integrales.modelo.ResultadoIntegral;
import integrales.modelo.SolicitudIntegral;
import org.junit.jupiter.api.Test;

class ServicioMockTest {

    private final ServicioMock mock = new ServicioMock(50);

    @Test
    void exitoPorDefecto() {
        ResultadoIntegral r = mock.resolver(new SolicitudIntegral("3*x^2", 0, 2));
        assertTrue(r.exito());
        assertEquals("x^3", r.primitiva());
        assertEquals(8.0, r.integralDefinida(), 1e-9);
        assertEquals(8.0, r.areaGeometrica(), 1e-9);
    }

    @Test
    void limitesInvertidosDanNegativo() {
        assertEquals(-8.0, mock.resolver(new SolicitudIntegral("3x^2", 2, 0)).integralDefinida(), 1e-9);
    }

    @Test
    void noSoportada() {
        assertEquals(ResultadoIntegral.NO_SOPORTADA,
                mock.resolver(new SolicitudIntegral("x * sin(x)", 0, 1)).codigoError());
    }

    @Test
    void dominioSoloSiCeroEstaEnElIntervalo() {
        assertEquals(ResultadoIntegral.DOMINIO,
                mock.resolver(new SolicitudIntegral("x^-1", -1, 1)).codigoError());
        ResultadoIntegral r = mock.resolver(new SolicitudIntegral("x^(-1)", 1, 2));
        assertTrue(r.exito());
        assertEquals(Math.log(2), r.integralDefinida(), 1e-9);
    }

    @Test
    void lentoTardaYNoTraeAreaGeometrica() {
        long inicio = System.nanoTime();
        ResultadoIntegral r = mock.resolver(new SolicitudIntegral("x^5", 0, 1));
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        assertTrue(ms >= 50, "debería tardar al menos 50 ms, tardó " + ms);
        assertEquals(1.0 / 6, r.integralDefinida(), 1e-9);
        assertNull(r.areaGeometrica());
    }
}
