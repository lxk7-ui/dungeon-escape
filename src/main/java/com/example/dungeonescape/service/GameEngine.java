package com.example.dungeonescape.service;

import com.example.dungeonescape.model.Direction;
import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.GameStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 游戏引擎：编排回合逻辑（移动 → 计步 → 目标判定 → 胜利事件）。
 *
 * <p>只负责编排、状态与事件；具体的移动规则委托给 {@link MovementService}。
 * 纯后端服务，不依赖任何 UI 框架。
 */
public class GameEngine {

    private final GameState state;
    private final MovementService movementService;
    private final List<GameEventListener> listeners = new ArrayList<>();

    /**
     * @param state 游戏局面（引擎直接操作该对象）
     */
    public GameEngine(GameState state) {
        this(state, new MovementService());
    }

    /**
     * @param state           游戏局面（引擎直接操作该对象）
     * @param movementService 移动规则实现（便于测试注入）
     */
    public GameEngine(GameState state, MovementService movementService) {
        this.state = Objects.requireNonNull(state, "state");
        this.movementService = Objects.requireNonNull(movementService, "movementService");
    }

    /** 返回当前游戏局面。 */
    public GameState getState() {
        return state;
    }

    /** 注册游戏事件监听器。 */
    public void addEventListener(GameEventListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** 移除游戏事件监听器。 */
    public void removeEventListener(GameEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * 尝试让玩家沿指定方向移动一步。
     *
     * <p>仅在 {@link GameStatus#PLAYING} 时生效：成功移动（含开门、推石）计一步；
     * 移动后若目标达成则状态置为 {@link GameStatus#WON} 并通知监听器。
     * 非 PLAYING 状态下的输入被忽略，不改变任何状态。
     *
     * @param direction 移动方向
     * @return 本次移动的结果
     */
    public MoveResult move(Direction direction) {
        if (state.getStatus() != GameStatus.PLAYING) {
            return MoveResult.notPlaying();
        }

        MoveResult result = movementService.move(state, direction);
        if (result.moved()) {
            state.incrementMoveCount();
            if (state.getGoal().isSatisfied(state)) {
                state.setStatus(GameStatus.WON);
                notifyWon();
            }
        }
        return result;
    }

    private void notifyWon() {
        for (GameEventListener listener : List.copyOf(listeners)) {
            listener.onGameWon(state);
        }
    }
}
