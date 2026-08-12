package com.example.dungeonescape.model;

/**
 * 四方向移动枚举，携带每一步的坐标增量 (dx, dy)。
 */
public enum Direction {

    /** 上（y 减 1）。 */
    UP(0, -1),
    /** 下（y 加 1）。 */
    DOWN(0, 1),
    /** 左（x 减 1）。 */
    LEFT(-1, 0),
    /** 右（x 加 1）。 */
    RIGHT(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** 返回 x 轴增量。 */
    public int getDx() {
        return dx;
    }

    /** 返回 y 轴增量。 */
    public int getDy() {
        return dy;
    }
}
