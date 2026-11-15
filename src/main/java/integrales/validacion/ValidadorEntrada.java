package integrales.validacion;

import integrales.validacion.EntradaInvalidaException.Campo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valida el texto que escribe el usuario antes de enviarlo al servicio.
 *
 * <p>Solo comprueba que la entrada esté bien formada: caracteres, nombres de
 * funciones, paréntesis, operadores y límites numéricos. <b>No</b> normaliza la
 * expresión (eso corresponde a {@code NormalizadorExpresion}), no construye un
 * árbol de la expresión y no comprueba el dominio matemático ni las
 * singularidades (eso corresponde a la base de conocimiento).</p>
 *
 * <p>La expresión se recorre una sola vez dividiéndola en piezas (números,
 * palabras, operadores y paréntesis) y se revisa cada pieza respecto de la
 * anterior.</p>
 */
public final class ValidadorEntrada {

    /** Longitud máxima de la expresión, sin contar espacios al inicio y al final. */
    public static final int LONGITUD_MAXIMA = 200;

    /**
     * Valor absoluto máximo de los límites. Decisión provisional del proyecto
     * (no es un requisito del PDF): evita desbordamientos al evaluar potencias
     * y exponenciales.
     */
    public static final double LIMITE_ABSOLUTO = 1_000_000;

    /**
     * Funciones aceptadas. Incluye los nombres del contrato y los alias que
     * usa el PDF oficial (arcsin, arcsinh, sh, ...).
     */
    public static final Set<String> FUNCIONES_PERMITIDAS = Set.of(
            "sin", "sen", "cos", "tan",
            "asin", "acos", "atan",
            "sinh", "cosh", "tanh",
            "asinh", "acosh", "atanh",
            "exp", "log", "ln",
            "arcsin", "arccos", "arctan",
            "arcsinh", "arccosh", "arctanh",
            "sh");

    /** Constantes y variable aceptadas. */
    private static final Set<String> SIMBOLOS = Set.of("x", "e");

    /** Funciones simbólicas internas de la base de conocimiento. */
    private static final Set<String> FUNCIONES_GENERICAS = Set.of("u", "v");

    /** Funciones ordenadas de mayor a menor longitud, para sugerir correcciones. */
    private static final List<String> FUNCIONES_POR_LONGITUD = FUNCIONES_PERMITIDAS.stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();

    /** Número dentro de la expresión: 12, 1.5, .5, 2. */
    private static final Pattern NUMERO_EXPRESION = Pattern.compile("\\d+\\.?\\d*|\\.\\d+");

    /** Límite numérico: signo opcional, decimales y notación científica opcional. */
    private static final Pattern NUMERO_LIMITE =
            Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

    private static final String OPERADORES = "+-*/^";

    /**
     * Valida la expresión f(x).
     *
     * @param texto expresión escrita por el usuario
     * @return la misma expresión sin espacios al inicio y al final (no se normaliza)
     * @throws EntradaInvalidaException si la expresión está mal formada
     */
    public String validarExpresion(String texto) throws EntradaInvalidaException {
        if (texto == null || texto.isBlank()) {
            throw error("La expresión está vacía. Escriba una función de x, por ejemplo 3*x^2.");
        }
        String expresion = texto.strip();
        if (expresion.length() > LONGITUD_MAXIMA) {
            throw error("La expresión tiene " + expresion.length()
                    + " caracteres; el máximo es " + LONGITUD_MAXIMA + ".");
        }
        revisarSecuencia(dividirEnPiezas(expresion));
        return expresion;
    }

    /**
     * Valida un límite de integración. No compara a con b: a = b y a &gt; b son válidos.
     *
     * @param texto valor escrito por el usuario
     * @param campo {@link Campo#LIMITE_A} o {@link Campo#LIMITE_B}
     * @return el valor numérico del límite
     * @throws EntradaInvalidaException si el valor no es un número válido o está fuera de rango
     * @throws IllegalArgumentException si {@code campo} no es un límite
     */
    public double validarLimite(String texto, Campo campo) throws EntradaInvalidaException {
        if (campo != Campo.LIMITE_A && campo != Campo.LIMITE_B) {
            throw new IllegalArgumentException("El campo debe ser LIMITE_A o LIMITE_B: " + campo);
        }
        String nombre = campo == Campo.LIMITE_A ? "a" : "b";

        if (texto == null || texto.isBlank()) {
            throw new EntradaInvalidaException(campo, "El límite " + nombre + " es obligatorio.");
        }
        String valor = texto.strip();
        if (valor.indexOf(',') >= 0) {
            throw new EntradaInvalidaException(campo,
                    "El límite " + nombre + " usa coma. Use punto decimal, por ejemplo 1.5.");
        }
        if (!NUMERO_LIMITE.matcher(valor).matches()) {
            throw new EntradaInvalidaException(campo,
                    "El límite " + nombre + " debe ser un número, por ejemplo 0, -2.5 o 3.14159.");
        }
        double numero = Double.parseDouble(valor);
        if (!Double.isFinite(numero)) {
            throw new EntradaInvalidaException(campo,
                    "El límite " + nombre + " está fuera de rango.");
        }
        if (Math.abs(numero) > LIMITE_ABSOLUTO) {
            throw new EntradaInvalidaException(campo,
                    "El límite " + nombre + " debe estar entre -1000000 y 1000000.");
        }
        return numero;
    }

