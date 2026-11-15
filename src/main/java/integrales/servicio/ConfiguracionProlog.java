package integrales.servicio;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Configuración para ejecutar SWI-Prolog, leída de {@code config.properties}.
 *
 * <ul>
 *   <li>{@code swipl.ruta}: ruta del ejecutable. Si está vacía, se busca {@code swipl} en el
 *       {@code PATH} y en las rutas de instalación típicas.</li>
 *   <li>{@code prolog.dir}: carpeta que contiene {@code puente.pl}. Por defecto {@code base_conocimiento}.</li>
 *   <li>{@code timeout.ms}: tiempo máximo de una consulta. Por defecto 5000.</li>
 * </ul>
 *
 * <p>{@code config.properties} y las rutas relativas se buscan primero en el directorio actual y
 * después en la carpeta del {@code .jar}. Si no existe el archivo, se usan los valores por defecto.
 */
public final class ConfiguracionProlog {

    public static final String ARCHIVO = "config.properties";
    static final String CLAVE_SWIPL = "swipl.ruta";
    static final String CLAVE_DIRECTORIO = "prolog.dir";
    static final String CLAVE_TIEMPO = "timeout.ms";
    static final String DIRECTORIO_POR_DEFECTO = "base_conocimiento";
    static final long TIEMPO_POR_DEFECTO_MS = 5000;

    private static final List<String> RUTAS_WINDOWS = List.of(
            "C:\\Program Files\\swipl\\bin\\swipl.exe",
            "C:\\Program Files (x86)\\swipl\\bin\\swipl.exe");
    private static final List<String> RUTAS_UNIX = List.of(
            "/usr/bin/swipl",
            "/usr/local/bin/swipl",
            "/opt/homebrew/bin/swipl",
            "/Applications/SWI-Prolog.app/Contents/MacOS/swipl");

    private final Path swipl;
    private final Path directorioProlog;
    private final long tiempoMaximoMs;

    /** @param swipl ejecutable de SWI-Prolog, o {@code null} si no se encontró */
    public ConfiguracionProlog(Path swipl, Path directorioProlog, long tiempoMaximoMs) {
        this.swipl = swipl;
        this.directorioProlog = directorioProlog;
        this.tiempoMaximoMs = tiempoMaximoMs;
    }

    /** Lee {@code config.properties} (si existe) y completa lo que falte con valores por defecto. */
    public static ConfiguracionProlog cargar() {
        List<Path> bases = directoriosBase();
        Properties propiedades = new Properties();
        for (Path base : bases) {
            Path archivo = base.resolve(ARCHIVO);
            if (Files.isRegularFile(archivo)) {
                try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                    propiedades.load(lector);
                } catch (IOException e) {
                    System.err.println("No se pudo leer " + archivo + ": " + e.getMessage());
                }
                break;
            }
        }
        return desde(propiedades, bases, System.getenv("PATH"), esWindows());
    }

    static ConfiguracionProlog desde(Properties propiedades, List<Path> bases, String path, boolean windows) {
        String ruta = propiedades.getProperty(CLAVE_SWIPL, "").strip();
        Path swipl = ruta.isEmpty() ? buscarSwipl(path, windows).orElse(null) : aRuta(ruta);

        String dir = propiedades.getProperty(CLAVE_DIRECTORIO, "").strip();
        Path directorio = resolverDirectorio(dir.isEmpty() ? DIRECTORIO_POR_DEFECTO : dir, bases);

        return new ConfiguracionProlog(swipl, directorio, leerTiempo(propiedades.getProperty(CLAVE_TIEMPO)));
    }

    /** Busca el ejecutable en cada carpeta del {@code PATH} y luego en las rutas típicas. */
    static Optional<Path> buscarSwipl(String path, boolean windows) {
        String nombre = windows ? "swipl.exe" : "swipl";
        List<Path> candidatos = new ArrayList<>();
        if (path != null) {
            for (String carpeta : path.split(windows ? ";" : ":")) {
                String limpia = carpeta.strip().replace("\"", "");
                if (!limpia.isEmpty()) {
                    Path p = aRuta(limpia);
                    if (p != null) {
                        candidatos.add(p.resolve(nombre));
                    }
                }
            }
        }
        for (String tipica : windows ? RUTAS_WINDOWS : RUTAS_UNIX) {
            candidatos.add(aRuta(tipica));
        }
        return candidatos.stream()
                .filter(p -> p != null && Files.isRegularFile(p) && (windows || Files.isExecutable(p)))
                .findFirst();
    }

    private static Path resolverDirectorio(String valor, List<Path> bases) {
        Path dir = aRuta(valor);
        if (dir == null) {
            dir = Path.of(DIRECTORIO_POR_DEFECTO);
        }
        if (dir.isAbsolute()) {
            return dir;
        }
        for (Path base : bases) {
            if (Files.isDirectory(base.resolve(dir))) {
                return base.resolve(dir).normalize();
            }
        }
        return bases.get(0).resolve(dir).normalize();
    }

    private static long leerTiempo(String valor) {
        try {
            long ms = Long.parseLong(valor == null ? "" : valor.strip());
            return ms > 0 ? ms : TIEMPO_POR_DEFECTO_MS;
        } catch (NumberFormatException e) {
            return TIEMPO_POR_DEFECTO_MS;
        }
    }

    /** Directorio actual y, si es distinto, la carpeta que contiene el {@code .jar}. */
    private static List<Path> directoriosBase() {
        List<Path> bases = new ArrayList<>();
        Path actual = Path.of("").toAbsolutePath();
        bases.add(actual);
        try {
            Path codigo = Path.of(ConfiguracionProlog.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path carpetaJar = Files.isRegularFile(codigo) ? codigo.getParent() : null;
            if (carpetaJar != null && !carpetaJar.equals(actual)) {
                bases.add(carpetaJar);
            }
        } catch (URISyntaxException | RuntimeException e) {
            // Sin ubicación del código: solo se usa el directorio actual.
        }
        return bases;
    }

    private static Path aRuta(String texto) {
        try {
            return Path.of(texto);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private static boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** Ejecutable de SWI-Prolog, si se configuró o se encontró. */
    public Optional<Path> swipl() {
        return Optional.ofNullable(swipl);
    }

    public Path directorioProlog() {
        return directorioProlog;
    }

    public long tiempoMaximoMs() {
        return tiempoMaximoMs;
    }
}
