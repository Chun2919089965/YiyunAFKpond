package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.command.CommandSender;

public class RemoveCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    
    public RemoveCommand(YiyunAFKpond plugin) { this.plugin = plugin; }
    
    @Override public String getName() { return "remove"; }
    @Override public String getDescription() { return "删除已存在的AFK池"; }
    @Override public String getUsage() { return "remove <池ID>"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.delete"; }
    @Override public boolean isPlayerOnly() { return false; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD使用方法: /yap remove <池ID>");
            return true;
        }
        String poolId = args[1].toLowerCase();
        Pond pond = plugin.getPondManager().getPond(poolId);
        if (pond == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD池ID不存在！");
            return true;
        }
        String poolName = pond.getName();
        plugin.getRewardManager().stopPoolRewardTasks(pond);
        plugin.getPondManager().removePond(poolId);
        plugin.sendPlayerMessage(sender, "&#87CEEB成功删除AFK池: &#B0E0E6" + poolName + " &#ADD8E6(" + poolId + ")");
        plugin.getPondManager().savePonds(true);
        plugin.getAuditLogger().logPoolDelete(sender, poolId);
        return true;
    }
}
