package com.example.dungeonescape.ui;

import com.example.dungeonescape.persistence.EntityDefinition;
import com.example.dungeonescape.persistence.GoalDefinition;
import com.example.dungeonescape.persistence.LevelDefinition;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 地牢视觉基础资源测试：色板 token、图标 viewport 有效性、真实素材可加载、
 * 背景层次结构、样式表存在（含 focused/hover/pressed 规则）、
 * 关卡图标选择与卡片图标来源。
 *
 * <p>纯几何/文本断言不依赖 Toolkit；涉及 Image 解析与节点构建的用例
 * 复用共享 Toolkit（与 GameLayoutTest 同一模式，启动幂等）。
 */
class DungeonThemeResourceTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // 其他测试类已启动 Toolkit：幂等，无需重复启动
        }
    }

    @Test
    void paletteTokensAreDefined() {
        assertTrue(DungeonTheme.COLOR_BG_BASE.startsWith("#"), "底色必须是十六进制色");
        assertTrue(DungeonTheme.COLOR_GOLD.startsWith("#"));
        assertTrue(DungeonTheme.COLOR_PRIMARY.startsWith("#"));
        assertTrue(DungeonTheme.COLOR_SECONDARY.startsWith("#"));
        assertTrue(DungeonTheme.COLOR_PANEL.startsWith("#"));
        assertTrue(DungeonTheme.COLOR_BORDER.startsWith("#"));
        assertTrue(DungeonTheme.COLOR_TEXT.startsWith("#"));
        assertTrue(DungeonTheme.COLOR_TEXT_MUTED.startsWith("#"));
    }

    @Test
    void iconViewportsLieInsideSpritesheetAndHaveSubstance() {
        // 图集 1448×1086；每个图标必须落在图集内且宽高 ≥ 100px（紧贴内容而非空区域）
        for (Rectangle2D vp : List.of(DungeonTheme.ICON_EXIT, DungeonTheme.ICON_TREASURE,
                DungeonTheme.ICON_KEY, DungeonTheme.ICON_BOULDER,
                DungeonTheme.ICON_SWITCH, DungeonTheme.ICON_DOOR)) {
            assertTrue(vp.getMinX() >= 0 && vp.getMinY() >= 0, "图标起点必须在图集内");
            assertTrue(vp.getMaxX() <= 1448 && vp.getMaxY() <= 1086,
                    "图标终点必须在图集内：" + vp);
            assertTrue(vp.getWidth() >= 100 && vp.getHeight() >= 100,
                    "图标必须有实际内容（≥100px）：" + vp);
        }
    }

    @Test
    void realSpritesheetIsLoadable() {
        assertTrue(DungeonTheme.spritesheetAvailable(), "实体图集必须可经 classpath 加载");
    }

    @Test
    void createIconProducesRealImageView() {
        javafx.scene.image.ImageView icon = DungeonTheme.createIcon(DungeonTheme.ICON_KEY, 44);
        assertNotNull(icon.getImage(), "图标必须引用真实图集 Image");
        assertSame(icon.getImage(), DungeonTheme.createIcon(DungeonTheme.ICON_KEY, 44).getImage(),
                "图标应共享同一缓存图集");
        assertEquals(44.0, icon.getFitWidth());
        assertEquals(44.0, icon.getFitHeight());
        assertTrue(icon.isPreserveRatio());
    }

    @Test
    void backgroundHasTextureBrickBorderDimGradientVignetteAndDecor() {
        StackPane background = DungeonTheme.createBackground();
        List<String> classes = background.getStyleClass();
        assertTrue(classes.contains(DungeonTheme.STYLE_BG_LAYER), "背景必须带 dungeon-bg-layer 类");
        // 自底向上：纹理 / 石砖边框 / 暗化 / 渐变 / 暗角 / 门与出口装饰
        assertTrue(background.getChildren().size() >= 7, "背景至少含 7 个子层");
        boolean hasTexture = background.getChildren().stream()
                .anyMatch(n -> n.getStyleClass().contains(DungeonTheme.STYLE_BG_TEXTURE));
        boolean hasBorder = background.getChildren().stream()
                .anyMatch(n -> n.getStyleClass().contains(DungeonTheme.STYLE_BG_BORDER));
        boolean hasDim = background.getChildren().stream()
                .anyMatch(n -> n.getStyleClass().contains(DungeonTheme.STYLE_BG_DIM));
        boolean hasGradient = background.getChildren().stream()
                .anyMatch(n -> n.getStyleClass().contains(DungeonTheme.STYLE_BG_GRADIENT));
        boolean hasVignette = background.getChildren().stream()
                .anyMatch(n -> n.getStyleClass().contains(DungeonTheme.STYLE_BG_VIGNETTE));
        assertTrue(hasTexture, "必须包含真实素材纹理层");
        assertTrue(hasBorder, "必须包含石砖边框层（与地图墙美术同源）");
        assertTrue(hasDim, "必须包含暗化层");
        assertTrue(hasGradient, "必须包含上下渐变层");
        assertTrue(hasVignette, "必须包含暗角层");
        // 装饰：门与出口为真实 sprite，直接作为背景子层（底边与石砖边框相接）
        long sprites = background.getChildrenUnmodifiable().stream()
                .filter(ImageView.class::isInstance)
                .count();
        assertEquals(2, sprites, "背景必须含门与出口两个真实 sprite 装饰");
    }

    @Test
    void brickAndGroundUnitsAreSubstantial() {
        // 石砖/地面平铺单元必须位于地形图集内且有实际内容
        assertTrue(DungeonTheme.WALL_BRICK_VIEWPORT.getMinX() >= 0
                        && DungeonTheme.WALL_BRICK_VIEWPORT.getMaxX() <= 1672
                        && DungeonTheme.WALL_BRICK_VIEWPORT.getMaxY() <= 941,
                "石砖单元必须在连贯地形图集内");
        assertEquals(64.0, DungeonTheme.WALL_BRICK_VIEWPORT.getWidth());
        assertEquals(64.0, DungeonTheme.WALL_BRICK_VIEWPORT.getHeight());
        assertEquals(128.0, DungeonTheme.GROUND_PATCH_VIEWPORT.getWidth());
        assertEquals(128.0, DungeonTheme.GROUND_PATCH_VIEWPORT.getHeight());
        assertEquals(64.0, DungeonTheme.BRICK_BORDER, "石砖边框厚度必须为 64px");
    }

    @Test
    void dungeonUiCssIsOnClasspathWithFocusStates() throws Exception {
        try (InputStream in = DungeonThemeResourceTest.class.getClassLoader()
                .getResourceAsStream(SceneNavigator.CSS_DUNGEON_UI)) {
            assertNotNull(in, "dungeon-ui.css 必须存在");
            String css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String rule : new String[]{".primary-button:focused", ".secondary-button:focused",
                    ".level-card:focused", ".primary-button:hover", ".secondary-button:hover",
                    ".level-card:hover", ".primary-button:pressed", ".secondary-button:pressed",
                    ".level-card:pressed"}) {
                assertTrue(css.contains(rule), "样式表必须包含规则：" + rule);
            }
            assertTrue(css.contains("#ffd76a"), "焦点金色描边必须存在（金色 token）");
        }
    }

    @Test
    void levelSelectFxmlUsesRealBackgroundAndScrollPane() throws Exception {
        // 纯 XML 断言：选关页用同一美术体系（DungeonBackground 组件 + 深色 ScrollPane + 返回按钮）
        String fxml = readResource(SceneNavigator.FXML_LEVEL_SELECT);
        assertTrue(fxml.contains("DungeonBackground"), "选关页必须复用 DungeonBackground 视觉基础");
        assertTrue(fxml.contains("ScrollPane"), "选关页必须提供 ScrollPane（小窗口滚动）");
        assertTrue(fxml.contains("backButton"), "选关页必须声明返回按钮");
        String mainMenu = readResource(SceneNavigator.FXML_MAIN_MENU);
        assertTrue(mainMenu.contains("DungeonBackground"), "主菜单必须复用 DungeonBackground 视觉基础");
    }

    @Test
    void cardIconSelectionIsDataDriven() {
        // 含钥匙 → 钥匙图标
        assertEquals(DungeonTheme.ICON_KEY, LevelSelectCard.iconFor(levelWith("key")));
        // 无钥匙但含宝物 → 宝物图标
        assertEquals(DungeonTheme.ICON_TREASURE, LevelSelectCard.iconFor(levelWith("treasure")));
        // 无钥匙/宝物但含机关或巨石 → 巨石图标
        assertEquals(DungeonTheme.ICON_BOULDER, LevelSelectCard.iconFor(levelWith("switch")));
        assertEquals(DungeonTheme.ICON_BOULDER, LevelSelectCard.iconFor(levelWith("boulder")));
        // 仅出口 → 出口图标
        assertEquals(DungeonTheme.ICON_EXIT, LevelSelectCard.iconFor(levelWith("exit")));
    }

    @Test
    void levelJsonMetadataLoadsForAllFiveLevels() {
        for (LevelCatalog.LevelEntry entry : LevelCatalog.LEVELS) {
            LevelDefinition def = new com.example.dungeonescape.persistence.LevelLoader()
                    .loadDefinitionFromClasspath(entry.resourcePath());
            assertNotNull(def, "关卡 " + entry.number() + " 定义必须可加载");
            assertNotNull(def.name());
            assertNotNull(def.goal());
            assertTrue(GoalSummary.of(def.goal()).length() > 0,
                    "关卡 " + entry.number() + " 必须能推导机制说明");
        }
    }

    @Test
    void levelSelectFxmlHasScrollPaneNode() throws Exception {
        // 真实验证 FXML 能加载出 ScrollPane（与 GameLayoutTest 同模式）
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                DungeonThemeResourceTest.class.getClassLoader().getResource(SceneNavigator.FXML_LEVEL_SELECT));
        Node root = loader.load();
        assertTrue(root instanceof StackPane);
        assertTrue(findScrollPane((StackPane) root) != null, "选关页场景图必须包含 ScrollPane");
    }

    private static ScrollPane findScrollPane(StackPane root) {
        return root.getChildrenUnmodifiable().stream()
                .map(DungeonThemeResourceTest::findScrollPaneIn)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static ScrollPane findScrollPaneIn(Node node) {
        if (node instanceof ScrollPane scrollPane) {
            return scrollPane;
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                ScrollPane found = findScrollPaneIn(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 构造只含指定类型实体的关卡定义（供图标选择测试）。 */
    private static LevelDefinition levelWith(String entityType) {
        EntityDefinition entity = new EntityDefinition("e1", entityType, 1, 1, null, null, false);
        return new LevelDefinition("测试", 5, 5, List.of(entity),
                new GoalDefinition("EXIT", null));
    }

    private static String readResource(String path) throws Exception {
        try (InputStream in = DungeonThemeResourceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "classpath 资源不存在：" + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
