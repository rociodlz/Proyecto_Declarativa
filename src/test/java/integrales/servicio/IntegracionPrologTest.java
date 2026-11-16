package integrales.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import integrales.modelo.ResultadoIntegral;
import integrales.modelo.SolicitudIntegral;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Casos de {@code docs/casos_prueba.md} contra SWI-Prolog real.
 *
 * <p>Se omiten (no fallan) si no hay {@code swipl} o si falta algún archivo de la base de
 * conocimiento, para que {@code mvnw clean verify} funcione en cualquier máquina (contrato, sección 10).
 * Solo se compara la integral definida: la forma exacta de la primitiva depende de la simplificación.
 */
class IntegracionPrologTest {

    private static final double TOLERANCIA = 1e-6;
    private static ServicioProlog servicio;

    @BeforeAll
    static void requierePrologYBase() {
        ConfiguracionProlog config = ConfiguracionProlog.cargar();
        assumeTrue(config.swipl().isPresent(), "swipl no está instalado");
        for (String archivo : new String[] {"puente.pl", "bc_integrales.pl", "bc_evaluacion.pl"}) {
            Path ruta = config.directorioProlog().resolve(archivo);
            assumeTrue(Files.isRegularFile(ruta), "falta " + ruta);
        }
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
            "25; x;        2;  0;        -2.000000;  2.000000",
    })
    void integralDefinida(int caso, String f, double a, double b, double esperado, double areaEsperada) {
        ResultadoIntegral r = resolver(f, a, b);
        assertTrue(r.exito(), "caso " + caso + ": " + r);
        assertEquals(esperado, r.integralDefinida(), TOLERANCIA, "caso " + caso + ", primitiva " + r.primitiva());
        // El área geométrica es opcional (contrato, sección 3): solo se comprueba si se calculó.
        if (r.areaGeometrica() != null) {
            assertEquals(areaEsperada, r.areaGeometrica(), TOLERANCIA, "caso " + caso + ", área geométrica");
        }
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
    })
    void dominio(int caso, String f, double a, double b) {
        ResultadoIntegral r = resolver(f, a, b);
        assertEquals(ResultadoIntegral.DOMINIO, r.codigoError(), "caso " + caso + ": " + r);
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
}
