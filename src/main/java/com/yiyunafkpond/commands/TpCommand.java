package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    public TpCommand(YiyunAFKpond plugin) { this.plugin = plugin; }
    @Override public String getName() { return "tp"; }
    @Override public String getDescription() { return "传送到指定挂机池"; }
    @Override public String getUsage() { return "tp <池ID>"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.tp"; }
    @Override public boolean isPlayerOnly() { return true; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD只有玩家才能执行此命令！");
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD用法：/yap tp <池ID>");
            return true;
        }
        String pondId = args[1];
        Pond pond = plugin.getPondManager().getPond(pondId);
        if (pond == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD未找到指定的挂机池！");
            return true;
        }
        if (!pond.isEnabled()) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD该挂机池已禁用！");
            return true;
        }
        if (player.hasPermission("yiyunafkpond.bypass.teleport") || player.hasPermission("yiyunafkpond.pond." + pondId)) {
            player.teleportAsync(pond.getCenterLocation()).thenAccept(success -> {
                if (success) {
                    plugin.sendPlayerMessage(player, "&#87CEEB已传送到挂机池：&#B0E0E6" + pond.getName());
                } else {
                    plugin.sendPlayerMessage(player, "&#6CA6CD传送失败！");
                }
            });
        } else {
            plugin.sendPlayerMessage(player, "&#6CA6CD您没有权限传送到该挂机池！");
        }
        return true;
    }
}
