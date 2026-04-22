package com.yiyunafkpond.scheduler;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.yiyunafkpond.YiyunAFKpond;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class FoliaSchedulerAdapter {
    private final YiyunAFKpond plugin;
    private final FoliaLib foliaLib;
    private final PlatformScheduler scheduler;
    private final ConcurrentHashMap<String, WrappedTask> tasks = new ConcurrentHashMap<>();

    public FoliaSchedulerAdapter(YiyunAFKpond plugin) {
        this.plugin = plugin;
        this.foliaLib = new FoliaLib(plugin);
        this.scheduler = foliaLib.getScheduler();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("FoliaLib 调度器已初始化, 实现类型: " + foliaLib.getImplType());
        }
    }

    public WrappedTask runSync(Runnable task) {
        return scheduler.runLater(task, 1L);
    }

    public WrappedTask runSyncLater(Runnable task, long delayTicks) {
        return scheduler.runLater(task, delayTicks);
    }

    public WrappedTask runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        return scheduler.runTimer(task, delayTicks, periodTicks);
    }

    public WrappedTask runSyncRepeating(String taskId, Runnable task, long delayTicks, long periodTicks) {
        cancelTask(taskId);
        WrappedTask wrappedTask = scheduler.runTimer(task, delayTicks, periodTicks);
        tasks.put(taskId, wrappedTask);
        return wrappedTask;
    }

    public CompletableFuture<Void> runAsync(Runnable task) {
        return scheduler.runAsync(wt -> task.run());
    }

    public WrappedTask runAsyncLater(Runnable task, long delayTicks) {
        return scheduler.runLaterAsync(task, delayTicks);
    }

    public WrappedTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        return scheduler.runTimerAsync(task, delayTicks, periodTicks);
    }

    public WrappedTask runAsyncRepeating(String taskId, Runnable task, long delayTicks, long periodTicks) {
        cancelTask(taskId);
        WrappedTask wrappedTask = scheduler.runTimerAsync(task, delayTicks, periodTicks);
        tasks.put(taskId, wrappedTask);
        return wrappedTask;
    }

    public CompletableFuture<Void> runAtLocation(Location location, Runnable task) {
        return scheduler.runAtLocation(location, wt -> task.run());
    }

    public WrappedTask runAtLocationTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        return scheduler.runAtLocationTimer(location, task, delayTicks, periodTicks);
    }

    public WrappedTask runAtLocationTimer(String taskId, Location location, Runnable task, long delayTicks, long periodTicks) {
        cancelTask(taskId);
        WrappedTask wrappedTask = scheduler.runAtLocationTimer(location, task, delayTicks, periodTicks);
        tasks.put(taskId, wrappedTask);
        return wrappedTask;
    }

    public CompletableFuture<com.tcoded.folialib.enums.EntityTaskResult> runAtEntity(Entity entity, Runnable task) {
        return scheduler.runAtEntity(entity, wt -> task.run());
    }

    public WrappedTask runAtEntityLater(Entity entity, Runnable task, long delayTicks) {
        return scheduler.runAtEntityLater(entity, task, delayTicks);
    }

    public WrappedTask runAtEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        return scheduler.runAtEntityTimer(entity, task, delayTicks, periodTicks);
    }

    public WrappedTask runAtEntityTimer(String taskId, Entity entity, Runnable task, long delayTicks, long periodTicks) {
        cancelTask(taskId);
        WrappedTask wrappedTask = scheduler.runAtEntityTimer(entity, task, delayTicks, periodTicks);
        tasks.put(taskId, wrappedTask);
        return wrappedTask;
    }

    public void cancelTask(String taskId) {
        WrappedTask existing = tasks.remove(taskId);
        if (existing != null) {
            existing.cancel();
        }
    }

    public void cancelTask(WrappedTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    public void cancelAllTasks() {
        for (WrappedTask task : tasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        tasks.clear();
        scheduler.cancelAllTasks();
    }

    public boolean isFolia() {
        return foliaLib.isFolia();
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public PlatformScheduler getScheduler() {
        return scheduler;
    }

    public Player getPlayer(String name) {
        return scheduler.getPlayer(name);
    }

    public Player getPlayerExact(String name) {
        return scheduler.getPlayerExact(name);
    }

    public Player getPlayer(java.util.UUID uuid) {
        return scheduler.getPlayer(uuid);
    }

    public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        return scheduler.teleportAsync(entity, location);
    }
}
