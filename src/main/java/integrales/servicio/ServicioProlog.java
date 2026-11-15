package integrales.servicio;

import integrales.modelo.ResultadoIntegral;
import integrales.modelo.SolicitudIntegral;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Resuelve integrales ejecutando {@code swipl <prolog.dir>/puente.pl "<expresion>" <a> <b>}
 * ({@code docs/contrato.md}, sección 5).
 *
 * <p>Los argumentos se pasan como lista (sin shell). La salida estándar se interpreta con
 * {@link ParserRespuesta}; la salida de error solo se usa para enriquecer los errores {@code INTERNO}.
 */
public final class ServicioProlog implements ServicioIntegrales {

    static final String PUENTE = "puente.pl";
    private static final int MAX_STDERR = 400;

    private final ConfiguracionProlog configuracion;
    private final NormalizadorExpresion normalizador = new NormalizadorExpresion();
    private final ParserRespuesta parser = new ParserRespuesta();

    public ServicioProlog(ConfiguracionProlog configuracion) {
        this.configuracion = configuracion;
    }

    @Override
    public ResultadoIntegral resolver(SolicitudIntegral solicitud) {
        try {
            return ejecutar(solicitud);
        } catch (RuntimeException e) {
            return ResultadoIntegral.error(ResultadoIntegral.INTERNO, "Error inesperado en Java: " + e);
        }
    }

    private ResultadoIntegral ejecutar(SolicitudIntegral solicitud) {
        if (solicitud == null || solicitud.expresion() == null) {
            return ResultadoIntegral.error(ResultadoIntegral.INTERNO, "Solicitud vacía");
        }
        Optional<Path> swipl = configuracion.swipl();
        if (swipl.isEmpty()) {
            return ResultadoIntegral.error(ResultadoIntegral.PROLOG_NO_DISPONIBLE,
                    "No se encontró swipl. Instale SWI-Prolog o indique su ruta en "
                            + ConfiguracionProlog.ARCHIVO + " (swipl.ruta).");
        }
        Path directorio = configuracion.directorioProlog();
        Path puente = directorio.resolve(PUENTE).toAbsolutePath();
        if (!Files.isRegularFile(puente)) {
            return ResultadoIntegral.error(ResultadoIntegral.INTERNO, "No se encontró " + puente);
        }

        List<String> comando = List.of(
                swipl.get().toString(),
                puente.toString(),
                normalizador.normalizar(solicitud.expresion()),
                numero(solicitud.a()),
                numero(solicitud.b()));

        Process proceso;
        try {
            proceso = new ProcessBuilder(comando).directory(directorio.toFile()).start();
        } catch (IOException e) {
            return ResultadoIntegral.error(ResultadoIntegral.PROLOG_NO_DISPONIBLE,
                    "No se pudo ejecutar " + swipl.get() + ": " + e.getMessage());
        }
        return esperar(proceso);
    }

    private ResultadoIntegral esperar(Process proceso) {
        FutureTask<String> salida = leerEnSegundoPlano(proceso.getInputStream());
        FutureTask<String> errores = leerEnSegundoPlano(proceso.getErrorStream());
        try {
            proceso.getOutputStream().close();
            if (!proceso.waitFor(configuracion.tiempoMaximoMs(), TimeUnit.MILLISECONDS)) {
                proceso.destroyForcibly();
                return ResultadoIntegral.error(ResultadoIntegral.TIEMPO_AGOTADO,
                        "Prolog tardó más de " + configuracion.tiempoMaximoMs() / 1000.0 + " s");
            }
            ResultadoIntegral resultado = parser.parsear(salida.get(1, TimeUnit.SECONDS));
            if (ResultadoIntegral.INTERNO.equals(resultado.codigoError())) {
                return conErrores(resultado, errores);
            }
            return resultado;
        } catch (InterruptedException e) {
            proceso.destroyForcibly();
            Thread.currentThread().interrupt();
            return ResultadoIntegral.error(ResultadoIntegral.INTERNO, "Consulta interrumpida");
        } catch (IOException | ExecutionException | TimeoutException e) {
            proceso.destroyForcibly();
            return ResultadoIntegral.error(ResultadoIntegral.INTERNO,
                    "No se pudo leer la respuesta de Prolog: " + e.getMessage());
        }
    }

    /** Agrega al detalle el final de la salida de error de Prolog, si lo hay. */
    private static ResultadoIntegral conErrores(ResultadoIntegral resultado, FutureTask<String> errores) {
        String texto;
        try {
            texto = errores.get(1, TimeUnit.SECONDS).strip();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return resultado;
        } catch (ExecutionException | TimeoutException e) {
            return resultado;
        }
        if (texto.isEmpty()) {
            return resultado;
        }
        if (texto.length() > MAX_STDERR) {
            texto = "…" + texto.substring(texto.length() - MAX_STDERR);
        }
        return ResultadoIntegral.error(ResultadoIntegral.INTERNO,
                resultado.detalle() + ". Salida de error de Prolog: " + texto);
    }

    private static FutureTask<String> leerEnSegundoPlano(InputStream flujo) {
        FutureTask<String> tarea = new FutureTask<>(() -> {
            try (InputStream f = flujo) {
                return new String(f.readAllBytes(), StandardCharsets.UTF_8);
            }
        });
        Thread hilo = new Thread(tarea, "lector-swipl");
        hilo.setDaemon(true);
        hilo.start();
        return tarea;
    }

    /** Número con punto decimal y sin notación científica, independiente del {@code Locale}. */
    static String numero(double valor) {
        return BigDecimal.valueOf(valor).toPlainString();
    }
}
