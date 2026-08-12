package com.example.dungeonescape.ui;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 连贯地形图集：加载黑底预览资产 dungeon-terrain-coherent.png（1672×941），
 * 使用紧贴素材内部的 {@link Rectangle2D} viewport 裁取地形素材，
 * 绘制时统一铺满 48×48 格子、关闭平滑插值保持像素风格。
 *
 * <p>该图集只用于地形层（地面/直路/转角/墙体），实体仍由透明实体图集
 * （dungeon-spritesheet.png）绘制，黑底绝不会进入格子。
 *
 * <p>图集 Image 只解析一次并全局缓存；提供者可注入（测试用），
 * 加载失败、尺寸不符或提供者异常时一律视为不可用（{@link #isAvailable()}
 * 返回 false），调用方回退到旧背景，绝不导致应用启动失败。
 */
public final class TerrainTexture {

    /** 图集 classpath 路径（不含前导斜杠）。 */
    public static final String SHEET_PATH = "com/example/dungeonescape/images/dungeon-terrain-coherent.png";

    /** 图集总宽/总高（像素）。 */
    public static final double SHEET_WIDTH = 1672.0;
    public static final double SHEET_HEIGHT = 941.0;

    // ---- 紧贴素材内部的 viewport（x, y, 宽, 高；不包含外侧黑色画布） ----
    // 四个通道/地面素材均取自顶部一排（y=82..310），间距均匀、紧贴主体、排除黑画布；
    // 墙体为顶部一排最左侧的顶部连续砖墙带（x=340..576, y=82..171）。
    /** 普通地面：方形石地板（顶部一排 x=610..828, y=82..308）。 */
    public static final Rectangle2D GROUND_VIEWPORT = new Rectangle2D(610, 82, 219, 227);
    /** 横向直路：完整横向通道（顶部一排 x=861..1094, y=82..310）。 */
    public static final Rectangle2D H_ROAD_VIEWPORT = new Rectangle2D(861, 82, 234, 229);
    /** 纵向直路：完整纵向通道（顶部一排 x=1126..1338, y=82..310）。 */
    public static final Rectangle2D V_ROAD_VIEWPORT = new Rectangle2D(1126, 82, 213, 229);
    /** 标准转角：横道与纵道交汇处（顶部一排 x=1373..1599, y=82..310）；按需旋转覆盖四向。 */
    public static final Rectangle2D CORNER_VIEWPORT = new Rectangle2D(1373, 82, 227, 229);
    /** 墙体：顶部连续砖墙带（x=340..576, y=82..171），仅取砖墙顶部一带。 */
    public static final Rectangle2D WALL_VIEWPORT = new Rectangle2D(340, 82, 237, 90);

    /**
     * 转角旋转角（顺时针，度）：基准裁剪区为“道路自西进入、向南折转”（连接南+西），
     * 顺时针旋转 90/180/270 度后分别覆盖北+西、北+东、南+东三个方向。
     */
    private static final Map<TerrainKind, Double> CORNER_ROTATION = Map.of(
            TerrainKind.CORNER_SW, 0.0,
            TerrainKind.CORNER_NW, 90.0,
            TerrainKind.CORNER_NE, 180.0,
            TerrainKind.CORNER_SE, 270.0);

    /** 默认图集提供者：从 classpath 加载（解析一次后由本类缓存，不重复加载）。 */
    public static final Supplier<Image> DEFAULT_SHEET_PROVIDER = TerrainTexture::loadSheet;

    /** 当前图集提供者；为 null 表示显式禁用图集。 */
    private static volatile Supplier<Image> sheetProvider = DEFAULT_SHEET_PROVIDER;

    /** 已解析的图集缓存（null 表示已尝试但不可用，避免反复加载）。 */
    private static volatile Image cachedSheet;

    private TerrainTexture() {
    }

    /**
     * 注入图集提供者（主要用于测试：注入 null 或返回 null/出错/尺寸不符的 Image
     * 以验证回退）。每次注入都会清空缓存；传入 {@link #DEFAULT_SHEET_PROVIDER}
     * 恢复默认行为。
     *
     * @param provider 图集 Image 提供者；null 表示禁用图集
     */
    public static void setSheetProvider(Supplier<Image> provider) {
        synchronized (TerrainTexture.class) {
            sheetProvider = provider;
            cachedSheet = null;
        }
    }

    /** 返回当前图集提供者（可能为 null，表示已禁用）。 */
    public static Supplier<Image> getSheetProvider() {
        return sheetProvider;
    }

    /**
     * 图集是否可用：提供者可用、Image 解码成功且尺寸为 1672×941。
     *
     * <p>首次调用会触发一次图集加载；不可用时调用方应回退到旧背景绘制。
     */
    public static boolean isAvailable() {
        return sheetImage() != null;
    }

    /**
     * 返回指定地形素材的图源矩形；转角统一返回基准裁剪区（旋转由
     * {@link #rotationFor(TerrainKind)} 表达），其余素材为各自紧贴裁剪区。
     */
    public static Rectangle2D viewportFor(TerrainKind kind) {
        return switch (kind) {
            case GROUND -> GROUND_VIEWPORT;
            case H_ROAD -> H_ROAD_VIEWPORT;
            case V_ROAD -> V_ROAD_VIEWPORT;
            case WALL -> WALL_VIEWPORT;
            case CORNER_NW, CORNER_NE, CORNER_SW, CORNER_SE -> CORNER_VIEWPORT;
        };
    }

    /** 返回转角素材的顺时针旋转角（度）；非转角返回 0。 */
    public static double rotationFor(TerrainKind kind) {
        Double rotation = CORNER_ROTATION.get(kind);
        return rotation == null ? 0.0 : rotation;
    }

    /**
     * 创建铺满 {@link EntityViewFactory#CELL_SIZE}×{@link EntityViewFactory#CELL_SIZE}
     * 格子的地形视图（ImageView + viewport + 转角旋转，关闭平滑）。
     *
     * @param kind 地形素材种类
     * @return 全新 ImageView 实例
     * @throws IllegalStateException 图集不可用时抛出（调用方应先检查
     *                               {@link #isAvailable()}
     */
    public static ImageView createTile(TerrainKind kind) {
        Image sheet = sheetImage();
        if (sheet == null) {
            throw new IllegalStateException("地形图集不可用：" + SHEET_PATH);
        }
        ImageView view = new ImageView(sheet);
        view.setViewport(viewportFor(kind));
        // 满格 48×48：任何长宽比的素材都拉伸铺满，格与格之间不留黑色缝隙
        view.setFitWidth(EntityViewFactory.CELL_SIZE);
        view.setFitHeight(EntityViewFactory.CELL_SIZE);
        view.setPreserveRatio(false);
        view.setSmooth(false);
        double rotation = rotationFor(kind);
        if (rotation != 0.0) {
            view.setRotate(rotation);
        }
        return view;
    }

    /** 返回缓存的图集 Image；不可用时返回 null。 */
    static Image sheetImage() {
        Supplier<Image> provider = sheetProvider;
        if (provider == null) {
            return null;
        }
        Image image = cachedSheet;
        if (image != null) {
            return image;
        }
        synchronized (TerrainTexture.class) {
            image = cachedSheet;
            if (image == null) {
                try {
                    image = provider.get();
                } catch (RuntimeException ex) {
                    image = null;
                }
                if (image != null && (image.isError() || !matchesSheetSize(image))) {
                    image = null;
                }
                cachedSheet = image;
            }
            return image;
        }
    }

    /** 尺寸校验：图集必须是 1672×941。 */
    private static boolean matchesSheetSize(Image image) {
        return Math.abs(image.getWidth() - SHEET_WIDTH) < 0.5
                && Math.abs(image.getHeight() - SHEET_HEIGHT) < 0.5;
    }

    /** 默认图集加载：从 classpath 读取（不写文件、不引用外部绝对路径）；失败返回 null。 */
    private static Image loadSheet() {
        try (InputStream in = TerrainTexture.class.getResourceAsStream('/' + SHEET_PATH)) {
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
