package com.example.dungeonescape.model.entity;

import com.example.dungeonescape.model.Position;

/**
 * 巨石实体，阻挡移动；压住地板机关时可将其触发。
 */
public class Boulder extends Entity {

    /**
     * @param id       实体唯一标识
     * @param position 坐标
     */
    public Boulder(String id, Position position) {
        super(id, position, true);
    }
}
