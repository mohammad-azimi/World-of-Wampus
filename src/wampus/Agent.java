package wampus;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

final class Agent {
    record Decision(Game.Action action, String reason) {}

    Optional<Decision> decide(Grid grid, Game.State state, KnowledgeBase knowledge) {
        if (state.status() != Game.Status.ACTIVE) {
            return Optional.empty();
        }
        if (state.percept().glitter()) {
            return Optional.of(new Decision(Game.Action.GRAB, "Glitter places the gold in the current room."));
        }
        if (state.carryingGold() && state.room().equals(grid.entrance())) {
            return Optional.of(new Decision(Game.Action.EXIT, "The hunter is back at the entrance with the gold."));
        }

        KnowledgeBase.Beliefs beliefs = knowledge.beliefs();
        Map<Cell, Cell> routes = shortestRoutes(grid, state.room(), beliefs.safe());
        if (state.carryingGold()) {
            return Optional.of(moveToward(state, grid.entrance(), routes,
                    "Return to the entrance " + grid.entrance() + " through safe rooms."));
        }

        for (Cell room : routes.keySet()) {
            if (!knowledge.visited().contains(room)) {
                return Optional.of(moveToward(state, room, routes,
                        "Explore " + room + ", the nearest unvisited room proved safe."));
            }
        }

        if (state.arrowAvailable() && !beliefs.wumpusPossible().isEmpty()) {
            for (Cell room : routes.keySet()) {
                for (Direction direction : Grid.SEARCH_ORDER) {
                    if (grid.ray(room, direction).containsAll(beliefs.wumpusPossible())) {
                        String directionName = direction.display().toLowerCase(Locale.ROOT);
                        String reason = "All live Wumpus candidates lie on the " + directionName + " ray from " + room + ".";
                        if (state.room().equals(room)) {
                            Game.Action action = state.heading() == direction
                                    ? Game.Action.SHOOT : turnToward(state.heading(), direction);
                            return Optional.of(new Decision(action, reason));
                        }
                        return Optional.of(moveToward(state, room, routes, reason));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Map<Cell, Cell> shortestRoutes(Grid grid, Cell start, Set<Cell> safe) {
        Map<Cell, Cell> parent = new LinkedHashMap<>();
        Queue<Cell> queue = new ArrayDeque<>();
        parent.put(start, start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Cell room = queue.remove();
            for (Cell neighbor : grid.neighbors(room)) {
                if (safe.contains(neighbor) && !parent.containsKey(neighbor)) {
                    parent.put(neighbor, room);
                    queue.add(neighbor);
                }
            }
        }
        return parent;
    }

    private Decision moveToward(Game.State state, Cell target, Map<Cell, Cell> parent, String reason) {
        Cell next = target;
        while (!parent.get(next).equals(state.room())) {
            next = parent.get(next);
        }
        Direction desired = state.room().directionTo(next);
        Game.Action action = desired == state.heading()
                ? Game.Action.FORWARD : turnToward(state.heading(), desired);
        return new Decision(action, reason + " Next room " + next + ".");
    }

    private Game.Action turnToward(Direction heading, Direction desired) {
        return heading.left() == desired ? Game.Action.LEFT : Game.Action.RIGHT;
    }
}
