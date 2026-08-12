package com.example.dungeonescape.model.goal;

import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.Position;
import com.example.dungeonescape.model.entity.Boulder;
import com.example.dungeonescape.model.entity.Entity;
import com.example.dungeonescape.model.entity.Exit;
import com.example.dungeonescape.model.entity.FloorSwitch;
import com.example.dungeonescape.model.entity.Player;
import com.example.dungeonescape.model.entity.Treasure;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阶段 3 目标系统测试：四种基础目标、组合目标、任意嵌套、
 * 历史无关性、描述进度与构造校验。
 */
class GoalTest {

    /** 构建 7x7 网格：玩家位于指定坐标。 */
    private static GameState state(Goal goal, Position playerPosition, Entity... entities) {
        return new GameState(7, 7, new Player("player", playerPosition),
                Arrays.asList(entities), goal, "目标测试");
    }

    /** 构建 7x7 网格：玩家位于 (3,3)。 */
    private static GameState state(Goal goal, Entity... entities) {
        return state(goal, new Position(3, 3), entities);
    }

    // ---------- 出口目标 ----------

    @Test
    void exitGoalSatisfiedOnlyWhilePlayerStandsOnExit() {
        Exit exit = new Exit("exit-2", new Position(4, 3));
        ExitGoal goal = new ExitGoal();
        GameState gameState = state(goal, exit);

        assertFalse(goal.isSatisfied(gameState));
        assertEquals("到达出口", goal.description());
        assertEquals("到达出口", goal.description(gameState)); // 默认委托给无参描述

        gameState.getPlayer().setPosition(new Position(4, 3));

        assertTrue(goal.isSatisfied(gameState));
    }

    @Test
    void exitGoalDoesNotTrackHistory() {
        Exit exit = new Exit("exit-2", new Position(4, 3));
        ExitGoal goal = new ExitGoal();
        GameState gameState = state(goal, exit);

        gameState.getPlayer().setPosition(new Position(4, 3)); // 曾站在出口
        gameState.getPlayer().setPosition(new Position(3, 3)); // 但已离开

        assertFalse(goal.isSatisfied(gameState));
    }

    // ---------- 宝物目标 ----------

    @Test
    void treasureGoalSatisfiedWhenNoTreasureRemains() {
        Treasure first = new Treasure("treasure-2", new Position(0, 0));
        Treasure second = new Treasure("treasure-3", new Position(0, 1));
        TreasureGoal goal = new TreasureGoal(2);
        GameState gameState = state(goal, first, second);

        assertFalse(goal.isSatisfied(gameState));
        assertEquals(2, goal.getTotalTreasures());
        assertEquals("收集所有宝物（0/2）", goal.description(gameState));

        gameState.removeEntity(first);
        assertFalse(goal.isSatisfied(gameState));
        assertEquals("收集所有宝物（1/2）", goal.description(gameState));

        gameState.removeEntity(second);
        assertTrue(goal.isSatisfied(gameState));
        assertEquals("收集所有宝物（2/2）", goal.description(gameState));
    }

    @Test
    void treasureGoalSatisfiedImmediatelyWhenLevelHasNoTreasure() {
        TreasureGoal goal = new TreasureGoal(0);
        GameState gameState = state(goal);

        assertTrue(goal.isSatisfied(gameState));
        assertEquals("收集所有宝物（0/0）", goal.description(gameState));
    }

    @Test
    void treasureGoalRejectsNegativeTotal() {
        assertThrows(IllegalArgumentException.class, () -> new TreasureGoal(-1));
    }

    // ---------- 机关目标 ----------

    @Test
    void switchGoalRequiresAllSwitchesCovered() {
        FloorSwitch first = new FloorSwitch("switch-2", new Position(4, 3));
        FloorSwitch second = new FloorSwitch("switch-3", new Position(5, 3));
        Boulder boulderA = new Boulder("boulder-4", new Position(0, 0));
        Boulder boulderB = new Boulder("boulder-5", new Position(0, 1));
        SwitchGoal goal = new SwitchGoal();
        GameState gameState = state(goal, first, second, boulderA, boulderB);

        assertFalse(goal.isSatisfied(gameState)); // 均未覆盖
        assertEquals("覆盖所有机关（0/2）", goal.description(gameState));

        boulderA.setPosition(new Position(4, 3)); // 部分覆盖
        assertFalse(goal.isSatisfied(gameState));
        assertEquals("覆盖所有机关（1/2）", goal.description(gameState));

        boulderB.setPosition(new Position(5, 3)); // 全部覆盖
        assertTrue(goal.isSatisfied(gameState));
        assertEquals("覆盖所有机关（2/2）", goal.description(gameState));

        boulderA.setPosition(new Position(6, 3)); // 移开巨石后不再满足
        assertFalse(goal.isSatisfied(gameState));
    }

