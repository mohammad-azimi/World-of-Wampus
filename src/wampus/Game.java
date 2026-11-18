package wampus;

import java.util.List;
import java.util.Locale;

final class Game {
    enum Action {
        FORWARD("Forward"), LEFT("Turn left"), RIGHT("Turn right"),
        SHOOT("Shoot"), GRAB("Grab gold"), EXIT("Exit cave");

        final String label;

        Action(String label) {
            this.label = label;
        }
    }

    enum Status {ACTIVE, WON, LOST}

    record State(Cell room, Direction heading, boolean arrowAvailable, boolean carryingGold,
                 Status status, int actions, Percept percept) {}

    record Result(boolean applied, String message) {}

    private final Cave cave;
    private final KnowledgeBase knowledge;
    private Cell room;
    private Direction heading = Direction.EAST;
    private boolean wumpusAlive = true;
    private boolean arrowAvailable = true;
    private boolean carryingGold;
    private Status status = Status.ACTIVE;
    private int actions;
    private Percept percept;

    Game(Cave cave) {
        this.cave = cave;
        this.room = cave.grid().entrance();
        this.knowledge = new KnowledgeBase(cave.grid());
        this.percept = sense(false, false);
        knowledge.observe(room, percept);
    }

    Cave cave() {
        return cave;
    }

    KnowledgeBase knowledge() {
        return knowledge;
    }

    State state() {
        return new State(room, heading, arrowAvailable, carryingGold, status, actions, percept);
    }

    boolean wumpusAlive() {
        return wumpusAlive;
    }

    boolean canApply(Action action) {
        return status == Status.ACTIVE && switch (action) {
            case SHOOT -> arrowAvailable;
            case GRAB -> percept.glitter();
            case EXIT -> carryingGold && room.equals(cave.grid().entrance());
            default -> true;
        };
    }

    Result apply(Action action) {
        if (!canApply(action)) {
            return new Result(false, "Action unavailable in the current state.");
        }

        String message;
        boolean bump = false;
        boolean scream = false;
        switch (action) {
            case FORWARD -> {
                Cell next = room.translate(heading);
                if (!cave.grid().contains(next)) {
                    bump = true;
                    message = "Wall at " + next + ". Stayed in " + room + ".";
                } else {
                    room = next;
                    message = "Entered " + room + ".";
                    if (cave.pits().contains(room)) {
                        status = Status.LOST;
                        message = "The hunter fell into a pit at " + room + ".";
                    } else if (wumpusAlive && room.equals(cave.wumpus())) {
                        status = Status.LOST;
                        message = "The hunter met the live Wumpus at " + room + ".";
                    }
                }
            }
            case LEFT, RIGHT -> {
                heading = action == Action.LEFT ? heading.left() : heading.right();
                message = "Facing " + heading.display().toLowerCase(Locale.ROOT) + ".";
            }
            case SHOOT -> {
                arrowAvailable = false;
                scream = wumpusAlive && cave.grid().ray(room, heading).contains(cave.wumpus());
                if (scream) {
                    wumpusAlive = false;
                }
                knowledge.recordShot(room, heading, scream);
                message = scream ? "The arrow hit. A scream confirms the Wumpus is defeated."
                        : "The arrow missed. The shot rules out every room on its ray.";
            }
            case GRAB -> {
                carryingGold = true;
                message = "Picked up the gold in " + room + ".";
            }
            case EXIT -> {
                status = Status.WON;
                message = "The hunter returned to the entrance and escaped with the gold.";
            }
            default -> throw new IllegalStateException("Unknown action.");
        }

        actions++;
        percept = status == Status.LOST ? Percept.QUIET : sense(bump, scream);
        if (status == Status.ACTIVE) {
            knowledge.observe(room, percept);
        }
        return new Result(true, message);
    }

    private Percept sense(boolean bump, boolean scream) {
        List<Cell> neighbors = cave.grid().neighbors(room);
        boolean stench = wumpusAlive && neighbors.contains(cave.wumpus());
        boolean breeze = neighbors.stream().anyMatch(cave.pits()::contains);
        boolean glitter = !carryingGold && room.equals(cave.gold());
        return new Percept(stench, breeze, glitter, bump, scream);
    }
}
