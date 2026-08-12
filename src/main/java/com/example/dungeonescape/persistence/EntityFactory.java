package com.example.dungeonescape.persistence;

import com.example.dungeonescape.model.Position;
import com.example.dungeonescape.model.entity.Boulder;
import com.example.dungeonescape.model.entity.Door;
import com.example.dungeonescape.model.entity.Entity;
import com.example.dungeonescape.model.entity.Exit;
import com.example.dungeonescape.model.entity.FloorSwitch;
import com.example.dungeonescape.model.entity.Key;
import com.example.dungeonescape.model.entity.Player;
import com.example.dungeonescape.model.entity.Treasure;
import com.example.dungeonescape.model.entity.Wall;

/**
 * 将 {@link EntityDefinition} 转换为模型实体 {@link Entity} 的工厂。
 *
 * <p>按小写类型名映射到对应实体；key 缺少 keyId、door 缺少 doorId、
 * 未知类型等结构性错误统一抛出携带来源的 {@link InvalidLevelException}。
 */
public final class EntityFactory {

    private EntityFactory() {
    }

    /**
     * 依据定义创建实体。
     *
     * @param def    实体定义
     * @param source 关卡来源（用于错误消息）
     * @return 对应的模型实体
     * @throws InvalidLevelException 定义缺失、id 空白、缺少坐标、缺少 keyId/doorId 或类型未知
     */
    public static Entity create(EntityDefinition def, String source) {
        if (def == null) {
            throw new InvalidLevelException(source, "实体定义不能为空");
        }
        if (def.id() == null || def.id().isBlank()) {
            throw new InvalidLevelException(source, "实体 id 不能为空白");
        }
        if (def.x() == null || def.y() == null) {
            throw new InvalidLevelException(source, "实体 " + def.id() + " 缺少坐标");
        }
        Position position = new Position(def.x(), def.y());
        String type = def.type();
        return switch (type == null ? "" : type) {
            case "player" -> new Player(def.id(), position);
            case "wall" -> new Wall(def.id(), position);
            case "exit" -> new Exit(def.id(), position);
            case "treasure" -> new Treasure(def.id(), position);
            case "key" -> {
                if (def.keyId() == null) {
                    throw new InvalidLevelException(source, "钥匙 " + def.id() + " 缺少 keyId");
                }
                yield new Key(def.id(), position, def.keyId());
            }
            case "door" -> {
                if (def.doorId() == null) {
                    throw new InvalidLevelException(source, "门 " + def.id() + " 缺少 doorId");
                }
                yield new Door(def.id(), position, def.doorId(), def.open());
            }
            case "boulder" -> new Boulder(def.id(), position);
            case "switch" -> new FloorSwitch(def.id(), position);
            default -> throw new InvalidLevelException(source, "未知实体类型：" + type + "（实体 " + def.id() + "）");
        };
    }
}
