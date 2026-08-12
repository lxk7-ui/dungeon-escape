package com.example.dungeonescape.model.entity;

import com.example.dungeonescape.model.Position;

/**
 * 出口实体，可通行；通常为胜利目标所在地。
 */
public class Exit extends Entity {

    /**
     * @param id       实体唯一标识
     * @param position 坐标
     */
    public Exit(String id, Position position) {
        super(id, position, false);
    }
}
