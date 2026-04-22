package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SelectionCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    public SelectionCommand(YiyunAFKpond plugin) { this.plugin = plugin; }
    @Override public String getName() { return "selection"; }
    @Override public String getDescription() { return "获取选区工具"; }
    @Override public String getUsage() { return "selection"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.create"; }
    @Override public boolean isPlayerOnly() { return true; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD此命令只能由玩家执行!");
            return true;
        }
        Player player = (Player) sender;
        plugin.getSelectionManager().giveSelectionTool(player);
        return true;
    }
}
