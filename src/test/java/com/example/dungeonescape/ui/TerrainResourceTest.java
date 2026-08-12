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
 * 连贯地形图集资源校验：classpath 可定位、PNG 签名正确、尺寸必须为 1672×941
 * （与 {@link TerrainTexture#SHEET_WIDTH}/{@link TerrainTexture#SHEET_HEIGHT}
 * 的尺寸契约一致）、ImageIO 可完整解码。
 *
 * <p>纯字节/ImageIO 校验，不启动 JavaFX Toolkit、不构造 JavaFX 对象。
 */
class TerrainResourceTest {

    private static InputStream resource() {
        return TerrainResourceTest.class.getClassLoader()
                .getResourceAsStream(TerrainTexture.SHEET_PATH);
    }

    @Test
    void terrainSheetResolvesOnClasspath() throws Exception {
        try (InputStream in = resource()) {
            assertNotNull(in, "地形图集必须存在于 classpath：" + TerrainTexture.SHEET_PATH);
            assertTrue(in.readAllBytes().length > 0, "地形图集文件不应为空");
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
    void dimensionsMatch1672x941Spec() throws Exception {
        byte[] ihdr;
        try (InputStream in = resource()) {
            ihdr = in.readNBytes(24);
        }
        assertEquals(24, ihdr.length, "应能读到 8 字节签名 + 8 字节块头 + 8 字节 IHDR 数据");
        int width = ((ihdr[16] & 0xFF) << 24) | ((ihdr[17] & 0xFF) << 16)
                | ((ihdr[18] & 0xFF) << 8) | (ihdr[19] & 0xFF);
        int height = ((ihdr[20] & 0xFF) << 24) | ((ihdr[21] & 0xFF) << 16)
                | ((ihdr[22] & 0xFF) << 8) | (ihdr[23] & 0xFF);
        assertEquals(1672, width, "地形图集宽度必须为 1672");
        assertEquals(941, height, "地形图集高度必须为 941");
    }

    @Test
    void imageIoDecodesFullSheet() throws Exception {
        try (InputStream in = resource()) {
            BufferedImage image = ImageIO.read(in);
            assertNotNull(image, "ImageIO 应能解码地形图集");
            assertEquals(1672, image.getWidth());
            assertEquals(941, image.getHeight());
        }
    }
}
