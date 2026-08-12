package com.example.dungeonescape.ui;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图集资源校验：classpath 可定位、PNG 签名正确、尺寸必须为 1448×1086（4 列 × 3 行、
 * 每格 362×362）、32 位 RGBA（含透明通道），且透明背景与不透明内容同时存在。
 *
 * <p>纯字节/ImageIO 校验，不启动 JavaFX Toolkit、不构造 JavaFX 对象。
 */
class SpritesheetResourceTest {

    private static InputStream resource() {
        return SpritesheetResourceTest.class.getClassLoader()
                .getResourceAsStream(EntityViewFactory.SPRITESHEET_PATH);
    }

    @Test
    void spritesheetResolvesOnClasspath() throws Exception {
        try (InputStream in = resource()) {
            assertNotNull(in, "图集必须存在于 classpath：" + EntityViewFactory.SPRITESHEET_PATH);
            assertTrue(in.readAllBytes().length > 0, "图集文件不应为空");
        }
    }

    @Test
    void pngSignatureIsValid() throws Exception {
        byte[] header;
        try (InputStream in = resource()) {
            header = in.readNBytes(8);
        }
        assertArrayEquals(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a},
                header, "文件必须以 PNG 魔数开头");
    }

    @Test
    void dimensionsMatch1448x1086Spec() throws Exception {
        byte[] ihdr;
        try (InputStream in = resource()) {
            ihdr = in.readNBytes(24);
        }
        assertEquals(24, ihdr.length, "应能读到 8 字节签名 + 8 字节块头 + 8 字节 IHDR 数据");
        int width = ((ihdr[16] & 0xFF) << 24) | ((ihdr[17] & 0xFF) << 16)
                | ((ihdr[18] & 0xFF) << 8) | (ihdr[19] & 0xFF);
        int height = ((ihdr[20] & 0xFF) << 24) | ((ihdr[21] & 0xFF) << 16)
                | ((ihdr[22] & 0xFF) << 8) | (ihdr[23] & 0xFF);
        assertEquals(1448, width, "图集宽度必须为 1448（4 列 × 362）");
        assertEquals(1086, height, "图集高度必须为 1086（3 行 × 362）");
    }

    @Test
    void pngIs32BitWithAlphaChannel() throws Exception {
        try (InputStream in = resource()) {
            BufferedImage image = ImageIO.read(in);
            assertNotNull(image, "ImageIO 应能解码图集");
            assertTrue(image.getColorModel().hasAlpha(), "图集必须是 32 位 RGBA（含透明通道）");
        }
    }

    @Test
    void hasTransparentBackgroundAndOpaqueContent() throws Exception {
        try (InputStream in = resource()) {
            BufferedImage image = ImageIO.read(in);
            assertNotNull(image);
            boolean hasTransparent = false;
            boolean hasOpaque = false;
            int step = 3; // 抽样足够覆盖整图，同时控制扫描开销
            for (int y = 0; y < image.getHeight() && !(hasTransparent && hasOpaque); y += step) {
                for (int x = 0; x < image.getWidth() && !(hasTransparent && hasOpaque); x += step) {
                    int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                    if (alpha == 0) {
                        hasTransparent = true;
                    } else if (alpha == 255) {
                        hasOpaque = true;
                    }
                }
            }
            assertTrue(hasTransparent, "图集背景应为透明（存在 alpha=0 像素）");
            assertTrue(hasOpaque, "图集应包含不透明内容（存在 alpha=255 像素）");
        }
    }
}
