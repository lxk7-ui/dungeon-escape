package com.example.dungeonescape.ui;

import com.example.dungeonescape.persistence.EntityDefinition;
import com.example.dungeonescape.persistence.LevelDefinition;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * 选关卡片的构建：真实像素 sprite 图标 + 关卡编号/名称 + 数据驱动的机制说明。
 *
 * <p>图标按关卡真实内容选择（优先级：钥匙 &gt; 宝物 &gt; 机关/巨石 &gt; 出口），
 * 全部来自 dungeon-spritesheet.png 的紧贴裁切；名称与机制说明取自关卡 JSON
 * （经 {@link GoalSummary} 推导），不硬编码。单个关卡加载失败时返回禁用卡片
 * （图标仍为出口占位，名称与说明显示占位文案），由控制器负责错误提示。
 */
public final class LevelSelectCard {

    /** 卡片样式类（CSS 定位，测试断言用）。 */
    public static final String CARD_STYLE = "level-card";
    /** 标题行样式类。 */
    public static final String TITLE_STYLE = "level-card-title";
    /** 说明行样式类。 */
    public static final String DESC_STYLE = "level-card-desc";
    /** 图标样式类。 */
    public static final String ICON_STYLE = "level-card-icon";

    private LevelSelectCard() {
    }

    /**
     * 创建关卡卡片按钮（不绑定动作，由控制器注入 onAction）。
     *
     * @param entry 关卡元数据（编号与资源路径）
     * @param def   已加载的关卡定义；为 null 时创建禁用占位卡片
     * @return 可聚焦的卡片按钮
     */
    public static Button create(LevelCatalog.LevelEntry entry, LevelDefinition def) {
        Button card = new Button();
        card.getStyleClass().add(CARD_STYLE);
        card.setFocusTraversable(true);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(64);

        boolean usable = def != null;
        String name = usable ? def.name() : "关卡加载失败";
        String summary = usable ? GoalSummary.of(def.goal()) : "无法读取关卡数据";
        Rectangle2D iconRect = usable ? iconFor(def) : DungeonTheme.ICON_EXIT;

        ImageView icon = DungeonTheme.createIcon(iconRect, DungeonTheme.CARD_ICON_SIZE);
        icon.getStyleClass().add(ICON_STYLE);
        Label title = new Label("第 " + entry.number() + " 关：" + name);
        title.getStyleClass().add(TITLE_STYLE);
        Label desc = new Label(summary);
        desc.getStyleClass().add(DESC_STYLE);
        desc.setWrapText(true);
        // wrapText Label 的 prefHeight 按无界宽度计算（逐字换行），会让卡片高度虚高、
        // 撑爆 ScrollPane 内容区；说明文案在受支持窗口宽度下均单行，
        // 固定 pref 行高即可让布局稳定，极端窄窗口换行时仍正常显示
        desc.setPrefHeight(17.0);
        desc.setMaxWidth(Double.MAX_VALUE);

        VBox textBox = new VBox(3, title, desc);
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

        HBox graphic = new HBox(14, icon, textBox);
        graphic.setAlignment(Pos.CENTER_LEFT);
        graphic.setMaxWidth(Double.MAX_VALUE);
        card.setGraphic(graphic);
        card.setDisable(!usable);
        return card;
    }

    /**
     * 依据关卡真实内容选择图标（从图集紧贴裁切）：
     * 含钥匙实体用钥匙，否则含宝物用宝物，否则含机关/巨石用巨石，否则用出口。
     */
    static Rectangle2D iconFor(LevelDefinition def) {
        List<EntityDefinition> entities = def.entities() == null ? List.of() : def.entities();
        boolean hasKey = entities.stream().anyMatch(e -> "key".equals(e.type()));
        boolean hasTreasure = entities.stream().anyMatch(e -> "treasure".equals(e.type()));
        boolean hasSwitchOrBoulder = entities.stream()
                .anyMatch(e -> "switch".equals(e.type()) || "boulder".equals(e.type()));
        if (hasKey) {
            return DungeonTheme.ICON_KEY;
        }
        if (hasTreasure) {
            return DungeonTheme.ICON_TREASURE;
        }
        if (hasSwitchOrBoulder) {
            return DungeonTheme.ICON_BOULDER;
        }
        return DungeonTheme.ICON_EXIT;
    }
}
