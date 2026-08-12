package com.example.dungeonescape;

import com.example.dungeonescape.ui.ErrorDialogView;
import com.example.dungeonescape.ui.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Dungeon Escape 应用程序入口。
 *
 * <p>启动后经 {@link SceneNavigator} 切换到主菜单场景；界面资源缺失或加载失败时，
 * 先弹出信息明确的错误 Alert，再抛出异常（便于尽早暴露资源路径问题）。
 */
public class DungeonEscapeApplication extends Application {

    /** 窗口标题（供 SmokeTest 校验的常量）。 */
    public static final String APP_TITLE = "Dungeon Escape";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle(APP_TITLE);
        // 最小窗口（逻辑像素）：默认 960×640 不受影响；允许缩到
        // 800×600 物理窗口（150% DPI 下约 533×400 逻辑）以验证响应式布局，
        // 游戏地图在极小窗口下由 ScrollPane 按需滚动
        stage.setMinWidth(520);
        stage.setMinHeight(380);
        try {
            new SceneNavigator(stage).showMainMenu();
            stage.show();
        } catch (RuntimeException e) {
            // 启动失败同样走暗色石板错误框；异常原样抛出（不吞）
            ErrorDialogView.Prepared prepared = ErrorDialogView.createAlert(
                    "启动失败", "无法加载界面资源", e.getMessage());
            prepared.alert().showAndWait();
            throw e;
        }
    }
}
