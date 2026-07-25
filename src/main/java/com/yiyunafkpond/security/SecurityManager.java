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
    private final Object ipIndexLock = new Object();

    public SecurityManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        teleportInterceptEnabled = plugin.getConfig().getBoolean("security.teleport-intercept.enabled", true);
        teleportInterceptMessage = plugin.getConfig().getString("security.teleport-intercept.message", "&#6CA6CD您不能直接传送到挂机池区域！");
        teleportBypassPermission = plugin.getConfig().getString("security.teleport-intercept.bypass-permission", "yiyunafkpond.bypass.teleport");
        enterPermissionCheckEnabled = plugin.getConfig().getBoolean("security.enter-permission-check.enabled", false);

        ipLimitEnabled = plugin.getConfig().getBoolean("security.ip-limit.enabled", false);
        ipLimitMode = plugin.getConfig().getString("security.ip-limit.mode", "global");
        maxPerIp = Math.max(1, plugin.getConfig().getInt("security.ip-limit.max-per-ip", 1));
        ipLimitBypassPermission = plugin.getConfig().getString("security.ip-limit.bypass-permission", "yiyunafkpond.bypass.ip");
        ipLimitMessage = plugin.getConfig().getString("security.ip-limit.message", "&#6CA6CD检测到同IP已有其他角色在挂机池中，已阻止进入！");
    }

    public void handlePlayerTeleport(PlayerTeleportEvent event) {
        if (!teleportInterceptEnabled) {
            return;
        }

        Player player = event.getPlayer();
        Pond targetPool = plugin.getPondManager().getPondByLocation(event.getTo());
        Pond sourcePool = plugin.getPondManager().getPondByLocation(event.getFrom());
        boolean staysInSamePool = sourcePool != null && targetPool != null
                && sourcePool.getId().equals(targetPool.getId());

        if (targetPool != null && !staysInSamePool && !player.hasPermission(teleportBypassPermission)) {
            event.setCancelled(true);
            plugin.sendPlayerMessage(player, teleportInterceptMessage);
            plugin.sendPlayerMessage(player, plugin.getLanguageManager().getMessage("player.teleport-intercept"));
        } else if (targetPool != null && !staysInSamePool && player.hasPermission(teleportBypassPermission)) {
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
        synchronized (ipIndexLock) {
            return canPlayerEnterPoolByIpLocked(player, pond);
        }
    }

    private boolean canPlayerEnterPoolByIpLocked(Player player, Pond pond) {
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
            int count = getOtherPlayerCountForPool(ip, pond.getId(), player.getUniqueId());
            return count < maxPerIp;
        } else {
            int count = getOtherPlayerCountGlobal(ip, player.getUniqueId());
            return count < maxPerIp;
        }
    }

    /**
     * 原子执行 IP 准入检查与索引登记，避免 Folia 多区域并发进入时突破人数上限。
     */
    public boolean tryRegisterPlayerInPool(Player player, Pond pond) {
        if (pond == null) return false;
        synchronized (ipIndexLock) {
            if (!canPlayerEnterPoolByIpLocked(player, pond)) return false;
            registerPlayerInPoolLocked(player, pond.getId());
            return true;
        }
    }

    public void onPlayerEnterPool(Player player, String pondId) {
        synchronized (ipIndexLock) {
            registerPlayerInPoolLocked(player, pondId);
        }
    }

    private void registerPlayerInPoolLocked(Player player, String pondId) {
        String ip = getPlayerIp(player);
        if (ip == null) return;

        // 重复同步和跨池切换都只允许保留一份 IP 索引。
        removePlayerFromIpIndexLocked(player.getUniqueId());
        playerIpCache.put(player.getUniqueId(), ip);
        ipPoolIndex.computeIfAbsent(ip, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(pondId, k -> ConcurrentHashMap.newKeySet())
                .add(player.getUniqueId());
    }

    public void onPlayerLeavePool(Player player, String pondId) {
        synchronized (ipIndexLock) {
            removePlayerFromIpIndexLocked(player.getUniqueId());
        }
    }

    private void removePlayerFromIpIndexLocked(UUID uuid) {
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

    public void onPlayerQuit(UUID uuid) {
        synchronized (ipIndexLock) {
            removePlayerFromIpIndexLocked(uuid);
        }
    }

    public void onPlayerSwitchPool(Player player, String oldPondId, String newPondId) {
        onPlayerEnterPool(player, newPondId);
    }

    public void clearIpIndex() {
        synchronized (ipIndexLock) {
            ipPoolIndex.clear();
            playerIpCache.clear();
        }
    }

    private int getOtherPlayerCountForPool(String ip, String pondId, UUID excludedPlayer) {
        Map<String, Set<UUID>> poolMap = ipPoolIndex.get(ip);
        if (poolMap == null) return 0;
        Set<UUID> players = poolMap.get(pondId);
        return countOtherPlayers(players, excludedPlayer);
    }

    private int getOtherPlayerCountGlobal(String ip, UUID excludedPlayer) {
        Map<String, Set<UUID>> poolMap = ipPoolIndex.get(ip);
        if (poolMap == null) return 0;
        Set<UUID> uniquePlayers = new HashSet<>();
        for (Set<UUID> players : poolMap.values()) {
            uniquePlayers.addAll(players);
        }
        uniquePlayers.remove(excludedPlayer);
        return uniquePlayers.size();
    }

    static int countOtherPlayers(Set<UUID> players, UUID excludedPlayer) {
        if (players == null || players.isEmpty()) return 0;
        int total = 0;
        for (UUID uuid : players) {
            if (!uuid.equals(excludedPlayer)) total++;
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
