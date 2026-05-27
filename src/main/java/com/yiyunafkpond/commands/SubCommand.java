package com.yiyunafkpond.commands;

import org.bukkit.command.CommandSender;

public interface SubCommand {
    String getName();
    String getDescription();
    String getUsage();
    String getPermission();
    boolean execute(CommandSender sender, String[] args);
    boolean isPlayerOnly();
}