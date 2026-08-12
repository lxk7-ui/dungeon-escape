package com.example.dungeonescape.service;

/**
 * 一次移动尝试的详细结果分类。
 *
 * <p>与 {@link MoveResult#moved()} 不同：{@code MOVED} 之外的分类均表示
 * 移动失败、不消耗步数；每个失败原因单独成类，便于调用方区分规则边界。
 */
public enum MoveOutcome {

    /** 成功移动并进入目标格（可能包含开门、推石与拾取），消耗一步。 */
    MOVED,
    /** 目标格越出地图边界。 */
    OUT_OF_BOUNDS,
    /** 目标格为墙或其他无法通过的阻挡物。 */
    WALL,
    /** 目标格为巨石且无法推动（推出边界、推入墙/关闭的门/另一巨石）。 */
    BOULDER_UNMOVABLE,
    /** 目标格为关闭的门，玩家未持有编号匹配的钥匙。 */
    DOOR_LOCKED,
    /** 游戏不在 PLAYING 状态，输入被忽略。 */
    NOT_PLAYING
}
