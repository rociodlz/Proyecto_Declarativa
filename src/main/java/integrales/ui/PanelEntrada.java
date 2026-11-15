package integrales.ui;

import integrales.validacion.EntradaInvalidaException.Campo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.Objects;

/**
 * Panel con los datos de la integral: f(x), límites a y b, y los botones
 * Resolver y Limpiar.
 *
 * <p>No valida ni calcula: expone el texto escrito y avisa cuando se pulsa un
 * botón. Todos los métodos públicos deben llamarse desde el EDT.</p>
 */
public class PanelEntrada extends JPanel {

    private static final long serialVersionUID = 1L;

    static final String AYUDA = "Ejemplos:  3*x^2  ·  sin(x)  ·  2^x  ·  x^-1  ·  atan(x)          "
            + "F1: ayuda de sintaxis";

    private final Estilo.CampoTexto campoExpresion = new Estilo.CampoTexto(28);
    private final Estilo.CampoTexto campoA = new Estilo.CampoTexto(8);
    private final Estilo.CampoTexto campoB = new Estilo.CampoTexto(8);
    private final Estilo.Boton botonResolver = new Estilo.Boton("Resolver", true);
    private final Estilo.Boton botonLimpiar = new Estilo.Boton("Limpiar", false);
    private final Estilo.Recuadro recuadroError = Estilo.recuadro(Estilo.ERROR_FONDO, Estilo.ERROR_BORDE);
    private final Estilo.TextoAjustable textoError =
            Estilo.texto(Estilo.TEXTO_NORMAL, Estilo.ERROR_TEXTO, true);

    private Runnable accionResolver = () -> { };
    private Runnable accionLimpiar = this::limpiar;

