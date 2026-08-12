package com.example.dungeonescape.service;

/**
 * 一次移动尝试的结果。
 *
 * @param moved             是否成功移动；成功移动（含开门、推石、拾取）为 true，
 *                          此时 {@link com.example.dungeonescape.model.GameState#incrementMoveCount()} 应被调用
 * @param outcome           结果分类，用于区分各种失败原因
 * @param doorOpened        本次移动是否打开了门
 * @param boulderPushed     本次移动是否推动了巨石
 * @param treasureCollected 本次移动是否拾取了宝物
 * @param keyCollected      本次移动是否拾取了钥匙
 */
public record MoveResult(boolean moved, MoveOutcome outcome,
                         boolean doorOpened, boolean boulderPushed,
                         boolean treasureCollected, boolean keyCollected) {

    /** 移动失败（不消耗步数）。 */
    public static MoveResult failed(MoveOutcome outcome) {
        return new MoveResult(false, outcome, false, false, false, false);
    }

    /** 游戏不在 PLAYING 状态，输入被忽略（不改变任何状态）。 */
    public static MoveResult notPlaying() {
        return new MoveResult(false, MoveOutcome.NOT_PLAYING, false, false, false, false);
    }
}
