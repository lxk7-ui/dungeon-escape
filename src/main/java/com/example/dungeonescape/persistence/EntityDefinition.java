package com.example.dungeonescape.persistence;

/**
 * 实体定义的 JSON 载体（DTO）。
 *
 * <p>type 为小写实体类型：player / wall / exit / treasure / key / door / boulder / switch。
 * x、y 可为 null（JSON 缺失）以支持缺失字段校验；keyId 仅对 key 有意义，
 * doorId 与 open（默认 false）仅对 door 有意义。
 *
 * @param id     实体唯一标识
 * @param type   实体类型（小写）
 * @param x      横坐标（可为 null 表示缺失）
 * @param y      纵坐标（可为 null 表示缺失）
 * @param keyId  钥匙编号（仅 key 使用）
 * @param doorId 门编号（仅 door 使用）
 * @param open   门初始是否打开（仅 door 使用，默认 false）
 */
public record EntityDefinition(String id, String type, Integer x, Integer y,
                               Integer keyId, Integer doorId, boolean open) {
}
