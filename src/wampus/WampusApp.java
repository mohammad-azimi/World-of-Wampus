package wampus;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public final class WampusApp {
    private WampusApp() {}

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("World of Wampus needs a desktop session to open its Swing window.");
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            JFrame frame = new JFrame("World of Wampus");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setContentPane(new WorldPanel());
            frame.setMinimumSize(new Dimension(960, 660));
            Rectangle desktop = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            frame.setSize(Math.min(1200, desktop.width), Math.min(820, desktop.height));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
