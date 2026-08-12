package com.example.dungeonescape.ui;

/**
 * 地形素材种类。
 *
 * <p>由 {@link TerrainLogic} 依据当前位置四邻的可行走拓扑选出，
 * 再由 {@link TerrainTexture} 映射到连贯地形图集（dungeon-terrain-coherent.png）
 * 的紧贴素材 viewport。
 *
 * <p>转角按连通方向命名：{@link #CORNER_NW} 表示同时连通北、西两向，其余类推。
 */
public enum TerrainKind {

    /** 普通地面：三向/四向/房间区域/孤立格。 */
    GROUND,

    /** 横向直路：左右直连的通道。 */
    H_ROAD,

    /** 纵向直路：上下直连的通道。 */
    V_ROAD,

    /** 转角：同时连通北、西两向。 */
    CORNER_NW,

    /** 转角：同时连通北、东两向。 */
    CORNER_NE,

    /** 转角：同时连通南、西两向。 */
    CORNER_SW,

    /** 转角：同时连通南、东两向。 */
    CORNER_SE,

    /** 墙体（含 Wall 实体所在格）。 */
    WALL
}