    @Test
    void switchGoalNeverSatisfiedWithoutSwitches() {
        SwitchGoal goal = new SwitchGoal();
        GameState gameState = state(goal);

        assertFalse(goal.isSatisfied(gameState));
        assertEquals("覆盖所有机关（0/0）", goal.description(gameState));
    }

    // ---------- 且目标 ----------

    @Test
    void andGoalSatisfiedOnlyWhenAllChildrenAre() {
        Exit exit = new Exit("exit-2", new Position(4, 3));
        Treasure treasure = new Treasure("treasure-3", new Position(0, 0));
        AndGoal goal = new AndGoal(new ExitGoal(), new TreasureGoal(1));
        GameState gameState = state(goal, exit, treasure);

        assertFalse(goal.isSatisfied(gameState)); // 出口与宝物均未达成

        gameState.getPlayer().setPosition(new Position(4, 3));
        assertFalse(goal.isSatisfied(gameState)); // 站上出口但宝物未收集

        gameState.removeEntity(treasure);
        gameState.getPlayer().setPosition(new Position(3, 3));
        assertFalse(goal.isSatisfied(gameState)); // 宝物收齐但不在出口

        gameState.getPlayer().setPosition(new Position(4, 3));
        assertTrue(goal.isSatisfied(gameState)); // 全部满足
    }

    @Test
    void andGoalDescriptionCombinesProgressAndUnfinished() {
        Exit exit = new Exit("exit-2", new Position(4, 3));
        Treasure treasure = new Treasure("treasure-3", new Position(0, 0));
        AndGoal goal = new AndGoal(new ExitGoal(), new TreasureGoal(1));
        GameState gameState = state(goal, exit, treasure);

        String description = goal.description(gameState);
        assertTrue(description.startsWith("完成所有目标（0/2）：未完成——"));
        assertTrue(description.contains("到达出口"));
        assertTrue(description.contains("收集所有宝物（0/1）"));

        gameState.removeEntity(treasure);
        gameState.getPlayer().setPosition(new Position(4, 3));

        assertEquals("完成所有目标（2/2）", goal.description(gameState));
    }

    // ---------- 或目标 ----------

    @Test
    void orGoalSatisfiedWhenAnyChildIs() {
        Exit exit = new Exit("exit-2", new Position(4, 3));
        Treasure treasure = new Treasure("treasure-3", new Position(0, 0));
        OrGoal goal = new OrGoal(new ExitGoal(), new TreasureGoal(1));
        GameState gameState = state(goal, exit, treasure);

        assertFalse(goal.isSatisfied(gameState));

        gameState.getPlayer().setPosition(new Position(4, 3)); // 出口分支满足
        assertTrue(goal.isSatisfied(gameState));

        gameState.getPlayer().setPosition(new Position(3, 3));
        gameState.removeEntity(treasure); // 宝物分支满足
        assertTrue(goal.isSatisfied(gameState));
    }

    @Test
    void orGoalDescriptionCombinesProgressAndUnfinished() {
        Exit exit = new Exit("exit-2", new Position(4, 3));
        Treasure treasure = new Treasure("treasure-3", new Position(0, 0)); // 宝物流在地图，宝物分支未满足
        OrGoal goal = new OrGoal(new ExitGoal(), new TreasureGoal(1));
        GameState gameState = state(goal, exit, treasure);

        String description = goal.description(gameState);
        assertTrue(description.startsWith("满足任一目标（0/2）：未完成——"));
        assertTrue(description.contains("到达出口"));
        assertTrue(description.contains("收集所有宝物（0/1）"));

        gameState.getPlayer().setPosition(new Position(4, 3));

        assertEquals("满足任一目标（1/2）", goal.description(gameState));
    }

