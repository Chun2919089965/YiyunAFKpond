package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class CommandManager implements CommandExecutor {
    private final YiyunAFKpond plugin;
    private final Map<String, SubCommand> subCommands;
    
    public CommandManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        
        registerSubCommands();
        
        plugin.getCommand("yiyunafkpond").setExecutor(this);
    }
    
    private void registerSubCommands() {
        registerSubCommand(new HelpCommand());
        registerSubCommand(new SelectionCommand(plugin));
        registerSubCommand(new CreateCommand(plugin));
        registerSubCommand(new RemoveCommand(plugin));
        registerSubCommand(new ListCommand(plugin));
        registerSubCommand(new ReloadCommand(plugin));
        registerSubCommand(new SetCommand(plugin));
        registerSubCommand(new InfoCommand(plugin));
        registerSubCommand(new ToggleCommand(plugin));
        registerSubCommand(new DebugCommand(plugin));
        registerSubCommand(new TpCommand(plugin));
        registerSubCommand(new ResetCommand(plugin));
        registerSubCommand(new StatsCommand(plugin));
        registerSubCommand(new PlayerSubCommand(plugin));
        registerSubCommand(new PerformanceCommand(plugin));
        
        plugin.getLogger().info("已注册 " + subCommands.size() + " 个子命令!");
    }
    
    public void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }
    
    public Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }
    
    public SubCommand getSubCommand(String name) {
        return subCommands.get(name.toLowerCase());
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        
        if (subCommand == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD未知命令! 请使用 /yap help 查看帮助信息。");
            return true;
        }
        
        if (!sender.hasPermission(subCommand.getPermission())) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD你没有权限执行此命令!");
            return true;
        }
        
        if (subCommand.isPlayerOnly() && !(sender instanceof Player)) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD此命令只能由玩家执行!");
            return true;
        }
        
        return subCommand.execute(sender, args);
    }
    
    private void showHelp(CommandSender sender) {
        plugin.sendPlayerMessage(sender, "&#87CEEB===== YiyunAFKpond 帮助信息 =====");
        plugin.sendPlayerMessage(sender, "&#B0E0E6/yiyunafkpond help &#ADD8E6- 显示此帮助信息");
        plugin.sendPlayerMessage(sender, "&#B0E0E6别名: /yafkpond, /yafk, /yap");
        
        for (SubCommand subCommand : subCommands.values()) {
            if (sender.hasPermission(subCommand.getPermission())) {
                plugin.sendPlayerMessage(sender, "&#B0E0E6/yap " + subCommand.getUsage() + " &#ADD8E6- " + subCommand.getDescription());
            }
        }
        
        plugin.sendPlayerMessage(sender, "&#87CEEB==============================");
    }
    
    private class HelpCommand implements SubCommand {
        @Override
        public String getName() { return "help"; }
        @Override
        public String getDescription() { return "显示帮助信息"; }
        @Override
        public String getUsage() { return "help"; }
        @Override
        public String getPermission() { return "yiyunafkpond.user"; }
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            showHelp(sender);
            return true;
        }
        @Override
        public boolean isPlayerOnly() { return false; }
    }
}
