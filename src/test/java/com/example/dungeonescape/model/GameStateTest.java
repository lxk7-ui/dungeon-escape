package com.example.dungeonescape.model;

import com.example.dungeonescape.model.entity.Boulder;
import com.example.dungeonescape.model.entity.Door;
import com.example.dungeonescape.model.entity.Entity;
import com.example.dungeonescape.model.entity.Exit;
import com.example.dungeonescape.model.entity.FloorSwitch;
import com.example.dungeonescape.model.entity.Key;
import com.example.dungeonescape.model.entity.Player;
import com.example.dungeonescape.model.entity.Treasure;
import com.example.dungeonescape.model.entity.Wall;
import com.example.dungeonescape.model.goal.Goal;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link GameState} 测试：查询、阻挡、删除计数、机关触发、只读视图与门开合。
 */
class GameStateTest {

    /** 测试用目标：永不满足。 */
    private static final class DummyGoal implements Goal {

        @Override
        public boolean isSatisfied(GameState gameState) {
            return false;
        }

        @Override
        public String description() {
            return "测试目标";
        }
    }

    /** 构建 5x5 网格：玩家位于 (0,0)，其余实体由参数指定。 */
    private static GameState state(Entity... entities) {
        Player player = new Player("player", new Position(0, 0));
        return new GameState(5, 5, player, Arrays.asList(entities), new DummyGoal(), "测试关卡");
    }

    // ---------- 查询 ----------

    @Test
    void isInsideChecksGridBounds() {
        GameState gameState = state();

        assertTrue(gameState.isInside(new Position(0, 0)));
        assertTrue(gameState.isInside(new Position(4, 4)));
        assertTrue(gameState.isInside(new Position(2, 3)));
        assertFalse(gameState.isInside(new Position(-1, 0)));
        assertFalse(gameState.isInside(new Position(5, 0)));
        assertFalse(gameState.isInside(new Position(0, 5)));
    }

    @Test
    void entitiesAtReturnsEntitiesOnThatPosition() {
        Wall wall = new Wall("wall-1", new Position(2, 2));
        Treasure treasure = new Treasure("treasure-2", new Position(2, 2));
        Key key = new Key("key-3", new Position(4, 0), 7);
        GameState gameState = state(wall, treasure, key);

        assertEquals(List.of(wall, treasure), gameState.entitiesAt(new Position(2, 2)));
        assertEquals(List.of(key), gameState.entitiesAt(new Position(4, 0)));
        assertTrue(gameState.entitiesAt(new Position(0, 4)).isEmpty());
    }

    @Test
    void genericEntityAtFindsEntityOfRequestedType() {
        Wall wall = new Wall("wall-1", new Position(2, 2));
        Key key = new Key("key-2", new Position(2, 2), 5);
        GameState gameState = state(wall, key);

        Optional<Wall> found = gameState.entityAt(new Position(2, 2), Wall.class);
        assertTrue(found.isPresent());
        assertSame(wall, found.get());

        assertTrue(gameState.entityAt(new Position(2, 2), Key.class).isPresent());
        assertTrue(gameState.entityAt(new Position(2, 2), Boulder.class).isEmpty());
        assertTrue(gameState.entityAt(new Position(1, 1), Wall.class).isEmpty());
    }

    @Test
    void gettersExposeStateProperties() {
        Player player = new Player("player", new Position(0, 0));
        DummyGoal goal = new DummyGoal();
        GameState gameState = new GameState(5, 4, player, List.of(),
                goal, 7, GameStatus.WON, "关卡A");

        assertEquals(5, gameState.getWidth());
        assertEquals(4, gameState.getHeight());
        assertSame(player, gameState.getPlayer());
        assertSame(goal, gameState.getGoal());
        assertEquals(7, gameState.getMoveCount());
        assertEquals(GameStatus.WON, gameState.getStatus());
        assertEquals("关卡A", gameState.getLevelName());
    }

    @Test
    void convenienceConstructorDefaultsMoveCountAndStatus() {
        GameState gameState = state();

        assertEquals(0, gameState.getMoveCount());
        assertEquals(GameStatus.PLAYING, gameState.getStatus());
    }

    @Test
    void constructorCopiesGivenEntityList() {
        List<Entity> input = new ArrayList<>(List.of(new Wall("wall-1", new Position(0, 1))));
        GameState gameState = new GameState(5, 5, new Player("player", new Position(0, 0)),
                input, new DummyGoal(), "关卡");

        input.clear();

        assertEquals(1, gameState.countEntities());
    }

