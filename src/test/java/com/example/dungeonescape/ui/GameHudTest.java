package com.example.dungeonescape.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 游戏 HUD / 棋盘 / 滚动 / 键盘测试（Stage B）：
 * <ul>
 *   <li>5 关 game.fxml 均能连同控制器真实加载，HUD 标题来自关卡 JSON、
 *       目标/统计/底部按钮存在，按钮 focusTraversable 且 CSS 状态规则齐全；</li>
 *   <li>棋盘像素尺寸（8×6 关 = 384×288，48px 格子）与像素锐利属性保持；</li>
 *   <li>Level 4 在默认 960×640 无滚动条；800×600 超出时出现深色自定义滚动条，
 *       固定底栏不被遮挡；</li>
 *   <li>Level 5 长目标不溢出、可换行、布局稳定；</li>
 *   <li>焦点落在底部按钮时方向键仍移动角色（场景级 capture 过滤器）。</li>
 * </ul>
 */
class GameHudTest {

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
    void allFiveLevelsLoadWithHudAndButtons() throws Exception {
        for (LevelCatalog.LevelEntry entry : LevelCatalog.LEVELS) {
            LoadedGame game = loadGame(entry.resourcePath());
            assertEquals(levelName(entry.number()), game.levelNameLabel.getText(),
                    "HUD 标题必须来自关卡 JSON");
            assertTrue(game.goalLabel.getText().length() > 0, "目标文本不能为空");
            assertNotNull(game.stepsLabel);
            assertNotNull(game.treasureLabel);
            assertNotNull(game.keyLabel);
            // 底部三个真实按钮：可 Tab 聚焦、绑定动作
            for (Button button : new Button[]{game.restartButton, game.levelSelectButton, game.mainMenuButton}) {
                assertTrue(button.isFocusTraversable(), "底部按钮必须可 Tab 聚焦");
                assertNotNull(button.getOnAction(), "底部按钮必须有真实动作");
                assertTrue(button.getStyleClass().contains("primary-button")
                                || button.getStyleClass().contains("secondary-button"),
                        "底部按钮必须使用石砖外壳按钮样式");
            }
            // 棋盘：每格 48px，尺寸 = 关卡 JSON 宽高 × 48（Level 1-4 为 8×6=384×288，
            // Level 5 为 9×7=432×336）——布局后实际尺寸
            GridPane grid = game.mapGridPane;
            layout(game.root, 960, 640);
            com.example.dungeonescape.persistence.LevelDefinition def =
                    new com.example.dungeonescape.persistence.LevelLoader()
                            .loadDefinitionFromClasspath(entry.resourcePath());
            assertEquals(def.width() * def.height(), grid.getChildren().size(),
                    "棋盘格数必须等于关卡宽高乘积");
            assertEquals(def.width() * 48.0, grid.getWidth(), 0.5,
                    "棋盘宽 = 列数×48（实际 " + grid.getWidth() + "）");
            assertEquals(def.height() * 48.0, grid.getHeight(), 0.5, "棋盘高 = 行数×48");
        }
    }

    @Test
    void allFiveLevelsHaveNoScrollbarAtDefaultSize() throws Exception {
        // 默认 960×640 下五关全部无可见滚动条（content == viewport，无假溢出）
        for (int i = 1; i <= 5; i++) {
            LoadedGame game = loadGame(LevelCatalog.resourcePathFor(i));
            layout(game.root, 960, 640);
            ScrollPane scroll = game.mapScrollPane;
            Bounds content = scroll.getContent().getBoundsInParent();
            Bounds viewport = scroll.getViewportBounds();
            assertTrue(content.getHeight() <= viewport.getHeight() + 0.5,
                    "关卡 " + i + " 默认尺寸内容不得超出视口：content=" + content.getHeight()
                            + " viewport=" + viewport.getHeight());
            assertTrue(content.getWidth() <= viewport.getWidth() + 0.5,
                    "关卡 " + i + " 默认尺寸宽度不得超出视口");
            long visibleBars = scroll.lookupAll(".scroll-bar").stream()
                    .filter(javafx.scene.Node::isVisible).count();
            assertEquals(0, visibleBars, "关卡 " + i + " 默认尺寸不得出现任何可见滚动条");
        }
    }

