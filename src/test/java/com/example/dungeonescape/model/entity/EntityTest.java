package com.example.dungeonescape.model.entity;

import com.example.dungeonescape.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 阶段 3.5 实体标识契约测试：
 * 实体 id 必须是非空、非空白的字符串并原样保存；
 * Key/Door 的 keyId/doorId 是独立于实体 id 的数字编号，匹配仍按数字。
 */
class EntityTest {

    // ---------- id 校验 ----------

    @Test
    void allEntitiesRejectNullId() {
        assertThrows(NullPointerException.class, () -> new Player(null, new Position(0, 0)));
        assertThrows(NullPointerException.class, () -> new Wall(null, new Position(0, 0)));
        assertThrows(NullPointerException.class, () -> new Exit(null, new Position(0, 0)));
        assertThrows(NullPointerException.class, () -> new Treasure(null, new Position(0, 0)));
        assertThrows(NullPointerException.class, () -> new Key(null, new Position(0, 0), 1));
        assertThrows(NullPointerException.class, () -> new Door(null, new Position(0, 0), 1, false));
        assertThrows(NullPointerException.class, () -> new Boulder(null, new Position(0, 0)));
        assertThrows(NullPointerException.class, () -> new FloorSwitch(null, new Position(0, 0)));
    }

    @Test
    void allEntitiesRejectBlankId() {
        assertThrows(IllegalArgumentException.class, () -> new Player("", new Position(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new Wall("   ", new Position(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new Exit("\t", new Position(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new Treasure("\n", new Position(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new Key(" ", new Position(0, 0), 1));
        assertThrows(IllegalArgumentException.class, () -> new Door("  ", new Position(0, 0), 1, false));
        assertThrows(IllegalArgumentException.class, () -> new Boulder("", new Position(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new FloorSwitch(" ", new Position(0, 0)));
    }

    // ---------- 字符串 id 保存 ----------

    @Test
    void stringEntityIdsAreStoredAndReturnedVerbatim() {
        Entity player = new Player("player", new Position(0, 0));
        Entity wall = new Wall("wall-1", new Position(0, 1));
        Entity exit = new Exit("exit-1", new Position(0, 2));
        Entity treasure = new Treasure("treasure-1", new Position(0, 3));
        Entity key = new Key("key-1", new Position(0, 4), 7);
        Entity door = new Door("door-1", new Position(0, 5), 7, false);
        Entity boulder = new Boulder("boulder-1", new Position(0, 6));
        Entity floorSwitch = new FloorSwitch("switch-1", new Position(0, 7));

        assertEquals("player", player.getId());
        assertEquals("wall-1", wall.getId());
        assertEquals("exit-1", exit.getId());
        assertEquals("treasure-1", treasure.getId());
        assertEquals("key-1", key.getId());
        assertEquals("door-1", door.getId());
        assertEquals("boulder-1", boulder.getId());
        assertEquals("switch-1", floorSwitch.getId());
    }

    @Test
    void differentEntitiesKeepDistinctStringIds() {
        Wall first = new Wall("wall-1", new Position(1, 1));
        Wall second = new Wall("wall-2", new Position(2, 2));
        Door door = new Door("door-1", new Position(3, 3), 7, false);

        assertEquals("wall-1", first.getId());
        assertEquals("wall-2", second.getId());
        assertNotEquals(first.getId(), second.getId());
        assertNotEquals(first.getId(), door.getId());
    }

    // ---------- Key/Door 仍按数字编号匹配 ----------

    @Test
    void keyAndDoorIdsAreNumericAndIndependentOfEntityId() {
        Key key = new Key("key-red", new Position(0, 0), 7);
        Door matchingDoor = new Door("door-red", new Position(0, 1), 7, false);
        Door otherDoor = new Door("door-blue", new Position(0, 2), 8, false);

        // keyId/doorId 仍为数字编号，与实体字符串 id 无关
        assertEquals(7, key.getKeyId());
        assertEquals(7, matchingDoor.getDoorId());
        assertEquals(8, otherDoor.getDoorId());

        // 匹配规则仍只按数字编号：keyId == doorId 即匹配，不同则不匹配
        assertEquals(key.getKeyId(), matchingDoor.getDoorId());
        assertNotEquals(key.getKeyId(), otherDoor.getDoorId());
    }
}
