package com.example.dungeonescape.model;

import java.util.Objects;

/**
 * 不可变的二维网格坐标，原点在左上角，x 向右、y 向下增大。
 *
 * @param x 横坐标（列）
 * @param y 纵坐标（行）
 */
public record Position(int x, int y) {

    /**
     * 返回沿指定方向移动一步后的新坐标（原坐标不变）。
     *
     * @param direction 移动方向
     * @return 移动后的新坐标
     */
    public Position move(Direction direction) {
        Objects.requireNonNull(direction, "direction");
        return new Position(x + direction.getDx(), y + direction.getDy());
    }
}
