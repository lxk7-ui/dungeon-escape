package com.example.dungeonescape.ui;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * 通关插画素材：victory-next-level-art.png（1536×1024 概念图，含大标题、中部
 * 绿色传送门/魔法光效与底部按钮区）。
 *
 * <p>不把整张概念图当背景显示：{@link #createPortalGraphic()} 生成的 graphic
 * 只取图中部的绿色传送门/魔法光效，由 {@link #GRAPHIC_VIEWPORT} 这个严格在图内的
 * {@link Rectangle2D} 裁取（x=650, y=470, 宽=240, 高=190），避开顶部标题与底部按钮。
 *
 * <p>素材 Image 只解析一次并全局缓存（单例）；提供者可注入（测试用）。素材缺失、
 * 解码失败或尺寸不符时视为不可用，{@link #createPortalGraphic()} 改返回一个
 * 克制的占位图形（暗绿色圆盘 ImageView），任何情况下都不抛错。所有路径均为
 * classpath 相对路径，不引用任何外部绝对路径。
 */
public final class VictoryArt {

    /** 素材 classpath 路径（不含前导斜杠）。 */
    public static final String ART_PATH = "com/example/dungeonescape/images/victory-next-level-art.png";

    /** 素材锁定内容的 SHA-256（十六进制大写）；资源必须保持原样，不得改动。 */
    public static final String ART_SHA256 = "97E4386C7C61953001E7FA1C3FC01231B5C3D1044CFABA9A49DE71601B74A1D5";

    /** 素材总宽/总高（像素）。 */
    public static final double ART_WIDTH = 1536.0;
    public static final double ART_HEIGHT = 1024.0;

    /**
     * 传送门/光效的素材裁取区（x=650, y=470, 宽=240, 高=190；严格在 1536×1024 图内，
     * 取中央绿色传送门与魔法光效，避开顶部大标题与底部三个按钮）。
     */
    public static final Rectangle2D GRAPHIC_VIEWPORT = new Rectangle2D(650, 470, 240, 190);

    /** graphic 的展示宽度（像素）；preserveRatio 下高度按裁取区原比例自动计算。 */
    public static final double GRAPHIC_FIT_WIDTH = 180.0;

    /** 默认素材提供者：从 classpath 加载（解析一次后由本类缓存，不重复加载）。 */
    public static final Supplier<Image> DEFAULT_ART_PROVIDER = VictoryArt::loadArt;

    /** 当前素材提供者；为 null 表示显式禁用素材。 */
    private static volatile Supplier<Image> artProvider = DEFAULT_ART_PROVIDER;

    /** 已解析的素材缓存（null 表示已尝试但不可用，避免反复加载）。 */
    private static volatile Image cachedArt;

    /** 回退占位图缓存（生成一次后复用，与素材缓存相互独立）。 */
    private static volatile Image cachedFallback;

    private VictoryArt() {
    }

    /**
     * 注入素材提供者（主要用于测试：注入 null 或返回 null/出错/尺寸不符的 Image
     * 以验证回退占位）。每次注入都会清空素材缓存；传入 {@link #DEFAULT_ART_PROVIDER}
     * 恢复默认行为。回退占位图缓存不受注入影响。
     *
     * @param provider 素材 Image 提供者；null 表示禁用素材
     */
    public static void setArtProvider(Supplier<Image> provider) {
        synchronized (VictoryArt.class) {
            artProvider = provider;
            cachedArt = null;
        }
    }

    /** 返回当前素材提供者（可能为 null，表示已禁用）。 */
    public static Supplier<Image> getArtProvider() {
        return artProvider;
    }

    /** 素材是否可用：提供者可用、Image 解码成功且尺寸为 1536×1024。 */
    public static boolean isAvailable() {
        return artImage() != null;
    }

    /**
     * 创建通关插画的传送门 graphic：裁剪中央绿色传送门/魔法光效的 ImageView。
     *
     * <p>每次调用返回全新 ImageView（可重复加入场景），共享同一个缓存的 Image；
     * fitWidth 为 {@link #GRAPHIC_FIT_WIDTH}、preserveRatio=true 保持裁取区原比例、
     * smooth=false 关闭平滑插值。素材不可用（缺失/解码失败/尺寸不符）时返回
     * 克制的回退占位 ImageView；任何情况下都不抛错、不返回 null。
     *
     * @return 配置好的 ImageView（素材可用时为传送门裁剪图，否则为回退占位图）
     */
    public static ImageView createPortalGraphic() {
        Image art = artImage();
        ImageView view = new ImageView(art != null ? art : fallbackImage());
        if (art != null) {
            view.setViewport(GRAPHIC_VIEWPORT);
        }
        view.setFitWidth(GRAPHIC_FIT_WIDTH);
        view.setPreserveRatio(true);
        view.setSmooth(false);
        return view;
    }

    /** 返回缓存的素材 Image；不可用时返回 null。 */
    static Image artImage() {
        Supplier<Image> provider = artProvider;
        if (provider == null) {
            return null;
        }
        Image image = cachedArt;
        if (image != null) {
            return image;
        }
        synchronized (VictoryArt.class) {
            image = cachedArt;
            if (image == null) {
                try {
                    image = provider.get();
                } catch (RuntimeException ex) {
                    image = null;
                }
                if (image != null && (image.isError() || !matchesArtSize(image))) {
                    image = null;
                }
                cachedArt = image;
            }
            return image;
        }
    }

    /** 尺寸校验：素材必须是 1536×1024。 */
    private static boolean matchesArtSize(Image image) {
        return Math.abs(image.getWidth() - ART_WIDTH) < 0.5
                && Math.abs(image.getHeight() - ART_HEIGHT) < 0.5;
    }

    /** 默认素材加载：从 classpath 读取（不写文件、不引用外部绝对路径）；失败返回 null。 */
    private static Image loadArt() {
        try (InputStream in = VictoryArt.class.getResourceAsStream('/' + ART_PATH)) {
            if (in == null) {
                return null;
            }
            Image image = new Image(in);
            return image.isError() ? null : image;
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    /** 回退占位图：克制的暗绿色圆盘（暗示传送门），生成一次后缓存复用；失败返回 null。 */
    private static Image fallbackImage() {
        Image image = cachedFallback;
        if (image != null) {
            return image;
        }
        synchronized (VictoryArt.class) {
            image = cachedFallback;
            if (image == null) {
                try {
                    image = createFallbackImage();
                } catch (IOException | RuntimeException ex) {
                    image = null;
                }
                cachedFallback = image;
            }
            return image;
        }
    }

    /** 生成回退占位图：与裁取区同比例（240×190）的透明底 + 暗绿色圆盘与浅色细环。 */
    private static Image createFallbackImage() throws IOException {
        BufferedImage canvas = new BufferedImage(
                (int) GRAPHIC_VIEWPORT.getWidth(), (int) GRAPHIC_VIEWPORT.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = canvas.getWidth() / 2;
            int cy = canvas.getHeight() / 2;
            int radius = (int) (Math.min(canvas.getWidth(), canvas.getHeight()) * 0.36);
            g.setColor(new Color(72, 132, 88, 204));
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g.setColor(new Color(142, 202, 152, 230));
            g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(canvas, "png", out);
        Image image = new Image(new ByteArrayInputStream(out.toByteArray()));
        return image.isError() ? null : image;
    }
}
