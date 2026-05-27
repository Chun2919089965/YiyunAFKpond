package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    
    public ReloadCommand(YiyunAFKpond plugin) { this.plugin = plugin; }
    
    @Override public String getName() { return "reload"; }
    @Override public String getDescription() { return "重新加载插件配置和数据"; }
    @Override public String getUsage() { return "reload"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.reload"; }
    @Override public boolean isPlayerOnly() { return false; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.sendPlayerMessage(sender, "&#B0E0E6正在重载YiyunAFKpond插件...");
        long startTime = System.currentTimeMillis();
        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getSecurityManager().reload();
            plugin.getUiManager().reload();
            plugin.getPondManager().loadPonds();
            plugin.getDataManager().loadAllPlayerData();
            plugin.getRewardManager().restartAllRewardTasks();
            plugin.rescanPlayersInPonds();
            long endTime = System.currentTimeMillis();
            plugin.sendPlayerMessage(sender, "&#87CEEB插件重载成功！&#ADD8E6耗时: &#B0E0E6" + (endTime - startTime) + "ms");
        } catch (Exception e) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD插件重载失败！错误信息: " + e.getMessage());
            plugin.getLogger().severe("插件重载失败: " + e.getMessage());
        }
        return true;
    }
}
