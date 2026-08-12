package com.example.dungeonescape.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 选关页键盘可达性与内容测试：恰有 5 张可聚焦关卡卡片 + 返回按钮，
 * 每张卡片含真实图集图标、关卡编号/名称（来自 JSON）与机制说明，
 * 初始焦点在第一关，Tab 循环与 UP/DOWN 方向键导航均可操作。
 */
class LevelSelectKeyboardTest {

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
    void tearDown() throws Exception {
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
    void exactlyFiveFocusableLevelCardsAndBackButton() throws Exception {
        LoadedSelect select = loadSelect();
        assertEquals(5, select.cards.size(), "必须恰有 5 张关卡卡片");
        for (Button card : select.cards) {
            assertTrue(card.isFocusTraversable(), "关卡卡片必须可聚焦");
            assertFalse(card.isDisabled(), "内置关卡必须可用（非置灰）");
        }
        assertTrue(select.backButton.isFocusTraversable(), "返回按钮必须可聚焦");
        assertNotNull(select.backButton.getOnAction(), "返回按钮必须有动作");
    }

    @Test
    void eachCardHasRealIconNumberNameAndMechanism() throws Exception {
        LoadedSelect select = loadSelect();
        for (int i = 0; i < 5; i++) {
            Button card = select.cards.get(i);
            ImageView icon = findIcon(card);
            assertNotNull(icon, "卡片 " + (i + 1) + " 必须含图标");
            assertNotNull(icon.getImage(), "卡片 " + (i + 1) + " 图标必须引用真实图集");
            assertTrue(icon.getViewport() != null && icon.getViewport().getWidth() >= 100,
                    "卡片 " + (i + 1) + " 图标必须是紧贴内容的真实裁切");
            List<Label> labels = findLabels(card);
            assertEquals(2, labels.size(), "卡片 " + (i + 1) + " 必须有编号/名称行与机制说明行");
            // 编号与名称来自关卡 JSON（level1.name = "01基础迷宫"）
            String expectedTitle = "第 " + (i + 1) + " 关：" + levelName(i + 1);
            assertEquals(expectedTitle, labels.get(0).getText(),
                    "卡片 " + (i + 1) + " 标题必须来自关卡数据");
            assertTrue(labels.get(1).getText().length() > 0,
                    "卡片 " + (i + 1) + " 机制说明不能为空");
        }
        // 已知关卡的机制说明（数据驱动，与 GoalSummary 一致）
        assertEquals("到达出口", findLabels(select.cards.get(0)).get(1).getText());
        assertEquals("到达出口并收集所有宝物", findLabels(select.cards.get(1)).get(1).getText());
        assertEquals("到达出口并覆盖所有机关", findLabels(select.cards.get(3)).get(1).getText());
    }

    @Test
    void initialFocusTargetIsFirstCard() throws Exception {
        LoadedSelect select = loadSelect();
        assertEquals(select.cards.get(0), select.controller.initialFocusTarget(),
                "初始焦点必须安排在第一关卡片");
    }

    @Test
    void initialFocusLandsOnFirstCardWhenWindowShows() throws Exception {
        LoadedSelect select = loadSelect();
        attachStage(select);
        runOnFx(select.controller::requestInitialFocus);
        awaitFocus(stage.getScene(), select.cards.get(0));
    }

    @Test
    void arrowKeysMoveFocusThroughCardsAndBack() throws Exception {
        LoadedSelect select = loadSelect();
        attachStage(select);
        runOnFx(select.controller::requestInitialFocus);
        awaitFocus(stage.getScene(), select.cards.get(0));

        // DOWN ×3：第1关 → 第4关
        fireKey(KeyCode.DOWN);
        awaitFocus(stage.getScene(), select.cards.get(1));
        fireKey(KeyCode.DOWN);
        awaitFocus(stage.getScene(), select.cards.get(2));
        fireKey(KeyCode.DOWN);
        awaitFocus(stage.getScene(), select.cards.get(3));

        // UP ×2：第4关 → 第2关
        fireKey(KeyCode.UP);
        awaitFocus(stage.getScene(), select.cards.get(2));
        fireKey(KeyCode.UP);
        awaitFocus(stage.getScene(), select.cards.get(1));

        // 首尾循环：第1关 UP → 返回按钮；返回按钮 DOWN → 第1关
        fireKey(KeyCode.UP);
        awaitFocus(stage.getScene(), select.cards.get(0));
        fireKey(KeyCode.UP);
        awaitFocus(stage.getScene(), select.backButton);
        fireKey(KeyCode.DOWN);
        awaitFocus(stage.getScene(), select.cards.get(0));

        // 第5关 DOWN → 返回按钮；返回按钮 DOWN → 第1关（循环回绕）
        fireKey(KeyCode.DOWN);
        fireKey(KeyCode.DOWN);
        fireKey(KeyCode.DOWN);
        fireKey(KeyCode.DOWN);
        awaitFocus(stage.getScene(), select.cards.get(4));
        fireKey(KeyCode.DOWN);
        awaitFocus(stage.getScene(), select.backButton);
        fireKey(KeyCode.DOWN);
        awaitFocus(stage.getScene(), select.cards.get(0));
    }

    @Test
    void tabCyclesCardsAndBackButton() throws Exception {
        LoadedSelect select = loadSelect();
        attachStage(select);
        runOnFx(select.controller::requestInitialFocus);
        awaitFocus(stage.getScene(), select.cards.get(0));

        // Tab ×6：完整循环一轮（5 卡 + 返回）
        Node[] order = {select.cards.get(1), select.cards.get(2), select.cards.get(3),
                select.cards.get(4), select.backButton, select.cards.get(0)};
        for (Node expected : order) {
            fireKey(KeyCode.TAB);
            awaitFocus(stage.getScene(), (Button) expected);
        }
    }

    @Test
    void enterTriggersFocusedCardAction() throws Exception {
        LoadedSelect select = loadSelect();
        attachStage(select);
        runOnFx(select.controller::requestInitialFocus);
        awaitFocus(stage.getScene(), select.cards.get(0));
        // 替换为安全的记录动作（避免触发真实导航加载游戏场景）
        AtomicReference<Boolean> clicked = new AtomicReference<>(false);
        runOnFx(() -> select.cards.get(0).setOnAction(e -> clicked.set(true)));
        fireKey(KeyCode.ENTER);
        awaitFxPulse();
        assertEquals(Boolean.TRUE, clicked.get(), "Enter 必须触发聚焦卡片的 action");
    }

    // ---- 工具 ----

    private record LoadedSelect(Parent root, LevelSelectController controller,
                                List<Button> cards, Button backButton) {
    }

    private LoadedSelect loadSelect() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                LevelSelectKeyboardTest.class.getClassLoader().getResource(SceneNavigator.FXML_LEVEL_SELECT));
        Parent root = loader.load();
        LevelSelectController controller = loader.getController();
        // ScrollPane 内容不在 lookup 的 CSS 树中，按样式类递归查找卡片容器
        VBox cardsBox = findFirst(root, n -> n instanceof VBox v
                && v.getStyleClass().contains("level-cards"));
        assertNotNull(cardsBox, "必须找到关卡卡片容器");
        List<Button> cards = new ArrayList<>();
        for (Node child : cardsBox.getChildren()) {
            cards.add((Button) child);
        }
        Button back = (Button) root.lookup("#backButton");
        return new LoadedSelect(root, controller, List.copyOf(cards), back);
    }

    /** 递归查找节点（含 ScrollPane content 内部）。 */
    private static <T extends Node> T findFirst(Node node, java.util.function.Predicate<Node> test) {
        if (test.test(node)) {
            @SuppressWarnings("unchecked")
            T found = (T) node;
            return found;
        }
        if (node instanceof javafx.scene.control.ScrollPane scrollPane && scrollPane.getContent() != null) {
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

    /** 创建窗口并把场景附加到控制器（setNavigator 依赖 scene 非空以挂键盘监听）。 */
    private void attachStage(LoadedSelect select) throws Exception {
        AtomicReference<Stage> ref = new AtomicReference<>();
        runOnFx(() -> {
            Stage s = new Stage();
            s.setScene(new Scene(select.root));
            select.controller.setNavigator(new SceneNavigator(s));
            s.show();
            ref.set(s);
        });
        stage = ref.get();
        awaitFxPulse();
    }

    /** 焦点对象可读描述：返回按钮直接标注；关卡卡片打印标题行文本。 */
    private static String describe(Object owner) {
        if (owner == null) {
            return "无焦点";
        }
        if (owner instanceof Button button) {
            if (button.getStyleClass().contains("secondary-button")) {
                return "返回按钮";
            }
            List<Label> labels = findLabels(button);
            return labels.isEmpty() ? "关卡卡片(无文字)" : "卡片[" + labels.get(0).getText() + "]";
        }
        return String.valueOf(owner);
    }

    private static ImageView findIcon(Button card) {
        return (ImageView) ((javafx.scene.layout.HBox) card.getGraphic()).getChildren().get(0);
    }

    private static List<Label> findLabels(Button card) {
        javafx.scene.layout.VBox textBox =
                (javafx.scene.layout.VBox) ((javafx.scene.layout.HBox) card.getGraphic()).getChildren().get(1);
        List<Label> labels = new ArrayList<>();
        for (Node child : textBox.getChildren()) {
            if (child instanceof Label label) {
                labels.add(label);
            }
        }
        return labels;
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

    private void fireKey(KeyCode code) throws Exception {
        // 事件发到 focusOwner（Button）才能触发 Enter/Space 默认行为；
        // Tab 遍历与 UP/DOWN 导航在 Scene 层处理，同样会收到冒泡事件
        runOnFx(() -> {
            Node target = stage.getScene().getFocusOwner() != null
                    ? stage.getScene().getFocusOwner() : stage.getScene().getRoot();
            target.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, code.getChar(), code.getName(),
                    code, false, false, false, false));
            target.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, code.getChar(), code.getName(),
                    code, false, false, false, false));
        });
    }

    private static void awaitFocus(Scene scene, Button expected) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        java.util.List<String> trace = new ArrayList<>();
        while (System.currentTimeMillis() < deadline) {
            boolean focused = queryOnFx(() -> scene.getFocusOwner() == expected);
            Object tracedOwner = queryOnFxValue(scene::getFocusOwner);
            trace.add(describe(tracedOwner) + "@" + System.identityHashCode(tracedOwner));
            if (focused) {
                awaitFxPulse();
                return;
            }
            Thread.sleep(40);
        }
        Object owner = queryOnFxValue(scene::getFocusOwner);
        assertTrue(false, "焦点必须落在预期控件，当前=" + owner + "（" + describe(owner) + "@"
                + System.identityHashCode(owner) + "） 预期=" + describe(expected) + "@"
                + System.identityHashCode(expected)
                + " 轮询轨迹=" + trace.stream().distinct().limit(8).toList());
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
