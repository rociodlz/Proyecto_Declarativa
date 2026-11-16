package integrales.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import integrales.modelo.ResultadoIntegral;
import integrales.modelo.SolicitudIntegral;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Casos de {@code docs/casos_prueba.md} contra SWI-Prolog real.
 *
 * <p>Se omiten (no fallan) si no hay {@code swipl} o si falta algún archivo de la base de
 * conocimiento, para que {@code mvnw clean verify} funcione en cualquier máquina (contrato, sección 10).
 * Se comparan la integral definida y el área geométrica; no la forma exacta de la primitiva, que
 * depende de la simplificación.
 */
class IntegracionPrologTest {

    private static final double TOLERANCIA = 1e-6;
    private static ConfiguracionProlog config;
    private static ServicioProlog servicio;

    @TempDir
    Path tmp;

    @BeforeAll
    static void requierePrologYBase() {
        config = ConfiguracionProlog.cargar();
        assumeTrue(config.swipl().isPresent(), "swipl no está instalado");
        assumeTrue(config.archivoFaltante().isEmpty(),
                () -> "falta " + config.archivoFaltante().orElse("") + " en " + config.directorioProlog());
        servicio = new ServicioProlog(config);
    }

    private static ResultadoIntegral resolver(String f, double a, double b) {
        return servicio.resolver(new SolicitudIntegral(f, a, b));
    }

    // Secciones 1 y 2 de casos_prueba.md: tipos del PDF oficial y límites especiales.
    @ParameterizedTest(name = "#{0}: ∫ {1} de {2} a {3} = {4}")
    @CsvSource(delimiter = ';', value = {
            "1;  5;        0;  2;        10.000000; 10.000000",
            "2;  x;        0;  2;        2.000000;  2.000000",
            "3;  3*x;      1;  3;        12.000000; 12.000000",
            "4;  x^3;      -1; 2;        3.750000;  4.250000",
            "5;  3*x^2;    0;  2;        8.000000;  8.000000",
            "6;  x^(-1);   1;  2;        0.693147;  0.693147",
            "7;  x^(-2);   1;  2;        0.500000;  0.500000",
            "8;  x^2+x;    0;  1;        0.833333;  0.833333",
            "9;  x^2-x;    0;  2;        0.666667;  1.000000",
            "10; 2^x;      0;  3;        10.098865; 10.098865",
            "11; sin(x);   0;  3.141593; 2.000000;  2.000000",
            "12; sin(x);   0;  6.283185; 0.000000;  4.000000",
            "13; cos(x);   0;  1.570796; 1.000000;  1.000000",
            "14; tan(x);   0;  1;        0.615626;  0.615626",
            "15; asin(x);  0;  1;        0.570796;  0.570796",
            "16; acos(x);  0;  1;        1.000000;  1.000000",
            "17; atan(x);  0;  1;        0.438825;  0.438825",
            "18; sinh(x);  0;  1;        0.543081;  0.543081",
            "19; cosh(x);  0;  1;        1.175201;  1.175201",
            "20; tanh(x);  0;  1;        0.433781;  0.433781",
            "21; asinh(x); 0;  1;        0.467160;  0.467160",
            "22; acosh(x); 1;  2;        0.901865;  0.901865",
            "23; atanh(x); 0;  0.5;      0.130812;  0.130812",
            "24; x;        1;  1;        0.000000;  0.000000",
            "25; x;        2;  0;        -2.000000; 2.000000",
            // Sección 2.1: área geométrica.
            "79; x^(-1);   -2; -1;       -0.693147; 0.693147",
            "80; asin(x);  -1; 1;        0.000000;  1.141593",
            "81; acos(x);  -1; 1;        3.141593;  3.141593",
            "82; x^3-x;    -2; 2;        0.000000;  5.000000",
            "83; x^3;      2;  -1;       -3.750000; 4.250000",
    })
    void integralDefinida(String caso, String f, double a, double b, double esperado, double areaEsperada) {
        ResultadoIntegral r = resolver(f, a, b);
        assertTrue(r.exito(), "caso " + caso + ": " + r);
        assertEquals(esperado, r.integralDefinida(), TOLERANCIA, "caso " + caso + ", primitiva " + r.primitiva());
        assertNotNull(r.areaGeometrica(), "caso " + caso + ": falta el área geométrica");
        assertEquals(areaEsperada, r.areaGeometrica(), TOLERANCIA, "caso " + caso + ", área geométrica");
    }

