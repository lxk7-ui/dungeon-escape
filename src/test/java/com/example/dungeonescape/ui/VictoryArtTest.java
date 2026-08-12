package com.example.dungeonescape.ui;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VictoryArt（通关插画）测试：资源存在、PNG 1536×1024、SHA-256 锁定、传送门
 * viewport 精确且在图内、ImageView 共享缓存 Image 但每次调用实例独立、
 * fitWidth/等比/smooth 配置，以及素材缺失/解码失败/尺寸不符时的回退占位。
 *
 * <p>素材不可用路径通过注入提供者验证（{@link VictoryArt#setArtProvider}），
 * 每个用例结束后恢复默认提供者；JavaFX 对象构造依赖 Toolkit（与
 * {@code SpritesheetViewTest} 相同的既有模式）。
 */
class VictoryArtTest {

    @AfterEach
    void restoreDefaultProvider() {
        VictoryArt.setArtProvider(VictoryArt.DEFAULT_ART_PROVIDER);
    }

    @Test
    void artResourceExistsOnClasspath() {
        try (InputStream in = VictoryArt.class.getResourceAsStream('/' + VictoryArt.ART_PATH)) {
            assertNotNull(in, "通关插画资源必须存在于 classpath：" + VictoryArt.ART_PATH);
        } catch (IOException ex) {
            throw new AssertionError("资源读取失败", ex);
        }
    }

    @Test
    void artIsPngWith1536x1024Dimensions() throws IOException {
        byte[] bytes = readArtBytes();
        // PNG 魔数（8 字节）
        byte[] magic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertArrayEquals(magic, Arrays.copyOfRange(bytes, 0, 8), "文件头必须是 PNG 魔数");
        // IHDR 块：偏移 12..15 为块类型，16..19 宽、20..23 高（大端）
        assertEquals("IHDR", new String(bytes, 12, 4, StandardCharsets.US_ASCII), "首个块必须是 IHDR");
        assertEquals(1536, readBeInt(bytes, 16), "PNG 宽度必须为 1536");
        assertEquals(1024, readBeInt(bytes, 20), "PNG 高度必须为 1024");
    }

    @Test
    void artSha256MatchesPinnedDigest() throws IOException, NoSuchAlgorithmException {
        byte[] bytes = readArtBytes();
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                .toUpperCase();
        assertEquals(VictoryArt.ART_SHA256, digest,
                "通关插画内容必须保持锁定版本（SHA-256 不符说明资源被改动）");
    }

    @Test
    void viewportIsExactAndLiesInsideArtBounds() {
        Rectangle2D viewport = VictoryArt.GRAPHIC_VIEWPORT;
        assertEquals(new Rectangle2D(650, 470, 240, 190), viewport,
                "传送门裁取区必须精确为 (650,470,240,190)");
        assertTrue(viewport.getMinX() >= 0 && viewport.getMinY() >= 0,
                "viewport 原点必须在图内");
        assertTrue(viewport.getMaxX() <= VictoryArt.ART_WIDTH
                        && viewport.getMaxY() <= VictoryArt.ART_HEIGHT,
                "viewport 必须完整落在 1536×1024 图内，实际 " + viewport);
    }

    @Test
    void createPortalGraphicReturnsFreshViewsSharingCachedImage() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<Image> countingProvider = () -> {
            calls.incrementAndGet();
            return loadArt();
        };
        VictoryArt.setArtProvider(countingProvider);
        ImageView first = VictoryArt.createPortalGraphic();
        ImageView second = VictoryArt.createPortalGraphic();
        assertNotSame(first, second, "每次调用必须返回全新 ImageView，避免重复加入场景图");
        assertSame(first.getImage(), second.getImage(), "两个视图必须共享同一个缓存的 Image");
        assertEquals(1, calls.get(), "素材 Image 应只解析一次并缓存共享");
        assertSame(VictoryArt.artImage(), first.getImage(), "createPortalGraphic 应使用缓存的素材");
    }

    @Test
    void createPortalGraphicUsesExactViewportAndFitConfiguration() {
        VictoryArt.setArtProvider(VictoryArt.DEFAULT_ART_PROVIDER);
        ImageView view = VictoryArt.createPortalGraphic();
        Image art = view.getImage();
        assertNotNull(art, "默认提供者应加载出素材");
        assertFalse(art.isError(), "素材应能正常解码");
        assertEquals(VictoryArt.ART_WIDTH, art.getWidth(), 0.5, "素材应为原始 1536 宽");
        assertEquals(VictoryArt.ART_HEIGHT, art.getHeight(), 0.5, "素材应为原始 1024 高");
        assertSame(VictoryArt.GRAPHIC_VIEWPORT, view.getViewport(), "必须精确使用 GRAPHIC_VIEWPORT 常量");
        assertEquals(VictoryArt.GRAPHIC_FIT_WIDTH, view.getFitWidth(), "fitWidth 必须为 180");
        assertTrue(view.isPreserveRatio(), "必须保留纵横比");
        assertFalse(view.isSmooth(), "像素风应关闭平滑插值");
    }

    @Test
    void fallsBackToPlaceholderWhenProviderReturnsNull() {
        VictoryArt.setArtProvider(() -> null);
        ImageView first = VictoryArt.createPortalGraphic();
        ImageView second = VictoryArt.createPortalGraphic();
        assertNotNull(first, "素材缺失时不得返回 null，必须给回退占位图");
        assertFalse(VictoryArt.isAvailable(), "素材不可用时 isAvailable 应为 false");
        assertNotSame(first, second, "回退路径每次调用也应返回全新 ImageView");
        assertPlaceholder(first);
        assertSame(first.getImage(), second.getImage(), "回退占位图也应缓存共享");
    }

    @Test
    void fallsBackToPlaceholderWhenProviderThrows() {
        VictoryArt.setArtProvider(() -> {
            throw new IllegalStateException("模拟素材加载异常");
        });
        ImageView view = VictoryArt.createPortalGraphic();
        assertNotNull(view, "提供者抛异常时不得抛错，必须返回回退占位图");
        assertPlaceholder(view);
    }

    @Test
    void fallsBackToPlaceholderWhenImageIsErrored() {
        // 非法 PNG 字节 → Image.isError() == true
        byte[] garbage = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x00, 0x00};
        VictoryArt.setArtProvider(() -> new Image(new ByteArrayInputStream(garbage)));
        ImageView view = VictoryArt.createPortalGraphic();
        assertNotNull(view, "Image 解码失败时不得抛错，必须返回回退占位图");
        assertPlaceholder(view);
    }

    @Test
    void fallsBackToPlaceholderWhenImageSizeMismatch() throws Exception {
        // 用 JDK ImageIO 生成一张 10×10 PNG 注入：尺寸不符 1536×1024 时应回退
        BufferedImage tiny = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(tiny, "png", out);
        VictoryArt.setArtProvider(() -> new Image(new ByteArrayInputStream(out.toByteArray())));
        ImageView view = VictoryArt.createPortalGraphic();
        assertNotNull(view, "尺寸不符时不得抛错，必须返回回退占位图");
        assertPlaceholder(view);
    }

    /** 断言视图为回退占位：非空 Image、不是 1536×1024 的素材原图、且可正常解码。 */
    private static void assertPlaceholder(ImageView view) {
        Image image = view.getImage();
        assertNotNull(image, "回退占位图必须带非空 Image");
        assertFalse(image.isError(), "回退占位图必须可正常解码");
        assertTrue(image.getWidth() < VictoryArt.ART_WIDTH / 2,
                "回退占位图尺寸必须与素材原图可区分，实际宽 " + image.getWidth());
        assertEquals(VictoryArt.GRAPHIC_VIEWPORT.getWidth(), image.getWidth(), 0.5,
                "回退占位图应与裁取区同宽（240）");
        assertEquals(VictoryArt.GRAPHIC_VIEWPORT.getHeight(), image.getHeight(), 0.5,
                "回退占位图应与裁取区同高（190）");
    }

    /** 从 classpath 读取素材字节（供纯字节级断言使用）。 */
    private static byte[] readArtBytes() throws IOException {
        try (InputStream in = VictoryArt.class.getResourceAsStream('/' + VictoryArt.ART_PATH)) {
            assertNotNull(in, "通关插画资源必须存在于 classpath：" + VictoryArt.ART_PATH);
            return in.readAllBytes();
        }
    }

    /** 大端序读取 PNG 块内的 4 字节整数。 */
    private static int readBeInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    /** 从 classpath 加载真实素材（供注入）。 */
    private static Image loadArt() {
        // 注意：Class.getResource 才剥前导斜杠，ClassLoader.getResource 不剥
        return new Image(VictoryArtTest.class.getResourceAsStream('/' + VictoryArt.ART_PATH));
    }
}
