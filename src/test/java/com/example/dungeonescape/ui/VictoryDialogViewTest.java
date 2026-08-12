package com.example.dungeonescape.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 胜利弹窗视图测试：{@link VictoryDialogView#createAlert} 的样式化与美术接线。
 *
 * <p>验证 DialogPane 应用 victory-dialog 样式类并显式加载 game.css、graphic 来自
 * {@link VictoryArt#createPortalGraphic()}（素材可用时为传送门裁剪图，素材不可用时
 * 为其内置占位图，任何情况 graphic 都非空）、“下一关”按钮为 next-level-action
 * 主样式、其余动作按钮为 victory-secondary 次级样式。Alert 构造依赖 JavaFX Toolkit
 * （与 {@code GameLayoutTest} 相同的启动模式）；素材提供者每个用例结束后恢复默认。
 */
class VictoryDialogViewTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // 同 JVM 内其他测试类（如 GameLayoutTest）可能已启动 Toolkit：幂等，无需重复启动
        }
    }

    @AfterEach
    void restoreDefaultProvider() {
        VictoryArt.setArtProvider(VictoryArt.DEFAULT_ART_PROVIDER);
    }

    /** 非末关配置：包含 下一关/重新开始/选择关卡 三个动作。 */
    private static VictoryDialog.Config configWithNextLevel() {
        return VictoryDialog.configFor("关卡1", 10,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(1)));
    }

    /**
     * 在 FX 线程上构建弹窗（Alert/DialogPane 构造有严格的 FX 线程检查）。
     *
     * <p>经 {@link Platform#runLater} 执行构建并用 CountDownLatch 等待完成；
     * FX 线程上的异常会被重新抛出为 AssertionError，避免被 runLater 吞掉。
     */
    private static VictoryDialogView.Prepared prepareOnFxThread(VictoryDialog.Config config) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        VictoryDialogView.Prepared[] result = new VictoryDialogView.Prepared[1];
        Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                result[0] = VictoryDialogView.createAlert(config);
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("FX 线程构建弹窗超时");
        }
        if (error[0] != null) {
            throw new AssertionError("FX 线程构建弹窗失败", error[0]);
        }
        return result[0];
    }

    /** 素材可用（默认提供者加载真实资源）时，graphic 必须是传送门裁剪图。 */
    @Test
    void alertUsesPortalCropWhenArtAvailable() throws Exception {
        VictoryArt.setArtProvider(VictoryArt.DEFAULT_ART_PROVIDER);
        assertTrue(VictoryArt.isAvailable(), "测试前置：默认提供者应能加载出素材");

        DialogPane pane = prepareOnFxThread(configWithNextLevel()).alert().getDialogPane();
        ImageView graphic = (ImageView) pane.getGraphic();
        assertNotNull(graphic, "素材可用时 graphic 必须为传送门裁剪图");
        assertSame(VictoryArt.GRAPHIC_VIEWPORT, graphic.getViewport(),
                "必须精确使用 VictoryArt.GRAPHIC_VIEWPORT 裁取");
        assertSame(VictoryArt.artImage(), graphic.getImage(), "必须使用缓存的素材 Image");
        assertEquals(VictoryArt.GRAPHIC_FIT_WIDTH, graphic.getFitWidth(),
                "graphic 必须保持 VictoryArt 的 fitWidth");
        assertTrue(graphic.isPreserveRatio(), "必须保留纵横比");
        assertFalse(graphic.isSmooth(), "像素风应关闭平滑插值");
    }

    /** 素材不可用时 graphic 不得为空，必须落到 VictoryArt 的内置占位图。 */
    @Test
    void alertKeepsFallbackGraphicWhenArtUnavailable() throws Exception {
        VictoryArt.setArtProvider(() -> null);
        assertFalse(VictoryArt.isAvailable(), "测试前置：注入 null 提供者后素材应不可用");

        DialogPane pane = prepareOnFxThread(configWithNextLevel()).alert().getDialogPane();
        ImageView graphic = (ImageView) pane.getGraphic();
        assertNotNull(graphic, "素材不可用时仍必须设置 graphic（占位图兜底）");
        assertNull(graphic.getViewport(), "占位图是完整图像，不得设置素材裁取区");
        assertNotNull(graphic.getImage(), "占位图必须带非空 Image");
        assertFalse(graphic.getImage().isError(), "占位图必须可正常解码");
        assertEquals(VictoryArt.GRAPHIC_VIEWPORT.getWidth(), graphic.getImage().getWidth(), 0.5,
                "占位图应与裁取区同宽");
        assertEquals(VictoryArt.GRAPHIC_VIEWPORT.getHeight(), graphic.getImage().getHeight(), 0.5,
                "占位图应与裁取区同高");
    }

    /** 弹窗 DialogPane：victory-dialog 样式类 + 显式加载 game.css（独立场景必须自带样式表）。 */
    @Test
    void dialogPaneHasVictoryStylesAndGameCss() throws Exception {
        DialogPane pane = prepareOnFxThread(configWithNextLevel()).alert().getDialogPane();
        assertTrue(pane.getStyleClass().contains(VictoryDialogView.STYLE_VICTORY_DIALOG),
                "DialogPane 必须应用 victory-dialog 样式类");
        assertTrue(pane.getStylesheets().stream()
                        .anyMatch(url -> url.endsWith(SceneNavigator.CSS_GAME)),
                "对话框使用独立场景，必须显式加载 game.css");
    }

    /** 按钮样式：下一关为 next-level-action 主样式，其余为 victory-secondary 次级样式。 */
    @Test
    void nextLevelButtonIsPrimaryAndOthersSecondary() throws Exception {
        VictoryDialogView.Prepared prepared = prepareOnFxThread(configWithNextLevel());
        DialogPane pane = prepared.alert().getDialogPane();

        Button next = (Button) pane.lookupButton(prepared.nextLevel());
        assertTrue(next.getStyleClass().contains(VictoryDialogView.STYLE_NEXT_LEVEL),
                "下一关按钮必须使用 next-level-action 主样式");

        for (ButtonType type : new ButtonType[]{prepared.restart(), prepared.levelSelect()}) {
            Button button = (Button) pane.lookupButton(type);
            assertTrue(button.getStyleClass().contains(VictoryDialogView.STYLE_SECONDARY),
                    "其余动作按钮必须使用 victory-secondary 次级样式：" + button.getText());
            assertFalse(button.getStyleClass().contains(VictoryDialogView.STYLE_NEXT_LEVEL),
                    "次级按钮不得混入主样式：" + button.getText());
        }
    }

    /** 非末关（1–4 关）：DialogPane 严格只有 3 个按钮类型，绝无系统默认“确定”。 */
    @Test
    void nonFinalDialogPaneHasExactlyThreeButtons() throws Exception {
        for (int level = 1; level <= 4; level++) {
            VictoryDialog.Config config = VictoryDialog.configFor("关卡" + level, 10,
                    LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(level)));
            VictoryDialogView.Prepared prepared = prepareOnFxThread(config);
            DialogPane pane = prepared.alert().getDialogPane();

            assertEquals(3, pane.getButtonTypes().size(),
                    "关卡 " + level + " 弹窗必须严格只有 3 个按钮（下一关/重新开始/选择关卡），"
                            + "不得残留系统默认“确定”");
            assertEquals(Set.of(prepared.nextLevel(), prepared.restart(), prepared.levelSelect()),
                    Set.copyOf(pane.getButtonTypes()),
                    "关卡 " + level + " 按钮类型必须恰好是配置的三个动作，不允许配置之外的默认 OK");

            assertEquals(VictoryDialog.TEXT_NEXT,
                    ((Button) pane.lookupButton(prepared.nextLevel())).getText(),
                    "下一关按钮文本必须为 " + VictoryDialog.TEXT_NEXT);
            assertEquals(VictoryDialog.TEXT_RESTART,
                    ((Button) pane.lookupButton(prepared.restart())).getText(),
                    "重新开始按钮文本必须为 " + VictoryDialog.TEXT_RESTART);
            assertEquals(VictoryDialog.TEXT_SELECT,
                    ((Button) pane.lookupButton(prepared.levelSelect())).getText(),
                    "选择关卡按钮文本必须为 " + VictoryDialog.TEXT_SELECT);
        }
    }

    /** 弹窗内不得存在任何“确定”按钮（清除 AlertType.INFORMATION 自带 OK 后的验收）。 */
    @Test
    void dialogHasNoConfirmButtonBeyondConfiguredActions() throws Exception {
        VictoryDialogView.Prepared prepared = prepareOnFxThread(configWithNextLevel());
        DialogPane pane = prepared.alert().getDialogPane();

        for (ButtonType type : pane.getButtonTypes()) {
            assertFalse("确定".equals(type.getText()),
                    "不得出现系统默认“确定”按钮，实际按钮：" + type.getText());
            Button button = (Button) pane.lookupButton(type);
            assertFalse("确定".equals(button.getText()),
                    "按钮文本不得为“确定”，实际：" + button.getText());
        }
    }

    /** 末关：严格只有 2 个按钮（重新开始/选择关卡），无下一关、无“确定”。 */
    @Test
    void finalLevelDialogPaneHasExactlyTwoButtonsWithoutNext() throws Exception {
        VictoryDialog.Config config = VictoryDialog.configFor("最后一关", 99,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(5)));
        VictoryDialogView.Prepared prepared = prepareOnFxThread(config);
        DialogPane pane = prepared.alert().getDialogPane();

        assertNull(prepared.nextLevel(), "末关不得产生下一关按钮");
        assertEquals(2, pane.getButtonTypes().size(),
                "末关弹窗必须严格只有 2 个按钮（重新开始/选择关卡），不得残留系统默认“确定”");
        assertEquals(Set.of(prepared.restart(), prepared.levelSelect()),
                Set.copyOf(pane.getButtonTypes()),
                "末关按钮必须恰好是 重新开始/选择关卡");
        for (ButtonType type : pane.getButtonTypes()) {
            assertFalse("确定".equals(type.getText()), "末关也不得出现“确定”按钮");
        }
    }

    /** game.css 文本验收（不依赖脆弱像素）：头部暗色覆盖规则、金色标题、浅色内容、按钮区透明。 */
    @Test
    void gameCssOverridesDarkHeaderAndKeepsGoldTitle() throws Exception {
        String css = readGameCss();

        // 头部暗色：.victory-dialog .header-panel 规则必须存在，覆盖 Modena 的白色头部
        int selector = css.indexOf(".victory-dialog .header-panel");
        assertTrue(selector >= 0, "game.css 必须覆盖 .victory-dialog .header-panel（Modena 白色头部）");
        int open = css.indexOf('{', selector);
        int close = css.indexOf('}', open);
        String headerBlock = css.substring(open, close);
        int bgIndex = headerBlock.indexOf("-fx-background-color");
        assertTrue(bgIndex >= 0, "header-panel 规则必须设置 -fx-background-color");
        String bg = headerBlock.substring(bgIndex).split(";")[0].trim();
        assertFalse(bg.matches("(?s).*:\\s*white\\s*$") || bg.contains("#ffffff") || bg.contains("#FFFFFF"),
                "header-panel 背景不得为白色，实际：" + bg);

        // 标题金色：header-panel .label 规则必须存在且 text-fill 为 #ffd76a
        assertTrue(css.matches("(?s).*?header-panel \\.label\\s*\\{[^}]*#ffd76a[^}]*\\}.*"),
                "标题 label 必须为金色 #ffd76a");
        // 内容浅绿/浅色
        assertTrue(css.matches("(?s).*?victory-dialog \\.content\\.label\\s*\\{[^}]*#9fe8b8[^}]*\\}.*"),
                "内容文本必须为浅绿色 #9fe8b8");
        // 按钮区融入弹窗：背景透明或同色
        assertTrue(css.matches("(?s).*?victory-dialog \\.button-bar\\s*\\{[^}]*transparent[^}]*\\}.*"),
                "button-bar 背景必须透明/同色，不得与暗色弹窗割裂");
    }

    /** 从 classpath 读取 game.css 全文（文本验收样式规则，不依赖渲染像素）。 */
    private static String readGameCss() throws Exception {
        try (InputStream in = VictoryDialogViewTest.class.getResourceAsStream('/' + SceneNavigator.CSS_GAME)) {
            assertNotNull(in, "game.css 必须存在于 classpath：" + SceneNavigator.CSS_GAME);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
