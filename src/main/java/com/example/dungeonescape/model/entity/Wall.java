package com.example.dungeonescape.model.entity;

import com.example.dungeonescape.model.Position;

/**
 * 墙壁实体，阻挡移动。
 */
public class Wall extends Entity {

    /**
     * @param id       实体唯一标识
     * @param position 坐标
     */
    public Wall(String id, Position position) {
        super(id, position, true);
    }
}
