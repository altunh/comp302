package util;

import javax.swing.*;
import java.awt.*;

/**
 * Utility methods for Swing.
 * @since 1.0.0
 */
public class SwingUtils {

    /**
     * Search for a (grand) child component with the given name.
     *
     * @return a component; or {@code null}
     */
    @SuppressWarnings( "unchecked" )
    public static <T extends Component> T getComponentByName(Container parent, String name ) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName()))
                return (T) child;
            if (child instanceof Container) {
                T c = getComponentByName((Container) child, name);
                if (c != null)
                    return c;
            }
        }
        return null;
    }

    public static void setComponentEnabled(Container parent, boolean enabled) {
        for (Component child : parent.getComponents()) {
            child.setEnabled(enabled);
            if (child instanceof Container) {
                setComponentEnabled((Container) child, enabled);
            }
        }
    }

    public static void setComponentVisible(Container parent, boolean visible) {
        for (Component child : parent.getComponents()) {
            child.setVisible(visible);
            if (child instanceof Container) {
                setComponentVisible((Container) child, visible);
            }
        }
    }

    public static JPanel createYBoxedPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }


}
