package integrales;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import integrales.modelo.ResultadoIntegral;
import integrales.modelo.SolicitudIntegral;
import integrales.servicio.ConfiguracionProlog;
import integrales.servicio.ServicioProlog;
import integrales.ui.FormateadorResultado;
import integrales.validacion.EntradaInvalidaException;
import integrales.validacion.EntradaInvalidaException.Campo;
import integrales.validacion.ValidadorEntrada;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/**
 * Plan de pruebas completo (secciones A a G), ejecutado por el mismo camino que la ventana:
 * {@link ValidadorEntrada} → {@link ServicioProlog} (normalizador, SWI-Prolog) → {@link FormateadorResultado}.
 *
 * <p>Los casos están en {@code src/test/resources/plan_de_pruebas.csv}:
 * <ul>
 *   <li>{@code esperado}: {@code OK}, {@code ND} (resultado con área «No disponible»), {@code DOMINIO},
 *       {@code NO_SOPORTADA} o {@code ENTRADA_<campo>} (la validación lo rechaza en ese campo).</li>
 *   <li>{@code integral} y {@code area}: valores de referencia calculados aparte, con integración numérica
 *       (no salen del sistema). Se comparan con una tolerancia relativa de 10⁻⁶.</li>
 *   <li>{@code primitiva}: lo que muestra la ventana. Si una mejora de la simplificación la cambia a propósito,
 *       se actualiza aquí.</li>
 *   <li>Las limitaciones conocidas fijan el comportamiento actual ({@code NO_SOPORTADA}): si se agrega una regla,
 *       la prueba avisa y hay que actualizar el caso con el valor de referencia.</li>
 * </ul>
 *
 * <p>Los casos de validación ({@code ENTRADA_*}) se ejecutan siempre. Los demás se omiten si no hay SWI-Prolog
 * o falta algún archivo de la base de conocimiento.
 */
class PlanDePruebasTest {

    private static final ValidadorEntrada VALIDADOR = new ValidadorEntrada();
    private static final FormateadorResultado FORMATEADOR = new FormateadorResultado();
    private static ServicioProlog servicio;

    @BeforeAll
    static void prepararServicio() {
        ConfiguracionProlog config = ConfiguracionProlog.cargar();
        if (config.swipl().isPresent() && config.archivoFaltante().isEmpty()) {
            servicio = new ServicioProlog(config);
        }
    }

    @ParameterizedTest(name = "{0}: {2} [{3}, {4}] → {5} ({1})")
    @CsvFileSource(resources = "/plan_de_pruebas.csv", delimiter = ';', numLinesToSkip = 1,
            encoding = "UTF-8", ignoreLeadingAndTrailingWhitespace = false)
    void caso(String id, String descripcion, String f, String a, String b, String esperado,
              String primitiva, Double integral, Double area) {
        String expresion;
        double limiteA;
        double limiteB;
        try {
            expresion = VALIDADOR.validarExpresion(f);
            limiteA = VALIDADOR.validarLimite(a, Campo.LIMITE_A);
            limiteB = VALIDADOR.validarLimite(b, Campo.LIMITE_B);
        } catch (EntradaInvalidaException e) {
            assertEquals(esperado, "ENTRADA_" + e.getCampo(), id + ": " + e.getMessage());
            return;
        }
        if (esperado.startsWith("ENTRADA_")) {
            fail(id + ": la validación debía rechazarlo (" + esperado + ")");
        }

        assumeTrue(servicio != null, "SWI-Prolog o la base de conocimiento no están disponibles");
        ResultadoIntegral r = servicio.resolver(new SolicitudIntegral(expresion, limiteA, limiteB));

        switch (esperado) {
            case "OK", "ND" -> {
                assertTrue(r.exito(), id + ": se esperaba un resultado y se obtuvo " + r);
                assertEquals(primitiva, FORMATEADOR.formatearPrimitiva(r.primitiva()), id + ": primitiva");
                assertCerca(id + ": integral definida", integral, r.integralDefinida());
                if (esperado.equals("ND")) {
                    assertNull(r.areaGeometrica(), id + ": el área debía quedar «No disponible»");
                } else {
                    assertNotNull(r.areaGeometrica(), id + ": falta el área geométrica");
                    assertCerca(id + ": área geométrica", area, r.areaGeometrica());
                }
            }
            default -> assertEquals(esperado, r.codigoError(), id + ": " + r);
        }
    }

    private static void assertCerca(String mensaje, double esperado, double obtenido) {
        assertEquals(esperado, obtenido, Math.max(1e-6, 1e-6 * Math.abs(esperado)), mensaje);
    }
}
