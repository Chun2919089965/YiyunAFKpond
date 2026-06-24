package com.yiyunafkpond.scheduler;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.concurrent.*;

public class SchedulerManager {
    private final YiyunAFKpond plugin;
    private final FoliaSchedulerAdapter adapter;

    private WrappedTask dailyResetTask;

    private final ExecutorService asyncExecutor;

    public SchedulerManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        this.adapter = new FoliaSchedulerAdapter(plugin);

        this.asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("调度管理器已初始化 (FoliaLib 适配器), Folia模式: " + adapter.isFolia());
        }
    }

    public void startAllSchedulers() {
        startDailyResetTask();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("所有调度任务已启动!");
        }
    }

    public void shutdownAllSchedulers() {
        if (dailyResetTask != null) {
            dailyResetTask.cancel();
        }

        adapter.cancelAllTasks();

        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("所有调度任务已关闭!");
        }
    }

    private void startDailyResetTask() {
        long delayTicks = calculateDelayToNextReset();
        long periodTicks = 24 * 60 * 60 * 20L;

        dailyResetTask = adapter.runSyncRepeating(this::resetAllDailyData, delayTicks, periodTicks);

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("每日重置任务已启动，将在 " + (delayTicks / 20 / 60) + " 分钟后执行首次重置");
        }
    }

    private long calculateDelayToNextReset() {
        // 读取用户配置的重置时间（默认 "00:00"）
        String resetTime = plugin.getConfig().getString("reset.time", "00:00");
        String[] parts = resetTime.split(":");
        int resetHour = 0;
        int resetMinute = 0;
        try {
            resetHour = Integer.parseInt(parts[0]);
            resetMinute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            resetHour = Math.max(0, Math.min(23, resetHour));
            resetMinute = Math.max(0, Math.min(59, resetMinute));
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("无效的 reset.time 配置: " + resetTime + "，使用默认值 00:00");
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime nextReset = now.toLocalDate().atTime(resetHour, resetMinute);

        if (!now.isBefore(nextReset)) {
            nextReset = nextReset.plusDays(1);
        }

        long delaySeconds = java.time.Duration.between(now, nextReset).getSeconds();
        return delaySeconds * 20L;
    }

    private void resetAllDailyData() {
        plugin.getLogger().info("开始执行每日数据重置...");

        int resetCount = 0;

        // 遍历所有已加载的玩家数据（包括离线玩家），确保无人遗漏
        for (PlayerData playerData : plugin.getDataManager().getAllPlayerData()) {
            playerData.resetDailyData();
            plugin.getDataManager().queuePlayerDataSave(playerData);
            resetCount++;
        }

        // 同时通知 UI 管理器刷新所有在线玩家的显示
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.getUiManager().markDirty(player);
        }

        plugin.getLogger().info("每日数据重置完成，共重置 " + resetCount + " 个玩家的数据");
    }

    public void runAsync(Runnable task) {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.execute(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    plugin.getLogger().severe("异步任务执行失败: " + e.getMessage());
                }
            });
        }
    }

    public WrappedTask runAsyncRepeating(Runnable task, long delay, long period) {
        return adapter.runAsyncRepeating(task, delay, period);
    }

    public <T> Future<T> submitAsync(Callable<T> task) {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            return asyncExecutor.submit(task);
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(new RejectedExecutionException("异步执行器已关闭"));
        return future;
    }

    public void runSync(Runnable task) {
        adapter.runSync(task);
    }

    public WrappedTask runSyncRepeating(Runnable task, long delay, long period) {
        return adapter.runSyncRepeating(task, delay, period);
    }

    public FoliaSchedulerAdapter getAdapter() {
        return adapter;
    }
}