    @Test
    void constructorRejectsNonPositiveDimensions() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(0, 5, new Player("player", new Position(0, 0)), List.of(), new DummyGoal(), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(5, -1, new Player("player", new Position(0, 0)), List.of(), new DummyGoal(), "x"));
    }

    @Test
    void incrementMoveCountIncreasesByOne() {
        GameState gameState = state();

        gameState.incrementMoveCount();
        gameState.incrementMoveCount();

        assertEquals(2, gameState.getMoveCount());
    }

    @Test
    void statusCanBeUpdated() {
        GameState gameState = state();

        gameState.setStatus(GameStatus.WON);

        assertEquals(GameStatus.WON, gameState.getStatus());
    }

    @Test
    void entityListExcludesPlayer() {
        GameState gameState = state();

        assertTrue(gameState.getEntities().isEmpty());
        assertEquals(0, gameState.countEntities());
    }

    // ---------- 阻挡 ----------

    @Test
    void blockingEntitiesBlockMovement() {
        Wall wall = new Wall("wall-1", new Position(1, 0));
        Boulder boulder = new Boulder("boulder-2", new Position(2, 0));
        Door closedDoor = new Door("door-3", new Position(3, 0), 1, false);
        GameState gameState = state(wall, boulder, closedDoor);

        assertTrue(gameState.isBlocked(new Position(1, 0)));
        assertTrue(gameState.isBlocked(new Position(2, 0)));
        assertTrue(gameState.isBlocked(new Position(3, 0)));
    }

    @Test
    void passableEntitiesDoNotBlock() {
        Exit exit = new Exit("exit-1", new Position(1, 0));
        Treasure treasure = new Treasure("treasure-2", new Position(2, 0));
        Key key = new Key("key-3", new Position(3, 0), 1);
        FloorSwitch floorSwitch = new FloorSwitch("switch-4", new Position(4, 0));
        GameState gameState = state(exit, treasure, key, floorSwitch);

        assertFalse(gameState.isBlocked(new Position(1, 0)));
        assertFalse(gameState.isBlocked(new Position(2, 0)));
        assertFalse(gameState.isBlocked(new Position(3, 0)));
        assertFalse(gameState.isBlocked(new Position(4, 0)));
    }

    @Test
    void outOfBoundsIsAlwaysBlocked() {
        GameState gameState = state();

        assertTrue(gameState.isBlocked(new Position(-1, 0)));
        assertTrue(gameState.isBlocked(new Position(0, -1)));
        assertTrue(gameState.isBlocked(new Position(5, 2)));
    }

    // ---------- 门打开 ----------

    @Test
    void openDoorNoLongerBlocks() {
        Door door = new Door("door-1", new Position(2, 2), 3, false);
        GameState gameState = state(door);

        assertFalse(door.isOpen());
        assertTrue(door.isBlocking());
        assertTrue(gameState.isBlocked(new Position(2, 2)));

        door.setOpen(true);

        assertTrue(door.isOpen());
        assertFalse(door.isBlocking());
        assertFalse(gameState.isBlocked(new Position(2, 2)));
    }

    // ---------- 删除与计数 ----------

    @Test
    void removeEntityRemovesItAndDecrementsCount() {
        Wall wall = new Wall("wall-1", new Position(1, 0));
        Key key = new Key("key-2", new Position(2, 0), 1);
        GameState gameState = state(wall, key);

        assertEquals(2, gameState.countEntities());

        assertTrue(gameState.removeEntity(wall));
        assertEquals(1, gameState.countEntities());
        assertTrue(gameState.entitiesAt(new Position(1, 0)).isEmpty());

        assertFalse(gameState.removeEntity(wall)); // 已移除，重复删除返回 false
        assertTrue(gameState.removeEntity(key));
        assertEquals(0, gameState.countEntities());
    }

    @Test
    void countEntitiesCanFilterByType() {
        GameState gameState = state(
                new Wall("wall-1", new Position(0, 1)),
                new Wall("wall-2", new Position(0, 2)),
                new Key("key-3", new Position(1, 1), 1),
                new Boulder("boulder-4", new Position(2, 2)));

        assertEquals(4, gameState.countEntities());
        assertEquals(2, gameState.countEntities(Wall.class));
        assertEquals(1, gameState.countEntities(Key.class));
        assertEquals(0, gameState.countEntities(Door.class));
    }

    // ---------- 巨石触发机关 ----------

    @Test
    void floorSwitchIsCoveredOnlyWhenBoulderStandsOnIt() {
        FloorSwitch floorSwitch = new FloorSwitch("switch-1", new Position(2, 2));
        Boulder boulder = new Boulder("boulder-2", new Position(2, 2));
        GameState gameState = state(floorSwitch, boulder);

        assertTrue(gameState.isSwitchCovered(new Position(2, 2)));

        boulder.setPosition(new Position(3, 2));
        assertFalse(gameState.isSwitchCovered(new Position(2, 2))); // 机关上无巨石
        assertFalse(gameState.isSwitchCovered(new Position(3, 2))); // 有巨石但无机关
    }

    @Test
    void playerStandingOnSwitchDoesNotCoverIt() {
        FloorSwitch floorSwitch = new FloorSwitch("switch-1", new Position(1, 0));
        GameState gameState = state(floorSwitch);

        assertFalse(gameState.isSwitchCovered(new Position(1, 0)));
    }

    // ---------- 只读列表 ----------

    @Test
    void entityListIsUnmodifiable() {
        GameState gameState = state(new Wall("wall-1", new Position(0, 1)));

        assertThrows(UnsupportedOperationException.class,
                () -> gameState.getEntities().add(new Wall("wall-9", new Position(4, 4))));
        assertThrows(UnsupportedOperationException.class,
                () -> gameState.getEntities().remove(0));
        assertThrows(UnsupportedOperationException.class,
                () -> gameState.getEntities().clear());
    }

    @Test
    void entitiesAtResultIsUnmodifiable() {
        GameState gameState = state(new Wall("wall-1", new Position(0, 1)));

        assertThrows(UnsupportedOperationException.class,
                () -> gameState.entitiesAt(new Position(0, 1)).add(new Wall("wall-9", new Position(4, 4))));
    }
}
