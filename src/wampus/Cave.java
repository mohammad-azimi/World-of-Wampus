package wampus;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

record Cave(Grid grid, Cell wumpus, Cell gold, Set<Cell> pits) {
    private static final double PIT_CHANCE = 0.18;

    Cave {
        pits = Set.copyOf(pits);
        if (!grid.contains(wumpus) || !grid.contains(gold) || !pits.stream().allMatch(grid::contains)) {
            throw new IllegalArgumentException("Every object must be inside the cave.");
        }
        Set<Cell> occupied = new HashSet<>(pits);
        if (!occupied.add(wumpus) || !occupied.add(gold) || !occupied.add(grid.entrance())) {
            throw new IllegalArgumentException("The entrance, hazards, and gold require distinct rooms.");
        }
    }

    static Cave lesson() {
        return new Cave(new Grid(4), new Cell(2, 1), new Cell(2, 2),
                Set.of(new Cell(1, 4), new Cell(2, 3), new Cell(4, 3)));
    }

    static Cave random(int size, long seed) {
        Grid grid = new Grid(size);
        List<Cell> available = grid.rooms();
        available.remove(grid.entrance());
        Random random = new Random(seed);
        Collections.shuffle(available, random);

        Cell wumpus = available.get(0);
        Cell gold = available.get(1);
        Set<Cell> pits = new HashSet<>();
        for (Cell room : available.subList(2, available.size())) {
            if (random.nextDouble() < PIT_CHANCE) {
                pits.add(room);
            }
        }
        if (pits.isEmpty()) {
            pits.add(available.get(2));
        }
        return new Cave(grid, wumpus, gold, pits);
    }
}
