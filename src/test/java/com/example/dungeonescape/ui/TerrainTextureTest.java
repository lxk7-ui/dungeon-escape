package com.example.dungeonescape.ui;

import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.Position;
import com.example.dungeonescape.model.entity.Player;
import com.example.dungeonescape.model.goal.ExitGoal;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 连贯地形图集（dungeon-terrain-coherent.png）测试：各素材 viewport 精确取值与图集
 * 边界约束、转角旋转角、满格 48×48 铺设（关等比/关平滑）、默认提供者加载真实
 * 1672×941 图集，以及图集不可用时三参数地形背景的回退。
 *
 * <p>JavaFX 对象构造依赖 Toolkit（与 {@code SpritesheetViewTest} 相同的既有模式）；
 * 每个用例显式注入提供者，结束后恢复默认。
 */
class TerrainTextureTest {

    @AfterEach
    void restoreDefaultProviders() {
        TerrainTexture.setSheetProvider(TerrainTexture.DEFAULT_SHEET_PROVIDER);
        EntityViewFactory.setSpriteProvider(EntityViewFactory.DEFAULT_SPRITE_PROVIDER);
    }

    @Test
    void viewportForEachKindIsExact() {
        // 顶部一排完整素材的紧裁矩形（x, y, 宽, 高）
        assertEquals(new Rectangle2D(610, 82, 219, 227), TerrainTexture.viewportFor(TerrainKind.GROUND));
        assertEquals(new Rectangle2D(861, 82, 234, 229), TerrainTexture.viewportFor(TerrainKind.H_ROAD));
        assertEquals(new Rectangle2D(1126, 82, 213, 229), TerrainTexture.viewportFor(TerrainKind.V_ROAD));
        assertEquals(new Rectangle2D(340, 82, 237, 90), TerrainTexture.viewportFor(TerrainKind.WALL));
        // 四个转角共用同一基准裁剪区
        assertEquals(new Rectangle2D(1373, 82, 227, 229), TerrainTexture.viewportFor(TerrainKind.CORNER_NW));
        assertEquals(new Rectangle2D(1373, 82, 227, 229), TerrainTexture.viewportFor(TerrainKind.CORNER_NE));
        assertEquals(new Rectangle2D(1373, 82, 227, 229), TerrainTexture.viewportFor(TerrainKind.CORNER_SW));
        assertEquals(new Rectangle2D(1373, 82, 227, 229), TerrainTexture.viewportFor(TerrainKind.CORNER_SE));
    }

    @Test
    void viewportsLieInTheirSemanticRegions() {
        // 顶部一排四个素材的语义区间：紧裁矩形必须完全落在对应区间内（排除外侧黑画布）
        assertSemanticRegion(TerrainKind.GROUND, 600, 840, 70, 320);
        assertSemanticRegion(TerrainKind.H_ROAD, 850, 1110, 70, 320);
        assertSemanticRegion(TerrainKind.V_ROAD, 1110, 1350, 70, 320);
        assertSemanticRegion(TerrainKind.CORNER_NW, 1360, 1620, 70, 320);
        // 墙体：顶部一排最左侧的完整砖墙（x 位于其余素材之前，同样处于顶部带 y=70..320）
        assertSemanticRegion(TerrainKind.WALL, 0, 600, 70, 320);
        // 墙体必须是顶部连续砖墙带而非完整方框：高度严格小于 120，防回归到旧值 228
        assertTrue(TerrainTexture.viewportFor(TerrainKind.WALL).getHeight() < 120,
                "WALL viewport 应为顶部连续砖墙带（高 < 120），防止回归到完整方框（高 228）");
    }

    private void assertSemanticRegion(TerrainKind kind, double x0, double x1, double y0, double y1) {
        Rectangle2D rect = TerrainTexture.viewportFor(kind);
        assertTrue(rect.getMinX() >= x0 && rect.getMaxX() <= x1,
                kind + " viewport x 应落在 [" + x0 + "," + x1 + "]，实际 " + rect);
        assertTrue(rect.getMinY() >= y0 && rect.getMaxY() <= y1,
                kind + " viewport y 应落在 [" + y0 + "," + y1 + "]，实际 " + rect);
    }

    @Test
    void everyViewportLiesInsideSheetBounds() {
        for (TerrainKind kind : TerrainKind.values()) {
            Rectangle2D rect = TerrainTexture.viewportFor(kind);
            assertTrue(rect.getMinX() >= 0 && rect.getMinY() >= 0,
                    kind + " viewport 起点不得为负");
            assertTrue(rect.getMaxX() <= TerrainTexture.SHEET_WIDTH,
                    kind + " viewport 右缘 " + rect.getMaxX() + " 超出图集宽 " + TerrainTexture.SHEET_WIDTH);
            assertTrue(rect.getMaxY() <= TerrainTexture.SHEET_HEIGHT,
                    kind + " viewport 下缘 " + rect.getMaxY() + " 超出图集高 " + TerrainTexture.SHEET_HEIGHT);
        }
    }

