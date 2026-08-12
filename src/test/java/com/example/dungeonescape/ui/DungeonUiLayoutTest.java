package com.example.dungeonescape.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 响应式与视觉完整性测试：
 * <ul>
 *   <li>主菜单 / 选关页在默认窗口（960×640）与 800×600 下所有按钮不裁剪、不越界、
 *       卡片互不重叠、长中文说明可换行且不挤坏层级；</li>
 *   <li>背景不是纯色空白：快照采样颜色丰富（真实纹理 + 暗角），纹理层存在；</li>
 *   <li>默认尺寸下选关页不出现无意义滚动条（内容不超出视口）。</li>
 * </ul>
 */
class DungeonUiLayoutTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // 幂等
        }
    }

    @Test
    void mainMenuFitsAtDefaultAnd800x600() throws Exception {
        Parent root = load(SceneNavigator.FXML_MAIN_MENU);
        for (double[] size : new double[][]{{960, 640}, {800, 600}}) {
            layout(root, size[0], size[1]);
            // 内容列 VBox（maxWidth 限制宽度、居中）；按钮相对 VBox 的位置
            // 用 layoutBounds 断言（boundsInParent 含面板阴影 effect，会外溢 18px，属视觉合理范围）
            javafx.scene.layout.VBox content = (javafx.scene.layout.VBox) root.getChildrenUnmodifiable().get(1);
            double contentMinX = content.getLayoutX();
            double contentMinY = content.getLayoutY();
            double contentMaxX = contentMinX + content.getLayoutBounds().getWidth();
            double contentMaxY = contentMinY + content.getLayoutBounds().getHeight();
            assertTrue(contentMinX >= 0 && contentMinY >= 0
                            && contentMaxX <= size[0] && contentMaxY <= size[1],
                    "内容列必须完整落在场景内 " + size[0] + "x" + size[1] + "："
                            + contentMinX + ".." + contentMaxX + " x " + contentMinY + ".." + contentMaxY);
            for (Node node : List.of(root.lookup("#startButton"), root.lookup("#exitButton"))) {
                Bounds b = node.getBoundsInParent();
                assertTrue(b.getMinX() >= 0 && b.getMinY() >= 0
                                && b.getMaxX() <= content.getLayoutBounds().getWidth()
                                && b.getMaxY() <= content.getLayoutBounds().getHeight(),
                        "主菜单按钮必须完整落在内容列内 " + size[0] + "x" + size[1] + "：" + node.getId()
                                + " bounds=" + b + " content=" + content.getLayoutBounds());
            }
        }
    }

    @Test
    void levelSelectFitsAtDefaultAnd800x600() throws Exception {
        Parent root = load(SceneNavigator.FXML_LEVEL_SELECT);
        for (double[] size : new double[][]{{960, 640}, {800, 600}}) {
            layout(root, size[0], size[1]);
            // ScrollPane 内容不在 lookup 的 CSS 树中，按样式类递归查找卡片容器
            javafx.scene.layout.VBox cardsBox = findFirst(root,
                    n -> n instanceof javafx.scene.layout.VBox v && v.getStyleClass().contains("level-cards"));
            assertTrue(cardsBox != null, "必须找到关卡卡片容器");
            List<Node> cards = new ArrayList<>();
            for (Node node : cardsBox.getChildren()) {
                cards.add(node);
            }
            assertEquals(5, cards.size());
            // 内容列（maxWidth 限制、居中）必须完整落在场景内；卡片容器在 ScrollPane 内、
            // 卡片在容器内（boundsInParent 同父比较）
            javafx.scene.layout.VBox content = (javafx.scene.layout.VBox) root.getChildrenUnmodifiable().get(1);
            Bounds contentBounds = content.getBoundsInParent();
            assertTrue(contentBounds.getMinX() >= 0 && contentBounds.getMinY() >= 0
                            && contentBounds.getMaxX() <= size[0] && contentBounds.getMaxY() <= size[1],
                    "内容列必须完整落在场景内 " + size[0] + "x" + size[1] + "：" + contentBounds);
            // 返回按钮（内容列直接子级）必须完整落在内容列内
            Bounds back = root.lookup("#backButton").getBoundsInParent();
            assertTrue(back.getMinX() >= 0 && back.getMinY() >= 0
                            && back.getMaxX() <= contentBounds.getWidth()
                            && back.getMaxY() <= contentBounds.getHeight(),
                    "返回按钮必须完整落在内容列内：" + back);
            // 卡片互不重叠（同父坐标系）
            for (int i = 0; i < cards.size(); i++) {
                for (int j = i + 1; j < cards.size(); j++) {
                    assertTrue(!intersects(cards.get(i).getBoundsInParent(), cards.get(j).getBoundsInParent()),
                            "卡片 " + (i + 1) + " 与卡片 " + (j + 1) + " 不得重叠");
                }
            }
            // 卡片内文字不溢出卡片边界（sceneToLocal 与 localToScene 互为逆变换，
            // 抵消无窗口 Scene 的坐标缩放；inCard 为文字在卡片自身坐标系中的边界，
            // 与卡片 (0,0,宽,高) 比较）
            for (Node card : cards) {
                for (Text text : findTexts(card)) {
                    Bounds tb = text.localToScene(text.getBoundsInParent());
                    Bounds inCard = card.sceneToLocal(tb);
                    double slack = 4.0; // 阴影/描边容差
                    double cardW = card.getBoundsInLocal().getWidth();
                    double cardH = card.getBoundsInLocal().getHeight();
                    assertTrue(inCard.getMinX() >= -slack
                                    && inCard.getMaxX() <= cardW + slack
                                    && inCard.getMinY() >= -slack
                                    && inCard.getMaxY() <= cardH + slack,
                            "卡片文字不得溢出卡片边界：" + text.getText()
                                    + " inCard=" + inCard + " card=" + cardW + "x" + cardH);
                }
            }
        }
    }

    @Test
    void levelSelectShowsNoScrollbarAtDefaultSize() throws Exception {
        Parent root = load(SceneNavigator.FXML_LEVEL_SELECT);
        layout(root, 960, 640);
        ScrollPane scroll = findFirst(root, n -> n instanceof ScrollPane);
        assertTrue(scroll != null, "必须找到深色 ScrollPane");
        // 默认尺寸下内容不应超出视口（无无意义滚动条）
        assertTrue(scroll.getContent().getBoundsInParent().getHeight()
                        <= scroll.getViewportBounds().getHeight() + 1,
                "默认尺寸下卡片列表不得超出视口（不应出现无意义滚动条）");
        assertTrue(scroll.getContent().getBoundsInParent().getWidth()
                        <= scroll.getViewportBounds().getWidth() + 1,
                "默认尺寸下卡片列表宽度不得超出视口");
    }

    @Test
    void dungeonBackgroundIsNotSolidBlank() throws Exception {
        // 快照采样：默认与 800×600 下背景区域颜色丰富（非纯色空白）
        for (double[] size : new double[][]{{960, 640}, {800, 600}}) {
            StackPane background = DungeonTheme.createBackground();
            layout(background, size[0], size[1]);
            WritableImage snap = snapshotOnFx(background);
            Set<Long> colors = new HashSet<>();
            int stepX = Math.max(1, (int) (snap.getWidth() / 16));
            int stepY = Math.max(1, (int) (snap.getHeight() / 12));
            for (int y = (int) (snap.getHeight() * 0.12); y < snap.getHeight() * 0.88; y += stepY) {
                for (int x = (int) (snap.getWidth() * 0.12); x < snap.getWidth() * 0.88; x += stepX) {
                    Color c = snap.getPixelReader().getColor(x, y);
                    colors.add((long) (Math.round(c.getRed() * 255) / 8) << 16
                            | (long) (Math.round(c.getGreen() * 255) / 8) << 8
                            | Math.round(c.getBlue() * 255) / 8);
                }
            }
            assertTrue(colors.size() >= 4,
                    "背景必须是纹理+暗角的多色画面而非纯色空白（" + size[0] + "x" + size[1]
                            + " 采样到 " + colors.size() + " 种颜色）");
        }
    }

    @Test
    void buttonsAreComfortableHitTargetsAndDecorUsesRealSprites() throws Exception {
        Parent root = load(SceneNavigator.FXML_MAIN_MENU);
        layout(root, 960, 640);
        // 主按钮触控目标：宽度 ≥ 240、高度 ≥ 44
        for (Node node : List.of(root.lookup("#startButton"), root.lookup("#exitButton"))) {
            Bounds b = node.getBoundsInParent();
            assertTrue(b.getWidth() >= 240, "主菜单按钮宽度应 ≥ 240（触控目标）");
            assertTrue(b.getHeight() >= 44, "主菜单按钮高度应 ≥ 44（触控目标）");
        }
        // 背景装饰使用真实图集 sprite（ImageView 非空）
        Node background = root.getChildrenUnmodifiable().get(0);
        long sprites = findAll(background).stream()
                .filter(ImageView.class::isInstance)
                .map(ImageView.class::cast)
                .filter(v -> v.getImage() != null)
                .count();
        assertTrue(sprites >= 2, "背景装饰必须使用真实 sprite（至少 2 个 ImageView）");
    }

    // ---- 工具 ----

    private static Parent load(String fxmlPath) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                DungeonUiLayoutTest.class.getClassLoader().getResource(fxmlPath));
        return loader.load();
    }

    private static void layout(Parent root, double width, double height) throws Exception {
        runOnFx(() -> {
            // 附加场景并加载真实样式表（否则 CSS 规则不生效，控件尺寸失真）
            if (root.getScene() == null) {
                Scene scene = new Scene(root);
                scene.getStylesheets().add(cssUrl(SceneNavigator.CSS_GAME));
                scene.getStylesheets().add(cssUrl(SceneNavigator.CSS_DUNGEON_UI));
            }
            root.resize(width, height);
            root.applyCss();
            root.layout();
        });
    }

    private static String cssUrl(String classpathPath) {
        return DungeonUiLayoutTest.class.getClassLoader().getResource(classpathPath).toExternalForm();
    }

    private static WritableImage snapshotOnFx(StackPane node) throws Exception {
        AtomicReference<WritableImage> ref = new AtomicReference<>();
        runOnFx(() -> ref.set(node.snapshot(new SnapshotParameters(), null)));
        return ref.get();
    }

    /** 递归查找节点（含 ScrollPane content 内部）。 */
    private static <T extends Node> T findFirst(Node node, java.util.function.Predicate<Node> test) {
        if (test.test(node)) {
            @SuppressWarnings("unchecked")
            T found = (T) node;
            return found;
        }
        if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            T found = findFirst(scrollPane.getContent(), test);
            if (found != null) {
                return found;
            }
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = findFirst(child, test);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean intersects(Bounds a, Bounds b) {
        return a.intersects(b) && (a.getMinX() < b.getMaxX() - 1 && b.getMinX() < a.getMaxX() - 1
                && a.getMinY() < b.getMaxY() - 1 && b.getMinY() < a.getMaxY() - 1);
    }

    private static List<Text> findTexts(Node node) {
        List<Text> texts = new ArrayList<>();
        collect(node, n -> n instanceof Text, n -> texts.add((Text) n));
        return texts;
    }

    private static List<Node> findAll(Node node) {
        List<Node> nodes = new ArrayList<>();
        collect(node, n -> true, nodes::add);
        return nodes;
    }

    private static void collect(Node node, java.util.function.Predicate<Node> accept,
                                java.util.function.Consumer<Node> sink) {
        if (accept.test(node)) {
            sink.accept(node);
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collect(child, accept, sink);
            }
        }
    }

    /** 在 FX 线程执行动作；FX 线程抛出的异常重抛到测试线程。 */
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
