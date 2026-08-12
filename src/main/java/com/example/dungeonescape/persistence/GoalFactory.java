package com.example.dungeonescape.persistence;

import com.example.dungeonescape.model.goal.AndGoal;
import com.example.dungeonescape.model.goal.ExitGoal;
import com.example.dungeonescape.model.goal.Goal;
import com.example.dungeonescape.model.goal.OrGoal;
import com.example.dungeonescape.model.goal.SwitchGoal;
import com.example.dungeonescape.model.goal.TreasureGoal;

import java.util.List;

/**
 * 将 {@link GoalDefinition} 转换为模型目标 {@link Goal} 的工厂。
 *
 * <p>按大写类型名映射：EXIT / TREASURE / SWITCHES / AND / OR。
 * 校验每种基础目标所需的实体存在（TREASURE 至少 1 个宝物、SWITCHES 至少 1 个机关、
 * EXIT 至少 1 个出口），复合目标至少 2 个非空子目标；
 * 创建 {@link TreasureGoal} 时按关卡初始宝物总数传入。
 */
public final class GoalFactory {

    private GoalFactory() {
    }

    /**
     * 依据定义与关卡实体列表创建目标（递归处理复合目标）。
     *
     * @param def      目标定义
     * @param source   关卡来源（用于错误消息）
     * @param entities 关卡实体定义列表（用于统计宝物/机关/出口数量）
     * @return 对应的模型目标
     * @throws InvalidLevelException 类型缺失/未知、所需实体缺失或子目标不合法
     */
    public static Goal create(GoalDefinition def, String source, List<EntityDefinition> entities) {
        if (def == null) {
            throw new InvalidLevelException(source, "目标不能为空");
        }
        String type = def.type();
        if (type == null || type.isBlank()) {
            throw new InvalidLevelException(source, "目标类型缺失");
        }
        return switch (type) {
            case "EXIT" -> {
                if (count(entities, "exit") < 1) {
                    throw new InvalidLevelException(source, "EXIT 目标需要至少 1 个出口实体");
                }
                yield new ExitGoal();
            }
            case "TREASURE" -> {
                long treasures = count(entities, "treasure");
                if (treasures < 1) {
                    throw new InvalidLevelException(source, "TREASURE 目标需要至少 1 个宝物实体");
                }
                yield new TreasureGoal((int) treasures);
            }
            case "SWITCHES" -> {
                if (count(entities, "switch") < 1) {
                    throw new InvalidLevelException(source, "SWITCHES 目标需要至少 1 个机关实体");
                }
                yield new SwitchGoal();
            }
            case "AND" -> new AndGoal(children(def, source, entities));
            case "OR" -> new OrGoal(children(def, source, entities));
            default -> throw new InvalidLevelException(source, "未知目标类型：" + type);
        };
    }

    /** 校验并递归创建复合目标的子目标；至少 2 个且均非空。 */
    private static Goal[] children(GoalDefinition def, String source, List<EntityDefinition> entities) {
        List<GoalDefinition> children = def.children();
        if (children == null || children.size() < 2) {
            throw new InvalidLevelException(source, def.type() + " 目标至少需要 2 个子目标");
        }
        Goal[] goals = new Goal[children.size()];
        for (int i = 0; i < children.size(); i++) {
            GoalDefinition child = children.get(i);
            if (child == null) {
                throw new InvalidLevelException(source, def.type() + " 目标第 " + (i + 1) + " 个子目标为 null");
            }
            goals[i] = create(child, source, entities);
        }
        return goals;
    }

    /** 统计实体定义列表中指定类型的数量。 */
    private static long count(List<EntityDefinition> entities, String type) {
        if (entities == null) {
            return 0;
        }
        return entities.stream().filter(d -> type.equals(d.type())).count();
    }
}
