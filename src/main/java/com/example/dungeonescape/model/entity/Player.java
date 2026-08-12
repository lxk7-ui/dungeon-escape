package com.example.dungeonescape.model.entity;

import com.example.dungeonescape.model.Position;

/**
 * 玩家实体，可持有单把钥匙并累计宝物数量。
 */
public class Player extends Entity {

    private Integer key;       // 当前持有的钥匙 id；null 表示未持有钥匙
    private int treasureCount; // 已收集宝物数量

    /**
     * 构造玩家（不阻挡移动）。
     *
     * @param id       实体唯一标识
     * @param position 初始坐标
     */
    public Player(String id, Position position) {
        super(id, position, false);
    }

    /**
     * 返回当前持有的钥匙 id；未持有钥匙时返回 null。
     */
    public Integer getKey() {
        return key;
    }

    /** 返回玩家是否持有钥匙。 */
    public boolean hasKey() {
        return key != null;
    }

    /**
     * 设置玩家持有的钥匙。
     *
     * <p>玩家只能持有单把钥匙：新钥匙会替换旧钥匙，传 {@code null} 可清除钥匙。
     *
     * @param keyId 钥匙 id；为 null 表示清除
     */
    public void setKey(Integer keyId) {
        this.key = keyId;
    }

    /** 返回已收集宝物数量。 */
    public int getTreasureCount() {
        return treasureCount;
    }

    /** 收集一件宝物，宝物计数加一。 */
    public void addTreasure() {
        treasureCount++;
    }
}
