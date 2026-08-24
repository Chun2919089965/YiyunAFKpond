package com.yiyunafkpond.data;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataItemRewardTest {
    @Test
    void tracksSuccessfulRollsIndependentlyPerPool() {
        PlayerData data = new PlayerData(UUID.randomUUID(), "Tester");

        data.addTodayItemRoll("pond-a");
        data.addTodayItemRoll("pond-a");
        data.addTodayItemRoll("pond-b");

        assertEquals(2, data.getDailyItemRollsByPool("pond-a"));
        assertEquals(1, data.getDailyItemRollsByPool("pond-b"));
        assertEquals(0, data.getDailyItemRollsByPool("missing"));
    }

    @Test
    void dailyResetClearsItemRollCounters() {
        PlayerData data = new PlayerData(UUID.randomUUID(), "Tester");
        data.addTodayItemRoll("pond-a");
        data.setLastReset(Date.from(Instant.now().minus(2, ChronoUnit.DAYS)));

        data.checkAndResetDailyData();

        assertTrue(data.getPoolTodayItemRolls().isEmpty());
        assertEquals(0, data.getDailyItemRollsByPool("pond-a"));
    }
}
