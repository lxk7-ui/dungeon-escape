package com.example.dungeonescape.ui;

import com.example.dungeonescape.model.GameState;
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
import com.example.dungeonescape.model.goal.ExitGoal;
import com.example.dungeonescape.ui.EntityViewFactory.SpriteCell;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图集视图路径测试：4×3 映射（含门开合、机关被压的状态选格）、严格 362 单元格 viewport、
 * 每次调用返回全新节点但共享同一个缓存的 Image、背景使用 floor 格、
 * 以及可注入方式验证的资源回退逻辑。
 *
 * <p>本类每个用例都显式注入图集提供者（{@link EntityViewFactory#setSpriteProvider}），
 * 结束后恢复默认，不依赖类执行顺序；纯映射断言（spriteCellFor/sourceRect）不依赖 Toolkit。
 */
class SpritesheetViewTest {

    @AfterEach
    void restoreDefaultProvider() {
        EntityViewFactory.setSpriteProvider(EntityViewFactory.DEFAULT_SPRITE_PROVIDER);
    }

    @Test
    void cellMappingMatches4x3Spec() {
        GameState state = minimalState();
        assertEquals(new SpriteCell(0, 0), EntityViewFactory.spriteCellFor(new Player("player", pos(0, 0)), state));
        assertEquals(new SpriteCell(1, 0), EntityViewFactory.spriteCellFor(new Wall("wall", pos(0, 0)), state));
        assertEquals(new SpriteCell(2, 0), EntityViewFactory.spriteCellFor(new Exit("exit", pos(0, 0)), state));
        assertEquals(new SpriteCell(3, 0), EntityViewFactory.spriteCellFor(new Treasure("treasure", pos(0, 0)), state));
        assertEquals(new SpriteCell(0, 1), EntityViewFactory.spriteCellFor(new Key("key", pos(0, 0), 1), state));
        assertEquals(new SpriteCell(3, 1), EntityViewFactory.spriteCellFor(new Boulder("boulder", pos(0, 0)), state));
        assertEquals(new SpriteCell(0, 2), EntityViewFactory.spriteCellFor(new FloorSwitch("switch", pos(0, 0)), state));
    }

    @Test
    void sourceRectUsesStrict362PixelCells() {
        GameState state = minimalState();
        // Treasure 位于 (3,0)：x=3*362、y=0，宽高各 362
        assertEquals(new Rectangle2D(1086.0, 0.0, 362.0, 362.0),
                EntityViewFactory.sourceRect(new Treasure("treasure", pos(0, 0)), state));
        // FloorSwitch 位于 (0,2)
        assertEquals(new Rectangle2D(0.0, 724.0, 362.0, 362.0),
                EntityViewFactory.sourceRect(new FloorSwitch("switch", pos(0, 0)), state));
    }

    @Test
    void doorCellDependsOnOpenState() {
        GameState state = minimalState();
        Door closed = new Door("door-1", pos(0, 0), 1, false);
        Door open = new Door("door-2", pos(0, 0), 1, true);
        assertEquals(new SpriteCell(1, 1), EntityViewFactory.spriteCellFor(closed, state), "关着的门用关闭格");
        assertEquals(new SpriteCell(2, 1), EntityViewFactory.spriteCellFor(open, state), "开着的门用打开格");
        // 同一扇门状态翻转后应换格
        closed.setOpen(true);
        assertEquals(new SpriteCell(2, 1), EntityViewFactory.spriteCellFor(closed, state));
        closed.setOpen(false);
        assertEquals(new SpriteCell(1, 1), EntityViewFactory.spriteCellFor(closed, state));
    }

    @Test
    void switchCellDependsOnCoveredState() {
        FloorSwitch floorSwitch = new FloorSwitch("switch", pos(1, 1));
        assertEquals(new SpriteCell(0, 2), EntityViewFactory.spriteCellFor(floorSwitch, null),
                "state 为 null 时按未触发处理");
        assertEquals(new SpriteCell(0, 2), EntityViewFactory.spriteCellFor(floorSwitch, stateWith(floorSwitch)),
                "未被巨石压住用未触发（红）格");
        GameState covered = stateWith(floorSwitch, new Boulder("boulder", pos(1, 1)));
        assertEquals(new SpriteCell(1, 2), EntityViewFactory.spriteCellFor(floorSwitch, covered),
                "被巨石压住用激活（绿）格");
    }

    @Test
    void defaultProviderLoadsRealSpritesheet() {
        EntityViewFactory.setSpriteProvider(EntityViewFactory.DEFAULT_SPRITE_PROVIDER);
        GameState state = minimalState();
        ImageView wall = assertInstanceOf(ImageView.class,
                EntityViewFactory.createEntityView(new Wall("wall", pos(0, 0)), state));
        Image sprite = wall.getImage();
        assertEquals(1448.0, sprite.getWidth(), 0.5, "共享 Image 应为图集原始尺寸");
        assertEquals(1086.0, sprite.getHeight(), 0.5);
        assertFalse(sprite.isError(), "图集应能正常解码");
    }

    @Test
    void wallFillsCellAndOthersStayWithin44() {
        EntityViewFactory.setSpriteProvider(SpritesheetViewTest::loadSheet);
        GameState state = minimalState();
        ImageView wall = assertInstanceOf(ImageView.class,
                EntityViewFactory.createEntityView(new Wall("wall", pos(0, 0)), state));
        assertEquals(EntityViewFactory.CELL_SIZE, wall.getFitWidth(), "墙应铺满 48 避免留缝");
        assertEquals(EntityViewFactory.CELL_SIZE, wall.getFitHeight());

        ImageView player = assertInstanceOf(ImageView.class,
                EntityViewFactory.createEntityView(new Player("player", pos(0, 0)), state));
        assertEquals(EntityViewFactory.SPRITE_MAX_FIT, player.getFitWidth(), "精灵宽不超过 44");
        assertEquals(EntityViewFactory.SPRITE_MAX_FIT, player.getFitHeight(), "精灵高不超过 44");
        assertTrue(player.isPreserveRatio(), "保留纵横比");
        assertFalse(player.isSmooth(), "像素风应关闭平滑插值");
    }

    @Test
    void backgroundUsesFloorCellAndFillsCell() {
        EntityViewFactory.setSpriteProvider(SpritesheetViewTest::loadSheet);
        Node background = EntityViewFactory.createCellBackground(2, 3);
        ImageView floor = assertInstanceOf(ImageView.class, background);
        // floor 格位于 (2,2)：x=2*362、y=2*362，铺满 48×48
        assertEquals(new Rectangle2D(724.0, 724.0, 362.0, 362.0), floor.getViewport());
        assertEquals(EntityViewFactory.CELL_SIZE, floor.getFitWidth());
        assertEquals(EntityViewFactory.CELL_SIZE, floor.getFitHeight());
    }

    @Test
    void eachCallReturnsFreshNodeSharingCachedImage() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<Image> countingProvider = () -> {
            calls.incrementAndGet();
            return loadSheet();
        };
        EntityViewFactory.setSpriteProvider(countingProvider);
        GameState state = minimalState();
        Wall wall = new Wall("wall", pos(0, 0));
        Node first = EntityViewFactory.createEntityView(wall, state);
        Node second = EntityViewFactory.createEntityView(wall, state);
        assertNotSame(first, second, "每次调用必须返回全新节点，避免重复加入场景图");
        assertSame(((ImageView) first).getImage(), ((ImageView) second).getImage(),
                "两个视图必须共享同一个缓存的 Image");
        assertEquals(1, calls.get(), "图集 Image 应只解析一次并缓存共享，不按格重复加载");
    }

    @Test
    void fallsBackToShapeWhenProviderReturnsNull() {
        EntityViewFactory.setSpriteProvider(() -> null);
        GameState state = minimalState();
        assertInstanceOf(Rectangle.class, EntityViewFactory.createEntityView(new Wall("wall", pos(0, 0)), state));
        assertInstanceOf(Rectangle.class, EntityViewFactory.createCellBackground(1, 1),
                "背景也应回退到棋盘格 Shape");
    }

    @Test
    void fallsBackToShapeWhenProviderThrows() {
        EntityViewFactory.setSpriteProvider(() -> {
            throw new IllegalStateException("模拟图集加载异常");
        });
        GameState state = minimalState();
        assertInstanceOf(Rectangle.class, EntityViewFactory.createEntityView(new Wall("wall", pos(0, 0)), state),
                "提供者抛异常时应回退，不导致启动失败");
    }

    @Test
    void fallsBackToShapeWhenProviderReturnsErroredImage() {
        // 非法 PNG 字节 → Image.isError() == true
        byte[] garbage = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x00, 0x00};
        EntityViewFactory.setSpriteProvider(() -> new Image(new ByteArrayInputStream(garbage)));
        GameState state = minimalState();
        assertInstanceOf(Rectangle.class, EntityViewFactory.createEntityView(new Wall("wall", pos(0, 0)), state),
                "Image 解码失败时应回退到 Shape");
    }

    @Test
    void fallsBackToShapeWhenImageSizeMismatch() throws Exception {
        // 用 JDK ImageIO 生成一张 10x10 PNG 注入：尺寸不符 1448×1086 时应回退
        BufferedImage tiny = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(tiny, "png", out);
        EntityViewFactory.setSpriteProvider(() -> new Image(new ByteArrayInputStream(out.toByteArray())));
        GameState state = minimalState();
        assertInstanceOf(Rectangle.class, EntityViewFactory.createEntityView(new Wall("wall", pos(0, 0)), state),
                "尺寸不符时应回退到 Shape");
    }

    /** 从 classpath 加载真实图集（供注入）。 */
    private static Image loadSheet() {
        // 注意：Class.getResource 才剥前导斜杠，ClassLoader.getResource 不剥
        return new Image(SpritesheetViewTest.class.getResourceAsStream(
                "/" + EntityViewFactory.SPRITESHEET_PATH));
    }

    private static Position pos(int x, int y) {
        return new Position(x, y);
    }

    /** 最小可绘制局面（2x2、无实体、EXIT 目标）。 */
    private static GameState minimalState() {
        Player player = new Player("player", new Position(0, 0));
        return new GameState(2, 2, player, List.of(), new ExitGoal(), "测试关卡");
    }

    /** 指定实体列表的局面（玩家固定在 (0,0)）。 */
    private static GameState stateWith(Entity... entities) {
        Player player = new Player("player", new Position(0, 0));
        return new GameState(4, 4, player, List.of(entities), new ExitGoal(), "测试关卡");
    }
}
