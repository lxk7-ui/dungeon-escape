package com.example.dungeonescape.ui;

import javafx.scene.layout.StackPane;

/**
 * 地牢背景组件：铺满父容器的多层视觉背景（真实地形纹理平铺 + 暗化 +
 * 上下渐变 + 暗角 + 底部真实 sprite 装饰），构建细节见
 * {@link DungeonTheme#createBackground()}。
 *
 * <p>以独立组件类承载，FXML 可直接实例化（替代 fx:factory 方案），
 * 便于测试与样式定位。
 */
public final class DungeonBackground extends StackPane {

    /** 组件样式类（测试与 CSS 定位）。 */
    public static final String STYLE_CLASS = "dungeon-bg";

    /** 构建完整背景层。 */
    public DungeonBackground() {
        getStyleClass().add(STYLE_CLASS);
        getChildren().add(DungeonTheme.createBackground());
        // 背景层不拦截鼠标（点击背景让出焦点，聚焦态由按钮自身管理）
        setMouseTransparent(true);
    }
}