    @Test
    void cornerRotationsFollowBendDirection() {
        // 基准裁剪区为“道路自西进入、向南折转”（南+西），顺时针旋转覆盖其余三向
        assertEquals(0.0, TerrainTexture.rotationFor(TerrainKind.CORNER_SW));
        assertEquals(90.0, TerrainTexture.rotationFor(TerrainKind.CORNER_NW));
        assertEquals(180.0, TerrainTexture.rotationFor(TerrainKind.CORNER_NE));
        assertEquals(270.0, TerrainTexture.rotationFor(TerrainKind.CORNER_SE));
        for (TerrainKind kind : List.of(TerrainKind.GROUND, TerrainKind.H_ROAD, TerrainKind.V_ROAD, TerrainKind.WALL)) {
            assertEquals(0.0, TerrainTexture.rotationFor(kind), kind + " 不旋转");
        }
    }

    @Test
    void tileFills48x48CellWithCorrectViewportAndRotation() {
        TerrainTexture.setSheetProvider(TerrainTextureTest::loadSheet);
        ImageView corner = assertInstanceOf(ImageView.class, TerrainTexture.createTile(TerrainKind.CORNER_NW));
        assertEquals(EntityViewFactory.CELL_SIZE, corner.getFitWidth(), "地形素材必须铺满 48×48");
        assertEquals(EntityViewFactory.CELL_SIZE, corner.getFitHeight());
        assertFalse(corner.isPreserveRatio(), "铺满要求关闭等比缩放，格与格之间不留黑色缝隙");
        assertFalse(corner.isSmooth(), "像素风应关闭平滑插值");
        assertEquals(TerrainTexture.viewportFor(TerrainKind.CORNER_NW), corner.getViewport());
        assertEquals(TerrainTexture.rotationFor(TerrainKind.CORNER_NW), corner.getRotate());

        ImageView ground = assertInstanceOf(ImageView.class, TerrainTexture.createTile(TerrainKind.GROUND));
        assertEquals(EntityViewFactory.CELL_SIZE, ground.getFitWidth());
        assertEquals(EntityViewFactory.CELL_SIZE, ground.getFitHeight());
        assertEquals(TerrainTexture.viewportFor(TerrainKind.GROUND), ground.getViewport());
        assertEquals(0.0, ground.getRotate(), "非转角不旋转");
    }

    @Test
    void everyKindTileFills48x48Cell() {
        TerrainTexture.setSheetProvider(TerrainTextureTest::loadSheet);
        for (TerrainKind kind : TerrainKind.values()) {
            ImageView tile = assertInstanceOf(ImageView.class, TerrainTexture.createTile(kind));
            assertEquals(EntityViewFactory.CELL_SIZE, tile.getFitWidth(), kind + " 必须铺满 48×48");
            assertEquals(EntityViewFactory.CELL_SIZE, tile.getFitHeight(), kind + " 必须铺满 48×48");
            assertFalse(tile.isPreserveRatio(), kind + " 铺满要求关闭等比缩放");
            assertFalse(tile.isSmooth(), kind + " 像素风应关闭平滑插值");
            assertEquals(TerrainTexture.viewportFor(kind), tile.getViewport(), kind + " viewport 应生效");
            assertEquals(TerrainTexture.rotationFor(kind), tile.getRotate(), kind + " 旋转角应生效");
        }
    }

    @Test
    void eachTileIsFreshInstance() {
        TerrainTexture.setSheetProvider(TerrainTextureTest::loadSheet);
        assertNotSame(TerrainTexture.createTile(TerrainKind.WALL), TerrainTexture.createTile(TerrainKind.WALL),
                "每次调用必须返回全新节点，避免重复加入场景图");
    }

    @Test
    void defaultProviderLoadsRealCoherentSheet() {
        TerrainTexture.setSheetProvider(TerrainTexture.DEFAULT_SHEET_PROVIDER);
        assertTrue(TerrainTexture.isAvailable(), "真实地形图集必须可加载");
        ImageView tile = assertInstanceOf(ImageView.class, TerrainTexture.createTile(TerrainKind.GROUND));
        Image sheet = tile.getImage();
        assertEquals(TerrainTexture.SHEET_WIDTH, sheet.getWidth(), 0.5, "图集应为 1672 宽");
        assertEquals(TerrainTexture.SHEET_HEIGHT, sheet.getHeight(), 0.5, "图集应为 941 高");
        assertFalse(sheet.isError(), "图集应能正常解码");
    }

