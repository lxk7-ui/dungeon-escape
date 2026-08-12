package com.example.dungeonescape.ui;

import com.example.dungeonescape.persistence.InvalidLevelException;
import com.example.dungeonescape.persistence.LevelDefinition;
import com.example.dungeonescape.persistence.LevelLoader;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * 选关界面控制器：为 5 个内置关卡生成带真实图标/编号/名称/机制说明的宽卡片，
 * 并支持返回主菜单。
 *
 * <p>键盘可达性：5 张关卡卡片 + 返回按钮均 focusTraversable；进入页面后初始焦点
 * 落在第一关卡片；Tab/Shift+Tab 在 6 个控件间循环；UP/DOWN 方向键在列表内移动
 * 焦点（纵向布局，首尾循环）。方向键在 capture 阶段接管：立即按逻辑顺序计算
 * 目标，并在事件分发完成（Button 原生方向键遍历/ScrollPane 滚动之后）用
 * runLater 强制落位——原生遍历在 ScrollPane 边缘的循环方向与列表逻辑不一致，
 * 最终焦点以本控制器计算为准；Enter/Space 触发聚焦卡片（Button 默认行为）。
 *
 * <p>关卡名称/机制说明运行时经 {@link LevelLoader} 读取 JSON 推导；单个关卡加载
 * 失败时弹出错误 Alert 并将该卡片置灰，程序继续运行。
 */
public class LevelSelectController implements FocusableScreen {

    @FXML
    private javafx.scene.layout.StackPane rootPane;
    @FXML
    private HBox levelIconStrip;
    @FXML
    private VBox levelCardsBox;
    @FXML
    private Button backButton;

    private final LevelLoader levelLoader = new LevelLoader();
    private final List<Button> levelCards = new ArrayList<>();
    /** 焦点导航顺序：5 张卡片 + 返回按钮。 */
    private final List<Node> focusOrder = new ArrayList<>();

    private SceneNavigator navigator;

    @FXML
    private void initialize() {
        // 标题上方的关卡主题小图标装饰行（真实图集 sprite，低透明度）
        for (LevelCatalog.LevelEntry entry : LevelCatalog.LEVELS) {
            javafx.scene.image.ImageView icon = DungeonTheme.createIcon(
                    iconFor(entry.number()), DungeonTheme.STRIP_ICON_SIZE);
            icon.setOpacity(0.7);
            levelIconStrip.getChildren().add(icon);
        }
        for (LevelCatalog.LevelEntry entry : LevelCatalog.LEVELS) {
            Button card = createLevelCard(entry);
            levelCards.add(card);
            levelCardsBox.getChildren().add(card);
        }
        focusOrder.addAll(levelCards);
        focusOrder.add(backButton);
    }

    /** 装饰行图标：与关卡卡片同一映射（1 出口 / 2 宝物 / 3 钥匙 / 4 巨石 / 5 宝物）。 */
    private javafx.geometry.Rectangle2D iconFor(int number) {
        return switch (number) {
            case 1 -> DungeonTheme.ICON_EXIT;
            case 2 -> DungeonTheme.ICON_TREASURE;
            case 3 -> DungeonTheme.ICON_KEY;
            case 4 -> DungeonTheme.ICON_BOULDER;
            default -> DungeonTheme.ICON_TREASURE;
        };
    }

    /** 由 {@link SceneNavigator} 在加载完 FXML 后注入（挂根节点级键盘过滤器）。 */
    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
        // capture 阶段过滤器挂在根节点：事件从 Scene 向下分发的必经节点，
        // 先于 ScrollPane 等控件的方向键行为拿到事件
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
    }

    @Override
    public Node initialFocusTarget() {
        return levelCards.isEmpty() ? backButton : levelCards.get(0);
    }

    /** 生成单个关卡卡片；加载失败时弹 Alert 并置灰该卡片。 */
    private Button createLevelCard(LevelCatalog.LevelEntry entry) {
        LevelDefinition def = loadLevel(entry.resourcePath());
        Button card = LevelSelectCard.create(entry, def);
        card.setOnAction(event -> navigator.showGame(entry.resourcePath()));
        return card;
    }

    /** 读取关卡定义（名称/实体/目标，供卡片展示）；失败时弹暗色错误框（程序继续），返回 null。 */
    private LevelDefinition loadLevel(String resourcePath) {
        try {
            return levelLoader.loadDefinitionFromClasspath(resourcePath);
        } catch (InvalidLevelException e) {
            ErrorDialogView.Prepared prepared = ErrorDialogView.createAlert(
                    "关卡加载失败", "无法加载关卡资源：" + resourcePath, e.getMessage());
            if (navigator != null) {
                prepared.alert().initOwner(navigator.getStage());
            }
            prepared.alert().showAndWait();
            return null;
        }
    }

    /**
     * 方向键导航：UP/DOWN 在焦点顺序列表内移动（纵向布局，首尾循环）。
     *
     * <p>在 capture 阶段计算目标并 consume 事件；真正落位推迟到 runLater——
     * Button 的原生方向键遍历与 ScrollPane 滚动会在事件分发期间执行，
     * 其边界循环方向与列表逻辑不一致，故以其结束后强制设置最终焦点。
     */
    private void onKeyPressed(KeyEvent event) {
        int delta = switch (event.getCode()) {
            case DOWN -> 1;
            case UP -> -1;
            default -> 0;
        };
        if (delta == 0) {
            return;
        }
        event.consume();
        Scene scene = levelCardsBox.getScene();
        Node owner = scene == null ? null : scene.getFocusOwner();
        int index = focusOrder.indexOf(owner);
        if (index < 0) {
            index = delta > 0 ? -1 : focusOrder.size();
        }
        int next = Math.floorMod(index + delta, focusOrder.size());
        Node target = focusOrder.get(next);
        Platform.runLater(() -> target.requestFocus());
    }

    /** 返回主菜单。 */
    @FXML
    private void onBackClick() {
        navigator.showMainMenu();
    }
}
