package com.example.dungeonescape.persistence;

import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.Position;
import com.example.dungeonescape.model.entity.Boulder;
import com.example.dungeonescape.model.entity.Door;
import com.example.dungeonescape.model.entity.Exit;
import com.example.dungeonescape.model.entity.FloorSwitch;
import com.example.dungeonescape.model.entity.Key;
import com.example.dungeonescape.model.entity.Treasure;
import com.example.dungeonescape.model.entity.Wall;
import com.example.dungeonescape.model.goal.AndGoal;
import com.example.dungeonescape.model.goal.ExitGoal;
import com.example.dungeonescape.model.goal.Goal;
import com.example.dungeonescape.model.goal.OrGoal;
import com.example.dungeonescape.model.goal.SwitchGoal;
import com.example.dungeonescape.model.goal.TreasureGoal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 关卡加载器测试：5 个正式关卡、合法内存 JSON、全部校验规则、
 * 非法重叠与允许重叠、JSON 语法与缺失字段、来源+原因错误消息。
 */
class LevelLoaderTest {

    private static final String LEVEL_DIR = "com/example/dungeonescape/levels";
    private static final String SOURCE = "memory";

    private final LevelLoader loader = new LevelLoader();

    // ---------- JSON 拼装工具 ----------

    private static String entity(String type, String id, int x, int y) {
        return "{\"type\": \"%s\", \"id\": \"%s\", \"x\": %d, \"y\": %d}".formatted(type, id, x, y);
    }

    private static String key(String id, int x, int y, int keyId) {
        return "{\"type\": \"key\", \"id\": \"%s\", \"x\": %d, \"y\": %d, \"keyId\": %d}"
                .formatted(id, x, y, keyId);
    }

    private static String door(String id, int x, int y, int doorId) {
        return "{\"type\": \"door\", \"id\": \"%s\", \"x\": %d, \"y\": %d, \"doorId\": %d}"
                .formatted(id, x, y, doorId);
    }

    private static String door(String id, int x, int y, int doorId, boolean open) {
        return "{\"type\": \"door\", \"id\": \"%s\", \"x\": %d, \"y\": %d, \"doorId\": %d, \"open\": %s}"
                .formatted(id, x, y, doorId, open);
    }

    private static String entities(String... entries) {
        return String.join(",", entries);
    }

    private static String goal(String type, String children) {
        if (children == null) {
            return "{\"type\": \"%s\"}".formatted(type);
        }
        return "{\"type\": \"%s\", \"children\": [%s]}".formatted(type, children);
    }

    private static String levelJson(String name, int width, int height, String entities, String goal) {
        return """
                {
                  "name": "%s",
                  "width": %d,
                  "height": %d,
                  "entities": [%s],
                  "goal": %s
                }
                """.formatted(name, width, height, entities, goal);
    }

    /** 合法基准关卡：6x6，玩家 (1,1)，出口 (5,5)，EXIT 目标。 */
    private static String validLevel(String entities, String goal) {
        return levelJson("内存测试关卡", 6, 6, entities, goal);
    }

    private static String baseEntities() {
        return entities(entity("player", "player", 1, 1), entity("exit", "exit-1", 5, 5));
    }

    // ---------- 加载工具 ----------

