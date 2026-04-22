package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.command.CommandSender;

public class ToggleCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    public ToggleCommand(YiyunAFKpond plugin) { this.plugin = plugin; }
    @Override public String getName() { return "toggle"; }
    @Override public String getDescription() { return "启用/禁用挂机池"; }
    @Override public String getUsage() { return "toggle <池ID>"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.toggle"; }
    @Override public boolean isPlayerOnly() { return false; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD用法：/yap toggle <池ID>");
            return true;
        }
        String pondId = args[1];
        Pond pond = plugin.getPondManager().getPond(pondId);
        if (pond == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD未找到指定的挂机池！");
            return true;
        }
        pond.setEnabled(!pond.isEnabled());
        String status = pond.isEnabled() ? "&#87CEEB启用" : "&#6CA6CD禁用";
        plugin.sendPlayerMessage(sender, "&#87CEEB成功将挂机池 &#B0E0E6" + pond.getName() + " &#ADD8E6[" + status + "&#ADD8E6]");
        plugin.getPondManager().savePonds(true);
        plugin.getRewardManager().startPoolRewardTasks(pond);
        return true;
    }
}