    @Test
    void integralYAreaPuedenDiferir() {
        ResultadoIntegral r = resolver("sin(x)", 0, 2 * Math.PI);
        assertEquals(0, r.integralDefinida(), TOLERANCIA);
        assertEquals(4, r.areaGeometrica(), TOLERANCIA);
        assertNotEquals(r.integralDefinida(), r.areaGeometrica());
    }

    // Sección 2.1, regresión A1: en intervalos largos el área no puede omitir cambios de signo.
    @ParameterizedTest(name = "#{0}: ∫ |{1}| de {2} a {3} = {5}")
    @CsvSource(delimiter = ';', value = {
            "84; sin(x);     0;  7000; 0.137986565705;   4456.137986565705",
            "85; cos(x);     0;  7000; 0.506885429949;   4456.506885429949",
            "86; sin(x)+0.5; 0;  10;   6.839071529076;   8.211062527604",
            "87; sin(x)+0.5; 0;  1000; 500.437620923709; 718.220956450095",
            "88; x^20-1;     -2; 2;    199724.761904761; 199728.571428571",
    })
    void areaEnIntervalosLargos(int caso, String f, double a, double b, double integral, double area) {
        ResultadoIntegral r = resolver(f, a, b);
        assertTrue(r.exito(), "caso " + caso + ": " + r);
        assertEquals(integral, r.integralDefinida(), 1e-6 * Math.max(1, Math.abs(integral)));
        assertNotNull(r.areaGeometrica(), "falta el área geométrica");
        assertEquals(area, r.areaGeometrica(), 1e-6 * Math.max(1, area));
    }

    // Regresión A1: si los cambios de signo no se pueden obtener con confianza, el área queda
    // como no disponible en vez de devolver un valor incorrecto.
    // También se compara la integral definida, que sí se informa.
    @ParameterizedTest(name = "#{0}: ∫ |{1}| de {2} a {3}: área no disponible, integral {4}")
    @CsvSource(delimiter = ';', value = {
            "89; sin(x)+0.5; 0; 100000;  50001.999361",
            "90; sin(x);     0; 1000000; 0.063248",
    })
    void areaNoConfiableNoSeInforma(int caso, String f, double a, double b, double integral) {
        ResultadoIntegral r = resolver(f, a, b);
        assertTrue(r.exito(), "caso " + caso + ": " + r);
        assertNull(r.areaGeometrica(), "área no confiable: " + r.areaGeometrica());
        assertEquals(integral, r.integralDefinida(), 1e-6 * Math.max(1, Math.abs(integral)), "caso " + caso);
    }

    // Sección 5.1: entradas que acepta la validación y transforma el normalizador.
    @ParameterizedTest(name = "#{0}: ∫ {1} de {2} a {3} = {4}")
    @CsvSource(delimiter = ';', value = {
            "55; 3x^2;        0; 2;        8.000000",
            "56; x+-1;        0; 1;        -0.500000",
            "57; x^-1;        1; 2;        0.693147",
            "59; Sen(X);      0; 3.141593; 2.000000",
            "60; arcsinh(x);  0; 1;        0.467160",
            "—;  ' 3 X ^ 2 '; 0; 2;        8.000000",
    })
    void entradaNormalizada(String caso, String f, double a, double b, double esperado) {
        ResultadoIntegral r = resolver(f, a, b);
        assertTrue(r.exito(), "caso " + caso + ": " + r);
        assertEquals(esperado, r.integralDefinida(), TOLERANCIA, "caso " + caso);
    }

