package com.example.dungeonescape.model.entity;

import com.example.dungeonescape.model.Position;

/**
 * 门实体：关闭时阻挡移动，打开后不再阻挡。
 *
 * <p>阻挡行为由 {@link #isBlocking()} 依据 open 状态动态决定，而非构造时的静态属性。
 */
public class Door extends Entity {

    private final int doorId;
    private boolean open;

    /**
     * @param id       实体唯一标识
     * @param position 坐标
     * @param doorId   门编号（独立于实体 id 的数字编号，须与对应钥匙编号匹配）
     * @param open     初始是否打开
     */
    public Door(String id, Position position, int doorId, boolean open) {
        super(id, position, false); // 阻挡与否由 open 动态决定
        this.doorId = doorId;
        this.open = open;
    }

    /** 返回门编号。 */
    public int getDoorId() {
        return doorId;
    }

    /** 返回门是否打开。 */
    public boolean isOpen() {
        return open;
    }

    /** 设置门开合状态。 */
    public void setOpen(boolean open) {
        this.open = open;
    }

    /** 门打开时不阻挡移动，关闭时阻挡。 */
    @Override
    public boolean isBlocking() {
        return !open;
    }
}
