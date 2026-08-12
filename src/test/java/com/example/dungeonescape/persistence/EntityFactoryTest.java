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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 实体工厂测试：各类型映射、类型专属字段、结构与来源错误消息。
 */
class EntityFactoryTest {

    private static final String SOURCE = "factory-test";

    private static EntityDefinition def(String id, String type, Integer x, Integer y,
                                        Integer keyId, Integer doorId, boolean open) {
        return new EntityDefinition(id, type, x, y, keyId, doorId, open);
    }

    private static EntityDefinition def(String id, String type) {
        return def(id, type, 1, 1, null, null, false);
    }

    private static InvalidLevelException invalid(EntityDefinition definition) {
        return assertThrows(InvalidLevelException.class, () -> EntityFactory.create(definition, SOURCE));
    }

    @Test
    void createsPlayer() {
        Entity entity = EntityFactory.create(def("player", "player", 2, 3, null, null, false), SOURCE);
        assertInstanceOf(Player.class, entity);
        assertEquals("player", entity.getId());
        assertEquals(new Position(2, 3), entity.getPosition());
        assertFalse(entity.isBlocking());
    }

    @Test
    void createsWall() {
        Entity entity = EntityFactory.create(def("wall-1", "wall"), SOURCE);
        assertInstanceOf(Wall.class, entity);
        assertTrue(entity.isBlocking());
    }

    @Test
    void createsExit() {
        Entity entity = EntityFactory.create(def("exit-1", "exit"), SOURCE);
        assertInstanceOf(Exit.class, entity);
        assertFalse(entity.isBlocking());
    }

    @Test
    void createsTreasure() {
        Entity entity = EntityFactory.create(def("treasure-1", "treasure"), SOURCE);
        assertInstanceOf(Treasure.class, entity);
        assertFalse(entity.isBlocking());
    }

    @Test
    void createsKeyWithKeyId() {
        Entity entity = EntityFactory.create(def("key-1", "key", 4, 4, 7, null, false), SOURCE);
        assertInstanceOf(Key.class, entity);
        assertEquals(7, ((Key) entity).getKeyId());
        assertFalse(entity.isBlocking());
    }

    @Test
    void createsDoorWithDoorIdAndOpenState() {
        Entity closed = EntityFactory.create(def("door-1", "door", 4, 4, null, 3, false), SOURCE);
        assertInstanceOf(Door.class, closed);
        Door door = (Door) closed;
        assertEquals(3, door.getDoorId());
        assertFalse(door.isOpen());
        assertTrue(door.isBlocking()); // 关闭的门阻挡

        Entity open = EntityFactory.create(def("door-2", "door", 4, 5, null, 3, true), SOURCE);
        assertTrue(((Door) open).isOpen());
        assertFalse(open.isBlocking()); // 打开的门不阻挡
    }

    @Test
    void createsBoulder() {
        Entity entity = EntityFactory.create(def("boulder-1", "boulder"), SOURCE);
        assertInstanceOf(Boulder.class, entity);
        assertTrue(entity.isBlocking());
    }

    @Test
    void createsFloorSwitch() {
        Entity entity = EntityFactory.create(def("switch-1", "switch"), SOURCE);
        assertInstanceOf(FloorSwitch.class, entity);
        assertFalse(entity.isBlocking());
    }

    @Test
    void rejectsKeyWithoutKeyId() {
        InvalidLevelException e = invalid(def("key-1", "key"));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("key-1"));
        assertTrue(e.getMessage().contains("keyId"));
    }

    @Test
    void rejectsDoorWithoutDoorId() {
        InvalidLevelException e = invalid(def("door-1", "door"));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("door-1"));
        assertTrue(e.getMessage().contains("doorId"));
    }

    @Test
    void rejectsUnknownType() {
        InvalidLevelException e = invalid(def("monster-1", "monster"));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("monster"));
        assertTrue(e.getMessage().contains("未知实体类型"));
    }

    @Test
    void rejectsNullDefinition() {
        InvalidLevelException e = invalid(null);
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("实体定义"));
    }

    @Test
    void rejectsBlankId() {
        InvalidLevelException e = invalid(def("  ", "wall"));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("id"));
    }

    @Test
    void rejectsMissingCoordinates() {
        InvalidLevelException e = invalid(new EntityDefinition("wall-1", "wall", null, 2, null, null, false));
        assertTrue(e.getMessage().contains(SOURCE));
        assertTrue(e.getMessage().contains("wall-1"));
        assertTrue(e.getMessage().contains("缺少坐标"));
    }

    @Test
    void errorMessageCarriesSourceAndReason() {
        InvalidLevelException e = invalid(def("bad", "nope"));
        assertEquals(SOURCE, e.getSource());
        assertEquals("未知实体类型：nope（实体 bad）", e.getReason());
        assertTrue(e.getMessage().contains(SOURCE));
    }
}
