package com.yiyunafkpond.listeners;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldListener implements Listener {
    private final YiyunAFKpond plugin;

    public WorldListener(YiyunAFKpond plugin) {
        this.plugin = plugin;
    }

    /**
     * 世界加载时的处理。
     * 只更新新加载世界中的池的 World 引用，不清空现有池追踪数据，
     * 避免玩家状态与池追踪脱钩导致的奖励泄漏。
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();

        if (plugin.isPondsLoaded()) {
            int updated = 0;
            for (Pond pond : plugin.getPondManager().getAllPonds()) {
                if (worldName.equals(pond.getWorldName()) && pond.getWorld() == null) {
                    pond.updateWorld(world);
                    updated++;
                }
            }
            if (updated > 0) {
                plugin.getLogger().info("世界 " + worldName + " 已加载，更新了 " + updated + " 个挂机池的 World 引用");
                // 重新扫描该世界中可能已在池内的玩家
                plugin.rescanPlayersInPonds();
            }
        } else {
            plugin.getLogger().info("世界 " + worldName + " 已加载");
        }
    }
}