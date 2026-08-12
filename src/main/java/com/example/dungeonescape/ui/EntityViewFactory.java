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
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * 实体视图工厂：把后端实体转换为 JavaFX 节点。
 *
 * <p>图集可用时优先使用单个缓存的 {@link Image} + {@link ImageView} viewport 切图
 * （4 列 × 3 行、每格 362×362 的像素风图集，见 {@link #SPRITESHEET_PATH} 与
 * {@link #spriteCellFor(Entity, GameState)} 的映射表）；图集缺失、加载出错或尺寸不符时
 * 自动回退到 {@link #shapeView(Entity, GameState)} 的纯 Shape/Label 绘制，
 * 保证应用在任何情况下都能正常启动。
 *
 * <p>每格固定 {@link #CELL_SIZE}×{@link #CELL_SIZE} 像素。每次调用都创建全新的节点实例，
 * 绝不复用同一个 Node（避免同一节点被重复加入场景图而抛异常）；
 * 图集 {@link Image} 只解析一次并缓存，全局共享，不按格重复加载。
 *
 * <p>分层约定（层号越小越靠下，见 {@link #layerOf(Class)}）：
 * {@link FloorSwitch} 位于最底层（被 {@link Boulder} 压住时机关显示在巨石之下）、
 * {@link Exit} 位于次底层（玩家站上出口时出口显示在玩家之下），
 * Treasure/Key/Door/Boulder/Wall 依次向上，{@link Player} 恒为最顶层。
 *
 * <p>回退配色（深色地牢风）：
 * <ul>
 *   <li>地板：深色棋盘格地砖；</li>
 *   <li>墙壁：灰蓝砖块；</li>
 *   <li>出口：绿色拱门；</li>
 *   <li>宝物：金色菱形；钥匙：金色圆头 + 短杆；</li>
 *   <li>门：关闭棕色 / 打开暗色门洞；</li>
 *   <li>巨石：灰色圆石；</li>
 *   <li>地板机关：未触发红色圆钮 / 被压绿色圆钮；</li>
 *   <li>玩家：蓝色圆 + 白色 P 字。</li>
 * </ul>
 */
public final class EntityViewFactory {

    /** 每格边长（像素）。 */
    public static final double CELL_SIZE = 48.0;

    /** 图集 classpath 路径（不含前导斜杠）。 */
    public static final String SPRITESHEET_PATH = "com/example/dungeonescape/images/dungeon-spritesheet.png";

    /** 图集列数。 */
    public static final int SHEET_COLUMNS = 4;

    /** 图集行数。 */
    public static final int SHEET_ROWS = 3;

    /** 图集每格边长（像素）。 */
    public static final double SHEET_CELL_PX = 362.0;

    /** 图集总宽（像素）。 */
    public static final double SHEET_WIDTH_PX = SHEET_COLUMNS * SHEET_CELL_PX;

    /** 图集总高（像素）。 */
    public static final double SHEET_HEIGHT_PX = SHEET_ROWS * SHEET_CELL_PX;

    /** 非铺满类精灵的最大显示边长（像素）：留出边距，居中后不贴格线。 */
    public static final double SPRITE_MAX_FIT = 44.0;

    /**
     * 原图集墙精灵的内容边界（x=404..671, y=56..336，位于 362×362 单元格内）：
     * 紧裁剪后铺满整格，消除独立卡片式透明边距造成的视觉缝隙。
     */
    private static final Rectangle2D WALL_TIGHT_RECT = new Rectangle2D(404, 56, 268, 281);

    /** 图集中的一个格（0 基列/行）。 */
    public record SpriteCell(int col, int row) {
    }

    /** 默认图集提供者：从 classpath 加载（解析一次后由工厂缓存，不重复加载）。 */
    public static final Supplier<Image> DEFAULT_SPRITE_PROVIDER = EntityViewFactory::loadSpritesheet;

    // ---- 图集映射（0 基列/行，对应 Codex 生成的 4x3 图集） ----
    private static final SpriteCell PLAYER_CELL = new SpriteCell(0, 0);
    private static final SpriteCell WALL_CELL = new SpriteCell(1, 0);
    private static final SpriteCell EXIT_CELL = new SpriteCell(2, 0);
    private static final SpriteCell TREASURE_CELL = new SpriteCell(3, 0);
    private static final SpriteCell KEY_CELL = new SpriteCell(0, 1);
    private static final SpriteCell DOOR_CLOSED_CELL = new SpriteCell(1, 1);
    private static final SpriteCell DOOR_OPEN_CELL = new SpriteCell(2, 1);
    private static final SpriteCell BOULDER_CELL = new SpriteCell(3, 1);
    private static final SpriteCell SWITCH_INACTIVE_CELL = new SpriteCell(0, 2);
    private static final SpriteCell SWITCH_ACTIVE_CELL = new SpriteCell(1, 2);
    private static final SpriteCell FLOOR_CELL = new SpriteCell(2, 2);

    private static final Color FLOOR_DARK = Color.web("#1d222f");
    private static final Color FLOOR_LIGHT = Color.web("#232837");
    private static final Color FLOOR_STROKE = Color.web("#141822");
    private static final Color WALL_FILL = Color.web("#3c4456");
    private static final Color WALL_STROKE = Color.web("#586175");
    private static final Color EXIT_FILL = Color.web("#2f9e6e");
    private static final Color EXIT_STROKE = Color.web("#8fe8bd");
    private static final Color EXIT_OPENING = Color.web("#143a2b");
    private static final Color TREASURE_FILL = Color.web("#ffd166");
    private static final Color TREASURE_STROKE = Color.web("#c9972e");
    private static final Color KEY_FILL = Color.web("#ffd166");
    private static final Color KEY_STROKE = Color.web("#c9972e");
    private static final Color DOOR_CLOSED_FILL = Color.web("#8b5a2b");
    private static final Color DOOR_CLOSED_STROKE = Color.web("#5c3a1c");
    private static final Color DOOR_OPEN_FILL = Color.web("#4a5568");
    private static final Color DOOR_OPEN_STROKE = Color.web("#6b778c");
    private static final Color BOULDER_FILL = Color.web("#8b93a7");
    private static final Color BOULDER_STROKE = Color.web("#5f6675");
    private static final Color SWITCH_UNCOVERED_FILL = Color.web("#e05555");
    private static final Color SWITCH_UNCOVERED_STROKE = Color.web("#8f2f2f");
    private static final Color SWITCH_COVERED_FILL = Color.web("#55c07a");
    private static final Color SWITCH_COVERED_STROKE = Color.web("#2f7a4d");
    private static final Color PLAYER_FILL = Color.web("#4a9eff");
    private static final Color PLAYER_STROKE = Color.web("#1f6fd6");

    /** 当前图集提供者；为 null 表示显式禁用图集（强制走 Shape 回退）。 */
    private static volatile Supplier<Image> spriteProvider = DEFAULT_SPRITE_PROVIDER;

    /** 已解析的图集缓存（null 表示已尝试但不可用，避免反复加载）。 */
    private static volatile Image cachedSprite;

    private EntityViewFactory() {
    }

    /**
     * 注入图集提供者（主要用于测试：注入 null 或返回 null/出错/尺寸不符的 Image 以验证回退）。
     *
     * <p>每次注入都会清空已解析的图集缓存；传入 {@link #DEFAULT_SPRITE_PROVIDER} 恢复默认行为。
     *
     * @param provider 图集 Image 提供者；null 表示禁用图集，全部走 Shape 回退
     */
    public static void setSpriteProvider(Supplier<Image> provider) {
        synchronized (EntityViewFactory.class) {
            spriteProvider = provider;
            cachedSprite = null;
        }
    }

    /** 返回当前图集提供者（可能为 null，表示已禁用）。 */
    public static Supplier<Image> getSpriteProvider() {
        return spriteProvider;
    }

    /**
     * 创建地图格背景（棋盘格深色地砖）；空格同样绘制背景，保证整张地图底色连续。
     *
     * <p>图集可用时使用图集的 floor 格（{@link #FLOOR_CELL}）铺满整格 48×48；
     * 否则回退到纯 Shape 棋盘格。
     */
    public static Node createCellBackground(int x, int y) {
        Image sprite = spriteImage();
        if (sprite != null) {
            ImageView floor = new ImageView(sprite);
            floor.setViewport(floorSourceRect());
            floor.setFitWidth(CELL_SIZE);
            floor.setFitHeight(CELL_SIZE);
            floor.setPreserveRatio(true);
            floor.setSmooth(false);
            return floor;
        }
        return floorShape(x, y);
    }

    /**
     * 创建地形感知的地图格背景：连贯地形图集可用时按四邻可行走拓扑选择
     * 地面/横路/竖路/转角/墙体素材（满格 48×48 铺设，相邻格边缘连续）；
     * 图集不可用时回退到 {@link #createCellBackground(int, int)}。
     *
     * @param x     格列
     * @param y     格行
     * @param state 当前局面（用于墙判定与四邻连通拓扑；为 null 时回退旧背景）
     * @return 背景节点
     */
    public static Node createCellBackground(int x, int y, GameState state) {
        if (state != null && TerrainTexture.isAvailable()) {
            TerrainKind kind = TerrainLogic.terrainFor(state, new Position(x, y));
            return TerrainTexture.createTile(kind);
        }
        return createCellBackground(x, y);
    }

    /**
     * 创建单个实体的视图节点。
     *
     * @param entity 后端实体
     * @param state  当前局面（用于展示门开合、机关被压等动态状态；可为 null，此时按默认状态绘制）
     * @return 全新节点实例
     * @throws IllegalArgumentException 遇到未知实体类型
     */
    public static Node createEntityView(Entity entity, GameState state) {
        Image sprite = spriteImage();
        if (sprite != null) {
            return spriteView(entity, state, sprite);
        }
        return shapeView(entity, state);
    }

    /**
     * 返回实体在图集中的格（0 基列/行）。动态状态：门按 {@link Door#isOpen()}、
     * 机关按 {@link GameState#isSwitchCovered(Position)}（被巨石压住选激活格）选择不同格。
     *
     * <p>纯映射逻辑，不依赖 JavaFX 运行时，可直接单元测试。
     */
    public static SpriteCell spriteCellFor(Entity entity, GameState state) {
        if (entity instanceof Wall) {
            return WALL_CELL;
        }
        if (entity instanceof Exit) {
            return EXIT_CELL;
        }
        if (entity instanceof Treasure) {
            return TREASURE_CELL;
        }
        if (entity instanceof Key) {
            return KEY_CELL;
        }
        if (entity instanceof Door door) {
            return door.isOpen() ? DOOR_OPEN_CELL : DOOR_CLOSED_CELL;
        }
        if (entity instanceof Boulder) {
            return BOULDER_CELL;
        }
        if (entity instanceof FloorSwitch floorSwitch) {
            boolean covered = state != null && state.isSwitchCovered(floorSwitch.getPosition());
            return covered ? SWITCH_ACTIVE_CELL : SWITCH_INACTIVE_CELL;
        }
        if (entity instanceof Player) {
            return PLAYER_CELL;
        }
        throw new IllegalArgumentException("未知实体类型：" + entity.getClass().getName());
    }

    /** 实体图源矩形：严格按 362×362 单元格从图集切图。 */
    public static Rectangle2D sourceRect(Entity entity, GameState state) {
        SpriteCell cell = spriteCellFor(entity, state);
        return cellRect(cell);
    }

    /** 背景 floor 格的图源矩形（{@link #FLOOR_CELL}，铺满整格）。 */
    public static Rectangle2D floorSourceRect() {
        return cellRect(FLOOR_CELL);
    }

    private static Rectangle2D cellRect(SpriteCell cell) {
        return new Rectangle2D(cell.col() * SHEET_CELL_PX, cell.row() * SHEET_CELL_PX,
                SHEET_CELL_PX, SHEET_CELL_PX);
    }

    /** 图集视图：ImageView + viewport 切图；墙按内容紧裁剪后铺满整格（避免留缝），其余精灵 ≤44 居中。 */
    private static ImageView spriteView(Entity entity, GameState state, Image sprite) {
        ImageView view = new ImageView(sprite);
        // 墙精灵紧贴内容裁剪（x=404..671, y=56..336），铺满 48×48，
        // 消除原图集透明边距在相邻墙之间形成的黑色间隔
        view.setViewport(entity instanceof Wall ? WALL_TIGHT_RECT : sourceRect(entity, state));
        // 像素风：关闭平滑插值，保持锐利像素
        view.setSmooth(false);
        boolean fullCell = entity instanceof Wall;
        view.setFitWidth(fullCell ? CELL_SIZE : SPRITE_MAX_FIT);
        view.setFitHeight(fullCell ? CELL_SIZE : SPRITE_MAX_FIT);
        view.setPreserveRatio(true);
        return view;
    }

    /** 回退绘制：纯 Shape/Label，不依赖任何图片资源。 */
    private static Node shapeView(Entity entity, GameState state) {
        if (entity instanceof Wall) {
            return wallView();
        }
        if (entity instanceof Exit) {
            return exitView();
        }
        if (entity instanceof Treasure) {
            return treasureView();
        }
        if (entity instanceof Key) {
            return keyView();
        }
        if (entity instanceof Door door) {
            return doorView(door);
        }
        if (entity instanceof Boulder) {
            return boulderView();
        }
        if (entity instanceof FloorSwitch floorSwitch) {
            return switchView(floorSwitch, state);
        }
        if (entity instanceof Player) {
            return playerView();
        }
        throw new IllegalArgumentException("未知实体类型：" + entity.getClass().getName());
    }

    /** 回退背景：棋盘格地砖。 */
    private static Rectangle floorShape(int x, int y) {
        Rectangle floor = new Rectangle(CELL_SIZE, CELL_SIZE);
        floor.setFill(((x + y) % 2 == 0) ? FLOOR_DARK : FLOOR_LIGHT);
        floor.setStroke(FLOOR_STROKE);
        floor.setStrokeWidth(1);
        return floor;
    }

    /** 墙壁：铺满整格的灰蓝砖块。 */
    private static Rectangle wallView() {
        Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
        rect.setFill(WALL_FILL);
        rect.setStroke(WALL_STROKE);
        rect.setStrokeWidth(2);
        return rect;
    }

    /** 出口：绿色拱门（外框 + 深色门洞），玩家站上出口时仍可看到下方的门。 */
    private static Node exitView() {
        Rectangle frame = new Rectangle(34, 40);
        frame.setFill(EXIT_FILL);
        frame.setStroke(EXIT_STROKE);
        frame.setStrokeWidth(2);
        frame.setArcWidth(34);
        frame.setArcHeight(18);
        Rectangle opening = new Rectangle(20, 28);
        opening.setFill(EXIT_OPENING);
        opening.setTranslateY(6);
        return new StackPane(frame, opening);
    }

    /** 宝物：金色菱形。 */
    private static Polygon treasureView() {
        Polygon diamond = new Polygon(24, 6, 42, 24, 24, 42, 6, 24);
        diamond.setFill(TREASURE_FILL);
        diamond.setStroke(TREASURE_STROKE);
        diamond.setStrokeWidth(2);
        return diamond;
    }

    /** 钥匙：金色圆头 + 短杆（组合形状）。 */
    private static Node keyView() {
        Circle head = new Circle(7);
        head.setFill(KEY_FILL);
        head.setStroke(KEY_STROKE);
        head.setStrokeWidth(2);
        Rectangle shaft = new Rectangle(20, 5);
        shaft.setFill(KEY_FILL);
        shaft.setStroke(KEY_STROKE);
        shaft.setStrokeWidth(1.5);
        shaft.setTranslateY(11);
        return new StackPane(head, shaft);
    }

    /** 门：关闭时为棕色，打开时为可通行的暗色门洞。 */
    private static Rectangle doorView(Door door) {
        Rectangle rect = new Rectangle(32, 40);
        rect.setFill(door.isOpen() ? DOOR_OPEN_FILL : DOOR_CLOSED_FILL);
        rect.setStroke(door.isOpen() ? DOOR_OPEN_STROKE : DOOR_CLOSED_STROKE);
        rect.setStrokeWidth(2);
        rect.setArcWidth(10);
        rect.setArcHeight(10);
        return rect;
    }

    /** 巨石：灰色圆石。 */
    private static Circle boulderView() {
        Circle circle = new Circle(20);
        circle.setFill(BOULDER_FILL);
        circle.setStroke(BOULDER_STROKE);
        circle.setStrokeWidth(2);
        return circle;
    }

    /** 地板机关：未触发为红色圆钮，被巨石压住（触发）为绿色圆钮。 */
    private static Circle switchView(FloorSwitch floorSwitch, GameState state) {
        boolean covered = state != null && state.isSwitchCovered(floorSwitch.getPosition());
        Circle circle = new Circle(9);
        circle.setFill(covered ? SWITCH_COVERED_FILL : SWITCH_UNCOVERED_FILL);
        circle.setStroke(covered ? SWITCH_COVERED_STROKE : SWITCH_UNCOVERED_STROKE);
        circle.setStrokeWidth(2);
        return circle;
    }

    /** 玩家：蓝色圆 + 白色 P 字。 */
    private static Node playerView() {
        Circle circle = new Circle(15);
        circle.setFill(PLAYER_FILL);
        circle.setStroke(PLAYER_STROKE);
        circle.setStrokeWidth(2);
        Label label = new Label("P");
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px;");
        return new StackPane(circle, label);
    }

    /**
     * 返回实体类型的绘制层号（越小越靠下）。
     *
     * <p>{@link FloorSwitch} 最底（巨石压住机关时巨石在上）、{@link Exit} 次底
     * （玩家踩上出口时玩家在上）、{@link Player} 恒为最顶；未知类型按默认层 1 处理。
     */
    public static int layerOf(Class<? extends Entity> type) {
        if (type == FloorSwitch.class) {
            return 0;
        }
        if (type == Exit.class) {
            return 1;
        }
        if (type == Treasure.class) {
            return 2;
        }
        if (type == Key.class) {
            return 3;
        }
        if (type == Door.class) {
            return 4;
        }
        if (type == Boulder.class) {
            return 5;
        }
        if (type == Wall.class) {
            return 6;
        }
        if (type == Player.class) {
            return 7;
        }
        return 1;
    }

    /** {@link #layerOf(Class)} 的实体实例便捷重载。 */
    public static int layerOf(Entity entity) {
        return layerOf(entity.getClass());
    }

    /**
     * 返回可用的图集 Image（全局缓存，只解析一次）。
     *
     * <p>提供者为 null、返回 null、返回的 Image 处于 error 状态或尺寸与
     * {@link #SHEET_WIDTH_PX}×{@link #SHEET_HEIGHT_PX} 不符时一律视为不可用并返回 null，
     * 由调用方回退到 Shape 绘制；任何运行时异常都被捕获，绝不导致应用启动失败。
     */
    private static Image spriteImage() {
        Supplier<Image> provider = spriteProvider;
        if (provider == null) {
            return null;
        }
        Image image = cachedSprite;
        if (image != null) {
            return image;
        }
        synchronized (EntityViewFactory.class) {
            image = cachedSprite;
            if (image == null) {
                try {
                    image = provider.get();
                } catch (RuntimeException ex) {
                    image = null;
                }
                if (image != null && (image.isError() || !matchesSheetSize(image))) {
                    image = null;
                }
                cachedSprite = image;
            }
            return image;
        }
    }

    /** 尺寸校验：图集必须是 1448×1086。 */
    private static boolean matchesSheetSize(Image image) {
        return Math.abs(image.getWidth() - SHEET_WIDTH_PX) < 0.5
                && Math.abs(image.getHeight() - SHEET_HEIGHT_PX) < 0.5;
    }

    /** 默认图集加载：从 classpath 读取（不写文件、不引用外部绝对路径）；失败返回 null。 */
    private static Image loadSpritesheet() {
        try (InputStream in = EntityViewFactory.class.getResourceAsStream('/' + SPRITESHEET_PATH)) {
            if (in == null) {
                return null;
            }
            Image image = new Image(in);
            return image.isError() ? null : image;
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }
}
