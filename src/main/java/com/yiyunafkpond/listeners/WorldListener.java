package com.yiyunafkpond.listeners;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.List;

public class WorldListener implements Listener {
    private final YiyunAFKpond plugin;

    public WorldListener(YiyunAFKpond plugin) {
        this.plugin = plugin;
    }

    /**
     * 世界加载时的处理。
     * 1. 更新已有池的 World 引用
     * 2. 补充加载因世界未就绪而在启动时跳过的池（不清空已有追踪数据）
     * 3. 为新池启动奖励任务并重扫描玩家
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();

        if (!plugin.isPondsLoaded()) {
            plugin.getLogger().info("世界 " + worldName + " 已加载");
            return;
        }

        // 1. 更新已有池的世界引用
        int updated = 0;
        for (Pond pond : plugin.getPondManager().getAllPonds()) {
            if (worldName.equals(pond.getWorldName()) && pond.getWorld() == null) {
                pond.updateWorld(world);
                updated++;
            }
        }

        // 2. 补充加载启动时因世界未就绪而跳过的池
        List<String> newPondIds = plugin.getPondManager().loadPondsForWorld(worldName);

        // 3. 只为新加载的池启动奖励任务（不影响已有池的奖励周期）
        if (!newPondIds.isEmpty() && plugin.getRewardManager() != null) {
            for (String pondId : newPondIds) {
                Pond pond = plugin.getPondManager().getPond(pondId);
                if (pond != null && pond.isEnabled()) {
                    plugin.getRewardManager().startPoolRewardTasks(pond);
                }
            }
        }

        // 4. 重扫描玩家
        if (updated > 0 || !newPondIds.isEmpty()) {
            plugin.rescanPlayersInPonds();
        }
    }
}