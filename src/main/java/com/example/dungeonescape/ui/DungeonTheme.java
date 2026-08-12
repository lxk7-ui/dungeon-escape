package com.example.dungeonescape.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;

/**
 * 地牢界面视觉基础（Stage A）：主菜单 / 选关页共享的色板 token、背景层
 * 与图集图标裁切。
 *
 * <p><b>背景</b>：{@link #createBackground()} 返回一个铺满父容器的 StackPane，自底向上为
 * <ol>
 *   <li>真实地面素材平铺层：从连贯地形图集（dungeon-terrain-coherent.png）裁取
 *       {@link TerrainTexture#GROUND_VIEWPORT} 地面素材，像素级复制后按原尺寸平铺，
 *       与游戏内地图美术同源连贯；</li>
 *   <li>暗化层：全屏半透明黑，压暗纹理保证文字可读；</li>
 *   <li>上下渐变层：顶部/底部加深，标题与按钮区域对比更强；</li>
 *   <li>暗角层：中心径向渐变，四周渐暗，聚焦视线；</li>
 *   <li>装饰层：底部两角的门/出口 sprite（真实图集素材、低透明度），克制点缀。</li>
 * </ol>
 * 任何素材缺失或加载失败都回退为纯色深底，绝不导致应用启动失败。
 *
 * <p><b>图标</b>：{@link #createIcon(Rectangle2D, double)} 从实体图集
 * （dungeon-spritesheet.png，4×3 格、每格 362×362）按紧贴内容的 viewport 裁切
 * 单个 sprite（关闭平滑保持像素风），供选关卡片与装饰行复用；viewport 常量均
 * 取自图集分析得到的实际内容边界，不包含透明边距。
 */
public final class DungeonTheme {

    private DungeonTheme() {
    }

    // ---- 色板 token（对齐 art-direction 视觉稿取样） ----

    /** 场景底色：近黑蓝调。 */
    public static final String COLOR_BG_BASE = "#0a0d12";
    /** 金色（标题/焦点描边/光晕）。 */
    public static final String COLOR_GOLD = "#ffd76a";
    /** 暗金（阴影/次要描边）。 */
    public static final String COLOR_GOLD_DEEP = "#c9972e";
    /** 蓝色主按钮（开始游戏，视觉稿 #1B419A）。 */
    public static final String COLOR_PRIMARY = "#1b419a";
    /** 主按钮 hover。 */
    public static final String COLOR_PRIMARY_HOVER = "#2450b8";
    /** 主按钮 pressed。 */
    public static final String COLOR_PRIMARY_PRESSED = "#14336f";
    /** 深色次按钮（退出游戏/返回主菜单，视觉稿 #4D5151）。 */
    public static final String COLOR_SECONDARY = "#4d5151";
    /** 次按钮 hover。 */
    public static final String COLOR_SECONDARY_HOVER = "#5d6363";
    /** 次按钮 pressed。 */
    public static final String COLOR_SECONDARY_PRESSED = "#3c4040";
    /** 卡片/面板底色。 */
    public static final String COLOR_PANEL = "#11151d";
    /** 常规边框。 */
    public static final String COLOR_BORDER = "#3a4254";
    /** 主文字。 */
    public static final String COLOR_TEXT = "#e8eaf0";
    /** 弱化文字。 */
    public static final String COLOR_TEXT_MUTED = "#9aa3b2";

    // ---- 图标 viewport（362×362 格内紧贴内容，来自 dungeon-spritesheet.png） ----

    /** 出口（绿拱门）图标区。 */
    public static final Rectangle2D ICON_EXIT = new Rectangle2D(2 * 362 + 35, 0 * 362 + 40, 282, 304);
    /** 宝物（金菱形）图标区。 */
    public static final Rectangle2D ICON_TREASURE = new Rectangle2D(3 * 362 + 69, 0 * 362 + 80, 209, 246);
    /** 钥匙（金钥匙）图标区。 */
    public static final Rectangle2D ICON_KEY = new Rectangle2D(0 * 362 + 84, 1 * 362 + 48, 173, 268);
    /** 巨石图标区。 */
    public static final Rectangle2D ICON_BOULDER = new Rectangle2D(3 * 362 + 54, 1 * 362 + 69, 231, 238);
    /** 地板机关（红圆钮）图标区。 */
    public static final Rectangle2D ICON_SWITCH = new Rectangle2D(0 * 362 + 57, 2 * 362 + 59, 241, 206);
    /** 关闭的门（棕色门板）图标区。 */
    public static final Rectangle2D ICON_DOOR = new Rectangle2D(1 * 362 + 39, 1 * 362 + 31, 258, 298);

    /** 卡片图标显示边长。 */
    public static final double CARD_ICON_SIZE = 44.0;
    /** 装饰图标显示边长（门/出口，与石砖边框连接，视觉权重明显）。 */
    public static final double DECOR_ICON_SIZE = 80.0;
    /** 选关页标题上方装饰小图标边长。 */
    public static final double STRIP_ICON_SIZE = 36.0;

