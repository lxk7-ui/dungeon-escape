package com.example.dungeonescape.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

/**
 * 可聚焦界面约定：进入页面后将初始焦点落到指定控件（键盘可达性的基础）。
 *
 * <p>由 {@link SceneNavigator} 在切换场景后调用 {@link #requestInitialFocus()}：
 * 窗口已显示时直接排队请求焦点；窗口尚未显示（应用首次启动）时挂监听，
 * 在窗口 showing 后再请求。实现方只需提供 {@link #initialFocusTarget()}。
 */
public interface FocusableScreen {

    /** 初始焦点目标节点。 */
    Node initialFocusTarget();

    /**
     * 请求初始焦点：窗口已显示则立即（FX 线程）请求；否则等待窗口显示后请求。
     */
    default void requestInitialFocus() {
        Node target = initialFocusTarget();
        Scene scene = target.getScene();
        if (scene == null) {
            return;
        }
        Window window = scene.getWindow();
        if (window != null && window.isShowing()) {
            Platform.runLater(target::requestFocus);
        } else if (window != null) {
            window.showingProperty().addListener((obs, was, showing) -> {
                if (showing) {
                    Platform.runLater(target::requestFocus);
                }
            });
        } else {
            scene.windowProperty().addListener((obs, oldWindow, newWindow) -> {
                if (newWindow != null) {
                    newWindow.showingProperty().addListener((o2, was2, showing2) -> {
                        if (showing2) {
                            Platform.runLater(target::requestFocus);
                        }
                    });
                }
            });
        }
    }
}