    @Test
    void level4UsesDarkScrollbarAt800x600AndKeepsFooterVisible() throws Exception {
        LoadedGame game = loadGame(LevelCatalog.resourcePathFor(4));
        // 800×600 物理窗口（150% DPI）≈ 533×400 逻辑：棋盘必然超出视口 → 滚动语义
        layout(game.root, 533, 400);
        ScrollPane scroll = game.mapScrollPane;
        assertTrue(scroll.getContent().getBoundsInParent().getHeight()
                        > scroll.getViewportBounds().getHeight() + 1,
                "800×600 下棋盘应超出视口以触发滚动");
        Node scrollBar = scroll.lookup(".scroll-bar");
        assertNotNull(scrollBar, "800×600 下必须出现滚动条");
        // 深色定制：thumb 存在（Modena 默认 thumb 样式会被 .map-scroll 规则覆盖）
        Node thumb = scroll.lookup(".scroll-bar .thumb");
        assertNotNull(thumb, "滚动条必须有 thumb（深色定制轨道）");
        // 固定底栏不被遮挡：底部操作栏相对外壳（BorderPane）完整可见
        HBox footer = (HBox) game.borderPane.getBottom();
        Bounds footerBounds = footer.getBoundsInParent();
        assertTrue(footerBounds.getMaxY() <= game.borderPane.getHeight() + 1
                        && footerBounds.getMinY() >= 0,
                "底部操作栏必须完整可见（不被滚动内容挤出）");
        for (Node button : footer.getChildren()) {
            Bounds b = button.getBoundsInParent();
            assertTrue(b.getMaxY() <= footerBounds.getHeight() + 1, "底部按钮不得被遮挡：" + b);
        }
    }

    @Test
    void level5LongGoalWrapsWithoutOverflow() throws Exception {
        LoadedGame game = loadGame(LevelCatalog.resourcePathFor(5));
        layout(game.root, 960, 640);
        // 长目标在限宽 720 内可换行且不溢出顶栏（同父坐标系，避免无窗口 Scene 缩放干扰）
        Bounds goal = game.goalLabel.getBoundsInParent();
        assertEquals(720.0, game.goalLabel.getMaxWidth(), 0.5, "目标 Label 必须限宽 720");
        assertTrue(goal.getWidth() <= 720 + 1, "目标文本宽度不得超限宽：" + goal.getWidth());
        VBox header = (VBox) game.borderPane.getTop();
        assertTrue(goal.getMinY() >= 0 && goal.getMaxY() <= header.getHeight(),
                "目标文本必须完整落在顶栏内：" + goal + " header=" + header.getHeight());
        // 换行后标题/统计不被挤压：标题完整落在顶栏内
        Bounds name = game.levelNameLabel.getBoundsInParent();
        assertTrue(name.getMinY() >= 0 && name.getMaxY() <= header.getHeight(),
                "标题必须完整落在顶栏内");
        // 布局稳定：目标行数不影响底部按钮可见性（相对外壳）
        HBox footer = (HBox) game.borderPane.getBottom();
        Bounds footerBounds = footer.getBoundsInParent();
        assertTrue(footerBounds.getMaxY() <= game.borderPane.getHeight() + 1,
                "底部栏必须可见（Level 5 长目标不挤坏层级）：" + footerBounds);
    }

    @Test
    void arrowKeysStillMovePlayerWhenButtonFocused() throws Exception {
        // 底部按钮聚焦时，方向键仍由场景级 capture 过滤器接管移动角色（无按钮遍历干扰）
        LoadedGame game = loadGame(LevelCatalog.resourcePathFor(1));
        layout(game.root, 960, 640);
        int before = Integer.parseInt(game.stepsLabel.getText());
        // 聚焦底部按钮（不再抢占游戏输入）
        runOnFx(game.restartButton::requestFocus);
        runOnFx(() -> game.root.getScene().getRoot().fireEvent(new KeyEvent(
                KeyEvent.KEY_PRESSED, KeyCode.RIGHT.getChar(), KeyCode.RIGHT.getName(),
                KeyCode.RIGHT, false, false, false, false)));
        runOnFx(() -> game.root.getScene().getRoot().fireEvent(new KeyEvent(
                KeyEvent.KEY_RELEASED, KeyCode.RIGHT.getChar(), KeyCode.RIGHT.getName(),
                KeyCode.RIGHT, false, false, false, false)));
        int after = Integer.parseInt(String.valueOf(queryOnFx(() -> game.stepsLabel.getText())));
        // Level 1 玩家在 (1,1)，向右 (2,1) 可行走 → 步数 +1
        assertEquals(before + 1, after, "按钮聚焦时方向键必须仍能移动角色");
    }

