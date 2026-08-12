package com.example.dungeonescape.service;

import com.example.dungeonescape.model.Direction;
import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.GameStatus;
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
import com.example.dungeonescape.model.goal.AndGoal;
import com.example.dungeonescape.model.goal.ExitGoal;
import com.example.dungeonescape.model.goal.Goal;
import com.example.dungeonescape.model.goal.SwitchGoal;
import com.example.dungeonescape.model.goal.TreasureGoal;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link GameEngine} 测试：普通移动、越界、墙、推石、机关、拾取、
 * 开门规则、步数计数、胜利状态与事件通知。
 */
class GameEngineTest {

    /** 测试用目标：永不满足。 */
    private static final class NeverGoal implements Goal {

        @Override
        public boolean isSatisfied(GameState gameState) {
            return false;
        }

        @Override
        public String description() {
            return "永不满足的目标";
        }
    }

    /** 测试用目标：玩家到达指定位置即满足。 */
    private static final class ReachGoal implements Goal {

        private final Position target;

        ReachGoal(Position target) {
            this.target = target;
        }

        @Override
        public boolean isSatisfied(GameState gameState) {
            return gameState.getPlayer().getPosition().equals(target);
        }

        @Override
        public String description() {
            return "到达目标格";
        }
    }

    /** 构建 7x7 网格：玩家位于给定坐标，其余实体由参数指定。 */
    private static GameState state(Position playerPosition, Entity... entities) {
        return new GameState(7, 7, new Player("player", playerPosition),
                Arrays.asList(entities), new NeverGoal(), "规则测试");
    }

    /** 构建 7x7 网格：玩家位于 (3,3)。 */
    private static GameState state(Entity... entities) {
        return state(new Position(3, 3), entities);
    }

    // ---------- 普通移动、越界与墙 ----------

    @Test
    void movesIntoEmptyCell() {
        GameState gameState = state();
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertTrue(result.moved());
        assertEquals(MoveOutcome.MOVED, result.outcome());
        assertFalse(result.doorOpened());
        assertFalse(result.boulderPushed());
        assertFalse(result.treasureCollected());
        assertFalse(result.keyCollected());
        assertEquals(new Position(4, 3), gameState.getPlayer().getPosition());
        assertEquals(1, gameState.getMoveCount());
    }

    @Test
    void moveOutOfBoundsFailsWithoutAnyChange() {
        GameState gameState = state(new Position(0, 0));
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.UP);

