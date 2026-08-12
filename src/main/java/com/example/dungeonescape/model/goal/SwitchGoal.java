package com.example.dungeonescape.model.goal;

import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.entity.FloorSwitch;

import java.util.List;

/**
 * 机关目标：至少存在一个 {@link FloorSwitch}，且每一个都被巨石压住时满足。
 *
 * <p>地图上没有任何机关时永不满足；描述按实时“已覆盖/总数”展示。
 */
public class SwitchGoal implements Goal {

    @Override
    public boolean isSatisfied(GameState gameState) {
        List<FloorSwitch> switches = switches(gameState);
        return !switches.isEmpty()
                && switches.stream().allMatch(s -> gameState.isSwitchCovered(s.getPosition()));
    }

    @Override
    public String description() {
        return "覆盖所有机关";
    }

    @Override
    public String description(GameState gameState) {
        List<FloorSwitch> switches = switches(gameState);
        long covered = switches.stream()
                .filter(s -> gameState.isSwitchCovered(s.getPosition()))
                .count();
        return "覆盖所有机关（" + covered + "/" + switches.size() + "）";
    }

    /** 地图上现存的所有地板机关。 */
    private List<FloorSwitch> switches(GameState gameState) {
        return gameState.getEntities().stream()
                .filter(FloorSwitch.class::isInstance)
                .map(FloorSwitch.class::cast)
                .toList();
    }
}
