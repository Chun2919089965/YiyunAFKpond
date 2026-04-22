package com.yiyunafkpond.hook;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.placeholder.YiyunAFKpondExpansion;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class HookManager {
    private final YiyunAFKpond plugin;
    private boolean placeholderAPIEnabled;
    private Economy economy;
    private PlayerPointsAPI playerPointsAPI;

    public HookManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        this.placeholderAPIEnabled = false;
        this.economy = null;
        this.playerPointsAPI = null;
        init();
    }

    public void init() {
        setupVault();
        setupPlayerPoints();
        setupPlaceholderAPI();
    }

    public void rehookPlugins() {
        plugin.getLogger().info("正在重新尝试挂钩外部插件...");
        init();
    }

    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault 未安装，金币奖励功能不可用");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().info("Vault 已安装，但未找到经济系统");
            return;
        }

        this.economy = rsp.getProvider();
        if (this.economy != null) {
            plugin.getLogger().info("已挂钩 Vault 经济系统");
        } else {
            plugin.getLogger().info("Vault 经济系统获取失败");
        }
    }

    private void setupPlayerPoints() {
        org.bukkit.plugin.Plugin ppPlugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (ppPlugin == null || !ppPlugin.isEnabled()) {
            plugin.getLogger().info("PlayerPoints 未安装，点券奖励功能不可用");
            return;
        }

        try {
            this.playerPointsAPI = ((PlayerPoints) ppPlugin).getAPI();
            if (this.playerPointsAPI != null) {
                plugin.getLogger().info("已挂钩 PlayerPoints 点券系统");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("挂钩 PlayerPoints 失败: " + e.getMessage());
        }
    }

    private void setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            this.placeholderAPIEnabled = false;
            return;
        }

        try {
            new YiyunAFKpondExpansion(plugin).registerExpansion();
            this.placeholderAPIEnabled = true;
            plugin.getLogger().info("已挂钩 PlaceholderAPI");
        } catch (Exception e) {
            plugin.getLogger().warning("注册 PlaceholderAPI 扩展失败: " + e.getMessage());
        }
    }

    public boolean depositMoney(Player player, double amount) {
        if (economy == null || amount <= 0 || player == null) return false;

        try {
            EconomyResponse resp = economy.depositPlayer(player, amount);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info(String.format("Vault 发放金币: %s, 金额: %.2f, 成功: %s",
                        player.getName(), amount, resp.transactionSuccess()));
            }
            return resp.transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().warning("Vault 发放金币失败: " + e.getMessage());
            return false;
        }
    }

    public boolean depositMoney(OfflinePlayer player, double amount) {
        if (economy == null || amount <= 0 || player == null) return false;

        try {
            EconomyResponse resp = economy.depositPlayer(player, amount);
            return resp.transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().warning("Vault 发放金币失败: " + e.getMessage());
            return false;
        }
    }

    public boolean depositMoney(java.util.UUID uuid, double amount) {
        return depositMoney(Bukkit.getOfflinePlayer(uuid), amount);
    }

    public boolean depositPoint(Player player, int amount) {
        if (playerPointsAPI == null || amount <= 0 || player == null) return false;

        try {
            boolean success = playerPointsAPI.give(player.getUniqueId(), amount);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info(String.format("PlayerPoints 发放点券: %s, 数量: %d, 成功: %s",
                        player.getName(), amount, success));
            }
            return success;
        } catch (Exception e) {
            plugin.getLogger().warning("PlayerPoints 发放点券失败: " + e.getMessage());
            return false;
        }
    }

    public boolean isVaultPresent() { return economy != null; }
    public boolean isPlaceholderAPIEnabled() { return placeholderAPIEnabled; }
    public boolean isPlayerPointsPresent() { return playerPointsAPI != null; }
}
