package com.example.dungeonescape.persistence;

import java.util.List;

/**
 * 目标定义的 JSON 载体（DTO）。
 *
 * <p>type 为大写目标类型：EXIT / TREASURE / SWITCHES / AND / OR；
 * 复合目标（AND / OR）通过 children 递归嵌套子目标。
 *
 * @param type     目标类型（大写）
 * @param children 子目标列表（仅 AND / OR 使用）
 */
public record GoalDefinition(String type, List<GoalDefinition> children) {
}
