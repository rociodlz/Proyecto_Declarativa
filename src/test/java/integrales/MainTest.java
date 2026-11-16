package integrales;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import integrales.servicio.ConfiguracionProlog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @TempDir
    Path tmp;

    @Test
    void avisaDelModoSimulado() {
        assertTrue(Main.avisoInicial(null).contains("--mock"));
    }

    @Test
    void avisaSiNoHaySwipl() {
        String aviso = Main.avisoInicial(new ConfiguracionProlog(null, tmp, 5000));
        assertTrue(aviso.contains("swipl.ruta"), aviso);
    }

    @Test
    void avisaDelPrimerArchivoQueFalta() throws IOException {
        Path swipl = Files.createFile(tmp.resolve("swipl"));
        Files.createFile(tmp.resolve("puente.pl"));
        Files.createFile(tmp.resolve("bc_integrales.pl"));
        String aviso = Main.avisoInicial(new ConfiguracionProlog(swipl, tmp, 5000));
        assertTrue(aviso.contains("bc_evaluacion.pl"), aviso);
    }

    @Test
    void sinAvisoSiTodoEstaInstalado() throws IOException {
        Path swipl = Files.createFile(tmp.resolve("swipl"));
        for (String archivo : Main.ARCHIVOS_PROLOG) {
            Files.createFile(tmp.resolve(archivo));
        }
        assertNull(Main.avisoInicial(new ConfiguracionProlog(swipl, tmp, 5000)));
    }
}
