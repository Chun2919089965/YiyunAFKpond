package com.yiyunafkpond.reward;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.entity.Player;

public class PointReward implements RewardType {
    private final YiyunAFKpond plugin;

    public PointReward(YiyunAFKpond plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "point"; }

    @Override
    public void giveReward(Player player, long amount, Pond pond) {
        giveReward(player, (double) amount, pond);
    }

    @Override
    public void giveReward(Player player, double amount, Pond pond) {
        int pointAmount = (int) amount;
        if (pointAmount <= 0) return;
        PlayerData playerData = plugin.getDataManager().getPlayerDataIfLoaded(player.getUniqueId());
        if (playerData == null) return;

        if (!plugin.getHookManager().depositPoint(player, pointAmount)) return;

        plugin.sendPlayerMessage(player, pond.getPointRewardMessage().replace("{point_amount}", String.valueOf(pointAmount)));
        playerData.addPointGained(pointAmount);
        playerData.addTodayPoint(pond.getId(), pointAmount);
        playerData.setLastRewardTime(System.currentTimeMillis());
        plugin.getDataManager().queuePlayerDataSave(playerData);
        plugin.getUiManager().markDirty(player);
    }
}
