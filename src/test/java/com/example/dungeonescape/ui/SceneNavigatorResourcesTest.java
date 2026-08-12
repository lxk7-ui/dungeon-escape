package com.example.dungeonescape.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SceneNavigator 资源存在性测试：FXML/CSS 可经 classpath 定位、
 * 每个 FXML 声明的 fx:controller 与期望控制器匹配、文件为 UTF-8 编码。
 *
 * <p>纯资源校验，不启动 JavaFX Toolkit / 不启动完整 Application。
 */
class SceneNavigatorResourcesTest {

    private static InputStream resource(String path) {
        return SceneNavigatorResourcesTest.class.getClassLoader().getResourceAsStream(path);
    }

    @Test
    void allFxmlResourcesResolveOnClasspath() {
        assertNotNull(resource(SceneNavigator.FXML_MAIN_MENU), "主菜单 FXML 必须存在：" + SceneNavigator.FXML_MAIN_MENU);
        assertNotNull(resource(SceneNavigator.FXML_LEVEL_SELECT), "选关 FXML 必须存在：" + SceneNavigator.FXML_LEVEL_SELECT);
        assertNotNull(resource(SceneNavigator.FXML_GAME), "游戏 FXML 必须存在：" + SceneNavigator.FXML_GAME);
    }

    @Test
    void cssResourceResolvesOnClasspath() {
        assertNotNull(resource(SceneNavigator.CSS_GAME), "样式表必须存在：" + SceneNavigator.CSS_GAME);
    }

    @Test
    void eachFxmlDeclaresItsExpectedController() throws IOException {
        assertEquals("com.example.dungeonescape.ui.MainMenuController",
                declaredController(SceneNavigator.FXML_MAIN_MENU));
        assertEquals("com.example.dungeonescape.ui.LevelSelectController",
                declaredController(SceneNavigator.FXML_LEVEL_SELECT));
        assertEquals("com.example.dungeonescape.ui.GameController",
                declaredController(SceneNavigator.FXML_GAME));
    }

    @Test
    void fxmlFilesAreUtf8Encoded() throws IOException {
        // 以 UTF-8 严格解码后应能读回中文文案，说明资源未被转成其他编码
        String mainMenu = readUtf8(SceneNavigator.FXML_MAIN_MENU);
        assertTrue(mainMenu.contains("开始游戏"), "主菜单 FXML 应包含中文文案");
        assertTrue(mainMenu.contains("退出游戏"), "主菜单 FXML 应包含中文文案");
        String levelSelect = readUtf8(SceneNavigator.FXML_LEVEL_SELECT);
        assertTrue(levelSelect.contains("选择关卡"), "选关 FXML 应包含中文文案");
        assertTrue(levelSelect.contains("返回主菜单"), "选关 FXML 应包含中文文案");
        String game = readUtf8(SceneNavigator.FXML_GAME);
        assertTrue(game.contains("重新开始"), "游戏 FXML 应包含中文文案");
        assertTrue(game.contains("主菜单"), "游戏 FXML 应包含中文文案");
    }

    /** 读取 FXML 中声明的 fx:controller 全限定类名。 */
    private static String declaredController(String path) throws IOException {
        String content = readUtf8(path);
        Matcher matcher = Pattern.compile("fx:controller=\"([^\"]+)\"").matcher(content);
        assertTrue(matcher.find(), "FXML 未声明 fx:controller：" + path);
        return matcher.group(1);
    }

    private static String readUtf8(String path) throws IOException {
        try (InputStream in = resource(path)) {
            assertNotNull(in, "classpath 资源不存在：" + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
