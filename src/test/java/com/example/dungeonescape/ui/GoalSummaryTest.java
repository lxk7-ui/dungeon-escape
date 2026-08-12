package com.example.dungeonescape.ui;

import com.example.dungeonescape.persistence.GoalDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link GoalSummary} 机制说明生成测试：文案完全来自关卡 JSON 的
 * {@link GoalDefinition} 结构，不硬编码目标语义。
 */
class GoalSummaryTest {

    @Test
    void exitGoalSummarizesAsReachExit() {
        assertEquals("到达出口", GoalSummary.of(new GoalDefinition("EXIT", null)));
    }

    @Test
    void treasureGoalSummarizesAsCollectAll() {
        assertEquals("收集所有宝物", GoalSummary.of(new GoalDefinition("TREASURE", null)));
    }

    @Test
    void switchesGoalSummarizesAsCoverAll() {
        assertEquals("覆盖所有机关", GoalSummary.of(new GoalDefinition("SWITCHES", null)));
    }

    @Test
    void andGoalJoinsChildrenWithAnd() {
        GoalDefinition goal = new GoalDefinition("AND", List.of(
                new GoalDefinition("EXIT", null),
                new GoalDefinition("TREASURE", null)));
        assertEquals("到达出口并收集所有宝物", GoalSummary.of(goal));
    }

    @Test
    void orGoalJoinsChildrenWithOr() {
        GoalDefinition goal = new GoalDefinition("OR", List.of(
                new GoalDefinition("TREASURE", null),
                new GoalDefinition("SWITCHES", null)));
        assertEquals("收集所有宝物或覆盖所有机关", GoalSummary.of(goal));
    }

    @Test
    void nestedAndOrMatchesLevel5Structure() {
        // level5.json: AND(EXIT, OR(TREASURE, SWITCHES))
        GoalDefinition goal = new GoalDefinition("AND", List.of(
                new GoalDefinition("EXIT", null),
                new GoalDefinition("OR", List.of(
                        new GoalDefinition("TREASURE", null),
                        new GoalDefinition("SWITCHES", null)))));
        assertEquals("到达出口并收集所有宝物或覆盖所有机关", GoalSummary.of(goal));
    }

    @Test
    void nullGoalIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> GoalSummary.of(null));
    }

    @Test
    void unknownTypeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> GoalSummary.of(new GoalDefinition("TOTALLY_UNKNOWN", null)));
    }
}