    // Sección 3: dominio y singularidades.
    @ParameterizedTest(name = "#{0}: ∫ {1} de {2} a {3} → DOMINIO")
    @CsvSource(delimiter = ';', value = {
            "26; x^(-1);   -1; 1",
            "27; x^(-2);   -1; 1",
            "28; tan(x);   0;  2",
            "29; asin(x);  0;  2",
            "30; acosh(x); 0;  2",
            "31; atanh(x); 0;  1",
            // Regresión A2: singularidades que el muestreo no ve.
            "91; tan(x);   0;        12566.370614",
            "92; tan(x);   1.570796; 2",
            "93; x^(-1);   0;        1",
            "94; x^(-2);   0;        1",
            "95; atanh(x); -1;       0",
    })
    void dominio(int caso, String f, double a, double b) {
        ResultadoIntegral r = resolver(f, a, b);
        assertEquals(ResultadoIntegral.DOMINIO, r.codigoError(), "caso " + caso + ": " + r);
    }

    // Casos 31, 93, 94 y 95: una singularidad en un extremo se informa como función no definida,
    // no como un valor demasiado grande.
    @ParameterizedTest(name = "#{0}: ∫ {1} de {2} a {3} → DOMINIO por función no definida")
    @CsvSource(delimiter = ';', value = {
            "31; atanh(x); 0;  1",
            "93; x^(-1);   0;  1",
            "94; x^(-2);   0;  1",
            "95; atanh(x); -1; 0",
    })
    void singularidadEnUnExtremoNoEsDesbordamiento(int caso, String f, double a, double b) {
        ResultadoIntegral r = resolver(f, a, b);
        assertEquals(ResultadoIntegral.DOMINIO, r.codigoError(), "caso " + caso + ": " + r);
        assertTrue(r.detalle().contains("no está definida"), "caso " + caso + ": " + r.detalle());
        assertFalse(r.detalle().contains("demasiado grande"), "caso " + caso + ": " + r.detalle());
    }

    // Un desbordamiento numérico auténtico se sigue distinguiendo de una función no definida.
    @Test
    void desbordamientoNumericoSeIndicaComoTal() {
        ResultadoIntegral r = resolver("2^x", 0, 1_000_000);
        assertEquals(ResultadoIntegral.DOMINIO, r.codigoError(), r.toString());
        assertTrue(r.detalle().contains("demasiado grande"), r.detalle());
        assertFalse(r.detalle().contains("no está definida"), r.detalle());
    }

    // Sección 4 (casos 32-36) y funciones que se escriben pero no se pueden integrar.
    @ParameterizedTest(name = "{0}: ∫ {1} → NO_SOPORTADA")
    @CsvSource(delimiter = ';', value = {
            "32;   e^x",
            "33;   x*3",
            "34;   -x",
            "35;   x/2",
            "36;   sin(2*x)",
            "S10;  exp(x)",
            "S11;  ln(x)",
            "S12;  log(x)",
            "S13;  e",
            "M4;   x*sin(x)",
    })
    void integralNoSoportada(String caso, String f) {
        ResultadoIntegral r = resolver(f, 1, 2);
        assertEquals(ResultadoIntegral.NO_SOPORTADA, r.codigoError(), "caso " + caso + ": " + r);
    }

    // Caso 50: con un tiempo máximo mínimo la consulta real a SWI-Prolog se corta.
    @Test
    void consultaQueTardaDemasiadoEsTiempoAgotado() {
        ServicioProlog impaciente = new ServicioProlog(
                new ConfiguracionProlog(config.swipl().orElseThrow(), config.directorioProlog(), 1));
        ResultadoIntegral r = impaciente.resolver(new SolicitudIntegral("3*x^2", 0, 2));
        assertEquals(ResultadoIntegral.TIEMPO_AGOTADO, r.codigoError(), r.toString());
    }

