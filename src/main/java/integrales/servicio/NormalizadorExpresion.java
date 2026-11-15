package integrales.servicio;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Convierte la expresión del usuario a la forma que espera Prolog ({@code docs/contrato.md}, sección 4).
 *
 * <p>Reglas, en este orden (secciones 4 y 4.1 del contrato):
 * <ol>
 *   <li>Minúsculas: {@code Sin(X)} → {@code sin(x)}.</li>
 *   <li>Un espacio entre dos operandos es un producto: {@code x sin(x)} → {@code x*sin(x)},
 *       {@code (x+1) (x-1)} → {@code (x+1)*(x-1)}. Después se quitan todos los espacios
 *       ({@code sin (x)} → {@code sin(x)}).</li>
 *   <li>{@code **} → {@code ^}.</li>
 *   <li>Nombres en español: {@code sen} → {@code sin} (también {@code senh}, {@code asen}, {@code asenh}).</li>
 *   <li>Nombres del PDF: {@code arcsin} → {@code asin} (y {@code arccos}, {@code arctan}, con o sin
 *       {@code h}), {@code sh} → {@code sinh}.</li>
 *   <li>Logaritmo natural como {@code ln}: {@code log(x)} → {@code ln(x)}.</li>
 *   <li>Decimales completos: {@code .5} → {@code 0.5}, {@code 2.} → {@code 2.0}.</li>
 *   <li>Multiplicación explícita: {@code 3x} → {@code 3*x}, {@code 2(} → {@code 2*(},
 *       {@code )(} → {@code )*(}, {@code )x} → {@code )*x}, {@code x(} → {@code x*(}.</li>
 *   <li>Signo negativo después de un operador entre paréntesis: {@code x^-1} → {@code x^(-1)},
 *       {@code 2*-x} → {@code 2*(-x)}. Sin paréntesis, Prolog lee {@code ^-} como un solo operador
 *       desconocido y da error de sintaxis.</li>
 * </ol>
 *
 * <p>Es idempotente: normalizar dos veces da lo mismo que normalizar una vez.
 * No valida: la validación la hace la interfaz antes de llamar al servicio.
 */
public final class NormalizadorExpresion {

    private static final Pattern ESPACIOS = Pattern.compile("\\s+");
    /** {@code sen(}, {@code senh(}, {@code asen(}, {@code asenh(}. */
    private static final Pattern SEN = Pattern.compile("sen(?=h?\\()");
    /** {@code arcsin(}, {@code arccosh(}, ... → {@code asin(}, {@code acosh(}, ... */
    private static final Pattern ARCO = Pattern.compile("(?<![a-z])arc(?=(sin|cos|tan)h?\\()");
    private static final Pattern SH = Pattern.compile("(?<![a-z])sh\\(");
    private static final Pattern LOG = Pattern.compile("(?<![a-z])log\\(");
    private static final Pattern PUNTO_INICIAL = Pattern.compile("(?<![0-9])\\.(?=[0-9])");
    private static final Pattern PUNTO_FINAL = Pattern.compile("(?<=[0-9])\\.(?![0-9])");
    /** Dígito seguido de letra o de paréntesis: {@code 3x}, {@code 2(}, {@code 2sin(x)}. */
    private static final Pattern DIGITO_ANTES = Pattern.compile("(\\d)(?=[a-z(])");
    /** Paréntesis de cierre seguido de un operando: {@code )(}, {@code )x}, {@code )2}. */
    private static final Pattern CIERRE_ANTES = Pattern.compile("\\)(?=[a-z0-9(])");
    /** La variable {@code x} sola seguida de paréntesis: {@code x(x+1)}. */
    private static final Pattern X_ANTES = Pattern.compile("(?<![a-z])x(?=\\()");

    private static final String OPERADORES = "^*/+-";

    public String normalizar(String expresion) {
        if (expresion == null) {
            return "";
        }
        String s = expresion.toLowerCase(Locale.ROOT);
        s = espaciosComoProducto(s);
        s = ESPACIOS.matcher(s).replaceAll("");
        s = s.replace("**", "^");
        s = SEN.matcher(s).replaceAll("sin");
        s = ARCO.matcher(s).replaceAll("a");
        s = SH.matcher(s).replaceAll("sinh(");
        s = LOG.matcher(s).replaceAll("ln(");
        s = PUNTO_INICIAL.matcher(s).replaceAll("0.");
        s = PUNTO_FINAL.matcher(s).replaceAll(".0");
        s = DIGITO_ANTES.matcher(s).replaceAll("$1*");
        s = CIERRE_ANTES.matcher(s).replaceAll(")*");
        s = X_ANTES.matcher(s).replaceAll("x*");
        return envolverNegativos(s);
    }

    /**
     * Cambia por {@code *} los espacios que separan dos operandos. Un nombre de función seguido de
     * espacio no es un operando ({@code sin (x)} sigue siendo {@code sin(x)}); {@code x} y {@code e} sí.
     */
    private static String espaciosComoProducto(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            if (!Character.isWhitespace(s.charAt(i))) {
                sb.append(s.charAt(i++));
                continue;
            }
            int j = i;
            while (j < s.length() && Character.isWhitespace(s.charAt(j))) {
                j++;
            }
            if (j < s.length() && terminaEnOperando(sb) && empiezaOperando(s.charAt(j))) {
                sb.append('*');
            }
            i = j;
        }
        return sb.toString();
    }

    private static boolean terminaEnOperando(CharSequence s) {
        if (s.length() == 0) {
            return false;
        }
        char ultimo = s.charAt(s.length() - 1);
        if (Character.isDigit(ultimo) || ultimo == '.' || ultimo == ')') {
            return true;
        }
        int inicio = s.length();
        while (inicio > 0 && Character.isLetterOrDigit(s.charAt(inicio - 1))) {
            inicio--;
        }
        String palabra = s.subSequence(inicio, s.length()).toString();
        return palabra.equals("x") || palabra.equals("e");
    }

    private static boolean empiezaOperando(char c) {
        return Character.isLetterOrDigit(c) || c == '.' || c == '(';
    }

    /** {@code op-operando} → {@code op(-operando)}, donde {@code op} es {@code ^ * / + -}. */
    private static String envolverNegativos(String s) {
        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i + 1 < sb.length(); i++) {
            if (OPERADORES.indexOf(sb.charAt(i)) >= 0 && sb.charAt(i + 1) == '-') {
                int fin = finDeFactor(sb, i + 2);
                if (fin > 0) {
                    sb.insert(fin, ')');
                    sb.insert(i + 1, '(');
                }
            }
        }
        return sb.toString();
    }

    /**
     * Fin (exclusivo) del factor que empieza en {@code inicio}, incluidas las potencias que lo siguen
     * ({@code -x^2} es {@code -(x^2)}). Devuelve -1 si en {@code inicio} no empieza un factor.
     */
    private static int finDeFactor(CharSequence s, int inicio) {
        int fin = finDeOperando(s, inicio);
        while (fin > 0 && fin < s.length() && s.charAt(fin) == '^') {
            int siguiente = finDeOperando(s, fin + 1);
            if (siguiente < 0) {
                break;
            }
            fin = siguiente;
        }
        return fin;
    }

    /** Un número, un nombre (con su llamada, si la hay) o un grupo entre paréntesis. */
    private static int finDeOperando(CharSequence s, int inicio) {
        if (inicio >= s.length()) {
            return -1;
        }
        char c = s.charAt(inicio);
        if (c == '(') {
            return cierreDe(s, inicio);
        }
        int i = inicio;
        if (Character.isDigit(c) || c == '.') {
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                i++;
            }
            return i;
        }
        if (Character.isLetter(c)) {
            while (i < s.length() && Character.isLetterOrDigit(s.charAt(i))) {
                i++;
            }
            return (i < s.length() && s.charAt(i) == '(') ? cierreDe(s, i) : i;
        }
        return -1;
    }

    /** Posición siguiente al paréntesis que cierra el que abre en {@code abre}, o -1 si no cierra. */
    private static int cierreDe(CharSequence s, int abre) {
        int nivel = 0;
        for (int i = abre; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                nivel++;
            } else if (s.charAt(i) == ')' && --nivel == 0) {
                return i + 1;
            }
        }
        return -1;
    }
}
