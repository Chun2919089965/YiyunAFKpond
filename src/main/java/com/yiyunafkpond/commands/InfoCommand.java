package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.command.CommandSender;

public class InfoCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    public InfoCommand(YiyunAFKpond plugin) { this.plugin = plugin; }
    @Override public String getName() { return "info"; }
    @Override public String getDescription() { return "查看挂机池信息"; }
    @Override public String getUsage() { return "info <池ID>"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.info"; }
    @Override public boolean isPlayerOnly() { return false; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD用法：/yap info <池ID>");
            return true;
        }
        String pondId = args[1];
        Pond pond = plugin.getPondManager().getPond(pondId);
        if (pond == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD未找到指定的挂机池！");
            return true;
        }
        plugin.sendPlayerMessage(sender, "&#87CEEB=== 挂机池信息 ===");
        plugin.sendPlayerMessage(sender, "&#5B9BD5ID: &#B0E0E6" + pond.getId());
        plugin.sendPlayerMessage(sender, "&#5B9BD5名称: &#B0E0E6" + pond.getName());
        plugin.sendPlayerMessage(sender, "&#5B9BD5状态: &#B0E0E6" + (pond.isEnabled() ? "启用" : "禁用"));
        plugin.sendPlayerMessage(sender, "&#5B9BD5世界: &#B0E0E6" + pond.getWorld().getName());
        plugin.sendPlayerMessage(sender, "&#5B9BD5位置: &#B0E0E6X:" + pond.getMinX() + "-" + pond.getMaxX() + ", Y:" + pond.getMinY() + "-" + pond.getMaxY() + ", Z:" + pond.getMinZ() + "-" + pond.getMaxZ());
        plugin.sendPlayerMessage(sender, "&#5B9BD5大小: &#B0E0E6" + pond.getSize() + " 方块");
        plugin.sendPlayerMessage(sender, "&#5B9BD5玩家数量: &#B0E0E6" + plugin.getPondManager().getPlayersInPond(pond).size());
        plugin.sendPlayerMessage(sender, "&#5B9BD5经验间隔: &#B0E0E6" + pond.getExpInterval() + " 秒");
        plugin.sendPlayerMessage(sender, "&#5B9BD5金币间隔: &#B0E0E6" + pond.getMoneyInterval() + " 秒");
        plugin.sendPlayerMessage(sender, "&#87CEEB================");
        return true;
    }
}