    // Simplificación de la primitiva: la forma devuelta y el valor de la integral.
    @ParameterizedTest(name = "∫ {0} = {1}")
    @CsvSource(delimiter = ';', value = {
            "3*x^2;         x^3;                0; 2; 8.000000",
            "x^(-2);        -1/x;               1; 2; 0.500000",
            "2*(x+1);       x^2+2*x;            0; 2; 8.000000",
            "x^3-x;         x^4/4-x^2/2;        -2; 2; 0.000000",
            "3*x;           3*x^2/2;            1; 3; 12.000000",
            "x+-1;          x^2/2-x;            0; 1; -0.500000",
            "3*sin(x);      -3*cos(x);          0; 3.141593; 6.000000",
            "0.5*x;         0.5*x^2/2;          0; 2; 1.000000",
            // Coeficientes decimales: se conserva la fracción, sin aproximaciones como 0.1666…
            "0.5*x^2;       0.5*x^3/3;          0; 1; 0.166667",
            "1.1*x^2;       1.1*x^3/3;          0; 1; 0.366667",
            "0.7*x^(-3);    -0.7/(2*x^2);       1; 2; 0.262500",
            "0.3*x^(-2);    -0.3/x;             1; 2; 0.150000",
            "2.5*(x+1);     2.5*x^2/2+2.5*x;    0; 1; 3.750000",
            "x^(-3);        -1/(2*x^2);         1; 2; 0.375000",
            "2*x^(-2);      -2/x;               1; 2; 1.000000",
            "x^2-2*x+1;     x^3/3-x^2+x;        0; 2; 0.666667",
            "x^(-1);        ln(abs(x));         1; 2; 0.693147",
            "asin(x);       x*asin(x)+raiz(1-x^2,2); 0; 1; 0.570796",
    })
    void primitivaSimplificada(String f, String primitiva, double a, double b, double integral) {
        ResultadoIntegral r = resolver(f, a, b);
        assertTrue(r.exito(), f + ": " + r);
        assertEquals(primitiva, r.primitiva(), f);
        assertEquals(integral, r.integralDefinida(), TOLERANCIA, f);
    }

    // Sección 4, caso 37: las funciones simbólicas nunca se resuelven.
    @Test
    void funcionSimbolicaNoSoportada() {
        assertEquals(ResultadoIntegral.NO_SOPORTADA, resolver("u(x)", 0, 1).codigoError());
    }

    @Test
    void expresionIlegibleEsSintaxis() {
        assertEquals(ResultadoIntegral.SINTAXIS, resolver("3*x^", 0, 1).codigoError());
    }

    // Regresión A3: una base de conocimiento incompleta es INTERNO, nunca NO_SOPORTADA.

    /** Copia los archivos de Prolog a una carpeta temporal y devuelve un servicio que la usa. */
    private ServicioProlog copiaDeLaBase() throws IOException {
        for (String archivo : ConfiguracionProlog.ARCHIVOS_PROLOG) {
            Files.copy(config.directorioProlog().resolve(archivo), tmp.resolve(archivo),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        return new ServicioProlog(new ConfiguracionProlog(config.swipl().orElseThrow(), tmp, 10_000));
    }

    @Test   // caso 96
    void faltaUnArchivoDeLaBase() throws IOException {
        ServicioProlog copia = copiaDeLaBase();
        Files.delete(tmp.resolve("bc_derivadas.pl"));
        ResultadoIntegral r = copia.resolver(new SolicitudIntegral("3*x^2", 0, 2));
        assertEquals(ResultadoIntegral.INTERNO, r.codigoError(), r.toString());
        assertTrue(r.detalle().contains("bc_derivadas.pl"), r.detalle());
    }

    @Test   // caso 97
    void faltaUnPredicadoDeLaBase() throws IOException {
        ServicioProlog copia = copiaDeLaBase();
        Files.writeString(tmp.resolve("bc_simplificaciones.pl"), "");
        ResultadoIntegral r = copia.resolver(new SolicitudIntegral("3*x^2", 0, 2));
        assertEquals(ResultadoIntegral.INTERNO, r.codigoError(), r.toString());
        assertTrue(r.detalle().contains("simplificar"), r.detalle());
    }

    @Test   // caso 98
    void errorDeSintaxisEnLaBase() throws IOException {
        ServicioProlog copia = copiaDeLaBase();
        Files.writeString(tmp.resolve("bc_derivadas.pl"), "roto(\n", StandardOpenOption.APPEND);
        ResultadoIntegral r = copia.resolver(new SolicitudIntegral("3*x^2", 0, 2));
        assertEquals(ResultadoIntegral.INTERNO, r.codigoError(), r.toString());
        assertTrue(r.detalle().contains("cargar"), r.detalle());
    }
}
