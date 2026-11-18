package wampus;

record Cell(int row, int column) implements Comparable<Cell> {
    Cell translate(Direction direction) {
        return new Cell(row + direction.rowOffset, column + direction.columnOffset);
    }

    Direction directionTo(Cell neighbor) {
        for (Direction direction : Direction.values()) {
            if (translate(direction).equals(neighbor)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Rooms must be orthogonal neighbors.");
    }

    @Override
    public int compareTo(Cell other) {
        int byRow = Integer.compare(row, other.row);
        return byRow != 0 ? byRow : Integer.compare(column, other.column);
    }

    @Override
    public String toString() {
        return "(" + row + "," + column + ")";
    }
}
