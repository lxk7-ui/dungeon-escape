package com.example.dungeonescape.model.goal;

import com.example.dungeonescape.model.GameState;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 且目标：所有子目标全部满足时才满足。
 *
 * <p>构造时接收至少 2 个非空子目标并做防御性拷贝（内部列表不可修改）；
 * 允许任意递归嵌套，子目标本身可以是组合目标。
 * 描述展示整体条件、完成进度与尚未满足的子目标清单。
 */
public class AndGoal implements Goal {

    private final List<Goal> children;

    /**
     * @param children 子目标，至少 2 个且均非空；内部持有防御性拷贝
     * @throws IllegalArgumentException 子目标少于 2 个或包含 null
     */
    public AndGoal(Goal... children) {
        this.children = validate(children);
    }

    /** 返回子目标只读列表（外部不可修改）。 */
    public List<Goal> getChildren() {
        return children;
    }

    @Override
    public boolean isSatisfied(GameState gameState) {
        return children.stream().allMatch(child -> child.isSatisfied(gameState));
    }

    @Override
    public String description() {
        return "完成所有目标";
    }

    @Override
    public String description(GameState gameState) {
        List<Goal> unfinished = children.stream()
                .filter(child -> !child.isSatisfied(gameState))
                .collect(java.util.stream.Collectors.toList());
        int done = children.size() - unfinished.size();
        String text = "完成所有目标（" + done + "/" + children.size() + "）";
        if (unfinished.isEmpty()) {
            return text;
        }
        String details = unfinished.stream()
                .map(child -> child.description(gameState))
                .collect(Collectors.joining("；"));
        return text + "：未完成——" + details;
    }

    /** 校验子目标并返回不可修改的防御性拷贝。 */
    private static List<Goal> validate(Goal... children) {
        if (children == null || children.length < 2) {
            throw new IllegalArgumentException("组合目标至少需要 2 个子目标");
        }
        for (Goal child : children) {
            if (child == null) {
                throw new IllegalArgumentException("子目标不能为 null");
            }
        }
        return List.of(children);
    }
}
