package com.example.dungeonescape.model.goal;

import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.entity.Exit;

/**
 * 出口目标：仅当玩家当前位置存在 {@link Exit} 时满足。
 *
 * <p>只考察玩家当前站位，不记录历史——曾踩过出口但已离开不算达成。
 */
public class ExitGoal implements Goal {

    @Override
    public boolean isSatisfied(GameState gameState) {
        return gameState.entityAt(gameState.getPlayer().getPosition(), Exit.class).isPresent();
    }

    @Override
    public String description() {
        return "到达出口";
    }
}
