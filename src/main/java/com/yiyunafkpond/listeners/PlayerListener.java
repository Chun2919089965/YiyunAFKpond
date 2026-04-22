package com.yiyunafkpond.listeners;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import com.yiyunafkpond.util.ColorUtil;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.*;

public class PlayerListener implements Listener {
    private final YiyunAFKpond plugin;
    
    private final boolean titleEnabled;
    private final String enterTitle;
    private final String enterSubtitle;
    private final String leaveTitle;
    private final String leaveSubtitle;
    private final Duration titleFadeIn;
    private final Duration titleStay;
    private final Duration titleFadeOut;
    
    public PlayerListener(YiyunAFKpond plugin) {
        this.plugin = plugin;
        
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
        plugin.getLogger().info("玩家 " + player.getName() + " 已加入服务器，已初始化数据!");
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
        plugin.getRewardManager().cleanupPlayerData(uuid);
        
        if (currentPondId != null && playerData != null) {
            Pond pond = plugin.getPondManager().getPond(currentPondId);
            if (pond != null) {
                playerData.setCurrentPondId(null);
                playerData.setAfk(false);
            }
        }
        
        plugin.getDataManager().removePlayerData(uuid);
        plugin.getLogger().info("玩家 " + player.getName() + " 已离开服务器，数据已保存!");
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (!hasBlockPositionChanged(from, to)) return;
        
        handlePoolTransition(player, from, to, event);
    }
    
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        plugin.getSecurityManager().handlePlayerTeleport(event);
        
        if (event.isCancelled()) return;
        
        if (!hasBlockPositionChanged(from, to)) return;
        
        handlePoolTransition(player, from, to, null);
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
        Player player = event.getPlayer();
        Location respawnLocation = event.getRespawnLocation();
        
        Pond pond = plugin.getPondManager().getPondByLocation(respawnLocation);
        if (pond != null) {
            if (!plugin.getSecurityManager().canPlayerEnterPool(player, pond)) return;
            
            PlayerData playerData = plugin.getDataManager().getOrCreatePlayerData(player);
            handlePlayerEnterPool(player, pond, playerData, true);
        }
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
    
    private void handlePoolTransition(Player player, Location from, Location to, PlayerMoveEvent event) {
        PlayerData playerData = plugin.getDataManager().getOrCreatePlayerData(player);
        
        Pond currentPond = plugin.getPondManager().getPondByLocation(from);
        Pond newPond = plugin.getPondManager().getPondByLocation(to);
        
        if (currentPond != null && newPond == null) {
            handlePlayerLeavePool(player, currentPond.getId(), playerData, event != null);
        } else if (currentPond == null && newPond != null) {
            if (!plugin.getSecurityManager().canPlayerEnterPool(player, newPond)) {
                plugin.sendPlayerMessage(player, plugin.getLanguageManager().getMessage("player.no-permission"));
                if (event != null) event.setCancelled(true);
                return;
            }
            handlePlayerEnterPool(player, newPond, playerData, event != null);
        } else if (currentPond != null && newPond != null && !currentPond.getId().equals(newPond.getId())) {
            if (!plugin.getSecurityManager().canPlayerEnterPool(player, newPond)) {
                plugin.sendPlayerMessage(player, plugin.getLanguageManager().getMessage("player.no-permission"));
                if (event != null) event.setCancelled(true);
                return;
            }
            handlePlayerSwitchPool(player, currentPond, newPond, playerData, event != null);
        }
    }
    
    private void handlePlayerEnterPool(Player player, Pond pond, PlayerData playerData, boolean sendMessage) {
        playerData.setCurrentPondId(pond.getId());
        playerData.setAfk(true);
        
        plugin.getUiManager().registerPlayerForUpdate(player);
        plugin.getDataManager().queuePlayerDataSave(playerData);
        
        if (sendMessage) {
            plugin.sendPlayerMessage(player, pond.getEnterMessage());
            
            if (titleEnabled) {
                showEnterTitle(player, pond);
            }
        }
    }
    
    private void handlePlayerLeavePool(Player player, String pondId, PlayerData playerData, boolean sendMessage) {
        playerData.setCurrentPondId(null);
        playerData.setAfk(false);
        
        plugin.getUiManager().unregisterPlayerForUpdate(player);
        plugin.getDataManager().queuePlayerDataSave(playerData);
        
        if (sendMessage) {
            Pond pond = plugin.getPondManager().getPond(pondId);
            if (pond != null) {
                plugin.sendPlayerMessage(player, pond.getLeaveMessage());
                
                if (titleEnabled) {
                    showLeaveTitle(player, pond);
                }
            }
        }
    }
    
    private void handlePlayerSwitchPool(Player player, Pond oldPond, Pond newPond, PlayerData playerData, boolean sendMessage) {
        playerData.setCurrentPondId(newPond.getId());
        
        plugin.getDataManager().queuePlayerDataSave(playerData);
        
        if (sendMessage) {
            plugin.sendPlayerMessage(player, oldPond.getLeaveMessage());
            plugin.sendPlayerMessage(player, newPond.getEnterMessage());
            
            if (titleEnabled) {
                showLeaveTitle(player, oldPond);
                showEnterTitle(player, newPond);
            }
        }
    }
    
    private void showEnterTitle(Player player, Pond pond) {
        String title = ColorUtil.replacePlaceholders(enterTitle, "{pool_name}", pond.getName());
        String subtitle = ColorUtil.replacePlaceholders(enterSubtitle, "{pool_name}", pond.getName());
        
        Component titleComp = ColorUtil.parseToComponent(title);
        Component subtitleComp = ColorUtil.parseToComponent(subtitle);
        
        Title.Times times = Title.Times.times(titleFadeIn, titleStay, titleFadeOut);
        player.showTitle(Title.title(titleComp, subtitleComp, times));
    }
    
    private void showLeaveTitle(Player player, Pond pond) {
        String title = ColorUtil.replacePlaceholders(leaveTitle, "{pool_name}", pond.getName());
        String subtitle = ColorUtil.replacePlaceholders(leaveSubtitle, "{pool_name}", pond.getName());
        
        Component titleComp = ColorUtil.parseToComponent(title);
        Component subtitleComp = ColorUtil.parseToComponent(subtitle);
        
        Title.Times times = Title.Times.times(titleFadeIn, titleStay, titleFadeOut);
        player.showTitle(Title.title(titleComp, subtitleComp, times));
    }
}
