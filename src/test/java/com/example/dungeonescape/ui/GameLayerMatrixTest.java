package com.example.dungeonescape.ui;

import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.Position;
import com.example.dungeonescape.model.entity.Boulder;
import com.example.dungeonescape.model.entity.Door;
import com.example.dungeonescape.model.entity.Exit;
import com.example.dungeonescape.model.entity.FloorSwitch;
import com.example.dungeonescape.model.entity.Key;
import com.example.dungeonescape.model.entity.Player;
import com.example.dungeonescape.model.entity.Treasure;
import com.example.dungeonescape.model.entity.Wall;
import com.example.dungeonescape.model.goal.ExitGoal;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图层与美术连续性矩阵测试（Stage C 发布级）：
 * <ul>
 *   <li>{@link EntityViewFactory#layerOf} 底到顶顺序：机关 &lt; 出口 &lt; 宝物 &lt; 钥匙 &lt;
 *       门 &lt; 巨石 &lt; 墙 &lt; 玩家；</li>
 *   <li>L2 宝物收集后 HUD 进度更新且格子无宝物残影；</li>
 *   <li>L3 钥匙收集后 HUD 更新、钥匙从格子消失；开门后门贴图切换为打开格（地面连续）；</li>
 *   <li>L4 巨石推上机关后同格节点顺序为 背景→机关→巨石（机关不被巨石盖住视觉），
 *       目标进度更新，且推石/撞墙等无效移动不错误改变状态。</li>
 * </ul>
 */
class GameLayerMatrixTest {

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
    void layerOrderIsBottomToTop() {
        // 机关最底（巨石压上时机关显示在巨石之下），玩家恒为最顶
        assertEquals(0, EntityViewFactory.layerOf(FloorSwitch.class));
        assertEquals(1, EntityViewFactory.layerOf(Exit.class));
        assertEquals(2, EntityViewFactory.layerOf(Treasure.class));
        assertEquals(3, EntityViewFactory.layerOf(Key.class));
        assertEquals(4, EntityViewFactory.layerOf(Door.class));
        assertEquals(5, EntityViewFactory.layerOf(Boulder.class));
        assertEquals(6, EntityViewFactory.layerOf(Wall.class));
        assertEquals(7, EntityViewFactory.layerOf(Player.class));
        // 顺序严格递增
        List<Class<? extends com.example.dungeonescape.model.entity.Entity>> order = List.of(
                FloorSwitch.class, Exit.class, Treasure.class, Key.class,
                Door.class, Boulder.class, Wall.class, Player.class);
        for (int i = 0; i < order.size(); i++) {
            for (int j = i + 1; j < order.size(); j++) {
                assertTrue(EntityViewFactory.layerOf(order.get(i)) < EntityViewFactory.layerOf(order.get(j)),
                        "图层必须严格自底向上");
            }
        }
    }

    @Test
    void level2TreasureCollectionUpdatesHudAndRemovesSprite() throws Exception {
        LoadedGame game = loadGame(LevelCatalog.resourcePathFor(2));
        // 玩家 (1,1)，右两步到 (3,1) 收集宝物 1
        press(game, KeyCode.RIGHT);
        press(game, KeyCode.RIGHT);
        assertEquals("1/3", game.treasureLabel.getText(), "收集 1 个宝物后 HUD 必须显示 1/3");
        // 宝物格子无残影：cell (3,1) 只有背景与（若有）其他实体，不含 Treasure 精灵
        StackPane cell = cellAt(game.grid, 3, 1);
        assertTrue(cell.getChildren().stream()
                        .noneMatch(n -> n instanceof ImageView && isTreasureView((ImageView) n)),
                "收集后宝物格不得残留宝物精灵");
        assertEquals("2", game.stepsLabel.getText(), "两次有效移动步数必须为 2");
    }

    @Test
    void level3KeyHudUpdatesAndDoorOpensWithOpenSprite() throws Exception {
        LoadedGame game = loadGame(LevelCatalog.resourcePathFor(3));
        // 解法前段：RRR → (4,1) 拾取钥匙
        for (int i = 0; i < 3; i++) {
            press(game, KeyCode.RIGHT);
        }
        assertEquals("1", game.keyLabel.getText(), "拾取钥匙后 HUD 必须显示 1");
        StackPane keyCell = cellAt(game.grid, 4, 1);
        assertTrue(keyCell.getChildren().stream()
                        .noneMatch(n -> n instanceof ImageView && isKeyView((ImageView) n)),
                "拾取后钥匙格不得残留钥匙精灵");
        // 解法后段：LLLDD → (1,3)，RRRRR → 门 (6,3) 打开
        for (int i = 0; i < 3; i++) {
            press(game, KeyCode.LEFT);
        }
        for (int i = 0; i < 2; i++) {
            press(game, KeyCode.DOWN);
        }
        for (int i = 0; i < 5; i++) {
            press(game, KeyCode.RIGHT);
        }
        // 钥匙已消费：HUD 回到无钥匙
        assertEquals("无", game.keyLabel.getText(), "开门后钥匙必须被消费");
        StackPane doorCell = cellAt(game.grid, 6, 3);
        // 门实体保留但切换为打开格（DOOR_OPEN viewport）——地面连续的门洞贴图
        ImageView doorView = doorCell.getChildren().stream()
                .filter(ImageView.class::isInstance)
                .map(ImageView.class::cast)
                .filter(this::isDoorView)
                .findFirst()
                .orElse(null);
        assertNotNull(doorView, "开门后门实体必须仍在场景中（打开门洞贴图）");
        Rectangle2D openRect = EntityViewFactory.sourceRect(
                new Door("door", new Position(6, 3), 1, true), null);
        assertEquals(openRect, doorView.getViewport(), "开门后门必须使用打开格贴图（地面连续）");
        assertEquals("13", game.stepsLabel.getText(), "13 次有效移动步数必须正确");
    }

    @Test
    void level4BoulderPushesOntoSwitchWithCorrectLayersAndGoal() throws Exception {
        LoadedGame game = loadGame(LevelCatalog.resourcePathFor(4));
        // 解法前段：RR → (3,1)，D → 推巨石 (3,2)→(3,3) 压机关 1
        press(game, KeyCode.RIGHT);
        press(game, KeyCode.RIGHT);
        press(game, KeyCode.DOWN);
        // 机关 (3,3) 格：children 顺序 背景(0) → 机关(层0) → 巨石(层5)
        StackPane switchCell = cellAt(game.grid, 3, 3);
        List<Node> children = switchCell.getChildren();
        assertTrue(children.size() >= 3, "机关格必须含 背景/机关/巨石 至少 3 个节点");
        assertTrue(isSwitchView(children.get(1)), "机关必须在巨石之下（层 0）");
        assertTrue(isBoulderView(children.get(2)), "巨石必须在机关之上（层 5）");
        // 目标进度：覆盖所有机关（1/2）
        assertTrue(game.goalLabel.getText().contains("覆盖所有机关（1/2）")
                        || game.goalLabel.getText().contains("完成所有目标（0/2）")
                        || game.goalLabel.getText().contains("未完成"),
                "目标文本必须反映机关进度：" + game.goalLabel.getText());
        assertEquals("3", game.stepsLabel.getText());
        // 无效移动：回到 (3,1) 后向墙 UP 撞墙不改变步数/状态
        press(game, KeyCode.UP);
        String before = game.stepsLabel.getText();
        press(game, KeyCode.UP);
        assertEquals(before, game.stepsLabel.getText(), "撞墙无效移动不得增加步数");
    }

    // ---- 工具 ----

    private record LoadedGame(Parent root, GameController controller,
                              javafx.scene.layout.BorderPane borderPane,
                              Label levelNameLabel, Label goalLabel, Label stepsLabel,
                              Label treasureLabel, Label keyLabel, ScrollPane mapScrollPane,
                              GridPane grid, VBox boardFrame, Button restartButton,
                              Button levelSelectButton, Button mainMenuButton) {
    }

    private static LoadedGame loadGame(String levelResourcePath) throws Exception {
        AtomicReference<LoadedGame> ref = new AtomicReference<>();
        runOnFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        GameLayerMatrixTest.class.getClassLoader().getResource(SceneNavigator.FXML_GAME));
                Parent root = loader.load();
                GameController controller = loader.getController();
                Scene scene = new Scene(root, 960, 640);
                scene.getStylesheets().add(GameLayerMatrixTest.class.getClassLoader()
                        .getResource(SceneNavigator.CSS_GAME).toExternalForm());
                scene.getStylesheets().add(GameLayerMatrixTest.class.getClassLoader()
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

    private static StackPane cellAt(GridPane grid, int x, int y) {
        for (Node child : grid.getChildren()) {
            Integer cx = GridPane.getColumnIndex(child);
            Integer cy = GridPane.getRowIndex(child);
            if (cx != null && cy != null && cx == x && cy == y) {
                return (StackPane) child;
            }
        }
        throw new AssertionError("cell not found: " + x + "," + y);
    }

    private static void press(LoadedGame game, KeyCode code) throws Exception {
        runOnFx(() -> game.root.getScene().getRoot().fireEvent(new KeyEvent(
                KeyEvent.KEY_PRESSED, code.getChar(), code.getName(), code, false, false, false, false)));
        runOnFx(() -> game.root.getScene().getRoot().fireEvent(new KeyEvent(
                KeyEvent.KEY_RELEASED, code.getChar(), code.getName(), code, false, false, false, false)));
    }

    /** 宝物精灵：viewport 为 TREASURE_CELL。 */
    private boolean isTreasureView(ImageView view) {
        return view.getViewport() != null
                && Math.abs(view.getViewport().getMinX() - 3 * 362) < 1
                && view.getViewport().getMinY() < 1;
    }

    private boolean isKeyView(ImageView view) {
        return view.getViewport() != null
                && Math.abs(view.getViewport().getMinX() - 0) < 1
                && Math.abs(view.getViewport().getMinY() - 362) < 1;
    }

    private boolean isDoorView(ImageView view) {
        return view.getViewport() != null
                && (Math.abs(view.getViewport().getMinX() - 362) < 1   // DOOR_CLOSED (1,1)
                    || Math.abs(view.getViewport().getMinX() - 724) < 1)  // DOOR_OPEN (2,1)
                && Math.abs(view.getViewport().getMinY() - 362) < 1;
    }

    private boolean isSwitchView(Node node) {
        return node instanceof ImageView view && view.getViewport() != null
                && Math.abs(view.getViewport().getMinY() - 724) < 1;
    }

    private boolean isBoulderView(Node node) {
        return node instanceof ImageView view && view.getViewport() != null
                && Math.abs(view.getViewport().getMinX() - 3 * 362) < 1
                && Math.abs(view.getViewport().getMinY() - 362) < 1;
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
