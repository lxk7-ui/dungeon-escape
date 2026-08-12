package com.example.dungeonescape.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import java.util.EnumMap;
import java.util.Map;

/**
 * 胜利弹窗视图：把 {@link GameController#showVictory()} 中 Alert 的构建与样式化
 * 剥离为可测试方法（不显示、不阻塞、不触碰窗口）。
 *
 * <p>{@link #createAlert(VictoryDialog.Config)} 按配置完成：
 * <ul>
 *   <li>DialogPane 加 {@code victory-dialog} 样式类，并显式加载现有 game.css
 *       （对话框使用独立场景，场景级样式表不会自动到达，必须自行加载）；</li>
 *   <li>graphic 为绿色传送门/魔法光效裁剪图（{@link VictoryArt#createPortalGraphic()}，
 *       ImageView + viewport）；素材不可用时自动使用其内置占位图，任何情况都有 graphic；</li>
 *   <li>“下一关”按钮加 {@code next-level-action} 样式类，其余动作加
 *       {@code victory-secondary} 样式类。</li>
 * </ul>
 *
 * <p>末关/无法识别下一关时配置不含 NEXT_LEVEL 动作，自然不产生“下一关”按钮；
 * 返回的 {@link Prepared#nextLevel()} 为 null，控制器永不命中下一关分支。
 */
public final class VictoryDialogView {

    /** 弹窗 DialogPane 样式类（game.css 中 .victory-dialog）。 */
    public static final String STYLE_VICTORY_DIALOG = "victory-dialog";
    /** “下一关”主按钮样式类（game.css 中 .next-level-action）。 */
    public static final String STYLE_NEXT_LEVEL = "next-level-action";
    /** “重新开始/选择关卡”次级按钮样式类（game.css 中 .victory-secondary）。 */
    public static final String STYLE_SECONDARY = "victory-secondary";

    /** 已样式化的胜利弹窗：Alert 及其各动作按钮的 ButtonType（末关时 nextLevel 为 null）。 */
    public record Prepared(Alert alert, ButtonType nextLevel, ButtonType restart,
                           ButtonType levelSelect) {
    }

    private VictoryDialogView() {
    }

    /**
     * 构建并样式化胜利弹窗。
     *
     * @param config 由 {@link VictoryDialog#configFor} 计算的展示配置
     * @return 样式化完成的 Alert 与各动作按钮类型；调用方负责 initOwner 与 showAndWait
     */
    public static Prepared createAlert(VictoryDialog.Config config) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        // AlertType.INFORMATION 构造自带 ButtonType.OK（“确定”），必须清除，
        // 最终按钮只来自下方 Config actions：非末关 3 个、末关 2 个，绝无默认“确定”
        alert.getButtonTypes().clear();
        alert.setTitle(VictoryDialog.TITLE);
        DialogPane pane = alert.getDialogPane();
        pane.setHeaderText(config.headerText());
        pane.setContentText(config.contentText());

        pane.getStyleClass().add(STYLE_VICTORY_DIALOG);
        // 对话框使用独立场景：显式加载共享样式表（game.css 的 victory-dialog 规则
        // 与 dungeon-ui.css 的石砖控件基础），样式才能生效
        pane.getStylesheets().add(SceneNavigator.resourceUrl(SceneNavigator.CSS_GAME).toExternalForm());
        pane.getStylesheets().add(SceneNavigator.resourceUrl(SceneNavigator.CSS_DUNGEON_UI).toExternalForm());

        // 弹窗 graphic 用 VictoryArt 的传送门裁剪图（Image 单例缓存 + 新 ImageView）；
        // 素材不可用时其内置占位图兜底，任何情况都设置 graphic
        pane.setGraphic(VictoryArt.createPortalGraphic());

        Map<VictoryDialog.ActionKind, ButtonType> byKind = new EnumMap<>(VictoryDialog.ActionKind.class);
        for (VictoryDialog.Action action : config.actions()) {
            ButtonType type = new ButtonType(action.text(), action.buttonData());
            pane.getButtonTypes().add(type);
            byKind.put(action.kind(), type);
            // lookupButton 会按需创建并接线按钮；末关没有 NEXT_LEVEL 动作，不会产生下一关按钮
            Button button = (Button) pane.lookupButton(type);
            button.getStyleClass().add(action.kind() == VictoryDialog.ActionKind.NEXT_LEVEL
                    ? STYLE_NEXT_LEVEL : STYLE_SECONDARY);
        }

        return new Prepared(alert,
                byKind.get(VictoryDialog.ActionKind.NEXT_LEVEL),
                byKind.get(VictoryDialog.ActionKind.RESTART),
                byKind.get(VictoryDialog.ActionKind.LEVEL_SELECT));
    }
}
