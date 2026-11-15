package integrales.ui;

import integrales.ui.FormateadorResultado.MensajeError;
import integrales.ui.FormateadorResultado.ResultadoFormateado;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Objects;

/**
 * Panel que muestra el resultado: integral indefinida, integral definida,
 * área geométrica y una nota opcional; o bien un mensaje de error.
 *
 * <p>Los valores y el error están en tarjetas distintas de un
 * {@link CardLayout}: nunca se muestran a la vez. Todos los métodos públicos
 * deben llamarse desde el EDT.</p>
 */
public class PanelResultado extends JPanel {

    private static final long serialVersionUID = 1L;

    static final String SIN_VALOR = "—";
    static final String CALCULANDO = "Calculando…";

    private static final String VISTA_VALORES = "valores";
    private static final String VISTA_ERROR = "error";

    private final CardLayout vistas = new CardLayout();
    private final JPanel contenedor = new JPanel(vistas);

    private final Estilo.TextoAjustable primitiva =
            Estilo.texto(Estilo.FORMULA_RESULTADO, Estilo.TEXTO, false);
    private final Estilo.TextoAjustable integralDefinida =
            Estilo.texto(Estilo.NUMERO, Estilo.TEXTO, false);
    private final Estilo.TextoAjustable areaGeometrica =
            Estilo.texto(Estilo.NUMERO, Estilo.TEXTO, false);
    private final Estilo.Recuadro recuadroNota =
            Estilo.recuadro(Estilo.CHAMPAGNE_SUAVE, Estilo.CHAMPAGNE_BORDE);
    private final Estilo.TextoAjustable nota = Estilo.texto(Estilo.TEXTO_NORMAL, Estilo.TEXTO, true);

    private final Estilo.Recuadro recuadroError = Estilo.recuadro(Estilo.ERROR_FONDO, Estilo.ERROR_BORDE);
    private final JLabel tituloError = Estilo.etiqueta("", Estilo.TITULO_TARJETA, Estilo.ERROR_TEXTO);
    private final Estilo.TextoAjustable mensajeError = Estilo.texto(Estilo.TEXTO_NORMAL, Estilo.TEXTO, true);
    private final Estilo.TextoAjustable detalleError =
            Estilo.texto(Estilo.TEXTO_PEQUENO, Estilo.TEXTO_SECUNDARIO, true);

    private String vistaActual = VISTA_VALORES;

    public PanelResultado() {
        Estilo.verificarEDT();
        Estilo.comoTarjeta(this);
        setLayout(new BorderLayout(0, 12));
        add(Estilo.etiqueta("Resultado", Estilo.TITULO_TARJETA, Estilo.TEXTO), BorderLayout.NORTH);

        contenedor.setOpaque(false);
        contenedor.add(construirValores(), VISTA_VALORES);
        contenedor.add(construirError(), VISTA_ERROR);
        add(contenedor, BorderLayout.CENTER);

        primitiva.getAccessibleContext().setAccessibleName("Integral indefinida");
        integralDefinida.getAccessibleContext().setAccessibleName("Integral definida");
        areaGeometrica.getAccessibleContext().setAccessibleName("Área geométrica");
        limpiar();
    }

    private JPanel construirValores() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;

        // Nivel 1: la primitiva, destacada con barra de acento.
        Estilo.Recuadro bloquePrimitiva = bloque("INTEGRAL INDEFINIDA", "∫ f(x) dx", primitiva,
                Estilo.VERDE_MUY_SUAVE, Estilo.VERDE_BORDE);
        bloquePrimitiva.setAcento(Estilo.PETROLEO);
        bloquePrimitiva.setBorder(BorderFactory.createEmptyBorder(12, 20, 14, 16));
        g.gridy = 0;
        panel.add(bloquePrimitiva, g);

        // Nivel 2: los dos valores numéricos, lado a lado.
        JPanel numeros = new JPanel(new GridLayout(1, 2, 12, 0));
        numeros.setOpaque(false);
        numeros.add(bloque("INTEGRAL DEFINIDA", "∫ₐᵇ f(x) dx", integralDefinida,
                Estilo.CALIDO_SUAVE, Estilo.BORDE_TARJETA));
        numeros.add(bloque("ÁREA GEOMÉTRICA", "∫ₐᵇ |f(x)| dx", areaGeometrica,
                Estilo.CALIDO_SUAVE, Estilo.BORDE_TARJETA));
        g.gridy = 1;
        g.insets = new Insets(12, 0, 0, 0);
        panel.add(numeros, g);

        // Nivel 3: nota informativa.
        JLabel iconoNota = new JLabel(Estilo.iconoInfo(Estilo.PETROLEO));
        iconoNota.setVerticalAlignment(SwingConstants.TOP);
        recuadroNota.setLayout(new BorderLayout(10, 0));
        recuadroNota.add(iconoNota, BorderLayout.WEST);
        recuadroNota.add(nota, BorderLayout.CENTER);
        g.gridy = 2;
        panel.add(recuadroNota, g);

