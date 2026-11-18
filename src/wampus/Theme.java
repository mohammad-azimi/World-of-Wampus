package wampus;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

final class Theme {
    static final Color BACKGROUND = new Color(0x111820);
    static final Color PANEL = new Color(0x19232E);
    static final Color INSET = new Color(0x131C25);
    static final Color LINE = new Color(0x344451);
    static final Color TEXT = new Color(0xEDF3F7);
    static final Color MUTED = new Color(0xAEC0CD);
    static final Color MINT = new Color(0x90E0C3);
    static final Color AMBER = new Color(0xF4CE88);
    static final Color ROSE = new Color(0xF6A5AF);
    static final Color BLUE = new Color(0xAFCBFA);
    static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private Theme() {}

    static void install() {
        UIManager.put("control", PANEL);
        UIManager.put("text", TEXT);
        UIManager.put("TextField.background", INSET);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", MINT);
        UIManager.put("TextField.font", MONO);
        UIManager.put("ScrollBar.width", 12);
    }

    static JLabel label(String text, int size, int weight, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(BODY.deriveFont(weight, (float) size));
        label.setForeground(color);
        return label;
    }

    static JPanel surface(java.awt.LayoutManager layout, int padding) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(padding, padding, padding, padding)));
        return panel;
    }

    static void styleField(JTextField field) {
        field.setFont(MONO);
        field.setBackground(INSET);
        field.setForeground(TEXT);
        field.setCaretColor(MINT);
        field.setSelectionColor(LINE);
        field.setSelectedTextColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(6, 9, 6, 9)));
    }

    static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        styleButton(button, primary);
        return button;
    }

    static JToggleButton toggle(String text) {
        JToggleButton button = new JToggleButton(text);
        styleButton(button, false);
        return button;
    }

    static void styleScrollBar(JScrollBar bar) {
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                trackColor = INSET;
                thumbColor = LINE;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return emptyArrow();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return emptyArrow();
            }

            private JButton emptyArrow() {
                JButton hidden = new JButton();
                hidden.setPreferredSize(new Dimension(0, 0));
                hidden.setFocusable(false);
                return hidden;
            }

            @Override
            protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
                if (bounds.isEmpty() || !component.isEnabled()) {
                    return;
                }
                graphics.setColor(isThumbRollover() ? MUTED : LINE);
                graphics.fillRoundRect(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4, 7, 7);
            }
        });
    }

    private static void styleButton(AbstractButton button, boolean primary) {
        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics graphics, JComponent component) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean enabled = button.isEnabled();
                boolean emphasized = enabled && (primary || button.isSelected());
                Color fill = emphasized ? MINT : INSET;
                if (enabled && button.getModel().isRollover()) {
                    fill = emphasized ? MINT.brighter() : PANEL.brighter();
                }
                if (enabled && button.getModel().isPressed()) {
                    fill = fill.darker();
                }
                g.setColor(fill);
                g.fillRoundRect(0, 0, component.getWidth() - 1, component.getHeight() - 1, 9, 9);
                g.setColor(button.isFocusOwner() ? BLUE : emphasized ? fill : LINE);
                g.drawRoundRect(0, 0, component.getWidth() - 1, component.getHeight() - 1, 9, 9);
                g.dispose();
                super.paint(graphics, component);
            }

            @Override
            protected void paintText(Graphics graphics, AbstractButton item, Rectangle bounds, String text) {
                if (!item.isEnabled()) {
                    graphics.setColor(MUTED.darker());
                } else {
                    graphics.setColor(primary || item.isSelected() ? BACKGROUND : TEXT);
                }
                graphics.drawString(text, bounds.x, bounds.y + graphics.getFontMetrics().getAscent());
            }
        });
        button.setFont(BODY.deriveFont(Font.BOLD));
        button.setMargin(new Insets(7, 13, 7, 13));
        button.setBorder(BorderFactory.createEmptyBorder(7, 13, 7, 13));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
