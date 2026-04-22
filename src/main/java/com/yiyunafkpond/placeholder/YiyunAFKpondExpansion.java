package com.yiyunafkpond.placeholder;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.Map;

public class YiyunAFKpondExpansion extends PlaceholderExpansion {
    private final YiyunAFKpond plugin;

    public YiyunAFKpondExpansion(YiyunAFKpond plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "yap";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        PlayerData playerData = plugin.getDataManager().getOrCreatePlayerData(player);
        String pondId = playerData.getCurrentPondId();

        if (identifier.startsWith("pond_")) {
            return handlePondPlaceholder(identifier, playerData, pondId);
        }

        switch (identifier.toLowerCase()) {
            case "afk":
                return playerData.isAfk() ? "是" : "否";

            case "current_pond":
                if (pondId != null) {
                    Pond pond = plugin.getPondManager().getPond(pondId);
                    return pond != null ? pond.getName() : "无";
                }
                return "无";

            case "current_pond_id":
                return pondId != null ? pondId : "无";

            case "today_xp":
                return String.valueOf(playerData.getTodayExp());

            case "today_money":
                return String.format("%.2f", playerData.getTodayMoney());

            case "today_point":
                return String.valueOf(playerData.getTodayPoint());

            case "total_afk_time":
                return String.valueOf(playerData.getTotalAfkTime() / 1000);

            case "total_xp":
                return String.valueOf(playerData.getTotalXpGained());

            case "total_money":
                return String.format("%.2f", playerData.getTotalMoneyGained());

            case "total_point":
                return String.valueOf(playerData.getTotalPointGained());

            case "pool_count":
                return String.valueOf(plugin.getPondManager().getPonds().size());

            default:
                return null;
        }
    }

    private String handlePondPlaceholder(String identifier, PlayerData playerData, String currentPondId) {
        String[] parts = identifier.split("_", 3);
        if (parts.length < 3) return null;

        String targetPondId;
        if (parts[1].equals("current")) {
            if (currentPondId == null) return "0";
            targetPondId = currentPondId;
        } else {
            targetPondId = parts[1];
        }

        Pond pond = plugin.getPondManager().getPond(targetPondId);
        if (pond == null) return null;

        String property = parts[2];

        switch (property) {
            case "name":
                return pond.getName();
            case "enabled":
                return String.valueOf(pond.isEnabled());
            case "online":
                return String.valueOf(plugin.getPondManager().getPlayersInPond(pond).size());
            case "today-xp":
                return String.valueOf(playerData.getDailyExpByPool(targetPondId));
            case "today-money":
                return String.format("%.2f", playerData.getDailyMoneyByPool(targetPondId));
            case "today-point":
                return String.valueOf(playerData.getDailyPointByPool(targetPondId));
            case "xp-max":
                return String.valueOf(pond.getExpMaxDaily());
            case "money-max":
                return String.format("%.2f", pond.getMoneyMaxDaily());
            case "point-max":
                return String.valueOf((int) pond.getPointMaxDaily());
            case "xp-rate":
                return String.valueOf(pond.getXpRate());
            case "money-rate":
                return String.format("%.2f", pond.getMoneyRate());
            case "point-rate":
                return String.valueOf(pond.getPointRate());
            default:
                return null;
        }
    }

    public void registerExpansion() {
        this.register();
    }
}
