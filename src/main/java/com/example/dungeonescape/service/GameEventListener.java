package com.example.dungeonescape.service;

import com.example.dungeonescape.model.GameState;

/**
 * 游戏事件监听器：接收引擎在回合推进过程中产生的事件。
 */
public interface GameEventListener {

    /**
     * 游戏胜利时被调用（玩家达成目标、状态变为 {@code WON} 之后）。
     *
     * @param gameState 达成胜利时的游戏局面
     */
    void onGameWon(GameState gameState);
}
