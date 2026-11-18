package wampus;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Verification {
    private record Reading(Cell room, boolean breeze, Boolean liveStench) {}
    private record Shot(Cell origin, Direction heading, boolean hit) {}

    private Verification() {}

    public static void main(String[] args) {
        Game lesson = lessonDeductions();
        verifyActions(lesson);
        Game automatic = new Game(Cave.lesson());
        int lessonHits = runAgent(automatic);
        require(automatic.state().status() == Game.Status.WON, "Lesson policy must escape with gold");
        System.out.printf("Lesson agent: %d actions, %d distinct rooms, %d successful shots%n",
                automatic.state().actions(), automatic.knowledge().visited().size(), lessonHits);
        compareWithExhaustiveSearch();
        evaluateRandomCaves();
        System.out.println("All verification checks passed.");
    }

    private static Game lessonDeductions() {
        Game game = new Game(Cave.lesson());
        Cell start = new Cell(4, 1);
        Cell east = new Cell(4, 2);
        Cell north = new Cell(3, 1);
        Cell shared = new Cell(3, 2);
        Cell pit = new Cell(4, 3);
        require(game.state().room().equals(start) && game.state().percept().equals(Percept.QUIET), "Step 1 percept");
        require(game.knowledge().beliefs().safe().equals(Set.of(start, east, north)), "Step 1 safe rooms");
        Agent agent = new Agent();
        takeAgentAction(game, agent);
        require(game.state().room().equals(east), "The first action moves east");
        require(game.state().percept().equals(new Percept(false, true, false, false, false)), "Step 2 percept");
        require(game.knowledge().beliefs().pitSuspects().equals(Set.of(shared, pit)), "Step 2 pit alternatives");
        require(game.knowledge().beliefs().pitCertain().isEmpty(), "Step 2 has no confirmed pit");
        for (int count = 0; count < 5; count++) {
            takeAgentAction(game, agent);
        }
        require(game.state().room().equals(north) && game.state().heading() == Direction.NORTH, "Step 3 pose");
        require(game.state().percept().equals(new Percept(true, false, false, false, false)), "Step 3 percept");
        KnowledgeBase.Beliefs beliefs = game.knowledge().beliefs();
        require(beliefs.safe().contains(shared), "Step 3 confirms (3,2) is safe");
        require(beliefs.pitCertain().equals(Set.of(pit)), "Step 3 confirms the pit at (4,3)");
        require(beliefs.wumpusPossible().equals(Set.of(new Cell(2, 1))), "Step 3 confirms Wumpus at (2,1)");
        System.out.println("Lesson observations: (4,1) quiet; (4,2) breeze; (3,1) stench.");
        return game;
    }

    private static void verifyActions(Game lessonAtThirdReading) {
        Game hit = lessonAtThirdReading;
        require(hit.apply(Game.Action.SHOOT).applied(), "North-facing shot");
        require(hit.state().percept().scream() && !hit.state().percept().stench(), "Scream and cleared stench");
        require(!hit.state().arrowAvailable() && hit.knowledge().beliefs().wumpusPossible().isEmpty(),
                "No arrow or live Wumpus after hit");
        require(hit.knowledge().beliefs().safe().contains(new Cell(2, 1)), "Defeated Wumpus room becomes safe");
        hit.apply(Game.Action.FORWARD);
        require(hit.state().status() == Game.Status.ACTIVE && !hit.state().percept().scream(), "Scream lasts one action");

        Game miss = new Game(Cave.lesson());
        miss.apply(Game.Action.SHOOT);
        require(!miss.state().percept().scream() && miss.wumpusAlive(), "Miss does not create a scream");
        require(!miss.knowledge().beliefs().wumpusPossible().contains(new Cell(4, 4)), "Miss excludes the eastern ray");
        require(!miss.apply(Game.Action.SHOOT).applied() && miss.state().actions() == 1, "Second shot rejected");

        Game wall = new Game(Cave.lesson());
        apply(wall, Game.Action.RIGHT, Game.Action.RIGHT, Game.Action.FORWARD);
        require(wall.state().room().equals(new Cell(4, 1)) && wall.state().percept().bump(), "Wall bump");
        wall.apply(Game.Action.RIGHT);
        require(!wall.state().percept().bump(), "Bump lasts one action");

        Game pit = new Game(Cave.lesson());
        apply(pit, Game.Action.FORWARD, Game.Action.FORWARD);
        require(pit.state().status() == Game.Status.LOST && !pit.knowledge().visited().contains(new Cell(4, 3)),
                "A fatal pit gives no survival evidence");
        require(!pit.apply(Game.Action.LEFT).applied(), "Terminal state rejects actions");

        Game wumpus = new Game(Cave.lesson());
        apply(wumpus, Game.Action.LEFT, Game.Action.FORWARD, Game.Action.FORWARD);
        require(wumpus.state().status() == Game.Status.LOST, "Live Wumpus is lethal");

        Game gold = new Game(Cave.lesson());
        require(!gold.apply(Game.Action.GRAB).applied() && !gold.apply(Game.Action.EXIT).applied(), "Invalid gold and exit commands");
        apply(gold, Game.Action.LEFT, Game.Action.FORWARD, Game.Action.RIGHT, Game.Action.FORWARD,
                Game.Action.LEFT, Game.Action.FORWARD);
        require(gold.state().percept().glitter(), "Glitter in the gold room");
        gold.apply(Game.Action.GRAB);
        require(gold.state().carryingGold() && !gold.state().percept().glitter(), "Grab clears Glitter");
        require(!gold.apply(Game.Action.EXIT).applied(), "Exit requires the entrance");
        runAgent(gold);
        require(gold.state().status() == Game.Status.WON, "Automatic return after a manual pickup");
        System.out.println("Action checks: wall, pit and Wumpus loss, arrow hit and miss, gold and exit.");
    }

    private static void apply(Game game, Game.Action... actions) {
        for (Game.Action action : actions) {
            require(game.apply(action).applied(), "Expected a legal action " + action);
        }
    }

    private static void takeAgentAction(Game game, Agent agent) {
        Agent.Decision decision = agent.decide(game.cave().grid(), game.state(), game.knowledge()).orElseThrow();
        require(game.apply(decision.action()).applied(), "Lesson agent command");
    }

    private static int runAgent(Game game) {
        Grid grid = game.cave().grid();
        Agent agent = new Agent();
        int actionLimit = 3 * grid.size() * grid.size() * grid.size() * grid.size() + 4;
        int hits = 0;
        while (game.state().status() == Game.Status.ACTIVE) {
            Game.State state = game.state();
            require(state.actions() < actionLimit, "Agent must terminate or stop at a frontier");
            var decision = agent.decide(grid, state, game.knowledge());
            if (decision.isEmpty()) {
                break;
            }
            Game.Action action = decision.get().action();
            if (action == Game.Action.FORWARD) {
                Cell next = state.room().translate(state.heading());
                require(grid.contains(next) && game.knowledge().beliefs().safe().contains(next), "Safe move invariant");
            }
            if (action == Game.Action.SHOOT) {
                require(grid.ray(state.room(), state.heading()).contains(game.cave().wumpus()), "Guaranteed shot invariant");
                hits++;
            }
            require(game.apply(action).applied(), "Legal agent action");
            require(game.state().status() != Game.Status.LOST, "Safe policy must preserve the hunter");
        }
        return hits;
    }

    private static void evaluateRandomCaves() {
        System.out.println("Size,escaped,blocked,lost,successful_shots,max_actions");
        for (int size : List.of(2, 4, 6, 8, 10)) {
            int escaped = 0;
            int hits = 0;
            int maxActions = 0;
            for (int seed = 0; seed < 100; seed++) {
                Cave cave = Cave.random(size, seed);
                require(cave.equals(Cave.random(size, seed)), "Seed must reproduce the map");
                Game game = new Game(cave);
                hits += runAgent(game);
                if (game.state().status() == Game.Status.WON) {
                    escaped++;
                }
                maxActions = Math.max(maxActions, game.state().actions());
            }
            System.out.printf("%d,%d,%d,0,%d,%d%n", size, escaped, 100 - escaped, hits, maxActions);
        }
    }

    private static void compareWithExhaustiveSearch() {
        int comparisons = 0;
        for (int seed = 0; seed < 30; seed++) {
            Cave cave = Cave.random(3, seed);
            Grid grid = cave.grid();
            KnowledgeBase knowledge = new KnowledgeBase(grid);
            Cell start = grid.entrance();
            Reading first = read(cave, start, true);
            knowledge.observe(start, percept(first));
            compare(knowledge, grid, List.of(first), null);
            comparisons++;

            Direction heading = seed % 2 == 0 ? Direction.EAST : Direction.NORTH;
            boolean hit = grid.ray(start, heading).contains(cave.wumpus());
            Shot shot = new Shot(start, heading, hit);
            knowledge.recordShot(start, heading, hit);
            compare(knowledge, grid, List.of(first), shot);
            comparisons++;

            Cell secondRoom = grid.rooms().stream().filter(room -> !room.equals(start) && !cave.pits().contains(room)
                    && (hit || !room.equals(cave.wumpus()))).findFirst().orElseThrow();
            Reading second = read(cave, secondRoom, !hit);
            knowledge.observe(secondRoom, percept(second));
            compare(knowledge, grid, List.of(first, second), shot);
            comparisons++;
        }
        System.out.println("Exhaustive hazard-model comparisons on 3 x 3 grids: " + comparisons);
    }

    private static Reading read(Cave cave, Cell room, boolean wumpusAlive) {
        List<Cell> adjacent = cave.grid().neighbors(room);
        return new Reading(room, adjacent.stream().anyMatch(cave.pits()::contains),
                wumpusAlive ? adjacent.contains(cave.wumpus()) : null);
    }

    private static Percept percept(Reading reading) {
        return new Percept(Boolean.TRUE.equals(reading.liveStench()), reading.breeze(), false, false, false);
    }

    private static void compare(KnowledgeBase knowledge, Grid grid, List<Reading> readings, Shot shot) {
        List<Cell> rooms = grid.rooms();
        Set<Cell> possiblePits = new HashSet<>();
        Set<Cell> certainPits = new HashSet<>(rooms);
        Set<Cell> liveWumpus = new HashSet<>();
        int matching = 0;
        for (Cell wumpus : rooms) {
            if (wumpus.equals(grid.entrance()) || !matchesStench(grid, readings, wumpus)) {
                continue;
            }
            if (shot != null && grid.ray(shot.origin(), shot.heading()).contains(wumpus) != shot.hit()) {
                continue;
            }
            for (int mask = 0; mask < (1 << rooms.size()); mask++) {
                Set<Cell> pits = new HashSet<>();
                for (int bit = 0; bit < rooms.size(); bit++) {
                    if ((mask & (1 << bit)) != 0) {
                        pits.add(rooms.get(bit));
                    }
                }
                if (pits.contains(wumpus) || pits.contains(grid.entrance()) || !matchesBreeze(grid, readings, pits)) {
                    continue;
                }
                matching++;
                possiblePits.addAll(pits);
                certainPits.retainAll(pits);
                if (shot == null || !shot.hit()) {
                    liveWumpus.add(wumpus);
                }
            }
        }
        require(matching > 0, "Brute-force model must have a solution");
        Set<Cell> safe = new HashSet<>(rooms);
        safe.removeAll(possiblePits);
        safe.removeAll(liveWumpus);
        KnowledgeBase.Beliefs actual = knowledge.beliefs();
        Map<String, Boolean> matches = Map.of(
                "possible pits", possiblePits.equals(actual.pitPossible()),
                "certain pits", certainPits.equals(actual.pitCertain()),
                "live Wumpus candidates", liveWumpus.equals(actual.wumpusPossible()),
                "safe rooms", safe.equals(actual.safe())
        );
        matches.forEach((label, match) -> require(match, "Exhaustive comparison for " + label));
    }

    private static boolean matchesStench(Grid grid, List<Reading> readings, Cell wumpus) {
        return readings.stream().allMatch(reading -> reading.liveStench() == null
                || (!reading.room().equals(wumpus)
                && grid.neighbors(reading.room()).contains(wumpus) == reading.liveStench()));
    }

    private static boolean matchesBreeze(Grid grid, List<Reading> readings, Set<Cell> pits) {
        return readings.stream().allMatch(reading -> !pits.contains(reading.room())
                && grid.neighbors(reading.room()).stream().anyMatch(pits::contains) == reading.breeze());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
