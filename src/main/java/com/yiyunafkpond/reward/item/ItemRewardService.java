package com.yiyunafkpond.reward.item;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ItemRewardService {
    private final YiyunAFKpond plugin;
    private final Random random;

    public ItemRewardService(YiyunAFKpond plugin) {
        this(plugin, new Random());
    }

    ItemRewardService(YiyunAFKpond plugin, Random random) {
        this.plugin = plugin;
        this.random = random;
    }

    public boolean processScheduledRolls(Player player, PlayerData data, Pond pond) {
        if (!player.hasPermission("yiyunafkpond.reward.item")) return false;
        ItemRewardSettings settings = pond.getItemRewardSettings();
        int successful = 0;
        for (int roll = 0; roll < settings.getRolls(); roll++) {
            int maxDaily = settings.getMaxSuccessfulRollsDaily();
            if (maxDaily > 0 && data.getDailyItemRollsByPool(pond.getId()) >= maxDaily) {
                if (successful == 0) sendLimitMessage(player, pond);
                break;
            }
            if (random.nextDouble() * 100.0 >= settings.getChance()) continue;
            if (grantSelectedReward(player, pond, settings)) {
                data.addTodayItemRoll(pond.getId());
                successful++;
            }
        }
        return successful > 0;
    }

    public boolean grantTestReward(Player player, Pond pond) {
        if (!player.hasPermission("yiyunafkpond.reward.item")) return false;
        return grantSelectedReward(player, pond, pond.getItemRewardSettings());
    }

    private boolean grantSelectedReward(Player player, Pond pond, ItemRewardSettings settings) {
        List<ItemRewardEntry> entries = List.copyOf(settings.getEntries());
        ItemRewardEntry entry = WeightedPicker.pick(entries, ItemRewardEntry::isEnabled,
                ItemRewardEntry::getWeight, random);
        if (entry == null) return false;

        int amount = entry.getMinAmount();
        if (entry.getMaxAmount() > entry.getMinAmount()) {
            amount += random.nextInt(entry.getMaxAmount() - entry.getMinAmount() + 1);
        }

        ItemStack template = entry.getTemplate();
        PlayerInventory inventory = player.getInventory();
        if (settings.getOverflowPolicy() == ItemRewardSettings.OverflowPolicy.SKIP
                && !canFit(inventory, template, amount)) {
            sendMessage(player, "player.item-inventory-full", pond, entry, template, amount);
            return false;
        }

        for (ItemStack stack : splitStacks(template, amount)) {
            Map<Integer, ItemStack> leftovers = inventory.addItem(stack);
            for (ItemStack leftover : leftovers.values()) {
                // SKIP capacity is prechecked; this guard prevents loss if another plugin mutates the inventory.
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        sendMessage(player, "player.item-reward", pond, entry, template, amount);
        return true;
    }

    private void sendLimitMessage(Player player, Pond pond) {
        plugin.sendPlayerMessage(player, plugin.getLanguageManager().getMessage("player.item-limit", Map.of(
                "pool_name", pond.getName(),
                "pool_id", pond.getId()
        )));
    }

    private void sendMessage(Player player, String path, Pond pond, ItemRewardEntry entry,
                             ItemStack item, int amount) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("pool_name", pond.getName());
        placeholders.put("pool_id", pond.getId());
        placeholders.put("reward_id", entry.getId());
        placeholders.put("item_name", getDisplayName(item));
        placeholders.put("amount", String.valueOf(amount));
        plugin.sendPlayerMessage(player, plugin.getLanguageManager().getMessage(path, placeholders));
    }

    private String getDisplayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component displayName = meta.displayName();
            if (displayName != null) {
                return PlainTextComponentSerializer.plainText().serialize(displayName);
            }
        }
        return item.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    static List<ItemStack> splitStacks(ItemStack template, int amount) {
        List<ItemStack> result = new ArrayList<>();
        int maxStackSize = Math.max(1, template.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = template.clone();
            int stackAmount = Math.min(maxStackSize, remaining);
            stack.setAmount(stackAmount);
            result.add(stack);
            remaining -= stackAmount;
        }
        return result;
    }

    static boolean canFit(PlayerInventory inventory, ItemStack template, int amount) {
        int capacity = 0;
        int maxStackSize = Math.max(1, template.getMaxStackSize());
        for (ItemStack current : inventory.getStorageContents()) {
            if (current == null || current.getType().isAir()) {
                capacity += maxStackSize;
            } else if (current.isSimilar(template)) {
                capacity += Math.max(0, current.getMaxStackSize() - current.getAmount());
            }
            if (capacity >= amount) return true;
        }
        return false;
    }
}
