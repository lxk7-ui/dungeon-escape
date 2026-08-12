package com.example.dungeonescape.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 错误对话框测试（Stage B + P2）：暗色石板样式、两个共享样式表显式加载、
 * 标题/摘要/可换行详情、真实位图错误徽记（无文字假图标）、
 * 唯一确认按钮为暗红危险态且为默认按钮（初始键盘焦点）、
 * 绝无 Modena 默认“确定”之外的按钮。
 */
class ErrorDialogViewTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // 幂等
        }
        runOnFx(() -> Platform.setImplicitExit(false));
    }

    @Test
    void createAlertProducesDarkStyledDialog() throws Exception {
        ErrorDialogView.Prepared prepared = createOnFx("错误", "摘要：加载失败", "详细信息行1\n详细信息行2（可换行）");
        DialogPane pane = prepared.alert().getDialogPane();
        assertTrue(pane.getStyleClass().contains(ErrorDialogView.STYLE_ERROR_DIALOG),
                "DialogPane 必须带 error-dialog 样式类");
        // 对话框独立场景：必须显式加载两个共享样式表
        assertTrue(pane.getStylesheets().stream()
                        .anyMatch(s -> s.endsWith(SceneNavigator.CSS_GAME)),
                "必须显式加载 game.css");
        assertTrue(pane.getStylesheets().stream()
                        .anyMatch(s -> s.endsWith(SceneNavigator.CSS_DUNGEON_UI)),
                "必须显式加载 dungeon-ui.css");
        // 标题 / 摘要 / 详情保留（不吞错误信息）
        assertEquals("错误", prepared.alert().getTitle());
        assertEquals("摘要：加载失败", pane.getHeaderText());
        assertEquals("详细信息行1\n详细信息行2（可换行）", pane.getContentText());
    }

    @Test
    void errorMarkAssetExistsOnClasspathWithSubstance() throws Exception {
        try (InputStream in = ErrorDialogViewTest.class.getClassLoader()
                .getResourceAsStream(ErrorDialogView.ERROR_MARK_PATH)) {
            assertNotNull(in, "错误徽记位图必须存在于 classpath：" + ErrorDialogView.ERROR_MARK_PATH);
            // 位图必须有实际内容（文件非空且超过 PNG 最小体积）
            byte[] bytes = in.readAllBytes();
            assertTrue(bytes.length > 2000, "错误徽记 PNG 必须非空：" + bytes.length + " 字节");
        }
    }

    @Test
    void errorMarkHasRealTransparentChannel() throws Exception {
        // 徽记 PNG 必须带透明通道：四角透明（无黑方块/矩形底）、中心有内容、边缘羽化
        javafx.scene.image.Image image = ErrorDialogView.loadErrorMark().getImage();
        assertNotNull(image);
        assertFalse(image.isError(), "徽记位图必须解码成功");
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        assertTrue(w >= 100 && h >= 100, "徽记尺寸必须合理（≥100px）");
        // 四角 alpha 必须为 0（外部背景透明，不得是矩形黑方块）
        assertTrue(image.getPixelReader().getColor(0, 0).getOpacity() < 0.02, "左上角必须透明");
        assertTrue(image.getPixelReader().getColor(w - 1, 0).getOpacity() < 0.02, "右上角必须透明");
        assertTrue(image.getPixelReader().getColor(0, h - 1).getOpacity() < 0.02, "左下角必须透明");
        assertTrue(image.getPixelReader().getColor(w - 1, h - 1).getOpacity() < 0.02, "右下角必须透明");
        // 中心（徽记主体）必须不透明
        assertTrue(image.getPixelReader().getColor(w / 2, h / 2).getOpacity() > 0.8,
                "徽记中心必须不透明");
        // 边缘存在半透明过渡（抗锯齿自然，非硬切）
        boolean hasPartial = false;
        for (int y = 0; y < h; y += 6) {
            for (int x = 0; x < w; x += 6) {
                double a = image.getPixelReader().getColor(x, y).getOpacity();
                if (a > 0.05 && a < 0.95) {
                    hasPartial = true;
                    break;
                }
            }
            if (hasPartial) {
                break;
            }
        }
        assertTrue(hasPartial, "徽记边缘必须存在半透明羽化（抗锯齿自然）");
        // 非空内容充分（中心区域采样不透明像素比例）
        int opaque = 0;
        int sampled = 0;
        for (int y = h / 4; y < h * 3 / 4; y += 4) {
            for (int x = w / 4; x < w * 3 / 4; x += 4) {
                sampled++;
                if (image.getPixelReader().getColor(x, y).getOpacity() > 0.5) {
                    opaque++;
                }
            }
        }
        assertTrue(opaque > sampled * 0.3, "徽记主体区域必须有充分的不透明内容（非空白占位）");
    }

    @Test
    void dialogGraphicIsRealRasterImageNotTextIcon() throws Exception {
        ErrorDialogView.Prepared prepared = createOnFx("错误", "摘要", "详情");
        javafx.scene.Node graphic = prepared.alert().getDialogPane().getGraphic();
        assertNotNull(graphic, "错误对话框必须设置真实位图徽记（不允许无图标或文字假图标）");
        assertTrue(graphic instanceof ImageView, "徽记必须是 ImageView（真实位图）而非 Label/CSS 图形");
        ImageView mark = (ImageView) graphic;
        assertNotNull(mark.getImage(), "徽记必须引用真实图集位图");
        assertFalse(mark.getImage().isError(), "徽记位图必须解码成功");
        assertEquals(ErrorDialogView.ERROR_MARK_WIDTH, mark.getFitWidth(), "徽记显示宽度必须为设定值");
        assertTrue(mark.isPreserveRatio(), "徽记必须保持纵横比");
        assertFalse(mark.isSmooth(), "徽记必须关闭平滑保持像素清晰");
        // 尺寸合理：位图本体应有实际像素
        assertTrue(mark.getImage().getWidth() >= 100 && mark.getImage().getHeight() >= 100,
                "徽记位图尺寸必须合理（≥100px）");
    }

    @Test
    void loadErrorMarkReturnsViewForRealAsset() throws Exception {
        ImageView mark = ErrorDialogView.loadErrorMark();
        assertNotNull(mark, "真实 classpath 徽记必须可加载");
        assertEquals(ErrorDialogView.ERROR_MARK_WIDTH, mark.getFitWidth());
    }

    @Test
    void contentIsWrapEnabledLabel() throws Exception {
        ErrorDialogView.Prepared prepared = createOnFx("错误", "摘要", "长详情" .repeat(30));
        // 详情经 DialogPane 的 content.label 渲染，必须可换行
        Label content = (Label) prepared.alert().getDialogPane().lookup(".content.label");
        assertNotNull(content, "DialogPane 必须渲染 content label");
        assertTrue(content.isWrapText(), "错误详情必须可换行");
    }

    @Test
    void onlyConfirmButtonExistsAndIsDefault() throws Exception {
        ErrorDialogView.Prepared prepared = createOnFx("错误", "摘要", "详情");
        DialogPane pane = prepared.alert().getDialogPane();
        List<ButtonType> types = pane.getButtonTypes();
        assertEquals(1, types.size(), "必须只有确认按钮（无 Modena 默认确定/取消）");
        assertEquals(ErrorDialogView.CONFIRM_TEXT, types.get(0).getText());
        Button confirm = (Button) pane.lookupButton(prepared.confirm());
        assertNotNull(confirm, "确认按钮必须存在");
        assertTrue(confirm.isDefaultButton(), "确认按钮必须是默认按钮（初始键盘焦点）");
        assertTrue(confirm.getStyleClass().contains(ErrorDialogView.STYLE_DANGER_BUTTON),
                "确认按钮必须使用暗红危险态样式（与红色边框/徽记统一）");
    }

    @Test
    void confirmButtonTextIsStable() {
        assertEquals("确定", ErrorDialogView.CONFIRM_TEXT);
    }

    // ---- 工具 ----

    private static ErrorDialogView.Prepared createOnFx(String title, String header, String detail)
            throws Exception {
        AtomicReference<ErrorDialogView.Prepared> ref = new AtomicReference<>();
        runOnFx(() -> ref.set(ErrorDialogView.createAlert(title, header, detail)));
        return ref.get();
    }

    private static void runOnFx(Runnable action) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX 线程任务超时");
        if (error.get() != null) {
            throw new AssertionError("FX 线程任务失败", error.get());
        }
    }
}