    public PanelEntrada() {
        Estilo.verificarEDT();
        Estilo.comoTarjeta(this);
        setLayout(new GridBagLayout());

        Estilo.prepararCampo(campoExpresion, Estilo.FORMULA);
        Estilo.prepararCampo(campoA, Estilo.FORMULA);
        Estilo.prepararCampo(campoB, Estilo.FORMULA);
        campoExpresion.setToolTipText("Función de x. Use * para multiplicar y ^ para las potencias.");
        campoA.setToolTipText("Límite inferior. Use punto decimal, por ejemplo -2.5.");
        campoB.setToolTipText("Límite superior. Use punto decimal, por ejemplo 3.14159.");
        campoExpresion.getAccessibleContext().setAccessibleName("Función f(x)");
        campoA.getAccessibleContext().setAccessibleName("Límite inferior a");
        campoB.getAccessibleContext().setAccessibleName("Límite superior b");

        botonResolver.setMnemonic(KeyEvent.VK_R);
        botonResolver.setToolTipText("Calcular la integral (Enter o Alt+R)");
        botonResolver.addActionListener(e -> accionResolver.run());
        botonLimpiar.setMnemonic(KeyEvent.VK_L);
        botonLimpiar.setToolTipText("Borrar los datos y el resultado (Alt+L)");
        botonLimpiar.addActionListener(e -> accionLimpiar.run());

        JLabel iconoError = new JLabel(Estilo.iconoAlerta(Estilo.ERROR));
        iconoError.setVerticalAlignment(SwingConstants.TOP);
        iconoError.getAccessibleContext().setAccessibleName("Error");
        recuadroError.setLayout(new BorderLayout(10, 0));
        recuadroError.add(iconoError, BorderLayout.WEST);
        recuadroError.add(textoError, BorderLayout.CENTER);
        recuadroError.setVisible(false);

        DocumentListener alEditar = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                quitarErroresSiHay();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                quitarErroresSiHay();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                quitarErroresSiHay();
            }
        };
        campoExpresion.getDocument().addDocumentListener(alEditar);
        campoA.getDocument().addDocumentListener(alEditar);
        campoB.getDocument().addDocumentListener(alEditar);

        construir();
    }

    private void construir() {
        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.WEST;

        // Título
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        g.insets = new Insets(0, 0, 12, 0);
        add(Estilo.etiqueta("Datos de la integral", Estilo.TITULO_TARJETA, Estilo.TEXTO), g);

        // f(x)
        g.gridy = 1;
        g.gridwidth = 1;
        g.insets = new Insets(0, 0, 0, 12);
        add(etiquetaPara("f(x) =", campoExpresion), g);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 0);
        add(campoExpresion, g);

        // Ayuda
        g.gridy = 2;
        g.insets = new Insets(5, 2, 12, 0);
        Estilo.TextoAjustable ayuda = Estilo.texto(Estilo.TEXTO_PEQUENO, Estilo.TEXTO_SECUNDARIO, true);
        ayuda.setText(AYUDA);
        ayuda.setFocusable(false);
        add(ayuda, g);

        // Límites y botones en la misma fila
        g.gridx = 0;
        g.gridy = 3;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        g.insets = new Insets(0, 0, 0, 12);
        add(etiquetaPara("a =", campoA), g);

        JPanel limites = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        limites.setOpaque(false);
        limites.add(campoA);
        JLabel etiquetaB = etiquetaPara("b =", campoB);
        etiquetaB.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 12));
        limites.add(etiquetaB);
        limites.add(campoB);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setOpaque(false);
        botones.add(botonLimpiar);
        botones.add(botonResolver);

        JPanel fila = new JPanel(new BorderLayout(16, 0));
        fila.setOpaque(false);
        fila.add(limites, BorderLayout.WEST);
        fila.add(botones, BorderLayout.EAST);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 0);
        add(fila, g);

        // Error de entrada
        g.gridx = 0;
        g.gridy = 4;
        g.gridwidth = 2;
        g.insets = new Insets(14, 0, 0, 0);
        add(recuadroError, g);
    }

    private static JLabel etiquetaPara(String texto, JTextField campo) {
        JLabel etiqueta = Estilo.etiqueta(texto, Estilo.FORMULA, Estilo.TEXTO);
        etiqueta.setLabelFor(campo);
        return etiqueta;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Estilo.pintarTarjeta(g, this);
    }

    // ------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------

    /** Texto escrito en f(x), sin validar. */
    public String getExpresion() {
        Estilo.verificarEDT();
        return campoExpresion.getText();
    }

    /** Texto escrito en el límite a, sin validar. */
    public String getLimiteA() {
        Estilo.verificarEDT();
        return campoA.getText();
    }

    /** Texto escrito en el límite b, sin validar. */
    public String getLimiteB() {
        Estilo.verificarEDT();
        return campoB.getText();
    }

    /** Escribe los tres campos (útil para la vista previa y las pruebas). */
    public void setDatos(String expresion, String a, String b) {
        Estilo.verificarEDT();
        campoExpresion.setText(expresion);
        campoA.setText(a);
        campoB.setText(b);
    }

    /** Acción que se ejecuta al pulsar Resolver. Reemplaza la anterior. */
    public void alResolver(Runnable accion) {
        Estilo.verificarEDT();
        accionResolver = Objects.requireNonNull(accion, "accion");
    }

    /** Acción que se ejecuta al pulsar Limpiar. Reemplaza la anterior. */
    public void alLimpiar(Runnable accion) {
        Estilo.verificarEDT();
        accionLimpiar = Objects.requireNonNull(accion, "accion");
    }

    /** Resalta el campo con error, muestra el mensaje y lleva el foco a ese campo. */
    public void mostrarErrorEntrada(Campo campo, String mensaje) {
        Estilo.verificarEDT();
        Objects.requireNonNull(campo, "campo");
        quitarErrores();
        JTextField destino = campo(campo);
        Estilo.marcarError(destino, true);
        textoError.setText(mensaje);
        recuadroError.setVisible(true);
        destino.requestFocusInWindow();
        revalidate();
        repaint();
    }

    /** Quita los resaltados y el mensaje de error. */
    public void quitarErrores() {
        Estilo.verificarEDT();
        Estilo.marcarError(campoExpresion, false);
        Estilo.marcarError(campoA, false);
        Estilo.marcarError(campoB, false);
        textoError.setText("");
        recuadroError.setVisible(false);
        revalidate();
        repaint();
    }

    /** Activa o desactiva los campos y los botones (por ejemplo, mientras se calcula). */
    public void setHabilitado(boolean habilitado) {
        Estilo.verificarEDT();
        campoExpresion.setEnabled(habilitado);
        campoA.setEnabled(habilitado);
        campoB.setEnabled(habilitado);
        botonResolver.setEnabled(habilitado);
        botonLimpiar.setEnabled(habilitado);
    }

    /** Vacía los campos, quita los errores y lleva el foco a f(x). */
    public void limpiar() {
        Estilo.verificarEDT();
        setDatos("", "", "");
        quitarErrores();
        campoExpresion.requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    // Acceso para la ventana y las pruebas del paquete
    // ------------------------------------------------------------------

    JButton botonResolver() {
        return botonResolver;
    }

    JButton botonLimpiar() {
        return botonLimpiar;
    }

    JTextField campo(Campo campo) {
        return switch (campo) {
            case EXPRESION -> campoExpresion;
            case LIMITE_A -> campoA;
            case LIMITE_B -> campoB;
        };
    }

    boolean errorVisible() {
        return recuadroError.isVisible();
    }

    String textoError() {
        return textoError.getText();
    }

    private void quitarErroresSiHay() {
        if (recuadroError.isVisible()) {
            quitarErrores();
        }
    }
}
