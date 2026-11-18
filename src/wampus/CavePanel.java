package wampus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.function.Consumer;
import javax.swing.JPanel;

final class CavePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private enum Mark {
        UNKNOWN("", 0x202D3A, Theme.MUTED),
        CLEAR("CLEAR", 0x202D3A, Theme.MUTED),
        VISITED("VISITED", 0x243950, Theme.BLUE),
        SAFE("SAFE", 0x1E4039, Theme.MINT),
        PIT_RISK("PIT?", 0x473D2C, Theme.AMBER),
        WUMPUS_RISK("W?", 0x473D2C, Theme.AMBER),
        BOTH_RISKS("PIT? W?", 0x473D2C, Theme.AMBER),
        PIT("PIT", 0x4B2A34, Theme.ROSE),
        WUMPUS("WUMPUS", 0x4B2A34, Theme.ROSE),
        DEFEATED("DEFEATED", 0x243950, Theme.BLUE),
        GOLD("GOLD", 0x493A25, Theme.AMBER);

        final String label;
        final Color fill;
        final Color ink;

        Mark(String label, int fill, Color ink) {
            this.label = label;
            this.fill = new Color(fill);
            this.ink = ink;
        }
    }

    private record Layout(int x, int y, int roomSize, int gap) {
        int stride() {
            return roomSize + gap;
        }
    }

    private Game game;
    private Cell inspected;
    private boolean revealed;

    CavePanel(Consumer<Cell> onSelect) {
        setBackground(Theme.PANEL);
        setFocusable(true);
        getAccessibleContext().setAccessibleName("Cave map");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                Cell clicked = cellAt(event.getPoint());
                if (clicked != null) {
                    onSelect.accept(clicked);
                }
            }
        });
    }

    void show(Game game, Cell inspected, boolean revealed) {
        this.game = game;
        this.inspected = inspected;
        this.revealed = revealed;
        repaint();
    }

    private Layout gridLayout() {
        int count = game.cave().grid().size();
        int gap = count >= 8 ? 4 : 7;
        int available = Math.min(getWidth() - 44, getHeight() - 38);
        int roomSize = Math.max(1, (available - (count - 1) * gap) / count);
        int extent = count * roomSize + (count - 1) * gap;
        return new Layout((getWidth() - extent + 18) / 2, (getHeight() - extent + 18) / 2,
                roomSize, gap);
    }

    private Cell cellAt(Point point) {
        if (game == null) {
            return null;
        }
        Layout layout = gridLayout();
        int x = point.x - layout.x();
        int y = point.y - layout.y();
        if (x < 0 || y < 0 || x % layout.stride() >= layout.roomSize() || y % layout.stride() >= layout.roomSize()) {
            return null;
        }
        Cell room = new Cell(y / layout.stride() + 1, x / layout.stride() + 1);
        return game.cave().grid().contains(room) ? room : null;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (game == null) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Layout layout = gridLayout();
        Grid grid = game.cave().grid();
        Game.State state = game.state();
        g.setFont(Theme.MONO);
        g.setColor(Theme.MUTED);
        for (int index = 0; index < grid.size(); index++) {
            String number = Integer.toString(index + 1);
            int middle = index * layout.stride() + layout.roomSize() / 2;
            centered(g, number, layout.x() + middle, layout.y() - 11);
            centered(g, number, layout.x() - 15, layout.y() + middle + 4);
        }
        for (Cell room : grid.rooms()) {
            int x = layout.x() + (room.column() - 1) * layout.stride();
            int y = layout.y() + (room.row() - 1) * layout.stride();
            paintRoom(g, room, state, x, y, layout.roomSize());
        }
        g.dispose();
    }

    private Mark mark(Cell room, Game.State state) {
        if (revealed) {
            Cave cave = game.cave();
            if (cave.pits().contains(room)) {
                return Mark.PIT;
            }
            if (cave.wumpus().equals(room)) {
                return game.wumpusAlive() ? Mark.WUMPUS : Mark.DEFEATED;
            }
            if (cave.gold().equals(room) && !state.carryingGold()) {
                return Mark.GOLD;
            }
            return game.knowledge().visited().contains(room) ? Mark.VISITED : Mark.CLEAR;
        }
        KnowledgeBase.Beliefs beliefs = game.knowledge().beliefs();
        if (game.knowledge().visited().contains(room)) {
            return Mark.VISITED;
        }
        if (beliefs.safe().contains(room)) {
            return Mark.SAFE;
        }
        if (beliefs.pitCertain().contains(room)) {
            return Mark.PIT;
        }
        if (beliefs.wumpusPossible().size() == 1 && beliefs.wumpusPossible().contains(room)) {
            return Mark.WUMPUS;
        }
        boolean pit = beliefs.pitSuspects().contains(room);
        boolean wumpus = beliefs.wumpusSuspects().contains(room);
        if (pit && wumpus) {
            return Mark.BOTH_RISKS;
        }
        if (pit) {
            return Mark.PIT_RISK;
        }
        return wumpus ? Mark.WUMPUS_RISK : Mark.UNKNOWN;
    }

    private void paintRoom(Graphics2D g, Cell room, Game.State state, int x, int y, int size) {
        Mark mark = mark(room, state);
        boolean hunter = state.room().equals(room);
        boolean lost = state.status() == Game.Status.LOST;
        Color ink = hunter ? (lost ? Theme.ROSE : Theme.MINT) : mark.ink;
        if (hunter) {
            g.setColor(lost ? Mark.PIT.fill : new Color(0x20483F));
        } else {
            g.setColor(mark.fill);
        }
        int curve = Math.min(13, size / 4);
        g.fillRoundRect(x, y, size, size, curve, curve);
        boolean selected = room.equals(inspected);
        g.setStroke(new BasicStroke(selected || hunter ? 2f : 1f));
        g.setColor(selected ? Theme.TEXT : hunter ? ink : Theme.LINE);
        g.drawRoundRect(x, y, size, size, curve, curve);

        if (size >= 62) {
            g.setFont(Theme.MONO.deriveFont(11f));
            g.setColor(Theme.MUTED);
            g.drawString(room.toString(), x + 8, y + 15);
            if (room.equals(game.cave().grid().entrance())) {
                String entrance = "HOME";
                g.drawString(entrance, x + size - 8 - g.getFontMetrics().stringWidth(entrance), y + 15);
            }
        }
        int cx = x + size / 2;
        int cy = y + size / 2;
        g.setColor(ink);
        String label = hunter ? (lost ? "FALLEN" : "HUNTER") : mark.label;
        if (size >= 57) {
            paintSymbol(g, mark, state, hunter, cx, cy - 3, Math.min(23, size / 5));
            g.setColor(ink);
            g.setFont(Theme.BODY.deriveFont(Font.BOLD, size >= 62 ? 11f : 9f));
            centered(g, label, cx, y + size - 12);
        } else if (size >= 16) {
            paintSymbol(g, mark, state, hunter, cx, cy, Math.min(14, size / 3));
        }
    }

    private void paintSymbol(Graphics2D g, Mark mark, Game.State state, boolean hunter, int cx, int cy, int radius) {
        if (hunter) {
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            Graphics2D arrow = (Graphics2D) g.create();
            arrow.translate(cx, cy);
            arrow.rotate(state.heading().ordinal() * Math.PI / 2);
            Path2D pointer = new Path2D.Double();
            pointer.moveTo(0, -radius * .70);
            pointer.lineTo(radius * .51, radius * .52);
            pointer.lineTo(0, radius * .25);
            pointer.lineTo(-radius * .51, radius * .52);
            pointer.closePath();
            arrow.setColor(Theme.BACKGROUND);
            arrow.fill(pointer);
            arrow.dispose();
            return;
        }
        g.setStroke(new BasicStroke(radius >= 16 ? 2.4f : 1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (mark) {
            case SAFE -> {
                g.drawLine(cx - radius / 2, cy, cx - radius / 6, cy + radius / 3);
                g.drawLine(cx - radius / 6, cy + radius / 3, cx + radius / 2, cy - radius / 3);
            }
            case VISITED -> g.fillOval(cx - 3, cy - 3, 6, 6);
            case PIT -> {
                g.fillOval(cx - radius, cy - radius / 2, radius * 2, radius);
                g.setColor(mark.fill);
                g.fillOval(cx - radius / 2, cy - radius / 4, radius, radius / 2);
            }
            case GOLD -> {
                int[] px = {cx, cx + radius, cx, cx - radius};
                int[] py = {cy - radius, cy, cy + radius, cy};
                g.fillPolygon(px, py, 4);
            }
            case WUMPUS, DEFEATED, PIT_RISK, WUMPUS_RISK, BOTH_RISKS -> {
                String symbol = switch (mark) {
                    case WUMPUS, DEFEATED -> "W";
                    case PIT_RISK -> "P?";
                    case WUMPUS_RISK -> "W?";
                    default -> "?";
                };
                g.setFont(Theme.MONO.deriveFont(Font.BOLD, (float) Math.max(12, radius + 4)));
                centered(g, symbol, cx, cy + g.getFontMetrics().getAscent() / 3);
            }
            case UNKNOWN, CLEAR -> {
                g.setColor(Theme.LINE);
                g.drawLine(cx - 4, cy, cx + 4, cy);
            }
        }
    }

    private void centered(Graphics2D g, String text, int x, int baseline) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }
}
