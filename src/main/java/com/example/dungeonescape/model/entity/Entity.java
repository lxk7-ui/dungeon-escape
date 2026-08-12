package com.example.dungeonescape.model.entity;

import com.example.dungeonescape.model.Position;

import java.util.Objects;

/**
 * 地牢中所有游戏实体的抽象基类。
 *
 * <p>每个实体拥有唯一标识 id、所在坐标 position 以及静态阻挡属性 blocking；
 * 子类可通过重写 {@link #isBlocking()} 提供动态阻挡行为（如 {@link Door} 随开合变化）。
 */
public abstract class Entity {

    private final String id;
    private Position position;
    private final boolean blocking;

    /**
     * 构造实体。
     *
     * @param id       实体唯一标识（非空且非空白字符串）
     * @param position 初始坐标
     * @param blocking 是否阻挡移动
     */
    protected Entity(String id, Position position, boolean blocking) {
        this.id = Objects.requireNonNull(id, "id");
        if (this.id.isBlank()) {
            throw new IllegalArgumentException("id 不能为空白字符串");
        }
        this.position = Objects.requireNonNull(position, "position");
        this.blocking = blocking;
    }

    /** 返回实体唯一标识。 */
    public String getId() {
        return id;
    }

    /** 返回实体当前坐标。 */
    public Position getPosition() {
        return position;
    }

    /** 设置实体坐标。 */
    public void setPosition(Position position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    /**
     * 返回该实体是否阻挡玩家移动。
     *
     * <p>默认返回构造时传入的静态属性；子类可重写以实现动态阻挡（如门开合）。
     */
    public boolean isBlocking() {
        return blocking;
    }
}