    @Test
    void terrainBackgroundFallsBackToPlainFloorWhenSheetUnavailable() {
        GameState state = minimalState();
        // 旧背景路径：实体图集也禁用时回退到纯 Shape 棋盘格
        EntityViewFactory.setSpriteProvider(null);
        byte[] invalidPng = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x00, 0x00};
        for (Supplier<Image> provider : List.<Supplier<Image>>of(
                () -> null,
                () -> {
                    throw new IllegalStateException("模拟图集加载异常");
                },
                () -> new Image(new ByteArrayInputStream(invalidPng)))) {
            TerrainTexture.setSheetProvider(provider);
            assertFalse(TerrainTexture.isAvailable(), "不可用提供者应判定图集不可用");
            Node background = EntityViewFactory.createCellBackground(1, 1, state);
            Rectangle rect = assertInstanceOf(Rectangle.class, background, "图集不可用时地形背景应回退到棋盘格");
            assertEquals(EntityViewFactory.CELL_SIZE, rect.getWidth());
            assertEquals(EntityViewFactory.CELL_SIZE, rect.getHeight());
        }
    }

    @Test
    void terrainBackgroundFallsBackWhenSheetSizeMismatch() throws Exception {
        // 用 JDK ImageIO 生成一张 10x10 PNG 注入：尺寸不符 1672×941 时应判定不可用并回退
        BufferedImage tiny = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(tiny, "png", out);
        TerrainTexture.setSheetProvider(() -> new Image(new ByteArrayInputStream(out.toByteArray())));
        EntityViewFactory.setSpriteProvider(null);
        GameState state = minimalState();
        assertFalse(TerrainTexture.isAvailable(), "尺寸不符 1672×941 时应判定图集不可用");
        Rectangle rect = assertInstanceOf(Rectangle.class, EntityViewFactory.createCellBackground(1, 1, state),
                "尺寸不符时应回退到棋盘格");
        assertEquals(EntityViewFactory.CELL_SIZE, rect.getWidth());
        assertEquals(EntityViewFactory.CELL_SIZE, rect.getHeight());
    }

    @Test
    void terrainBackgroundFallsBackWhenStateIsNull() {
        TerrainTexture.setSheetProvider(TerrainTextureTest::loadSheet);
        assertTrue(TerrainTexture.isAvailable());
        // state 为 null 时即使图集可用也应回退到旧背景：与两参数背景完全一致
        Node fallback = EntityViewFactory.createCellBackground(1, 1, null);
        Node plain = EntityViewFactory.createCellBackground(1, 1);
        assertEquals(plain.getClass(), fallback.getClass(), "state 为 null 时应走与两参数背景相同的路径");
        if (fallback instanceof ImageView fallbackView && plain instanceof ImageView plainView) {
            assertEquals(plainView.getViewport(), fallbackView.getViewport(), "回退背景应与两参数背景使用同一素材");
        }
    }

    @Test
    void terrainBackgroundUsesTerrainTileWhenAvailable() {
        TerrainTexture.setSheetProvider(TerrainTextureTest::loadSheet);
        GameState state = minimalState();
        // 2×2 空地图的 (1,1) 格：北、西可行走 → 北西转角
        Node background = EntityViewFactory.createCellBackground(1, 1, state);
        ImageView tile = assertInstanceOf(ImageView.class, background, "图集可用时应走地形图集路径");
        assertEquals(TerrainTexture.viewportFor(TerrainKind.CORNER_NW), tile.getViewport());
        assertEquals(TerrainTexture.rotationFor(TerrainKind.CORNER_NW), tile.getRotate());
        assertEquals(EntityViewFactory.CELL_SIZE, tile.getFitWidth());
        assertEquals(EntityViewFactory.CELL_SIZE, tile.getFitHeight());
    }

    @Test
    void terrainBackgroundTilesShareCachedSheetImage() {
        TerrainTexture.setSheetProvider(TerrainTextureTest::loadSheet);
        GameState state = minimalState();
        ImageView first = assertInstanceOf(ImageView.class, EntityViewFactory.createCellBackground(1, 1, state));
        ImageView second = assertInstanceOf(ImageView.class, EntityViewFactory.createCellBackground(1, 1, state));
        assertSame(first.getImage(), second.getImage(), "图集 Image 应解析一次并全局共享缓存");
    }

    /** 从 classpath 加载真实地形图集（供注入）。 */
    private static Image loadSheet() {
        return new Image(TerrainTextureTest.class.getResourceAsStream("/" + TerrainTexture.SHEET_PATH));
    }

    /** 最小局面（2x2、无实体、EXIT 目标）。 */
    private static GameState minimalState() {
        Player player = new Player("player", new Position(0, 0));
        return new GameState(2, 2, player, List.of(), new ExitGoal(), "测试关卡");
    }
}
