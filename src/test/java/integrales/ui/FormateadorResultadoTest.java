package integrales.ui;

import integrales.ui.FormateadorResultado.MensajeError;
import integrales.ui.FormateadorResultado.ResultadoFormateado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormateadorResultadoTest {

    private final FormateadorResultado formateador = new FormateadorResultado();

    @Nested
    @DisplayName("Primitiva")
    class Primitiva {

        @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
        @CsvSource(delimiter = ';', value = {
                // Formas reales que devuelve la base de conocimiento de Henry (commit 1de3224)
                "x^3                                      ; x^3 + C",
                "-cos(x)                                  ; -cos(x) + C",
                "2^x/ln(2)                                ; 2^x/ln(2) + C",
                "x*asin(x)+raiz(1-x^2,2)                  ; x*asin(x)+sqrt(1-x^2) + C",
                "x*acos(x)-raiz(1-x^2,2)                  ; x*acos(x)-sqrt(1-x^2) + C",
                "x*asinh(x)-raiz(x^2+1,2)                 ; x*asinh(x)-sqrt(x^2+1) + C",
                "x*acosh(x)-raiz(x-1,2)*raiz(x+1,2)       ; x*acosh(x)-sqrt(x-1)*sqrt(x+1) + C",
                "ln(abs(x))                               ; ln(|x|) + C",
                "-ln(abs(cos(x)))                         ; -ln(|cos(x)|) + C",
                "x*atan(x)-ln(1+x^2)/2                    ; x*atan(x)-ln(1+x^2)/2 + C"})
        @DisplayName("Transforma raiz y abs de las primitivas reales")
        void primitivasReales(String prolog, String esperado) {
            assertEquals(esperado, formateador.formatearPrimitiva(prolog));
        }

        @Test
        @DisplayName("Conserva los espacios que escribe Prolog (no cambia el significado)")
        void conservaEspacios() {
            assertEquals("2* -cos(x) + C", formateador.formatearPrimitiva("2* -cos(x)"));
            assertEquals("x^ -1/ -1 + C", formateador.formatearPrimitiva("x^ -1/ -1"));
        }

        @Test
        @DisplayName("Quita solo los espacios exteriores")
        void quitaEspaciosExteriores() {
            assertEquals("x^2/2 + C", formateador.formatearPrimitiva("  x^2/2  "));
        }

        @Test
        @DisplayName("Raíces anidadas")
        void raicesAnidadas() {
            assertEquals("sqrt(sqrt(x)+1) + C", formateador.formatearPrimitiva("raiz(raiz(x,2)+1,2)"));
        }

        @Test
        @DisplayName("Raíz de índice N: se encierra completa entre paréntesis")
        void raizIndiceN() {
            assertEquals("(x^(1/3)) + C", formateador.formatearPrimitiva("raiz(x,3)"));
            assertEquals("((x+1)^(1/3))^2 + C", formateador.formatearPrimitiva("raiz(x+1,3)^2"));
        }

        @ParameterizedTest(name = "conserva \"{0}\"")
        @ValueSource(strings = {
                "raiz(x,n)",          // índice no numérico
                "raiz(x)",            // un solo argumento
                "raiz(x,2",           // paréntesis sin cerrar
                "abs(x,y)",           // dos argumentos
                "fabs(x)",            // otro nombre que contiene "abs"
                "xraiz(x,2)"})        // otro nombre que contiene "raiz"
        @DisplayName("Si la transformación no es segura, conserva el original")
        void conservaSiNoEsSeguro(String prolog) {
            assertEquals(prolog + " + C", formateador.formatearPrimitiva(prolog));
        }

        @Test
        @DisplayName("abs anidado: no se usan barras dobles ambiguas")
        void absAnidado() {
            assertEquals("abs(|x|-1) + C", formateador.formatearPrimitiva("abs(abs(x)-1)"));
        }

        @Test
        @DisplayName("Primitiva nula o vacía")
        void primitivaInvalida() {
            assertThrows(NullPointerException.class, () -> formateador.formatearPrimitiva(null));
            assertThrows(IllegalArgumentException.class, () -> formateador.formatearPrimitiva("  "));
        }
    }

    @Nested
    @DisplayName("Números")
    class Numeros {

        @ParameterizedTest(name = "{0} → \"{1}\"")
        @CsvSource({
                "8.0, 8",
                "-2.0, -2",
                "0.5, 0.5",
                "0.6931471805599453, 0.693147",
                "10.098865286222744, 10.098865",
                "0.6666666666666666, 0.666667",
                "0.3333333333333333, 0.333333",
                "0.0000006, 0.000001",
                "0.0000004, 0",
                "-0.0, 0",
                "-2.4492935982947064E-16, 0",
                "999999999.4, 999999999.4",
                "1234567890.5, 1.234568e+09",
                "2.5E12, 2.5e+12",
                "-3.0E10, -3e+10"})
        @DisplayName("Redondeo a 6 decimales y notación científica")
        void formato(double valor, String esperado) {
            assertEquals(esperado, formateador.formatearNumero(valor));
        }

        @ParameterizedTest(name = "rechaza {0}")
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        @DisplayName("Valores no finitos")
        void noFinitos(double valor) {
            assertThrows(IllegalArgumentException.class, () -> formateador.formatearNumero(valor));
        }
    }

    @Nested
    @DisplayName("Resultado completo")
    class Resultado {

        @Test
        @DisplayName("Caso 5: 3*x^2 en [0, 2]")
        void caso5() {
            ResultadoFormateado r = formateador.formatearExito("x^3", 8.0, 8.0, 0, 2);
            assertEquals("x^3 + C", r.primitiva());
            assertEquals("8", r.integralDefinida());
            assertEquals("8", r.areaGeometrica());
            assertNull(r.nota());
        }

        @Test
        @DisplayName("Caso 9: integral y área distintas")
        void caso9() {
            ResultadoFormateado r = formateador.formatearExito("x^3/3-x^2/2", 2.0 / 3, 1.0, 0, 2);
            assertEquals("0.666667", r.integralDefinida());
            assertEquals("1", r.areaGeometrica());
            assertNull(r.nota());
        }

        @Test
        @DisplayName("Área no calculada: 'No disponible', nunca un valor inventado")
        void areaNoDisponible() {
            ResultadoFormateado r = formateador.formatearExito("ln(abs(x))", 0.6931471805599453, null, 1, 2);
            assertEquals(FormateadorResultado.NO_DISPONIBLE, r.areaGeometrica());
            assertNotNull(r.nota());
            assertTrue(r.nota().contains("no está disponible"));
        }

        @Test
        @DisplayName("Caso 25: a > b")
        void limitesInvertidos() {
            ResultadoFormateado r = formateador.formatearExito("x^2/2", -2.0, 2.0, 2, 0);
            assertEquals("-2", r.integralDefinida());
            assertTrue(r.nota().contains("invertidos"));
        }

        @Test
        @DisplayName("a = b")
        void limitesIguales() {
            ResultadoFormateado r = formateador.formatearExito("x^2/2", 0.0, 0.0, 1, 1);
            assertEquals("0", r.integralDefinida());
            assertTrue(r.nota().contains("iguales"));
        }

        @Test
        @DisplayName("Integral negativa con a < b")
        void negativa() {
            ResultadoFormateado r = formateador.formatearExito("-cos(x)", -2.0, 2.0, Math.PI, 2 * Math.PI);
            assertTrue(r.nota().contains("negativa"));
        }

        @Test
        @DisplayName("Caso 12: un residuo numérico que se muestra como 0 no genera nota de valor negativo")
        void residuoNegativo() {
            ResultadoFormateado r = formateador.formatearExito("-cos(x)", -2.4492935982947064E-16, 4.0,
                    0, 2 * Math.PI);
            assertEquals("0", r.integralDefinida());
            assertNull(r.nota());
        }

        @Test
        @DisplayName("Integral definida no finita")
        void noFinita() {
            assertThrows(IllegalArgumentException.class,
                    () -> formateador.formatearExito("x", Double.NaN, null, 0, 1));
        }
    }

    @Nested
    @DisplayName("Errores")
    class Errores {

        @ParameterizedTest(name = "{0} → \"{1}\"")
        @CsvSource({
                "SINTAXIS, Expresión no reconocida",
                "NO_SOPORTADA, Integral no soportada",
                "DOMINIO, Fuera del dominio",
                "PROLOG_NO_DISPONIBLE, SWI-Prolog no disponible",
                "TIEMPO_AGOTADO, Tiempo agotado",
                "INTERNO, Error interno"})
        @DisplayName("Cada código del contrato tiene título y mensaje en español")
        void codigos(String codigo, String titulo) {
            MensajeError e = formateador.formatearError(codigo, "detalle");
            assertEquals(titulo, e.titulo());
            assertTrue(e.mensaje().length() > 20);
            assertEquals("detalle", e.detalle());
        }

        @Test
        @DisplayName("Código desconocido: mensaje genérico con el código en el detalle")
        void codigoDesconocido() {
            MensajeError e = formateador.formatearError("FOO", "algo");
            assertEquals("Error desconocido", e.titulo());
            assertTrue(e.detalle().contains("FOO"));
            assertTrue(e.detalle().contains("algo"));
        }

        @Test
        @DisplayName("Código nulo")
        void codigoNulo() {
            assertEquals("Error desconocido", formateador.formatearError(null, null).titulo());
        }

        @Test
        @DisplayName("Detalle vacío se convierte en null")
        void detalleVacio() {
            assertNull(formateador.formatearError("DOMINIO", "   ").detalle());
            assertNull(formateador.formatearError("DOMINIO", null).detalle());
        }
    }
}
