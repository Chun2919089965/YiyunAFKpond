package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ListCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    
    public ListCommand(YiyunAFKpond plugin) { this.plugin = plugin; }
    
    @Override public String getName() { return "list"; }
    @Override public String getDescription() { return "显示所有已存在的AFK池"; }
    @Override public String getUsage() { return "list"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.list"; }
    @Override public boolean isPlayerOnly() { return false; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        List<Pond> ponds = plugin.getPondManager().getPonds();
        plugin.sendPlayerMessage(sender, "&#87CEEB===== AFK池列表 =====");
        if (ponds.isEmpty()) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD当前没有已创建的AFK池！");
        } else {
            for (Pond pond : ponds) {
                String status = pond.isEnabled() ? "&#87CEEB启用" : "&#6CA6CD禁用";
                plugin.sendPlayerMessage(sender, 
                        String.format("&#B0E0E6%s &#ADD8E6- &#87CEEB%s &#ADD8E6| %s &#ADD8E6| 世界: &#B0E0E6%s", 
                                pond.getId(), pond.getName(), status, pond.getWorld().getName()));
            }
        }
        plugin.sendPlayerMessage(sender, String.format("&#87CEEB===== 共 %d 个AFK池 =====", ponds.size()));
        return true;
    }
}
