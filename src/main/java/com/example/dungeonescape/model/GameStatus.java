package com.example.dungeonescape.model;

/**
 * 游戏回合进行状态。
 */
public enum GameStatus {

    /** 游戏进行中。 */
    PLAYING,
    /** 玩家达成目标，胜利。 */
    WON,
    /** 玩家失败（如被怪物击杀）。 */
    LOST
}
