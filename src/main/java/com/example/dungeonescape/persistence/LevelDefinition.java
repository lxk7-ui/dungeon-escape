package com.example.dungeonescape.persistence;

import java.util.List;

/**
 * 关卡定义的 JSON 载体（DTO）。
 *
 * @param name     关卡名称
 * @param width    网格宽度（列数）
 * @param height   网格高度（行数）
 * @param entities 实体定义列表（含玩家）
 * @param goal     胜利目标定义
 */
public record LevelDefinition(String name, int width, int height,
                              List<EntityDefinition> entities, GoalDefinition goal) {
}
