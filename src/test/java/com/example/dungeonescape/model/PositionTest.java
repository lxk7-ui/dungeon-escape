package com.example.dungeonescape.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link Position} 与 {@link Direction} 测试：方向移动与坐标增量。
 */
class PositionTest {

    @Test
    void moveAppliesDirectionDelta() {
        Position start = new Position(3, 4);

        assertEquals(new Position(3, 3), start.move(Direction.UP));
        assertEquals(new Position(3, 5), start.move(Direction.DOWN));
        assertEquals(new Position(2, 4), start.move(Direction.LEFT));
        assertEquals(new Position(4, 4), start.move(Direction.RIGHT));
    }

    @Test
    void moveReturnsNewPositionAndKeepsOriginalUnchanged() {
        Position start = new Position(1, 1);

        Position moved = start.move(Direction.RIGHT);

        assertEquals(new Position(1, 1), start);
        assertEquals(new Position(2, 1), moved);
    }

    @Test
    void directionsExposeExpectedDeltas() {
        assertEquals(0, Direction.UP.getDx());
        assertEquals(-1, Direction.UP.getDy());
        assertEquals(0, Direction.DOWN.getDx());
        assertEquals(1, Direction.DOWN.getDy());
        assertEquals(-1, Direction.LEFT.getDx());
        assertEquals(0, Direction.LEFT.getDy());
        assertEquals(1, Direction.RIGHT.getDx());
        assertEquals(0, Direction.RIGHT.getDy());
    }
}