    private GameState load(String json, String source) {
        return loader.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), source);
    }

    private GameState loadLevel(String fileName) {
        return loader.loadFromClasspath(LEVEL_DIR + "/" + fileName);
    }

    /** 断言加载失败，且消息同时包含来源名与全部原因片段。 */
    private InvalidLevelException assertInvalid(String json, String source, String... reasonParts) {
        InvalidLevelException e = assertThrows(InvalidLevelException.class, () -> load(json, source));
        assertTrue(e.getMessage().contains(source),
                "错误消息应包含来源 " + source + "，实际：" + e.getMessage());
        for (String part : reasonParts) {
            assertTrue(e.getMessage().contains(part),
                    "错误消息应包含原因片段 " + part + "，实际：" + e.getMessage());
        }
        return e;
    }

    // ---------- 5 个正式关卡 ----------

    @Test
    void level1LoadsWithExpectedStats() {
        GameState state = loadLevel("level1.json");
        assertEquals("01基础迷宫", state.getLevelName());
        assertEquals(8, state.getWidth());
        assertEquals(6, state.getHeight());
        assertEquals(new Position(1, 1), state.getPlayer().getPosition());
        assertEquals(1, state.countEntities(Exit.class));
        assertEquals(27, state.countEntities(Wall.class));
        assertEquals(0, state.countEntities(Treasure.class));
        assertInstanceOf(ExitGoal.class, state.getGoal());
    }

    @Test
    void level2LoadsWithExpectedStats() {
        GameState state = loadLevel("level2.json");
        assertEquals("02宝物猎人", state.getLevelName());
        assertEquals(8, state.getWidth());
        assertEquals(6, state.getHeight());
        assertEquals(3, state.countEntities(Treasure.class));
        assertEquals(27, state.countEntities(Wall.class));
        assertInstanceOf(AndGoal.class, state.getGoal());
        List<Goal> children = ((AndGoal) state.getGoal()).getChildren();
        assertEquals(2, children.size());
        assertInstanceOf(ExitGoal.class, children.get(0));
        assertInstanceOf(TreasureGoal.class, children.get(1));
        assertEquals(3, ((TreasureGoal) children.get(1)).getTotalTreasures());
    }

    @Test
    void level3LoadsWithExpectedStats() {
        GameState state = loadLevel("level3.json");
        assertEquals("03钥匙与门", state.getLevelName());
        assertEquals(8, state.getWidth());
        assertEquals(6, state.getHeight());
        assertEquals(1, state.countEntities(Key.class));
        assertEquals(1, state.countEntities(Door.class));
        Key key = (Key) state.getEntities().stream().filter(Key.class::isInstance).findFirst().orElseThrow();
        assertEquals(1, key.getKeyId());
        Door door = (Door) state.getEntities().stream().filter(Door.class::isInstance).findFirst().orElseThrow();
        assertEquals(1, door.getDoorId());
        assertFalse(door.isOpen());
        assertTrue(door.isBlocking());
        assertInstanceOf(ExitGoal.class, state.getGoal());
    }

    @Test
    void level4LoadsWithExpectedStats() {
        GameState state = loadLevel("level4.json");
        assertEquals("04推箱机关", state.getLevelName());
        assertEquals(8, state.getWidth());
        assertEquals(6, state.getHeight());
        assertEquals(2, state.countEntities(Boulder.class));
        assertEquals(2, state.countEntities(FloorSwitch.class));
        assertInstanceOf(AndGoal.class, state.getGoal());
        List<Goal> children = ((AndGoal) state.getGoal()).getChildren();
        assertEquals(2, children.size());
        assertInstanceOf(ExitGoal.class, children.get(0));
        assertInstanceOf(SwitchGoal.class, children.get(1));
    }

    @Test
    void level5LoadsWithExpectedStats() {
        GameState state = loadLevel("level5.json");
        assertEquals("05综合挑战", state.getLevelName());
        assertEquals(9, state.getWidth());
        assertEquals(7, state.getHeight());
        assertEquals(2, state.countEntities(Treasure.class));
        assertEquals(1, state.countEntities(Boulder.class));
        assertEquals(1, state.countEntities(FloorSwitch.class));
        assertInstanceOf(AndGoal.class, state.getGoal());
        List<Goal> andChildren = ((AndGoal) state.getGoal()).getChildren();
        assertEquals(2, andChildren.size());
        assertInstanceOf(ExitGoal.class, andChildren.get(0));
        assertInstanceOf(OrGoal.class, andChildren.get(1));
        List<Goal> orChildren = ((OrGoal) andChildren.get(1)).getChildren();
        assertEquals(2, orChildren.size());
        assertInstanceOf(TreasureGoal.class, orChildren.get(0));
        assertEquals(2, ((TreasureGoal) orChildren.get(0)).getTotalTreasures());
        assertInstanceOf(SwitchGoal.class, orChildren.get(1));
    }

    // ---------- 合法输入 ----------

    @Test
    void loadsValidInMemoryLevel() {
        GameState state = load(validLevel(baseEntities(), goal("EXIT", null)), SOURCE);
        assertNotNull(state);
        assertEquals("内存测试关卡", state.getLevelName());
        assertEquals(new Position(1, 1), state.getPlayer().getPosition());
        assertEquals(new Position(5, 5),
                state.getEntities().stream().filter(Exit.class::isInstance).findFirst().orElseThrow().getPosition());
        assertInstanceOf(ExitGoal.class, state.getGoal());
    }

    @Test
    void loadsFromPath() throws Exception {
        Path path = Files.createTempFile("dungeon-level", ".json");
        try {
            Files.writeString(path, validLevel(baseEntities(), goal("EXIT", null)), StandardCharsets.UTF_8);
            GameState state = loader.load(path);
            assertEquals("内存测试关卡", state.getLevelName());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    void doorDefaultsToClosedWhenOpenMissing() {
        String json = validLevel(entities(baseEntities(), key("key-1", 2, 2, 1), door("door-1", 3, 3, 1)),
                goal("EXIT", null));
        GameState state = load(json, SOURCE);
        Door door = (Door) state.getEntities().stream().filter(Door.class::isInstance).findFirst().orElseThrow();
        assertFalse(door.isOpen());
    }

    @Test
    void doorCanBeInitiallyOpen() {
        String json = validLevel(entities(baseEntities(), key("key-1", 2, 2, 1), door("door-1", 3, 3, 1, true)),
                goal("EXIT", null));
        GameState state = load(json, SOURCE);
        Door door = (Door) state.getEntities().stream().filter(Door.class::isInstance).findFirst().orElseThrow();
        assertTrue(door.isOpen());
        assertFalse(door.isBlocking());
    }

    // ---------- 实体/目标类型 ----------

    @Test
    void rejectsUnknownEntityType() {
        String json = validLevel(entities(baseEntities(), entity("monster", "monster-1", 2, 2)), goal("EXIT", null));
        assertInvalid(json, SOURCE, "未知实体类型", "monster");
    }

    @Test
    void rejectsUnknownGoalType() {
        String json = validLevel(baseEntities(), goal("ESCORT", null));
        assertInvalid(json, SOURCE, "未知目标类型", "ESCORT");
    }

    // ---------- 玩家与出口数量 ----------

    @Test
    void rejectsTwoPlayers() {
        String json = validLevel(entities(baseEntities(), entity("player", "player-2", 4, 4)), goal("EXIT", null));
        assertInvalid(json, SOURCE, "玩家", "2 个");
    }

    @Test
    void rejectsZeroPlayers() {
        String json = validLevel(entities(entity("exit", "exit-1", 5, 5)), goal("EXIT", null));
        assertInvalid(json, SOURCE, "玩家", "0 个");
    }

    @Test
    void rejectsZeroExits() {
        String json = validLevel(entities(entity("player", "player", 1, 1)), goal("EXIT", null));
        assertInvalid(json, SOURCE, "出口", "0 个");
    }

    // ---------- id / 坐标 / 尺寸 ----------

    @Test
    void rejectsDuplicateEntityIds() {
        String json = validLevel(entities(baseEntities(), entity("wall", "wall-1", 2, 2), entity("wall", "wall-1", 3, 3)),
                goal("EXIT", null));
        assertInvalid(json, SOURCE, "重复", "wall-1");
    }

    @Test
    void rejectsOutOfBoundsCoordinate() {
        String json = validLevel(entities(entity("player", "player", 6, 1), entity("exit", "exit-1", 5, 5)),
                goal("EXIT", null));
        assertInvalid(json, SOURCE, "player", "超出范围", "(6,1)");
    }

    @Test
    void rejectsNegativeCoordinate() {
        String json = validLevel(entities(entity("player", "player", -1, 1), entity("exit", "exit-1", 5, 5)),
                goal("EXIT", null));
        assertInvalid(json, SOURCE, "player", "超出范围");
    }

    @Test
    void rejectsWidthBelowMinimum() {
        String json = levelJson("太窄", 4, 6, baseEntities(), goal("EXIT", null));
        assertInvalid(json, SOURCE, "宽度", "4", "5..50");
    }

    @Test
    void rejectsWidthAboveMaximum() {
        String json = levelJson("太宽", 51, 6, baseEntities(), goal("EXIT", null));
        assertInvalid(json, SOURCE, "宽度", "51");
    }

    @Test
    void rejectsHeightBelowMinimum() {
        String json = levelJson("太矮", 6, 4, baseEntities(), goal("EXIT", null));
        assertInvalid(json, SOURCE, "高度", "4");
    }

    @Test
    void rejectsHeightAboveMaximum() {
        String json = levelJson("太高", 6, 51, baseEntities(), goal("EXIT", null));
        assertInvalid(json, SOURCE, "高度", "51");
    }

    @Test
    void rejectsBlankName() {
        String json = levelJson("   ", 6, 6, baseEntities(), goal("EXIT", null));
        assertInvalid(json, SOURCE, "名称");
    }

    @Test
    void rejectsNullName() {
        String json = """
                {
                  "name": null,
                  "width": 6,
                  "height": 6,
                  "entities": [%s],
                  "goal": {"type": "EXIT"}
                }
                """.formatted(baseEntities());
        assertInvalid(json, SOURCE, "名称");
    }

    @Test
    void rejectsBlankEntityId() {
        String json = validLevel(entities(baseEntities(), entity("wall", "  ", 2, 2)), goal("EXIT", null));
        assertInvalid(json, SOURCE, "id");
    }

    // ---------- 门与钥匙 ----------

    @Test
    void rejectsDoorWithoutMatchingKey() {
        String json = validLevel(entities(baseEntities(), door("door-1", 3, 3, 1)), goal("EXIT", null));
        assertInvalid(json, SOURCE, "door-1", "doorId 1", "钥匙");
    }

    @Test
    void rejectsDoorWithMismatchedKey() {
        String json = validLevel(entities(baseEntities(), key("key-1", 2, 2, 9), door("door-1", 3, 3, 1)),
                goal("EXIT", null));
        assertInvalid(json, SOURCE, "door-1", "钥匙");
    }

    @Test
    void rejectsOpenDoorWithoutKey() {
        String json = validLevel(entities(baseEntities(), door("door-1", 3, 3, 1, true)), goal("EXIT", null));
        assertInvalid(json, SOURCE, "door-1", "钥匙");
    }

    @Test
    void doorMayShareKeyWithAnotherDoor() {
        String json = validLevel(entities(baseEntities(), key("key-1", 2, 2, 1),
                        door("door-1", 3, 3, 1), door("door-2", 4, 4, 1)),
                goal("EXIT", null));
        GameState state = load(json, SOURCE);
        assertEquals(2, state.countEntities(Door.class));
    }

    // ---------- 目标语义 ----------

    @Test
    void rejectsAndGoalWithSingleChild() {
        String json = validLevel(baseEntities(), goal("AND", goal("EXIT", null)));
        assertInvalid(json, SOURCE, "AND", "至少需要 2 个子目标");
    }

    @Test
    void rejectsOrGoalWithoutChildren() {
        String json = validLevel(baseEntities(), "{\"type\": \"OR\"}");
        assertInvalid(json, SOURCE, "OR", "至少需要 2 个子目标");
    }

    @Test
    void rejectsOrGoalWithNullChild() {
        String json = validLevel(baseEntities(), "{\"type\": \"OR\", \"children\": [{\"type\": \"EXIT\"}, null]}");
        assertInvalid(json, SOURCE, "OR", "子目标为 null");
    }

    @Test
    void rejectsSwitchesGoalWithoutSwitch() {
        String json = validLevel(baseEntities(), goal("SWITCHES", null));
        assertInvalid(json, SOURCE, "SWITCHES", "机关");
    }

    @Test
    void rejectsTreasureGoalWithoutTreasure() {
        String json = validLevel(baseEntities(), goal("TREASURE", null));
        assertInvalid(json, SOURCE, "TREASURE", "宝物");
    }

    @Test
    void rejectsExitGoalWithoutExit() {
        // 实体级“至少 1 个出口”校验先于目标校验触发
        String json = validLevel(entities(entity("player", "player", 1, 1), entity("treasure", "treasure-1", 2, 2)),
                goal("EXIT", null));
        assertInvalid(json, SOURCE, "出口", "0 个");
    }

    // ---------- 初始重叠 ----------

    record OverlapCase(String label, String entityJson, String expectedFragment) {
    }

    static Stream<OverlapCase> illegalOverlapCases() {
        return Stream.of(
                new OverlapCase("两个巨石同格",
                        entities(baseEntities(), entity("boulder", "boulder-1", 2, 2), entity("boulder", "boulder-2", 2, 2)),
                        "位置 (2,2) 存在多个阻挡实体"),
                new OverlapCase("玩家与墙同格",
                        entities(entity("player", "player", 1, 1), entity("wall", "wall-1", 1, 1),
                                entity("exit", "exit-1", 5, 5)),
                        "位置 (1,1) 玩家与墙重叠"),
                new OverlapCase("玩家与巨石同格",
                        entities(entity("player", "player", 1, 1), entity("boulder", "boulder-1", 1, 1),
                                entity("exit", "exit-1", 5, 5)),
                        "位置 (1,1) 玩家与巨石重叠"),
                new OverlapCase("巨石与墙同格",
                        entities(baseEntities(), entity("boulder", "boulder-1", 2, 2), entity("wall", "wall-1", 2, 2)),
                        "位置 (2,2) 存在多个阻挡实体"),
                new OverlapCase("两门同格",
                        entities(baseEntities(), door("door-1", 3, 3, 1), door("door-2", 3, 3, 2)),
                        "位置 (3,3) 存在多个阻挡实体"),
                new OverlapCase("两玩家同格",
                        entities(entity("player", "player", 1, 1), entity("player", "player-2", 1, 1),
                                entity("exit", "exit-1", 5, 5)),
                        "恰好包含 1 个玩家，实际有 2 个"),
                new OverlapCase("墙与墙同格",
                        entities(baseEntities(), entity("wall", "wall-1", 2, 2), entity("wall", "wall-2", 2, 2)),
                        "位置 (2,2) 存在多个阻挡实体"),
                new OverlapCase("巨石与门同格",
                        entities(baseEntities(), entity("boulder", "boulder-1", 2, 2), door("door-1", 2, 2, 1)),
                        "位置 (2,2) 存在多个阻挡实体"));
    }

    @ParameterizedTest(name = "非法重叠：{0}")
    @MethodSource("illegalOverlapCases")
    void rejectsIllegalOverlaps(OverlapCase overlap) {
        String json = validLevel(overlap.entityJson(), goal("EXIT", null));
        assertInvalid(json, SOURCE, overlap.expectedFragment());
    }

    static Stream<OverlapCase> allowedOverlapCases() {
        return Stream.of(
                new OverlapCase("玩家+出口",
                        entities(entity("player", "player", 1, 1), entity("exit", "exit-1", 1, 1)), null),
                new OverlapCase("玩家+机关",
                        entities(entity("player", "player", 1, 1), entity("switch", "switch-1", 1, 1),
                                entity("exit", "exit-1", 5, 5)),
                        null),
                new OverlapCase("巨石+机关",
                        entities(entity("player", "player", 1, 1), entity("exit", "exit-1", 5, 5),
                                entity("boulder", "boulder-1", 2, 2), entity("switch", "switch-1", 2, 2)),
                        null),
                new OverlapCase("巨石+出口",
                        entities(entity("player", "player", 1, 1),
                                entity("boulder", "boulder-1", 2, 2), entity("exit", "exit-1", 2, 2)),
                        null));
    }

    @ParameterizedTest(name = "允许重叠：{0}")
    @MethodSource("allowedOverlapCases")
    void acceptsAllowedOverlaps(OverlapCase overlap) {
        String json = validLevel(overlap.entityJson(), goal("EXIT", null));
        GameState state = load(json, SOURCE);
        assertNotNull(state);
        assertEquals(1, state.countEntities(Exit.class));
    }

    // ---------- JSON 语法与缺失字段 ----------

    @Test
    void rejectsMalformedJson() {
        assertInvalid("{ not valid json", SOURCE, "JSON 解析失败");
    }

    @Test
    void rejectsEmptyJson() {
        assertInvalid("", SOURCE, "JSON 解析失败");
    }

    static Stream<OverlapCase> missingFieldCases() {
        return Stream.of(
                new OverlapCase("缺少 name", """
                        {
                          "width": 6, "height": 6,
                          "entities": [%s],
                          "goal": {"type": "EXIT"}
                        }
                        """.formatted(baseEntities()), "名称"),
                new OverlapCase("缺少 entities", """
                        {
                          "name": "缺实体", "width": 6, "height": 6,
                          "goal": {"type": "EXIT"}
                        }
                        """, "实体列表"),
                new OverlapCase("entities 为空", levelJson("空实体", 6, 6, "", goal("EXIT", null)), "实体列表"),
                new OverlapCase("缺少 goal", """
                        {
                          "name": "缺目标", "width": 6, "height": 6,
                          "entities": [%s]
                        }
                        """.formatted(baseEntities()), "目标不能为空"),
                new OverlapCase("goal 为 null", levelJson("目标为空", 6, 6, baseEntities(), "null"), "目标不能为空"),
                new OverlapCase("缺少 x", """
                        {
                          "name": "缺x", "width": 6, "height": 6,
                          "entities": [%s, {"type": "wall", "id": "wall-1", "y": 2}],
                          "goal": {"type": "EXIT"}
                        }
                        """.formatted(baseEntities()), "缺少坐标"),
                new OverlapCase("缺少 id", """
                        {
                          "name": "缺id", "width": 6, "height": 6,
                          "entities": [%s, {"type": "wall", "x": 2, "y": 2}],
                          "goal": {"type": "EXIT"}
                        }
                        """.formatted(baseEntities()), "id 不能为空白"),
                new OverlapCase("缺少 type", """
                        {
                          "name": "缺type", "width": 6, "height": 6,
                          "entities": [%s, {"id": "thing-1", "x": 2, "y": 2}],
                          "goal": {"type": "EXIT"}
                        }
                        """.formatted(baseEntities()), "未知实体类型"),
                new OverlapCase("缺少目标 type", levelJson("缺目标类型", 6, 6, baseEntities(), "{}"), "目标类型缺失"),
                new OverlapCase("key 缺少 keyId",
                        validLevel(entities(baseEntities(), "{\"type\": \"key\", \"id\": \"key-1\", \"x\": 2, \"y\": 2}"),
                                goal("EXIT", null)),
                        "keyId"),
                new OverlapCase("door 缺少 doorId",
                        validLevel(entities(baseEntities(), "{\"type\": \"door\", \"id\": \"door-1\", \"x\": 2, \"y\": 2}"),
                                goal("EXIT", null)),
                        "doorId"));
    }

    @ParameterizedTest(name = "缺失字段：{0}")
    @MethodSource("missingFieldCases")
    void rejectsMissingFields(OverlapCase missing) {
        assertInvalid(missing.entityJson(), SOURCE, missing.expectedFragment());
    }

    // ---------- classpath 与来源消息 ----------

    @Test
    void classpathResourceNotFoundReportsSource() {
        InvalidLevelException e = assertThrows(InvalidLevelException.class,
                () -> loader.loadFromClasspath(LEVEL_DIR + "/not-exist.json"));
        assertTrue(e.getMessage().contains("not-exist.json"));
        assertTrue(e.getMessage().contains("资源不存在"));
    }

    @Test
    void blankClasspathPathRejected() {
        InvalidLevelException e = assertThrows(InvalidLevelException.class,
                () -> loader.loadFromClasspath("   "));
        assertTrue(e.getMessage().contains("资源路径"));
    }

    @Test
    void errorMessageCarriesSourceAndReason() {
        String json = validLevel(entities(entity("player", "player", 1, 1), entity("player", "player-2", 4, 4)),
                goal("EXIT", null));
        InvalidLevelException e = assertThrows(InvalidLevelException.class, () -> load(json, "my-source"));
        assertEquals("my-source", e.getSource());
        assertTrue(e.getReason().contains("玩家"));
        assertTrue(e.getMessage().contains("my-source"));
        assertTrue(e.getMessage().contains("关卡加载失败"));
    }
}
