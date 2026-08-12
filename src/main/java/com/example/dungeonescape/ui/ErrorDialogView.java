package com.example.dungeonescape.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.io.InputStream;

/**
 * 错误对话框视图：统一暗色石板风格（与胜利对话框同一视觉体系），
 * 取代默认 Modena 白底 Alert。
 *
 * <p>{@link #createAlert(String, String, String)} 完成：
 * <ul>
 *   <li>DialogPane 加 {@code error-dialog} 样式类，显式加载 game.css 与 dungeon-ui.css
 *       （对话框使用独立场景，场景级样式表不会自动到达，必须自行加载）；</li>
 *   <li>标题 / 简洁错误摘要（header）/ 可换行详情（content）；</li>
 *   <li>错误徽记：classpath 真实位图 {@link #ERROR_MARK_PATH}（从视觉参考离线裁切的
 *       红色八角徽记），ImageView 等比清晰显示；素材缺失/解码失败时不显示图标
 *       （绝不降级为文字/Unicode/CSS 假图标）；</li>
 *   <li>“确定”确认按钮使用 {@code danger-button} 暗红金属危险态（红色边框体系统一），
 *       并设为默认按钮（打开即获得键盘焦点，Enter/Space 可触发），focused 金色描边可见；</li>
 *   <li>错误信息原样保留（不吞异常、不截断文案）。</li>
 * </ul>
 */
public final class ErrorDialogView {

    /** 弹窗 DialogPane 样式类（game.css 中 .error-dialog）。 */
    public static final String STYLE_ERROR_DIALOG = "error-dialog";
    /** 确认按钮文案。 */
    public static final String CONFIRM_TEXT = "确定";
    /** 确认按钮危险态样式类（game.css 中 .danger-button）。 */
    public static final String STYLE_DANGER_BUTTON = "danger-button";
    /** 错误徽记 classpath 路径（真实位图，离线从视觉参考裁切）。 */
    public static final String ERROR_MARK_PATH = "com/example/dungeonescape/images/error-mark.png";
    /** 徽记显示宽度（视觉权重 ≈ 64-72px，保持像素清晰）。 */
    public static final double ERROR_MARK_WIDTH = 72.0;

    /** 已样式化的错误弹窗：Alert 及其确认按钮类型。 */
    public record Prepared(Alert alert, ButtonType confirm) {
    }

    private ErrorDialogView() {
    }

    /**
     * 构建并样式化错误弹窗。
     *
     * @param title   窗口标题
     * @param header  简洁错误摘要（显示在标题区）
     * @param detail  可换行的详细信息（原错误信息，不截断）
     * @return 样式化完成的 Alert 与确认按钮类型；调用方负责 initOwner 与 showAndWait
     */
    public static Prepared createAlert(String title, String header, String detail) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        // 清除 AlertType.ERROR 自带 ButtonType.OK，只保留自建“确定”按钮
        alert.getButtonTypes().clear();
        alert.setTitle(title);
        DialogPane pane = alert.getDialogPane();
        pane.setHeaderText(header);
        pane.setContentText(detail);

        pane.getStyleClass().add(STYLE_ERROR_DIALOG);
        // 对话框使用独立场景：显式加载共享样式表，样式才能生效
        pane.getStylesheets().add(SceneNavigator.resourceUrl(SceneNavigator.CSS_GAME).toExternalForm());
        pane.getStylesheets().add(SceneNavigator.resourceUrl(SceneNavigator.CSS_DUNGEON_UI).toExternalForm());

        // 错误徽记：真实位图 ImageView（替代 Modena 默认 ERROR 图形与任何文字假图标）
        ImageView mark = loadErrorMark();
        if (mark != null) {
            pane.setGraphic(mark);
        }

        ButtonType confirm = new ButtonType(CONFIRM_TEXT, ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().add(confirm);
        Button button = (Button) pane.lookupButton(confirm);
        button.getStyleClass().add(STYLE_DANGER_BUTTON);
        // 默认按钮：打开即聚焦，Enter/Space 可触发（键盘焦点明显）
        button.setDefaultButton(true);

        return new Prepared(alert, confirm);
    }

    /**
     * 从 classpath 加载错误徽记位图（等比、关闭平滑保持像素清晰）。
     *
     * <p>素材缺失或解码失败时返回 null（调用方不设置 graphic，绝不降级为文字图标）。
     *
     * @return 徽记 ImageView；不可用时为 null
     */
    static ImageView loadErrorMark() {
        try (InputStream in = ErrorDialogView.class.getResourceAsStream('/' + ERROR_MARK_PATH)) {
            if (in == null) {
                return null;
            }
            Image image = new Image(in);
            if (image.isError()) {
                return null;
            }
            ImageView view = new ImageView(image);
            view.setFitWidth(ERROR_MARK_WIDTH);
            view.setPreserveRatio(true);
            view.setSmooth(false);
            return view;
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }
}
