package com.example.dungeonescape.model.entity;

import com.example.dungeonescape.model.Position;

/**
 * 地板机关实体，可通行；被巨石压住时视为触发（见 GameState.isSwitchCovered）。
 */
public class FloorSwitch extends Entity {

    /**
     * @param id       实体唯一标识
     * @param position 坐标
     */
    public FloorSwitch(String id, Position position) {
        super(id, position, false);
    }
}
