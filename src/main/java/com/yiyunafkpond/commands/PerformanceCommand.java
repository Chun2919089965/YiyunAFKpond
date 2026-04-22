package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import org.bukkit.command.CommandSender;

public class PerformanceCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    public PerformanceCommand(YiyunAFKpond plugin) { this.plugin = plugin; }
    @Override public String getName() { return "perf"; }
    @Override public String getDescription() { return "查看性能监控统计"; }
    @Override public String getUsage() { return "perf <stats|reset>"; }
    @Override public String getPermission() { return "yiyunafkpond.performance"; }
    @Override public boolean isPlayerOnly() { return false; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.sendPlayerMessage(sender, "&#87CEEB=== YiyunAFKpond 性能监控 ===");
            plugin.sendPlayerMessage(sender, "&#B0E0E6/yap perf stats &#ADD8E6- 查看性能统计");
            plugin.sendPlayerMessage(sender, "&#B0E0E6/yap perf reset &#ADD8E6- 重置性能统计");
            return true;
        }
        
        Runtime runtime = Runtime.getRuntime();
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long freeMem = runtime.freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;
        long maxMem = runtime.maxMemory() / (1024 * 1024);
        
        switch (args[1].toLowerCase()) {
            case "stats":
                plugin.sendPlayerMessage(sender, "&#87CEEB=== 性能统计 ===");
                plugin.sendPlayerMessage(sender, "&#5B9BD5内存: &#B0E0E6" + usedMem + "MB / " + maxMem + "MB (&#87CEEB" + (maxMem > 0 ? (usedMem * 100 / maxMem) : 0) + "%&#B0E0E6)");
                plugin.sendPlayerMessage(sender, "&#5B9BD5缓存玩家数: &#B0E0E6" + plugin.getDataManager().getAllPlayerData().size());
                plugin.sendPlayerMessage(sender, "&#5B9BD5待保存队列: &#B0E0E6" + plugin.getDataManager().getPendingSaveCount());
                plugin.sendPlayerMessage(sender, "&#5B9BD5活跃池数: &#B0E0E6" + plugin.getPondManager().getPonds().size());
                plugin.sendPlayerMessage(sender, "&#5B9BD5调试模式: &#B0E0E6" + (plugin.isDebugMode() ? "&#87CEEB开启" : "&#6CA6CD关闭"));
                break;
            case "reset":
                plugin.sendPlayerMessage(sender, "&#87CEEB性能统计已重置！");
                break;
            default:
                plugin.sendPlayerMessage(sender, "&#6CA6CD未知命令！使用 /yap perf 查看帮助");
                break;
        }
        return true;
    }
}
