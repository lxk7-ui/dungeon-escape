package com.example.dungeonescape.persistence;

import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.Position;
import com.example.dungeonescape.model.entity.Entity;
import com.example.dungeonescape.model.entity.Player;
import com.example.dungeonescape.model.goal.Goal;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关卡加载器：从 JSON 读取 {@link LevelDefinition}，严格校验后构建 {@link GameState}。
 *
 * <p>支持 classpath 资源路径（供未来 UI 使用）、{@link InputStream}（供测试）与
 * {@link Path} 三种来源；所有流使用 UTF-8 且以 try-with-resources 关闭，
 * 不依赖工作目录绝对路径。
 *
 * <p>校验规则（失败一律抛携带来源与原因的 {@link InvalidLevelException}）：
 * <ul>
 *   <li>宽高在 5..50；name 非空白；实体 id 非空白且唯一；坐标在网格内；</li>
 *   <li>恰好 1 个玩家、至少 1 个出口；实体/目标类型已知；</li>
 *   <li>禁止初始重叠：多个阻挡实体同格（墙/巨石/门视为阻挡）、玩家+墙、玩家+巨石、
 *       多个玩家；允许玩家+出口、玩家+机关、巨石+机关、巨石+出口；</li>
 *   <li>每扇门至少存在一把同 doorId 的钥匙；</li>
 *   <li>目标非空：SWITCHES 至少 1 个机关、TREASURE 至少 1 个宝物、EXIT 至少 1 个出口，
 *       AND/OR 至少 2 个非空子目标。</li>
 * </ul>
 * 不做关卡可解性搜索。
 */