    /** 石砖边框条厚度（像素，贴窗口四边）。 */
    public static final double BRICK_BORDER = 64.0;
    /** 石砖平铺单元（从连贯地形图集墙素材裁切的正方块）。 */
    public static final Rectangle2D WALL_BRICK_VIEWPORT = new Rectangle2D(427, 95, 64, 64);
    /** 地面平铺单元（从连贯地形图集地面素材裁切的正方块）。 */
    public static final Rectangle2D GROUND_PATCH_VIEWPORT = new Rectangle2D(655, 131, 128, 128);

    /** 背景层样式类（供测试与 CSS 定位）。 */
    public static final String STYLE_BG_LAYER = "dungeon-bg-layer";
    /** 纹理层样式类。 */
    public static final String STYLE_BG_TEXTURE = "dungeon-bg-texture";
    /** 暗化层样式类。 */
    public static final String STYLE_BG_DIM = "dungeon-bg-dim";
    /** 上下渐变层样式类。 */
    public static final String STYLE_BG_GRADIENT = "dungeon-bg-gradient";
    /** 暗角层样式类。 */
    public static final String STYLE_BG_VIGNETTE = "dungeon-bg-vignette";
    /** 石砖边框层样式类。 */
    public static final String STYLE_BG_BORDER = "dungeon-bg-border";

    /** 已裁切的地面平铺素材（只解析一次）。 */
    private static volatile WritableImage cachedGroundTile;
    /** 已裁切的石砖边框素材（只解析一次）。 */
    private static volatile WritableImage cachedBrickTile;
    /** 已解析的实体图集（与 {@link EntityViewFactory} 共享同一资源文件）。 */
    private static volatile Image cachedSpritesheet;