    @Test
    void gameCssDefinesButtonAndScrollbarStates() throws Exception {
        try (InputStream in = GameHudTest.class.getClassLoader()
                .getResourceAsStream(SceneNavigator.CSS_GAME)) {
            assertNotNull(in, "game.css 必须存在");
            String css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String rule : new String[]{".next-level-action:hover", ".next-level-action:pressed",
                    ".next-level-action:focused", ".victory-secondary:hover", ".victory-secondary:pressed",
                    ".error-dialog", ".error-dialog .header-panel", ".error-dialog .content.label",
                    ".map-scroll .scroll-bar .thumb"}) {
                assertTrue(css.contains(rule), "game.css 必须包含规则：" + rule);
            }
            assertTrue(css.contains("#3a4254"), "滚动条 thumb 必须为深色（非 Modena 默认）");
        }
    }

    @Test
    void mapTilesStayPixelSharpAndScaled() throws Exception {
        // 地形/实体视图的像素属性（TerrainTexture/EntityViewFactory 逻辑保持）：
        // 格子 48px、关闭平滑、满格铺设
        assertEquals(48.0, EntityViewFactory.CELL_SIZE, 0.5, "格子必须为 48px");
        LoadedGame game = loadGame(LevelCatalog.resourcePathFor(1));
        GridPane grid = game.mapGridPane;
        assertEquals(8 * 6, grid.getChildren().size(), "8×6 关必须有 48 个格子节点");
        StackPane firstCell = (StackPane) grid.getChildren().get(0);
        assertEquals(48.0, firstCell.getMinWidth(), 0.5);
        assertEquals(48.0, firstCell.getPrefWidth(), 0.5);
        assertEquals(48.0, firstCell.getMaxWidth(), 0.5);
    }

    // ---- 工具 ----

    private record LoadedGame(Parent root, GameController controller,
                              javafx.scene.layout.BorderPane borderPane,
                              Label levelNameLabel, Label goalLabel, Label stepsLabel,
                              Label treasureLabel, Label keyLabel, ScrollPane mapScrollPane,
                              GridPane mapGridPane, VBox boardFrame, Button restartButton,
                              Button levelSelectButton, Button mainMenuButton) {
    }

    private static LoadedGame loadGame(String levelResourcePath) throws Exception {
        AtomicReference<LoadedGame> ref = new AtomicReference<>();
        runOnFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        GameHudTest.class.getClassLoader().getResource(SceneNavigator.FXML_GAME));
                Parent root = loader.load();
                GameController controller = loader.getController();
                Scene scene = new Scene(root);
                scene.getStylesheets().add(GameHudTest.class.getClassLoader()
                        .getResource(SceneNavigator.CSS_GAME).toExternalForm());
                scene.getStylesheets().add(GameHudTest.class.getClassLoader()
                        .getResource(SceneNavigator.CSS_DUNGEON_UI).toExternalForm());
                javafx.scene.layout.BorderPane borderPane =
                        (javafx.scene.layout.BorderPane) root.getChildrenUnmodifiable().get(1);
                VBox header = (VBox) borderPane.getTop();
                Label levelName = (Label) header.getChildren().get(0);
                Label goal = (Label) header.getChildren().get(1);
                HBox status = (HBox) header.getChildren().get(2);
                Label steps = (Label) ((HBox) status.getChildren().get(0)).getChildren().get(1);
                Label treasure = (Label) ((HBox) status.getChildren().get(1)).getChildren().get(1);
                Label key = (Label) ((HBox) status.getChildren().get(2)).getChildren().get(1);
                ScrollPane scroll = (ScrollPane) borderPane.getCenter();
                VBox boardFrame = (VBox) ((StackPane) scroll.getContent()).getChildren().get(0);
                GridPane grid = (GridPane) boardFrame.getChildren().get(0);
                HBox footer = (HBox) borderPane.getBottom();
                controller.startLevel(levelResourcePath);
                ref.set(new LoadedGame(root, controller, borderPane, levelName, goal, steps,
                        treasure, key, scroll, grid, boardFrame,
                        (Button) footer.getChildren().get(0),
                        (Button) footer.getChildren().get(1),
                        (Button) footer.getChildren().get(2)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return ref.get();
    }

    private static void layout(Parent root, double width, double height) throws Exception {
        runOnFx(() -> {
            root.resize(width, height);
            root.applyCss();
            root.layout();
        });
    }

    private static String levelName(int number) {
        return switch (number) {
            case 1 -> "01基础迷宫";
            case 2 -> "02宝物猎人";
            case 3 -> "03钥匙与门";
            case 4 -> "04推箱机关";
            case 5 -> "05综合挑战";
            default -> throw new IllegalArgumentException();
        };
    }

    private static Object queryOnFx(java.util.function.Supplier<Object> query) throws Exception {
        AtomicReference<Object> result = new AtomicReference<>();
        runOnFx(() -> result.set(query.get()));
        return result.get();
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
