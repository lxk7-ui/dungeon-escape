package com.example.dungeonescape.model.entity;

import com.example.dungeonescape.model.Position;

/**
 * 宝物实体，可通行；玩家拾取后累加宝物计数。
 */
public class Treasure extends Entity {

    /**
     * @param id       实体唯一标识
     * @param position 坐标
     */
    public Treasure(String id, Position position) {
        super(id, position, false);
    }
}
