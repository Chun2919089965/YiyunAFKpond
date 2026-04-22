package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import org.bukkit.command.CommandSender;

public class DebugCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    public DebugCommand(YiyunAFKpond plugin) { this.plugin = plugin; }
    @Override public String getName() { return "debug"; }
    @Override public String getDescription() { return "切换调试模式"; }
    @Override public String getUsage() { return "debug"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.debug"; }
    @Override public boolean isPlayerOnly() { return false; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        boolean debugMode = !plugin.isDebugMode();
        plugin.setDebugMode(debugMode);
        String status = debugMode ? "&#87CEEB开启" : "&#6CA6CD关闭";
        plugin.sendPlayerMessage(sender, "&#B0E0E6调试模式已" + status + "&#B0E0E6！");
        return true;
    }
}
