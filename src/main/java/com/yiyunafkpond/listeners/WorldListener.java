package com.yiyunafkpond.listeners;

import com.yiyunafkpond.YiyunAFKpond;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldListener implements Listener {
    private final YiyunAFKpond plugin;
    
    public WorldListener(YiyunAFKpond plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 世界加载时的处理
     * @param event 事件对象
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        // 只有当挂机池已经加载过（即pondsLoaded标志为true），才重新加载挂机池
        // 这样可以避免在插件启动阶段就进行世界检查
        if (plugin.isPondsLoaded()) {
            // 重新加载挂机池，确保新加载的世界中的挂机池能被正确处理
            plugin.getPondManager().loadPonds();
        } else {
            // 挂机池尚未加载，不进行处理
            plugin.getLogger().info("世界 " + event.getWorld().getName() + " 已加载");
        }
    }
}