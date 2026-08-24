package com.yiyunafkpond.reward.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemRewardServiceTest {
    @Test
    void splitsRewardsAtTheMaterialStackLimit() {
        List<ItemStack> stacks = ItemRewardService.splitStacks(new ItemStack(Material.BREAD), 130);

        assertEquals(List.of(64, 64, 2), stacks.stream().map(ItemStack::getAmount).toList());
    }

    @Test
    void preservesUnstackableMaterialLimits() {
        List<ItemStack> stacks = ItemRewardService.splitStacks(new ItemStack(Material.DIAMOND_SWORD), 2);

        assertEquals(List.of(1, 1), stacks.stream().map(ItemStack::getAmount).toList());
    }
}
