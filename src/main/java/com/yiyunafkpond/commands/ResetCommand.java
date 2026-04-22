package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ResetCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    public ResetCommand(YiyunAFKpond plugin) { this.plugin = plugin; }
    @Override public String getName() { return "reset"; }
    @Override public String getDescription() { return "重置玩家数据"; }
    @Override public String getUsage() { return "reset <player>"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.reset"; }
    @Override public boolean isPlayerOnly() { return false; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD使用方法: /yap reset <player>");
            return true;
        }
        String playerName = args[1];
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD玩家不存在或未在线: " + playerName);
            return true;
        }
        UUID uuid = target.getUniqueId();
        PlayerData playerData = plugin.getDataManager().getPlayerData(uuid);
        if (playerData == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD玩家数据不存在: " + playerName);
            return true;
        }
        playerData.setTotalAfkTime(0);
        playerData.setTotalXpGained(0);
        playerData.setTotalMoneyGained(0.0);
        playerData.setTotalPointGained(0);
        playerData.setTodayExp(0);
        playerData.setTodayMoney(0.0);
        playerData.setTodayPoint(0);
        playerData.clearPoolTodayExp();
        playerData.clearPoolTodayMoney();
        playerData.clearPoolTodayPoint();
        playerData.clearPondAfkTimes();
        plugin.getDataManager().savePlayerData(playerData);
        plugin.sendPlayerMessage(sender, "&#87CEEB成功重置玩家 &#B0E0E6" + playerName + " &#ADD8E6的数据!");
        plugin.getAuditLogger().logPlayerReset(sender, playerName);
        return true;
    }
}
