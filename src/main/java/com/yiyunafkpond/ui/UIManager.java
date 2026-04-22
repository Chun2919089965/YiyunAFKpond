package com.yiyunafkpond.ui;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import com.yiyunafkpond.scheduler.FoliaSchedulerAdapter;
import com.yiyunafkpond.util.ColorUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UIManager {
    private final YiyunAFKpond plugin;
    private WrappedTask updateTask;

    private enum BarType {
        EXP, MONEY, POINT
    }

    private final Map<UUID, EnumMap<BarType, BossBar>> playerBossBars = new ConcurrentHashMap<>();
    private final Set<UUID> playersNeedingUIUpdate = ConcurrentHashMap.newKeySet();
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();

    private boolean actionBarEnabled;
    private boolean bossBarEnabled;
    private boolean dynamicBossBarColor;
    private boolean hideWhenMax;
    private boolean hideWhenNoLimit;
    private String actionBarFormat;

    private String expBarTitle;
    private BossBar.Color expBarColor;
    private String moneyBarTitle;
    private BossBar.Color moneyBarColor;
    private String pointBarTitle;
    private BossBar.Color pointBarColor;
    private BossBar.Overlay bossBarOverlay;
    private long updateIntervalTicks;

    private final Map<UUID, CachedMultiBarState> uiStateCache = new ConcurrentHashMap<>();

    public UIManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        actionBarEnabled = plugin.getConfig().getBoolean("display.actionbar.enabled", false);
        bossBarEnabled = plugin.getConfig().getBoolean("display.bossbar.enabled", true);
        dynamicBossBarColor = plugin.getConfig().getBoolean("display.bossbar.dynamic-color", true);
        hideWhenMax = plugin.getConfig().getBoolean("display.bossbar.hide-when-max", true);
        hideWhenNoLimit = plugin.getConfig().getBoolean("display.bossbar.hide-when-no-limit", true);
        actionBarFormat = plugin.getConfig().getString("display.actionbar.format",
                "&#B0E0E6【{pool_name}】 &#87CEEB+{exp} 经验 &#ADD8E6| &#87CEEB+{money} 金币");

        expBarTitle = plugin.getConfig().getString("display.bossbar.exp.title",
                "&#87CEEB【{pool_name}】 &#B0E0E6经验: {pond_today_exp}/{pond_exp_max}");
        moneyBarTitle = plugin.getConfig().getString("display.bossbar.money.title",
                "&#87CEEB【{pool_name}】 &#B0E0E6金币: {pond_today_money}/{pond_money_max}");
        pointBarTitle = plugin.getConfig().getString("display.bossbar.point.title",
                "&#87CEEB【{pool_name}】 &#B0E0E6点券: {pond_today_point}/{pond_point_max}");

        expBarColor = parseBarColor("display.bossbar.exp.color", BossBar.Color.BLUE);
        moneyBarColor = parseBarColor("display.bossbar.money.color", BossBar.Color.BLUE);
        pointBarColor = parseBarColor("display.bossbar.point.color", BossBar.Color.BLUE);

        String overlayName = plugin.getConfig().getString("display.bossbar.style", "NOTCHED_20");
        try {
            bossBarOverlay = BossBar.Overlay.valueOf(overlayName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的BossBar样式配置: " + overlayName + ", 使用默认值NOTCHED_20");
            bossBarOverlay = BossBar.Overlay.NOTCHED_20;
        }

        long updateIntervalSeconds = plugin.getConfig().getLong("display.update-interval", 1);
        updateIntervalTicks = Math.max(1L, updateIntervalSeconds * 20L);
    }

    private BossBar.Color parseBarColor(String configPath, BossBar.Color defaultColor) {
        String colorName = plugin.getConfig().getString(configPath, defaultColor.name());
        try {
            return BossBar.Color.valueOf(colorName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的BossBar颜色配置: " + configPath + "=" + colorName + ", 使用默认值" + defaultColor);
            return defaultColor;
        }
    }

    public void start() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        if (actionBarEnabled || bossBarEnabled) {
            FoliaSchedulerAdapter adapter = plugin.getSchedulerManager().getAdapter();
            updateTask = adapter.runSyncRepeating(this::updateUI, 1L, updateIntervalTicks);

            if (plugin.isDebugMode()) {
                plugin.getLogger().info("UI更新任务已启动，间隔：" + (updateIntervalTicks / 20L) + "秒");
            }
        }
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        for (Map.Entry<UUID, EnumMap<BarType, BossBar>> entry : playerBossBars.entrySet()) {
            Player player = plugin.getSchedulerManager().getAdapter().getPlayer(entry.getKey());
            if (player != null) {
                for (BossBar bar : entry.getValue().values()) {
                    player.hideBossBar(bar);
                }
            }
        }
        playerBossBars.clear();
        uiStateCache.clear();
        dirtyPlayers.clear();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("UI更新任务已停止");
        }
    }

    public void markDirty(Player player) {
        dirtyPlayers.add(player.getUniqueId());
    }

    public void markDirty(UUID playerId) {
        dirtyPlayers.add(playerId);
    }

    public void updateUI() {
        if (!plugin.isEnabled()) return;

        Set<UUID> toUpdate = new HashSet<>(dirtyPlayers);
        toUpdate.addAll(playersNeedingUIUpdate);
        dirtyPlayers.clear();

        for (UUID playerId : toUpdate) {
            Player player = plugin.getSchedulerManager().getAdapter().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                updatePlayerUI(player);
            } else {
                playersNeedingUIUpdate.remove(playerId);
                removeAllBossBars(playerId);
                uiStateCache.remove(playerId);
            }
        }
    }

    public void registerPlayerForUpdate(Player player) {
        playersNeedingUIUpdate.add(player.getUniqueId());
        markDirty(player);
        updatePlayerUI(player);
    }

    public void unregisterPlayerForUpdate(Player player) {
        playersNeedingUIUpdate.remove(player.getUniqueId());
        dirtyPlayers.remove(player.getUniqueId());
        removeAllBossBars(player.getUniqueId());
        uiStateCache.remove(player.getUniqueId());
    }

    private void updatePlayerUI(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerData playerData = plugin.getDataManager().getPlayerDataIfLoaded(player.getUniqueId());

        if (playerData != null && playerData.isAfk() && playerData.getCurrentPondId() != null) {
            Pond currentPond = plugin.getPondManager().getPond(playerData.getCurrentPondId());
            if (currentPond != null) {
                if (actionBarEnabled) {
                    updateActionBar(player, currentPond, playerData);
                }

                if (bossBarEnabled) {
                    updateBossBars(player, currentPond, playerData);
                }
            } else {
                removeAllBossBars(playerId);
                uiStateCache.remove(playerId);
            }
        } else {
            removeAllBossBars(playerId);
            uiStateCache.remove(playerId);
        }
    }

    private void updateActionBar(Player player, Pond pond, PlayerData playerData) {
        String message = buildActionBarMessage(pond, playerData);
        ColorUtil.sendActionBar(player, message);
    }

    private String buildActionBarMessage(Pond pond, PlayerData playerData) {
        int onlinePlayers = plugin.getPondManager().getPlayersInPond(pond).size();

        return ColorUtil.replacePlaceholders(actionBarFormat,
                "{pool_name}", pond.getName(),
                "{pool_id}", pond.getId(),
                "{exp}", formatRewardRange(pond.getExpRewardMode(), pond.getExpFixedAmount(), pond.getExpRandomMin(), pond.getExpRandomMax()),
                "{money}", formatMoneyRange(pond.getMoneyRewardMode(), pond.getMoneyFixedAmount(), pond.getMoneyRandomMin(), pond.getMoneyRandomMax()),
                "{point}", formatPointRange(pond.getPointRewardMode(), pond.getPointFixedAmount(), pond.getPointRandomMin(), pond.getPointRandomMax()),
                "{today_exp}", String.valueOf(playerData.getTodayExp()),
                "{today_money}", String.format("%.2f", playerData.getTodayMoney()),
                "{today_point}", String.valueOf(playerData.getTodayPoint()),
                "{pond_today_exp}", String.valueOf(playerData.getDailyExpByPool(pond.getId())),
                "{pond_today_money}", String.format("%.2f", playerData.getDailyMoneyByPool(pond.getId())),
                "{pond_today_point}", String.valueOf(playerData.getDailyPointByPool(pond.getId())),
                "{online_players}", String.valueOf(onlinePlayers),
                "{exp_max}", pond.getExpMaxDaily() > 0 ? String.valueOf(pond.getExpMaxDaily()) : "∞",
                "{money_max}", pond.getMoneyMaxDaily() > 0 ? String.format("%.2f", pond.getMoneyMaxDaily()) : "∞",
                "{point_max}", pond.getPointMaxDaily() > 0 ? String.valueOf((int) pond.getPointMaxDaily()) : "∞",
                "{pond_exp_max}", pond.getExpMaxDaily() > 0 ? String.valueOf(pond.getExpMaxDaily()) : "∞",
                "{pond_money_max}", pond.getMoneyMaxDaily() > 0 ? String.format("%.2f", pond.getMoneyMaxDaily()) : "∞",
                "{pond_point_max}", pond.getPointMaxDaily() > 0 ? String.valueOf((int) pond.getPointMaxDaily()) : "∞",
                "{exp_rate}", String.valueOf(pond.getXpRate()),
                "{money_rate}", String.format("%.2f", pond.getMoneyRate()),
                "{point_rate}", String.valueOf(pond.getPointRate())
        );
    }

    private void updateBossBars(Player player, Pond pond, PlayerData playerData) {
        UUID playerId = player.getUniqueId();
        String poolId = pond.getId();

        long pondTodayExp = playerData.getDailyExpByPool(poolId);
        double pondTodayMoney = playerData.getDailyMoneyByPool(poolId);
        int pondTodayPoint = playerData.getDailyPointByPool(poolId);

        updateSingleBar(player, playerId, BarType.EXP,
                pond.isExpEnabled(), pond.getExpMaxDaily(), pondTodayExp,
                expBarTitle, expBarColor, pond, playerData);

        updateSingleBar(player, playerId, BarType.MONEY,
                pond.isMoneyEnabled(), pond.getMoneyMaxDaily(), pondTodayMoney,
                moneyBarTitle, moneyBarColor, pond, playerData);

        updateSingleBar(player, playerId, BarType.POINT,
                pond.isPointEnabled(), pond.getPointMaxDaily(), pondTodayPoint,
                pointBarTitle, pointBarColor, pond, playerData);

        uiStateCache.put(playerId, new CachedMultiBarState(poolId, pondTodayExp, pondTodayMoney, pondTodayPoint));
    }

    private void updateSingleBar(Player player, UUID playerId, BarType barType,
                                  boolean rewardEnabled, double maxDaily, double currentAmount,
                                  String titleFormat, BossBar.Color defaultColor,
                                  Pond pond, PlayerData playerData) {
        EnumMap<BarType, BossBar> bars = playerBossBars.computeIfAbsent(playerId, k -> new EnumMap<>(BarType.class));

        if (!rewardEnabled) {
            BossBar existing = bars.remove(barType);
            if (existing != null) {
                player.hideBossBar(existing);
            }
            return;
        }

        if (hideWhenNoLimit && maxDaily <= 0) {
            BossBar existing = bars.remove(barType);
            if (existing != null) {
                player.hideBossBar(existing);
            }
            return;
        }

        double progress = Math.min(1.0, currentAmount / maxDaily);

        if (hideWhenMax && maxDaily > 0 && progress >= 1.0) {
            BossBar existing = bars.remove(barType);
            if (existing != null) {
                player.hideBossBar(existing);
            }
            return;
        }

        String title = buildSingleBarTitle(titleFormat, pond, playerData, barType);
        Component titleComponent = ColorUtil.buildBossBarTitle(title);

        BossBar.Color barColor;
        if (dynamicBossBarColor) {
            barColor = ColorUtil.getBossBarColorByProgress(progress);
        } else {
            barColor = defaultColor;
        }

        BossBar bar = bars.get(barType);
        if (bar == null) {
            bar = BossBar.bossBar(titleComponent, (float) progress, barColor, bossBarOverlay);
            bars.put(barType, bar);
            player.showBossBar(bar);
        } else {
            bar.name(titleComponent);
            bar.progress((float) progress);
            bar.color(barColor);
        }
    }

    private String buildSingleBarTitle(String titleFormat, Pond pond, PlayerData playerData,
                                        BarType barType) {
        String poolId = pond.getId();
        long pondTodayExp = playerData.getDailyExpByPool(poolId);
        double pondTodayMoney = playerData.getDailyMoneyByPool(poolId);
        int pondTodayPoint = playerData.getDailyPointByPool(poolId);
        long pondExpMax = pond.getExpMaxDaily();
        double pondMoneyMax = pond.getMoneyMaxDaily();
        double pondPointMax = pond.getPointMaxDaily();

        return ColorUtil.replacePlaceholders(titleFormat,
                "{pool_name}", pond.getName(),
                "{pool_id}", pond.getId(),
                "{pond_today_exp}", String.valueOf(pondTodayExp),
                "{pond_exp_max}", pondExpMax > 0 ? String.valueOf(pondExpMax) : "∞",
                "{pond_today_money}", String.format("%.2f", pondTodayMoney),
                "{pond_money_max}", pondMoneyMax > 0 ? String.format("%.2f", pondMoneyMax) : "∞",
                "{pond_today_point}", String.valueOf(pondTodayPoint),
                "{pond_point_max}", pondPointMax > 0 ? String.valueOf((int) pondPointMax) : "∞",
                "{today_exp}", String.valueOf(playerData.getTodayExp()),
                "{today_money}", String.format("%.2f", playerData.getTodayMoney()),
                "{today_point}", String.valueOf(playerData.getTodayPoint()),
                "{exp_rate}", String.valueOf(pond.getXpRate()),
                "{money_rate}", String.format("%.2f", pond.getMoneyRate()),
                "{point_rate}", String.valueOf(pond.getPointRate()),
                "{exp}", formatRewardRange(pond.getExpRewardMode(), pond.getExpFixedAmount(), pond.getExpRandomMin(), pond.getExpRandomMax()),
                "{money}", formatMoneyRange(pond.getMoneyRewardMode(), pond.getMoneyFixedAmount(), pond.getMoneyRandomMin(), pond.getMoneyRandomMax()),
                "{point}", formatPointRange(pond.getPointRewardMode(), pond.getPointFixedAmount(), pond.getPointRandomMin(), pond.getPointRandomMax())
        );
    }

    private String formatRewardRange(String mode, long fixed, long randomMin, long randomMax) {
        if (mode.equalsIgnoreCase("fixed")) return String.valueOf(fixed);
        return String.format("%d~%d", randomMin, randomMax);
    }

    private String formatMoneyRange(String mode, double fixed, double randomMin, double randomMax) {
        if (mode.equalsIgnoreCase("fixed")) return String.format("%.2f", fixed);
        return String.format("%.2f~%.2f", randomMin, randomMax);
    }

    private String formatPointRange(String mode, double fixed, double randomMin, double randomMax) {
        if (mode.equalsIgnoreCase("fixed")) return String.format("%.0f", fixed);
        return String.format("%.0f~%.0f", randomMin, randomMax);
    }

    private void removeAllBossBars(UUID playerId) {
        EnumMap<BarType, BossBar> bars = playerBossBars.remove(playerId);
        if (bars != null) {
            Player player = plugin.getSchedulerManager().getAdapter().getPlayer(playerId);
            if (player != null) {
                for (BossBar bar : bars.values()) {
                    player.hideBossBar(bar);
                }
            }
        }
    }

    public void onPlayerQuit(Player player) {
        removeAllBossBars(player.getUniqueId());
        uiStateCache.remove(player.getUniqueId());
        dirtyPlayers.remove(player.getUniqueId());
    }

    public void reload() {
        loadConfig();

        for (Map.Entry<UUID, EnumMap<BarType, BossBar>> entry : new HashMap<>(playerBossBars).entrySet()) {
            Player player = plugin.getSchedulerManager().getAdapter().getPlayer(entry.getKey());
            if (player != null) {
                for (BossBar bar : entry.getValue().values()) {
                    player.hideBossBar(bar);
                }
            }
        }
        playerBossBars.clear();
        uiStateCache.clear();

        if ((actionBarEnabled || bossBarEnabled) && (updateTask == null)) {
            start();
        } else if (!actionBarEnabled && !bossBarEnabled && updateTask != null) {
            stop();
        }
    }

    private static class CachedMultiBarState {
        private final String poolId;
        private final long todayExp;
        private final double todayMoney;
        private final int todayPoint;

        CachedMultiBarState(String poolId, long todayExp, double todayMoney, int todayPoint) {
            this.poolId = poolId;
            this.todayExp = todayExp;
            this.todayMoney = todayMoney;
            this.todayPoint = todayPoint;
        }

        boolean isDirty(String currentPoolId, long currentExp, double currentMoney, int currentPoint) {
            if (!Objects.equals(poolId, currentPoolId)) return true;
            if (todayExp != currentExp) return true;
            if (Math.abs(todayMoney - currentMoney) > 0.001) return true;
            if (todayPoint != currentPoint) return true;
            return false;
        }
    }
}
