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
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime nextReset = now.toLocalDate().atStartOfDay();

        if (now.isAfter(nextReset)) {
            nextReset = nextReset.plusDays(1);
        }

        long delaySeconds = java.time.Duration.between(now, nextReset).getSeconds();
        return delaySeconds * 20L;
    }

    private void resetAllDailyData() {
        plugin.getLogger().info("开始执行每日数据重置...");

        int resetCount = 0;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerData playerData = plugin.getDataManager().getPlayerDataIfLoaded(player.getUniqueId());
            if (playerData != null) {
                playerData.resetDailyData();
                plugin.getDataManager().queuePlayerDataSave(playerData);
                resetCount++;
            }
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