        // Relleno para que el contenido quede arriba al agrandar la ventana.
        g.gridy = 3;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(0, 0, 0, 0);
        JPanel relleno = new JPanel();
        relleno.setOpaque(false);
        panel.add(relleno, g);
        return panel;
    }

    private static Estilo.Recuadro bloque(String nombre, String simbolo, JComponent valor,
                                          Color fondo, Color borde) {
        JPanel cabecera = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cabecera.setOpaque(false);
        cabecera.add(Estilo.etiqueta(nombre, Estilo.ETIQUETA, Estilo.PETROLEO));
        JLabel etiquetaSimbolo = Estilo.etiqueta(simbolo, Estilo.TEXTO_NORMAL, Estilo.TEXTO_SECUNDARIO);
        etiquetaSimbolo.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        cabecera.add(etiquetaSimbolo);

        Estilo.Recuadro recuadro = Estilo.recuadro(fondo, borde);
        recuadro.setLayout(new BorderLayout(0, 6));
        recuadro.setBorder(BorderFactory.createEmptyBorder(12, 16, 14, 16));
        recuadro.add(cabecera, BorderLayout.NORTH);
        recuadro.add(valor, BorderLayout.CENTER);
        return recuadro;
    }

    private JPanel construirError() {
        recuadroError.setLayout(new BorderLayout(12, 0));
        recuadroError.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JLabel iconoError = new JLabel(Estilo.iconoAlerta(Estilo.ERROR));
        iconoError.setVerticalAlignment(SwingConstants.TOP);
        iconoError.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        iconoError.getAccessibleContext().setAccessibleName("Error");
        recuadroError.add(iconoError, BorderLayout.WEST);

        JPanel textos = new JPanel(new GridBagLayout());
        textos.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.gridy = 0;
        textos.add(tituloError, g);
        g.gridy = 1;
        g.insets = new Insets(6, 0, 0, 0);
        textos.add(mensajeError, g);
        g.gridy = 2;
        g.insets = new Insets(10, 0, 0, 0);
        textos.add(detalleError, g);
        recuadroError.add(textos, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(recuadroError, BorderLayout.NORTH);
        return panel;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Estilo.pintarTarjeta(g, this);
    }

    // ------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------

    /** Muestra un resultado correcto y oculta cualquier error. */
    public void mostrarResultado(ResultadoFormateado resultado) {
        Estilo.verificarEDT();
        Objects.requireNonNull(resultado, "resultado");
        ponerTexto(primitiva, resultado.primitiva(), Estilo.FORMULA_RESULTADO);
        ponerTexto(integralDefinida, resultado.integralDefinida(), Estilo.NUMERO);
        ponerTexto(areaGeometrica, resultado.areaGeometrica(), Estilo.NUMERO);
        areaGeometrica.setToolTipText(FormateadorResultado.NO_DISPONIBLE.equals(resultado.areaGeometrica())
                ? "El área geométrica no se calculó para esta expresión." : null);
        mostrarNota(resultado.nota());
        cambiarVista(VISTA_VALORES);
    }

    /** Muestra un error de cálculo en lugar de los valores. */
    public void mostrarError(MensajeError error) {
        Estilo.verificarEDT();
        Objects.requireNonNull(error, "error");
        tituloError.setText(error.titulo());
        mensajeError.setText(error.mensaje());
        detalleError.setText(error.detalle() == null ? "" : "Detalle técnico: " + error.detalle());
        detalleError.setVisible(error.detalle() != null);
        cambiarVista(VISTA_ERROR);
    }

    /** Indica que se está calculando. */
    public void mostrarCalculando() {
        Estilo.verificarEDT();
        ponerMarcadores(CALCULANDO);
        mostrarNota(null);
        cambiarVista(VISTA_VALORES);
    }

    /** Vuelve al estado inicial, sin resultado ni error. */
    public void limpiar() {
        Estilo.verificarEDT();
        ponerMarcadores(SIN_VALOR);
        mostrarNota(null);
        cambiarVista(VISTA_VALORES);
    }

    // ------------------------------------------------------------------

    private void ponerMarcadores(String texto) {
        ponerTexto(primitiva, texto, Estilo.FORMULA_RESULTADO);
        ponerTexto(integralDefinida, texto, Estilo.NUMERO);
        ponerTexto(areaGeometrica, texto, Estilo.NUMERO);
        areaGeometrica.setToolTipText(null);
    }

    /** Los valores reales se destacan; los marcadores ("—", "Calculando…", "No disponible") se atenúan. */
    private static void ponerTexto(Estilo.TextoAjustable destino, String texto, Font fuenteValor) {
        boolean marcador = SIN_VALOR.equals(texto) || CALCULANDO.equals(texto)
                || FormateadorResultado.NO_DISPONIBLE.equals(texto);
        destino.setFont(marcador ? Estilo.MARCADOR : fuenteValor);
        destino.setForeground(marcador ? Estilo.TEXTO_SECUNDARIO : Estilo.TEXTO);
        destino.setText(texto);
    }

    private void mostrarNota(String texto) {
        nota.setText(texto == null ? "" : texto);
        recuadroNota.setVisible(texto != null);
    }

    private void cambiarVista(String vista) {
        vistaActual = vista;
        vistas.show(contenedor, vista);
        revalidate();
        repaint();
    }

    // --- Acceso para las pruebas del paquete -----------------------------

    boolean errorVisible() {
        return VISTA_ERROR.equals(vistaActual);
    }

    boolean valoresVisibles() {
        return VISTA_VALORES.equals(vistaActual);
    }

    String textoPrimitiva() {
        return primitiva.getText();
    }

    String textoIntegralDefinida() {
        return integralDefinida.getText();
    }

    String textoAreaGeometrica() {
        return areaGeometrica.getText();
    }

    String tooltipArea() {
        return areaGeometrica.getToolTipText();
    }

    boolean notaVisible() {
        return recuadroNota.isVisible();
    }

    String textoNota() {
        return nota.getText();
    }

    String textoTituloError() {
        return tituloError.getText();
    }

    String textoMensajeError() {
        return mensajeError.getText();
    }

    boolean detalleVisible() {
        return detalleError.isVisible();
    }

    String textoDetalle() {
        return detalleError.getText();
    }
}
