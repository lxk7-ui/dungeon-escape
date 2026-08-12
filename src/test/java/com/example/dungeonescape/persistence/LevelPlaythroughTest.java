package com.example.dungeonescape.persistence;

import com.example.dungeonescape.model.Direction;
import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.GameStatus;
import com.example.dungeonescape.service.GameEngine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 集成测试：用真实 {@link GameEngine} 按各关卡预期解法走完移动序列，
 * 验证 5 个正式关卡确实可解（拾取/开门/推箱/机关/目标判定全链路）。
 */
class LevelPlaythroughTest {

    private static final String LEVEL_DIR = "com/example/dungeonescape/levels";

    static Stream<Arguments> playthroughs() {
        return Stream.of(
                Arguments.of("level1.json", "RRDDRRRRDD"),
                Arguments.of("level2.json", "RRRRRDULLLDDDRRRD"),
                Arguments.of("level3.json", "RRRLLLDDRRRRRDD"),
                Arguments.of("level4.json", "RRDURRDRDDD"),
                Arguments.of("level5.json", "RRRRRRDDDDLLLLRRRRD"));
    }

    @ParameterizedTest(name = "关卡 {0} 按预期解法通关")
    @MethodSource("playthroughs")
    void intendedSolutionWins(String fileName, String moves) {
        GameState state = new LevelLoader().loadFromClasspath(LEVEL_DIR + "/" + fileName);
        GameEngine engine = new GameEngine(state);
        for (int i = 0; i < moves.length(); i++) {
            engine.move(direction(moves.charAt(i)));
        }
        assertEquals(GameStatus.WON, state.getStatus(),
                "关卡 " + fileName + " 按预期解法应通关");
    }

    private static Direction direction(char c) {
        return switch (c) {
            case 'U' -> Direction.UP;
            case 'D' -> Direction.DOWN;
            case 'L' -> Direction.LEFT;
            case 'R' -> Direction.RIGHT;
            default -> throw new IllegalArgumentException("未知方向：" + c);
        };
    }
}
