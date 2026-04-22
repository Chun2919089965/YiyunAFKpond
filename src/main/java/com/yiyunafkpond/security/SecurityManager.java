package com.yiyunafkpond.security;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public class SecurityManager {
    private final YiyunAFKpond plugin;
    
    private boolean teleportInterceptEnabled;
    private String teleportInterceptMessage;
    private String teleportBypassPermission;
    private boolean enterPermissionCheckEnabled;
    
    public SecurityManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        teleportInterceptEnabled = plugin.getConfig().getBoolean("security.teleport-intercept.enabled", true);
        teleportInterceptMessage = plugin.getConfig().getString("security.teleport-intercept.message", "&#6CA6CD您不能直接传送到挂机池区域！");
        teleportBypassPermission = plugin.getConfig().getString("security.teleport-intercept.bypass-permission", "yiyunafkpond.bypass.teleport");
        enterPermissionCheckEnabled = plugin.getConfig().getBoolean("security.enter-permission-check.enabled", true);
    }
    
    public void handlePlayerTeleport(PlayerTeleportEvent event) {
        if (!teleportInterceptEnabled) {
            return;
        }
        
        Player player = event.getPlayer();
        Pond targetPool = plugin.getPondManager().getPondByLocation(event.getTo());
        
        if (targetPool != null && !player.hasPermission(teleportBypassPermission)) {
            event.setCancelled(true);
            plugin.sendPlayerMessage(player, teleportInterceptMessage);
            plugin.sendPlayerMessage(player, plugin.getLanguageManager().getMessage("player.teleport-intercept"));
        } else if (targetPool != null && player.hasPermission(teleportBypassPermission)) {
            plugin.sendPlayerMessage(player, "&#87CEEB您已绕过传送限制，传送到挂机池区域");
        }
    }
    
    public boolean canPlayerEnterPool(Player player, Pond pond) {
        if (pond == null) {
            return true;
        }
        
        String requiredPermission = pond.getRequiredPermission();
        if (requiredPermission != null && !requiredPermission.isEmpty()) {
            return player.hasPermission(requiredPermission);
        }
        
        if (!enterPermissionCheckEnabled) {
            return true;
        }
        
        return player.hasPermission("yiyunafkpond.pool.*") ||
               player.hasPermission("yiyunafkpond.pool." + pond.getId());
    }
    
    public void reload() {
        loadConfig();
    }
    
    public boolean isTeleportInterceptEnabled() {
        return teleportInterceptEnabled;
    }
    
    public String getTeleportInterceptMessage() {
        return teleportInterceptMessage;
    }
    
    public String getTeleportBypassPermission() {
        return teleportBypassPermission;
    }
}
