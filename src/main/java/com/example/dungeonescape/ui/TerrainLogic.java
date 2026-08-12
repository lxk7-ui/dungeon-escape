package com.example.dungeonescape.ui;

import com.example.dungeonescape.model.Direction;
import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.Position;
import com.example.dungeonescape.model.entity.Wall;

/**
 * 地形连通判定（纯逻辑，不依赖 JavaFX，可直接单元测试）。
 *
 * <p>依据当前位置四邻的可行走拓扑选择地形素材：
 * <ul>
 *   <li>墙格（含 Wall 实体）→ {@link TerrainKind#WALL}；</li>
 *   <li>左右直连（东、西均可行走）→ {@link TerrainKind#H_ROAD}；</li>
 *   <li>上下直连（南、北均可行走）→ {@link TerrainKind#V_ROAD}；</li>
 *   <li>两个垂直方向连接 → 相应转角（{@link TerrainKind#CORNER_NW} /
 *       {@link TerrainKind#CORNER_NE} / {@link TerrainKind#CORNER_SW} /
 *       {@link TerrainKind#CORNER_SE}）；</li>
 *   <li>单端通道 → 与唯一连通方向一致的直路（南北向走竖路、东西向走横路）；</li>
 *   <li>三向/四向/孤立格（房间区域）→ {@link TerrainKind#GROUND}。</li>
 * </ul>
 *
 * <p>地形语义下的“可行走”仅排除墙：门（无论开合）与巨石所在格视为通道，
 * 保证走廊地形不因门锁/推石状态而断裂；本类不参与任何移动规则判定，
 * 玩家能否进入某格仍由 {@link GameState#isBlocked(Position)} 决定。
 */
public final class TerrainLogic {

    private TerrainLogic() {
    }

    /**
     * 依据四邻可行走布尔值选择地形素材（纯函数，供单测覆盖全部拓扑）。
     *
     * @param north 北邻可行走
     * @param south 南邻可行走
     * @param west  西邻可行走
     * @param east  东邻可行走
     * @return 对应 {@link TerrainKind}
     */
    public static TerrainKind terrainFor(boolean north, boolean south, boolean west, boolean east) {
        int count = (north ? 1 : 0) + (south ? 1 : 0) + (west ? 1 : 0) + (east ? 1 : 0);
        if (north && south && !west && !east) {
            return TerrainKind.V_ROAD;
        }
        if (west && east && !north && !south) {
            return TerrainKind.H_ROAD;
        }
        if (count == 2) {
            if (north && east) {
                return TerrainKind.CORNER_NE;
            }
            if (north && west) {
                return TerrainKind.CORNER_NW;
            }
            if (south && east) {
                return TerrainKind.CORNER_SE;
            }
            if (south && west) {
                return TerrainKind.CORNER_SW;
            }
        }
        if (count == 1) {
            // 单端通道：与唯一连通方向一致的直路
            return (north || south) ? TerrainKind.V_ROAD : TerrainKind.H_ROAD;
        }
        // 三向 / 四向 / 孤立格：房间区域使用普通地面
        return TerrainKind.GROUND;
    }

    /**
     * 依据游戏局面为指定位置选择地形素材。
     *
     * @param state    当前局面
     * @param position 目标格
     * @return 对应 {@link TerrainKind}
     */
    public static TerrainKind terrainFor(GameState state, Position position) {
        if (hasWall(state, position)) {
            return TerrainKind.WALL;
        }
        return terrainFor(
                walkable(state, position.move(Direction.UP)),
                walkable(state, position.move(Direction.DOWN)),
                walkable(state, position.move(Direction.LEFT)),
                walkable(state, position.move(Direction.RIGHT)));
    }

    /**
     * 地形语义下的可行走：位于网格内且该格不含墙。
     *
     * <p>地图边界（网格外）视为不可行走；门、巨石、机关等实体不影响地形连通。
     */
    public static boolean walkable(GameState state, Position position) {
        return state.isInside(position) && !hasWall(state, position);
    }

    private static boolean hasWall(GameState state, Position position) {
        return state.entitiesAt(position).stream().anyMatch(Wall.class::isInstance);
    }
}
