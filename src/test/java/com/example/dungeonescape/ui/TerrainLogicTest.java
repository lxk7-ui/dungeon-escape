package com.example.dungeonescape.ui;

import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.Position;
import com.example.dungeonescape.model.entity.Boulder;
import com.example.dungeonescape.model.entity.Door;
import com.example.dungeonescape.model.entity.Entity;
import com.example.dungeonescape.model.entity.Player;
import com.example.dungeonescape.model.entity.Wall;
import com.example.dungeonescape.model.goal.ExitGoal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 地形连通拓扑测试：横路、竖路、四转角、单端、三向、四向、孤立格，
 * 以及基于 GameState 的墙判定、门/巨石视为通道、地图边界语义。
 *
 * <p>纯逻辑测试，不构造 JavaFX 对象、不依赖 Toolkit。
 */
class TerrainLogicTest {

    // ---- 直路 ----

    @Test
    void horizontalRoadWhenWestAndEastWalkable() {
        assertEquals(TerrainKind.H_ROAD, TerrainLogic.terrainFor(false, false, true, true));
    }

    @Test
    void verticalRoadWhenNorthAndSouthWalkable() {
        assertEquals(TerrainKind.V_ROAD, TerrainLogic.terrainFor(true, true, false, false));
    }

    // ---- 四转角 ----

    @Test
    void fourCornerTopologies() {
        assertEquals(TerrainKind.CORNER_NW, TerrainLogic.terrainFor(true, false, true, false));
        assertEquals(TerrainKind.CORNER_NE, TerrainLogic.terrainFor(true, false, false, true));
        assertEquals(TerrainKind.CORNER_SW, TerrainLogic.terrainFor(false, true, true, false));
        assertEquals(TerrainKind.CORNER_SE, TerrainLogic.terrainFor(false, true, false, true));
    }

    // ---- 单端 ----

    @Test
    void deadEndUsesDirectionConsistentRoad() {
        // 只连通北/南 → 竖路；只连通西/东 → 横路
        assertEquals(TerrainKind.V_ROAD, TerrainLogic.terrainFor(true, false, false, false), "北向单端走竖路");
        assertEquals(TerrainKind.V_ROAD, TerrainLogic.terrainFor(false, true, false, false), "南向单端走竖路");
        assertEquals(TerrainKind.H_ROAD, TerrainLogic.terrainFor(false, false, true, false), "西向单端走横路");
        assertEquals(TerrainKind.H_ROAD, TerrainLogic.terrainFor(false, false, false, true), "东向单端走横路");
    }

    // ---- 三向 ----

    @Test
    void threeWayJunctionIsGround() {
        assertEquals(TerrainKind.GROUND, TerrainLogic.terrainFor(true, true, true, false));
        assertEquals(TerrainKind.GROUND, TerrainLogic.terrainFor(true, true, false, true));
        assertEquals(TerrainKind.GROUND, TerrainLogic.terrainFor(true, false, true, true));
        assertEquals(TerrainKind.GROUND, TerrainLogic.terrainFor(false, true, true, true));
    }

    // ---- 四向 ----

    @Test
    void fourWayJunctionIsGround() {
        assertEquals(TerrainKind.GROUND, TerrainLogic.terrainFor(true, true, true, true));
    }

    // ---- 孤立格 ----

    @Test
    void isolatedCellIsGround() {
        assertEquals(TerrainKind.GROUND, TerrainLogic.terrainFor(false, false, false, false));
    }

    // ---- 基于 GameState 的判定 ----

    @Test
    void wallCellIsWallRegardlessOfNeighbors() {
        GameState state = stateWith(new Wall("wall", new Position(1, 1)));
        assertEquals(TerrainKind.WALL, TerrainLogic.terrainFor(state, new Position(1, 1)));
    }

    @Test
    void doorDoesNotBreakCorridor() {
        // 南北两侧是墙、东西通畅、中间是门 → 走廊拓扑仍按横路绘制
        GameState closed = stateWith(new Wall("wall", new Position(1, 0)),
                new Wall("wall", new Position(1, 2)),
                new Door("door", new Position(1, 1), 1, false));
        assertEquals(TerrainKind.H_ROAD, TerrainLogic.terrainFor(closed, new Position(1, 1)),
                "关闭的门不打断走廊地形");
        GameState open = stateWith(new Wall("wall", new Position(1, 0)),
                new Wall("wall", new Position(1, 2)),
                new Door("door", new Position(1, 1), 1, true));
        assertEquals(TerrainKind.H_ROAD, TerrainLogic.terrainFor(open, new Position(1, 1)),
                "打开的门同样不打断走廊地形");
    }

    @Test
    void boulderCellIsWalkableForTerrain() {
        GameState state = stateWith(new Wall("wall", new Position(1, 0)),
                new Wall("wall", new Position(1, 2)),
                new Boulder("boulder", new Position(1, 1)));
        assertEquals(TerrainKind.H_ROAD, TerrainLogic.terrainFor(state, new Position(1, 1)),
                "巨石格视为通道，走廊地形不因推石状态断裂");
    }

    @Test
    void mapBoundaryNeighborsCountAsNonWalkable() {
        // 空 3×3 地图的角落格 (0,0)：仅南、东可行走 → 南东转角
        GameState state = emptyState(3, 3);
        assertEquals(TerrainKind.CORNER_SE, TerrainLogic.terrainFor(state, new Position(0, 0)),
                "北、西在地图外应视为不可行走");
        // 边格 (0,1)：北、南、东可行走（三向）→ 普通地面
        assertEquals(TerrainKind.GROUND, TerrainLogic.terrainFor(state, new Position(0, 1)));
        // 中心格 (1,1)：四向全通 → 普通地面
        assertEquals(TerrainKind.GROUND, TerrainLogic.terrainFor(state, new Position(1, 1)));
    }

    @Test
    void walkableIsInsideMapAndWallFree() {
        GameState state = stateWith(new Wall("wall", new Position(1, 1)));
        assertFalse(TerrainLogic.walkable(state, new Position(5, 5)), "地图外不可行走");
        assertFalse(TerrainLogic.walkable(state, new Position(1, 1)), "墙格不可行走");
        assertTrue(TerrainLogic.walkable(state, new Position(0, 0)), "普通空格可行走");
    }

    private static GameState emptyState(int width, int height) {
        return new GameState(width, height, new Player("player", new Position(0, 0)),
                List.of(), new ExitGoal(), "测试关卡");
    }

    private static GameState stateWith(Entity... entities) {
        return new GameState(4, 4, new Player("player", new Position(0, 0)),
                List.of(entities), new ExitGoal(), "测试关卡");
    }
}
