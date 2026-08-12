package com.example.dungeonescape.ui;

import javafx.scene.control.ButtonBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 胜利弹窗的展示配置（纯数据，不启动 JavaFX Toolkit，可单元测试）。
 *
 * <p>把“弹窗显示什么文本、提供哪些动作按钮”从 {@link GameController#showVictory()}
 * 阻塞的 {@code Alert.showAndWait()} 流程中剥离出来：控制器依据配置创建 Alert 并分发动作，
 * 测试只校验配置本身，无需触碰窗口。
 *
 * <p>动作规则：非末关提供 下一关 / 重新开始 / 选择关卡 三个动作，其中“下一关”使用
 * {@link ButtonBar.ButtonData#OK_DONE}（Alert 中呈现为最醒目的默认按钮，回车可触发）；
 * 末关或无法识别当前关卡时不加入“下一关”按钮，标题改为“恭喜完成全部关卡”。
 */
final class VictoryDialog {

    /** 弹窗标题。 */
    static final String TITLE = "胜利";
    /** 尚有下一关时的标题文本。 */
    static final String HEADER_HAS_NEXT = "恭喜通关！";
    /** 已通关全部关卡（或无法识别下一关）时的标题文本：明确告知全部通关。 */
    static final String HEADER_ALL_COMPLETE = "恭喜完成全部关卡";
    /** 动作按钮文案（纯文字，不使用 Unicode/emoji 符号冒充图标）。 */
    static final String TEXT_NEXT = "下一关";
    static final String TEXT_RESTART = "重新开始";
    static final String TEXT_SELECT = "选择关卡";

    /** 动作种类：{@link GameController} 据此分发导航。 */
    enum ActionKind { NEXT_LEVEL, RESTART, LEVEL_SELECT }

    /** 单个动作按钮的配置。 */
    record Action(String text, ButtonBar.ButtonData buttonData, ActionKind kind) {
    }

    /** 弹窗展示配置（纯数据）。 */
    record Config(String headerText, String contentText, List<Action> actions,
                  Optional<LevelCatalog.LevelEntry> nextLevel) {

        /** 是否包含“下一关”动作。 */
        boolean hasNextLevel() {
            return actions.stream().anyMatch(action -> action.kind() == ActionKind.NEXT_LEVEL);
        }
    }

    private VictoryDialog() {
    }

    /**
     * 由当前关卡信息与下一关元数据计算弹窗配置。
     *
     * @param levelName 当前关卡名称
     * @param moveCount 通关步数
     * @param nextLevel 下一关元数据（须来自 {@link LevelCatalog}）；末关或无下一关时为 empty
     */
    static Config configFor(String levelName, int moveCount, Optional<LevelCatalog.LevelEntry> nextLevel) {
        List<Action> actions = new ArrayList<>(3);
        if (nextLevel.isPresent()) {
            actions.add(new Action(TEXT_NEXT, ButtonBar.ButtonData.OK_DONE, ActionKind.NEXT_LEVEL));
        }
        actions.add(new Action(TEXT_RESTART, ButtonBar.ButtonData.LEFT, ActionKind.RESTART));
        actions.add(new Action(TEXT_SELECT, ButtonBar.ButtonData.CANCEL_CLOSE, ActionKind.LEVEL_SELECT));
        String headerText = nextLevel.isPresent() ? HEADER_HAS_NEXT : HEADER_ALL_COMPLETE;
        String contentText = "关卡：" + levelName + "\n步数：" + moveCount;
        return new Config(headerText, contentText, List.copyOf(actions), nextLevel);
    }
}
