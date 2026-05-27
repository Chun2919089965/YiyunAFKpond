package com.yiyunafkpond.security;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityManager {
    private final YiyunAFKpond plugin;

    private boolean teleportInterceptEnabled;
    private String teleportInterceptMessage;
    private String teleportBypassPermission;
    private boolean enterPermissionCheckEnabled;

    private boolean ipLimitEnabled;
    private String ipLimitMode;
    private int maxPerIp;
    private String ipLimitBypassPermission;
    private String ipLimitMessage;

    private final Map<String, Map<String, Set<UUID>>> ipPoolIndex = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerIpCache = new ConcurrentHashMap<>();

    public SecurityManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        teleportInterceptEnabled = plugin.getConfig().getBoolean("security.teleport-intercept.enabled", true);
        teleportInterceptMessage = plugin.getConfig().getString("security.teleport-intercept.message", "&#6CA6CD您不能直接传送到挂机池区域！");
        teleportBypassPermission = plugin.getConfig().getString("security.teleport-intercept.bypass-permission", "yiyunafkpond.bypass.teleport");
        enterPermissionCheckEnabled = plugin.getConfig().getBoolean("security.enter-permission-check.enabled", true);

        ipLimitEnabled = plugin.getConfig().getBoolean("security.ip-limit.enabled", false);
        ipLimitMode = plugin.getConfig().getString("security.ip-limit.mode", "global");
        maxPerIp = plugin.getConfig().getInt("security.ip-limit.max-per-ip", 1);
        ipLimitBypassPermission = plugin.getConfig().getString("security.ip-limit.bypass-permission", "yiyunafkpond.bypass.ip");
        ipLimitMessage = plugin.getConfig().getString("security.ip-limit.message", "&#6CA6CD检测到同IP已有其他角色在挂机池中，已阻止进入！");
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

    public boolean canPlayerEnterPoolByIp(Player player, Pond pond) {
        if (!ipLimitEnabled || pond == null) {
            return true;
        }

        if (player.hasPermission(ipLimitBypassPermission)) {
            return true;
        }

        String ip = getPlayerIp(player);
        if (ip == null) {
            return true;
        }

        if ("per-pool".equalsIgnoreCase(ipLimitMode)) {
            int count = getIpCountForPool(ip, pond.getId());
            return count < maxPerIp;
        } else {
            int count = getIpCountGlobal(ip);
            return count < maxPerIp;
        }
    }

    public void onPlayerEnterPool(Player player, String pondId) {
        String ip = getPlayerIp(player);
        if (ip == null) return;

        playerIpCache.put(player.getUniqueId(), ip);
        ipPoolIndex.computeIfAbsent(ip, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(pondId, k -> ConcurrentHashMap.newKeySet())
                .add(player.getUniqueId());
    }

    public void onPlayerLeavePool(Player player, String pondId) {
        String ip = playerIpCache.remove(player.getUniqueId());
        if (ip == null) return;

        Map<String, Set<UUID>> poolMap = ipPoolIndex.get(ip);
        if (poolMap == null) return;

        Set<UUID> players = poolMap.get(pondId);
        if (players != null) {
            players.remove(player.getUniqueId());
            if (players.isEmpty()) {
                poolMap.remove(pondId);
            }
        }
        if (poolMap.isEmpty()) {
            ipPoolIndex.remove(ip);
        }
    }

    public void onPlayerQuit(UUID uuid) {
        String ip = playerIpCache.remove(uuid);
        if (ip == null) return;

        Map<String, Set<UUID>> poolMap = ipPoolIndex.get(ip);
        if (poolMap == null) return;

        Iterator<Map.Entry<String, Set<UUID>>> it = poolMap.entrySet().iterator();
        while (it.hasNext()) {
            Set<UUID> players = it.next().getValue();
            players.remove(uuid);
            if (players.isEmpty()) {
                it.remove();
            }
        }
        if (poolMap.isEmpty()) {
            ipPoolIndex.remove(ip);
        }
    }

    public void onPlayerSwitchPool(Player player, String oldPondId, String newPondId) {
        onPlayerLeavePool(player, oldPondId);
        onPlayerEnterPool(player, newPondId);
    }

    public void clearIpIndex() {
        ipPoolIndex.clear();
        playerIpCache.clear();
    }

    private int getIpCountForPool(String ip, String pondId) {
        Map<String, Set<UUID>> poolMap = ipPoolIndex.get(ip);
        if (poolMap == null) return 0;
        Set<UUID> players = poolMap.get(pondId);
        return players == null ? 0 : players.size();
    }

    private int getIpCountGlobal(String ip) {
        Map<String, Set<UUID>> poolMap = ipPoolIndex.get(ip);
        if (poolMap == null) return 0;
        int total = 0;
        for (Set<UUID> players : poolMap.values()) {
            total += players.size();
        }
        return total;
    }

    private String getPlayerIp(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null || address.getAddress() == null) return null;
        return address.getAddress().getHostAddress();
    }

    public String getIpLimitMessage() {
        return ipLimitMessage;
    }

    public boolean isIpLimitEnabled() {
        return ipLimitEnabled;
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
