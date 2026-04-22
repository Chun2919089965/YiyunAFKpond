package com.yiyunafkpond.reward;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.entity.Player;

public class MoneyReward implements RewardType {
    private final YiyunAFKpond plugin;

    public MoneyReward(YiyunAFKpond plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "money"; }

    @Override
    public void giveReward(Player player, long amount, Pond pond) {
        giveReward(player, (double) amount, pond);
    }

    @Override
    public void giveReward(Player player, double amount, Pond pond) {
        if (amount <= 0) return;
        PlayerData playerData = plugin.getDataManager().getPlayerDataIfLoaded(player.getUniqueId());
        if (playerData == null) return;

        if (!plugin.getHookManager().depositMoney(player, amount)) return;

        plugin.sendPlayerMessage(player, pond.getMoneyRewardMessage().replace("{money_amount}", String.format("%.2f", amount)));
        playerData.addMoneyGained(amount);
        playerData.addTodayMoney(pond.getId(), amount);
        playerData.setLastRewardTime(System.currentTimeMillis());
        plugin.getDataManager().queuePlayerDataSave(playerData);
        plugin.getUiManager().markDirty(player);
    }
}
