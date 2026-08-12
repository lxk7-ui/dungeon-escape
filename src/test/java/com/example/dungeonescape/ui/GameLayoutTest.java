package com.example.dungeonescape.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 游戏关卡界面响应式布局测试。
 *
 * <p>验证 game.fxml 中的居中包装结构与关键 fit 属性（纯 XML 解析，不启动 JavaFX Toolkit）：
 * <pre>
 * ScrollPane #mapScrollPane（fitToWidth/fitToHeight=false，不拉伸 48px 格子）
 * └── StackPane #mapOuterPane（外层居中容器，min 尺寸绑定视口 → 小地图居中）
 *     └── VBox #boardFrame（按内容收缩的棋盘框）
 *         └── GridPane #mapGridPane
 * </pre>
 * 另验证 {@link GameController#bindCenteringViewport} 的绑定语义，以及 FXML 能连同控制器
 * 一起真实加载（加载后外层容器最小尺寸确实处于绑定状态）。
 */
class GameLayoutTest {

    private static final String FX_NAMESPACE = "http://javafx.com/fxml";

    /**
     * 加载 FXML 会实例化 Control（Button/Label/ScrollPane），其类初始化需要 JavaFX
     * Toolkit；本机有真实显示，直接启动 Toolkit。其余测试仅用 Region/属性，不依赖。
     *
     * <p>Toolkit 与同套件其他 JavaFX 测试类共享：启动幂等（已启动时
     * {@link Platform#startup} 抛 IllegalStateException，属预期），且套件结束前
     * 不调用 {@link Platform#exit}（退出后无法再启动，会破坏共享同一个 JVM 的
     * 后续测试类，如 {@code VictoryDialogViewTest}），由 JVM 结束时统一回收。
     */
    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // 其他测试类已启动 Toolkit：幂等，无需重复启动
        }
    }

    // ---- 纯 XML 结构校验（不启动 JavaFX Toolkit） ----

    @Test
    void fxmlHasCenteringWrapperStructure() throws Exception {
        Element scrollPane = elementByFxId(parseGameFxml(), "mapScrollPane");
        assertEquals("ScrollPane", scrollPane.getLocalName());

        // ScrollPane 直接子节点必须是外层居中容器 StackPane（styleClass 含 map-outer）
        Element outer = firstChildElement(scrollPane);
        assertNotNull(outer, "ScrollPane 必须包裹外层居中容器");
        assertEquals("StackPane", outer.getLocalName());
        assertEquals("mapOuterPane", fxId(outer));
        assertTrue(hasStyleClass(outer, "map-outer"), "外层容器应使用 map-outer 样式类");

        // 外层容器内是按内容尺寸收缩的棋盘框 VBox（styleClass 含 board-frame）
        Element frame = firstChildElement(outer);
        assertNotNull(frame, "外层容器必须包裹棋盘框");
        assertEquals("VBox", frame.getLocalName());
        assertEquals("boardFrame", fxId(frame));
        assertTrue(hasStyleClass(frame, "board-frame"), "棋盘框应使用 board-frame 样式类");

        // 棋盘框内才是地图 GridPane
        Element grid = firstChildElement(frame);
        assertNotNull(grid, "棋盘框必须包裹地图 GridPane");
        assertEquals("GridPane", grid.getLocalName());
        assertEquals("mapGridPane", fxId(grid));
    }

    @Test
    void fxmlKeepsFitAndScrollbarFlags() throws Exception {
        Element scrollPane = elementByFxId(parseGameFxml(), "mapScrollPane");
        // 关键 fit 属性：false 保证内容不被拉伸，48px 格子与像素画清晰度不变
        assertEquals("false", scrollPane.getAttribute("fitToWidth"), "fitToWidth 必须为 false");
        assertEquals("false", scrollPane.getAttribute("fitToHeight"), "fitToHeight 必须为 false");
        // 水平方向任何关卡（最大 9×48+16=448px）均能容纳，永不出现水平滚动条；
        // 垂直方向按需滚动（仅 800×600 等小窗出现深色滚动条）
        assertEquals("NEVER", scrollPane.getAttribute("hbarPolicy"), "hbarPolicy 必须为 NEVER（默认无水平滚动条）");
        assertEquals("AS_NEEDED", scrollPane.getAttribute("vbarPolicy"));
    }

    @Test
    void headerAndFooterAreCentered() throws Exception {
        Document doc = parseGameFxml();
        // 根为 StackPane（石砖外壳），其下才是 BorderPane；BorderPane 的
        // top/center/bottom 是属性元素，其下才是实际布局容器
        Element borderPane = firstChildElement(doc.getDocumentElement(), "BorderPane");
        assertNotNull(borderPane, "根 StackPane 必须包含 BorderPane 外壳");
        assertEquals("game-shell", borderPane.getAttribute("styleClass"), "外壳必须带 game-shell 样式类");
        Element top = firstChildElement(borderPane, "top");
        Element header = firstChildElement(top);
        assertNotNull(header, "BorderPane top 必须声明头部 VBox");
        assertEquals("VBox", header.getLocalName());
        // 顶部整体（关卡名 / 目标 / 状态栏）在 VBox 中水平居中
        assertEquals("CENTER", header.getAttribute("alignment"), "头部 VBox 必须 alignment=CENTER");

        // 关卡名撑满整行并文本居中；目标 Label 限宽 720 且居中（长目标防溢出）
        Element levelName = elementByFxId(doc, "levelNameLabel");
        assertEquals("Infinity", levelName.getAttribute("maxWidth"), "levelNameLabel 必须 maxWidth=Infinity 才能居中");
        assertEquals("CENTER", levelName.getAttribute("textAlignment"), "levelNameLabel 必须 textAlignment=CENTER");
        Element goal = elementByFxId(doc, "goalLabel");
        assertEquals("720", goal.getAttribute("maxWidth"), "goalLabel 必须限宽 720");
        assertEquals("CENTER", goal.getAttribute("textAlignment"), "goalLabel 必须 textAlignment=CENTER");

        // 底部按钮栏整体居中
        Element bottom = firstChildElement(borderPane, "bottom");
        Element footer = firstChildElement(bottom);
        assertNotNull(footer, "BorderPane bottom 必须声明底部按钮栏");
        assertEquals("HBox", footer.getLocalName());
        assertEquals("game-footer", footer.getAttribute("styleClass"));
        assertEquals("CENTER", footer.getAttribute("alignment"), "底部按钮栏必须 alignment=CENTER");
    }

    // ---- 绑定语义校验（不启动 JavaFX Toolkit） ----

    @Test
    void bindCenteringViewportTracksViewportSize() {
        SimpleObjectProperty<Bounds> viewport = new SimpleObjectProperty<>(new BoundingBox(0, 0, 0, 0));
        StackPane outer = new StackPane();
        GameController.bindCenteringViewport(viewport, outer);

        // 视口大于棋盘：外层容器 min 尺寸 = 视口尺寸 → 撑满视口使小地图居中
        viewport.set(new BoundingBox(0, 0, 900, 500));
        assertEquals(900.0, outer.getMinWidth(), "视口变大时外层容器 min 宽应跟随视口");
        assertEquals(500.0, outer.getMinHeight(), "视口变大时外层容器 min 高应跟随视口");

        // 视口小于棋盘：外层容器 min 尺寸 = 视口尺寸（小于棋盘 pref）→ ScrollPane 保持内容
        // 自身尺寸并出现滚动条，格子不被裁切
        viewport.set(new BoundingBox(0, 0, 300, 200));
        assertEquals(300.0, outer.getMinWidth(), "视口变小时外层容器 min 宽应跟随视口");
        assertEquals(200.0, outer.getMinHeight(), "视口变小时外层容器 min 高应跟随视口");
    }

    @Test
    void boardFrameKeepsContentSizeWhileOuterFollowsViewport() {
        // 棋盘框按内容收缩：GridPane 为 8×6 关（每格 48px）时，棋盘框 pref 尺寸 = 384×288
        GridPane grid = new GridPane();
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 8; x++) {
                StackPane cell = new StackPane();
                cell.setMinSize(48, 48);
                cell.setPrefSize(48, 48);
                cell.setMaxSize(48, 48);
                grid.add(cell, x, y);
            }
        }
        VBox frame = new VBox(grid);
        // 棋盘框内边距 8px（与 game.css .board-frame 一致）计入尺寸
        frame.setPadding(new javafx.geometry.Insets(8));
        assertEquals(384.0 + 16, frame.prefWidth(-1), 0.001, "棋盘框 pref 宽 = 地图宽 + 内边距");
        assertEquals(288.0 + 16, frame.prefHeight(-1), 0.001, "棋盘框 pref 高 = 地图高 + 内边距");

        // 外层容器 min 绑定视口、pref 仍按棋盘收缩
        SimpleObjectProperty<Bounds> viewport = new SimpleObjectProperty<>(new BoundingBox(0, 0, 700, 400));
        StackPane outer = new StackPane(frame);
        GameController.bindCenteringViewport(viewport, outer);
        assertEquals(700.0, outer.getMinWidth());
        assertEquals(400.0, outer.getMinHeight());
        assertEquals(400.0, outer.prefWidth(-1), 0.001, "外层容器 pref 宽应等于棋盘框尺寸");
    }

    // ---- FXML 真实加载校验（控制器 initialize 完成注入与绑定，不启动 Toolkit） ----

    @Test
    void gameFxmlLoadsWithControllerAndWiresBinding() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(GameLayoutTest.class.getClassLoader().getResource(SceneNavigator.FXML_GAME));
        Parent root = loader.load();
        assertNotNull(root);

        assertTrue(loader.getController() instanceof GameController, "FXML 应加载出 GameController");

        // 场景图结构：StackPane(dungeon-root) → BorderPane(game-shell)；
        // BorderPane 中央：ScrollPane → StackPane(map-outer) → VBox(board-frame) → GridPane
        assertTrue(root instanceof StackPane, "游戏根必须是 StackPane（石砖外壳）");
        assertTrue(root.getStyleClass().contains("dungeon-root"), "游戏根必须共享地牢外壳样式");
        assertEquals(2, root.getChildrenUnmodifiable().size(), "根必须含背景层与 BorderPane 外壳");
        javafx.scene.layout.BorderPane borderPane =
                (javafx.scene.layout.BorderPane) root.getChildrenUnmodifiable().get(1);
        assertTrue(borderPane.getStyleClass().contains("game-shell"), "外壳应带 game-shell 样式类");
        ScrollPane scrollPane = (ScrollPane) borderPane.getCenter();
        assertNotNull(scrollPane, "BorderPane 中央必须是 ScrollPane");
        assertFalse(scrollPane.isFitToWidth(), "加载后 fitToWidth 必须为 false");
        assertFalse(scrollPane.isFitToHeight(), "加载后 fitToHeight 必须为 false");

        Region outer = (Region) scrollPane.getContent();
        assertNotNull(outer, "ScrollPane 内容必须是外层居中容器");
        assertTrue(outer instanceof StackPane, "外层容器必须是 StackPane");
        assertTrue(outer.getStyleClass().contains("map-outer"), "外层容器应带 map-outer 样式类");
        // 控制器 initialize() 已把外层容器最小尺寸绑定到视口
        assertTrue(outer.minWidthProperty().isBound(), "外层容器 min 宽必须已绑定视口");
        assertTrue(outer.minHeightProperty().isBound(), "外层容器 min 高必须已绑定视口");

        VBox frame = (VBox) ((StackPane) outer).getChildren().get(0);
        assertTrue(frame.getStyleClass().contains("board-frame"), "棋盘框应带 board-frame 样式类");
        // 棋盘框按内容尺寸收缩：max 约束为 USE_PREF_SIZE，不被外层 StackPane 拉伸
        assertEquals(Region.USE_PREF_SIZE, frame.getMaxWidth(), "棋盘框 max 宽必须为 USE_PREF_SIZE");
        assertEquals(Region.USE_PREF_SIZE, frame.getMaxHeight(), "棋盘框 max 高必须为 USE_PREF_SIZE");
        assertEquals(1, frame.getChildren().size());
        assertSame(GridPane.class, frame.getChildren().get(0).getClass());

        // 目标 Label：真实加载后 alignment 必须为 Pos.CENTER（“到达出口”在标签内水平垂直居中），
        // wrapText 与 textAlignment 保持不变；长目标限宽 720 防溢出
        VBox header = (VBox) borderPane.getTop();
        Label goalLabel = (Label) header.getChildren().get(1);
        assertEquals(Pos.CENTER, goalLabel.getAlignment(), "goalLabel 必须 alignment=CENTER");
        assertTrue(goalLabel.isWrapText(), "goalLabel 必须保持 wrapText=true");
        assertEquals(TextAlignment.CENTER, goalLabel.getTextAlignment(), "goalLabel 必须保持 textAlignment=CENTER");
        assertEquals(720.0, goalLabel.getMaxWidth(), "goalLabel 必须限宽 720（Level 5 长目标防溢出）");

        // 键盘焦点：底部操作按钮 focusTraversable=true（Tab/Shift+Tab 循环、Enter/Space 触发）；
        // 游戏方向键由场景级 capture 过滤器接管，焦点在按钮上时移动输入仍生效
        javafx.scene.layout.HBox footer = (javafx.scene.layout.HBox) borderPane.getBottom();
        assertEquals(3, footer.getChildren().size());
        for (javafx.scene.Node button : footer.getChildren()) {
            assertTrue(((javafx.scene.control.Button) button).isFocusTraversable(), "底部按钮必须可 Tab 聚焦");
        }
    }

    // ---- 工具方法 ----

    private static Document parseGameFxml() throws Exception {
        try (InputStream in = GameLayoutTest.class.getClassLoader()
                .getResourceAsStream(SceneNavigator.FXML_GAME)) {
            assertNotNull(in, "game.fxml 必须存在：" + SceneNavigator.FXML_GAME);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(in);
        }
    }

    private static Element elementByFxId(Document doc, String id) {
        NodeList nodes = doc.getElementsByTagNameNS("*", "*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && id.equals(fxId(element))) {
                return element;
            }
        }
        throw new AssertionError("FXML 中未找到 fx:id=" + id);
    }

    private static String fxId(Element element) {
        String id = element.getAttributeNS(FX_NAMESPACE, "id");
        return id.isEmpty() ? element.getAttribute("fx:id") : id;
    }

    private static Element firstChildElement(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element element) {
                return element;
            }
        }
        return null;
    }

    private static Element firstChildElement(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element element && localName.equals(element.getLocalName())) {
                return element;
            }
        }
        throw new AssertionError("未找到子元素：" + localName);
    }

    private static boolean hasStyleClass(Element element, String styleClass) {
        Set<String> classes = new HashSet<>(Arrays.asList(element.getAttribute("styleClass").split("\\s+")));
        return classes.contains(styleClass);
    }
}
