package com.yiyunafkpond;

import com.yiyunafkpond.commands.CommandManager;
import com.yiyunafkpond.commands.YiyunAFKpondTabCompleter;
import com.yiyunafkpond.config.ConfigManager;
import com.yiyunafkpond.config.LanguageManager;
import com.yiyunafkpond.data.DataManager;
import com.yiyunafkpond.hook.HookManager;
import com.yiyunafkpond.libs.LibraryLoader;
import com.yiyunafkpond.listeners.PlayerListener;
import com.yiyunafkpond.listeners.WorldListener;
import com.yiyunafkpond.pond.PondManager;
import com.yiyunafkpond.pond.selection.SelectionManager;
import com.yiyunafkpond.reward.RewardManager;
import com.yiyunafkpond.scheduler.FoliaSchedulerAdapter;
import com.yiyunafkpond.scheduler.SchedulerManager;
import com.yiyunafkpond.security.SecurityManager;
import com.yiyunafkpond.ui.UIManager;
import com.yiyunafkpond.api.YiyunAFKpondAPI;
import com.yiyunafkpond.audit.AuditLogger;
import com.yiyunafkpond.util.ColorUtil;
import com.yiyunafkpond.data.PlayerData;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class YiyunAFKpond extends JavaPlugin {
    private static YiyunAFKpond instance;

    private volatile boolean debugMode = false;
    private volatile boolean pondsLoaded = false;
    private volatile boolean fullyInitialized = false;
    private volatile boolean playerMessageActionbar = false;

    private CommandManager commandManager;
    private ConfigManager configManager;
    private DataManager dataManager;
    private HookManager hookManager;
    private PondManager pondManager;
    private volatile RewardManager rewardManager;
    private SchedulerManager schedulerManager;
    private volatile SelectionManager selectionManager;
    private volatile SecurityManager securityManager;
    private volatile UIManager uiManager;
    private YiyunAFKpondAPI api;
    private AuditLogger auditLogger;
    private com.yiyunafkpond.stats.BStatsManager bStatsManager;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("YiyunAFKpond 插件正在初始化...");

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        configManager = new ConfigManager(this);

        debugMode = configManager.getConfig().getBoolean("core.debug", false);
        playerMessageActionbar = configManager.getConfig().getString("display.player-message-mode", "chat").equalsIgnoreCase("actionbar");

        String storageType = configManager.getConfig().getString("storage.type", "yaml");
        if (storageType.equals("mysql")) {
            LibraryLoader libraryLoader = new LibraryLoader(this);
            if (!libraryLoader.loadHikariCP()) {
                getLogger().warning("HikariCP 加载失败，将使用传统JDBC连接");
            }
        }

        schedulerManager = new SchedulerManager(this);
        dataManager = new DataManager(this);
        pondManager = new PondManager(this);

        hookManager = new HookManager(this);

        FoliaSchedulerAdapter adapter = schedulerManager.getAdapter();

        adapter.runSyncLater(this::initCoreModules, 5L);

        registerListeners();

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onServerLoad(ServerLoadEvent event) {
                if (!pondsLoaded) {
                    loadPondData();
                }
            }
        }, this);

        adapter.runSyncLater(() -> hookManager.rehookPlugins(), 20L);
    }

    private void initCoreModules() {
        selectionManager = new SelectionManager(this);
        securityManager = new SecurityManager(this);
        uiManager = new UIManager(this);
        rewardManager = new RewardManager(this);
        auditLogger = new AuditLogger(this);
        languageManager = new LanguageManager(this);

        commandManager = new CommandManager(this);

        this.api = new YiyunAFKpondAPI(this);

        bStatsManager = new com.yiyunafkpond.stats.BStatsManager(this);
        bStatsManager.init();

        getCommand("yiyunafkpond").setTabCompleter(new YiyunAFKpondTabCompleter(this));

        if (pondsLoaded) {
            rewardManager.startAllRewardTasks();
            startSchedulers();
            rescanPlayersInPonds();
        }

        fullyInitialized = true;
        getLogger().info("YiyunAFKpond 核心模块已初始化，API 已就绪!");
    }

    private void loadPondData() {
        dataManager.loadAllPlayerData();
        pondManager.loadPonds();

        pondsLoaded = true;

        if (rewardManager != null) {
            rewardManager.startAllRewardTasks();
            startSchedulers();
            rescanPlayersInPonds();
        }

        getLogger().info("YiyunAFKpond 挂机池数据加载完成，插件已成功启用!");
        getLogger().info("YiyunAFKpond API 版本: 1.0");
    }

    @Override
    public void onDisable() {
        saveData();
        shutdownSchedulers();
        dataManager.shutdown();

        if (bStatsManager != null) {
            bStatsManager.shutdown();
        }

        if (configManager != null) {
            configManager.stopConfigWatcher();
        }

        instance = null;
        getLogger().info("YiyunAFKpond 插件已成功关闭!");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
    }

    private void saveData() {
        dataManager.saveAllPlayerData();
        pondManager.savePonds();
        if (rewardManager != null) rewardManager.stopAllRewardTasks();
    }

    private void startSchedulers() {
        schedulerManager.startAllSchedulers();
        if (uiManager != null) uiManager.start();
    }

    private void shutdownSchedulers() {
        schedulerManager.shutdownAllSchedulers();
        if (uiManager != null) uiManager.stop();
        if (rewardManager != null) rewardManager.stopAllRewardTasks();
    }

    public static YiyunAFKpond getInstance() {
        return instance;
    }

    public CommandManager getCommandManager() { return commandManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public HookManager getHookManager() { return hookManager; }
    public PondManager getPondManager() { return pondManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public SchedulerManager getSchedulerManager() { return schedulerManager; }
    public SelectionManager getSelectionManager() { return selectionManager; }
    public SecurityManager getSecurityManager() { return securityManager; }
    public UIManager getUiManager() { return uiManager; }
    public AuditLogger getAuditLogger() { return auditLogger; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public YiyunAFKpondAPI getApi() { return api; }

    @Deprecated
    public static YiyunAFKpondAPI getAPI() {
        return YiyunAFKpondAPI.get();
    }

    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }

    public void debug(String message) {
        if (debugMode) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public void debug(String playerName, String message) {
        if (debugMode) {
            getLogger().info("[DEBUG] " + playerName + ": " + message);
        }
    }

    public boolean isPondsLoaded() { return pondsLoaded; }
    public boolean isFullyInitialized() { return fullyInitialized; }

    public void sendPlayerMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;
        if (playerMessageActionbar) {
            ColorUtil.sendActionBar(player, message);
        } else {
            ColorUtil.sendMessage(player, message);
        }
    }

    public void sendPlayerMessage(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) return;
        if (sender instanceof Player player) {
            sendPlayerMessage(player, message);
        } else {
            ColorUtil.sendMessage(sender, message);
        }
    }

    public void rescanPlayersInPonds() {
        for (Player player : getServer().getOnlinePlayers()) {
            Pond pond = pondManager.getPondByLocation(player.getLocation());
            if (pond == null || !pond.isEnabled()) continue;

            if (!securityManager.canPlayerEnterPool(player, pond)) continue;

            if (!securityManager.canPlayerEnterPoolByIp(player, pond)) continue;

            PlayerData playerData = dataManager.getOrCreatePlayerData(player);
            if (playerData == null) continue;

            playerData.setCurrentPondId(pond.getId());
            playerData.setAfk(true);
            securityManager.onPlayerEnterPool(player, pond.getId());
            uiManager.registerPlayerForUpdate(player);
            dataManager.queuePlayerDataSave(playerData);

            debug(player.getName(), "重载后重新检测到玩家在挂机池: " + pond.getName());
        }
    }

    public void onConfigReloaded() {
        getLogger().info("配置文件已变更，正在应用新配置...");

        debugMode = configManager.getConfig().getBoolean("core.debug", false);
        playerMessageActionbar = configManager.getConfig().getString("display.player-message-mode", "chat").equalsIgnoreCase("actionbar");

        languageManager.reload();
        pondManager.reloadPonds();

        schedulerManager.shutdownAllSchedulers();
        schedulerManager.startAllSchedulers();

        if (rewardManager != null) {
            rewardManager.restartAllRewardTasks();
        }

        if (uiManager != null) uiManager.reload();

        if (bStatsManager != null) {
            bStatsManager.reload();
        }

        securityManager.clearIpIndex();
        rescanPlayersInPonds();

        getLogger().info("新配置已成功应用!");
    }
}
