package com.yiyunafkpond.stats;

import com.yiyunafkpond.YiyunAFKpond;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

public class BStatsManager {
    private final YiyunAFKpond plugin;
    private Metrics metrics;
    private static final int PLUGIN_ID = 30848;

    public BStatsManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
    }

    public void init() {
        if (!isEnabled()) {
            plugin.getLogger().info("bStats 统计已禁用");
            return;
        }

        try {
            metrics = new Metrics(plugin, PLUGIN_ID);
            setupCharts();
            plugin.getLogger().info("bStats 统计已启用 (ID: " + PLUGIN_ID + ")");
        } catch (Exception e) {
            plugin.getLogger().warning("bStats 初始化失败: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("stats.bstats-enabled", true);
    }

    private void setupCharts() {
        // 存储类型统计
        metrics.addCustomChart(new SimplePie("storage_type", () -> {
            return plugin.getConfig().getString("storage.type", "yaml");
        }));

        // 挂机池数量统计
        metrics.addCustomChart(new SingleLineChart("pond_count", () -> {
            return plugin.getPondManager().getPonds().size();
        }));

        // 在线玩家数统计（在挂机池中的）
        metrics.addCustomChart(new SingleLineChart("afk_players", () -> {
            int count = 0;
            for (var playerData : plugin.getDataManager().getAllPlayerData()) {
                if (playerData.isAfk()) {
                    count++;
                }
            }
            return count;
        }));

        // 是否启用经验奖励
        metrics.addCustomChart(new SimplePie("exp_enabled", () -> {
            boolean enabled = false;
            for (var pond : plugin.getPondManager().getPonds()) {
                if (pond.isExpEnabled()) {
                    enabled = true;
                    break;
                }
            }
            return enabled ? "是" : "否";
        }));

        // 是否启用金币奖励
        metrics.addCustomChart(new SimplePie("money_enabled", () -> {
            boolean enabled = false;
            for (var pond : plugin.getPondManager().getPonds()) {
                if (pond.isMoneyEnabled()) {
                    enabled = true;
                    break;
                }
            }
            return enabled ? "是" : "否";
        }));

        // 是否启用点券奖励
        metrics.addCustomChart(new SimplePie("point_enabled", () -> {
            boolean enabled = false;
            for (var pond : plugin.getPondManager().getPonds()) {
                if (pond.isPointEnabled()) {
                    enabled = true;
                    break;
                }
            }
            return enabled ? "是" : "否";
        }));

        // 是否使用 MySQL
        metrics.addCustomChart(new SimplePie("use_mysql", () -> {
            String type = plugin.getConfig().getString("storage.type", "yaml");
            return type.equalsIgnoreCase("mysql") ? "是" : "否";
        }));

        // 经验修补是否启用
        metrics.addCustomChart(new SimplePie("mending_enabled", () -> {
            boolean enabled = false;
            for (var pond : plugin.getPondManager().getPonds()) {
                if (pond.isExpApplyMending()) {
                    enabled = true;
                    break;
                }
            }
            return enabled ? "是" : "否";
        }));
    }

    public void reload() {
        shutdown();
        init();
    }
}
