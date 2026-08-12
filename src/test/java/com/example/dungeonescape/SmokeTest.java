package com.example.dungeonescape;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 阶段 0 冒烟测试：仅校验应用静态元数据，不启动 JavaFX。
 */
class SmokeTest {

    @Test
    void appTitleIsDungeonEscape() {
        assertEquals("Dungeon Escape", DungeonEscapeApplication.APP_TITLE);
    }

    @Test
    void entryPointClassNameMatchesConfiguredMainClass() {
        assertEquals("com.example.dungeonescape.DungeonEscapeApplication",
                DungeonEscapeApplication.class.getName());
    }
}
