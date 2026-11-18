package wampus;

import java.util.ArrayList;
import java.util.List;

record Grid(int size) {
    static final int MIN_SIZE = 2;
    static final int MAX_SIZE = 10;
    static final List<Direction> SEARCH_ORDER =
            List.of(Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST);

    Grid {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException("Grid size must be between 2 and 10.");
        }
    }

    Cell entrance() {
        return new Cell(size, 1);
    }

    boolean contains(Cell cell) {
        return cell.row() >= 1 && cell.row() <= size && cell.column() >= 1 && cell.column() <= size;
    }

    List<Cell> rooms() {
        List<Cell> rooms = new ArrayList<>(size * size);
        for (int row = 1; row <= size; row++) {
            for (int column = 1; column <= size; column++) {
                rooms.add(new Cell(row, column));
            }
        }
        return rooms;
    }

    List<Cell> neighbors(Cell cell) {
        List<Cell> neighbors = new ArrayList<>(4);
        for (Direction direction : SEARCH_ORDER) {
            Cell neighbor = cell.translate(direction);
            if (contains(neighbor)) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    List<Cell> ray(Cell origin, Direction direction) {
        List<Cell> rooms = new ArrayList<>(size - 1);
        for (Cell cell = origin.translate(direction); contains(cell); cell = cell.translate(direction)) {
            rooms.add(cell);
        }
        return rooms;
    }
}
