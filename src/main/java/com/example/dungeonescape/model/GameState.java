package com.example.dungeonescape.model;

import com.example.dungeonescape.model.entity.Boulder;
import com.example.dungeonescape.model.entity.Entity;
import com.example.dungeonescape.model.entity.FloorSwitch;
import com.example.dungeonescape.model.entity.Player;
import com.example.dungeonescape.model.goal.Goal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 游戏局面的核心状态：网格尺寸、玩家、非玩家实体、目标、步数与状态。
 *
 * <p>纯后端模型，不依赖任何 UI 框架；实体列表对外只读。
 */
public class GameState {

    private final int width;
    private final int height;
    private final Player player;
    private final List<Entity> entities;
    private final Goal goal;
    private int moveCount;
    private GameStatus status;
    private final String levelName;

    /**
     * 构造完整游戏状态。
     *
     * @param width     网格宽度（列数）
     * @param height    网格高度（行数）
     * @param player    玩家实体
     * @param entities  非玩家实体（内部复制，不含玩家）
     * @param goal      胜利目标
     * @param moveCount 初始步数
     * @param status    初始游戏状态
     * @param levelName 关卡名称
     */
    public GameState(int width, int height, Player player, List<Entity> entities,
                     Goal goal, int moveCount, GameStatus status, String levelName) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width 和 height 必须为正数");
        }
        this.width = width;
        this.height = height;
        this.player = Objects.requireNonNull(player, "player");
        this.entities = new ArrayList<>(Objects.requireNonNull(entities, "entities"));
        this.goal = Objects.requireNonNull(goal, "goal");
        this.moveCount = moveCount;
        this.status = Objects.requireNonNull(status, "status");
        this.levelName = Objects.requireNonNull(levelName, "levelName");
    }

    /**
     * 便捷构造：步数为 0、状态为 {@link GameStatus#PLAYING}。
     */
    public GameState(int width, int height, Player player, List<Entity> entities,
                     Goal goal, String levelName) {
        this(width, height, player, entities, goal, 0, GameStatus.PLAYING, levelName);
    }

    /** 返回网格宽度（列数）。 */
    public int getWidth() {
        return width;
    }

    /** 返回网格高度（行数）。 */
    public int getHeight() {
        return height;
    }

    /** 返回玩家实体。 */
    public Player getPlayer() {
        return player;
    }

    /**
     * 返回非玩家实体的只读视图（不含玩家）。
     *
     * <p>视图结构不可修改；直接修改其中实体对象（如坐标）仍会反映到状态中。
     */
    public List<Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    /** 返回胜利目标。 */
    public Goal getGoal() {
        return goal;
    }

    /** 返回已走步数。 */
    public int getMoveCount() {
        return moveCount;
    }

    /** 步数加一（每移动一次调用）。 */
    public void incrementMoveCount() {
        moveCount++;
    }

    /** 返回当前游戏状态。 */
    public GameStatus getStatus() {
        return status;
    }

    /**
     * 设置游戏状态（供后续回合逻辑在胜利/失败时更新）。
     */
    public void setStatus(GameStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    /** 返回关卡名称。 */
    public String getLevelName() {
        return levelName;
    }

    /**
     * 判断坐标是否在网格内。
     */
    public boolean isInside(Position position) {
        return position.x() >= 0 && position.x() < width
                && position.y() >= 0 && position.y() < height;
    }

    /**
     * 返回指定位置上的所有非玩家实体（保持加入顺序，结果不可修改，可能为空）。
     */
    public List<Entity> entitiesAt(Position position) {
        return Collections.unmodifiableList(entities.stream()
                .filter(entity -> entity.getPosition().equals(position))
                .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * 返回指定位置上指定类型的实体；没有则为空。
     *
     * @param type 实体类型
     * @return 该位置上第一个匹配类型的实体
     */
    public <T extends Entity> Optional<T> entityAt(Position position, Class<T> type) {
        return entitiesAt(position).stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst();
    }

    /**
     * 判断位置是否阻挡移动：位置越界或存在阻挡实体（墙、巨石、关着的门等）即为阻挡。
     */
    public boolean isBlocked(Position position) {
        if (!isInside(position)) {
            return true;
        }
        return entitiesAt(position).stream().anyMatch(Entity::isBlocking);
    }

    /**
     * 从状态中移除实体。
     *
     * @param entity 待移除实体
     * @return 实体存在且移除成功时返回 true
     */
    public boolean removeEntity(Entity entity) {
        return entities.remove(entity);
    }

    /** 返回实体总数（不含玩家）。 */
    public int countEntities() {
        return entities.size();
    }

    /**
     * 返回指定类型的实体数量（不含玩家）。
     */
    public int countEntities(Class<? extends Entity> type) {
        return (int) entities.stream().filter(type::isInstance).count();
    }

    /**
     * 判断位置上的地板机关是否被巨石压住（即触发）。
     *
     * @return 该位置同时存在 {@link FloorSwitch} 与 {@link Boulder} 时为 true
     */
    public boolean isSwitchCovered(Position position) {
        return entityAt(position, FloorSwitch.class).isPresent()
                && entityAt(position, Boulder.class).isPresent();
    }
}
