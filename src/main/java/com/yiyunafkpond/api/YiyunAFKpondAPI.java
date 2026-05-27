package com.yiyunafkpond.api;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public final class YiyunAFKpondAPI {

    private final YiyunAFKpond plugin;

    public YiyunAFKpondAPI(YiyunAFKpond plugin) {
        this.plugin = plugin;
    }

    public static YiyunAFKpondAPI get() {
        YiyunAFKpond instance = YiyunAFKpond.getInstance();
        return instance != null ? instance.getApi() : null;
    }

    // ==================== 池管理 ====================

    public List<Pond> getAllPonds() {
        return new ArrayList<>(plugin.getPondManager().getAllPonds());
    }

    public Pond getPondById(String pondId) {
        return plugin.getPondManager().getPond(pondId);
    }

    public boolean pondExists(String pondId) {
        return plugin.getPondManager().pondExists(pondId);
    }

    public List<String> getAllPondIds() {
        return plugin.getPondManager().getPondIds();
    }

    public int getPondCount() {
        return plugin.getPondManager().getPonds().size();
    }

    public Pond getPondByLocation(Location location) {
        return plugin.getPondManager().getPondByLocation(location);
    }

    public Pond createPond(String id, String name, String worldName,
                           double minX, double minY, double minZ,
                           double maxX, double maxY, double maxZ) {
        Pond pond = plugin.getPondManager().createPond(id, name, worldName,
                minX, minY, minZ, maxX, maxY, maxZ);
        if (pond != null) {
            plugin.getPondManager().savePonds(true);
            plugin.getRewardManager().startPoolRewardTasks(pond);
        }
        return pond;
    }

    public boolean deletePond(String pondId) {
        Pond pond = plugin.getPondManager().getPond(pondId);
        if (pond == null) return false;

        plugin.getRewardManager().stopPoolRewardTasks(pond);

        List<Player> playersInPond = plugin.getPondManager().getPlayersInPond(pond);
        for (Player player : playersInPond) {
            PlayerData pd = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (pd != null) {
                pd.setAfk(false);
                pd.setCurrentPondId(null);
                plugin.getUiManager().unregisterPlayerForUpdate(player);
            }
        }

        boolean deleted = plugin.getPondManager().deletePond(pondId);
        if (deleted) {
            plugin.getPondManager().savePonds(true);
        }
        return deleted;
    }

    public boolean togglePond(String pondId) {
        Pond pond = plugin.getPondManager().getPond(pondId);
        if (pond == null) return false;
        pond.setEnabled(!pond.isEnabled());
        plugin.getPondManager().savePonds(true);
        if (pond.isEnabled()) {
            plugin.getRewardManager().startPoolRewardTasks(pond);
        } else {
            plugin.getRewardManager().stopPoolRewardTasks(pond);
        }
        return pond.isEnabled();
    }

    public List<Player> getPlayersInPond(String pondId) {
        Pond pond = plugin.getPondManager().getPond(pondId);
        return pond != null ? plugin.getPondManager().getPlayersInPond(pond) : List.of();
    }

    public int getPlayerCountInPond(String pondId) {
        return getPlayersInPond(pondId).size();
    }

    // ==================== 玩家状态查询 ====================

    public boolean isPlayerAfk(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null && pd.isAfk();
    }

    public boolean isPlayerInAnyPond(Player player) {
        return getPlayerCurrentPond(player) != null;
    }

    public boolean isPlayerInPond(Player player, String pondId) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null && pd.isAfk() && pondId.equals(pd.getCurrentPondId());
    }

    public Pond getPlayerCurrentPond(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        if (pd != null && pd.isAfk() && pd.getCurrentPondId() != null) {
            return plugin.getPondManager().getPond(pd.getCurrentPondId());
        }
        return null;
    }

    public String getPlayerCurrentPondId(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? pd.getCurrentPondId() : null;
    }

    // ==================== 玩家数据查询 ====================

    public long getPlayerTotalAfkTime(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? pd.getTotalAfkTime() / 1000 : 0;
    }

    public long getPlayerAfkTimeInPond(Player player, String pondId) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? pd.getPondAfkTimes().getOrDefault(pondId, 0L) / 1000 : 0;
    }

    public long getPlayerTotalXp(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? pd.getTotalXpGained() : 0;
    }

    public double getPlayerTotalMoney(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? pd.getTotalMoneyGained() : 0.0;
    }

    public int getPlayerTotalPoint(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? pd.getTotalPointGained() : 0;
    }

    public long getPlayerTodayXp(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? pd.getTodayExp() : 0;
    }

    public double getPlayerTodayMoney(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? pd.getTodayMoney() : 0.0;
    }

    public int getPlayerTodayPoint(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? pd.getTodayPoint() : 0;
    }

    public long getPlayerTodayXpInPond(Player player, String pondId) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? pd.getDailyExpByPool(pondId) : 0;
    }

    public double getPlayerTodayMoneyInPond(Player player, String pondId) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? pd.getDailyMoneyByPool(pondId) : 0.0;
    }

    public int getPlayerTodayPointInPond(Player player, String pondId) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? pd.getDailyPointByPool(pondId) : 0;
    }

    public Map<String, Long> getPlayerPondAfkTimes(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        return pd != null ? Collections.unmodifiableMap(pd.getPondAfkTimes()) : Collections.emptyMap();
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerDataSafely(player);
    }

    // ==================== 玩家状态操作 ====================

    public void setPlayerAfk(Player player, String pondId) {
        PlayerData pd = plugin.getDataManager().getOrCreatePlayerData(player);
        pd.setAfk(true);
        pd.setCurrentPondId(pondId);
        plugin.getDataManager().queuePlayerDataSave(pd);
        plugin.getUiManager().registerPlayerForUpdate(player);
    }

    public void setPlayerNotAfk(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        if (pd != null) {
            pd.setAfk(false);
            pd.setCurrentPondId(null);
            plugin.getDataManager().queuePlayerDataSave(pd);
            plugin.getUiManager().unregisterPlayerForUpdate(player);
        }
    }

    public void resetPlayerDailyData(Player player) {
        PlayerData pd = getPlayerDataSafely(player);
        if (pd != null) {
            pd.resetDailyData();
            plugin.getDataManager().queuePlayerDataSave(pd);
            plugin.getUiManager().markDirty(player);
        }
    }

    // ==================== 全局查询 ====================

    public List<Player> getAllAfkPlayers() {
        List<Player> afkPlayers = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isPlayerAfk(player)) {
                afkPlayers.add(player);
            }
        }
        return afkPlayers;
    }

    public int getTotalAfkPlayerCount() {
        int count = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isPlayerAfk(player)) count++;
        }
        return count;
    }

    // ==================== 服务器限制 ====================

    public long getServerExpDailyLimit() {
        return plugin.getConfig().getLong("server-limits.total-exp-daily", 0);
    }

    public double getServerMoneyDailyLimit() {
        return plugin.getConfig().getDouble("server-limits.total-money-daily", 0.0);
    }

    public double getServerPointDailyLimit() {
        return plugin.getConfig().getDouble("server-limits.total-point-daily", 0.0);
    }

    // ==================== 插件信息 ====================

    public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }

    public boolean isDebugMode() {
        return plugin.isDebugMode();
    }

    public boolean isFullyInitialized() {
        return plugin.isFullyInitialized();
    }

    // ==================== 内部方法 ====================

    private PlayerData getPlayerDataSafely(Player player) {
        if (player == null) return null;
        return plugin.getDataManager().getPlayerDataIfLoaded(player.getUniqueId());
    }
}
