package com.example.dungeonescape.ui;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 胜利弹窗动作分发测试：{@link VictoryActionDispatcher} 的按键 → 回调映射。
 *
 * <p>覆盖 下一关（第 1 关 → 第 2 关、第 4 关 → 第 5 关）、重新开始、选择关卡、
 * null、未知 {@link ButtonType}、以及末关（第 5 关）无“下一关”不跳转；每个用例
 * 都校验恰好一次目标回调、其余回调零次（无多余动作）。
 *
 * <p>用真实 {@link VictoryDialogView#createAlert} 构建 {@code Prepared}（按键类型与
 * 弹窗实际按钮一致，分发按身份相等匹配），Alert 构造依赖 JavaFX Toolkit
 * （与 {@code VictoryDialogViewTest} 相同的启动模式）。
 */
class VictoryActionDispatcherTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // 同 JVM 内其他测试类可能已启动 Toolkit：幂等，无需重复启动
        }
    }

    /** 记录各回调的调用次数与 showGame 收到的资源路径。 */
    private static final class CallLog {
        final List<String> shownGames = new ArrayList<>();
        int restartCalls;
        int levelSelectCalls;

        Consumer<String> showGame() {
            return shownGames::add;
        }

        Runnable restart() {
            return () -> restartCalls++;
        }

        Runnable levelSelect() {
            return () -> levelSelectCalls++;
        }
    }

    /**
     * 在 FX 线程上构建弹窗（Alert/DialogPane 构造有严格的 FX 线程检查）。
     *
     * <p>经 {@link Platform#runLater} 执行构建并用 CountDownLatch 等待完成；
     * FX 线程上的异常会被重新抛出为 AssertionError，避免被 runLater 吞掉。
     */
    private static VictoryDialogView.Prepared prepareOnFxThread(VictoryDialog.Config config) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        VictoryDialogView.Prepared[] result = new VictoryDialogView.Prepared[1];
        Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                result[0] = VictoryDialogView.createAlert(config);
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("FX 线程构建弹窗超时");
        }
        if (error[0] != null) {
            throw new AssertionError("FX 线程构建弹窗失败", error[0]);
        }
        return result[0];
    }

    /** 断言其余回调零调用：每个用例只允许目标动作生效。 */
    private static void assertNoExtraCalls(CallLog log, String message) {
        assertEquals(0, log.restartCalls, message + "：不得调用重新开始");
        assertEquals(0, log.levelSelectCalls, message + "：不得调用选择关卡");
    }

    /** 第 1 关点“下一关”：只调用 showGame 且目标为第 2 关资源，无多余回调。 */
    @Test
    void nextLevelFromLevel1DispatchesToLevel2() throws Exception {
        VictoryDialog.Config config = VictoryDialog.configFor("关卡1", 10,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(1)));
        VictoryDialogView.Prepared prepared = prepareOnFxThread(config);
        CallLog log = new CallLog();

        new VictoryActionDispatcher(prepared, config, log.showGame(), log.restart(), log.levelSelect())
                .dispatch(prepared.nextLevel());

        assertEquals(List.of(LevelCatalog.resourcePathFor(2)), log.shownGames,
                "下一关必须只调用 showGame 且目标为第 2 关资源路径");
        assertNoExtraCalls(log, "第 1 关下一关");
    }

    /** 第 4 关点“下一关”：只调用 showGame 且目标为第 5 关资源，无多余回调。 */
    @Test
    void nextLevelFromLevel4DispatchesToLevel5() throws Exception {
        VictoryDialog.Config config = VictoryDialog.configFor("关卡4", 20,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(4)));
        VictoryDialogView.Prepared prepared = prepareOnFxThread(config);
        CallLog log = new CallLog();

        new VictoryActionDispatcher(prepared, config, log.showGame(), log.restart(), log.levelSelect())
                .dispatch(prepared.nextLevel());

        assertEquals(List.of(LevelCatalog.resourcePathFor(5)), log.shownGames,
                "下一关必须只调用 showGame 且目标为第 5 关资源路径");
        assertNoExtraCalls(log, "第 4 关下一关");
    }

    /** 点“重新开始”：只调用 restart，不跳关、不回选关。 */
    @Test
    void restartDispatchesOnlyRestart() throws Exception {
        VictoryDialog.Config config = VictoryDialog.configFor("关卡2", 5,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(2)));
        VictoryDialogView.Prepared prepared = prepareOnFxThread(config);
        CallLog log = new CallLog();

        new VictoryActionDispatcher(prepared, config, log.showGame(), log.restart(), log.levelSelect())
                .dispatch(prepared.restart());

        assertEquals(1, log.restartCalls, "必须恰好调用一次重新开始");
        assertTrue(log.shownGames.isEmpty(), "重新开始不得跳关");
        assertEquals(0, log.levelSelectCalls, "重新开始不得调用选择关卡");
    }

    /** 点“选择关卡”：只调用 levelSelect，不跳关、不重开。 */
    @Test
    void levelSelectDispatchesOnlyLevelSelect() throws Exception {
        VictoryDialog.Config config = VictoryDialog.configFor("关卡3", 7,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(3)));
        VictoryDialogView.Prepared prepared = prepareOnFxThread(config);
        CallLog log = new CallLog();

        new VictoryActionDispatcher(prepared, config, log.showGame(), log.restart(), log.levelSelect())
                .dispatch(prepared.levelSelect());

        assertEquals(1, log.levelSelectCalls, "必须恰好调用一次选择关卡");
        assertTrue(log.shownGames.isEmpty(), "选择关卡不得跳关");
        assertEquals(0, log.restartCalls, "选择关卡不得调用重新开始");
    }

    /** 直接关闭对话框（null）：任何回调都不触发。 */
    @Test
    void nullResultDoesNothing() throws Exception {
        VictoryDialog.Config config = VictoryDialog.configFor("关卡1", 3,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(1)));
        VictoryDialogView.Prepared prepared = prepareOnFxThread(config);
        CallLog log = new CallLog();

        new VictoryActionDispatcher(prepared, config, log.showGame(), log.restart(), log.levelSelect())
                .dispatch(null);

        assertTrue(log.shownGames.isEmpty(), "null 不得调用 showGame");
        assertEquals(0, log.restartCalls, "null 不得调用重新开始");
        assertEquals(0, log.levelSelectCalls, "null 不得调用选择关卡");
    }

    /** 未知 ButtonType（不属于弹窗任何按钮）：任何回调都不触发。 */
    @Test
    void unknownButtonTypeDoesNothing() throws Exception {
        VictoryDialog.Config config = VictoryDialog.configFor("关卡1", 3,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(1)));
        VictoryDialogView.Prepared prepared = prepareOnFxThread(config);
        CallLog log = new CallLog();

        new VictoryActionDispatcher(prepared, config, log.showGame(), log.restart(), log.levelSelect())
                .dispatch(new ButtonType("未知按钮"));

        assertTrue(log.shownGames.isEmpty(), "未知按钮不得调用 showGame");
        assertEquals(0, log.restartCalls, "未知按钮不得调用重新开始");
        assertEquals(0, log.levelSelectCalls, "未知按钮不得调用选择关卡");
    }

    /** 末关（第 5 关）：无“下一关”按钮、无跳转；其余按钮仍只调自己的回调。 */
    @Test
    void finalLevelHasNoNextLevelAndNoJump() throws Exception {
        VictoryDialog.Config config = VictoryDialog.configFor("最后一关", 99,
                LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(5)));
        assertTrue(config.nextLevel().isEmpty(), "第 5 关配置必须没有下一关");
        VictoryDialogView.Prepared prepared = prepareOnFxThread(config);
        assertNull(prepared.nextLevel(), "末关不得产生下一关按钮");

        // 末关上点重新开始 / 选择关卡：各自只调自己的回调，任何情况都不跳关
        CallLog restartLog = new CallLog();
        new VictoryActionDispatcher(prepared, config, restartLog.showGame(), restartLog.restart(), restartLog.levelSelect())
                .dispatch(prepared.restart());
        assertEquals(1, restartLog.restartCalls, "末关重新开始仍须恰好一次");
        assertTrue(restartLog.shownGames.isEmpty(), "末关不得有任何跳关");
        assertEquals(0, restartLog.levelSelectCalls, "末关重新开始不得调用选择关卡");

        CallLog selectLog = new CallLog();
        new VictoryActionDispatcher(prepared, config, selectLog.showGame(), selectLog.restart(), selectLog.levelSelect())
                .dispatch(prepared.levelSelect());
        assertEquals(1, selectLog.levelSelectCalls, "末关选择关卡仍须恰好一次");
        assertTrue(selectLog.shownGames.isEmpty(), "末关不得有任何跳关");
        assertEquals(0, selectLog.restartCalls, "末关选择关卡不得调用重新开始");

        // 末关直接关闭对话框：同样无动作
        CallLog closedLog = new CallLog();
        new VictoryActionDispatcher(prepared, config, closedLog.showGame(), closedLog.restart(), closedLog.levelSelect())
                .dispatch(null);
        assertTrue(closedLog.shownGames.isEmpty(), "末关关闭对话框不得跳关");
        assertEquals(0, closedLog.restartCalls, "末关关闭对话框不得调用重新开始");
        assertEquals(0, closedLog.levelSelectCalls, "末关关闭对话框不得调用选择关卡");
    }
}
