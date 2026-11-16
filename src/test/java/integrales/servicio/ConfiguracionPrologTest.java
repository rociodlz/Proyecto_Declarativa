package integrales.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class ConfiguracionPrologTest {

    @TempDir
    Path tmp;

    @Test
    void valoresPorDefecto() {
        ConfiguracionProlog c = ConfiguracionProlog.desde(new Properties(), List.of(tmp), "", false);
        assertEquals(5000, c.tiempoMaximoMs());
        assertEquals(tmp.resolve("base_conocimiento"), c.directorioProlog());
    }

    @Test
    void rutaExplicitaSeRespetaAunqueNoExista() {
        Properties p = new Properties();
        p.setProperty("swipl.ruta", "/no/existe/swipl");
        p.setProperty("timeout.ms", "1234");
        ConfiguracionProlog c = ConfiguracionProlog.desde(p, List.of(tmp), "", false);
        assertEquals(Path.of("/no/existe/swipl"), c.swipl().orElseThrow());
        assertEquals(1234, c.tiempoMaximoMs());
    }

    @Test
    void tiempoInvalidoUsaElValorPorDefecto() {
        Properties p = new Properties();
        p.setProperty("timeout.ms", "abc");
        assertEquals(5000, ConfiguracionProlog.desde(p, List.of(tmp), "", false).tiempoMaximoMs());
        p.setProperty("timeout.ms", "-1");
        assertEquals(5000, ConfiguracionProlog.desde(p, List.of(tmp), "", false).tiempoMaximoMs());
    }

    @Test
    void directorioRelativoSeBuscaEnCadaBase() throws IOException {
        Path primera = Files.createDirectory(tmp.resolve("actual"));
        Path segunda = Files.createDirectory(tmp.resolve("jar"));
        Files.createDirectory(segunda.resolve("base_conocimiento"));
        ConfiguracionProlog c = ConfiguracionProlog.desde(new Properties(), List.of(primera, segunda), "", false);
        assertEquals(segunda.resolve("base_conocimiento"), c.directorioProlog());
    }

    @Test
    void encuentraSwiplEnElPath() throws IOException {
        Path vacia = Files.createDirectory(tmp.resolve("vacia"));
        Path bin = Files.createDirectory(tmp.resolve("bin"));
        // Se usa el sistema operativo real: en Windows las rutas llevan "C:", así que
        // el separador debe ser File.pathSeparator (";") y el ejecutable swipl.exe.
        boolean windows = OS.WINDOWS.isCurrentOs();
        Path swipl = Files.createFile(bin.resolve(windows ? "swipl.exe" : "swipl"));
        if (!windows) {
            assertTrue(swipl.toFile().setExecutable(true));
        }
        String path = vacia + File.pathSeparator + bin;
        assertEquals(swipl, ConfiguracionProlog.buscarSwipl(path, windows).orElseThrow());
    }

    @Test
    void enWindowsBuscaSwiplExe() throws IOException {
        Path bin = Files.createDirectory(tmp.resolve("bin"));
        Files.createFile(bin.resolve("swipl"));
        Path exe = Files.createFile(bin.resolve("swipl.exe"));
        assertEquals(exe, ConfiguracionProlog.buscarSwipl("\"" + bin + "\"", true).orElseThrow());
    }

    @Test
    void ignoraCarpetasDelPathSinSwipl() {
        assertFalse(ConfiguracionProlog.buscarSwipl(tmp.toString(), true)
                .filter(p -> p.startsWith(tmp)).isPresent());
    }
}
