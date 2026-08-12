package com.example.dungeonescape.model.goal;

import com.example.dungeonescape.model.GameState;

/**
 * 游戏目标：判定当前局面是否达成胜利条件，并提供人类可读描述。
 */
public interface Goal {

    /**
     * 判断给定游戏局面是否已满足该目标。
     *
     * @param gameState 当前游戏局面
     * @return 目标达成时返回 true
     */
    boolean isSatisfied(GameState gameState);

    /** 返回目标的人类可读描述。 */
    String description();

    /**
     * 返回目标在给定局面下的人类可读描述，可包含实时进度
     * （如宝物收集进度、机关覆盖进度、组合目标的未完成清单）。
     *
     * <p>默认委托给无参 {@link #description()}；需要展示动态进度的目标应重写本方法。
     *
     * @param gameState 当前游戏局面
     */
    default String description(GameState gameState) {
        return description();
    }
}
