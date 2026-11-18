package wampus;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.text.JTextComponent;

final class WorldPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int RUN_INTERVAL_MS = 430;
    private static final String KEY_HINT =
            "Click a room to inspect it. Map keys  ↑ Forward   ← / → Turn   Space Shoot   G Grab   E Exit";

    private final Agent agent = new Agent();
    private final CavePanel map = new CavePanel(this::inspect);
    private final JTextArea trace = textArea("Action trace");
    private final JTextArea facts = textArea("Knowledge base");
    private final JLabel status = Theme.label("", 15, Font.BOLD, Theme.MINT);
    private final JLabel counters = Theme.label("", 12, Font.PLAIN, Theme.MUTED);
    private final JLabel mapMode = Theme.label("", 12, Font.PLAIN, Theme.MUTED);
    private final JLabel hunter = Theme.label("", 13, Font.PLAIN, Theme.MUTED);
    private final JLabel inspectedRoom = Theme.label("", 15, Font.BOLD, Theme.TEXT);
    private final JLabel roomBeliefs = Theme.label("", 12, Font.PLAIN, Theme.MUTED);
    private final JLabel roomTruth = Theme.label("", 12, Font.PLAIN, Theme.MUTED);
    private final JLabel feedback = Theme.label(KEY_HINT, 12, Font.PLAIN, Theme.MUTED);
    private final JLabel[] signals = new JLabel[Percept.NAMES.size()];
    private final JSpinner gridSize = new JSpinner(new SpinnerNumberModel(4, Grid.MIN_SIZE, Grid.MAX_SIZE, 1));
    private final JTextField seed = new JTextField("42", 11);
    private final JToggleButton reveal = Theme.toggle("Reveal world");
    private final JButton follow = Theme.button("Follow hunter", false);
    private final JButton step = Theme.button("Step", true);
    private final JButton run = Theme.button("Auto run", false);
    private final Map<Game.Action, JButton> manual = new EnumMap<>(Game.Action.class);
    private final Timer timer = new Timer(RUN_INTERVAL_MS, event -> agentStep());
    private final CardLayout logTabs = new CardLayout();
    private final JPanel logPages = new JPanel(logTabs);

    private Game game;
    private String scenario;
    private Cell selected;
    private boolean blocked;

    WorldPanel() {
        super(new BorderLayout(0, 14));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 23, 12, 23));
        add(header(), BorderLayout.NORTH);

        JPanel workspace = transparent(new BorderLayout(14, 0));
        workspace.add(mapCard(), BorderLayout.CENTER);
        workspace.add(agentCards(), BorderLayout.EAST);
        add(workspace, BorderLayout.CENTER);
        add(controls(), BorderLayout.SOUTH);

        bind("UP", Game.Action.FORWARD);
        bind("LEFT", Game.Action.LEFT);
        bind("RIGHT", Game.Action.RIGHT);
        bind("SPACE", Game.Action.SHOOT);
        bind("G", Game.Action.GRAB);
        bind("E", Game.Action.EXIT);
        load(Cave.lesson(), "Lesson map");
    }

    private JPanel header() {
        JPanel heading = transparent(new BorderLayout(10, 0));
        JPanel names = vertical();
        names.add(Theme.label("World of Wampus", 30, Font.BOLD, Theme.TEXT));
        names.add(Theme.label("Knowledge-based cave agent", 13, Font.PLAIN, Theme.MUTED));
        heading.add(names, BorderLayout.CENTER);

        JPanel summary = transparent(new BorderLayout(0, 4));
        summary.setPreferredSize(new Dimension(245, 50));
        status.setHorizontalAlignment(JLabel.RIGHT);
        counters.setHorizontalAlignment(JLabel.RIGHT);
        summary.add(status, BorderLayout.NORTH);
        summary.add(counters, BorderLayout.CENTER);
        heading.add(summary, BorderLayout.EAST);

        JPanel row = transparent(new BorderLayout(8, 0));
        JPanel settings = transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton lesson = Theme.button("Lesson map", false);
        lesson.addActionListener(event -> {
            gridSize.setValue(4);
            load(Cave.lesson(), "Lesson map");
        });
        settings.add(lesson);

        JLabel sizeLabel = Theme.label("Size", 12, Font.BOLD, Theme.MUTED);
        sizeLabel.setLabelFor(gridSize);
        settings.add(sizeLabel);
        gridSize.setPreferredSize(new Dimension(63, 31));
        gridSize.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) gridSize.getEditor();
        Theme.styleField(editor.getTextField());
        editor.getTextField().setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 4));
        settings.add(gridSize);

        JLabel seedLabel = Theme.label("Seed", 12, Font.BOLD, Theme.MUTED);
        seedLabel.setLabelFor(seed);
        settings.add(seedLabel);
        Theme.styleField(seed);
        seed.setToolTipText("Use the same size and seed to reproduce a random cave.");
        seed.addActionListener(event -> loadRandom());
        settings.add(seed);

        JButton generate = Theme.button("Random map", false);
        generate.addActionListener(event -> loadRandom());
        settings.add(generate);
        JButton reset = Theme.button("Reset", false);
        reset.setToolTipText("Restart the current map with the same layout.");
        reset.addActionListener(event -> load(game.cave(), scenario));
        settings.add(reset);
        settings.setBorder(BorderFactory.createEmptyBorder(0, -8, 0, 0));
        row.add(settings, BorderLayout.CENTER);

        reveal.setToolTipText("Show the physical cave layout for inspection.");
        reveal.addActionListener(event -> {
            updateMap();
            updateInspector();
        });
        row.add(reveal, BorderLayout.EAST);

        JPanel top = transparent(new BorderLayout(0, 13));
        top.add(heading, BorderLayout.NORTH);
        top.add(row, BorderLayout.SOUTH);
        return top;
    }

    private JPanel mapCard() {
        JPanel card = Theme.surface(new BorderLayout(0, 5), 15);
        JPanel top = transparent(new BorderLayout());
        top.add(Theme.label("Cave map", 17, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        top.add(mapMode, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);
        card.add(map, BorderLayout.CENTER);

        JPanel legend = transparent(new FlowLayout(FlowLayout.LEFT, 12, 0));
        legend.add(Theme.label("●  Visited", 12, Font.PLAIN, Theme.BLUE));
        legend.add(Theme.label("✓  Safe", 12, Font.PLAIN, Theme.MINT));
        legend.add(Theme.label("?  Possible hazard", 12, Font.PLAIN, Theme.AMBER));
        legend.add(Theme.label("●  Confirmed hazard", 12, Font.PLAIN, Theme.ROSE));
        card.add(legend, BorderLayout.SOUTH);
        return card;
    }

    private JPanel agentCards() {
        JPanel right = transparent(new BorderLayout(0, 13));
        right.setPreferredSize(new Dimension(445, 0));

        JPanel senses = Theme.surface(new BorderLayout(0, 10), 13);
        JPanel titles = transparent(new BorderLayout(0, 4));
        titles.add(Theme.label("Hunter and percepts", 17, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        titles.add(hunter, BorderLayout.CENTER);
        senses.add(titles, BorderLayout.NORTH);
        JPanel chips = transparent(new GridLayout(1, signals.length, 6, 0));
        for (int i = 0; i < signals.length; i++) {
            JPanel chip = Theme.surface(new BorderLayout(0, 3), 6);
            JLabel label = Theme.label(Percept.NAMES.get(i), 11, Font.BOLD, Theme.MUTED);
            label.setHorizontalAlignment(JLabel.CENTER);
            chip.add(label, BorderLayout.NORTH);
            JLabel value = Theme.label("", 12, Font.BOLD, Theme.TEXT);
            value.setHorizontalAlignment(JLabel.CENTER);
            signals[i] = value;
            chip.add(value, BorderLayout.CENTER);
            chips.add(chip);
        }
        senses.add(chips, BorderLayout.CENTER);
        right.add(senses, BorderLayout.NORTH);

        JPanel logCard = Theme.surface(new BorderLayout(0, 9), 13);
        JPanel tabs = transparent(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tabs.setBorder(BorderFactory.createEmptyBorder(0, -6, 0, 0));
        ButtonGroup group = new ButtonGroup();
        JToggleButton traceTab = Theme.toggle("Action trace");
        JToggleButton factsTab = Theme.toggle("Knowledge base");
        group.add(traceTab);
        group.add(factsTab);
        traceTab.setSelected(true);
        traceTab.addActionListener(event -> logTabs.show(logPages, "trace"));
        factsTab.addActionListener(event -> logTabs.show(logPages, "facts"));
        tabs.add(traceTab);
        tabs.add(factsTab);
        logCard.add(tabs, BorderLayout.NORTH);
        logPages.add(scroll(trace), "trace");
        logPages.add(scroll(facts), "facts");
        logCard.add(logPages, BorderLayout.CENTER);
        right.add(logCard, BorderLayout.CENTER);

        JPanel inspector = Theme.surface(new BorderLayout(8, 5), 13);
        JPanel inspectorTop = transparent(new BorderLayout(8, 0));
        inspectorTop.add(inspectedRoom, BorderLayout.CENTER);
        inspectorTop.add(follow, BorderLayout.EAST);
        follow.addActionListener(event -> inspect(null));
        inspector.add(inspectorTop, BorderLayout.NORTH);
        JPanel details = transparent(new BorderLayout(0, 3));
        details.add(roomBeliefs, BorderLayout.NORTH);
        details.add(roomTruth, BorderLayout.CENTER);
        inspector.add(details, BorderLayout.CENTER);
        right.add(inspector, BorderLayout.SOUTH);
        return right;
    }

    private JPanel controls() {
        JPanel footer = transparent(new BorderLayout(0, 7));
        JPanel row = transparent(new FlowLayout(FlowLayout.LEFT, 7, 0));
        row.setBorder(BorderFactory.createEmptyBorder(0, -7, 0, 0));
        row.add(Theme.label("Agent", 12, Font.BOLD, Theme.MUTED));
        step.addActionListener(event -> agentStep());
        run.addActionListener(event -> toggleRun());
        row.add(step);
        row.add(run);

        JLabel manualLabel = Theme.label("Manual", 12, Font.BOLD, Theme.MUTED);
        manualLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        row.add(manualLabel);
        for (Game.Action action : List.of(Game.Action.LEFT, Game.Action.FORWARD, Game.Action.RIGHT,
                Game.Action.SHOOT, Game.Action.GRAB, Game.Action.EXIT)) {
            JButton button = Theme.button(action.label, false);
            button.addActionListener(event -> manualStep(action));
            manual.put(action, button);
            row.add(button);
        }
        footer.add(row, BorderLayout.NORTH);
        footer.add(feedback, BorderLayout.SOUTH);
        return footer;
    }

    private void loadRandom() {
        try {
            gridSize.commitEdit();
        } catch (ParseException error) {
            showFeedback("Enter a grid size between 2 and 10.", Theme.ROSE);
            return;
        }
        try {
            long number = Long.parseLong(seed.getText().strip());
            load(Cave.random((Integer) gridSize.getValue(), number), "Random map · Seed " + number);
        } catch (NumberFormatException error) {
            showFeedback("Enter a whole-number seed within the signed 64-bit range.", Theme.ROSE);
        }
    }

    private void load(Cave cave, String name) {
        timer.stop();
        game = new Game(cave);
        scenario = name;
        selected = null;
        blocked = false;
        reveal.setSelected(false);
        showFeedback(KEY_HINT, Theme.MUTED);
        Game.State state = game.state();
        trace.setText("");
        append("00  Entered " + state.room() + ", facing east.\n    " + state.percept().tuple()
                + "\n    Safe rooms  " + KnowledgeBase.listRooms(game.knowledge().beliefs().safe()) + "\n\n", false);
        refresh();
    }

    private void toggleRun() {
        if (timer.isRunning()) {
            timer.stop();
            refresh();
        } else {
            timer.start();
            agentStep();
        }
    }

    private void agentStep() {
        if (game.state().status() != Game.Status.ACTIVE || blocked) {
            timer.stop();
            refresh();
            return;
        }
        var decision = agent.decide(game.cave().grid(), game.state(), game.knowledge());
        if (decision.isEmpty()) {
            blocked = true;
            timer.stop();
            append("Stopped · No reachable safe unvisited room or guaranteed shot.\n"
                    + "    Manual controls can continue the run.\n", true);
            showFeedback("The agent stopped at an unresolved frontier. You can continue manually or reset.", Theme.AMBER);
            refresh();
            return;
        }
        apply(decision.get().action(), "Agent", decision.get().reason());
    }

    private void manualStep(Game.Action action) {
        if (!game.canApply(action)) {
            return;
        }
        timer.stop();
        blocked = false;
        apply(action, "Manual", "");
    }

    private void apply(Game.Action action, String actor, String reason) {
        KnowledgeBase.Beliefs before = game.knowledge().beliefs();
        Game.Result result = game.apply(action);
        if (!result.applied()) {
            timer.stop();
            showFeedback(result.message(), Theme.AMBER);
            refresh();
            return;
        }
        Game.State state = game.state();
        String output = String.format(Locale.ROOT, "%02d  %s · %s%n", state.actions(), actor, action.label);
        if (!reason.isBlank()) {
            output += "    " + reason + "\n";
        }
        output += "    " + result.message() + "\n";
        if (state.status() == Game.Status.ACTIVE) {
            output += "    " + state.percept().tuple() + "\n";
            String deductions = newDeductions(before, game.knowledge().beliefs());
            if (!deductions.isEmpty()) {
                output += "    " + deductions + "\n";
            }
        }
        append(output + "\n", true);
        if (state.status() != Game.Status.ACTIVE) {
            timer.stop();
            showFeedback(result.message(), state.status() == Game.Status.WON ? Theme.MINT : Theme.ROSE);
        } else {
            showFeedback(KEY_HINT, Theme.MUTED);
        }
        refresh();
    }

    private String newDeductions(KnowledgeBase.Beliefs before, KnowledgeBase.Beliefs after) {
        List<String> messages = new ArrayList<>();
        Set<Cell> safe = new HashSet<>(after.safe());
        safe.removeAll(before.safe());
        if (!safe.isEmpty()) {
            messages.add("New safe rooms " + KnowledgeBase.listRooms(safe) + ".");
        }
        Set<Cell> pits = new HashSet<>(after.pitCertain());
        pits.removeAll(before.pitCertain());
        if (!pits.isEmpty()) {
            messages.add("Confirmed pits " + KnowledgeBase.listRooms(pits) + ".");
        }
        if (after.wumpusPossible().size() == 1 && !before.wumpusPossible().equals(after.wumpusPossible())) {
            messages.add("Wumpus at " + KnowledgeBase.listRooms(after.wumpusPossible()) + ".");
        }
        return String.join(" ", messages);
    }

    private void refresh() {
        Game.State state = game.state();
        switch (state.status()) {
            case ACTIVE -> {
                status.setText(blocked ? "FRONTIER BLOCKED" : timer.isRunning() ? "AUTO EXPLORATION" : "EXPLORING");
                status.setForeground(blocked ? Theme.AMBER : Theme.MINT);
            }
            case WON -> {
                status.setText("ESCAPED WITH GOLD");
                status.setForeground(Theme.MINT);
            }
            case LOST -> {
                status.setText("HUNTER LOST");
                status.setForeground(Theme.ROSE);
            }
        }
        int actions = state.actions();
        int visited = game.knowledge().visited().size();
        counters.setText(actions + (actions == 1 ? " action" : " actions") + "  ·  "
                + visited + (visited == 1 ? " room visited" : " rooms visited"));
        hunter.setText(state.room() + "  ·  " + state.heading().display() + "  ·  "
                + (state.arrowAvailable() ? "Arrow ready" : "Arrow spent") + "  ·  "
                + (state.carryingGold() ? "Gold carried" : "No gold"));

        List<Boolean> activeSignals = state.percept().signals();
        for (int i = 0; i < signals.length; i++) {
            boolean active = activeSignals.get(i);
            signals[i].setText(active ? "ON" : "—");
            signals[i].setForeground(active ? Theme.AMBER : Theme.MUTED);
        }
        String updated = game.knowledge().describe();
        if (!updated.equals(facts.getText())) {
            facts.setText(updated);
            facts.setCaretPosition(0);
        }
        boolean canPlan = state.status() == Game.Status.ACTIVE && !blocked;
        step.setEnabled(canPlan && !timer.isRunning());
        run.setEnabled(canPlan);
        run.setText(timer.isRunning() ? "Pause" : "Auto run");
        manual.forEach((action, button) -> button.setEnabled(game.canApply(action)));
        updateMap();
        updateInspector();
    }

    private void updateMap() {
        int size = game.cave().grid().size();
        mapMode.setText(size + " × " + size + "  ·  " + (reveal.isSelected() ? "World view" : scenario));
        map.show(game, selected, reveal.isSelected());
    }

    private void inspect(Cell room) {
        selected = room;
        updateMap();
        updateInspector();
    }

    private void updateInspector() {
        Cell room = selected == null ? game.state().room() : selected;
        inspectedRoom.setText("Room " + room);
        follow.setEnabled(selected != null);
        KnowledgeBase.Beliefs beliefs = game.knowledge().beliefs();
        String pit = beliefs.pitCertain().contains(room) ? "certain"
                : beliefs.pitPossible().contains(room) ? "possible" : "ruled out";
        String wumpus = !beliefs.wumpusPossible().contains(room) ? "ruled out"
                : beliefs.wumpusPossible().size() == 1 ? "certain" : "possible";
        roomBeliefs.setText("Pit " + pit + "  ·  Live Wumpus " + wumpus);
        roomTruth.setVisible(reveal.isSelected());
        if (reveal.isSelected()) {
            roomTruth.setText("Actual contents  " + roomContents(room));
        }
    }

    private String roomContents(Cell room) {
        Cave cave = game.cave();
        if (room.equals(cave.grid().entrance())) {
            return "Entrance";
        }
        if (cave.pits().contains(room)) {
            return "Pit";
        }
        if (room.equals(cave.wumpus())) {
            return game.wumpusAlive() ? "Live Wumpus" : "Defeated Wumpus";
        }
        if (room.equals(cave.gold()) && !game.state().carryingGold()) {
            return "Gold";
        }
        return "Empty";
    }

    private void bind(String key, Game.Action action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), action);
        getActionMap().put(action, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                var focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (!(focused instanceof JTextComponent text) || !text.isEditable()) {
                    manualStep(action);
                }
            }
        });
    }

    private void append(String text, boolean scrollToEnd) {
        trace.append(text);
        trace.setCaretPosition(scrollToEnd ? trace.getDocument().getLength() : 0);
    }

    private void showFeedback(String message, Color color) {
        feedback.setText(message);
        feedback.setForeground(color);
    }

    private static JTextArea textArea(String accessibleName) {
        JTextArea area = new JTextArea();
        area.setFont(Theme.MONO);
        area.setBackground(Theme.INSET);
        area.setForeground(Theme.TEXT);
        area.setCaretColor(Theme.MINT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        area.getAccessibleContext().setAccessibleName(accessibleName);
        return area;
    }

    private static JScrollPane scroll(JTextArea area) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        Theme.styleScrollBar(scroll.getVerticalScrollBar());
        return scroll;
    }

    private static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static JPanel vertical() {
        JPanel panel = transparent(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
}
