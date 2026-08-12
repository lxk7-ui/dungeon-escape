package com.example.dungeonescape.ui;

import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.Position;
import com.example.dungeonescape.model.entity.Boulder;
import com.example.dungeonescape.model.entity.Entity;
import com.example.dungeonescape.model.entity.Exit;
import com.example.dungeonescape.model.entity.FloorSwitch;
import com.example.dungeonescape.model.entity.Player;
import com.example.dungeonescape.model.entity.Treasure;
import com.example.dungeonescape.model.entity.Wall;
import com.example.dungeonescape.model.goal.ExitGoal;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EntityViewFactory 辅助测试：格子尺寸、分层约定、Shape 回退路径的视图类型/尺寸、
 * 每次调用返回全新节点（不重复复用同一 Node）。
 *
 * <p>本类在每个用例前显式禁用图集（{@link EntityViewFactory#setSpriteProvider} 传 null），
 * 验证的是「图集缺失/不可用时自动回退到纯 Shape/Label」的路径；图集路径的测试
 * （viewport 映射、状态选格、Image 共享等）见 {@code SpritesheetViewTest}。
 *
 * <p>仅构造 JavaFX 节点对象，不启动完整 Application。
 */
class EntityViewFactoryTest {

    @BeforeEach
    void disableSpritesheet() {
        EntityViewFactory.setSpriteProvider(null);
    }

    @AfterEach
    void restoreDefaultSpritesheet() {
        EntityViewFactory.setSpriteProvider(EntityViewFactory.DEFAULT_SPRITE_PROVIDER);
    }

    @Test
    void cellSizeIs48Pixels() {
        assertEquals(48.0, EntityViewFactory.CELL_SIZE);
    }

    @Test
    void switchIsLayeredUnderBoulder() {
        assertTrue(EntityViewFactory.layerOf(FloorSwitch.class) < EntityViewFactory.layerOf(Boulder.class),
                "FloorSwitch 应绘制在 Boulder 之下（机关被巨石压住时仍可见）");
    }

    @Test
    void exitIsLayeredUnderPlayer() {
        assertTrue(EntityViewFactory.layerOf(Exit.class) < EntityViewFactory.layerOf(Player.class),
                "Exit 应绘制在 Player 之下（玩家踩上出口时仍可见）");
    }

    @Test
    void playerIsTopmostLayer() {
        List<Class<? extends Entity>> types = List.of(
                FloorSwitch.class, Exit.class, Treasure.class, Boulder.class, Wall.class);
        int playerLayer = EntityViewFactory.layerOf(Player.class);
        for (Class<? extends Entity> type : types) {
            assertTrue(EntityViewFactory.layerOf(type) < playerLayer,
                    type.getSimpleName() + " 应在 Player 之下");
        }
    }

    @Test
    void unknownEntityTypeFallsBackToDefaultLayer() {
        assertEquals(1, EntityViewFactory.layerOf(Entity.class));
    }

    @Test
    void wallViewIsFullCellRectangle() {
        GameState state = minimalState();
        Node view = EntityViewFactory.createEntityView(new Wall("wall", new Position(0, 0)), state);
        Rectangle rect = assertInstanceOf(Rectangle.class, view);
        assertEquals(EntityViewFactory.CELL_SIZE, rect.getWidth());
        assertEquals(EntityViewFactory.CELL_SIZE, rect.getHeight());
    }

    @Test
    void boulderViewIsCircle() {
        GameState state = minimalState();
        Node view = EntityViewFactory.createEntityView(new Boulder("boulder", new Position(0, 0)), state);
        assertInstanceOf(Circle.class, view);
    }

    @Test
    void treasureViewIsDiamondPolygon() {
        GameState state = minimalState();
        Node view = EntityViewFactory.createEntityView(new Treasure("treasure", new Position(0, 0)), state);
        assertInstanceOf(Polygon.class, view);
    }

    @Test
    void cellBackgroundIsFullCellRectangle() {
        Node floor = EntityViewFactory.createCellBackground(3, 7);
        Rectangle rect = assertInstanceOf(Rectangle.class, floor);
        assertEquals(EntityViewFactory.CELL_SIZE, rect.getWidth());
        assertEquals(EntityViewFactory.CELL_SIZE, rect.getHeight());
    }

    @Test
    void eachCallReturnsFreshNodeInstance() {
        GameState state = minimalState();
        Wall wall = new Wall("wall", new Position(0, 0));
        Node first = EntityViewFactory.createEntityView(wall, state);
        Node second = EntityViewFactory.createEntityView(wall, state);
        assertNotSame(first, second, "同一个实体每次应返回全新节点，避免重复加入场景图");
    }

    /** 最小可绘制局面（2x2、无实体、EXIT 目标）。 */
    private static GameState minimalState() {
        Player player = new Player("player", new Position(0, 0));
        return new GameState(2, 2, player, List.of(), new ExitGoal(), "测试关卡");
    }
}
