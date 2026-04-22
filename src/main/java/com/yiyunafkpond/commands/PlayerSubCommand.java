package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerSubCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    public PlayerSubCommand(YiyunAFKpond plugin) { this.plugin = plugin; }
    @Override public String getName() { return "player"; }
    @Override public String getDescription() { return "玩家管理命令"; }
    @Override public String getUsage() { return "player <info|reset|set|stats|list> [args...]"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.player"; }
    @Override public boolean isPlayerOnly() { return false; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) { sendHelp(sender); return true; }
        return switch (args[1].toLowerCase()) {
            case "info" -> handleInfo(sender, args);
            case "reset" -> handleReset(sender, args);
            case "set" -> handleSet(sender, args);
            case "stats" -> handleStats(sender, args);
            case "list" -> handleList(sender, args);
            default -> { sendHelp(sender); yield true; }
        };
    }
    
    private void sendHelp(CommandSender sender) {
        plugin.sendPlayerMessage(sender, "&#87CEEB=== 玩家管理命令 ===");
        plugin.sendPlayerMessage(sender, "&#B0E0E6/yap player info <玩家> &#ADD8E6- 查看玩家信息");
        plugin.sendPlayerMessage(sender, "&#B0E0E6/yap player reset <玩家> [daily|pond <池ID>] &#ADD8E6- 重置玩家数据");
        plugin.sendPlayerMessage(sender, "&#B0E0E6/yap player set <玩家> <属性> <值> &#ADD8E6- 设置玩家数据");
        plugin.sendPlayerMessage(sender, "&#B0E0E6/yap player stats <玩家> &#ADD8E6- 查看玩家统计");
        plugin.sendPlayerMessage(sender, "&#B0E0E6/yap player list [afk] &#ADD8E6- 列出在线玩家");
    }
    
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 3) { plugin.sendPlayerMessage(sender, "&#6CA6CD用法：/yap player info <玩家>"); return true; }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { plugin.sendPlayerMessage(sender, "&#6CA6CD玩家不存在或未在线"); return true; }
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        plugin.sendPlayerMessage(sender, "&#87CEEB=== 玩家信息: &#B0E0E6" + target.getName() + " &#87CEEB===");
        plugin.sendPlayerMessage(sender, "&#5B9BD5UUID: &#B0E0E6" + data.getUuid());
        plugin.sendPlayerMessage(sender, "&#5B9BD5总挂机时间: &#B0E0E6" + TimeUtil.formatDuration(data.getTotalAfkTime()));
        plugin.sendPlayerMessage(sender, "&#5B9BD5总经验: &#B0E0E6" + data.getTotalXpGained());
        plugin.sendPlayerMessage(sender, "&#5B9BD5总金币: &#B0E0E6" + String.format("%.2f", data.getTotalMoneyGained()));
        plugin.sendPlayerMessage(sender, "&#5B9BD5总点券: &#B0E0E6" + data.getTotalPointGained());
        plugin.sendPlayerMessage(sender, "&#5B9BD5AFK状态: &#B0E0E6" + (data.isAfk() ? "是" : "否"));
        plugin.sendPlayerMessage(sender, "&#5B9BD5当前池: &#B0E0E6" + (data.getCurrentPondId() != null ? data.getCurrentPondId() : "无"));
        plugin.sendPlayerMessage(sender, "&#5B9BD5今日经验: &#B0E0E6" + data.getTodayExp());
        plugin.sendPlayerMessage(sender, "&#5B9BD5今日金币: &#B0E0E6" + String.format("%.2f", data.getTodayMoney()));
        plugin.sendPlayerMessage(sender, "&#5B9BD5今日点券: &#B0E0E6" + data.getTodayPoint());
        return true;
    }
    
    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length < 3) { plugin.sendPlayerMessage(sender, "&#6CA6CD用法：/yap player reset <玩家> [daily|pond <池ID>]"); return true; }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { plugin.sendPlayerMessage(sender, "&#6CA6CD玩家不存在或未在线"); return true; }
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        
        if (args.length < 4) {
            plugin.getDataManager().removePlayerData(data.getUuid());
            plugin.sendPlayerMessage(sender, "&#87CEEB已删除玩家 &#B0E0E6" + target.getName() + " &#87CEEB的所有数据");
        } else if (args[3].equalsIgnoreCase("daily")) {
            data.resetDailyData();
            plugin.getDataManager().savePlayerData(data);
            plugin.sendPlayerMessage(sender, "&#87CEEB已重置玩家 &#B0E0E6" + target.getName() + " &#87CEEB的每日数据");
        } else if (args[3].equalsIgnoreCase("pond") && args.length >= 5) {
            String pondId = args[4];
            data.removePondAfkTime(pondId);
            data.removePoolTodayExp(pondId);
            data.removePoolTodayMoney(pondId);
            data.removePoolTodayPoint(pondId);
            plugin.getDataManager().savePlayerData(data);
            plugin.sendPlayerMessage(sender, "&#87CEEB已清除玩家 &#B0E0E6" + target.getName() + " &#87CEEB在池 &#B0E0E6" + pondId + " &#87CEEB的数据");
        } else {
            plugin.sendPlayerMessage(sender, "&#6CA6CD用法：/yap player reset <玩家> [daily|pond <池ID>]");
        }
        return true;
    }
    
    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 5) { 
            plugin.sendPlayerMessage(sender, "&#6CA6CD用法：/yap player set <玩家> <属性> <值>");
            plugin.sendPlayerMessage(sender, "&#ADD8E6可用属性: afk, currentPondId, pond <池ID> <exp|money|point> <值>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { plugin.sendPlayerMessage(sender, "&#6CA6CD玩家不存在或未在线"); return true; }
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        String property = args[3].toLowerCase();
        
        switch (property) {
            case "afk" -> {
                boolean afk = Boolean.parseBoolean(args[4]);
                data.setAfk(afk);
                if (afk && args.length >= 6) data.setCurrentPondId(args[5]);
                plugin.getDataManager().savePlayerData(data);
                plugin.sendPlayerMessage(sender, "&#87CEEB已设置 &#B0E0E6" + target.getName() + " &#87CEEB的AFK状态为 &#B0E0E6" + afk);
            }
            case "currentpondid" -> {
                String pondId = args[4].equalsIgnoreCase("none") ? null : args[4];
                data.setCurrentPondId(pondId);
                plugin.getDataManager().savePlayerData(data);
                plugin.sendPlayerMessage(sender, "&#87CEEB已设置 &#B0E0E6" + target.getName() + " &#87CEEB的当前池为 &#B0E0E6" + (pondId != null ? pondId : "无"));
            }
            case "pond" -> {
                if (args.length < 7) { plugin.sendPlayerMessage(sender, "&#6CA6CD用法：/yap player set <玩家> pond <池ID> <exp|money|point> <值>"); return true; }
                String pondId = args[4];
                String type = args[5].toLowerCase();
                double value = Double.parseDouble(args[6]);
                switch (type) {
                    case "exp" -> { data.setPoolTodayExp(pondId, (long) value); recalcTodayTotals(data); }
                    case "money" -> { data.setPoolTodayMoney(pondId, value); recalcTodayTotals(data); }
                    case "point" -> { data.setPoolTodayPoint(pondId, (int) value); recalcTodayTotals(data); }
                    default -> { plugin.sendPlayerMessage(sender, "&#6CA6CD无效类型，可用: exp, money, point"); return true; }
                }
                plugin.getDataManager().savePlayerData(data);
                plugin.sendPlayerMessage(sender, "&#87CEEB已设置 &#B0E0E6" + target.getName() + " &#87CEEB在池 &#B0E0E6" + pondId + " &#87CEEB的" + type + "为 &#B0E0E6" + value);
            }
            default -> plugin.sendPlayerMessage(sender, "&#6CA6CD未知属性: " + property);
        }
        return true;
    }
    
    private void recalcTodayTotals(PlayerData data) {
        long totalExp = data.getPoolTodayExp().values().stream().mapToLong(Long::longValue).sum();
        double totalMoney = data.getPoolTodayMoney().values().stream().mapToDouble(Double::doubleValue).sum();
        int totalPoint = data.getPoolTodayPoint().values().stream().mapToInt(Integer::intValue).sum();
        data.setTodayExp(totalExp);
        data.setTodayMoney(totalMoney);
        data.setTodayPoint(totalPoint);
    }
    
    private boolean handleStats(CommandSender sender, String[] args) {
        if (args.length < 3) { plugin.sendPlayerMessage(sender, "&#6CA6CD用法：/yap player stats <玩家>"); return true; }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { plugin.sendPlayerMessage(sender, "&#6CA6CD玩家不存在或未在线"); return true; }
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        plugin.sendPlayerMessage(sender, "&#87CEEB=== 玩家统计: &#B0E0E6" + target.getName() + " &#87CEEB===");
        plugin.sendPlayerMessage(sender, "&#5B9BD5总挂机时间: &#B0E0E6" + TimeUtil.formatDuration(data.getTotalAfkTime()));
        plugin.sendPlayerMessage(sender, "&#5B9BD5总经验: &#B0E0E6" + data.getTotalXpGained());
        plugin.sendPlayerMessage(sender, "&#5B9BD5总金币: &#B0E0E6" + String.format("%.2f", data.getTotalMoneyGained()));
        plugin.sendPlayerMessage(sender, "&#5B9BD5总点券: &#B0E0E6" + data.getTotalPointGained());
        plugin.sendPlayerMessage(sender, "&#5B9BD5今日经验: &#B0E0E6" + data.getTodayExp());
        plugin.sendPlayerMessage(sender, "&#5B9BD5今日金币: &#B0E0E6" + String.format("%.2f", data.getTodayMoney()));
        plugin.sendPlayerMessage(sender, "&#5B9BD5今日点券: &#B0E0E6" + data.getTodayPoint());
        if (!data.getPondAfkTimes().isEmpty()) {
            plugin.sendPlayerMessage(sender, "&#87CEEB--- 各池挂机时间 ---");
            data.getPondAfkTimes().forEach((pondId, time) -> 
                    plugin.sendPlayerMessage(sender, "&#B0E0E6" + pondId + ": &#ADD8E6" + TimeUtil.formatDuration(time)));
        }
        return true;
    }
    
    private boolean handleList(CommandSender sender, String[] args) {
        boolean afkOnly = args.length >= 3 && args[2].equalsIgnoreCase("afk");
        plugin.sendPlayerMessage(sender, "&#87CEEB=== 在线玩家列表 ===");
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getDataManager().getPlayerDataIfLoaded(player.getUniqueId());
            if (afkOnly && (data == null || !data.isAfk())) continue;
            String afkStatus = (data != null && data.isAfk()) ? "&#87CEEB[AFK]" : "&#ADD8E6[在线]";
            String pondInfo = (data != null && data.getCurrentPondId() != null) ? " &#ADD8E6- &#B0E0E6" + data.getCurrentPondId() : "";
            plugin.sendPlayerMessage(sender, afkStatus + " &#B0E0E6" + player.getName() + pondInfo);
            count++;
        }
        plugin.sendPlayerMessage(sender, "&#87CEEB共 &#B0E0E6" + count + " &#87CEEB人");
        return true;
    }
}
