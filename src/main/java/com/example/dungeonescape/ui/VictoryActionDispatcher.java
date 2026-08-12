package com.example.dungeonescape.ui;

import javafx.scene.control.ButtonType;

import java.util.function.Consumer;

/**
 * 胜利弹窗动作分发器（纯逻辑，不触碰窗口，可单元测试）。
 *
 * <p>把 {@link GameController#showVictory()} 中 {@code Alert.showAndWait()} 返回值
 * 的按键分发剥离出来：根据用户选中的 {@link ButtonType} 调用对应回调，每个动作
 * 只调用自己的回调，不产生任何多余动作。
 *
 * <p>分发规则：
 * <ul>
 *   <li>选中“下一关”（即 {@link Prepared#nextLevel()}，仅非末关存在）：只调用
 *       {@code showGame.accept(next.resourcePath())}，目标来自配置中的
 *       {@link LevelCatalog.LevelEntry}；</li>
 *   <li>选中“重新开始”：只调用 {@code restart.run()}；</li>
 *   <li>选中“选择关卡”：只调用 {@code levelSelect.run()}；</li>
 *   <li>null、未知 {@link ButtonType}、以及末关/无下一关（nextLevel 为 null、
 *       {@code Config.nextLevel()} 为空）时不做任何动作。</li>
 * </ul>
 */
final class VictoryActionDispatcher {

    private final VictoryDialogView.Prepared prepared;
    private final VictoryDialog.Config config;
    private final Consumer<String> showGame;
    private final Runnable restart;
    private final Runnable levelSelect;

    VictoryActionDispatcher(VictoryDialogView.Prepared prepared, VictoryDialog.Config config,
                            Consumer<String> showGame, Runnable restart, Runnable levelSelect) {
        this.prepared = prepared;
        this.config = config;
        this.showGame = showGame;
        this.restart = restart;
        this.levelSelect = levelSelect;
    }

    /**
     * 按用户点击的按钮分派动作。
     *
     * <p>用身份相等（==）匹配 {@link Prepared} 中的按钮类型：只有真实点击了弹窗
     * 提供的按钮才会命中，未知类型一律忽略。末关时 {@code prepared.nextLevel()} 为
     * null，任何非空按钮都不会命中“下一关”分支，配合 {@code config.nextLevel()} 为空，
     * 保证末关不会跳转。
     *
     * @param selected {@code showAndWait()} 的返回值；直接关闭对话框时为 null
     */
    void dispatch(ButtonType selected) {
        if (selected == null) {
            return;
        }
        if (selected == prepared.nextLevel()) {
            config.nextLevel().ifPresent(next -> showGame.accept(next.resourcePath()));
        } else if (selected == prepared.restart()) {
            restart.run();
        } else if (selected == prepared.levelSelect()) {
            levelSelect.run();
        }
    }
}
