package com.example.dungeonescape.model.goal;

import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.entity.Treasure;

/**
 * 宝物目标：地图上剩余 {@link Treasure} 为 0 时满足。
 *
 * <p>构造时传入关卡初始宝物总数（与未来关卡加载器解析关卡时的统计口径一致，
 * 不使用任何全局状态），用于展示“已收集/总数”进度。
 */
public class TreasureGoal implements Goal {

    private final int totalTreasures;

    /**
     * @param totalTreasures 关卡初始宝物总数，须为非负数（0 表示关卡本就没有宝物）
     */
    public TreasureGoal(int totalTreasures) {
        if (totalTreasures < 0) {
            throw new IllegalArgumentException("宝物总数不能为负数：" + totalTreasures);
        }
        this.totalTreasures = totalTreasures;
    }

    /** 返回关卡初始宝物总数。 */
    public int getTotalTreasures() {
        return totalTreasures;
    }

    @Override
    public boolean isSatisfied(GameState gameState) {
        return countRemaining(gameState) == 0;
    }

    @Override
    public String description() {
        return "收集所有宝物";
    }

    @Override
    public String description(GameState gameState) {
        return "收集所有宝物（" + collectedCount(gameState) + "/" + totalTreasures + "）";
    }

    /** 地图上尚未收集的宝物数。 */
    private int countRemaining(GameState gameState) {
        return gameState.countEntities(Treasure.class);
    }

    /** 已收集宝物数：初始总数减去地图剩余数，防御性钳制在 [0, 总数]。 */
    private int collectedCount(GameState gameState) {
        return Math.max(0, totalTreasures - countRemaining(gameState));
    }
}
