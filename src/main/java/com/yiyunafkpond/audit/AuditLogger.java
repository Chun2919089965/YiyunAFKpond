package com.yiyunafkpond.audit;

import com.yiyunafkpond.YiyunAFKpond;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLogger {
    private final YiyunAFKpond plugin;
    private final File auditLogFile;
    private final DateTimeFormatter dateFormat;
    
    public AuditLogger(YiyunAFKpond plugin) {
        this.plugin = plugin;
        this.auditLogFile = new File(plugin.getDataFolder(), "audit.log");
        this.dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
    
    public synchronized void log(CommandSender admin, String action, String target, String details) {
        String adminName = admin instanceof Player ? ((Player) admin).getName() : "Console";
        String timestamp = LocalDateTime.now().format(dateFormat);
        String logEntry = String.format("[%s] [%s] %s - %s: %s", timestamp, adminName, action, target, details);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(auditLogFile, true))) {
            writer.println(logEntry);
        } catch (IOException e) {
            plugin.getLogger().warning("无法写入审计日志: " + e.getMessage());
        }
        
        plugin.getLogger().info(logEntry);
    }
    
    public void logPoolCreate(CommandSender admin, String poolId, String poolName) {
        log(admin, "CREATE", "池 " + poolId, "名称: " + poolName);
    }
    
    public void logPoolDelete(CommandSender admin, String poolId) {
        log(admin, "DELETE", "池 " + poolId, "删除池");
    }
    
    public void logPoolModify(CommandSender admin, String poolId, String property, String oldValue, String newValue) {
        log(admin, "MODIFY", "池 " + poolId, property + ": " + oldValue + " -> " + newValue);
    }
    
    public void logPoolToggle(CommandSender admin, String poolId, boolean enabled) {
        log(admin, "TOGGLE", "池 " + poolId, enabled ? "启用" : "禁用");
    }
    
    public void logPlayerReset(CommandSender admin, String playerName) {
        log(admin, "RESET", "玩家 " + playerName, "重置数据");
    }
    
    public void logBackup(CommandSender admin, String backupFile) {
        log(admin, "BACKUP", "数据备份", "文件: " + backupFile);
    }
    
    public void logRestore(CommandSender admin, String backupFile) {
        log(admin, "RESTORE", "数据恢复", "文件: " + backupFile);
    }
    
    public void logConfigReload(CommandSender admin) {
        log(admin, "RELOAD", "配置", "重载配置");
    }
}