    // ---------- 嵌套组合 ----------

    @Test
    void nestedExitAndTreasureOrSwitches() {
        // EXIT AND (TREASURE OR SWITCHES)
        Goal goal = new AndGoal(new ExitGoal(),
                new OrGoal(new TreasureGoal(2), new SwitchGoal()));
        Exit exit = new Exit("exit-2", new Position(4, 3));
        Treasure first = new Treasure("treasure-3", new Position(0, 0));
        Treasure second = new Treasure("treasure-4", new Position(0, 1));
        GameState gameState = state(goal, exit, first, second);

        // 站在出口但宝物未收齐且无机关：OR 分支均不满足
        gameState.getPlayer().setPosition(new Position(4, 3));
        assertFalse(goal.isSatisfied(gameState));

        // 收齐宝物后：OR 的宝物分支满足，整体满足
        gameState.removeEntity(first);
        gameState.removeEntity(second);
        assertTrue(goal.isSatisfied(gameState));

        // 宝物仍在但机关全部被压住：OR 的机关分支满足，整体满足
        GameState switchState = state(goal, exit, first, second,
                new Boulder("boulder-5", new Position(4, 4)),
                new FloorSwitch("switch-6", new Position(4, 4)));
        switchState.getPlayer().setPosition(new Position(4, 3));
        assertTrue(goal.isSatisfied(switchState));
    }

    @Test
    void steppingOnExitBeforeTreasureDoesNotCount() {
        // 先踩出口再离开，随后完成宝物收集：仍未满足
        Goal goal = new AndGoal(new ExitGoal(), new TreasureGoal(1));
        Exit exit = new Exit("exit-2", new Position(4, 3));
        Treasure treasure = new Treasure("treasure-3", new Position(0, 0));
        GameState gameState = state(goal, exit, treasure);

        gameState.getPlayer().setPosition(new Position(4, 3)); // 踩上出口（宝物未收集）
        assertFalse(goal.isSatisfied(gameState));

        gameState.getPlayer().setPosition(new Position(3, 3)); // 离开出口
        gameState.removeEntity(treasure);                       // 宝物收集完成
        assertFalse(goal.isSatisfied(gameState));               // 踩过出口不算数

        gameState.getPlayer().setPosition(new Position(4, 3)); // 再次站上出口
        assertTrue(goal.isSatisfied(gameState));
    }

    // ---------- 构造校验与防御性拷贝 ----------

    @Test
    void compositeGoalsRejectFewerThanTwoChildren() {
        assertThrows(IllegalArgumentException.class, () -> new AndGoal());
        assertThrows(IllegalArgumentException.class, () -> new AndGoal(new ExitGoal()));
        assertThrows(IllegalArgumentException.class, () -> new OrGoal());
        assertThrows(IllegalArgumentException.class, () -> new OrGoal(new ExitGoal()));
    }

    @Test
    void compositeGoalsRejectNullChildren() {
        assertThrows(IllegalArgumentException.class,
                () -> new AndGoal(new ExitGoal(), null));
        assertThrows(IllegalArgumentException.class,
                () -> new OrGoal(new ExitGoal(), null));
    }

    @Test
    void childrenListCannotBeModifiedExternally() {
        AndGoal andGoal = new AndGoal(new ExitGoal(), new TreasureGoal(1));
        OrGoal orGoal = new OrGoal(new ExitGoal(), new TreasureGoal(1));

        assertThrows(UnsupportedOperationException.class,
                () -> andGoal.getChildren().add(new ExitGoal()));
        assertThrows(UnsupportedOperationException.class,
                () -> andGoal.getChildren().remove(0));
        assertThrows(UnsupportedOperationException.class,
                () -> orGoal.getChildren().clear());
    }

    @Test
    void compositeGoalDefensivelyCopiesConstructorInput() {
        ExitGoal exitGoal = new ExitGoal();
        TreasureGoal treasureGoal = new TreasureGoal(1);
        Goal[] input = {exitGoal, treasureGoal};

        AndGoal goal = new AndGoal(input);
        input[0] = null; // 外部修改原数组不影响已构造的目标

        assertEquals(List.of(exitGoal, treasureGoal), goal.getChildren());
    }
}
