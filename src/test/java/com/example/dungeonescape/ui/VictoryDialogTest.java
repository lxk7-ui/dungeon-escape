package com.example.dungeonescape.ui;

import javafx.scene.control.ButtonBar;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 胜利弹窗配置测试。
 *
 * <p>只校验 {@link VictoryDialog} 的纯数据配置：非末关包含且仅包含 下一关/重新开始/选择关卡
 * 三个动作且“下一关”为默认动作（OK_DONE）、末关不含“下一关”、显示文本正确、下一关资源
 * 来自 {@link LevelCatalog}。不启动 JavaFX Toolkit，也不调用阻塞的 showAndWait，无外部
 * mocking 依赖。
 */
class VictoryDialogTest {

    /** 关卡 1..4（非末关）：包含且仅包含 下一关/重新开始/选择关卡 三个动作。 */
    @Test
    void nonFinalLevelsContainExactlyTheThreeActions() {
        for (int level = 1; level <= 4; level++) {
            VictoryDialog.Config config = VictoryDialog.configFor("关卡" + level, 10,
                    LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(level)));
            List<String> texts = config.actions().stream().map(VictoryDialog.Action::text).toList();
            assertEquals(List.of("下一关", "重新开始", "选择关卡"), texts,
                    "关卡 " + level + " 应包含且仅包含 下一关/重新开始/选择关卡 三个动作（纯文字，无 Unicode 符号）");
            List<VictoryDialog.ActionKind> kinds = config.actions().stream()
                    .map(VictoryDialog.Action::kind).toList();
            assertEquals(List.of(VictoryDialog.ActionKind.NEXT_LEVEL,
                            VictoryDialog.ActionKind.RESTART,
                            VictoryDialog.ActionKind.LEVEL_SELECT),
                    kinds, "关卡 " + level + " 动作顺序应为 下一关/重新开始/选择关卡");
        }
    }

    /** “下一关”必须是默认动作（OK_DONE 且 isDefaultButton），其余动作不得抢占默认样式。 */
    @Test
    void nextLevelIsTheDefaultButtonData() {
        VictoryDialog.Config config = VictoryDialog.configFor("关卡1", 10,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(1)));
        VictoryDialog.Action next = config.actions().get(0);
        assertEquals(VictoryDialog.ActionKind.NEXT_LEVEL, next.kind());
        assertEquals(ButtonBar.ButtonData.OK_DONE, next.buttonData(), "下一关必须使用 OK_DONE");
        assertTrue(next.buttonData().isDefaultButton(), "下一关必须是默认按钮（最醒目）");
        for (VictoryDialog.Action action : config.actions().subList(1, 3)) {
            assertFalse(action.buttonData().isDefaultButton(),
                    "重新开始/选择关卡不得是默认按钮：" + action.text());
        }
    }

    /** 末关（第 5 关）：不提供可点击的“下一关”，仍保留 重新开始/选择关卡。 */
    @Test
    void finalLevelHasNoNextLevelAction() {
        VictoryDialog.Config config = VictoryDialog.configFor("最后一关", 99,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(5)));
        assertTrue(config.nextLevel().isEmpty(), "第 5 关没有下一关");
        assertFalse(config.hasNextLevel(), "末关不得包含下一关动作");
        assertEquals(List.of("重新开始", "选择关卡"),
                config.actions().stream().map(VictoryDialog.Action::text).toList(),
                "末关应仅保留 重新开始/选择关卡");
        assertEquals(List.of(VictoryDialog.ActionKind.RESTART, VictoryDialog.ActionKind.LEVEL_SELECT),
                config.actions().stream().map(VictoryDialog.Action::kind).toList());
    }

    /** 无法识别当前关卡路径时同样不提供“下一关”。 */
    @Test
    void unrecognizedCurrentPathHasNoNextLevelAction() {
        VictoryDialog.Config config = VictoryDialog.configFor("未知关卡", 0, Optional.empty());
        assertFalse(config.hasNextLevel());
        assertEquals(2, config.actions().size(), "无法识别下一关时不得加入下一关按钮");
    }

    /** 末关显示“恭喜完成全部关卡”，非末关保持“恭喜通关！”。 */
    @Test
    void headerTextReflectsWhetherAllLevelsCompleted() {
        VictoryDialog.Config last = VictoryDialog.configFor("末关", 1,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(5)));
        assertEquals("恭喜完成全部关卡", last.headerText(), "末关必须明确显示全部通关");

        for (int level = 1; level <= 4; level++) {
            VictoryDialog.Config config = VictoryDialog.configFor("关卡" + level, 1,
                    LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(level)));
            assertEquals("恭喜通关！", config.headerText(),
                    "关卡 " + level + " 未通关全部关卡时应保持原提示");
        }
    }

    /** 标题与内容文本：标题为“胜利”，内容原样展示关卡名与步数。 */
    @Test
    void titleAndContentTexts() {
        assertEquals("胜利", VictoryDialog.TITLE);
        VictoryDialog.Config config = VictoryDialog.configFor("地下城一", 42,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(1)));
        assertEquals("关卡：地下城一\n步数：42", config.contentText());
    }

    /** “下一关”的目标资源必须来自 LevelCatalog 的下一关条目。 */
    @Test
    void nextLevelEntryComesFromLevelCatalog() {
        for (int level = 1; level <= 4; level++) {
            LevelCatalog.LevelEntry current = LevelCatalog.LEVELS.get(level - 1);
            LevelCatalog.LevelEntry expectedNext = LevelCatalog.LEVELS.get(level);
            VictoryDialog.Config config = VictoryDialog.configFor("关卡" + level, 0,
                    LevelCatalog.nextAfterResourcePath(current.resourcePath()));
            assertEquals(expectedNext, config.nextLevel().orElseThrow(),
                    "关卡 " + level + " 的下一关元数据必须来自 LevelCatalog");
            assertEquals(expectedNext.resourcePath(), config.nextLevel().orElseThrow().resourcePath(),
                    "下一关资源路径必须与 LevelCatalog 一致");
        }
    }
}