    // ------------------------------------------------------------------
    // División en piezas
    // ------------------------------------------------------------------

    private enum Tipo { NUMERO, PALABRA, OPERADOR, ABRE, CIERRA }

    /** Pieza de la expresión; {@code posicion} empieza en 1. */
    private record Pieza(Tipo tipo, String texto, int posicion) { }

    private List<Pieza> dividirEnPiezas(String expresion) throws EntradaInvalidaException {
        List<Pieza> piezas = new ArrayList<>();
        int i = 0;
        while (i < expresion.length()) {
            char c = expresion.charAt(i);
            int inicio = i;
            if (Character.isWhitespace(c)) {
                i++;
            } else if (esDigito(c) || c == '.') {
                while (i < expresion.length() && (esDigito(expresion.charAt(i)) || expresion.charAt(i) == '.')) {
                    i++;
                }
                String numero = expresion.substring(inicio, i);
                if (!NUMERO_EXPRESION.matcher(numero).matches()) {
                    throw error("Número mal escrito: \"" + numero + "\" (posición " + (inicio + 1) + ").");
                }
                piezas.add(new Pieza(Tipo.NUMERO, numero, inicio + 1));
            } else if (esLetra(c)) {
                while (i < expresion.length() && esLetra(expresion.charAt(i))) {
                    i++;
                }
                String palabra = expresion.substring(inicio, i).toLowerCase(Locale.ROOT);
                piezas.add(new Pieza(Tipo.PALABRA, palabra, inicio + 1));
            } else if (OPERADORES.indexOf(c) >= 0) {
                piezas.add(new Pieza(Tipo.OPERADOR, String.valueOf(c), ++i));
            } else if (c == '(') {
                piezas.add(new Pieza(Tipo.ABRE, "(", ++i));
            } else if (c == ')') {
                piezas.add(new Pieza(Tipo.CIERRA, ")", ++i));
            } else if (c == ',') {
                throw error("La expresión contiene una coma (posición " + (i + 1)
                        + "). Use punto decimal, por ejemplo 1.5*x.");
            } else {
                throw error("Carácter no permitido: \"" + c + "\" (posición " + (i + 1) + ").");
            }
        }
        return piezas;
    }

    // ------------------------------------------------------------------
    // Revisión de la secuencia de piezas
    // ------------------------------------------------------------------

    /** Qué fue la pieza anterior, desde el punto de vista de la sintaxis. */
    private enum Anterior {
        INICIO,
        OPERADOR,      // operador binario
        MENOS_UNARIO,  // signo negativo
        ABRE,
        FUNCION,       // nombre de función, debe seguir "("
        NUMERO,
        SIMBOLO,       // x o e
        CIERRA
    }

