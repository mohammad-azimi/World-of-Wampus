package wampus;

enum Direction {
    NORTH(-1, 0), EAST(0, 1), SOUTH(1, 0), WEST(0, -1);

    final int rowOffset;
    final int columnOffset;

    Direction(int rowOffset, int columnOffset) {
        this.rowOffset = rowOffset;
        this.columnOffset = columnOffset;
    }

    Direction left() {
        return values()[(ordinal() + 3) % 4];
    }

    Direction right() {
        return values()[(ordinal() + 1) % 4];
    }

    String display() {
        return switch (this) {
            case NORTH -> "North";
            case EAST -> "East";
            case SOUTH -> "South";
            case WEST -> "West";
        };
    }
}
