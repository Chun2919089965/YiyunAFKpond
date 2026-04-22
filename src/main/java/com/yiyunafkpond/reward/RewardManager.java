package com.yiyunafkpond.reward;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.constants.Constants;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import com.yiyunafkpond.scheduler.FoliaSchedulerAdapter;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

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

    private void startExpTask(Pond pond) {
        if (!pond.isExpEnabled()) return;

        String poolId = pond.getId();
        long intervalTicks = getExpIntervalTicks(pond);
        if (intervalTicks <= 0) {
            plugin.getLogger().warning("经验奖励间隔 <= 0，跳过 - 池ID: " + poolId);
            return;
        }

        long intervalSec = intervalTicks / Constants.TICKS_PER_SECOND;
        expNextEpochSec.put(poolId, epochSec() + intervalSec);

        FoliaSchedulerAdapter adapter = plugin.getSchedulerManager().getAdapter();
        WrappedTask task = adapter.runSyncRepeating(() -> {
            List<Player> players = getPlayersInPond(pond);
            if (players.isEmpty()) return;

            for (Player player : players) {
                if (!player.isOnline() || !isEligibleForReward(player, pond)) continue;

                long expAmount = calculateExperienceAmount(pond);
                expAmount = (long) (expAmount * pond.getXpRate());
                if (expAmount <= 0) continue;

                PlayerData playerData = plugin.getDataManager().getPlayerDataIfLoaded(player.getUniqueId());
                if (playerData == null) continue;
                playerData.checkAndResetDailyData();

                long serverMax = plugin.getConfig().getLong("server-limits.total-exp-daily", 0);
                long poolMax = pond.getExpMaxDaily();
                long todayPool = playerData.getDailyExpByPool(poolId);
                long todayTotal = playerData.getTodayExp();

                long capped = capReward(expAmount, todayTotal, serverMax, todayPool, poolMax,
                        plugin.getLanguageManager().getMessage("player.total-exp-limit"),
                        pond.getExpLimitMessage(), player);
                if (capped <= 0) continue;
                expAmount = capped;

                player.giveExp((int) Math.min(expAmount, Integer.MAX_VALUE), pond.isExpApplyMending());

                plugin.sendPlayerMessage(player, pond.getExpRewardMessage().replace("{xp_amount}", String.valueOf(expAmount)));
                playerData.addXpGained(expAmount);
                playerData.addTodayExp(poolId, expAmount);
                playerData.setLastRewardTime(System.currentTimeMillis());
                plugin.getDataManager().queuePlayerDataSave(playerData);
                plugin.getUiManager().markDirty(player);
            }

            expNextEpochSec.put(poolId, epochSec() + intervalSec);
        }, 1L, intervalTicks);

        expTasks.put(poolId, task);
    }

    private void startMoneyTask(Pond pond) {
        if (!pond.isMoneyEnabled()) return;

        String poolId = pond.getId();
        long intervalSec = pond.getMoneyInterval();
        if (intervalSec <= 0) {
            plugin.getLogger().warning("金币奖励间隔 <= 0，跳过 - 池ID: " + poolId);
            return;
        }

        long intervalTicks = intervalSec * Constants.TICKS_PER_SECOND;
        moneyNextEpochSec.put(poolId, epochSec() + intervalSec);

        FoliaSchedulerAdapter adapter = plugin.getSchedulerManager().getAdapter();
        WrappedTask task = adapter.runSyncRepeating(() -> {
            List<Player> players = getPlayersInPond(pond);
            if (players.isEmpty()) return;

            for (Player player : players) {
                if (!player.isOnline() || !isEligibleForReward(player, pond)) continue;

                PlayerData playerData = plugin.getDataManager().getPlayerDataIfLoaded(player.getUniqueId());
                if (playerData == null) continue;
                playerData.checkAndResetDailyData();

                double moneyAmount = calculateMoneyAmount(pond);
                if (moneyAmount < Constants.MIN_REWARD_AMOUNT) continue;

                double serverMax = plugin.getConfig().getDouble("server-limits.total-money-daily", 0.0);
                double poolMax = pond.getMoneyMaxDaily();
                double todayPool = playerData.getDailyMoneyByPool(poolId);
                double todayTotal = playerData.getTodayMoney();

                double capped = capRewardDouble(moneyAmount, todayTotal, serverMax, todayPool, poolMax,
                        plugin.getLanguageManager().getMessage("player.total-money-limit"),
                        pond.getMoneyLimitMessage(), player);
                if (capped < Constants.MIN_REWARD_AMOUNT) continue;
                moneyAmount = capped;

                if (plugin.getHookManager().depositMoney(player, moneyAmount)) {
                    plugin.sendPlayerMessage(player, pond.getMoneyRewardMessage().replace("{money_amount}", String.format("%.2f", moneyAmount)));
                    playerData.addMoneyGained(moneyAmount);
                    playerData.addTodayMoney(poolId, moneyAmount);
                    playerData.setLastRewardTime(System.currentTimeMillis());
                    plugin.getDataManager().queuePlayerDataSave(playerData);
                    plugin.getUiManager().markDirty(player);
                }
            }

            moneyNextEpochSec.put(poolId, epochSec() + intervalSec);
        }, 1L, intervalTicks);

        moneyTasks.put(poolId, task);
    }

    private void startPointTask(Pond pond) {
        if (!pond.isPointEnabled()) return;

        String poolId = pond.getId();
        long intervalSec = pond.getPointInterval();
        if (intervalSec <= 0) {
            plugin.getLogger().warning("点券奖励间隔 <= 0，跳过 - 池ID: " + poolId);
            return;
        }

        long intervalTicks = intervalSec * Constants.TICKS_PER_SECOND;
        pointNextEpochSec.put(poolId, epochSec() + intervalSec);

        FoliaSchedulerAdapter adapter = plugin.getSchedulerManager().getAdapter();
        WrappedTask task = adapter.runSyncRepeating(() -> {
            List<Player> players = getPlayersInPond(pond);
            if (players.isEmpty()) return;

            for (Player player : players) {
                if (!player.isOnline() || !isEligibleForReward(player, pond)) continue;

                PlayerData playerData = plugin.getDataManager().getPlayerDataIfLoaded(player.getUniqueId());
                if (playerData == null) continue;
                playerData.checkAndResetDailyData();

                int pointAmount = calculatePointAmount(pond);
                if (pointAmount < 1) continue;

                double serverMax = plugin.getConfig().getDouble("server-limits.total-point-daily", 0.0);
                double poolMax = pond.getPointMaxDaily();
                double todayPool = playerData.getDailyPointByPool(poolId);
                double todayTotal = playerData.getTodayPoint();

                int capped = capRewardInt(pointAmount, todayTotal, serverMax, todayPool, poolMax,
                        plugin.getLanguageManager().getMessage("player.total-point-limit"),
                        pond.getPointLimitMessage(), player);
                if (capped < 1) continue;
                pointAmount = capped;

                if (plugin.getHookManager().depositPoint(player, pointAmount)) {
                    plugin.sendPlayerMessage(player, pond.getPointRewardMessage().replace("{point_amount}", String.valueOf(pointAmount)));
                    playerData.addPointGained(pointAmount);
                    playerData.addTodayPoint(poolId, pointAmount);
                    playerData.setLastRewardTime(System.currentTimeMillis());
                    plugin.getDataManager().queuePlayerDataSave(playerData);
                    plugin.getUiManager().markDirty(player);
                }
            }

            pointNextEpochSec.put(poolId, epochSec() + intervalSec);
        }, 1L, intervalTicks);

        pointTasks.put(poolId, task);
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
                        commandMap.dispatch(plugin.getServer().getConsoleSender(), processed.startsWith("/") ? processed.substring(1) : processed);
                    } catch (Exception e) {
                        plugin.getLogger().warning("执行命令失败: " + (processed != null ? processed : cmd) + ", 错误: " + e.getMessage());
                    }
                }
            }

            commandNextEpochSec.put(poolId, epochSec() + intervalSec);
        }, 1L, intervalTicks);

        commandTasks.put(poolId, task);
    }

    private long capReward(long amount, long todayTotal, long serverMax, long todayPool, long poolMax,
                           String serverLimitMsg, String poolLimitMsg, Player player) {
        if (serverMax > 0) {
            if (todayTotal >= serverMax) {
                sendLimitMessage(player, serverLimitMsg);
                return -1;
            }
            if (todayTotal + amount > serverMax) {
                amount = serverMax - todayTotal;
            }
        }
        if (poolMax > 0) {
            if (todayPool >= poolMax) {
                sendLimitMessage(player, poolLimitMsg);
                return -1;
            }
            if (todayPool + amount > poolMax) {
                amount = poolMax - todayPool;
            }
        }
        return amount;
    }

    private double capRewardDouble(double amount, double todayTotal, double serverMax, double todayPool, double poolMax,
                                    String serverLimitMsg, String poolLimitMsg, Player player) {
        if (serverMax > 0) {
            if (todayTotal >= serverMax) {
                sendLimitMessage(player, serverLimitMsg);
                return -1;
            }
            if (todayTotal + amount > serverMax) {
                amount = roundMoney(serverMax - todayTotal);
            }
        }
        if (poolMax > 0) {
            if (todayPool >= poolMax) {
                sendLimitMessage(player, poolLimitMsg);
                return -1;
            }
            if (todayPool + amount > poolMax) {
                amount = roundMoney(poolMax - todayPool);
            }
        }
        return roundMoney(amount);
    }

    private int capRewardInt(int amount, double todayTotal, double serverMax, double todayPool, double poolMax,
                              String serverLimitMsg, String poolLimitMsg, Player player) {
        if (serverMax > 0) {
            if (todayTotal >= serverMax) {
                sendLimitMessage(player, serverLimitMsg);
                return -1;
            }
            if (todayTotal + amount > serverMax) {
                amount = (int) (serverMax - todayTotal);
            }
        }
        if (poolMax > 0) {
            if (todayPool >= poolMax) {
                sendLimitMessage(player, poolLimitMsg);
                return -1;
            }
            if (todayPool + amount > poolMax) {
                amount = (int) (poolMax - todayPool);
            }
        }
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

    private boolean isEligibleForReward(Player player, Pond pond) {
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

    public void givePondRewards(Player player, Pond pond) {
        if (!pond.isEnabled() || !isEligibleForReward(player, pond)) return;

        if (pond.isExpEnabled() && plugin.getConfig().getBoolean("rewards.types.xp", true)) {
            long expAmount = (long) (calculateExperienceAmount(pond) * pond.getXpRate());
            if (expAmount > 0) giveReward(player, "xp", expAmount, pond);
        }

        if (pond.isMoneyEnabled() && plugin.getConfig().getBoolean("rewards.types.money", true)) {
            double moneyAmount = calculateMoneyAmount(pond);
            if (moneyAmount > 0) giveReward(player, "money", moneyAmount, pond);
        }

        if (pond.isPointEnabled() && plugin.getConfig().getBoolean("rewards.types.point", true)) {
            int pointAmount = calculatePointAmount(pond);
            if (pointAmount > 0) giveReward(player, "point", pointAmount, pond);
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
