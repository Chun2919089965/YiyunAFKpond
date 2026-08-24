package com.yiyunafkpond.reward.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRewardSettingsTest {
    @Test
    void validatesSelectionSettings() {
        ItemRewardSettings settings = new ItemRewardSettings();

        assertThrows(IllegalArgumentException.class, () -> settings.setIntervalSeconds(0));
        assertThrows(IllegalArgumentException.class, () -> settings.setRolls(0));
        assertThrows(IllegalArgumentException.class,
                () -> settings.setRolls(ItemRewardSettings.MAX_ROLLS_PER_CYCLE + 1));
        assertThrows(IllegalArgumentException.class, () -> settings.setChance(-0.01));
        assertThrows(IllegalArgumentException.class, () -> settings.setChance(100.01));
        assertThrows(IllegalArgumentException.class, () -> settings.setMaxSuccessfulRollsDaily(-1));

        settings.setIntervalSeconds(30);
        settings.setRolls(2);
        settings.setChance(25.5);
        settings.setMaxSuccessfulRollsDaily(10);

        assertEquals(30, settings.getIntervalSeconds());
        assertEquals(2, settings.getRolls());
        assertEquals(25.5, settings.getChance());
        assertEquals(10, settings.getMaxSuccessfulRollsDaily());
    }

    @Test
    void managesEntriesCaseInsensitively() {
        ItemRewardSettings settings = new ItemRewardSettings();
        ItemRewardEntry entry = new ItemRewardEntry("Daily_Bread",
                ItemRewardEntry.SourceType.MINECRAFT, true, 10.0, 1, 3,
                new ItemStack(Material.BREAD));

        settings.putEntry(entry);

        assertSame(entry, settings.getEntry("daily_bread"));
        assertTrue(settings.hasEnabledEntries());
        assertSame(entry, settings.removeEntry("DAILY_BREAD"));
        assertNull(settings.getEntry("daily_bread"));
        assertFalse(settings.hasEnabledEntries());
    }

    @Test
    void rejectsUnsafeEntryIdsAndAmounts() {
        assertThrows(IllegalArgumentException.class, () -> new ItemRewardEntry("bad.path",
                ItemRewardEntry.SourceType.MINECRAFT, true, 1.0, 1, 1,
                new ItemStack(Material.BREAD)));
        assertThrows(IllegalArgumentException.class, () -> new ItemRewardEntry("too_many",
                ItemRewardEntry.SourceType.MINECRAFT, true, 1.0, 1,
                ItemRewardEntry.MAX_AMOUNT_PER_ROLL + 1, new ItemStack(Material.BREAD)));
    }
}
