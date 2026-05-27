package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class YiyunAFKpondTabCompleter implements TabCompleter {
    private final YiyunAFKpond plugin;
    
    public YiyunAFKpondTabCompleter(YiyunAFKpond plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 检查命令是否是yiyunafkpond或其别名
        if (!command.getName().equalsIgnoreCase("yiyunafkpond") &&
            !command.getName().equalsIgnoreCase("yafkpond") &&
            !command.getName().equalsIgnoreCase("yafk")) {
            return completions;
        }
        
        // 处理不同的参数长度
        if (args.length == 1) {
            // 第一个参数：子命令名称
            List<String> subCommands = new ArrayList<>();
            
            // 添加所有已注册的子命令
            for (SubCommand subCommand : plugin.getCommandManager().getSubCommands().values()) {
                if (sender.hasPermission(subCommand.getPermission())) {
                    subCommands.add(subCommand.getName());
                }
            }
            
            // 过滤匹配的命令
            String partial = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length > 1) {
            // 第二个参数及以后：根据子命令类型提供不同的补全
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = plugin.getCommandManager().getSubCommand(subCommandName);
            
            if (subCommand != null) {
                // 根据子命令类型提供不同的补全
                switch (subCommandName) {
                    case "set":
                        if (args.length == 2) {
                            // 补全池ID
                            for (String pondId : plugin.getPondManager().getPonds().stream().map(p -> p.getId()).toList()) {
                                if (pondId.toLowerCase().startsWith(args[1].toLowerCase())) {
                                    completions.add(pondId);
                                }
                            }
                        } else if (args.length == 3) {
                            // 补全属性名
                            List<String> properties = List.of(
                                "expdistributionmode", "expinterval", 
                                "exprewardmode", "exprandommin", "exprandommax", "expfixedamount", "expmaxdaily", "expapplymending",
                                "xprate",
                                "moneyrewardmode", "moneyrandommin", "moneyrandommax", "moneyfixedamount",
                                "moneyinterval", "moneymaxdaily", "moneyrate",
                                "pointrewardmode", "pointrandommin", "pointrandommax", "pointfixedamount",
                                "pointinterval", "pointmaxdaily", "pointrate",
                                "requiredpermission", "enabled",
                                "entermessage", "leavemessage",
                                "exprewardmessage", "moneyrewardmessage", "pointrewardmessage",
                                "explimitmessage", "moneylimitmessage", "pointlimitmessage"
                            );
                            for (String property : properties) {
                                if (property.startsWith(args[2].toLowerCase())) {
                                    completions.add(property);
                                }
                            }
                        } else if (args.length == 4) {
                            // 补全属性值
                            String property = args[2].toLowerCase();
                            if (property.equals("expdistributionmode")) {
                                completions.addAll(List.of("interval", "continuous"));
                            } else if (property.equals("exprewardmode") || property.equals("moneyrewardmode") || property.equals("pointrewardmode")) {
                                completions.addAll(List.of("random", "fixed"));
                            } else if (property.equals("requiredpermission")) {
                                completions.add("null");
                            } else if (property.equals("enabled") || property.equals("expapplymending")) {
                                completions.addAll(List.of("true", "false"));
                            }
                        }
                        break;
                    case "info":
                    case "remove":
                    case "toggle":
                    case "tp":
                        if (args.length == 2) {
                            // 补全池ID
                            for (String pondId : plugin.getPondManager().getPonds().stream().map(p -> p.getId()).toList()) {
                                if (pondId.toLowerCase().startsWith(args[1].toLowerCase())) {
                                    completions.add(pondId);
                                }
                            }
                        }
                        break;
                    case "player":
                        if (args.length == 2) {
                            // 补全子命令
                            completions.addAll(List.of("info", "reset", "set", "stats", "list"));
                        } else if (args.length == 3) {
                            String playerSubCmd = args[1].toLowerCase();
                            if (playerSubCmd.equals("set")) {
                                // 补全玩家名
                                for (Player player : plugin.getServer().getOnlinePlayers()) {
                                    if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                                        completions.add(player.getName());
                                    }
                                }
                            } else if (playerSubCmd.equals("info") || playerSubCmd.equals("reset") || playerSubCmd.equals("stats")) {
                                // 补全玩家名
                                for (Player player : plugin.getServer().getOnlinePlayers()) {
                                    if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                                        completions.add(player.getName());
                                    }
                                }
                            }
                        } else if (args.length == 4) {
                            String playerSubCmd = args[1].toLowerCase();
                            if (playerSubCmd.equals("set")) {
                                // 补全字段名
                                completions.addAll(List.of("afk", "currentPondId", "pond"));
                            } else if (playerSubCmd.equals("reset")) {
                                completions.addAll(List.of("daily", "pond"));
                            }
                        } else if (args.length == 5) {
                            String playerSubCmd = args[1].toLowerCase();
                            if (playerSubCmd.equals("set")) {
                                String field = args[3].toLowerCase();
                                if (field.equals("afk")) {
                                    completions.addAll(List.of("true", "false"));
                                } else if (field.equals("currentpondid")) {
                                    completions.add("none");
                                    for (String pondId : plugin.getPondManager().getPonds().stream().map(p -> p.getId()).toList()) {
                                        completions.add(pondId);
                                    }
                                } else if (field.equals("pond")) {
                                    // 补全池ID
                                    for (String pondId : plugin.getPondManager().getPonds().stream().map(p -> p.getId()).toList()) {
                                        if (pondId.toLowerCase().startsWith(args[4].toLowerCase())) {
                                            completions.add(pondId);
                                        }
                                    }
                                }
                            } else if (playerSubCmd.equals("reset")) {
                                String type = args[3].toLowerCase();
                                if (type.equals("pond")) {
                                    // 补全池ID
                                    for (String pondId : plugin.getPondManager().getPonds().stream().map(p -> p.getId()).toList()) {
                                        if (pondId.toLowerCase().startsWith(args[4].toLowerCase())) {
                                            completions.add(pondId);
                                        }
                                    }
                                }
                            }
                        } else if (args.length == 6) {
                            String playerSubCmd = args[1].toLowerCase();
                            if (playerSubCmd.equals("set") && args[3].equals("pond")) {
                                // 补全数据类型
                                completions.addAll(List.of("exp", "money", "point"));
                            }
                        }
                        break;
                }
            }
        }
        
        return completions;
    }
}