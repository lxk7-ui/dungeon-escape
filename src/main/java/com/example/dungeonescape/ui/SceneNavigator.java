package com.example.dungeonescape.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * 场景导航器：持有 {@link Stage}，集中加载 FXML 并切换 main-menu / level-select / game 三个场景。
 *
 * <p>所有资源均通过 classpath 相对路径定位（如 "com/example/dungeonescape/fxml/main-menu.fxml"），
 * 不使用任何绝对路径；FXML/CSS 缺失或加载失败时抛出携带资源路径与原因、信息明确的
 * {@link IllegalStateException}（由 {@code DungeonEscapeApplication} 捕获后展示 Alert）。
 */
public final class SceneNavigator {

    /** 主菜单 FXML 的 classpath 路径。 */
    public static final String FXML_MAIN_MENU = "com/example/dungeonescape/fxml/main-menu.fxml";
    /** 选关界面 FXML 的 classpath 路径。 */
    public static final String FXML_LEVEL_SELECT = "com/example/dungeonescape/fxml/level-select.fxml";
    /** 游戏界面 FXML 的 classpath 路径。 */
    public static final String FXML_GAME = "com/example/dungeonescape/fxml/game.fxml";
    /** 全局样式表 game.css 的 classpath 路径。 */
    public static final String CSS_GAME = "com/example/dungeonescape/css/game.css";
    /** 主菜单/选关页地牢视觉基础样式表 dungeon-ui.css 的 classpath 路径。 */
    public static final String CSS_DUNGEON_UI = "com/example/dungeonescape/css/dungeon-ui.css";

    private static final double SCENE_WIDTH = 960;
    private static final double SCENE_HEIGHT = 640;

    private final Stage stage;

    /**
     * @param stage 应用主窗口（本类不会自行调用 show，由 Application 控制）
     */
    public SceneNavigator(Stage stage) {
        this.stage = Objects.requireNonNull(stage, "stage");
    }

    /** 返回持有窗口。 */
    public Stage getStage() {
        return stage;
    }

    /** 切换到主菜单场景。 */
    public void showMainMenu() {
        MainMenuController controller = loadController(FXML_MAIN_MENU);
        controller.setNavigator(this);
    }

    /** 切换到选关场景。 */
    public void showLevelSelect() {
        LevelSelectController controller = loadController(FXML_LEVEL_SELECT);
        controller.setNavigator(this);
    }

    /**
     * 切换到游戏场景并开始指定关卡。
     *
     * @param levelResourcePath 关卡 JSON 的 classpath 路径（由 {@link LevelCatalog} 提供）
     * @throws IllegalArgumentException 资源路径为空白
     */
    public void showGame(String levelResourcePath) {
        if (levelResourcePath == null || levelResourcePath.isBlank()) {
            throw new IllegalArgumentException("关卡资源路径不能为空白");
        }
        GameController controller = loadController(FXML_GAME);
        controller.setNavigator(this);
        controller.startLevel(levelResourcePath);
    }

    /** 加载指定 FXML、应用全局样式并切换场景，返回该场景的控制器。 */
    private <T> T loadController(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(resourceUrl(fxmlPath));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("FXML 加载失败：" + fxmlPath + "（" + e.getMessage() + "）", e);
        }
        Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
        scene.getStylesheets().add(resourceUrl(CSS_GAME).toExternalForm());
        scene.getStylesheets().add(resourceUrl(CSS_DUNGEON_UI).toExternalForm());
        stage.setScene(scene);
        Object controller = loader.getController();
        if (controller instanceof FocusableScreen focusableScreen) {
            focusableScreen.requestInitialFocus();
        }
        return (T) controller;
    }

    /**
     * 定位 classpath 资源并返回 URL；资源不存在时抛出明确的
     * {@link IllegalStateException}（不依赖工作目录与绝对路径）。
     */
    static URL resourceUrl(String path) {
        Objects.requireNonNull(path, "path");
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        URL url = SceneNavigator.class.getClassLoader().getResource(normalized);
        if (url == null) {
            throw new IllegalStateException("classpath 资源不存在：" + path);
        }
        return url;
    }
}
