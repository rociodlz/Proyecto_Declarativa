package integrales.validacion;

import integrales.validacion.EntradaInvalidaException.Campo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorEntradaTest {

    private final ValidadorEntrada validador = new ValidadorEntrada();

    /** Comprueba que la expresión se rechaza y que el mensaje contiene el fragmento indicado. */
    private void assertExpresionInvalida(String expresion, String fragmento) {
        EntradaInvalidaException e = assertThrows(EntradaInvalidaException.class,
                () -> validador.validarExpresion(expresion),
                () -> "Se esperaba rechazar: " + expresion);
        assertEquals(Campo.EXPRESION, e.getCampo());
        assertTrue(e.getMessage().toLowerCase().contains(fragmento.toLowerCase()),
                () -> "Mensaje para \"" + expresion + "\": " + e.getMessage());
    }

    private void assertLimiteInvalido(String valor, Campo campo, String fragmento) {
        EntradaInvalidaException e = assertThrows(EntradaInvalidaException.class,
                () -> validador.validarLimite(valor, campo),
                () -> "Se esperaba rechazar el límite: " + valor);
        assertEquals(campo, e.getCampo());
        assertTrue(e.getMessage().toLowerCase().contains(fragmento.toLowerCase()),
                () -> "Mensaje para \"" + valor + "\": " + e.getMessage());
    }

    // ==================================================================
    @Nested
    @DisplayName("Expresiones válidas")
    class ExpresionesValidas {

        @ParameterizedTest(name = "acepta \"{0}\"")
        @ValueSource(strings = {
                "3*x^2", "x", "5", "x^2+x", "x^2-x", "2^x", "0.5*x",
                "e^x", "exp(x)", "ln(x)", "log(x)", "tan(x)", "atanh(x)",
                "sin(x)+cos(x)", "3*(x+1)^2/4"})
        @DisplayName("Expresiones básicas")
        void expresionesBasicas(String expresion) {
            assertDoesNotThrow(() -> validador.validarExpresion(expresion));
        }

        @Test
        @DisplayName("Quita solo los espacios de los extremos y no normaliza")
        void devuelveExpresionSinEspaciosExteriores() throws EntradaInvalidaException {
            assertEquals("Sen( X ) + 3x", validador.validarExpresion("  Sen( X ) + 3x  "));
        }

        @ParameterizedTest(name = "acepta \"{0}\"")
        @ValueSource(strings = {"3x", "2(x+1)", "2sin(x)", "3e^x", "x(x+1)", "(x+1)(x-1)",
                "(x+1)x", "x sin(x)", "2 x"})
        @DisplayName("Multiplicación implícita (la completa el normalizador)")
        void multiplicacionImplicita(String expresion) {
            assertDoesNotThrow(() -> validador.validarExpresion(expresion));
        }

        @ParameterizedTest(name = "acepta \"{0}\"")
        @ValueSource(strings = {"Sen(X)", "SIN(x)", "LOG(x)", "sen(x)"})
        @DisplayName("Mayúsculas y nombres en español")
        void mayusculasYEspanol(String expresion) {
            assertDoesNotThrow(() -> validador.validarExpresion(expresion));
        }

        @ParameterizedTest(name = "acepta \"{0}\"")
        @ValueSource(strings = {"arcsin(x)", "arccos(x)", "arctan(x)", "arcsinh(x)",
                "arccosh(x)", "arctanh(x)", "sh(x)", "asinh(x)", "acosh(x)", "cosh(x)"})
        @DisplayName("Alias de funciones del PDF")
        void aliasDelPdf(String expresion) {
            assertDoesNotThrow(() -> validador.validarExpresion(expresion));
        }

        @ParameterizedTest(name = "acepta \"{0}\"")
        @ValueSource(strings = {"-x", "(-x+1)", "2*-x", "x/-2", "2^-x", "x^-1", "x^(-2)",
                "x--1", "x+-1", "-sin(x)"})
        @DisplayName("Signo negativo unario")
        void menosUnario(String expresion) {
            assertDoesNotThrow(() -> validador.validarExpresion(expresion));
        }

        @ParameterizedTest(name = "acepta \"{0}\"")
        @ValueSource(strings = {".5*x", "2.*x", "x^2.5", "10.25"})
        @DisplayName("Números decimales, incluidos .5 y 2.")
        void decimales(String expresion) {
            assertDoesNotThrow(() -> validador.validarExpresion(expresion));
        }

        @Test
        @DisplayName("Acepta exactamente 200 caracteres")
        void longitudMaxima() {
            String valida = "x" + "+x".repeat(98) + "+1";   // 1 + 196 + 2 = 199
            String doscientos = valida + "0";                 // ...+10 = 200 caracteres
            assertEquals(200, doscientos.length());
            assertDoesNotThrow(() -> validador.validarExpresion(doscientos));
            assertDoesNotThrow(() -> validador.validarExpresion("   " + doscientos + "   "),
                    "Los espacios exteriores no cuentan para el máximo");
        }

        @Test
        @DisplayName("La validación no comprueba el dominio: x^(-1) es sintácticamente válida")
        void noCompruebaDominio() {
            assertDoesNotThrow(() -> validador.validarExpresion("x^(-1)"));
            assertDoesNotThrow(() -> validador.validarExpresion("asin(x)"));
            assertDoesNotThrow(() -> validador.validarExpresion("sin(2*x)"));
        }
    }

    // ==================================================================
    @Nested
    @DisplayName("Expresiones inválidas")
    class ExpresionesInvalidas {

        @ParameterizedTest(name = "rechaza [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("E1: vacía")
        void vacia(String expresion) {
            assertExpresionInvalida(expresion, "vacía");
        }

        @Test
        @DisplayName("E2: más de 200 caracteres")
        void demasiadoLarga() {
            String larga = "x" + "+x".repeat(100);
            assertEquals(201, larga.length());
            assertExpresionInvalida(larga, "máximo es 200");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"x=2", "x$2", "x%2", "√x", "x²", "ñ"})
        @DisplayName("E3: carácter no permitido")
        void caracterNoPermitido(String expresion) {
            assertExpresionInvalida(expresion, "carácter no permitido");
        }

        @Test
        @DisplayName("E4: coma decimal")
        void comaDecimal() {
            assertExpresionInvalida("1,5*x", "punto decimal");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"1..2*x", "1.2.3", "x+.", "."})
        @DisplayName("E5: número mal escrito")
        void numeroMalEscrito(String expresion) {
            assertExpresionInvalida(expresion, "número mal escrito");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"y+1", "2*z", "t"})
        @DisplayName("E6: variable distinta de x")
        void variableNoPermitida(String expresion) {
            assertExpresionInvalida(expresion, "solo se permite la variable x");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"foo(x)", "sqrt(x)", "pi", "abs(x)"})
        @DisplayName("E6: función no permitida")
        void funcionNoPermitida(String expresion) {
            assertExpresionInvalida(expresion, "función no permitida");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"u(x)", "v(x)", "3*u(x)"})
        @DisplayName("E6: funciones genéricas u y v")
        void funcionesGenericas(String expresion) {
            assertExpresionInvalida(expresion, "función genérica");
        }

        @ParameterizedTest(name = "\"{0}\" sugiere \"{1}\"")
        @CsvSource({"sinx, sin(x)", "sinhx, sinh(x)", "2*cosx, cos(x)", "expx, exp(x)"})
        @DisplayName("E7: palabras pegadas con sugerencia")
        void sugerencia(String expresion, String sugerencia) {
            assertExpresionInvalida(expresion, "¿quiso escribir " + sugerencia);
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"sin x", "sin", "2*cos+1", "ln^2"})
        @DisplayName("E8: función sin paréntesis")
        void funcionSinParentesis(String expresion) {
            assertExpresionInvalida(expresion, "debe ir seguida de");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"(x+1", "x+1)", ")x(", "sin(x", "((x)"})
        @DisplayName("E9: paréntesis no balanceados")
        void parentesisNoBalanceados(String expresion) {
            assertExpresionInvalida(expresion, "paréntesis no balanceados");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"sin()", "()", "x*( )"})
        @DisplayName("E10: paréntesis vacíos")
        void parentesisVacios(String expresion) {
            assertExpresionInvalida(expresion, "paréntesis vacíos");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"x**2", "x+*2", "x*/2", "--x", "x^--1", "x+-*2"})
        @DisplayName("E11: operadores consecutivos")
        void operadoresConsecutivos(String expresion) {
            assertExpresionInvalida(expresion, "operadores consecutivos");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"*x", "+x", "^2", "(+x)", "(/x)"})
        @DisplayName("E12: operador al inicio o después de \"(\"")
        void operadorInicial(String expresion) {
            assertExpresionInvalida(expresion, "falta un valor antes");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"3*x^", "x+", "-", "(x+)", "sin(x-)", "2*-"})
        @DisplayName("E13: expresión incompleta")
        void incompleta(String expresion) {
            assertExpresionInvalida(expresion, "incompleta");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"2 3", "x2", "(x+1)2", "e2"})
        @DisplayName("E14: número sin operador después de otro valor (ambiguo)")
        void faltaOperador(String expresion) {
            assertExpresionInvalida(expresion, "falta un operador");
        }

        @Test
        @DisplayName("El mensaje indica la posición del error")
        void mensajeConPosicion() {
            assertExpresionInvalida("x+y", "posición 3");
        }
    }

    // ==================================================================
    @Nested
    @DisplayName("Límites")
    class Limites {

        @ParameterizedTest(name = "\"{0}\" → {1}")
        @CsvSource({
                "0, 0.0",
                "-2.5, -2.5",
                "+3, 3.0",
                "'  2  ', 2.0",
                "1e3, 1000.0",
                "1E-2, 0.01",
                ".5, 0.5",
                "2., 2.0",
                "3.14159, 3.14159",
                "1000000, 1000000.0",
                "-1000000, -1000000.0"})
        @DisplayName("Valores válidos")
        void valoresValidos(String texto, double esperado) throws EntradaInvalidaException {
            assertEquals(esperado, validador.validarLimite(texto, Campo.LIMITE_A));
        }

        @ParameterizedTest(name = "rechaza [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("L1: obligatorio")
        void obligatorio(String texto) {
            assertLimiteInvalido(texto, Campo.LIMITE_A, "obligatorio");
        }

        @Test
        @DisplayName("L2: coma decimal")
        void comaDecimal() {
            assertLimiteInvalido("1,5", Campo.LIMITE_A, "punto decimal");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"abc", "NaN", "Infinity", "-Infinity", "0x10", "2d", "1f",
                "pi", "1e", "--1", "1 2", "2*3", "."})
        @DisplayName("L3: no numérico")
        void noNumerico(String texto) {
            assertLimiteInvalido(texto, Campo.LIMITE_B, "debe ser un número");
        }

        @Test
        @DisplayName("L4: no finito")
        void noFinito() {
            assertLimiteInvalido("1e400", Campo.LIMITE_A, "fuera de rango");
        }

        @ParameterizedTest(name = "rechaza \"{0}\"")
        @ValueSource(strings = {"1e7", "1000000.1", "-1000000.1"})
        @DisplayName("L5: fuera del límite absoluto provisional")
        void fueraDeLimiteAbsoluto(String texto) {
            assertLimiteInvalido(texto, Campo.LIMITE_A, "entre -1000000 y 1000000");
        }

        @Test
        @DisplayName("El error indica el campo y el nombre del límite")
        void campoCorrecto() {
            assertLimiteInvalido("abc", Campo.LIMITE_A, "límite a");
            assertLimiteInvalido("abc", Campo.LIMITE_B, "límite b");
        }

        @Test
        @DisplayName("validarLimite solo acepta LIMITE_A o LIMITE_B")
        void campoExpresionNoPermitido() {
            assertThrows(IllegalArgumentException.class,
                    () -> validador.validarLimite("1", Campo.EXPRESION));
        }

        @Test
        @DisplayName("a = b y a > b son válidos: el validador no compara los límites")
        void ordenDeLimitesNoSeValida() throws EntradaInvalidaException {
            assertEquals(2.0, validador.validarLimite("2", Campo.LIMITE_A));
            assertEquals(0.0, validador.validarLimite("0", Campo.LIMITE_B));
            assertEquals(1.0, validador.validarLimite("1", Campo.LIMITE_A));
            assertEquals(1.0, validador.validarLimite("1", Campo.LIMITE_B));
        }
    }
}