        assertFalse(result.moved());
        assertEquals(MoveOutcome.OUT_OF_BOUNDS, result.outcome());
        assertEquals(new Position(0, 0), gameState.getPlayer().getPosition());
        assertEquals(0, gameState.getMoveCount());
    }

    @Test
    void wallBlocksMove() {
        GameState gameState = state(new Wall("wall-2", new Position(4, 3)));
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertFalse(result.moved());
        assertEquals(MoveOutcome.WALL, result.outcome());
        assertEquals(new Position(3, 3), gameState.getPlayer().getPosition());
        assertEquals(0, gameState.getMoveCount());
    }

    // ---------- 推石 ----------

    @Test
    void boulderIsPushedOneCellAndPlayerEnters() {
        Boulder boulder = new Boulder("boulder-2", new Position(4, 3));
        GameState gameState = state(boulder);
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertTrue(result.moved());
        assertEquals(MoveOutcome.MOVED, result.outcome());
        assertTrue(result.boulderPushed());
        assertEquals(new Position(5, 3), boulder.getPosition());
        assertEquals(new Position(4, 3), gameState.getPlayer().getPosition());
        assertEquals(1, gameState.getMoveCount());
    }

    @Test
    void boulderCannotBePushedIntoWall() {
        Wall wall = new Wall("wall-2", new Position(5, 3));
        Boulder boulder = new Boulder("boulder-3", new Position(4, 3));
        GameState gameState = state(boulder, wall);
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertFalse(result.moved());
        assertEquals(MoveOutcome.BOULDER_UNMOVABLE, result.outcome());
        assertEquals(new Position(4, 3), boulder.getPosition());
        assertEquals(new Position(3, 3), gameState.getPlayer().getPosition());
        assertEquals(0, gameState.getMoveCount());
    }

    @Test
    void boulderCannotBePushedIntoClosedDoor() {
        Door door = new Door("door-2", new Position(5, 3), 1, false);
        Boulder boulder = new Boulder("boulder-3", new Position(4, 3));
        GameState gameState = state(boulder, door);
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertFalse(result.moved());
        assertEquals(MoveOutcome.BOULDER_UNMOVABLE, result.outcome());
        assertEquals(new Position(4, 3), boulder.getPosition());
        assertFalse(door.isOpen());
        assertEquals(0, gameState.getMoveCount());
    }

    @Test
    void boulderCannotBePushedIntoAnotherBoulder() {
        Boulder first = new Boulder("boulder-2", new Position(4, 3));
        Boulder second = new Boulder("boulder-3", new Position(5, 3));
        GameState gameState = state(first, second);
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertFalse(result.moved());
        assertEquals(MoveOutcome.BOULDER_UNMOVABLE, result.outcome());
        assertEquals(new Position(4, 3), first.getPosition());
        assertEquals(new Position(5, 3), second.getPosition());
        assertEquals(0, gameState.getMoveCount());
    }

    @Test
    void boulderCannotBePushedOutOfBounds() {
        Boulder boulder = new Boulder("boulder-2", new Position(6, 3));
        GameState gameState = state(new Position(5, 3), boulder);
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertFalse(result.moved());
        assertEquals(MoveOutcome.BOULDER_UNMOVABLE, result.outcome());
        assertEquals(new Position(6, 3), boulder.getPosition());
        assertEquals(0, gameState.getMoveCount());
    }

    // ---------- 机关 ----------

    @Test
    void boulderPushedOntoSwitchCoversIt() {
        FloorSwitch floorSwitch = new FloorSwitch("switch-2", new Position(5, 3));
        Boulder boulder = new Boulder("boulder-3", new Position(4, 3));
        GameState gameState = state(boulder, floorSwitch);
        GameEngine engine = new GameEngine(gameState);

        assertFalse(gameState.isSwitchCovered(new Position(5, 3)));

        engine.move(Direction.RIGHT);

        assertTrue(gameState.isSwitchCovered(new Position(5, 3)));
        assertEquals(new Position(5, 3), boulder.getPosition());
    }

    @Test
    void boulderPushedOffSwitchReleasesIt() {
        FloorSwitch floorSwitch = new FloorSwitch("switch-2", new Position(5, 3));
        Boulder boulder = new Boulder("boulder-3", new Position(5, 3));
        GameState gameState = state(new Position(4, 3), boulder, floorSwitch);
        GameEngine engine = new GameEngine(gameState);

        assertTrue(gameState.isSwitchCovered(new Position(5, 3)));

        engine.move(Direction.RIGHT);

        assertFalse(gameState.isSwitchCovered(new Position(5, 3)));
        assertEquals(new Position(6, 3), boulder.getPosition());
        assertEquals(new Position(5, 3), gameState.getPlayer().getPosition());
    }

    // ---------- 拾取 ----------

    @Test
    void treasureIsCollectedWhenEnteringItsCell() {
        Treasure treasure = new Treasure("treasure-2", new Position(4, 3));
        GameState gameState = state(treasure);
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertTrue(result.moved());
        assertTrue(result.treasureCollected());
        assertEquals(1, gameState.getPlayer().getTreasureCount());
        assertTrue(gameState.entitiesAt(new Position(4, 3)).isEmpty());
    }

    @Test
    void keyIsCollectedWhenPlayerHasNoKey() {
        Key key = new Key("key-2", new Position(4, 3), 7);
        GameState gameState = state(key);
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertTrue(result.moved());
        assertTrue(result.keyCollected());
        assertTrue(gameState.getPlayer().hasKey());
        assertEquals(7, gameState.getPlayer().getKey());
        assertTrue(gameState.entitiesAt(new Position(4, 3)).isEmpty());
    }

    @Test
    void keyStaysOnMapWhenPlayerAlreadyHasOne() {
        Key key = new Key("key-2", new Position(4, 3), 9);
        GameState gameState = state(key);
        gameState.getPlayer().setKey(1);
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertTrue(result.moved()); // 有钥匙仍可进入该格
        assertFalse(result.keyCollected());
        assertEquals(1, gameState.getPlayer().getKey()); // 原钥匙不变
        assertEquals(List.of(key), gameState.entitiesAt(new Position(4, 3))); // 钥匙留在地图
    }

    // ---------- 门与钥匙 ----------

    @Test
    void closedDoorBlocksWithoutKey() {
        Door door = new Door("door-2", new Position(4, 3), 7, false);
        GameState gameState = state(door);
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertFalse(result.moved());
        assertEquals(MoveOutcome.DOOR_LOCKED, result.outcome());
        assertFalse(door.isOpen());
        assertEquals(new Position(3, 3), gameState.getPlayer().getPosition());
        assertEquals(0, gameState.getMoveCount());
    }

    @Test
    void closedDoorBlocksWithWrongKey() {
        Door door = new Door("door-2", new Position(4, 3), 7, false);
        GameState gameState = state(door);
        gameState.getPlayer().setKey(3);
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertFalse(result.moved());
        assertEquals(MoveOutcome.DOOR_LOCKED, result.outcome());
        assertFalse(door.isOpen());
        assertEquals(3, gameState.getPlayer().getKey()); // 错误钥匙不被消耗
        assertEquals(0, gameState.getMoveCount());
    }

    @Test
    void closedDoorOpensAndConsumesMatchingKey() {
        Door door = new Door("door-2", new Position(4, 3), 7, false);
        GameState gameState = state(door);
        gameState.getPlayer().setKey(7);
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertTrue(result.moved());
        assertEquals(MoveOutcome.MOVED, result.outcome());
        assertTrue(result.doorOpened());
        assertTrue(door.isOpen());
        assertFalse(gameState.getPlayer().hasKey()); // 钥匙被消耗
        assertNull(gameState.getPlayer().getKey());
        assertEquals(new Position(4, 3), gameState.getPlayer().getPosition());
        assertEquals(1, gameState.getMoveCount());
    }

    @Test
    void openedDoorStaysOpenAndRemainsPassableWithoutKey() {
        Door door = new Door("door-2", new Position(4, 3), 7, false);
        GameState gameState = state(door);
        gameState.getPlayer().setKey(7);
        GameEngine engine = new GameEngine(gameState);

        engine.move(Direction.RIGHT);  // 开门并通过（钥匙已消耗）
        engine.move(Direction.LEFT);   // 离开门所在格
        MoveResult reenter = engine.move(Direction.RIGHT); // 无钥匙再次进入

        assertTrue(reenter.moved());
        assertEquals(MoveOutcome.MOVED, reenter.outcome());
        assertFalse(reenter.doorOpened());
        assertTrue(door.isOpen());
        assertEquals(new Position(4, 3), gameState.getPlayer().getPosition());
        assertEquals(3, gameState.getMoveCount());
    }

    // ---------- 步数计数 ----------

    @Test
    void moveCountOnlyIncrementsOnSuccessfulMoves() {
        Wall wall = new Wall("wall-2", new Position(4, 3));
        Boulder boulder = new Boulder("boulder-3", new Position(2, 3));
        Door door = new Door("door-4", new Position(2, 4), 7, false); // 推石后玩家在 (2,3)，向下即 (2,4)
        GameState gameState = state(wall, boulder, door);
        GameEngine engine = new GameEngine(gameState);

        MoveResult r;

        r = engine.move(Direction.RIGHT); // 撞墙
        assertFalse(r.moved());
        assertEquals(0, gameState.getMoveCount());

        r = engine.move(Direction.LEFT); // 推石成功
        assertTrue(r.moved());
        assertTrue(r.boulderPushed());
        assertEquals(1, gameState.getMoveCount());

        r = engine.move(Direction.DOWN); // 无钥匙的门
        assertFalse(r.moved());
        assertEquals(1, gameState.getMoveCount());

        r = engine.move(Direction.UP); // 普通移动成功
        assertTrue(r.moved());
        assertEquals(2, gameState.getMoveCount());
    }

    // ---------- 胜利与事件 ----------

    @Test
    void reachingGoalSetsWonAndNotifiesListener() {
        Exit exit = new Exit("exit-10", new Position(4, 3));
        GameState gameState = new GameState(7, 7, new Player("player", new Position(3, 3)),
                List.of(exit), new ReachGoal(exit.getPosition()), "胜利测试");
        GameEngine engine = new GameEngine(gameState);
        AtomicInteger wonCount = new AtomicInteger();
        GameState[] wonState = new GameState[1];
        engine.addEventListener(gs -> {
            wonCount.incrementAndGet();
            wonState[0] = gs;
        });

        MoveResult result = engine.move(Direction.RIGHT);

        assertTrue(result.moved());
        assertEquals(GameStatus.WON, gameState.getStatus());
        assertEquals(1, gameState.getMoveCount());
        assertEquals(1, wonCount.get());
        assertSame(gameState, wonState[0]);
    }

    @Test
    void movesAfterWinChangeNothingAndDoNotNotifyAgain() {
        Exit exit = new Exit("exit-10", new Position(4, 3));
        GameState gameState = new GameState(7, 7, new Player("player", new Position(3, 3)),
                List.of(exit), new ReachGoal(exit.getPosition()), "胜利测试");
        GameEngine engine = new GameEngine(gameState);
        AtomicInteger wonCount = new AtomicInteger();
        engine.addEventListener(gs -> wonCount.incrementAndGet());

        engine.move(Direction.RIGHT); // 达成胜利
        Position before = gameState.getPlayer().getPosition();
        int moves = gameState.getMoveCount();

        MoveResult result = engine.move(Direction.LEFT);

        assertEquals(MoveOutcome.NOT_PLAYING, result.outcome());
        assertFalse(result.moved());
        assertEquals(before, gameState.getPlayer().getPosition());
        assertEquals(moves, gameState.getMoveCount());
        assertEquals(GameStatus.WON, gameState.getStatus());
        assertEquals(1, wonCount.get()); // 不再通知
    }

    @Test
    void movesWhileLostAreIgnored() {
        GameState gameState = state();
        gameState.setStatus(GameStatus.LOST);
        GameEngine engine = new GameEngine(gameState);

        MoveResult result = engine.move(Direction.RIGHT);

        assertEquals(MoveOutcome.NOT_PLAYING, result.outcome());
        assertFalse(result.moved());
        assertEquals(new Position(3, 3), gameState.getPlayer().getPosition());
        assertEquals(0, gameState.getMoveCount());
        assertEquals(GameStatus.LOST, gameState.getStatus());
    }

    // ---------- 阶段 3：目标系统集成 ----------

    @Test
    void lastMoveSatisfyingGoalSetsWonAndNotifiesOnce() {
        Treasure treasure = new Treasure("treasure-2", new Position(4, 3));
        Exit exit = new Exit("exit-3", new Position(5, 3));
        GameState gameState = new GameState(7, 7, new Player("player", new Position(3, 3)),
                List.of(treasure, exit), new AndGoal(new ExitGoal(), new TreasureGoal(1)),
                "目标集成测试");
        GameEngine engine = new GameEngine(gameState);
        AtomicInteger wonCount = new AtomicInteger();
        engine.addEventListener(gs -> wonCount.incrementAndGet());

        MoveResult first = engine.move(Direction.RIGHT); // 拾取宝物，尚未胜利

        assertEquals(MoveOutcome.MOVED, first.outcome());
        assertTrue(first.treasureCollected());
        assertEquals(GameStatus.PLAYING, gameState.getStatus());
        assertEquals(0, wonCount.get());

        MoveResult last = engine.move(Direction.RIGHT); // 走上出口，最后一步满足全部目标

        assertTrue(last.moved());
        assertEquals(GameStatus.WON, gameState.getStatus());
        assertEquals(1, gameState.getPlayer().getTreasureCount());
        assertEquals(2, gameState.getMoveCount());
        assertEquals(1, wonCount.get()); // 事件只通知一次
    }

    @Test
    void standingOnExitBeforeCollectingTreasureDoesNotWin() {
        Exit exit = new Exit("exit-2", new Position(4, 3));
        Treasure treasure = new Treasure("treasure-3", new Position(4, 4));
        GameState gameState = new GameState(7, 7, new Player("player", new Position(3, 3)),
                List.of(exit, treasure), new AndGoal(new ExitGoal(), new TreasureGoal(1)),
                "目标集成测试");
        GameEngine engine = new GameEngine(gameState);
        AtomicInteger wonCount = new AtomicInteger();
        engine.addEventListener(gs -> wonCount.incrementAndGet());

        engine.move(Direction.RIGHT); // 先踩出口：出口满足、宝物未收齐
        assertEquals(GameStatus.PLAYING, gameState.getStatus());
        assertEquals(0, wonCount.get());

        engine.move(Direction.DOWN); // 离开出口并拾取宝物：宝物满足、出口不再满足
        assertEquals(GameStatus.PLAYING, gameState.getStatus());
        assertEquals(0, wonCount.get());
        assertEquals(1, gameState.getPlayer().getTreasureCount());

        engine.move(Direction.UP); // 再次走上出口：最后一步满足全部目标
        assertEquals(GameStatus.WON, gameState.getStatus());
        assertEquals(1, wonCount.get());
    }

    @Test
    void coveringLastSwitchByPushingBoulderWins() {
        Boulder boulder = new Boulder("boulder-2", new Position(4, 3));
        FloorSwitch floorSwitch = new FloorSwitch("switch-3", new Position(5, 3));
        GameState gameState = new GameState(7, 7, new Player("player", new Position(3, 3)),
                List.of(boulder, floorSwitch), new SwitchGoal(), "目标集成测试");
        GameEngine engine = new GameEngine(gameState);
        AtomicInteger wonCount = new AtomicInteger();
        engine.addEventListener(gs -> wonCount.incrementAndGet());

        MoveResult result = engine.move(Direction.RIGHT); // 推石压住唯一机关

        assertTrue(result.moved());
        assertTrue(result.boulderPushed());
        assertEquals(GameStatus.WON, gameState.getStatus());
        assertEquals(1, gameState.getMoveCount());
        assertEquals(1, wonCount.get()); // 事件只通知一次
    }
}
