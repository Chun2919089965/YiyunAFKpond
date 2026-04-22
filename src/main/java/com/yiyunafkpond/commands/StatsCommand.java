package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import com.yiyunafkpond.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class StatsCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    public StatsCommand(YiyunAFKpond plugin) { this.plugin = plugin; }
    @Override public String getName() { return "stats"; }
    @Override public String getDescription() { return "显示统计数据"; }
    @Override public String getUsage() { return "stats [player <名称>|pool <池ID>]"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.stats"; }
    @Override public boolean isPlayerOnly() { return false; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 1) {
            showGlobalStats(sender);
        } else if (args.length == 2) {
            showPlayerStats(sender, args[1]);
        } else if (args.length == 3 && args[1].equalsIgnoreCase("pool")) {
            showPoolStats(sender, args[2]);
        } else {
            plugin.sendPlayerMessage(sender, "&#6CA6CD用法：/yap stats [player <名称>|pool <池ID>]");
        }
        return true;
    }
    
    private void showPlayerStats(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD玩家不存在或未在线: " + playerName);
            return;
        }
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        plugin.sendPlayerMessage(sender, "&#87CEEB=== 玩家统计: &#B0E0E6" + playerName + " &#87CEEB===");
        plugin.sendPlayerMessage(sender, "&#5B9BD5总挂机时间: &#B0E0E6" + TimeUtil.formatDuration(data.getTotalAfkTime()));
        plugin.sendPlayerMessage(sender, "&#5B9BD5总经验: &#B0E0E6" + data.getTotalXpGained());
        plugin.sendPlayerMessage(sender, "&#5B9BD5总金币: &#B0E0E6" + String.format("%.2f", data.getTotalMoneyGained()));
        plugin.sendPlayerMessage(sender, "&#5B9BD5总点券: &#B0E0E6" + data.getTotalPointGained());
        plugin.sendPlayerMessage(sender, "&#5B9BD5今日经验: &#B0E0E6" + data.getTodayExp());
        plugin.sendPlayerMessage(sender, "&#5B9BD5今日金币: &#B0E0E6" + String.format("%.2f", data.getTodayMoney()));
        plugin.sendPlayerMessage(sender, "&#5B9BD5今日点券: &#B0E0E6" + data.getTodayPoint());
        
        if (!data.getPondAfkTimes().isEmpty()) {
            plugin.sendPlayerMessage(sender, "&#87CEEB--- 各池挂机时间 ---");
            data.getPondAfkTimes().entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        Pond pond = plugin.getPondManager().getPond(entry.getKey());
                        String pondName = pond != null ? pond.getName() : entry.getKey();
                        plugin.sendPlayerMessage(sender, "&#B0E0E6" + pondName + ": &#ADD8E6" + TimeUtil.formatDuration(entry.getValue()));
                    });
        }
        
        if (!data.getPoolTodayExp().isEmpty()) {
            plugin.sendPlayerMessage(sender, "&#87CEEB--- 各池今日经验 ---");
            data.getPoolTodayExp().entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        Pond pond = plugin.getPondManager().getPond(entry.getKey());
                        String pondName = pond != null ? pond.getName() : entry.getKey();
                        plugin.sendPlayerMessage(sender, "&#B0E0E6" + pondName + ": &#ADD8E6" + entry.getValue());
                    });
        }
        
        if (!data.getPoolTodayMoney().isEmpty()) {
            plugin.sendPlayerMessage(sender, "&#87CEEB--- 各池今日金币 ---");
            data.getPoolTodayMoney().entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(entry -> {
                        Pond pond = plugin.getPondManager().getPond(entry.getKey());
                        String pondName = pond != null ? pond.getName() : entry.getKey();
                        plugin.sendPlayerMessage(sender, "&#B0E0E6" + pondName + ": &#ADD8E6" + String.format("%.2f", entry.getValue()));
                    });
        }
    }
    
    private void showPoolStats(CommandSender sender, String poolId) {
        Pond pond = plugin.getPondManager().getPond(poolId);
        if (pond == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD未找到池: " + poolId);
            return;
        }
        List<Player> playersInPond = plugin.getPondManager().getPlayersInPond(pond);
        plugin.sendPlayerMessage(sender, "&#87CEEB=== 池统计: &#B0E0E6" + pond.getName() + " &#87CEEB===");
        plugin.sendPlayerMessage(sender, "&#5B9BD5状态: &#B0E0E6" + (pond.isEnabled() ? "启用" : "禁用"));
        plugin.sendPlayerMessage(sender, "&#5B9BD5经验间隔: &#B0E0E6" + pond.getExpInterval() + "秒");
        plugin.sendPlayerMessage(sender, "&#5B9BD5金币间隔: &#B0E0E6" + pond.getMoneyInterval() + "秒");
        plugin.sendPlayerMessage(sender, "&#5B9BD5点券间隔: &#B0E0E6" + pond.getPointInterval() + "秒");
        plugin.sendPlayerMessage(sender, "&#5B9BD5经验倍率: &#B0E0E6" + pond.getXpRate());
        plugin.sendPlayerMessage(sender, "&#5B9BD5金币倍率: &#B0E0E6" + pond.getMoneyRate());
        plugin.sendPlayerMessage(sender, "&#5B9BD5点券倍率: &#B0E0E6" + pond.getPointRate());
        plugin.sendPlayerMessage(sender, "&#5B9BD5每日经验上限: &#B0E0E6" + (pond.getExpMaxDaily() > 0 ? pond.getExpMaxDaily() : "无限制"));
        plugin.sendPlayerMessage(sender, "&#5B9BD5每日金币上限: &#B0E0E6" + (pond.getMoneyMaxDaily() > 0 ? String.format("%.2f", pond.getMoneyMaxDaily()) : "无限制"));
        plugin.sendPlayerMessage(sender, "&#5B9BD5每日点券上限: &#B0E0E6" + (pond.getPointMaxDaily() > 0 ? String.format("%.0f", pond.getPointMaxDaily()) : "无限制"));
        plugin.sendPlayerMessage(sender, "&#5B9BD5当前玩家数: &#B0E0E6" + playersInPond.size());
    }
    
    private void showGlobalStats(CommandSender sender) {
        Collection<PlayerData> allData = plugin.getDataManager().getAllPlayerData();
        long totalAfkTime = allData.stream().mapToLong(PlayerData::getTotalAfkTime).sum();
        long totalXp = allData.stream().mapToLong(PlayerData::getTotalXpGained).sum();
        double totalMoney = allData.stream().mapToDouble(PlayerData::getTotalMoneyGained).sum();
        int totalPoint = allData.stream().mapToInt(PlayerData::getTotalPointGained).sum();
        
        plugin.sendPlayerMessage(sender, "&#87CEEB=== 全局统计 ===");
        plugin.sendPlayerMessage(sender, "&#5B9BD5总玩家数: &#B0E0E6" + allData.size());
        plugin.sendPlayerMessage(sender, "&#5B9BD5总池数: &#B0E0E6" + plugin.getPondManager().getPonds().size());
        plugin.sendPlayerMessage(sender, "&#5B9BD5总挂机时间: &#B0E0E6" + TimeUtil.formatDuration(totalAfkTime));
        plugin.sendPlayerMessage(sender, "&#5B9BD5总经验: &#B0E0E6" + totalXp);
        plugin.sendPlayerMessage(sender, "&#5B9BD5总金币: &#B0E0E6" + String.format("%.2f", totalMoney));
        plugin.sendPlayerMessage(sender, "&#5B9BD5总点券: &#B0E0E6" + totalPoint);
        
        for (Pond pond : plugin.getPondManager().getPonds()) {
            int playersInPond = plugin.getPondManager().getPlayersInPond(pond).size();
            plugin.sendPlayerMessage(sender, "&#B0E0E6" + pond.getName() + ": &#ADD8E6" + playersInPond + "人在线");
        }
    }
}
