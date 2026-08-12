package com.example.dungeonescape.ui;

import java.util.List;
import java.util.Optional;

/**
 * 内置 5 个关卡的元数据目录：关卡编号 → classpath 资源路径。
 *
 * <p>只承载静态映射，不含任何游戏规则；关卡名称由运行时经
 * {@link com.example.dungeonescape.persistence.LevelLoader} 从 JSON 读取。
 */
public final class LevelCatalog {

    /** 单个关卡元数据。 */
    public record LevelEntry(int number, String resourcePath) {
    }

    /** 关卡资源所在 classpath 目录。 */
    public static final String LEVELS_DIR = "com/example/dungeonescape/levels";

    /** 内置关卡（编号 1..5）。 */
    public static final List<LevelEntry> LEVELS = List.of(
            new LevelEntry(1, LEVELS_DIR + "/level1.json"),
            new LevelEntry(2, LEVELS_DIR + "/level2.json"),
            new LevelEntry(3, LEVELS_DIR + "/level3.json"),
            new LevelEntry(4, LEVELS_DIR + "/level4.json"),
            new LevelEntry(5, LEVELS_DIR + "/level5.json"));

    private LevelCatalog() {
    }

    /** 按编号查找关卡元数据。 */
    public static Optional<LevelEntry> find(int number) {
        return LEVELS.stream().filter(entry -> entry.number() == number).findFirst();
    }

    /**
     * 按当前关卡的 classpath 资源路径查询下一关。
     *
     * <p>入参兼容可选的单个前导斜杠（如
     * {@code /com/example/dungeonescape/levels/level1.json}），剥离后必须与内置路径
     * 完全一致才视为命中，不做模糊匹配；null、空白、未知或最后一关返回 empty。
     *
     * @param resourcePath 当前关卡的 classpath 资源路径
     * @return 下一关的元数据；无下一关或路径无法识别时为空
     */
    public static Optional<LevelEntry> nextAfterResourcePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return Optional.empty();
        }
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        for (int i = 0; i < LEVELS.size(); i++) {
            if (LEVELS.get(i).resourcePath().equals(normalized)) {
                return i + 1 < LEVELS.size() ? Optional.of(LEVELS.get(i + 1)) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * 返回编号对应的 classpath 资源路径。
     *
     * @param number 关卡编号（1..5）
     * @throws IllegalArgumentException 编号超出内置范围时抛出
     */
    public static String resourcePathFor(int number) {
        return find(number)
                .map(LevelEntry::resourcePath)
                .orElseThrow(() -> new IllegalArgumentException("未知关卡编号：" + number));
    }
}
