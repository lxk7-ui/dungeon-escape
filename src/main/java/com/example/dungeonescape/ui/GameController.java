package com.example.dungeonescape.ui;

import com.example.dungeonescape.model.Direction;
import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.Position;
import com.example.dungeonescape.model.entity.Entity;
import com.example.dungeonescape.model.entity.Treasure;
import com.example.dungeonescape.model.entity.Wall;
import com.example.dungeonescape.persistence.InvalidLevelException;
import com.example.dungeonescape.persistence.LevelLoader;
import com.example.dungeonescape.service.GameEngine;
import com.example.dungeonescape.service.GameEventListener;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 游戏主界面控制器。
 *
 * <p>职责仅限于界面展示与输入转发：从 classpath 加载关卡、创建 {@link GameEngine}、
 * 监听胜利事件、绘制地图与刷新状态栏；所有移动/开门/推石/拾取/目标判定规则
 * 均委托给后端服务，本类不复制任何游戏规则。
 *
 * <p>布局：地图经“ScrollPane → 外层居中容器 {@link #mapOuterPane} → 棋盘框
 * {@link #boardFrame} → GridPane”分层展示；外层容器的最小尺寸被绑定到滚动区视口
 * （{@link #bindCenteringViewport}），因此小地图在剩余区域内水平垂直居中，
 * 大地图撑开容器后由 ScrollPane 滚动，格子始终可达且不被拉伸。
 *
 * <p>键盘：WASD / 方向键移动，R 重新开始，Esc 返回选关。移动后整个 GridPane 重绘并刷新状态。
 * 胜利后弹出一次模态 Alert（含关卡名与步数；非末关可 下一关/重新开始/选择关卡，
 * 末关不提供“下一关”并显示“恭喜完成全部关卡”），每次开始新一局都重建引擎，
 * 避免监听器累积导致重复弹窗。
 */
public class GameController {

    @FXML
    private javafx.scene.layout.StackPane rootPane;
    @FXML
    private Label levelNameLabel;
    @FXML
    private Label goalLabel;
    @FXML
    private Label stepsLabel;
    @FXML
    private Label treasureLabel;
    @FXML
    private Label keyLabel;
    @FXML
    private ScrollPane mapScrollPane;
    @FXML
    private StackPane mapOuterPane;
    @FXML
    private VBox boardFrame;
    @FXML
    private GridPane mapGridPane;
    @FXML
    private Button restartButton;
    @FXML
    private Button levelSelectButton;
    @FXML
    private Button mainMenuButton;

    private final LevelLoader levelLoader = new LevelLoader();

    private SceneNavigator navigator;
    private GameEngine engine;
    private GameState state;
    private String levelResourcePath;
    private int initialTreasureTotal;
    private boolean victoryShown;
    /** 稳定的键盘事件处理器（幂等绑定：同一 controller 只挂一次，避免 restart 时累积）。 */
    private javafx.event.EventHandler<KeyEvent> keyHandler;

    /** 胜利监听器：经 FX 线程展示一次胜利弹窗。 */
    private final GameEventListener victoryListener = gameState -> Platform.runLater(this::showVictory);

    @FXML
    private void initialize() {
        // 底部操作按钮 focusTraversable=true：Tab/Shift+Tab 循环、Enter/Space 触发；
        // 方向键/WASD 由场景级 capture 过滤器接管（见 startLevel），
        // 焦点落在按钮上时移动输入仍然有效，按钮不会被键盘抢走游戏操作
        // 不拉伸内容：棋盘保持 48px 格子与像素画清晰度，大地图由滚动条访问
        mapScrollPane.setFitToWidth(false);
        mapScrollPane.setFitToHeight(false);
        // 棋盘框按内容尺寸收缩：外层 StackPane 默认会拉伸可调整尺寸子节点，
        // 必须用 max=USE_PREF_SIZE 约束阻止，居中才由外层容器负责
        boardFrame.setMaxWidth(Region.USE_PREF_SIZE);
        boardFrame.setMaxHeight(Region.USE_PREF_SIZE);
        // 外层容器最小尺寸跟随视口：视口大时容器填满视口（小地图水平垂直居中），
        // 视口小于棋盘时容器保持棋盘尺寸（ScrollPane 按需出现滚动条，格子全部可达）
        bindCenteringViewport(mapScrollPane.viewportBoundsProperty(), mapOuterPane);
    }

    /**
     * 将外层居中容器的最小尺寸绑定到滚动区视口。
     *
     * <p>ScrollPane 对可调整尺寸内容按 {@code max(min, pref)} 布局且默认贴视口左上角；
     * 通过把 min 尺寸绑定到视口尺寸，实现：视口大于棋盘时外层容器撑满视口、
     * 棋盘在剩余区域内水平垂直居中；视口小于棋盘时外层容器至少为视口大小、
     * 内容按自身尺寸布局并触发滚动条。fitToWidth/fitToHeight 必须保持 false，
     * 否则内容会被拉伸，破坏 48px 格子与像素画清晰度。
     *
     * @param viewportBounds 滚动区视口尺寸（如 {@link ScrollPane#viewportBoundsProperty()}）
     * @param outer          外层居中容器（如 game.fxml 中的 {@code mapOuterPane}）
     */
    static void bindCenteringViewport(ObservableValue<Bounds> viewportBounds, Region outer) {
        outer.minWidthProperty().bind(viewportBounds.map(Bounds::getWidth));
        outer.minHeightProperty().bind(viewportBounds.map(Bounds::getHeight));
    }

    /** 由 {@link SceneNavigator} 在加载完 FXML 后注入。 */
    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    /**
     * 从 classpath 加载选中关卡并开始新的一局。
     *
     * <p>每次调用都新建 {@link GameState} 与 {@link GameEngine}（旧引擎随之丢弃），
     * 因此监听器不会跨局累积；加载失败时弹出错误 Alert 并停留在当前界面。
     *
     * @param resourcePath 关卡 JSON 的 classpath 路径
     */
    public void startLevel(String resourcePath) {
        GameState loaded;
        try {
            loaded = levelLoader.loadFromClasspath(resourcePath);
        } catch (InvalidLevelException e) {
            showError("关卡加载失败", e.getMessage());
            return;
        }
        // 加载成功后才记录路径：失败时保留上一局的路径，重开仍回到当前显示的关卡
        this.levelResourcePath = resourcePath;
        this.state = loaded;
        this.engine = new GameEngine(loaded);
        this.victoryShown = false;
        this.initialTreasureTotal = loaded.countEntities(Treasure.class) + loaded.getPlayer().getTreasureCount();
        engine.addEventListener(victoryListener);
        // 键盘监听挂在场景 capture 阶段：无论焦点落在何处（含底部按钮聚焦时），
        // 移动/重开/选关都生效，且先于按钮原生方向键遍历消费事件，避免双跳。
        // 幂等绑定：startLevel 会被 restartLevel 反复调用，若每次 addEventFilter 会
        // 在事件派发期间累积过滤器（R 键处理中再注册新过滤器，JavaFX 派发链遍历
        // 新增项 → 指数级处理 → FX 线程卡死/Not Responding/画面空白）。
        // 故保存稳定的 EventHandler 实例，绑定前先移除再添加，同一 controller/Scene 恒只挂一个。
        if (keyHandler == null) {
            keyHandler = this::onKeyPressed;
        }
        Scene scene = loadedScene(rootPane);
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        refreshHeader();
        renderMap();
        focusGameArea();
    }

    private Scene loadedScene(javafx.scene.layout.StackPane root) {
        Scene scene = root.getScene();
        if (scene == null) {
            throw new IllegalStateException("游戏场景尚未附加到窗口，无法绑定键盘监听");
        }
        return scene;
    }

    /** 键盘输入：WASD / 方向键移动，R 重新开始，Esc 返回选关。 */
    private void onKeyPressed(KeyEvent event) {
        Direction direction = switch (event.getCode()) {
            case W, UP -> Direction.UP;
            case S, DOWN -> Direction.DOWN;
            case A, LEFT -> Direction.LEFT;
            case D, RIGHT -> Direction.RIGHT;
            default -> null;
        };
        if (direction != null) {
            move(direction);
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.R) {
            restartLevel();
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            navigator.showLevelSelect();
            event.consume();
        }
    }

    /** 移动一步（规则全部委托给引擎），随后整图重绘并刷新状态。 */
    private void move(Direction direction) {
        if (engine == null) {
            return;
        }
        engine.move(direction);
        renderMap();
        refreshHeader();
    }

    /** 重绘整个 GridPane（每格：地形背景 + 按层排序的非玩家实体 + 最上层的玩家）。 */
    private void renderMap() {
        mapGridPane.getChildren().clear();
        int width = state.getWidth();
        int height = state.getHeight();
        Position playerPosition = state.getPlayer().getPosition();
        // 连贯地形图集可用时，墙体由地形层砖块绘制（实体 Wall 不再重复叠加）
        boolean terrainWalls = TerrainTexture.isAvailable();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                StackPane cell = new StackPane();
                cell.setMinSize(EntityViewFactory.CELL_SIZE, EntityViewFactory.CELL_SIZE);
                cell.setPrefSize(EntityViewFactory.CELL_SIZE, EntityViewFactory.CELL_SIZE);
                cell.setMaxSize(EntityViewFactory.CELL_SIZE, EntityViewFactory.CELL_SIZE);
                cell.getStyleClass().add("map-cell");
                // 背景与空格同样绘制：满格地形（地面/直路/转角/墙体），相邻格边缘连续
                cell.getChildren().add(EntityViewFactory.createCellBackground(x, y, state));
                Position position = new Position(x, y);
                List<Entity> entities = state.entitiesAt(position).stream()
                        .sorted(Comparator.comparingInt(EntityViewFactory::layerOf))
                        .toList();
                for (Entity entity : entities) {
                    if (terrainWalls && entity instanceof Wall) {
                        continue;
                    }
                    cell.getChildren().add(EntityViewFactory.createEntityView(entity, state));
                }
                if (playerPosition.equals(position)) {
                    cell.getChildren().add(EntityViewFactory.createEntityView(state.getPlayer(), state));
                }
                mapGridPane.add(cell, x, y);
            }
        }
        mapGridPane.requestLayout();
    }

    /** 刷新顶部关卡名、动态目标描述与状态区（步数 / 宝物 / 钥匙）。 */
    private void refreshHeader() {
        levelNameLabel.setText(state.getLevelName());
        goalLabel.setText(state.getGoal().description(state));
        stepsLabel.setText(String.valueOf(state.getMoveCount()));
        int collected = Math.max(0, initialTreasureTotal - state.countEntities(Treasure.class));
        treasureLabel.setText(collected + "/" + initialTreasureTotal);
        keyLabel.setText(state.getPlayer().hasKey() ? String.valueOf(state.getPlayer().getKey()) : "无");
    }

    /**
     * 胜利弹窗：仅弹一次（victoryShown 防重复），按 {@link VictoryDialog} 配置提供动作。
     *
     * <p>构建与样式化委托给 {@link VictoryDialogView#createAlert}：DialogPane 加
     * victory-dialog 样式并显式加载 game.css，绿色传送门裁剪图作为 graphic，
     * 下一关按钮加 next-level-action（深绿渐变/金边/发光），重新开始与选择关卡为
     * victory-secondary 次级样式。非末关提供 下一关 / 重新开始 / 选择关卡，其中
     * “下一关”是默认动作（OK_DONE），点击后经 {@link SceneNavigator#showGame(String)}
     * 直接进入下一关并重建场景与引擎；末关或无法识别当前关卡时配置不含“下一关”按钮，
     * 标题明确显示“恭喜完成全部关卡”。重新开始仍重载当前关，选择关卡仍回选关。
     * {@code showAndWait()} 的返回值只经 {@link VictoryActionDispatcher#dispatch} 分派，
     * 本类不直接写任何按键分支；直接关闭对话框（null）时不做任何动作，停留在胜利画面。
     */
    private void showVictory() {
        if (state == null || victoryShown) {
            return;
        }
        victoryShown = true;
        engine.removeEventListener(victoryListener);

        VictoryDialog.Config config = VictoryDialog.configFor(
                state.getLevelName(), state.getMoveCount(),
                LevelCatalog.nextAfterResourcePath(levelResourcePath));

        VictoryDialogView.Prepared prepared = VictoryDialogView.createAlert(config);
        prepared.alert().initOwner(navigator.getStage());
        Optional<ButtonType> result = prepared.alert().showAndWait();
        VictoryActionDispatcher dispatcher = new VictoryActionDispatcher(
                prepared, config, navigator::showGame, this::restartLevel, navigator::showLevelSelect);
        dispatcher.dispatch(result.orElse(null));
        // 直接关闭对话框：停留在胜利画面，仍可经底部按钮重新开始 / 返回
    }

    /** 重新开始当前关卡（重新加载 JSON 并重建引擎）。 */
    private void restartLevel() {
        if (levelResourcePath == null) {
            return;
        }
        startLevel(levelResourcePath);
        focusGameArea();
    }

    @FXML
    private void onRestartClick() {
        restartLevel();
    }

    @FXML
    private void onLevelSelectClick() {
        navigator.showLevelSelect();
    }

    @FXML
    private void onMainMenuClick() {
        navigator.showMainMenu();
    }

    /** 按钮点击后把键盘焦点还给游戏区域，保证键盘操作连续可用。 */
    private void focusGameArea() {
        rootPane.requestFocus();
    }

    /** 生产错误提示统一走暗色石板 ErrorDialogView（标题 / 摘要 / 可换行详情 / 确认按钮）。 */
    private void showError(String header, String message) {
        ErrorDialogView.Prepared prepared = ErrorDialogView.createAlert("错误", header, message);
        if (navigator != null) {
            prepared.alert().initOwner(navigator.getStage());
        }
        prepared.alert().showAndWait();
    }
}