    private void revisarSecuencia(List<Pieza> piezas) throws EntradaInvalidaException {
        Anterior anterior = Anterior.INICIO;
        Pieza piezaAnterior = null;
        int profundidad = 0;

        for (Pieza pieza : piezas) {
            if (anterior == Anterior.FUNCION && pieza.tipo() != Tipo.ABRE) {
                throw funcionSinParentesis(piezaAnterior);
            }
            switch (pieza.tipo()) {
                case NUMERO -> {
                    if (anterior == Anterior.NUMERO || anterior == Anterior.SIMBOLO
                            || anterior == Anterior.CIERRA) {
                        throw error("Falta un operador antes de \"" + pieza.texto()
                                + "\" (posición " + pieza.posicion() + ").");
                    }
                    anterior = Anterior.NUMERO;
                }
                case PALABRA -> anterior = clasificarPalabra(pieza);
                case ABRE -> {
                    profundidad++;
                    anterior = Anterior.ABRE;
                }
                case CIERRA -> {
                    if (anterior == Anterior.ABRE) {
                        throw error("Paréntesis vacíos \"()\" (posición " + pieza.posicion() + ").");
                    }
                    if (anterior == Anterior.OPERADOR || anterior == Anterior.MENOS_UNARIO) {
                        throw error("Expresión incompleta: falta un valor después de \""
                                + piezaAnterior.texto() + "\" (posición " + piezaAnterior.posicion() + ").");
                    }
                    profundidad--;
                    if (profundidad < 0) {
                        throw error("Paréntesis no balanceados: sobra \")\" (posición "
                                + pieza.posicion() + ").");
                    }
                    anterior = Anterior.CIERRA;
                }
                case OPERADOR -> anterior = clasificarOperador(pieza, anterior, piezaAnterior);
            }
            piezaAnterior = pieza;
        }

        if (anterior == Anterior.FUNCION) {
            throw funcionSinParentesis(piezaAnterior);
        }
        if (anterior == Anterior.OPERADOR || anterior == Anterior.MENOS_UNARIO) {
            throw error("Expresión incompleta: termina con \"" + piezaAnterior.texto() + "\".");
        }
        if (profundidad > 0) {
            throw error("Paréntesis no balanceados: falta cerrar " + profundidad + " paréntesis.");
        }
    }

    private Anterior clasificarPalabra(Pieza pieza) throws EntradaInvalidaException {
        String palabra = pieza.texto();
        if (FUNCIONES_PERMITIDAS.contains(palabra)) {
            return Anterior.FUNCION;
        }
        if (SIMBOLOS.contains(palabra)) {
            return Anterior.SIMBOLO;
        }
        if (FUNCIONES_GENERICAS.contains(palabra)) {
            throw error("\"" + palabra + "\" representa una función genérica. "
                    + "Escriba la función concreta, por ejemplo sin(x) o x^2.");
        }
        if (palabra.length() == 1) {
            throw error("Variable no permitida: \"" + palabra + "\" (posición " + pieza.posicion()
                    + "). Solo se permite la variable x.");
        }
        String sugerencia = sugerir(palabra);
        if (sugerencia != null) {
            throw error("No se reconoce \"" + palabra + "\" (posición " + pieza.posicion()
                    + "). ¿Quiso escribir " + sugerencia + "?");
        }
        throw error("Función no permitida: \"" + palabra + "\" (posición " + pieza.posicion() + ").");
    }

    private Anterior clasificarOperador(Pieza pieza, Anterior anterior, Pieza piezaAnterior)
            throws EntradaInvalidaException {
        boolean esMenos = pieza.texto().equals("-");
        switch (anterior) {
            case INICIO, ABRE -> {
                if (esMenos) {
                    return Anterior.MENOS_UNARIO;
                }
                throw error("La expresión no puede tener \"" + pieza.texto() + "\" en la posición "
                        + pieza.posicion() + ": falta un valor antes del operador.");
            }
            case OPERADOR -> {
                if (esMenos) {
                    return Anterior.MENOS_UNARIO;
                }
                throw operadoresConsecutivos(piezaAnterior, pieza);
            }
            case MENOS_UNARIO -> throw operadoresConsecutivos(piezaAnterior, pieza);
            default -> {
                return Anterior.OPERADOR;
            }
        }
    }

    /** Sugiere "sin(x)" para "sinx", "sinh(x)" para "sinhx", etc. */
    private static String sugerir(String palabra) {
        for (String funcion : FUNCIONES_POR_LONGITUD) {
            if (palabra.startsWith(funcion)) {
                String resto = palabra.substring(funcion.length());
                if (SIMBOLOS.contains(resto)) {
                    return funcion + "(" + resto + ")";
                }
            }
        }
        return null;
    }

    private static EntradaInvalidaException funcionSinParentesis(Pieza funcion) {
        return error("La función " + funcion.texto() + " debe ir seguida de \"(\", por ejemplo "
                + funcion.texto() + "(x) (posición " + funcion.posicion() + ").");
    }

    private static EntradaInvalidaException operadoresConsecutivos(Pieza primero, Pieza segundo) {
        return error("Operadores consecutivos \"" + primero.texto() + segundo.texto()
                + "\" (posición " + primero.posicion() + ").");
    }

    private static EntradaInvalidaException error(String mensaje) {
        return new EntradaInvalidaException(Campo.EXPRESION, mensaje);
    }

    private static boolean esDigito(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean esLetra(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}
