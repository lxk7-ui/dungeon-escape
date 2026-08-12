package com.example.dungeonescape.ui;

import com.example.dungeonescape.persistence.GoalDefinition;

import java.util.List;
import java.util.Objects;

/**
 * 关卡机制说明生成器：从关卡 JSON 的 {@link GoalDefinition} 递归推导
 * 简短机制文案（选关卡片使用）。
 *
 * <p>文案完全来自关卡数据，不硬编码任何目标语义；基础目标映射：
 * <ul>
 *   <li>EXIT → “到达出口”；</li>
 *   <li>TREASURE → “收集所有宝物”；</li>
 *   <li>SWITCHES → “覆盖所有机关”；</li>
 *   <li>AND → 子目标以“并”连接；OR → 子目标以“或”连接。</li>
 * </ul>
 */
public final class GoalSummary {

    private GoalSummary() {
    }

    /**
     * 生成目标的人类可读简短说明。
     *
     * @param goal 关卡目标定义（不允许为 null）
     * @throws IllegalArgumentException goal 为 null 或类型未知
     */
    public static String of(GoalDefinition goal) {
        if (goal == null) {
            throw new IllegalArgumentException("goal 不能为 null");
        }
        String type = goal.type();
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("目标类型缺失");
        }
        return switch (type) {
            case "EXIT" -> "到达出口";
            case "TREASURE" -> "收集所有宝物";
            case "SWITCHES" -> "覆盖所有机关";
            case "AND" -> join(goal.children(), "并");
            case "OR" -> join(goal.children(), "或");
            default -> throw new IllegalArgumentException("未知目标类型：" + type);
        };
    }

    /** 连接子目标说明；子目标缺失时按空串处理（由加载器先做完整性校验）。 */
    private static String join(List<GoalDefinition> children, String separator) {
        if (children == null || children.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(of(children.get(i)));
        }
        return sb.toString();
    }
}
