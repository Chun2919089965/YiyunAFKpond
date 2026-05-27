package com.yiyunafkpond.reward;

import com.yiyunafkpond.pond.Pond;
import org.bukkit.entity.Player;

public interface RewardType {
    String getName();
    void giveReward(Player player, long amount, Pond pond);
    void giveReward(Player player, double amount, Pond pond);
}
