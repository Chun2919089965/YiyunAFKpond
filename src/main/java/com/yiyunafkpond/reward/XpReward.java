package com.yiyunafkpond.reward;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.entity.Player;

public class XpReward implements RewardType {
    private final YiyunAFKpond plugin;

    public XpReward(YiyunAFKpond plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "xp"; }

    @Override
    public void giveReward(Player player, long amount, Pond pond) {
        if (amount <= 0) return;
        PlayerData playerData = plugin.getDataManager().getPlayerDataIfLoaded(player.getUniqueId());
        if (playerData == null) return;

        player.giveExp((int) Math.min(amount, Integer.MAX_VALUE), pond.isExpApplyMending());

        plugin.sendPlayerMessage(player, pond.getExpRewardMessage().replace("{xp_amount}", String.valueOf(amount)));
        playerData.addXpGained(amount);
        playerData.addTodayExp(pond.getId(), amount);
        playerData.setLastRewardTime(System.currentTimeMillis());
        plugin.getDataManager().queuePlayerDataSave(playerData);
        plugin.getUiManager().markDirty(player);
    }

    @Override
    public void giveReward(Player player, double amount, Pond pond) {
        giveReward(player, (long) amount, pond);
    }
}
