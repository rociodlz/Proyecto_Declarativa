package integrales.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import integrales.modelo.ResultadoIntegral;
import integrales.modelo.SolicitudIntegral;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Prueba la ejecución del proceso con un {@code swipl} falso (un script de shell), sin necesitar
 * SWI-Prolog. Las pruebas con scripts se omiten en Windows.
 */
class ServicioPrologTest {

    @TempDir
    Path tmp;

    private Path directorio;

    @BeforeEach
    void crearPuente() throws IOException {
        directorio = Files.createDirectory(tmp.resolve("base_conocimiento"));
        Files.writeString(directorio.resolve("puente.pl"), "% falso\n");
    }

    private ResultadoIntegral resolver(Path swipl, long tiempoMs, String expresion) {
        ServicioProlog servicio = new ServicioProlog(new ConfiguracionProlog(swipl, directorio, tiempoMs));
        return servicio.resolver(new SolicitudIntegral(expresion, 0, 2));
    }

    /** Crea un {@code swipl} falso que ejecuta {@code cuerpo} en sh. */
    private Path swiplFalso(String cuerpo) throws IOException {
        assumeFalse(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"),
                "requiere sh");
        Path script = tmp.resolve("swipl");
        Files.writeString(script, "#!/bin/sh\n" + cuerpo + "\n");
        assertTrue(script.toFile().setExecutable(true));
        return script;
    }

    @Test
    void sinSwiplEsPrologNoDisponible() {
        ResultadoIntegral r = resolver(null, 5000, "x");
        assertEquals(ResultadoIntegral.PROLOG_NO_DISPONIBLE, r.codigoError());
    }

    @Test
    void swiplInexistenteEsPrologNoDisponible() {
        ResultadoIntegral r = resolver(tmp.resolve("no-existe"), 5000, "x");
        assertEquals(ResultadoIntegral.PROLOG_NO_DISPONIBLE, r.codigoError());
    }

    @Test
    void sinPuenteEsInterno() throws IOException {
        Files.delete(directorio.resolve("puente.pl"));
        ResultadoIntegral r = resolver(swiplFalso("echo 'OK|x|1|-'"), 5000, "x");
        assertEquals(ResultadoIntegral.INTERNO, r.codigoError());
    }

    @Test
    void pasaLosArgumentosComoListaYNormalizados() throws IOException {
        // Devuelve los argumentos recibidos dentro de la primitiva:
        // $1 = puente, $2 = "--", $3 = expresión, $4 = a, $5 = b.
        Path swipl = swiplFalso("echo 'Warning: ruido' >&2\necho \"OK|[$2][$3][$4][$5]|8.0|-\"");
        ResultadoIntegral r = resolver(swipl, 5000, "3 X ^ 2");
        assertTrue(r.exito(), String.valueOf(r));
        assertEquals("[--][3*x^2][0.0][2.0]", r.primitiva());
        assertEquals(8.0, r.integralDefinida());
    }

    @Test
    void respuestaDeErrorSeRespeta() throws IOException {
        ResultadoIntegral r = resolver(swiplFalso("echo 'ERROR|DOMINIO|singularidad'"), 5000, "x");
        assertEquals(ResultadoIntegral.DOMINIO, r.codigoError());
        assertEquals("singularidad", r.detalle());
    }

    @Test
    void tardarDemasiadoEsTiempoAgotado() throws IOException {
        long inicio = System.nanoTime();
        ResultadoIntegral r = resolver(swiplFalso("sleep 5\necho 'OK|x|1|-'"), 300, "x");
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        assertEquals(ResultadoIntegral.TIEMPO_AGOTADO, r.codigoError());
        assertTrue(ms < 3000, "no esperó al proceso: " + ms + " ms");
    }

    @Test
    void salidaVaciaEsInternoConLaSalidaDeError() throws IOException {
        ResultadoIntegral r = resolver(swiplFalso("echo 'algo se rompió' >&2\nexit 1"), 5000, "x");
        assertEquals(ResultadoIntegral.INTERNO, r.codigoError());
        assertTrue(r.detalle().contains("algo se rompió"), r.detalle());
    }

    @Test
    void numerosSinNotacionCientifica() {
        assertEquals("0.000010", ServicioProlog.numero(1e-5));
        assertEquals("1000000.0", ServicioProlog.numero(1e6));
        assertEquals("-1.5", ServicioProlog.numero(-1.5));
        assertEquals("2.0", ServicioProlog.numero(2));
    }
}
