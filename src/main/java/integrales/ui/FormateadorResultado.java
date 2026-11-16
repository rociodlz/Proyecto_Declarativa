package integrales.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Convierte los valores que devuelve el servicio en texto para mostrar.
 *
 * <p>No usa Swing ni depende del modelo: recibe valores simples. Las
 * transformaciones de la primitiva solo se aplican cuando conservan el
 * significado matemático; si no pueden hacerse con seguridad, se conserva la
 * representación original.</p>
 */
public final class FormateadorResultado {

    /** Texto para el área geométrica cuando no se calculó. */
    public static final String NO_DISPONIBLE = "No disponible";

    private static final int DECIMALES = 6;
    private static final double LIMITE_CIENTIFICA = 1e9;

    /** Resultado listo para mostrar. {@code nota} puede ser {@code null}. */
    public record ResultadoFormateado(String primitiva, String integralDefinida,
                                      String areaGeometrica, String nota) {
        public ResultadoFormateado {
            Objects.requireNonNull(primitiva, "primitiva");
            Objects.requireNonNull(integralDefinida, "integralDefinida");
            Objects.requireNonNull(areaGeometrica, "areaGeometrica");
        }
    }

    /** Error listo para mostrar. {@code detalle} puede ser {@code null}. */
    public record MensajeError(String titulo, String mensaje, String detalle) {
        public MensajeError {
            Objects.requireNonNull(titulo, "titulo");
            Objects.requireNonNull(mensaje, "mensaje");
        }
    }

    /**
     * @param primitiva        primitiva tal como la devuelve Prolog
     * @param integralDefinida valor de P(b) − P(a)
     * @param areaGeometrica   área ∫|f| o {@code null} si no se calculó
     * @param a                límite inferior ingresado
     * @param b                límite superior ingresado
     */
    public ResultadoFormateado formatearExito(String primitiva, double integralDefinida,
                                              Double areaGeometrica, double a, double b) {
        String definida = formatearNumero(integralDefinida);
        String area = areaGeometrica == null ? NO_DISPONIBLE : formatearNumero(areaGeometrica);

        List<String> notas = new ArrayList<>();
        if (a == b) {
            notas.add("Los límites son iguales (a = b): la integral definida vale 0.");
        } else if (a > b) {
            notas.add("Los límites están invertidos (a > b): el signo de la integral definida "
                    + "es el contrario al de integrar de b a a.");
        } else if (redondear(integralDefinida).signum() < 0) {
            notas.add("La integral definida es negativa: en el intervalo predomina la parte "
                    + "de la función que está debajo del eje x.");
        }
        if (areaGeometrica == null) {
            notas.add("El área geométrica no está disponible para esta expresión.");
        }
        String nota = notas.isEmpty() ? null : String.join("\n", notas);
        return new ResultadoFormateado(formatearPrimitiva(primitiva), definida, area, nota);
    }

    /**
     * @param codigoError código del contrato (SINTAXIS, NO_SOPORTADA, ...)
     * @param detalle     detalle técnico; puede ser {@code null}
     */
    public MensajeError formatearError(String codigoError, String detalle) {
        String detalleLimpio = (detalle == null || detalle.isBlank()) ? null : detalle.strip();
        String codigo = codigoError == null ? "" : codigoError.strip();
        return switch (codigo) {
            case "SINTAXIS" -> new MensajeError("Expresión no reconocida",
                    "La base de conocimiento no pudo leer la expresión. Revise cómo está escrita "
                            + "la función (Ayuda > Sintaxis).", detalleLimpio);
            case "NO_SOPORTADA" -> new MensajeError("Integral no soportada",
                    "La base de conocimiento no tiene una regla para integrar esta expresión. "
                            + "En Ayuda > Sintaxis (F1), la sección «Qué se puede integrar» indica "
                            + "las formas admitidas.", detalleLimpio);
            case "DOMINIO" -> new MensajeError("Fuera del dominio",
                    "La primitiva no se puede evaluar en todo el intervalo [a, b]: la función no "
                            + "está definida en algún punto, tiene una singularidad o el valor es "
                            + "demasiado grande. El detalle técnico indica la causa; pruebe con otros "
                            + "límites.", detalleLimpio);
            case "PROLOG_NO_DISPONIBLE" -> new MensajeError("SWI-Prolog no disponible",
                    "No se encontró SWI-Prolog. Verifique que esté instalado y que la ruta "
                            + "configurada sea correcta.", detalleLimpio);
            case "TIEMPO_AGOTADO" -> new MensajeError("Tiempo agotado",
                    "El cálculo tardó más de lo permitido y se canceló.", detalleLimpio);
            case "INTERNO" -> new MensajeError("Error interno",
                    "Ocurrió un error inesperado al procesar la respuesta de la base de conocimiento.",
                    detalleLimpio);
            default -> new MensajeError("Error desconocido",
                    "No se pudo completar el cálculo.",
                    "Código: " + (codigo.isEmpty() ? "(vacío)" : codigo)
                            + (detalleLimpio == null ? "" : ". " + detalleLimpio));
        };
    }