    /**
     * 创建铺满父容器的地牢背景（FXML 直接实例化 DungeonBackground 使用）。
     *
     * <p>层次（自底向上）：地面纹理平铺 → 四边石砖边框 → 暗化 → 中央亮度渐变
     * → 暗角 → 底部左右门/出口装饰（与石砖边框相接，不漂浮）。
     * 素材不可用时逐层回退，最终至少保留纯色深底。
     */
    public static StackPane createBackground() {
        StackPane layer = new StackPane();
        layer.getStyleClass().add(STYLE_BG_LAYER);
        // 1) 纹理平铺层：真实地面素材（128px 石砖单元，肉眼可见）
        WritableImage ground = groundTile();
        if (ground != null) {
            Region texture = new Region();
            texture.getStyleClass().add(STYLE_BG_TEXTURE);
            texture.setBackground(new Background(new BackgroundFill(
                    new ImagePattern(ground, 0, 0, 1, 1, false),
                    CornerRadii.EMPTY, Insets.EMPTY)));
            layer.getChildren().add(texture);
        }
        // 3) 暗化层：压暗中央纹理保证文字可读，但保留可见的石板层次
        Region dim = new Region();
        dim.getStyleClass().add(STYLE_BG_DIM);
        dim.setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.38), CornerRadii.EMPTY, Insets.EMPTY)));
        layer.getChildren().add(dim);
        // 4) 上下渐变层：顶部/底部进一步加深，突出中央标题与按钮
        Region gradient = new Region();
        gradient.getStyleClass().add(STYLE_BG_GRADIENT);
        gradient.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.rgb(0, 0, 0, 0.4)),
                        new Stop(0.28, Color.TRANSPARENT),
                        new Stop(0.72, Color.TRANSPARENT),
                        new Stop(1.0, Color.rgb(0, 0, 0, 0.4))),
                CornerRadii.EMPTY, Insets.EMPTY)));
        layer.getChildren().add(gradient);
        // 5) 石砖边框层：贴窗口四边的 64px 砖墙（真实墙素材平铺，与地图美术同源）。
        //    置于暗化/渐变之上，保持砖墙本色清晰可见
        WritableImage brick = brickTile();
        if (brick != null) {
            Region brickBorder = new Region();
            brickBorder.getStyleClass().add(STYLE_BG_BORDER);
            BackgroundSize brickSize = new BackgroundSize(BRICK_BORDER, BRICK_BORDER, false, false, false, false);
            brickBorder.setBackground(new Background(new BackgroundImage[]{
                    // 顶条：横向平铺 64×64 砖块
                    new BackgroundImage(brick, BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT,
                            new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false), brickSize),
                    // 底条
                    new BackgroundImage(brick, BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT,
                            new BackgroundPosition(Side.LEFT, 0, false, Side.BOTTOM, 0, false), brickSize),
                    // 左条：纵向平铺
                    new BackgroundImage(brick, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.REPEAT,
                            new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false), brickSize),
                    // 右条
                    new BackgroundImage(brick, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.REPEAT,
                            new BackgroundPosition(Side.RIGHT, 0, false, Side.TOP, 0, false), brickSize),
            }));
            layer.getChildren().add(brickBorder);
        }
        // 6) 暗角层：四周轻微渐暗（不掩盖石砖边框）
        Region vignette = new Region();
        vignette.getStyleClass().add(STYLE_BG_VIGNETTE);
        vignette.setBackground(new Background(new BackgroundFill(
                new RadialGradient(0, 0, 0.5, 0.42, 0.8, true, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.TRANSPARENT),
                        new Stop(0.7, Color.TRANSPARENT),
                        new Stop(1.0, Color.rgb(0, 0, 0, 0.3))),
                CornerRadii.EMPTY, Insets.EMPTY)));
        layer.getChildren().add(vignette);
        // 6) 装饰层：底部两角真实素材（门/出口），底边与石砖边框相接（不漂浮）
        ImageView door = createIcon(ICON_DOOR, DECOR_ICON_SIZE);
        door.setOpacity(0.9);
        StackPane.setAlignment(door, Pos.BOTTOM_LEFT);
        StackPane.setMargin(door, new Insets(0, 0, BRICK_BORDER, 28));
        layer.getChildren().add(door);
        ImageView exit = createIcon(ICON_EXIT, DECOR_ICON_SIZE);
        exit.setOpacity(0.85);
        StackPane.setAlignment(exit, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(exit, new Insets(0, 28, BRICK_BORDER, 0));
        layer.getChildren().add(exit);
        return layer;
    }

    /**
     * 从实体图集按紧贴内容的 viewport 裁切单个 sprite 图标。
     *
     * <p>关闭平滑插值保持像素风；图集不可用时返回空 ImageView（透明），
     * 不抛出异常（调用方无需回退逻辑）。
     *
     * @param viewport 图集内紧贴内容的源矩形（如 {@link #ICON_KEY}）
     * @param size     显示边长（等比缩放）
     */
    public static ImageView createIcon(Rectangle2D viewport, double size) {
        ImageView view = new ImageView(spritesheet());
        view.setViewport(viewport);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(false);
        return view;
    }

    /** 实体图集是否可解析（图标与装饰可用的前提）。 */
    public static boolean spritesheetAvailable() {
        return spritesheet() != null;
    }

    /**
     * 用真实砖墙素材平铺填充指定区域（如主菜单标题下方的装饰分隔条）。
     *
     * @param region 待填充区域（如 FXML 中的 menu-divider）
     */
    public static void applyBrickStrip(Region region) {
        WritableImage brick = brickTile();
        if (brick == null) {
            return;
        }
        region.setBackground(new Background(new BackgroundFill(
                new ImagePattern(brick, 0, 0, 1, 1, false),
                CornerRadii.EMPTY, Insets.EMPTY)));
    }

    /** 平铺用地面素材：从连贯地形图集裁取 128×128 石砖单元。 */
    private static WritableImage groundTile() {
        WritableImage tile = cachedGroundTile;
        if (tile != null) {
            return tile;
        }
        synchronized (DungeonTheme.class) {
            tile = cachedGroundTile;
            if (tile == null) {
                Image sheet = loadImage(TerrainTexture.SHEET_PATH);
                if (sheet != null) {
                    tile = crop(sheet, GROUND_PATCH_VIEWPORT);
                }
                cachedGroundTile = tile;
            }
            return tile;
        }
    }

    /** 石砖边框素材：从连贯地形图集墙素材裁取 64×64 砖块单元。 */
    private static WritableImage brickTile() {
        WritableImage tile = cachedBrickTile;
        if (tile != null) {
            return tile;
        }
        synchronized (DungeonTheme.class) {
            tile = cachedBrickTile;
            if (tile == null) {
                Image sheet = loadImage(TerrainTexture.SHEET_PATH);
                if (sheet != null) {
                    tile = crop(sheet, WALL_BRICK_VIEWPORT);
                }
                cachedBrickTile = tile;
            }
            return tile;
        }
    }

    /** 实体图集（只解析一次；失败返回 null）。 */
    private static Image spritesheet() {
        Image image = cachedSpritesheet;
        if (image != null) {
            return image;
        }
        synchronized (DungeonTheme.class) {
            image = cachedSpritesheet;
            if (image == null) {
                image = loadImage(EntityViewFactory.SPRITESHEET_PATH);
                cachedSpritesheet = image;
            }
            return image;
        }
    }

    /** 从 classpath 加载图片；失败返回 null。 */
    private static Image loadImage(String classpathPath) {
        try (InputStream in = DungeonTheme.class.getResourceAsStream('/' + classpathPath)) {
            if (in == null) {
                return null;
            }
            Image image = new Image(in);
            return image.isError() ? null : image;
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    /** 按 viewport 像素级复制图集子区域（原始像素，无缩放）。 */
    private static WritableImage crop(Image source, Rectangle2D viewport) {
        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return null;
        }
        int width = (int) Math.ceil(viewport.getWidth());
        int height = (int) Math.ceil(viewport.getHeight());
        WritableImage out = new WritableImage(width, height);
        PixelWriter writer = out.getPixelWriter();
        WritablePixelFormat<IntBuffer> format = PixelFormat.getIntArgbInstance();
        int[] buffer = new int[width * height];
        reader.getPixels((int) viewport.getMinX(), (int) viewport.getMinY(), width, height,
                format, buffer, 0, width);
        writer.setPixels(0, 0, width, height, format, buffer, 0, width);
        return out;
    }
}
