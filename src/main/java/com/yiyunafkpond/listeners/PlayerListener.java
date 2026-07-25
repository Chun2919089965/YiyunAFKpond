package com.yiyunafkpond.listeners;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import com.yiyunafkpond.util.ColorUtil;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PlayerListener implements Listener {
    private final YiyunAFKpond plugin;
    
    private boolean titleEnabled;
    private String enterTitle;
    private String enterSubtitle;
    private String leaveTitle;
    private String leaveSubtitle;
    private Duration titleFadeIn;
    private Duration titleStay;
    private Duration titleFadeOut;
    private WrappedTask presenceReconciliationTask;
    private final Map<UUID, Long> delayedSyncGenerations = new ConcurrentHashMap<>();
    private final Map<UUID, Location> internalTeleportTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pendingMountReturns = new ConcurrentHashMap<>();
    private final AtomicLong delayedSyncSequence = new AtomicLong();

    private enum SyncResult {
        SYNCHRONIZED,
        DENIED_PERMISSION,
        DENIED_IP;

        private boolean isDenied() {
            return this != SYNCHRONIZED;
        }
    }
    
    public PlayerListener(YiyunAFKpond plugin) {
        this.plugin = plugin;
        reloadTitleSettings();
    }

    public void reloadTitleSettings() {
        this.titleEnabled = plugin.getConfig().getBoolean("display.title.enabled", true);
        this.enterTitle = plugin.getConfig().getString("display.title.enter-title", "&#87CEEB欢迎来到 &#B0E0E6{pool_name}");
        this.enterSubtitle = plugin.getConfig().getString("display.title.enter-subtitle", "&#ADD8E6享受您的挂机时光~");
        this.leaveTitle = plugin.getConfig().getString("display.title.leave-title", "&#87CEEB感谢光临");
        this.leaveSubtitle = plugin.getConfig().getString("display.title.leave-subtitle", "&#ADD8E6期待下次再见~");
        
        int fadeIn = plugin.getConfig().getInt("display.title.fade-in", 10);
        int stay = plugin.getConfig().getInt("display.title.stay", 40);
        int fadeOut = plugin.getConfig().getInt("display.title.fade-out", 10);
        this.titleFadeIn = Duration.ofMillis(fadeIn * 50L);
        this.titleStay = Duration.ofMillis(stay * 50L);
        this.titleFadeOut = Duration.ofMillis(fadeOut * 50L);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getDataManager().getOrCreatePlayerData(player);
        schedulePresenceSync(player, 2L, false, null);
        plugin.debug("玩家 " + player.getName() + " 已加入服务器，已初始化数据!");
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        PlayerData playerData = plugin.getDataManager().getPlayerData(uuid);
        
        String currentPondId = null;
        if (playerData != null) {
            currentPondId = playerData.getCurrentPondId();
            plugin.getDataManager().savePlayerData(playerData);
        }
        
        plugin.getUiManager().onPlayerQuit(player);
        if (plugin.getRewardManager() != null) {
            plugin.getRewardManager().cleanupPlayerData(uuid);
        }
        plugin.getSecurityManager().onPlayerQuit(uuid);
        plugin.getPondManager().removePlayerFromAllPools(uuid);
        plugin.getSelectionManager().cancelParticleRender(player);
        delayedSyncGenerations.remove(uuid);
        internalTeleportTargets.remove(uuid);
        pendingMountReturns.remove(uuid);
        
        if (currentPondId != null && playerData != null) {
            Pond pond = plugin.getPondManager().getPond(currentPondId);
            if (pond != null) {
                playerData.setCurrentPondId(null);
                playerData.setAfk(false);
            }
        }
        
        plugin.getDataManager().removePlayerData(uuid);
        plugin.debug("玩家 " + player.getName() + " 已离开服务器，数据已保存!");
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (pendingMountReturns.containsKey(player.getUniqueId())) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (!hasBlockPositionChanged(from, to)) return;
        
        syncPlayerPondState(player, to, true, event, true);
    }
    
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        boolean internalTeleport = consumeInternalTeleport(player, to);
        if (!internalTeleport) {
            plugin.getSecurityManager().handlePlayerTeleport(event);
        }

        if (event.isCancelled()) return;

        if (!hasBlockPositionChanged(from, to)) return;

        SyncResult result = syncPlayerPondState(player, to, !internalTeleport, event, true);
        if (result.isDenied() || event.isCancelled()) return;

        // 在传送完成后使用玩家实体调度器复核，兼容 Folia 并防御其他插件后续取消事件。
        schedulePresenceSync(player, 2L, false, null);
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData playerData = plugin.getDataManager().getOrCreatePlayerData(player);
        
        String currentPondId = playerData.getCurrentPondId();
        if (currentPondId != null) {
            handlePlayerLeavePool(player, currentPondId, playerData, false);
        }
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        schedulePresenceSync(event.getPlayer(), 2L, true, null);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        schedulePresenceSync(player, 1L, true, null);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Location returnLocation = player.getLocation().clone();
        pendingMountReturns.put(player.getUniqueId(), returnLocation);
        schedulePresenceSync(player, 2L, true, returnLocation);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        pendingMountReturns.remove(player.getUniqueId());
        schedulePresenceSync(player, 2L, true, null);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!plugin.getSelectionManager().isSelectionTool(item)) return;
        
        Action action = event.getAction();
        
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return;
        
        event.setCancelled(true);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        
        if (event.getClickedBlock() == null) return;
        
        Location location = event.getClickedBlock().getLocation().add(0.5, 0, 0.5);
        
        if (action == Action.LEFT_CLICK_BLOCK) {
            plugin.getSelectionManager().setFirstPoint(player, location);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            plugin.getSelectionManager().setSecondPoint(player, location);
        }
    }
    
    private boolean hasBlockPositionChanged(Location from, Location to) {
        if (to == null) return false;
        if (!Objects.equals(from.getWorld(), to.getWorld())) return true;
        return from.getBlockX() != to.getBlockX() ||
               from.getBlockY() != to.getBlockY() ||
               from.getBlockZ() != to.getBlockZ();
    }
    
    /**
     * 按玩家最终位置修正挂机池状态。该方法是移动、传送、骑乘和周期自愈的唯一入口。
     */
    public void reconcilePlayerPondState(Player player, boolean sendMessage) {
        if (player == null || !player.isOnline() || !plugin.isPondsLoaded()) return;
        if (pendingMountReturns.containsKey(player.getUniqueId())) return;
        syncPlayerPondState(player, player.getLocation(), sendMessage, null, false);
    }

    private SyncResult syncPlayerPondState(Player player, Location targetLocation,
                                           boolean sendMessage, Cancellable cancellableEvent,
                                           boolean preserveCurrentStateOnDenial) {
        if (!plugin.isPondsLoaded()) return SyncResult.SYNCHRONIZED;
        PlayerData playerData = plugin.getDataManager().getOrCreatePlayerData(player);
        String storedPondId = playerData.getCurrentPondId();
        boolean hasCompleteState = playerData.isAfk() && storedPondId != null;
        Pond currentPond = hasCompleteState ? plugin.getPondManager().getPond(storedPondId) : null;

        // 修复重载或异常中断留下的半状态，防止一个玩家同时残留在多个池的索引中。
        if (!hasCompleteState && (playerData.isAfk() || storedPondId != null)) {
            clearInvalidPresence(player, playerData, storedPondId);
            storedPondId = null;
        } else if (hasCompleteState && currentPond == null) {
            clearInvalidPresence(player, playerData, storedPondId);
            storedPondId = null;
        }

        Pond targetPond = plugin.getPondManager().getPondByLocation(targetLocation);

        if (targetPond == null) {
            if (storedPondId != null) {
                handlePlayerLeavePool(player, storedPondId, playerData, sendMessage);
            } else {
                plugin.getSecurityManager().onPlayerQuit(player.getUniqueId());
                plugin.getPondManager().removePlayerFromAllPools(player.getUniqueId());
            }
            return SyncResult.SYNCHRONIZED;
        }

        if (targetPond.getId().equals(storedPondId)) {
            if (!plugin.getSecurityManager().canPlayerEnterPool(player, targetPond)) {
                handlePlayerLeavePool(player, storedPondId, playerData, false);
                denyEntry(player, SyncResult.DENIED_PERMISSION, sendMessage, cancellableEvent);
                return SyncResult.DENIED_PERMISSION;
            }
            if (!plugin.getSecurityManager().tryRegisterPlayerInPool(player, targetPond)) {
                handlePlayerLeavePool(player, storedPondId, playerData, false);
                denyEntry(player, SyncResult.DENIED_IP, sendMessage, cancellableEvent);
                return SyncResult.DENIED_IP;
            }

            // 状态相同但索引可能在热重载时被清空，按需自愈且不重复提示。
            if (!plugin.getPondManager().isPlayerTracked(targetPond.getId(), player.getUniqueId())) {
                plugin.getPondManager().addPlayerToPool(targetPond.getId(), player.getUniqueId());
                plugin.getUiManager().registerPlayerForUpdate(player);
            }
            return SyncResult.SYNCHRONIZED;
        }

        SyncResult accessResult = checkEntryAccess(player, targetPond);
        if (accessResult.isDenied()) {
            if (!preserveCurrentStateOnDenial && storedPondId != null) {
                handlePlayerLeavePool(player, storedPondId, playerData, false);
            }
            denyEntry(player, accessResult, sendMessage, cancellableEvent);
            return accessResult;
        }

        if (storedPondId == null) {
            handlePlayerEnterPool(player, targetPond, playerData, sendMessage);
        } else {
            Pond storedPond = plugin.getPondManager().getPond(storedPondId);
            if (storedPond == null) {
                clearInvalidPresence(player, playerData, storedPondId);
                handlePlayerEnterPool(player, targetPond, playerData, sendMessage);
            } else {
                handlePlayerSwitchPool(player, storedPond, targetPond, playerData, sendMessage);
            }
        }
        return SyncResult.SYNCHRONIZED;
    }

    private SyncResult checkEntryAccess(Player player, Pond pond) {
        if (!plugin.getSecurityManager().canPlayerEnterPool(player, pond)) {
            return SyncResult.DENIED_PERMISSION;
        }
        if (!plugin.getSecurityManager().tryRegisterPlayerInPool(player, pond)) {
            return SyncResult.DENIED_IP;
        }
        return SyncResult.SYNCHRONIZED;
    }

    private void denyEntry(Player player, SyncResult result, boolean sendMessage,
                           Cancellable cancellableEvent) {
        if (sendMessage) {
            String message = result == SyncResult.DENIED_IP
                    ? plugin.getSecurityManager().getIpLimitMessage()
                    : plugin.getLanguageManager().getMessage("player.no-permission");
            plugin.sendPlayerMessage(player, message);
        }
        if (cancellableEvent != null) cancellableEvent.setCancelled(true);
    }

    private void clearInvalidPresence(Player player, PlayerData playerData, String pondId) {
        playerData.setCurrentPondId(null);
        playerData.setAfk(false);
        plugin.getSecurityManager().onPlayerQuit(player.getUniqueId());
        plugin.getPondManager().removePlayerFromAllPools(player.getUniqueId());
        plugin.getUiManager().unregisterPlayerForUpdate(player);
        plugin.getDataManager().queuePlayerDataSave(playerData);
        if (pondId != null) {
            plugin.debug(player.getName(), "已清理无效挂机池状态: pondId=" + pondId);
        }
    }
    
    private void handlePlayerEnterPool(Player player, Pond pond, PlayerData playerData, boolean sendMessage) {
        playerData.setCurrentPondId(pond.getId());
        playerData.setAfk(true);

        plugin.getSecurityManager().onPlayerEnterPool(player, pond.getId());
        plugin.getPondManager().addPlayerToPool(pond.getId(), player.getUniqueId());
        plugin.getUiManager().registerPlayerForUpdate(player);
        plugin.getDataManager().queuePlayerDataSave(playerData);
        
        if (sendMessage) {
            plugin.sendPlayerMessage(player, pond.getEnterMessage());

            if (titleEnabled) {
                showTitle(player, pond, enterTitle, enterSubtitle);
            }
        }
    }

    private void handlePlayerLeavePool(Player player, String pondId, PlayerData playerData, boolean sendMessage) {
        playerData.setCurrentPondId(null);
        playerData.setAfk(false);

        plugin.getSecurityManager().onPlayerLeavePool(player, pondId);
        plugin.getPondManager().removePlayerFromPool(pondId, player.getUniqueId());
        plugin.getUiManager().unregisterPlayerForUpdate(player);
        plugin.getDataManager().queuePlayerDataSave(playerData);

        if (sendMessage) {
            Pond pond = plugin.getPondManager().getPond(pondId);
            if (pond != null) {
                plugin.sendPlayerMessage(player, pond.getLeaveMessage());

                if (titleEnabled) {
                    showTitle(player, pond, leaveTitle, leaveSubtitle);
                }
            }
        }
    }

    private void handlePlayerSwitchPool(Player player, Pond oldPond, Pond newPond, PlayerData playerData, boolean sendMessage) {
        playerData.setCurrentPondId(newPond.getId());

        plugin.getSecurityManager().onPlayerSwitchPool(player, oldPond.getId(), newPond.getId());
        plugin.getPondManager().removePlayerFromPool(oldPond.getId(), player.getUniqueId());
        plugin.getPondManager().addPlayerToPool(newPond.getId(), player.getUniqueId());
        plugin.getDataManager().queuePlayerDataSave(playerData);

        if (sendMessage) {
            plugin.sendPlayerMessage(player, oldPond.getLeaveMessage());
            plugin.sendPlayerMessage(player, newPond.getEnterMessage());

            if (titleEnabled) {
                showTitle(player, oldPond, leaveTitle, leaveSubtitle);
                showTitle(player, newPond, enterTitle, enterSubtitle);
            }
        }
    }

    private void showTitle(Player player, Pond pond, String titleTemplate, String subtitleTemplate) {
        String title = ColorUtil.replacePlaceholders(titleTemplate, "{pool_name}", pond.getName());
        String subtitle = ColorUtil.replacePlaceholders(subtitleTemplate, "{pool_name}", pond.getName());

        Component titleComp = ColorUtil.parseToComponent(title);
        Component subtitleComp = ColorUtil.parseToComponent(subtitle);

        Title.Times times = Title.Times.times(titleFadeIn, titleStay, titleFadeOut);
        player.showTitle(Title.title(titleComp, subtitleComp, times));
    }

    public void startPresenceReconciliation() {
        stopPresenceReconciliation();
        long intervalSeconds = Math.max(1L,
                plugin.getConfig().getLong("core.presence-reconcile-interval", 1L));
        long intervalTicks = intervalSeconds * 20L;

        presenceReconciliationTask = plugin.getSchedulerManager().getAdapter().runSyncRepeating(() -> {
            if (!plugin.isPondsLoaded()) return;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                plugin.getSchedulerManager().getAdapter().runAtEntity(player,
                        () -> reconcilePlayerPondState(player, false));
            }
        }, intervalTicks, intervalTicks);
    }

    public void stopPresenceReconciliation() {
        if (presenceReconciliationTask != null) {
            presenceReconciliationTask.cancel();
            presenceReconciliationTask = null;
        }
        delayedSyncGenerations.clear();
        internalTeleportTargets.clear();
        pendingMountReturns.clear();
    }

    private void schedulePresenceSync(Player player, long delayTicks, boolean sendMessage,
                                      Location deniedReturnLocation) {
        UUID uuid = player.getUniqueId();
        long generation = delayedSyncSequence.incrementAndGet();
        delayedSyncGenerations.put(uuid, generation);
        plugin.getSchedulerManager().getAdapter().runAtEntityLater(player, () -> {
            if (deniedReturnLocation != null) {
                pendingMountReturns.remove(uuid, deniedReturnLocation);
            }
            if (!Objects.equals(delayedSyncGenerations.get(uuid), generation)) return;
            delayedSyncGenerations.remove(uuid, generation);
            if (!player.isOnline() || !plugin.isPondsLoaded()) return;

            SyncResult result = syncPlayerPondState(player, player.getLocation(), sendMessage, null,
                    deniedReturnLocation != null);
            if (result.isDenied() && deniedReturnLocation != null) {
                rollbackDeniedMount(player, deniedReturnLocation);
            }
        }, delayTicks);
    }

    private void rollbackDeniedMount(Player player, Location returnLocation) {
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        if (returnLocation.getWorld() == null) return;

        UUID uuid = player.getUniqueId();
        Location target = returnLocation.clone();
        internalTeleportTargets.put(uuid, target);
        plugin.getSchedulerManager().getAdapter().teleportAsync(player, target)
                .whenComplete((success, error) -> {
                    internalTeleportTargets.remove(uuid, target);
                    if (error != null || !Boolean.TRUE.equals(success)) {
                        plugin.getSchedulerManager().getAdapter().runAtEntity(player,
                                () -> reconcilePlayerPondState(player, false));
                    }
                });
    }

    private boolean consumeInternalTeleport(Player player, Location to) {
        if (to == null) return false;
        UUID uuid = player.getUniqueId();
        Location expected = internalTeleportTargets.get(uuid);
        if (expected == null || !Objects.equals(expected.getWorld(), to.getWorld())) return false;
        if (expected.distanceSquared(to) > 1.0E-6) return false;
        return internalTeleportTargets.remove(uuid, expected);
    }
}
