package integrales.ui;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

/** Ejecuta código de prueba en el hilo de eventos de Swing y propaga sus errores. */
final class UtilEDT {

    @FunctionalInterface
    interface Bloque {
        void ejecutar() throws Exception;
    }

    private UtilEDT() {
    }

    static void enEDT(Bloque bloque) throws Exception {
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    bloque.ejecutar();
                } catch (Throwable t) {
                    fallo.set(t);
                }
            });
        } catch (InvocationTargetException e) {
            fallo.compareAndSet(null, e.getCause());
        }
        Throwable t = fallo.get();
        if (t instanceof Exception excepcion) {
            throw excepcion;
        }
        if (t instanceof Error error) {
            throw error;
        }
    }
}
