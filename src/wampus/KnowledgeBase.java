package wampus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class KnowledgeBase {
    record Beliefs(Set<Cell> pitPossible, Set<Cell> pitCertain, Set<Cell> wumpusPossible,
                   Set<Cell> safe, Set<Cell> pitSuspects, Set<Cell> wumpusSuspects) {
        Beliefs {
            pitPossible = Set.copyOf(pitPossible);
            pitCertain = Set.copyOf(pitCertain);
            wumpusPossible = Set.copyOf(wumpusPossible);
            safe = Set.copyOf(safe);
            pitSuspects = Set.copyOf(pitSuspects);
            wumpusSuspects = Set.copyOf(wumpusSuspects);
        }
    }

    private record Shot(Cell origin, Direction direction, boolean hit) {}

    private final Grid grid;
    private final Map<Cell, Boolean> breezes = new LinkedHashMap<>();
    private final Map<Cell, Boolean> liveStenches = new LinkedHashMap<>();
    private Shot shot;
    private Beliefs beliefs;

    KnowledgeBase(Grid grid) {
        this.grid = grid;
        infer();
    }

    void observe(Cell room, Percept percept) {
        if (!breezes.containsKey(room)) {
            breezes.put(room, percept.breeze());
            if (wumpusAlive()) {
                liveStenches.put(room, percept.stench());
            }
            infer();
        }
    }

    void recordShot(Cell origin, Direction direction, boolean hit) {
        if (shot != null) {
            throw new IllegalStateException("The hunter may fire a single arrow.");
        }
        shot = new Shot(origin, direction, hit);
        infer();
    }

    boolean wumpusAlive() {
        return shot == null || !shot.hit();
    }

    Set<Cell> visited() {
        return Collections.unmodifiableSet(breezes.keySet());
    }

    Beliefs beliefs() {
        return beliefs;
    }

    private void infer() {
        Set<Cell> noPit = new HashSet<>(visited());
        noPit.add(grid.entrance());
        for (var observation : breezes.entrySet()) {
            if (!observation.getValue()) {
                noPit.addAll(grid.neighbors(observation.getKey()));
            }
        }

        List<Set<Cell>> pitClauses = new ArrayList<>();
        for (var observation : breezes.entrySet()) {
            if (observation.getValue()) {
                Set<Cell> options = new HashSet<>(grid.neighbors(observation.getKey()));
                options.removeAll(noPit);
                pitClauses.add(options);
            }
        }

        Set<Cell> potentialPits = new HashSet<>(grid.rooms());
        potentialPits.removeAll(noPit);
        Set<Cell> certainPits = new HashSet<>(potentialPits);
        Set<Cell> feasibleWumpus = new HashSet<>();
        for (Cell wumpusRoom : stenchCandidates()) {
            Set<Cell> forced = forcedPits(pitClauses, wumpusRoom);
            if (forced == null) {
                continue;
            }
            feasibleWumpus.add(wumpusRoom);
            certainPits.retainAll(forced);
        }
        if (feasibleWumpus.isEmpty()) {
            throw new IllegalStateException("The observations contradict the cave rules.");
        }

        Set<Cell> possiblePits = new HashSet<>(potentialPits);
        if (feasibleWumpus.size() == 1) {
            possiblePits.removeAll(feasibleWumpus);
        }
        Set<Cell> liveWumpus = wumpusAlive() ? feasibleWumpus : Set.of();
        Set<Cell> safe = new HashSet<>(grid.rooms());
        safe.removeAll(possiblePits);
        safe.removeAll(liveWumpus);

        Set<Cell> suspectedPits = new HashSet<>();
        pitClauses.forEach(suspectedPits::addAll);
        suspectedPits.retainAll(possiblePits);
        Set<Cell> suspectedWumpus = liveStenches.containsValue(true) ? liveWumpus : Set.of();
        beliefs = new Beliefs(possiblePits, certainPits, liveWumpus, safe, suspectedPits, suspectedWumpus);
    }

    private Set<Cell> stenchCandidates() {
        Set<Cell> candidates = new HashSet<>(grid.rooms());
        candidates.remove(grid.entrance());
        for (var observation : liveStenches.entrySet()) {
            Cell room = observation.getKey();
            candidates.remove(room);
            if (observation.getValue()) {
                candidates.retainAll(grid.neighbors(room));
            } else {
                candidates.removeAll(grid.neighbors(room));
            }
        }
        if (shot != null) {
            List<Cell> ray = grid.ray(shot.origin(), shot.direction());
            if (shot.hit()) {
                candidates.retainAll(ray);
            } else {
                candidates.removeAll(ray);
            }
        }
        return candidates;
    }

    // After fixing a Wumpus room, the remaining pit clauses contain only positive literals.
    // An empty clause rejects that location, while a singleton forces its remaining room to be a pit.
    private Set<Cell> forcedPits(List<Set<Cell>> clauses, Cell wumpusRoom) {
        Set<Cell> forced = new HashSet<>();
        for (Set<Cell> clause : clauses) {
            int available = clause.size() - (clause.contains(wumpusRoom) ? 1 : 0);
            if (available == 0) {
                return null;
            }
            if (available == 1) {
                clause.stream().filter(room -> !room.equals(wumpusRoom)).findFirst().ifPresent(forced::add);
            }
        }
        return forced;
    }

    String describe() {
        StringBuilder text = new StringBuilder("CURRENT CONCLUSIONS\n")
                .append("Safe rooms  ").append(listRooms(beliefs.safe())).append('\n')
                .append("Confirmed pits  ").append(listRooms(beliefs.pitCertain())).append('\n')
                .append("Live Wumpus candidates  ")
                .append(wumpusAlive() ? listRooms(beliefs.wumpusPossible()) : "Wumpus defeated").append('\n')
                .append("Pits indicated by breeze  ").append(listRooms(beliefs.pitSuspects())).append('\n')
                .append("\nOBSERVED ROOMS\n");
        for (var observation : breezes.entrySet()) {
            Cell room = observation.getKey();
            Boolean stench = liveStenches.get(room);
            text.append(room).append("  ")
                    .append(observation.getValue() ? "Breeze" : "No breeze").append("  |  ")
                    .append(stench == null ? "After scream" : stench ? "Stench" : "No stench")
                    .append('\n');
        }
        text.append("\nBREEZE SENTENCES\n");
        for (var observation : breezes.entrySet()) {
            List<Cell> neighbors = grid.neighbors(observation.getKey());
            text.append(observation.getKey()).append("  ");
            if (observation.getValue()) {
                List<Cell> candidates = neighbors.stream().filter(beliefs.pitPossible()::contains).toList();
                text.append("Pit in at least one of ").append(listRooms(candidates));
            } else {
                text.append("No pit in ").append(listRooms(neighbors));
            }
            text.append('\n');
        }
        if (shot != null) {
            text.append("\nARROW EVIDENCE\n").append(shot.direction().display()).append(" from ")
                    .append(shot.origin()).append(shot.hit() ? " produced a scream." : " missed.")
                    .append('\n');
        }
        return text.toString();
    }

    static String listRooms(Iterable<Cell> rooms) {
        List<Cell> sorted = new ArrayList<>();
        rooms.forEach(sorted::add);
        if (sorted.isEmpty()) {
            return "none";
        }
        return sorted.stream().sorted().map(Cell::toString).collect(Collectors.joining(", "));
    }
}
