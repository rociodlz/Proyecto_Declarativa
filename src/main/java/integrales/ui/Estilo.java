package integrales.ui;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.plaf.metal.DefaultMetalTheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Paleta, tipografías y componentes con pintura propia de la interfaz.
 *
 * <p>La pintura personalizada se limita a formas simples de Java2D: rectángulos
 * redondeados, una sombra suave y dos íconos.</p>
 */
final class Estilo {

    // --- Paleta base ----------------------------------------------------
    static final Color NOCHE = new Color(0x031926);
    static final Color PETROLEO = new Color(0x468189);
    static final Color SALVIA = new Color(0x77ACA2);
    static final Color VERDE_SUAVE = new Color(0x9DBEBB);
    static final Color CHAMPAGNE = new Color(0xF4E9CD);

    // --- Tonos derivados ------------------------------------------------
    static final Color FONDO = new Color(0xFAF6EC);
    static final Color TARJETA = Color.WHITE;
    static final Color BORDE_TARJETA = new Color(0xE9E0CB);
    static final Color BORDE_CAMPO = new Color(0xC9D7D4);
    static final Color CAMPO_DESACTIVADO = new Color(0xF4F1E9);
    static final Color TEXTO = NOCHE;
    static final Color TEXTO_SECUNDARIO = new Color(0x4F6670);
    static final Color TEXTO_DESACTIVADO = new Color(0x93A3A8);
    // Botón primario: #3B6F76 da un contraste de 5.6:1 con texto blanco (WCAG AA).
    static final Color PETROLEO_BOTON = new Color(0x3B6F76);
    static final Color PETROLEO_HOVER = new Color(0x33636A);
    static final Color PETROLEO_PRESIONADO = new Color(0x2F5B61);
    static final Color PETROLEO_DESACTIVADO = new Color(0xB4CCC9);
    static final Color VERDE_MUY_SUAVE = new Color(0xEDF4F3);
    static final Color VERDE_HOVER = new Color(0xE1EDEB);
    static final Color VERDE_BORDE = new Color(0xD3E4E1);
    static final Color CALIDO_SUAVE = new Color(0xFCF9F2);
    static final Color CHAMPAGNE_SUAVE = new Color(0xFCF4DF);
    static final Color CHAMPAGNE_BORDE = new Color(0xE6D6AC);
    static final Color SELECCION = new Color(0xCFE3E0);
    static final Color ERROR = new Color(0xA8261B);
    static final Color ERROR_TEXTO = new Color(0x7E1D14);
    static final Color ERROR_FONDO = new Color(0xFDF0EE);
    static final Color ERROR_BORDE = new Color(0xE9B3AC);

    // --- Tipografías ----------------------------------------------------
    private static final String FAMILIA_TEXTO = elegirFamilia(Font.DIALOG, "Segoe UI");
    private static final String FAMILIA_SEMI = elegirFamilia(FAMILIA_TEXTO, "Segoe UI Semibold");
    // Consolas tiene negrita real; con fuentes sin negrita, Java la sintetiza y el
    // texto dibujado puede quedar más ancho que el medido al ajustar líneas.
    private static final String FAMILIA_FORMULA = elegirFamilia(Font.MONOSPACED, "Consolas");

    static final Font TEXTO_NORMAL = new Font(FAMILIA_TEXTO, Font.PLAIN, 14);
    static final Font TEXTO_PEQUENO = new Font(FAMILIA_TEXTO, Font.PLAIN, 12);
    static final Font TEXTO_NEGRITA = semi(14);
    static final Font BOTON = semi(15);
    static final Font TITULO_APP = semi(24);
    static final Font SUBTITULO_APP = new Font(FAMILIA_TEXTO, Font.PLAIN, 13);
    static final Font TITULO_TARJETA = semi(17);
    static final Font ETIQUETA = semi(12).deriveFont(Map.of(TextAttribute.TRACKING, 0.06f));
    static final Font FORMULA = new Font(FAMILIA_FORMULA, Font.PLAIN, 15);
    static final Font FORMULA_RESULTADO = new Font(FAMILIA_FORMULA, Font.BOLD, 18);
    static final Font NUMERO = semi(28);
    static final Font MARCADOR = new Font(FAMILIA_TEXTO, Font.PLAIN, 16);