public class LevelLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 已知的小写实体类型。 */
    private static final Set<String> KNOWN_TYPES =
            Set.of("player", "wall", "exit", "treasure", "key", "door", "boulder", "switch");

    /** 视为阻挡类型的实体（用于初始重叠校验；门关闭时也阻挡）。 */
    private static final Set<String> BLOCKING_TYPES = Set.of("wall", "boulder", "door");

    /**
     * 从 classpath 资源加载关卡。
     *
     * @param resourcePath 资源路径，如 "com/example/dungeonescape/levels/level1.json"
     * @return 构建完成的游戏局面
     * @throws InvalidLevelException 资源不存在或关卡非法
     */
    public GameState loadFromClasspath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new InvalidLevelException(resourcePath == null ? "classpath" : resourcePath, "资源路径不能为空白");
        }
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        InputStream in = LevelLoader.class.getClassLoader().getResourceAsStream(normalized);
        if (in == null) {
            throw new InvalidLevelException(normalized, "classpath 资源不存在");
        }
        return load(in, normalized);
    }

    /**
     * 从 classpath 资源加载关卡定义（仅 JSON 元数据解析，供选关页展示
     * 名称/目标/实体构成；不做 {@link #build} 的完整校验与 GameState 构建）。
     *
     * @param resourcePath 资源路径，如 "com/example/dungeonescape/levels/level1.json"
     * @return 解析后的关卡定义
     * @throws InvalidLevelException 资源不存在、JSON 无法解析或数据为空
     */
    public LevelDefinition loadDefinitionFromClasspath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new InvalidLevelException(resourcePath == null ? "classpath" : resourcePath, "资源路径不能为空白");
        }
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        InputStream in = LevelLoader.class.getClassLoader().getResourceAsStream(normalized);
        if (in == null) {
            throw new InvalidLevelException(normalized, "classpath 资源不存在");
        }
        try (in; Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            LevelDefinition definition = MAPPER.readValue(reader, LevelDefinition.class);
            if (definition == null) {
                throw new InvalidLevelException(normalized, "关卡数据为空");
            }
            return definition;
        } catch (IOException e) {
            throw new InvalidLevelException(normalized, "JSON 解析失败：" + e.getMessage());
        }
    }

    /**
     * 从输入流加载关卡（来源名固定为 "level"）。
     *
     * @param in 输入流（方法负责关闭）
     * @return 构建完成的游戏局面
     * @throws InvalidLevelException 读取失败或关卡非法
     */
    public GameState load(InputStream in) {
        return load(in, "level");
    }

    /**
     * 从输入流加载关卡，并指定来源名（用于错误消息）。
     *
     * @param in     输入流（方法负责关闭）
     * @param source 来源名（资源路径、文件名或自定义名称）
     * @return 构建完成的游戏局面
     * @throws InvalidLevelException 读取失败或关卡非法
     */
    public GameState load(InputStream in, String source) {
        try (in; Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            LevelDefinition definition = MAPPER.readValue(reader, LevelDefinition.class);
            return build(definition, source);
        } catch (InvalidLevelException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidLevelException(source, "JSON 解析失败：" + e.getMessage());
        }
    }

    /**
     * 从文件路径加载关卡。
     *
     * @param path 关卡 JSON 文件路径
     * @return 构建完成的游戏局面
     * @throws InvalidLevelException 文件不存在/不可读或关卡非法
     */
    public GameState load(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return load(in, path.toString());
        } catch (InvalidLevelException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidLevelException(path.toString(), "无法读取文件：" + e.getMessage());
        }
    }

    /** 对解析后的定义执行全部校验并构建游戏局面。 */
    private GameState build(LevelDefinition definition, String source) {
        if (definition == null) {
            throw new InvalidLevelException(source, "关卡数据为空");
        }
        if (isBlank(definition.name())) {
            throw new InvalidLevelException(source, "关卡名称不能为空白");
        }
        int width = definition.width();
        int height = definition.height();
        if (width < 5 || width > 50) {
            throw new InvalidLevelException(source, "宽度 " + width + " 超出允许范围 5..50");
        }
        if (height < 5 || height > 50) {
            throw new InvalidLevelException(source, "高度 " + height + " 超出允许范围 5..50");
        }
        List<EntityDefinition> defs = definition.entities();
        if (defs == null || defs.isEmpty()) {
            throw new InvalidLevelException(source, "实体列表不能为空");
        }
        if (definition.goal() == null) {
            throw new InvalidLevelException(source, "目标不能为空");
        }

        validateEntities(defs, source, width, height);
        validateCounts(defs, source);
        validateOverlaps(defs, source);
        validateDoorKeys(defs, source);

        List<Entity> entities = defs.stream().map(d -> EntityFactory.create(d, source))
                .collect(java.util.stream.Collectors.toList());
        Player player = entities.stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .findFirst()
                .orElseThrow(() -> new InvalidLevelException(source, "关卡缺少玩家"));
        List<Entity> others = entities.stream().filter(e -> !(e instanceof Player))
                .collect(java.util.stream.Collectors.toList());

        Goal goal = GoalFactory.create(definition.goal(), source, defs);

        return new GameState(width, height, player, others, goal, definition.name());
    }

    /** 校验实体 id、类型与坐标。 */
    private void validateEntities(List<EntityDefinition> defs, String source, int width, int height) {
        Set<String> ids = new HashSet<>();
        for (EntityDefinition def : defs) {
            String id = def.id();
            if (isBlank(id)) {
                throw new InvalidLevelException(source, "实体 id 不能为空白");
            }
            if (!ids.add(id)) {
                throw new InvalidLevelException(source, "实体 id 重复：" + id);
            }
            if (isBlank(def.type()) || !KNOWN_TYPES.contains(def.type())) {
                throw new InvalidLevelException(source, "未知实体类型：" + def.type() + "（实体 " + id + "）");
            }
            if (def.x() == null || def.y() == null) {
                throw new InvalidLevelException(source, "实体 " + id + " 缺少坐标");
            }
            if (def.x() < 0 || def.x() >= width || def.y() < 0 || def.y() >= height) {
                throw new InvalidLevelException(source,
                        "实体 " + id + " 坐标 (" + def.x() + "," + def.y() + ") 超出范围 " + width + "x" + height);
            }
        }
    }

    /** 校验恰好 1 个玩家、至少 1 个出口。 */
    private void validateCounts(List<EntityDefinition> defs, String source) {
        long players = defs.stream().filter(d -> "player".equals(d.type())).count();
        if (players != 1) {
            throw new InvalidLevelException(source, "关卡必须恰好包含 1 个玩家，实际有 " + players + " 个");
        }
        long exits = defs.stream().filter(d -> "exit".equals(d.type())).count();
        if (exits < 1) {
            throw new InvalidLevelException(source, "关卡必须至少包含 1 个出口，实际有 " + exits + " 个");
        }
    }

    /** 校验初始重叠：同格多个阻挡实体、玩家+墙、多个玩家。 */
    private void validateOverlaps(List<EntityDefinition> defs, String source) {
        Map<Position, List<EntityDefinition>> byPosition = defs.stream()
                .collect(Collectors.groupingBy(d -> new Position(d.x(), d.y())));
        for (Map.Entry<Position, List<EntityDefinition>> entry : byPosition.entrySet()) {
            List<EntityDefinition> group = entry.getValue();
            if (group.size() < 2) {
                continue;
            }
            Position position = entry.getKey();
            long blockingCount = group.stream().filter(d -> BLOCKING_TYPES.contains(d.type())).count();
            if (blockingCount >= 2) {
                throw new InvalidLevelException(source,
                        "位置 (" + position.x() + "," + position.y() + ") 存在多个阻挡实体：" + typesOf(group));
            }
            boolean hasPlayer = group.stream().anyMatch(d -> "player".equals(d.type()));
            boolean hasWall = group.stream().anyMatch(d -> "wall".equals(d.type()));
            boolean hasBoulder = group.stream().anyMatch(d -> "boulder".equals(d.type()));
            long playerCount = group.stream().filter(d -> "player".equals(d.type())).count();
            if (hasPlayer && hasWall) {
                throw new InvalidLevelException(source,
                        "位置 (" + position.x() + "," + position.y() + ") 玩家与墙重叠");
            }
            if (hasPlayer && hasBoulder) {
                throw new InvalidLevelException(source,
                        "位置 (" + position.x() + "," + position.y() + ") 玩家与巨石重叠");
            }
            if (playerCount > 1) {
                throw new InvalidLevelException(source,
                        "位置 (" + position.x() + "," + position.y() + ") 存在多个玩家");
            }
        }
    }

    /** 校验每扇门至少存在一把同 doorId 的钥匙。 */
    private void validateDoorKeys(List<EntityDefinition> defs, String source) {
        List<EntityDefinition> doors = defs.stream().filter(d -> "door".equals(d.type()))
                .collect(java.util.stream.Collectors.toList());
        List<EntityDefinition> keys = defs.stream().filter(d -> "key".equals(d.type()))
                .collect(java.util.stream.Collectors.toList());
        for (EntityDefinition door : doors) {
            if (door.doorId() == null) {
                throw new InvalidLevelException(source, "门 " + door.id() + " 缺少 doorId");
            }
            boolean hasKey = keys.stream()
                    .anyMatch(k -> k.keyId() != null && k.keyId().equals(door.doorId()));
            if (!hasKey) {
                throw new InvalidLevelException(source,
                        "门 " + door.id() + "（doorId " + door.doorId() + "）没有对应的钥匙");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** 拼接同格实体类型清单，用于错误消息。 */
    private static String typesOf(List<EntityDefinition> group) {
        return group.stream().map(EntityDefinition::type).sorted().collect(Collectors.joining("、"));
    }
}
