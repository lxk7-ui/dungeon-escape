package com.example.dungeonescape.ui;

import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.persistence.LevelLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 5 个内置关卡元数据映射测试：编号连续、资源路径唯一且存在、
 * 路径可经 LevelLoader 加载出带名称的关卡、编号查询映射正确。
 */
class LevelCatalogTest {

    @Test
    void containsExactlyFiveLevels() {
        assertEquals(5, LevelCatalog.LEVELS.size());
    }

    @Test
    void levelNumbersAreSequentialFromOne() {
        for (int i = 0; i < LevelCatalog.LEVELS.size(); i++) {
            assertEquals(i + 1, LevelCatalog.LEVELS.get(i).number(), "关卡编号应按 1..5 顺序排列");
        }
    }

    @Test
    void levelResourcePathsAreUniqueAndResolvable() {
        long distinct = LevelCatalog.LEVELS.stream()
                .map(LevelCatalog.LevelEntry::resourcePath)
                .distinct()
                .count();
        assertEquals(LevelCatalog.LEVELS.size(), distinct, "关卡资源路径不应重复");
        for (LevelCatalog.LevelEntry entry : LevelCatalog.LEVELS) {
            assertNotNull(LevelCatalogTest.class.getClassLoader().getResource(entry.resourcePath()),
                    "关卡资源必须存在于 classpath：" + entry.resourcePath());
        }
    }

    @Test
    void everyLevelPathLoadsAndExposesNonBlankName() {
        LevelLoader loader = new LevelLoader();
        for (LevelCatalog.LevelEntry entry : LevelCatalog.LEVELS) {
            GameState state = loader.loadFromClasspath(entry.resourcePath());
            assertFalse(state.getLevelName().isBlank(),
                    "关卡 " + entry.number() + " 名称不应为空白：" + entry.resourcePath());
        }
    }

    @Test
    void resourcePathForReturnsMappedPath() {
        for (LevelCatalog.LevelEntry entry : LevelCatalog.LEVELS) {
            assertEquals(entry.resourcePath(), LevelCatalog.resourcePathFor(entry.number()));
        }
    }

    @Test
    void resourcePathForOutOfRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> LevelCatalog.resourcePathFor(0));
        assertThrows(IllegalArgumentException.class, () -> LevelCatalog.resourcePathFor(6));
        assertThrows(IllegalArgumentException.class, () -> LevelCatalog.resourcePathFor(-1));
    }

    @Test
    void findReturnsEmptyForUnknownNumber() {
        assertTrue(LevelCatalog.find(5).isPresent());
        assertTrue(LevelCatalog.find(99).isEmpty());
    }

    @Test
    void nextAfterResourcePathReturnsSuccessiveLevel() {
        for (int i = 0; i < LevelCatalog.LEVELS.size() - 1; i++) {
            LevelCatalog.LevelEntry current = LevelCatalog.LEVELS.get(i);
            LevelCatalog.LevelEntry expectedNext = LevelCatalog.LEVELS.get(i + 1);
            assertEquals(expectedNext, LevelCatalog.nextAfterResourcePath(current.resourcePath()).orElse(null),
                    "关卡 " + current.number() + " 的下一关应为关卡 " + expectedNext.number());
        }
    }

    @Test
    void nextAfterResourcePathReturnsEmptyAfterLastLevel() {
        String lastPath = LevelCatalog.resourcePathFor(5);
        assertTrue(LevelCatalog.nextAfterResourcePath(lastPath).isEmpty());
    }

    @Test
    void nextAfterResourcePathAcceptsSingleLeadingSlash() {
        for (int i = 0; i < LevelCatalog.LEVELS.size() - 1; i++) {
            LevelCatalog.LevelEntry current = LevelCatalog.LEVELS.get(i);
            LevelCatalog.LevelEntry expectedNext = LevelCatalog.LEVELS.get(i + 1);
            assertEquals(expectedNext,
                    LevelCatalog.nextAfterResourcePath("/" + current.resourcePath()).orElse(null),
                    "带前导斜杠的关卡 " + current.number() + " 应解析出同一关卡");
        }
        assertTrue(LevelCatalog.nextAfterResourcePath("/" + LevelCatalog.resourcePathFor(5)).isEmpty(),
                "带前导斜杠的最后一关仍应返回 empty");
    }

    @Test
    void nextAfterResourcePathReturnsEmptyForNullAndBlank() {
        assertTrue(LevelCatalog.nextAfterResourcePath(null).isEmpty());
        assertTrue(LevelCatalog.nextAfterResourcePath("").isEmpty());
        assertTrue(LevelCatalog.nextAfterResourcePath("   ").isEmpty());
    }

    @Test
    void nextAfterResourcePathReturnsEmptyForUnknownPath() {
        assertTrue(LevelCatalog.nextAfterResourcePath("com/example/dungeonescape/levels/unknown.json").isEmpty());
        assertTrue(LevelCatalog.nextAfterResourcePath("/com/example/dungeonescape/levels/unknown.json").isEmpty());
    }

    @Test
    void nextAfterResourcePathDoesNotFuzzyMatchWrongFileNames() {
        assertTrue(LevelCatalog.nextAfterResourcePath("com/example/dungeonescape/levels/level1.jsonx").isEmpty(),
                "不应接受带尾缀的文件名");
        assertTrue(LevelCatalog.nextAfterResourcePath("com/example/dungeonescape/levels/level10.json").isEmpty(),
                "不应把 level10.json 当作 level1.json");
        assertTrue(LevelCatalog.nextAfterResourcePath("level1.json").isEmpty(),
                "裸文件名不应命中完整路径");
        assertTrue(LevelCatalog.nextAfterResourcePath("//" + LevelCatalog.LEVELS_DIR + "/level1.json").isEmpty(),
                "仅容忍单个前导斜杠");
        assertTrue(LevelCatalog.nextAfterResourcePath(LevelCatalog.resourcePathFor(1) + " ").isEmpty(),
                "尾随空白不应命中");
    }
}
