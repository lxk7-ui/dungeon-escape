package com.example.dungeonescape.service;

import com.example.dungeonescape.model.Direction;
import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.Position;
import com.example.dungeonescape.model.entity.Boulder;
import com.example.dungeonescape.model.entity.Door;
import com.example.dungeonescape.model.entity.Entity;
import com.example.dungeonescape.model.entity.Key;
import com.example.dungeonescape.model.entity.Player;
import com.example.dungeonescape.model.entity.Treasure;

import java.util.List;
import java.util.Optional;

/**
 * 移动规则：判定一次移动尝试的结果并就地更新局面。
 *
 * <p>规则要点：
 * <ul>
 *   <li>目标越界或为墙：移动失败；</li>
 *   <li>目标为关闭的门：持有编号匹配的钥匙才能开门、消耗钥匙并进入，否则失败；</li>
 *   <li>目标为巨石：向同方向推动一格，推动目标须在地图内且无墙、关闭的门、其他巨石或玩家，否则失败；</li>
 *   <li>成功进入目标格后拾取宝物；未持有钥匙时可拾取钥匙，已持有钥匙时钥匙留在地图。</li>
 * </ul>
 *
 * <p>只负责移动与拾取规则，不修改步数、游戏状态或触发事件
 * （由 {@link GameEngine} 负责编排）。
 */
public class MovementService {

    /**
     * 尝试让玩家沿指定方向移动一步，并就地更新玩家、实体位置与拾取状态。
     *
     * @param state     游戏局面
     * @param direction 移动方向
     * @return 移动结果；{@link MoveResult#moved()} 为 true 表示成功（应计一步）
     */
    public MoveResult move(GameState state, Direction direction) {
        Player player = state.getPlayer();
        Position target = player.getPosition().move(direction);

        // 越界：失败
        if (!state.isInside(target)) {
            return MoveResult.failed(MoveOutcome.OUT_OF_BOUNDS);
        }

        // 关闭的门：需持有编号匹配的钥匙，成功则开门、消耗钥匙并进入
        Optional<Door> door = state.entityAt(target, Door.class);
        if (door.isPresent() && !door.get().isOpen()) {
            if (!player.hasKey() || player.getKey() != door.get().getDoorId()) {
                return MoveResult.failed(MoveOutcome.DOOR_LOCKED);
            }
            door.get().setOpen(true);
            player.setKey(null);
            return enter(state, player, target, true, false);
        }

        // 巨石：向同方向推动一格，随后玩家进入
        Optional<Boulder> boulder = state.entityAt(target, Boulder.class);
        if (boulder.isPresent()) {
            Position boulderTarget = target.move(direction);
            if (!canPush(state, boulderTarget)) {
                return MoveResult.failed(MoveOutcome.BOULDER_UNMOVABLE);
            }
            boulder.get().setPosition(boulderTarget);
            return enter(state, player, target, false, true);
        }

        // 墙或其他阻挡物：失败
        if (state.isBlocked(target)) {
            return MoveResult.failed(MoveOutcome.WALL);
        }

        // 普通移动
        return enter(state, player, target, false, false);
    }

    /**
     * 巨石推动目标是否可行：必须在网格内，且不被任何阻挡实体占据
     * （墙、关闭的门、另一巨石等）。玩家位于巨石相邻格，几何上不可能
     * 出现在巨石推动目标处，故无需单独检查。
     */
    private boolean canPush(GameState state, Position boulderTarget) {
        return state.isInside(boulderTarget) && !state.isBlocked(boulderTarget);
    }

    /**
     * 玩家进入目标格并完成拾取：
     * 宝物被拾取并删除（计数加一）；钥匙仅在玩家未持有钥匙时被拾取删除，
     * 已持有钥匙时钥匙留在地图但玩家仍可进入。
     */
    private MoveResult enter(GameState state, Player player, Position target,
                             boolean doorOpened, boolean boulderPushed) {
        player.setPosition(target);

        boolean treasureCollected = false;
        boolean keyCollected = false;
        for (Entity entity : List.copyOf(state.entitiesAt(target))) {
            if (entity instanceof Treasure treasure) {
                state.removeEntity(treasure);
                player.addTreasure();
                treasureCollected = true;
            } else if (entity instanceof Key key && !player.hasKey()) {
                player.setKey(key.getKeyId());
                state.removeEntity(key);
                keyCollected = true;
            }
        }
        return new MoveResult(true, MoveOutcome.MOVED, doorOpened, boulderPushed,
                treasureCollected, keyCollected);
    }
}
