package com.example.dungeonescape.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R 重开过滤器累积回归测试（Stage C 根因：startLevel 每次 addEventFilter，
 * restartLevel 反复调用导致同一 Scene 挂 N 个键盘过滤器；R 事件派发期间
 * 注册新过滤器使 JavaFX 派发链遍历新增项 → 指数级处理 → FX 线程卡死/空白）。
 *
 * <p>验证（真实 JavaFX 事件路径，无生产 autopilot/debug 钩子）：
 * <ol>
 *   <li>多次 R 重开后一次方向键只移动一步（不会被 N 个旧过滤器处理）；</li>
 *   <li>R 重开在严格超时内完成、步数回 0、关卡状态恢复；</li>
 *   <li>点击“重新开始”按钮同样不累积过滤器；</li>
 *   <li>重开后地图恢复初始（同一关卡再次走棋可通关）。</li>
 * </ol>
 */
class GameRestartFilterTest {

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
    void repeatedRestartThenOneMoveMovesExactlyOneStep() throws Exception {
        LoadedGame game = loadGame(LevelCatalog.resourcePathFor(4));
        // 先走 3 步（推巨石压机关 1）
        press(game, KeyCode.RIGHT);
        press(game, KeyCode.RIGHT);
        press(game, KeyCode.DOWN);
        assertEquals("3", game.stepsLabel.getText());
        // 连续 3 次 R 重开：每次在严格超时内完成且步数回 0
        for (int i = 0; i < 3; i++) {
            press(game, KeyCode.R);
            assertEquals("0", game.stepsLabel.getText(), "第 " + (i + 1) + " 次重开后步数必须回 0");
        }
        // 一次方向键：只移动一步（旧过滤器不得重复处理）
        press(game, KeyCode.RIGHT);
        assertEquals("1", game.stepsLabel.getText(), "多次 R 后一次方向键必须只移动一步");
    }

    @Test
    void restartButtonClickDoesNotAccumulateFilters() throws Exception {
        LoadedGame game = loadGame(LevelCatalog.resourcePathFor(4));
        press(game, KeyCode.RIGHT);
        press(game, KeyCode.RIGHT);
        assertEquals("2", game.stepsLabel.getText());
        // 点击“重新开始”按钮两次
        runOnFx(() -> game.restartButton.fire());
        runOnFx(() -> game.restartButton.fire());
        assertEquals("0", game.stepsLabel.getText(), "按钮重开后步数必须回 0");
        // 一次方向键只移动一步
        press(game, KeyCode.RIGHT);
        assertEquals("1", game.stepsLabel.getText(), "按钮重开后一次方向键必须只移动一步");
    }

    @Test
    void resetThenReplayLevelRestoresState() throws Exception {
        LoadedGame game = loadGame(LevelCatalog.resourcePathFor(1));
        // 先走 4 步
        press(game, KeyCode.RIGHT);
        press(game, KeyCode.RIGHT);
        press(game, KeyCode.DOWN);
        press(game, KeyCode.DOWN);
        assertEquals("4", game.stepsLabel.getText());
        // R 重开
        press(game, KeyCode.R);
        assertEquals("0", game.stepsLabel.getText(), "重开后步数必须回 0");
        // 重开后地图/关卡恢复：官方解法前缀可再次行走（不触发胜利弹窗阻塞）
        for (char c : "RRDDR".toCharArray()) {
            press(game, switch (c) {
                case 'R' -> KeyCode.RIGHT;
                case 'L' -> KeyCode.LEFT;
                case 'U' -> KeyCode.UP;
                case 'D' -> KeyCode.DOWN;
                default -> throw new AssertionError(c);
            });
        }
        assertEquals("5", game.stepsLabel.getText(), "重开后官方解法前缀必须可再次行走（状态恢复）");
    }

    // ---- 工具 ----

    private record LoadedGame(Parent root, GameController controller,
                              Label stepsLabel, ScrollPane mapScrollPane,
                              GridPane grid, Button restartButton) {
    }

    private static LoadedGame loadGame(String levelResourcePath) throws Exception {
        AtomicReference<LoadedGame> ref = new AtomicReference<>();
        runOnFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        GameRestartFilterTest.class.getClassLoader().getResource(SceneNavigator.FXML_GAME));
                Parent root = loader.load();
                GameController controller = loader.getController();
                Scene scene = new Scene(root, 960, 640);
                scene.getStylesheets().add(GameRestartFilterTest.class.getClassLoader()
                        .getResource(SceneNavigator.CSS_GAME).toExternalForm());
                scene.getStylesheets().add(GameRestartFilterTest.class.getClassLoader()
                        .getResource(SceneNavigator.CSS_DUNGEON_UI).toExternalForm());
                javafx.scene.layout.BorderPane borderPane =
                        (javafx.scene.layout.BorderPane) root.getChildrenUnmodifiable().get(1);
                VBox header = (VBox) borderPane.getTop();
                HBox status = (HBox) header.getChildren().get(2);
                Label steps = (Label) ((HBox) status.getChildren().get(0)).getChildren().get(1);
                ScrollPane scroll = (ScrollPane) borderPane.getCenter();
                VBox boardFrame = (VBox) ((StackPane) scroll.getContent()).getChildren().get(0);
                GridPane grid = (GridPane) boardFrame.getChildren().get(0);
                HBox footer = (HBox) borderPane.getBottom();
                controller.startLevel(levelResourcePath);
                ref.set(new LoadedGame(root, controller, steps, scroll, grid,
                        (Button) footer.getChildren().get(0)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return ref.get();
    }

    private static void press(LoadedGame game, KeyCode code) throws Exception {
        runOnFx(() -> game.root.getScene().getRoot().fireEvent(new KeyEvent(
                KeyEvent.KEY_PRESSED, code.getChar(), code.getName(), code, false, false, false, false)));
        runOnFx(() -> game.root.getScene().getRoot().fireEvent(new KeyEvent(
                KeyEvent.KEY_RELEASED, code.getChar(), code.getName(), code, false, false, false, false)));
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
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX 线程任务超时（R 重开必须在严格超时内完成）");
        if (error.get() != null) {
            throw new AssertionError("FX 线程任务失败", error.get());
        }
    }
}
