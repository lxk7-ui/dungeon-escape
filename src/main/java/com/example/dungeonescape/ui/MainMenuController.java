package com.example.dungeonescape.ui;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

/**
 * 主菜单控制器：开始游戏（进入选关）与退出。
 *
 * <p>键盘可达性：两个按钮均为 focusTraversable（可 Tab/Shift+Tab 循环），
 * 进入页面后初始焦点落在“开始游戏”，Enter/Space 触发聚焦按钮。
 */
public class MainMenuController implements FocusableScreen {

    @FXML
    private Button startButton;
    @FXML
    private Button exitButton;
    @FXML
    private Region menuDivider;

    private SceneNavigator navigator;

    @FXML
    private void initialize() {
        // 标题下装饰分隔条：真实砖墙素材平铺（与背景石砖同源）
        DungeonTheme.applyBrickStrip(menuDivider);
        // 键盘焦点由 FXML/CSS 全权管理：按钮 focusTraversable=true，
        // 初始焦点由 FocusableScreen#requestInitialFocus 安排
    }

    /** 由 {@link SceneNavigator} 在加载完 FXML 后注入。 */
    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public Node initialFocusTarget() {
        return startButton;
    }

    /** 开始游戏 → 选关界面。 */
    @FXML
    private void onStartClick() {
        navigator.showLevelSelect();
    }

    /** 退出游戏：关闭主窗口。 */
    @FXML
    private void onExitClick() {
        navigator.getStage().close();
    }
}