    // --- Medidas ---------------------------------------------------------
    static final int SOMBRA = 6;
    static final int RADIO_TARJETA = 16;
    static final int RADIO_RECUADRO = 12;
    static final int RADIO_CAMPO = 10;

    private static final String CLAVE_ERROR = "integrales.ui.error";
    private static final String CLAVE_HOVER = "integrales.ui.hover";

    private Estilo() {
    }

    /** Lanza una excepción si se llama fuera del hilo de eventos de Swing. */
    static void verificarEDT() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "Los componentes de la interfaz deben usarse desde el hilo de eventos de Swing (EDT).");
        }
    }

    static Graphics2D suavizado(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    static JLabel etiqueta(String texto, Font fuente, Color color) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuente);
        etiqueta.setForeground(color);
        return etiqueta;
    }

    // ==================================================================
    // Tarjetas
    // ==================================================================

    /** Prepara un panel para pintarse como tarjeta con {@link #pintarTarjeta}. */
    static void comoTarjeta(JPanel panel) {
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(SOMBRA + 14, SOMBRA + 20, SOMBRA + 18, SOMBRA + 20));
    }

    /** Tarjeta blanca con esquinas redondeadas, borde delicado y sombra suave. */
    static void pintarTarjeta(Graphics g, JComponent c) {
        Graphics2D g2 = suavizado(g);
        float x = SOMBRA;
        float y = SOMBRA - 2;
        float ancho = c.getWidth() - 2f * SOMBRA;
        float alto = c.getHeight() - 2f * SOMBRA;
        for (int i = SOMBRA; i >= 1; i--) {
            g2.setColor(new Color(3, 25, 38, 2 + (SOMBRA - i)));
            float radio = RADIO_TARJETA + 2f * i;
            g2.fill(new RoundRectangle2D.Float(x - i, y - i + 3, ancho + 2f * i, alto + 2f * i, radio, radio));
        }
        RoundRectangle2D forma = new RoundRectangle2D.Float(x, y, ancho, alto, RADIO_TARJETA, RADIO_TARJETA);
        g2.setColor(TARJETA);
        g2.fill(forma);
        g2.setColor(BORDE_TARJETA);
        g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, ancho - 1, alto - 1, RADIO_TARJETA, RADIO_TARJETA));
        g2.dispose();
    }

    // ==================================================================
    // Recuadros redondeados (resultados, notas, errores)
    // ==================================================================

    static Recuadro recuadro(Color fondo, Color borde) {
        return new Recuadro(fondo, borde);
    }

    /** Panel con fondo redondeado, borde opcional y barra de acento opcional a la izquierda. */
    static final class Recuadro extends JPanel {

        private static final long serialVersionUID = 1L;
        private final Color fondo;
        private final Color borde;
        private Color acento;

        Recuadro(Color fondo, Color borde) {
            this.fondo = fondo;
            this.borde = borde;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        }

        void setAcento(Color acento) {
            this.acento = acento;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = suavizado(g);
            RoundRectangle2D forma = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f,
                    RADIO_RECUADRO, RADIO_RECUADRO);
            g2.setColor(fondo);
            g2.fill(forma);
            if (acento != null) {
                g2.clip(forma);
                g2.setColor(acento);
                g2.fillRect(0, 0, 5, getHeight());
                g2.setClip(null);
            }
            if (borde != null) {
                g2.setColor(borde);
                g2.draw(forma);
            }
            g2.dispose();
        }
    }

    // ==================================================================
    // Íconos dibujados (no dependen de glifos de la fuente)
    // ==================================================================

    /**
     * Distintivo del encabezado: cuadrado redondeado verde petróleo con una
     * integral dibujada con curvas de Bézier (no depende de ninguna fuente).
     */
    static Icon iconoLogo() {
        return new Icon() {
            private static final int TAMANO = 46;

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = suavizado(g);
                g2.setColor(PETROLEO);
                g2.fill(new RoundRectangle2D.Float(x, y, TAMANO, TAMANO, 14, 14));

                // Integral en coordenadas del cuadrado de 46 px: gancho superior a la
                // derecha, tallo ligeramente inclinado y gancho inferior a la izquierda.
                // Su caja (15–31, 8–38) queda centrada en el cuadrado.
                Path2D.Float integral = new Path2D.Float();
                integral.moveTo(31.0f, 12.6f);
                integral.curveTo(31.0f, 9.7f, 29.3f, 8.0f, 27.1f, 8.0f);
                integral.curveTo(24.2f, 8.0f, 23.5f, 10.9f, 23.4f, 14.4f);
                integral.lineTo(22.6f, 31.6f);
                integral.curveTo(22.5f, 35.1f, 21.8f, 38.0f, 18.9f, 38.0f);
                integral.curveTo(16.7f, 38.0f, 15.0f, 36.3f, 15.0f, 33.4f);

                g2.translate(x, y);
                g2.setColor(CHAMPAGNE_SUAVE);
                trazarConGrosorVariable(g2, integral, 1.8f, 3.2f);
                // Remates redondeados discretos en ambos extremos.
                float d = 3.4f;
                g2.fill(new Ellipse2D.Float(31.0f - d / 2, 12.6f - d / 2, d, d));
                g2.fill(new Ellipse2D.Float(15.0f - d / 2, 33.4f - d / 2, d, d));
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return TAMANO;
            }

            @Override
            public int getIconHeight() {
                return TAMANO;
            }
        };
    }

    /**
     * Dibuja la curva con un grosor que crece suavemente desde los extremos
     * ({@code minimo}) hasta el centro ({@code maximo}). La curva se recorre en
     * tramos cortos y cada uno se traza con el grosor de su posición.
     */
    private static void trazarConGrosorVariable(Graphics2D g2, Shape curva, float minimo, float maximo) {
        List<float[]> puntos = new ArrayList<>();
        float[] coordenadas = new float[6];
        for (PathIterator it = curva.getPathIterator(null, 0.05); !it.isDone(); it.next()) {
            it.currentSegment(coordenadas);
            puntos.add(new float[] {coordenadas[0], coordenadas[1]});
        }
        double total = 0;
        for (int i = 1; i < puntos.size(); i++) {
            total += distancia(puntos.get(i - 1), puntos.get(i));
        }
        double recorrido = 0;
        for (int i = 1; i < puntos.size(); i++) {
            float[] a = puntos.get(i - 1);
            float[] b = puntos.get(i);
            double largo = distancia(a, b);
            int tramos = Math.max(1, (int) Math.ceil(largo / 0.4));
            for (int k = 0; k < tramos; k++) {
                double t0 = (double) k / tramos;
                double t1 = (double) (k + 1) / tramos;
                double posicion = (recorrido + largo * (t0 + t1) / 2) / total;
                float grosor = (float) (minimo + (maximo - minimo) * Math.sin(Math.PI * posicion));
                g2.setStroke(new BasicStroke(grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Double(a[0] + (b[0] - a[0]) * t0, a[1] + (b[1] - a[1]) * t0,
                        a[0] + (b[0] - a[0]) * t1, a[1] + (b[1] - a[1]) * t1));
            }
            recorrido += largo;
        }
    }

    private static double distancia(float[] a, float[] b) {
        return Math.hypot(b[0] - a[0], b[1] - a[1]);
    }

    static Icon iconoAlerta(Color color) {
        return new Icono(true, color);
    }

    static Icon iconoInfo(Color color) {
        return new Icono(false, color);
    }

    private static final class Icono implements Icon {
        private static final int TAMANO = 18;
        private final boolean alerta;
        private final Color color;

        Icono(boolean alerta, Color color) {
            this.alerta = alerta;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = suavizado(g);
            g2.setColor(color);
            g2.fill(new Ellipse2D.Float(x, y, TAMANO, TAMANO));
            g2.setColor(Color.WHITE);
            float centro = x + TAMANO / 2f;
            if (alerta) {
                g2.fill(new RoundRectangle2D.Float(centro - 1.2f, y + 4f, 2.4f, 7.2f, 2f, 2f));
                g2.fill(new Ellipse2D.Float(centro - 1.4f, y + 12.4f, 2.8f, 2.8f));
            } else {
                g2.fill(new Ellipse2D.Float(centro - 1.4f, y + 3.6f, 2.8f, 2.8f));
                g2.fill(new RoundRectangle2D.Float(centro - 1.2f, y + 7.8f, 2.4f, 6.8f, 2f, 2f));
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return TAMANO;
        }

        @Override
        public int getIconHeight() {
            return TAMANO;
        }
    }

    // ==================================================================
    // Campos de texto
    // ==================================================================

    /** Aplica la fuente a un campo. El borde y el foco los gestiona {@link CampoTexto}. */
    static void prepararCampo(JTextField campo, Font fuente) {
        campo.setFont(fuente);
    }

    static void marcarError(JTextField campo, boolean error) {
        campo.putClientProperty(CLAVE_ERROR, error);
        campo.repaint();
    }

    static boolean tieneError(JTextField campo) {
        return Boolean.TRUE.equals(campo.getClientProperty(CLAVE_ERROR));
    }

    /** Campo con fondo redondeado; el borde indica foco, hover, error o desactivado. */
    static final class CampoTexto extends JTextField {

        private static final long serialVersionUID = 1L;

        CampoTexto(int columnas) {
            super(columnas);
            setOpaque(false);
            setBorder(new BordeCampo());
            setForeground(TEXTO);
            setCaretColor(NOCHE);
            setSelectionColor(SELECCION);
            setSelectedTextColor(NOCHE);
            setDisabledTextColor(TEXTO_DESACTIVADO);
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    putClientProperty(CLAVE_HOVER, Boolean.TRUE);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    putClientProperty(CLAVE_HOVER, Boolean.FALSE);
                    repaint();
                }
            });
        }

        /** Usa siempre la UI básica para que el Look &amp; Feel no pinte encima. */
        @Override
        public void updateUI() {
            setUI(new BasicTextFieldUI());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = suavizado(g);
            g2.setColor(isEnabled() ? TARJETA : CAMPO_DESACTIVADO);
            g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f,
                    RADIO_CAMPO, RADIO_CAMPO));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class BordeCampo implements Border {

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(9, 12, 9, 12);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int ancho, int alto) {
            JComponent componente = (JComponent) c;
            Color color;
            float grosor;
            if (!c.isEnabled()) {
                color = BORDE_TARJETA;
                grosor = 1f;
            } else if (Boolean.TRUE.equals(componente.getClientProperty(CLAVE_ERROR))) {
                color = ERROR;
                grosor = 2f;
            } else if (c.hasFocus()) {
                color = PETROLEO;
                grosor = 2f;
            } else if (Boolean.TRUE.equals(componente.getClientProperty(CLAVE_HOVER))) {
                color = SALVIA;
                grosor = 1f;
            } else {
                color = BORDE_CAMPO;
                grosor = 1f;
            }
            Graphics2D g2 = suavizado(g);
            g2.setStroke(new BasicStroke(grosor));
            g2.setColor(color);
            float m = grosor / 2f;
            g2.draw(new RoundRectangle2D.Float(x + m, y + m, ancho - grosor, alto - grosor, RADIO_CAMPO, RADIO_CAMPO));
            g2.dispose();
        }
    }

    // ==================================================================
    // Botones
    // ==================================================================

    /** Botón redondeado: primario (verde petróleo) o secundario (discreto). */
    static final class Boton extends JButton {

        private static final long serialVersionUID = 1L;
        private final boolean primario;

        Boton(String texto, boolean primario) {
            super(texto);
            this.primario = primario;
            setFont(primario ? BOTON : TEXTO_NORMAL);
            setForeground(primario ? Color.WHITE : PETROLEO_PRESIONADO);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBorder(BorderFactory.createEmptyBorder(9, primario ? 26 : 20, 9, primario ? 26 : 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        /** Usa siempre la UI propia para que el Look &amp; Feel no pinte encima. */
        @Override
        public void updateUI() {
            setUI(new BotonUI());
        }

        boolean esPrimario() {
            return primario;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = suavizado(g);
            boolean activo = isEnabled();
            boolean presionado = getModel().isPressed() && getModel().isArmed();
            boolean encima = getModel().isRollover();
            Color fondo;
            Color borde = null;
            if (primario) {
                fondo = !activo ? PETROLEO_DESACTIVADO
                        : presionado ? PETROLEO_PRESIONADO
                        : encima ? PETROLEO_HOVER : PETROLEO_BOTON;
            } else {
                fondo = !activo ? CAMPO_DESACTIVADO
                        : presionado ? VERDE_HOVER
                        : encima ? VERDE_MUY_SUAVE : TARJETA;
                borde = !activo ? BORDE_TARJETA : encima ? SALVIA : BORDE_CAMPO;
            }
            RoundRectangle2D forma = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f,
                    RADIO_CAMPO, RADIO_CAMPO);
            g2.setColor(fondo);
            g2.fill(forma);
            if (borde != null) {
                g2.setColor(borde);
                g2.draw(forma);
            }
            if (isFocusOwner()) {
                // Anillo de foco visible para la navegación con teclado.
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(primario ? NOCHE : PETROLEO);
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 3f, getHeight() - 3f,
                        RADIO_CAMPO, RADIO_CAMPO));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Dibuja el texto con el color del botón también cuando está desactivado. */
    private static final class BotonUI extends BasicButtonUI {

        @Override
        protected void paintText(Graphics g, AbstractButton b, Rectangle r, String texto) {
            FontMetrics fm = b.getFontMetrics(b.getFont());
            Color color = b.getForeground();
            if (!b.isEnabled()) {
                color = (b instanceof Boton boton && boton.esPrimario())
                        ? new Color(255, 255, 255, 220) : TEXTO_DESACTIVADO;
            }
            g.setColor(color);
            int mnemonico = b.isEnabled() ? b.getDisplayedMnemonicIndex() : -1;
            BasicGraphicsUtils.drawStringUnderlineCharAt(b, (Graphics2D) g, texto, mnemonico,
                    r.x + getTextShiftOffset(), r.y + fm.getAscent() + getTextShiftOffset());
        }
    }

    // ==================================================================
    // Barra de desplazamiento delgada
    // ==================================================================

    static final class BarraDesplazamientoUI extends BasicScrollBarUI {

        @Override
        protected void installDefaults() {
            super.installDefaults();
            scrollBarWidth = 12;
        }

        @Override
        protected void configureScrollBarColors() {
            thumbColor = VERDE_SUAVE;
            trackColor = FONDO;
        }

        @Override
        protected JButton createDecreaseButton(int orientacion) {
            return botonVacio();
        }

        @Override
        protected JButton createIncreaseButton(int orientacion) {
            return botonVacio();
        }

        private static JButton botonVacio() {
            JButton boton = new JButton();
            Dimension cero = new Dimension(0, 0);
            boton.setPreferredSize(cero);
            boton.setMinimumSize(cero);
            boton.setMaximumSize(cero);
            return boton;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(FONDO);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = suavizado(g);
            g2.setColor(isThumbRollover() || isDragging ? SALVIA : VERDE_SUAVE);
            float ancho = r.width - 6f;
            g2.fill(new RoundRectangle2D.Float(r.x + 3f, r.y + 2f, ancho, r.height - 4f, ancho, ancho));
            g2.dispose();
        }
    }

    // ==================================================================
    // Tema del Look & Feel (menús, diálogos, tooltips, barra de progreso)
    // ==================================================================

    /** Tema plano para Metal con la paleta del proyecto. */
    static final class TemaMetal extends DefaultMetalTheme {

        private static final FontUIResource FUENTE = new FontUIResource(TEXTO_NORMAL);
        private static final FontUIResource FUENTE_PEQUENA = new FontUIResource(TEXTO_PEQUENO);

        @Override
        public String getName() {
            return "Sistema de Integrales";
        }

        @Override
        protected ColorUIResource getPrimary1() {
            return new ColorUIResource(PETROLEO);
        }

        @Override
        protected ColorUIResource getPrimary2() {
            return new ColorUIResource(SALVIA);
        }

        @Override
        protected ColorUIResource getPrimary3() {
            return new ColorUIResource(SELECCION);
        }

        @Override
        protected ColorUIResource getSecondary1() {
            return new ColorUIResource(new Color(0x9FB3B1));
        }

        @Override
        protected ColorUIResource getSecondary2() {
            return new ColorUIResource(new Color(0xDCE4E2));
        }

        @Override
        protected ColorUIResource getSecondary3() {
            return new ColorUIResource(FONDO);
        }

        @Override
        protected ColorUIResource getBlack() {
            return new ColorUIResource(NOCHE);
        }

        @Override
        public FontUIResource getControlTextFont() {
            return FUENTE;
        }

        @Override
        public FontUIResource getSystemTextFont() {
            return FUENTE;
        }

        @Override
        public FontUIResource getUserTextFont() {
            return FUENTE;
        }

        @Override
        public FontUIResource getMenuTextFont() {
            return FUENTE;
        }

        @Override
        public FontUIResource getWindowTitleFont() {
            return FUENTE;
        }

        @Override
        public FontUIResource getSubTextFont() {
            return FUENTE_PEQUENA;
        }
    }

    // ==================================================================
    // Texto seleccionable
    // ==================================================================

    /**
     * Texto de solo lectura que se puede seleccionar y copiar, y que se ajusta
     * al ancho disponible al redimensionar la ventana.
     *
     * @param porPalabras {@code true} para cortar líneas entre palabras;
     *                    {@code false} para cortar en cualquier carácter (fórmulas)
     */
    static TextoAjustable texto(Font fuente, Color color, boolean porPalabras) {
        TextoAjustable area = new TextoAjustable();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(porPalabras);
        area.setFont(fuente);
        area.setForeground(color);
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setSelectionColor(SELECCION);
        area.setSelectedTextColor(NOCHE);
        return area;
    }

    /**
     * {@link JTextArea} con ajuste de línea que no impone su ancho al
     * contenedor y que recalcula su alto cuando cambia el ancho.
     */
    static final class TextoAjustable extends JTextArea {

        private static final long serialVersionUID = 1L;
        private int ultimoAncho = -1;

        TextoAjustable() {
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (getWidth() != ultimoAncho) {
                        ultimoAncho = getWidth();
                        revalidate();
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferido = super.getPreferredSize();
            return new Dimension(Math.min(preferido.width, 80), preferido.height);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(40, super.getMinimumSize().height);
        }
    }

    // ==================================================================

    private static Font semi(int tamano) {
        return FAMILIA_SEMI.equals(FAMILIA_TEXTO)
                ? new Font(FAMILIA_TEXTO, Font.BOLD, tamano)
                : new Font(FAMILIA_SEMI, Font.PLAIN, tamano);
    }

    /** Devuelve la primera familia instalada de la lista, o {@code respaldo}. */
    private static String elegirFamilia(String respaldo, String... preferidas) {
        try {
            List<String> familias = Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            for (String familia : preferidas) {
                if (familias.contains(familia)) {
                    return familia;
                }
            }
        } catch (RuntimeException e) {
            // Sin acceso a las fuentes del sistema: se usa la fuente lógica.
        }
        return respaldo;
    }
}
