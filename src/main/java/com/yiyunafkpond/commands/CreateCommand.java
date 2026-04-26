package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.pond.Pond;
import com.yiyunafkpond.pond.selection.PondSelection;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateCommand implements SubCommand {
    private final YiyunAFKpond plugin;
    
    public CreateCommand(YiyunAFKpond plugin) {
        this.plugin = plugin;
    }
    
    @Override public String getName() { return "create"; }
    @Override public String getDescription() { return "创建新的AFK池"; }
    @Override public String getUsage() { return "create <池ID> <池名称>"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.create"; }
    @Override public boolean isPlayerOnly() { return true; }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD此命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 3) {
            plugin.sendPlayerMessage(player, "&#6CA6CD使用方法: /yap create <池ID> <池名称>");
            return true;
        }
        
        String poolId = args[1].toLowerCase();
        
        if (poolId.isEmpty() || poolId.contains("../") || poolId.contains("./") || poolId.contains(":") || poolId.contains("\\")) {
            plugin.sendPlayerMessage(player, "&#6CA6CD无效的池ID！");
            return true;
        }
        
        StringBuilder poolNameBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            poolNameBuilder.append(args[i]).append(" ");
        }
        String poolName = poolNameBuilder.toString().trim();
        
        if (plugin.getPondManager().getPond(poolId) != null) {
            plugin.sendPlayerMessage(player, "&#6CA6CD池ID已存在！");
            return true;
        }
        
        PondSelection selection = plugin.getSelectionManager().getSelection(player);
        if (selection == null || !selection.isComplete()) {
            plugin.sendPlayerMessage(player, "&#6CA6CD请先使用选区工具选择区域！");
            plugin.sendPlayerMessage(player, "&#ADD8E6使用 /yap selection 获取选区工具");
            return true;
        }
        
        Pond pond = plugin.getPondManager().createPond(poolId, poolName, selection.getFirstPoint().getWorld(), 
                selection.getFirstPoint(), selection.getSecondPoint());
        
        if (pond != null) {
            plugin.sendPlayerMessage(player, "&#87CEEB成功创建AFK池: &#B0E0E6" + poolName + " &#ADD8E6(" + poolId + ")");
            plugin.sendPlayerMessage(player, "&#ADD8E6区域大小: &#B0E0E6" + selection.getSize() + " &#ADD8E6方块");
            plugin.getSelectionManager().clearSelection(player);
            plugin.getPondManager().savePonds(true);
            plugin.getRewardManager().startPoolRewardTasks(pond);
            plugin.rescanPlayersInPonds();
            plugin.getAuditLogger().logPoolCreate(sender, poolId, poolName);
        } else {
            plugin.sendPlayerMessage(player, "&#6CA6CD创建AFK池失败！");
        }
        
        return true;
    }
}
