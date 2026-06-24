package com.yiyunafkpond.reward;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.constants.Constants;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import com.yiyunafkpond.scheduler.FoliaSchedulerAdapter;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RewardManager {
    private final YiyunAFKpond plugin;
    private final Map<String, RewardType> rewardTypes = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private final Map<String, WrappedTask> expTasks = new ConcurrentHashMap<>();
    private final Map<String, WrappedTask> moneyTasks = new ConcurrentHashMap<>();
    private final Map<String, WrappedTask> pointTasks = new ConcurrentHashMap<>();
    private final Map<String, WrappedTask> commandTasks = new ConcurrentHashMap<>();

    private final Map<String, Long> expNextEpochSec = new ConcurrentHashMap<>();
    private final Map<String, Long> moneyNextEpochSec = new ConcurrentHashMap<>();
    private final Map<String, Long> pointNextEpochSec = new ConcurrentHashMap<>();
    private final Map<String, Long> commandNextEpochSec = new ConcurrentHashMap<>();

    private final Map<UUID, Long> lastLimitMessageTime = new ConcurrentHashMap<>();
    private static final long LIMIT_MESSAGE_COOLDOWN_MS = 30000L;

    private WrappedTask afkTimeTask;

    public RewardManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        registerRewardType(new XpReward(plugin));
        registerRewardType(new MoneyReward(plugin));
        registerRewardType(new PointReward(plugin));
    }

    public void registerRewardType(RewardType type) {
        rewardTypes.put(type.getName().toLowerCase(), type);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("已注册奖励类型: " + type.getName());
        }
    }

    private List<Player> getPlayersInPond(Pond pond) {
        return plugin.getPondManager().getPlayersInPond(pond);
    }

    public void startAllRewardTasks() {
        stopAllRewardTasks();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("启动所有池的奖励任务...");
        }

        int poolCount = 0;
        for (Pond pond : plugin.getPondManager().getAllPonds()) {
            if (pond.isEnabled()) {
                startPoolRewardTasks(pond);
                poolCount++;
            }
        }

        startAfkTimeTask();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("已启动 " + poolCount + " 个池的奖励任务");
        }
    }

    public void stopAllRewardTasks() {
        cancelAllTasks(expTasks);
        cancelAllTasks(moneyTasks);
        cancelAllTasks(pointTasks);
        cancelAllTasks(commandTasks);

        expNextEpochSec.clear();
        moneyNextEpochSec.clear();
        pointNextEpochSec.clear();
        commandNextEpochSec.clear();

        if (afkTimeTask != null) {
            afkTimeTask.cancel();
            afkTimeTask = null;
        }
    }

    private void cancelAllTasks(Map<String, WrappedTask> tasks) {
        for (WrappedTask task : tasks.values()) {
            if (task != null) task.cancel();
        }
        tasks.clear();
    }

    public void startPoolRewardTasks(Pond pond) {
        String poolId = pond.getId();
        plugin.getLogger().info("为挂机池 " + poolId + " 启动奖励任务");

        cancelTask(expTasks, poolId);
        cancelTask(moneyTasks, poolId);
        cancelTask(pointTasks, poolId);
        cancelTask(commandTasks, poolId);

        expNextEpochSec.remove(poolId);
        moneyNextEpochSec.remove(poolId);
        pointNextEpochSec.remove(poolId);
        commandNextEpochSec.remove(poolId);

        startExpTask(pond);
        startMoneyTask(pond);
        startPointTask(pond);
        startCommandTask(pond);

        plugin.getLogger().info("挂机池 " + poolId + " 奖励任务启动完成");
    }

    private void cancelTask(Map<String, WrappedTask> tasks, String poolId) {
        WrappedTask task = tasks.remove(poolId);
        if (task != null) task.cancel();
    }

    public void stopPoolRewardTasks(Pond pond) {
        String poolId = pond.getId();
        cancelTask(expTasks, poolId);
        cancelTask(moneyTasks, poolId);
        cancelTask(pointTasks, poolId);
        cancelTask(commandTasks, poolId);

        expNextEpochSec.remove(poolId);
        moneyNextEpochSec.remove(poolId);
        pointNextEpochSec.remove(poolId);
        commandNextEpochSec.remove(poolId);
    }

    public void restartAllRewardTasks() {
        plugin.getLogger().info("重启所有奖励任务...");
        stopAllRewardTasks();

        for (Pond pond : plugin.getPondManager().getAllPonds()) {
            if (pond.isEnabled()) {
                startPoolRewardTasks(pond);
            }
        }

        startAfkTimeTask();
        plugin.getLogger().info("所有奖励任务已重启完成！");
    }

    public void rebuildRewardTasks() {
        startAllRewardTasks();
    }

    private void startAfkTimeTask() {
        if (afkTimeTask != null) {
            afkTimeTask.cancel();
            afkTimeTask = null;
        }

        FoliaSchedulerAdapter adapter = plugin.getSchedulerManager().getAdapter();
        afkTimeTask = adapter.runSyncRepeating(() -> {
            for (Pond pond : plugin.getPondManager().getAllPonds()) {
                if (!pond.isEnabled()) continue;

                List<Player> players = getPlayersInPond(pond);
                for (Player player : players) {
                    PlayerData data = plugin.getDataManager().getPlayerDataIfLoaded(player.getUniqueId());
                    if (data != null && data.isAfk() && pond.getId().equals(data.getCurrentPondId())) {
                        data.addAfkTime(Constants.MILLISECONDS_PER_SECOND);
                        data.addPondAfkTime(pond.getId(), Constants.MILLISECONDS_PER_SECOND);
                    }
                }
            }
        }, Constants.TICKS_PER_SECOND, Constants.TICKS_PER_SECOND);
    }

    // ==================== 统一奖励任务模板 ====================

    /**
     * 每玩家奖励处理回调。
     * @return true 表示实际发放了奖励（触发保存和 UI 刷新）
     */
    @FunctionalInterface
    private interface RewardProcessor {
        boolean process(Player player, PlayerData data, Pond pond);
    }

    /**
     * 统一的奖励任务启动模板，消除 exp/money/point 三类任务间的重复代码。
     */
    private void startTypedRewardTask(Pond pond, boolean enabled, long intervalTicks,
                                       Map<String, WrappedTask> taskMap,
                                       Map<String, Long> nextEpochMap,
                                       RewardProcessor processor) {
        if (!enabled) return;

        String poolId = pond.getId();
        if (intervalTicks <= 0) {
            plugin.getLogger().warning("奖励间隔 <= 0，跳过 - 池ID: " + poolId);
            return;
        }

        long intervalSec = intervalTicks / Constants.TICKS_PER_SECOND;
        nextEpochMap.put(poolId, epochSec() + intervalSec);

        FoliaSchedulerAdapter adapter = plugin.getSchedulerManager().getAdapter();
        WrappedTask task = adapter.runSyncRepeating(() -> {
            List<Player> players = getPlayersInPond(pond);
            if (players.isEmpty()) return;

            for (Player player : players) {
                if (!player.isOnline() || !isEligibleForReward(player, pond)) continue;

                PlayerData playerData = plugin.getDataManager().getPlayerDataIfLoaded(player.getUniqueId());
                if (playerData == null) continue;
                playerData.checkAndResetDailyData();

                if (processor.process(player, playerData, pond)) {
                    playerData.setLastRewardTime(System.currentTimeMillis());
                    plugin.getDataManager().queuePlayerDataSave(playerData);
                    plugin.getUiManager().markDirty(player);
                }
            }

            nextEpochMap.put(poolId, epochSec() + intervalSec);
        }, 1L, intervalTicks);

        taskMap.put(poolId, task);
    }

    // ==================== 各类型奖励任务（仅保留类型特有逻辑） ====================

    private void startExpTask(Pond pond) {
        startTypedRewardTask(pond, pond.isExpEnabled(), getExpIntervalTicks(pond),
                expTasks, expNextEpochSec, (player, data, p) -> {
            long amount = calculateExperienceAmount(p);
            amount = (long) (amount * p.getXpRate());
            if (amount <= 0) return false;

            String poolId = p.getId();
            long serverMax = plugin.getConfig().getLong("server-limits.total-exp-daily", 0);
            long poolMax = p.getExpMaxDaily();
            long capped = capReward(amount, data.getTodayExp(), serverMax,
                    data.getDailyExpByPool(poolId), poolMax,
                    plugin.getLanguageManager().getMessage("player.total-exp-limit"),
                    p.getExpLimitMessage(), player);
            if (capped <= 0) return false;

            player.giveExp((int) Math.min(capped, Integer.MAX_VALUE), p.isExpApplyMending());
            plugin.sendPlayerMessage(player, p.getExpRewardMessage()
                    .replace("{xp_amount}", String.valueOf(capped)));
            data.addXpGained(capped);
            data.addTodayExp(poolId, capped);
            return true;
        });
    }

    private void startMoneyTask(Pond pond) {
        startTypedRewardTask(pond, pond.isMoneyEnabled(),
                pond.getMoneyInterval() * Constants.TICKS_PER_SECOND,
                moneyTasks, moneyNextEpochSec, (player, data, p) -> {
            double amount = calculateMoneyAmount(p);
            if (amount < Constants.MIN_REWARD_AMOUNT) return false;

            String poolId = p.getId();
            double serverMax = plugin.getConfig().getDouble("server-limits.total-money-daily", 0.0);
            double poolMax = p.getMoneyMaxDaily();
            double capped = capRewardDouble(amount, data.getTodayMoney(), serverMax,
                    data.getDailyMoneyByPool(poolId), poolMax,
                    plugin.getLanguageManager().getMessage("player.total-money-limit"),
                    p.getMoneyLimitMessage(), player);
            if (capped < Constants.MIN_REWARD_AMOUNT) return false;

            if (!plugin.getHookManager().depositMoney(player, capped)) return false;

            plugin.sendPlayerMessage(player, p.getMoneyRewardMessage()
                    .replace("{money_amount}", String.format("%.2f", capped)));
            data.addMoneyGained(capped);
            data.addTodayMoney(poolId, capped);
            return true;
        });
    }

    private void startPointTask(Pond pond) {
        startTypedRewardTask(pond, pond.isPointEnabled(),
                pond.getPointInterval() * Constants.TICKS_PER_SECOND,
                pointTasks, pointNextEpochSec, (player, data, p) -> {
            int amount = calculatePointAmount(p);
            if (amount < 1) return false;

            String poolId = p.getId();
            double serverMax = plugin.getConfig().getDouble("server-limits.total-point-daily", 0.0);
            double poolMax = p.getPointMaxDaily();
            int capped = capRewardInt(amount, data.getTodayPoint(), serverMax,
                    data.getDailyPointByPool(poolId), poolMax,
                    plugin.getLanguageManager().getMessage("player.total-point-limit"),
                    p.getPointLimitMessage(), player);
            if (capped < 1) return false;

            if (!plugin.getHookManager().depositPoint(player, capped)) return false;

            plugin.sendPlayerMessage(player, p.getPointRewardMessage()
                    .replace("{point_amount}", String.valueOf(capped)));
            data.addPointGained(capped);
            data.addTodayPoint(poolId, capped);
            return true;
        });
    }

    private void startCommandTask(Pond pond) {
        String poolId = pond.getId();
        cancelTask(commandTasks, poolId);

        List<String> cmds = pond.getCommands();
        if (cmds == null || cmds.isEmpty()) return;

        long intervalSec = pond.getCommandInterval();
        if (intervalSec <= 0) return;

        long intervalTicks = intervalSec * Constants.TICKS_PER_SECOND;
        commandNextEpochSec.put(poolId, epochSec() + intervalSec);

        FoliaSchedulerAdapter adapter = plugin.getSchedulerManager().getAdapter();
        WrappedTask task = adapter.runSyncRepeating(() -> {
            List<Player> players = getPlayersInPond(pond);
            if (players.isEmpty()) return;

            for (Player player : players) {
                if (!isEligibleForReward(player, pond)) continue;

                for (String cmd : cmds) {
                    String processed = null;
                    try {
                        processed = processCommandVariables(cmd, player, pond);
                        SimpleCommandMap commandMap = (SimpleCommandMap) plugin.getServer().getCommandMap();
                        commandMap.dispatch(plugin.getServer().getConsoleSender(),
                                processed.startsWith("/") ? processed.substring(1) : processed);
                    } catch (Exception e) {
                        plugin.getLogger().warning("执行命令失败: "
                                + (processed != null ? processed : cmd) + ", 错误: " + e.getMessage());
                    }
                }
            }

            commandNextEpochSec.put(poolId, epochSec() + intervalSec);
        }, 1L, intervalTicks);

        commandTasks.put(poolId, task);
    }

    private long capReward(long amount, long todayTotal, long serverMax, long todayPool, long poolMax,
                           String serverLimitMsg, String poolLimitMsg, Player player) {
        if (serverMax > 0 && todayTotal >= serverMax) {
            sendLimitMessage(player, serverLimitMsg);
            return -1;
        }
        if (poolMax > 0 && todayPool >= poolMax) {
            sendLimitMessage(player, poolLimitMsg);
            return -1;
        }
        if (serverMax > 0 && todayTotal + amount > serverMax) amount = serverMax - todayTotal;
        if (poolMax > 0 && todayPool + amount > poolMax) amount = poolMax - todayPool;
        return amount;
    }

    private double capRewardDouble(double amount, double todayTotal, double serverMax, double todayPool, double poolMax,
                                    String serverLimitMsg, String poolLimitMsg, Player player) {
        if (serverMax > 0 && todayTotal >= serverMax) {
            sendLimitMessage(player, serverLimitMsg);
            return -1;
        }
        if (poolMax > 0 && todayPool >= poolMax) {
            sendLimitMessage(player, poolLimitMsg);
            return -1;
        }
        if (serverMax > 0 && todayTotal + amount > serverMax) amount = roundMoney(serverMax - todayTotal);
        if (poolMax > 0 && todayPool + amount > poolMax) amount = roundMoney(poolMax - todayPool);
        return roundMoney(amount);
    }

    private int capRewardInt(int amount, double todayTotal, double serverMax, double todayPool, double poolMax,
                              String serverLimitMsg, String poolLimitMsg, Player player) {
        if (serverMax > 0 && todayTotal >= serverMax) {
            sendLimitMessage(player, serverLimitMsg);
            return -1;
        }
        if (poolMax > 0 && todayPool >= poolMax) {
            sendLimitMessage(player, poolLimitMsg);
            return -1;
        }
        if (serverMax > 0 && todayTotal + amount > serverMax) amount = (int)(serverMax - todayTotal);
        if (poolMax > 0 && todayPool + amount > poolMax) amount = (int)(poolMax - todayPool);
        return amount;
    }

    private void sendLimitMessage(Player player, String message) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        boolean shouldSend = lastLimitMessageTime.compute(uuid, (key, lastTime) -> {
            if (lastTime == null || now - lastTime > LIMIT_MESSAGE_COOLDOWN_MS) {
                return now;
            }
            return lastTime;
        }).equals(now);
        if (shouldSend) {
            plugin.sendPlayerMessage(player, message);
        }
    }

    public void cleanupPlayerData(UUID playerId) {
        lastLimitMessageTime.remove(playerId);
    }

    private long getExpIntervalTicks(Pond pond) {
        String distributionMode = pond.getExpDistributionMode();
        if (distributionMode != null && distributionMode.equalsIgnoreCase("continuous")) {
            return Constants.TICKS_PER_SECOND;
        }
        return pond.getExpInterval() * Constants.TICKS_PER_SECOND;
    }

    /**
     * 检查玩家是否有资格获得奖励。
     * 同时验证：
     * 1. 玩家是否真实位于挂机池区域内（防止传送后状态未清理的奖励泄漏）
     * 2. 玩家是否拥有进入该池的权限
     */
    private boolean isEligibleForReward(Player player, Pond pond) {
        if (!pond.isInPond(player.getLocation())) return false;
        String requiredPermission = pond.getRequiredPermission();
        return requiredPermission == null || player.hasPermission(requiredPermission);
    }

    private long calculateExperienceAmount(Pond pond) {
        if (pond.getExpRewardMode().equalsIgnoreCase("fixed")) {
            return pond.getExpFixedAmount();
        }
        long min = Math.max(1, pond.getExpRandomMin());
        long max = Math.max(min, pond.getExpRandomMax());
        if (min == max) return min;
        long range = max - min + 1;
        if (range <= 0) return min;
        return min + (Math.abs(random.nextLong()) % range);
    }

    private double calculateMoneyAmount(Pond pond) {
        double amount;
        if (pond.getMoneyRewardMode().equalsIgnoreCase("fixed")) {
            amount = pond.getMoneyFixedAmount();
        } else {
            double min = Math.max(Constants.MIN_REWARD_AMOUNT, pond.getMoneyRandomMin());
            double max = Math.max(min, pond.getMoneyRandomMax());
            amount = min + random.nextDouble() * (max - min);
        }
        amount = Math.max(Constants.MIN_REWARD_AMOUNT, amount);
        return roundMoney(amount * pond.getMoneyRate());
    }
    
    private static double roundMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private int calculatePointAmount(Pond pond) {
        int amount;
        if (pond.getPointRewardMode().equalsIgnoreCase("fixed")) {
            amount = (int) pond.getPointFixedAmount();
        } else {
            int min = Math.max(1, (int) pond.getPointRandomMin());
            int max = Math.max(min, (int) pond.getPointRandomMax());
            amount = min + random.nextInt(max - min + 1);
        }
        return Math.max(1, (int) Math.round(amount * pond.getPointRate()));
    }

    private String processCommandVariables(String command, Player player, Pond pond) {
        return command
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{pool_id}", pond.getId())
                .replace("{pool_name}", pond.getName());
    }

    public void giveReward(Player player, String rewardType, long amount, Pond pond) {
        RewardType type = rewardTypes.get(rewardType.toLowerCase());
        if (type != null) {
            type.giveReward(player, amount, pond);
        } else {
            plugin.getLogger().warning("尝试发放未知奖励类型: " + rewardType);
        }
    }

    public void giveReward(Player player, String rewardType, double amount, Pond pond) {
        RewardType type = rewardTypes.get(rewardType.toLowerCase());
        if (type != null) {
            type.giveReward(player, amount, pond);
        } else {
            plugin.getLogger().warning("尝试发放未知奖励类型: " + rewardType);
        }
    }

    public Map<String, RewardType> getRewardTypes() {
        return rewardTypes;
    }

    public int getNextRemainingSeconds(String poolId) {
        long now = epochSec();
        int best = Integer.MAX_VALUE;

        for (Map<String, Long> nextMap : List.of(expNextEpochSec, moneyNextEpochSec, pointNextEpochSec, commandNextEpochSec)) {
            Long time = nextMap.get(poolId);
            if (time != null) best = Math.min(best, (int) Math.max(0L, time - now));
        }

        return best == Integer.MAX_VALUE ? 0 : Math.max(1, best);
    }

    private static long epochSec() {
        return System.currentTimeMillis() / Constants.MILLISECONDS_PER_SECOND;
    }
}
