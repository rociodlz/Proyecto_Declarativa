package integrales.ui;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * Ventana principal: menú, encabezado, aviso opcional, panel de entrada,
 * panel de resultado y barra de estado.
 *
 * <p>No contiene lógica de cálculo. Debe crearse y usarse desde el EDT,
 * por ejemplo dentro de {@code SwingUtilities.invokeLater}.</p>
 */
public class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    static final String TITULO_VENTANA = "Sistema de Integrales — Programación Declarativa";
    static final String ESTADO_LISTO = "Listo";
    static final String ESTADO_CALCULANDO = "Calculando…";

    private final PanelEntrada panelEntrada = new PanelEntrada();
    private final PanelResultado panelResultado = new PanelResultado();
    private final JPanel franjaAviso = new JPanel(new BorderLayout(10, 0));
    private final Estilo.TextoAjustable textoAviso = Estilo.texto(Estilo.TEXTO_NEGRITA, Estilo.NOCHE, true);
    private final JLabel etiquetaEstado =
            Estilo.etiqueta(ESTADO_LISTO, Estilo.TEXTO_PEQUENO, Estilo.TEXTO_SECUNDARIO);
    private final JProgressBar progreso = new JProgressBar();
    private boolean calculando;

    public VentanaPrincipal() {
        super(TITULO_VENTANA);
        Estilo.verificarEDT();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setJMenuBar(crearMenu());

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Estilo.FONDO);
        raiz.add(crearCabecera(), BorderLayout.NORTH);
        raiz.add(crearCuerpo(), BorderLayout.CENTER);
        raiz.add(crearBarraEstado(), BorderLayout.SOUTH);
        setContentPane(raiz);

        getRootPane().setDefaultButton(panelEntrada.botonResolver());
        panelEntrada.alLimpiar(this::limpiarTodo);

        setMinimumSize(new Dimension(720, 560));
        setSize(new Dimension(920, 780));
        setLocationRelativeTo(null);
    }

    /**
     * Activa el Look &amp; Feel Metal con un tema plano en la paleta del
     * proyecto (menús, diálogos, tooltips y barra de progreso). Los campos,
     * botones y tarjetas tienen pintura propia. Si no se puede aplicar, se
     * conserva el Look &amp; Feel predeterminado. Debe llamarse antes de
     * crear la ventana.
     */
    public static void instalarAspecto() {
        try {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            MetalLookAndFeel.setCurrentTheme(new Estilo.TemaMetal());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
            UIManager.put("ProgressBar.foreground", Estilo.PETROLEO);
            UIManager.put("ProgressBar.background", Estilo.VERDE_MUY_SUAVE);
            UIManager.put("ToolTip.background", Estilo.NOCHE);
            UIManager.put("ToolTip.foreground", Color.WHITE);
            UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 10, 6, 10));
            UIManager.put("ToolTip.font", Estilo.TEXTO_PEQUENO);
            UIManager.put("MenuItem.acceleratorForeground", Estilo.TEXTO_SECUNDARIO);
        } catch (UnsupportedLookAndFeelException e) {
            // Se mantiene el Look & Feel predeterminado.
        }
    }

    // ------------------------------------------------------------------
    // Construcción
    // ------------------------------------------------------------------

    private JMenuBar crearMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        archivo.setMnemonic(KeyEvent.VK_A);
        JMenuItem salir = new JMenuItem("Salir");
        salir.setMnemonic(KeyEvent.VK_S);
        salir.addActionListener(e -> dispose());
        archivo.add(salir);

        JMenu ayuda = new JMenu("Ayuda");
        ayuda.setMnemonic(KeyEvent.VK_Y);
        JMenuItem sintaxis = new JMenuItem("Sintaxis");
        sintaxis.setMnemonic(KeyEvent.VK_S);
        sintaxis.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        sintaxis.addActionListener(e -> mostrarDialogo("Sintaxis", TEXTO_SINTAXIS));
        JMenuItem acercaDe = new JMenuItem("Acerca de");
        acercaDe.setMnemonic(KeyEvent.VK_C);
        acercaDe.addActionListener(e -> mostrarDialogo("Acerca de", TEXTO_ACERCA_DE));
        ayuda.add(sintaxis);
        ayuda.add(acercaDe);

        barra.add(archivo);
        barra.add(ayuda);
        return barra;
    }

    private JPanel crearCabecera() {
        // Encabezado azul noche con una línea de acento verde petróleo.
        JLabel logo = new JLabel(Estilo.iconoLogo());
        logo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(Estilo.etiqueta("Sistema de Integrales", Estilo.TITULO_APP, Color.WHITE));
        textos.add(Estilo.etiqueta("Integral indefinida, integral definida y área bajo la curva",
                Estilo.SUBTITULO_APP, Estilo.VERDE_SUAVE));

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(true);
        encabezado.setBackground(Estilo.NOCHE);
        encabezado.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 3, 0, Estilo.PETROLEO),
                BorderFactory.createEmptyBorder(16, 26, 16, 26)));
        encabezado.add(logo, BorderLayout.WEST);
        encabezado.add(textos, BorderLayout.CENTER);

        // Franja de aviso (solo la usa la vista previa).
        JLabel iconoAviso = new JLabel(Estilo.iconoAlerta(Estilo.NOCHE));
        iconoAviso.setVerticalAlignment(SwingConstants.TOP);
        franjaAviso.setOpaque(true);
        franjaAviso.setBackground(Estilo.CHAMPAGNE);
        franjaAviso.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, Estilo.CHAMPAGNE_BORDE),
                BorderFactory.createEmptyBorder(10, 26, 10, 26)));
        franjaAviso.add(iconoAviso, BorderLayout.WEST);
        franjaAviso.add(textoAviso, BorderLayout.CENTER);
        franjaAviso.setVisible(false);

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        cabecera.add(encabezado, BorderLayout.NORTH);
        cabecera.add(franjaAviso, BorderLayout.CENTER);
        return cabecera;
    }

    private JScrollPane crearCuerpo() {
        Cuerpo cuerpo = new Cuerpo();
        // El margen exterior de las tarjetas ya incluye el espacio de la sombra.
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTH;

        g.gridy = 0;
        cuerpo.add(panelEntrada, g);
        g.gridy = 1;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(4, 0, 0, 0);
        cuerpo.add(panelResultado, g);

        JScrollPane desplazamiento = new JScrollPane(cuerpo);
        desplazamiento.setBorder(BorderFactory.createEmptyBorder());
        desplazamiento.setOpaque(false);
        desplazamiento.getViewport().setBackground(Estilo.FONDO);
        desplazamiento.getVerticalScrollBar().setUI(new Estilo.BarraDesplazamientoUI());
        desplazamiento.getVerticalScrollBar().setOpaque(false);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(16);
        desplazamiento.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return desplazamiento;
    }

    private JPanel crearBarraEstado() {
        progreso.setIndeterminate(true);
        progreso.setBorderPainted(false);
        progreso.setPreferredSize(new Dimension(160, 6));
        progreso.setForeground(Estilo.PETROLEO);
        progreso.setBackground(Estilo.VERDE_MUY_SUAVE);
        progreso.setVisible(false);

        JPanel contenedorProgreso = new JPanel(new GridBagLayout());
        contenedorProgreso.setOpaque(false);
        contenedorProgreso.add(progreso);

        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(true);
        barra.setBackground(Estilo.TARJETA);
        barra.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, Estilo.BORDE_TARJETA),
                BorderFactory.createEmptyBorder(7, 26, 7, 26)));
        barra.add(etiquetaEstado, BorderLayout.CENTER);
        barra.add(contenedorProgreso, BorderLayout.EAST);
        return barra;
    }

    private void mostrarDialogo(String titulo, String texto) {
        JTextArea contenido = new JTextArea(texto);
        contenido.setEditable(false);
        contenido.setLineWrap(true);
        contenido.setWrapStyleWord(true);
        contenido.setOpaque(false);
        contenido.setFont(Estilo.TEXTO_NORMAL);
        contenido.setForeground(Estilo.TEXTO);
        contenido.setColumns(48);
        // Fijar un ancho permite calcular el alto con las líneas ya ajustadas.
        contenido.setSize(new Dimension(contenido.getPreferredSize().width, Short.MAX_VALUE));
        JOptionPane.showMessageDialog(this, contenido, titulo, JOptionPane.PLAIN_MESSAGE);
    }

    private void limpiarTodo() {
        panelEntrada.limpiar();
        panelResultado.limpiar();
        setEstado(ESTADO_LISTO);
    }

    // ------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------

    public PanelEntrada getPanelEntrada() {
        Estilo.verificarEDT();
        return panelEntrada;
    }

    public PanelResultado getPanelResultado() {
        Estilo.verificarEDT();
        return panelResultado;
    }

    /** Muestra u oculta el estado de cálculo y desactiva la entrada mientras dura. */
    public void setCalculando(boolean calculando) {
        Estilo.verificarEDT();
        this.calculando = calculando;
        panelEntrada.setHabilitado(!calculando);
        progreso.setVisible(calculando);
        etiquetaEstado.setText(calculando ? ESTADO_CALCULANDO : ESTADO_LISTO);
    }

    public boolean isCalculando() {
        Estilo.verificarEDT();
        return calculando;
    }

    /** Texto de la barra de estado. */
    public void setEstado(String texto) {
        Estilo.verificarEDT();
        etiquetaEstado.setText(texto == null || texto.isBlank() ? ESTADO_LISTO : texto);
    }

    /** Muestra una franja de aviso debajo del encabezado; {@code null} o vacío la oculta. */
    public void mostrarAviso(String texto) {
        Estilo.verificarEDT();
        boolean visible = texto != null && !texto.isBlank();
        textoAviso.setText(visible ? texto : "");
        franjaAviso.setVisible(visible);
        getContentPane().revalidate();
    }

    // ------------------------------------------------------------------

    /** Contenedor que se ajusta al ancho de la ventana y solo se desplaza en vertical. */
    private static final class Cuerpo extends JPanel implements Scrollable {

        private static final long serialVersionUID = 1L;

        Cuerpo() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
            return orientacion == SwingConstants.VERTICAL ? visible.height - 32 : visible.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    // ------------------------------------------------------------------

    static final String TEXTO_SINTAXIS = """
            CÓMO ESCRIBIR LA FUNCIÓN
            Use la variable x y los operadores  +  -  *  /  ^ (potencia).
            Multiplicación implícita:  3x y 2(x+1) se interpretan como 3*x y 2*(x+1).
            Decimales con punto (0.5), no con coma. Se aceptan mayúsculas.
            Límites a y b:  números entre -1000000 y 1000000. Se permiten a = b y a > b.

            QUÉ SE PUEDE INTEGRAR
            • Constantes y x:  5,  x
            • Potencias de x con exponente entero:  x^3,  x^-1,  x^-2
            • a^x con a numérico, también e:  2^x,  e^x,  exp(x)
            • a^u con u lineal:  2^(3*x),  e^(-x),  3^(2*x-1)
            • sin, cos, tan, asin, acos, atan, sinh, cosh, tanh, asinh, acosh, atanh, \
            con argumento exactamente x:  sin(x),  atanh(x)
            • Sumas y restas de las anteriores, su negación (-x, -sin(x)) y su producto \
            por una constante escrita a la izquierda:  3*x^2 + 2*cos(x)
            Las funciones trigonométricas usan radianes. Alias aceptados:  sen, sh, \
            arcsin, arccos, arctan, arcsinh, arccosh, arctanh.

            SE ESCRIBEN, PERO NO SE PUEDEN INTEGRAR
            ln (log) y la constante e sola son válidas al escribir, pero la base de \
            conocimiento no tiene reglas para integrarlas: ln(x) y e responden \
            «Integral no soportada». e^x y exp(x) sí se integran.

            OTRAS LIMITACIONES
            No se integran productos de funciones (x*sin(x)), funciones de un argumento \
            distinto de x (sin(2*x)), la constante a la derecha (sin(x)*3), divisiones \
            (1/x, x/2) ni potencias no enteras (x^0.5).""";

    static final String TEXTO_ACERCA_DE = """
            Sistema de solución de integrales

            Proyecto Final de Programación Declarativa — Ciclo 02/2026

            Interfaz: Java Swing
            Base de conocimiento: SWI-Prolog

            Integrantes: Henry, Erick y Rocío""";
}
