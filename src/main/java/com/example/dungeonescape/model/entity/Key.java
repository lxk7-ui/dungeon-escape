package com.example.dungeonescape.model.entity;

import com.example.dungeonescape.model.Position;

/**
 * 钥匙实体，可拾取，不阻挡移动；拾取后玩家获得对应 keyId 的钥匙。
 */
public class Key extends Entity {

    private final int keyId;

    /**
     * @param id       实体唯一标识
     * @param position 坐标
     * @param keyId    钥匙编号（独立于实体 id 的数字编号）
     */
    public Key(String id, Position position, int keyId) {
        super(id, position, false);
        this.keyId = keyId;
    }

    /** 返回钥匙编号。 */
    public int getKeyId() {
        return keyId;
    }
}
