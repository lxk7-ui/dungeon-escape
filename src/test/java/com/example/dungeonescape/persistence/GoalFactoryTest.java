package com.example.dungeonescape.persistence;

import com.example.dungeonescape.model.goal.AndGoal;
import com.example.dungeonescape.model.goal.ExitGoal;
import com.example.dungeonescape.model.goal.Goal;
import com.example.dungeonescape.model.goal.OrGoal;
import com.example.dungeonescape.model.goal.SwitchGoal;
import com.example.dungeonescape.model.goal.TreasureGoal;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 目标工厂测试：四种基础目标、复合目标的构建、所需实体校验与来源错误消息。
 */
class GoalFactoryTest {

    private static final String SOURCE = "goal-factory-test";

    private static EntityDefinition entity(String type, String id) {
        return new EntityDefinition(id, type, 1, 1, null, null, false);
    }

    private static GoalDefinition goal(String type, GoalDefinition... children) {
        return new GoalDefinition(type, children.length == 0 ? null : List.of(children));
    }

    private static Goal create(GoalDefinition definition, EntityDefinition... entities) {
        return GoalFactory.create(definition, SOURCE, List.of(entities));
    }

    private static InvalidLevelException invalid(GoalDefinition definition, EntityDefinition... entities) {
        return assertThrows(InvalidLevelException.class, () -> create(definition, entities));
    }

    @Test
    void createsExitGoal() {
        Goal goal = create(goal("EXIT"), entity("exit", "exit-1"));
        assertInstanceOf(ExitGoal.class, goal);
    }

    @Test
    void createsTreasureGoalWithInitialTotal() {
        Goal goal = create(goal("TREASURE"),
                entity("treasure", "treasure-1"),
                entity("treasure", "treasure-2"),
                entity("treasure", "treasure-3"));
        assertInstanceOf(TreasureGoal.class, goal);
        assertEquals(3, ((TreasureGoal) goal).getTotalTreasures());
    }

    @Test
    void rejectsTreasureGoalWithoutTreasure() {
        InvalidLevelException e = invalid(goal("TREASURE"), entity("exit", "exit-1"));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("宝物"));
    }

    @Test
    void createsSwitchGoal() {
        Goal goal = create(goal("SWITCHES"), entity("switch", "switch-1"));
        assertInstanceOf(SwitchGoal.class, goal);
    }

    @Test
    void rejectsSwitchGoalWithoutSwitch() {
        InvalidLevelException e = invalid(goal("SWITCHES"), entity("exit", "exit-1"));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("机关"));
    }

    @Test
    void createsAndGoalWithTwoChildren() {
        Goal goal = create(goal("AND", goal("EXIT"), goal("TREASURE")),
                entity("exit", "exit-1"), entity("treasure", "treasure-1"));
        assertInstanceOf(AndGoal.class, goal);
        List<Goal> children = ((AndGoal) goal).getChildren();
        assertEquals(2, children.size());
        assertInstanceOf(ExitGoal.class, children.get(0));
        assertInstanceOf(TreasureGoal.class, children.get(1));
    }

    @Test
    void createsOrGoalWithTwoChildren() {
        Goal goal = create(goal("OR", goal("EXIT"), goal("SWITCHES")),
                entity("exit", "exit-1"), entity("switch", "switch-1"));
        assertInstanceOf(OrGoal.class, goal);
        assertEquals(2, ((OrGoal) goal).getChildren().size());
    }

    @Test
    void createsNestedCompositeGoal() {
        Goal goal = create(goal("AND", goal("EXIT"), goal("OR", goal("TREASURE"), goal("SWITCHES"))),
                entity("exit", "exit-1"),
                entity("treasure", "treasure-1"),
                entity("switch", "switch-1"));
        assertInstanceOf(AndGoal.class, goal);
        Goal or = ((AndGoal) goal).getChildren().get(1);
        assertInstanceOf(OrGoal.class, or);
        assertInstanceOf(TreasureGoal.class, ((OrGoal) or).getChildren().get(0));
        assertInstanceOf(SwitchGoal.class, ((OrGoal) or).getChildren().get(1));
    }

    @Test
    void rejectsAndWithSingleChild() {
        InvalidLevelException e = invalid(goal("AND", goal("EXIT")), entity("exit", "exit-1"));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("至少需要 2 个子目标"));
    }

    @Test
    void rejectsOrWithNullChildren() {
        InvalidLevelException e = invalid(new GoalDefinition("OR", null), entity("exit", "exit-1"));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("至少需要 2 个子目标"));
    }

    @Test
    void rejectsOrWithNullChild() {
        InvalidLevelException e = invalid(new GoalDefinition("OR", Arrays.asList(goal("EXIT"), null)),
                entity("exit", "exit-1"));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("子目标为 null"));
    }

    @Test
    void rejectsUnknownGoalType() {
        InvalidLevelException e = invalid(goal("ESCORT"), entity("exit", "exit-1"));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("ESCORT"));
        assertTrue(e.getMessage().contains("未知目标类型"));
    }

    @Test
    void rejectsMissingGoalType() {
        InvalidLevelException e = invalid(new GoalDefinition(null, null), entity("exit", "exit-1"));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("目标类型缺失"));
    }

    @Test
    void rejectsNullDefinition() {
        InvalidLevelException e = invalid(null, entity("exit", "exit-1"));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("目标不能为空"));
    }
}
