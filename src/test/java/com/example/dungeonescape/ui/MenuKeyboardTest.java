package com.example.dungeonescape.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主菜单键盘可达性测试：两个按钮 focusTraversable=true、初始焦点落在
 * “开始游戏”（窗口显示前后两条路径）、Tab/Shift+Tab 循环、Enter/Space 可触发。
 *
 * <p>依赖真实窗口显示（本机有显示环境），与 GameLayoutTest 共享 Toolkit；
 * 每个用例创建独立 Stage，结束后隐藏清理。FX 线程任务抛出的异常经
 * {@link #runOnFx} 重抛到测试线程，避免异常被 FX 线程吞掉。
 */
class MenuKeyboardTest {

    private Stage stage;

    @BeforeAll
    static void startToolkit() throws Exception {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // 幂等
        }
        // 关闭测试窗口不得终止 FX 平台（否则后续测试的 runLater 不再执行）
        runOnFx(() -> Platform.setImplicitExit(false));
    }

    @AfterEach
    void tearDownStage() throws Exception {
        if (stage != null) {
            Stage s = stage;
            stage = null;
            runOnFx(() -> {
                s.hide();
                s.setScene(null);
            });
        }
    }

    @Test
    void bothButtonsAreFocusTraversable() throws Exception {
        LoadedMenu menu = loadMenu();
        assertTrue(menu.startButton.isFocusTraversable(), "开始游戏必须可聚焦（无键盘障碍）");
        assertTrue(menu.exitButton.isFocusTraversable(), "退出游戏必须可聚焦（无键盘障碍）");
    }

    @Test
    void initialFocusTargetIsStartButton() throws Exception {
        LoadedMenu menu = loadMenu();
        assertEquals(menu.startButton, menu.controller.initialFocusTarget(),
                "初始焦点安排必须指向开始游戏");
    }

    @Test
    void requestInitialFocusBeforeWindowShowFocusesStartButton() throws Exception {
        LoadedMenu menu = loadMenu();
        stage = newStage(menu.root);
        // 窗口显示前调用 requestInitialFocus（首次启动路径：Stage 尚未 show）
        runOnFx(() -> menu.controller.requestInitialFocus());
        showStage();
        awaitFocus(stage.getScene(), menu.startButton);
    }

    @Test
    void requestInitialFocusAfterWindowShowFocusesStartButton() throws Exception {
        LoadedMenu menu = loadMenu();
        stage = newStage(menu.root);
        showStage();
        // 窗口已显示后调用 requestInitialFocus（场景切换路径）
        runOnFx(() -> menu.controller.requestInitialFocus());
        awaitFocus(stage.getScene(), menu.startButton);
    }

    @Test
    void tabCyclesBetweenButtons() throws Exception {
        LoadedMenu menu = loadMenu();
        stage = newStage(menu.root);
        showStage();
        runOnFx(menu.startButton::requestFocus);
        awaitFocus(stage.getScene(), menu.startButton);

        // Tab：开始游戏 → 退出游戏
        fireKey(KeyCode.TAB, false);
        awaitFocus(stage.getScene(), menu.exitButton);

        // 再 Tab：退出游戏 → 开始游戏（循环）
        fireKey(KeyCode.TAB, false);
        awaitFocus(stage.getScene(), menu.startButton);

        // Shift+Tab：开始游戏 → 退出游戏
        fireKey(KeyCode.TAB, true);
        awaitFocus(stage.getScene(), menu.exitButton);
    }

    @Test
    void enterAndSpaceTriggerFocusedButton() throws Exception {
        LoadedMenu menu = loadMenu();
        AtomicReference<Boolean> startClicked = new AtomicReference<>(false);
        runOnFx(() -> menu.startButton.setOnAction(e -> startClicked.set(true)));
        stage = newStage(menu.root);
        showStage();
        runOnFx(menu.startButton::requestFocus);
        awaitFocus(stage.getScene(), menu.startButton);

        fireKey(KeyCode.ENTER, false);
        awaitFxPulse();
        assertEquals(Boolean.TRUE, startClicked.get(), "Enter 必须触发聚焦按钮");

        // Space
        startClicked.set(false);
        fireKey(KeyCode.SPACE, false);
        awaitFxPulse();
        assertEquals(Boolean.TRUE, startClicked.get(), "Space 必须触发聚焦按钮");
    }

    @Test
    void fxmlKeepsRealButtonsWithActions() throws Exception {
        LoadedMenu menu = loadMenu();
        assertNotNull(menu.startButton.getOnAction(), "开始游戏必须有真实动作");
        assertNotNull(menu.exitButton.getOnAction(), "退出游戏必须有真实动作");
    }

    // ---- 工具 ----

    private record LoadedMenu(Parent root, MainMenuController controller,
                              Button startButton, Button exitButton) {
    }

    private LoadedMenu loadMenu() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MenuKeyboardTest.class.getClassLoader().getResource(SceneNavigator.FXML_MAIN_MENU));
        Parent root = loader.load();
        MainMenuController controller = loader.getController();
        return new LoadedMenu(root, controller,
                (Button) root.lookup("#startButton"),
                (Button) root.lookup("#exitButton"));
    }

    /** 创建已附加场景的 Stage（必须在 FX 线程）。 */
    private Stage newStage(Parent root) throws Exception {
        AtomicReference<Stage> ref = new AtomicReference<>();
        runOnFx(() -> {
            Stage s = new Stage();
            s.setScene(new Scene(root));
            ref.set(s);
        });
        return ref.get();
    }

    private void showStage() throws Exception {
        runOnFx(stage::show);
        awaitFxPulse();
    }

    private void fireKey(KeyCode code, boolean shift) throws Exception {
        // 事件必须发到 focusOwner（Button）才能触发 Button 的 Enter/Space 默认行为；
        // Tab 的焦点遍历在 Scene 层处理，无论 target 是谁都会响应
        runOnFx(() -> {
            Node target = stage.getScene().getFocusOwner() != null
                    ? stage.getScene().getFocusOwner() : stage.getScene().getRoot();
            target.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, code.getChar(), code.getName(),
                    code, shift, false, false, false));
            target.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, code.getChar(), code.getName(),
                    code, shift, false, false, false));
        });
    }

    private static void awaitFocus(Scene scene, Button expected) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            boolean focused = queryOnFx(() -> scene.getFocusOwner() == expected);
            if (focused) {
                awaitFxPulse();
                return;
            }
            Thread.sleep(40);
        }
        assertTrue(false,
                "焦点必须落在 " + expected.getId() + "，当前=" + queryOnFxValue(scene::getFocusOwner));
    }

    private static boolean queryOnFx(java.util.function.Supplier<Object> query) throws Exception {
        return Boolean.TRUE.equals(queryOnFxValue(query));
    }

    private static Object queryOnFxValue(java.util.function.Supplier<Object> query) throws Exception {
        AtomicReference<Object> result = new AtomicReference<>();
        runOnFx(() -> result.set(query.get()));
        return result.get();
    }

    private static void awaitFxPulse() throws Exception {
        runOnFx(() -> { });
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