    /**
     * Prepara la primitiva para mostrarla y agrega " + C".
     * <ul>
     *   <li>{@code raiz(F,2)} → {@code sqrt(F)}</li>
     *   <li>{@code raiz(F,N)} con N entero positivo → {@code (F^(1/N))}</li>
     *   <li>{@code abs(F)} → {@code |F|}, salvo que F ya contenga barras</li>
     * </ul>
     * Los espacios que escribe Prolog (por ejemplo {@code 2* -cos(x)}) se conservan.
     */
    public String formatearPrimitiva(String primitivaProlog) {
        Objects.requireNonNull(primitivaProlog, "primitivaProlog");
        if (primitivaProlog.isBlank()) {
            throw new IllegalArgumentException("La primitiva está vacía.");
        }
        return transformar(primitivaProlog.strip()) + " + C";
    }

    /** Hasta 6 decimales, sin ceros sobrantes; notación científica para |v| ≥ 10⁹. */
    public String formatearNumero(double valor) {
        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException("El valor no es finito: " + valor);
        }
        if (Math.abs(valor) >= LIMITE_CIENTIFICA) {
            String texto = String.format(Locale.ROOT, "%." + DECIMALES + "e", valor);
            int e = texto.indexOf('e');
            return quitarCerosFinales(texto.substring(0, e)) + texto.substring(e);
        }
        BigDecimal redondeado = redondear(valor);
        if (redondeado.signum() == 0) {
            return "0";
        }
        return redondeado.stripTrailingZeros().toPlainString();
    }

    // ------------------------------------------------------------------

    private static BigDecimal redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    private static String quitarCerosFinales(String mantisa) {
        if (mantisa.indexOf('.') < 0) {
            return mantisa;
        }
        String sinCeros = mantisa.replaceAll("0+$", "");
        return sinCeros.endsWith(".") ? sinCeros.substring(0, sinCeros.length() - 1) : sinCeros;
    }

    /** Recorre el texto y reescribe raiz(...) y abs(...) cuando es seguro. */
    private static String transformar(String texto) {
        StringBuilder salida = new StringBuilder();
        int i = 0;
        while (i < texto.length()) {
            String funcion = funcionEn(texto, i);
            if (funcion == null) {
                salida.append(texto.charAt(i));
                i++;
                continue;
            }
            int abre = i + funcion.length();
            int cierra = parentesisQueCierra(texto, abre);
            if (cierra < 0) {
                // Paréntesis sin cerrar: se conserva el resto tal cual.
                salida.append(texto, i, texto.length());
                break;
            }
            String interior = texto.substring(abre + 1, cierra);
            salida.append(funcion.equals("raiz") ? reescribirRaiz(interior) : reescribirAbs(interior));
            i = cierra + 1;
        }
        return salida.toString();
    }

    /** Devuelve "raiz" o "abs" si empieza en {@code i} como nombre completo seguido de "(". */
    private static String funcionEn(String texto, int i) {
        if (i > 0) {
            char anterior = texto.charAt(i - 1);
            if (Character.isLetterOrDigit(anterior) || anterior == '_') {
                return null;
            }
        }
        if (texto.startsWith("raiz(", i)) {
            return "raiz";
        }
        if (texto.startsWith("abs(", i)) {
            return "abs";
        }
        return null;
    }

    private static String reescribirRaiz(String interior) {
        List<String> argumentos = dividirArgumentos(interior);
        if (argumentos == null || argumentos.size() != 2) {
            return "raiz(" + transformar(interior) + ")";
        }
        String radicando = transformar(argumentos.get(0).strip());
        String indice = argumentos.get(1).strip();
        if (indice.equals("2")) {
            return "sqrt(" + radicando + ")";
        }
        if (indice.matches("[1-9]\\d*")) {
            String base = radicando.matches("[A-Za-z0-9.]+") ? radicando : "(" + radicando + ")";
            return "(" + base + "^(1/" + indice + "))";
        }
        return "raiz(" + radicando + "," + transformar(indice) + ")";
    }

    private static String reescribirAbs(String interior) {
        List<String> argumentos = dividirArgumentos(interior);
        String contenido = transformar(interior.strip());
        if (argumentos == null || argumentos.size() != 1 || contenido.contains("|")) {
            return "abs(" + contenido + ")";
        }
        return "|" + contenido + "|";
    }

    /** Divide por comas de primer nivel; {@code null} si los paréntesis no cuadran. */
    private static List<String> dividirArgumentos(String interior) {
        List<String> argumentos = new ArrayList<>();
        int profundidad = 0;
        int inicio = 0;
        for (int i = 0; i < interior.length(); i++) {
            char c = interior.charAt(i);
            if (c == '(') {
                profundidad++;
            } else if (c == ')') {
                profundidad--;
                if (profundidad < 0) {
                    return null;
                }
            } else if (c == ',' && profundidad == 0) {
                argumentos.add(interior.substring(inicio, i));
                inicio = i + 1;
            }
        }
        if (profundidad != 0) {
            return null;
        }
        argumentos.add(interior.substring(inicio));
        return argumentos;
    }

    private static int parentesisQueCierra(String texto, int abre) {
        int profundidad = 0;
        for (int i = abre; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '(') {
                profundidad++;
            } else if (c == ')') {
                profundidad--;
                if (profundidad == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
