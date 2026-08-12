package com.example.dungeonescape.model.entity;

import com.example.dungeonescape.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Player} 测试：单把钥匙的限制与宝物计数。
 */
class PlayerTest {

    @Test
    void playerStartsWithoutKey() {
        Player player = new Player("player", new Position(0, 0));

        assertFalse(player.hasKey());
        assertNull(player.getKey());
    }

    @Test
    void playerHoldsSingleKeyAndNewKeyReplacesOldOne() {
        Player player = new Player("player", new Position(0, 0));

        player.setKey(3);
        assertTrue(player.hasKey());
        assertEquals(3, player.getKey());

        // 单把钥匙：拾取新钥匙直接替换旧钥匙，不会叠加
        player.setKey(9);
        assertTrue(player.hasKey());
        assertEquals(9, player.getKey());

        // 传 null 清除钥匙
        player.setKey(null);
        assertFalse(player.hasKey());
        assertNull(player.getKey());
    }

    @Test
    void treasureCountStartsAtZeroAndIncrements() {
        Player player = new Player("player", new Position(0, 0));

        assertEquals(0, player.getTreasureCount());
        player.addTreasure();
        player.addTreasure();
        assertEquals(2, player.getTreasureCount());
    }

    @Test
    void playerDoesNotBlockMovement() {
        assertFalse(new Player("player", new Position(0, 0)).isBlocking());
    }
}
