package integrales.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NormalizadorExpresionTest {

    private final NormalizadorExpresion normalizador = new NormalizadorExpresion();

    @ParameterizedTest(name = "[{index}] \"{0}\" → \"{1}\"")
    @CsvSource(delimiter = ';', value = {
            // Minúsculas y espacios
            "Sin( X );sin(x)",
            "'  3 * x ^ 2  ';3*x^2",
            // Nombres en español
            "sen(x);sin(x)",
            "senh(x);sinh(x)",
            "asen(x);asin(x)",
            "asenh(x);asinh(x)",
            "SEN(x)+cos(x);sin(x)+cos(x)",
            // Nombres del PDF
            "arcsin(x);asin(x)",
            "arccos(x);acos(x)",
            "arctan(x);atan(x)",
            "arcsinh(x);asinh(x)",
            "arccosh(x);acosh(x)",
            "ARCTANH(x);atanh(x)",
            "arcsen(x);asin(x)",
            "sh(x);sinh(x)",
            "cosh(x)+sinh(x);cosh(x)+sinh(x)",
            // Espacios entre operandos
            "x sin(x);x*sin(x)",
            "2 x;2*x",
            "(x+1) (x-1);(x+1)*(x-1)",
            "x (x+1);x*(x+1)",
            "e x;e*x",
            "sin (x);sin(x)",
            "3 * x;3*x",
            // Decimales
            ".5*x;0.5*x",
            "2.;2.0",
            "2.*x;2.0*x",
            "x^.5;x^0.5",
            "1.25x;1.25*x",
            // Logaritmo natural
            "log(x);ln(x)",
            "ln(x);ln(x)",
            "LOG(x+1)*ln(x);ln(x+1)*ln(x)",
            // Multiplicación implícita
            "3x;3*x",
            "3x^2;3*x^2",
            "0.5x;0.5*x",
            "2(x+1);2*(x+1)",
            "2sin(x);2*sin(x)",
            "(x+1)(x-1);(x+1)*(x-1)",
            "(x+1)x;(x+1)*x",
            "x(x+1);x*(x+1)",
            "2e^x;2*e^x",
            // ** como potencia
            "x**2;x^2",
            // Negativo después de un operador
            "x^-1;x^(-1)",
            "x^-2.5;x^(-2.5)",
            "e^-x;e^(-x)",
            "x^-(x+1);x^(-(x+1))",
            "2*-x;2*(-x)",
            "2*-x^2;2*(-x^2)",
            "x/-2;x/(-2)",
            "x+-1;x+(-1)",
            "x--1;x-(-1)",
            "3*-sin(x);3*(-sin(x))",
            // Sin cambios
            "-x;-x",
            "x^(-1);x^(-1)",
            "2^x;2^x",
            "atanh(x);atanh(x)",
            "asin(x)+acos(x);asin(x)+acos(x)",
            "exp(x);exp(x)",
            // Signo de una base numérica: Prolog leería (-2)^x.
            "-2^x;-(2^x)",
            "-2^x^2;-(2^x^2)",
            "3-2^x;3-2^x",
            "2*-2^x;2*(-(2^x))",
            "(-3^2);(-(3^2))",
            "-2*x;-2*x",
            "-2;-2",
            "-x^2;-x^2",
            "x^-2;x^(-2)",
    })
    void normaliza(String entrada, String esperado) {
        assertEquals(esperado, normalizador.normalizar(entrada));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "Sen( 2X ) + LOG(x)", "x^-1", "3x(x+1)", "2*-x^2", "e^-x", "x sin(x)", ".5x", "2.", "arcsinh(x)", "-2^x", "2*-2^x"})
    void esIdempotente(String entrada) {
        String una = normalizador.normalizar(entrada);
        assertEquals(una, normalizador.normalizar(una));
    }

    @Test
    void quitaTabuladoresYSaltosDeLinea() {
        assertEquals("x+1", normalizador.normalizar("X\t+\n1"));
    }

    @Test
    void nuloOVacio() {
        assertEquals("", normalizador.normalizar(null));
        assertEquals("", normalizador.normalizar("   "));
    }
}
