package com.yiyunafkpond.data;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.constants.Constants;
import com.yiyunafkpond.libs.LibraryLoader;
import com.yiyunafkpond.scheduler.FoliaSchedulerAdapter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class DataManager {
    private final YiyunAFKpond plugin;
    private final Logger logger;
    private final Map<UUID, PlayerData> playerDataMap;
    private final File dataFolder;
    
    private String storageType;
    private Connection connection;
    private Object dataSource;
    private boolean useConnectionPool;
    private String dbPrefix;
    private File sqliteFile;
    
    private WrappedTask batchSaveTask;
    private WrappedTask cleanupTask;
    private WrappedTask expiredDataCleanupTask;
    private final Map<UUID, Long> lastActivityTimes = new ConcurrentHashMap<>();
    
    private final Map<UUID, PlayerData> pendingSaveQueue = new ConcurrentHashMap<>();
    private static final long BATCH_SAVE_DELAY_TICKS = 100L;
    private static final long BATCH_SAVE_INTERVAL_TICKS = 200L;
    
    private static final int DAILY_DATA_RETENTION_DAYS = 30;
    private static final long EXPIRED_CLEANUP_INTERVAL_TICKS = 20L * 60L * 60L;
    
    public DataManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.playerDataMap = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        
        FileConfiguration config = plugin.getConfigManager().getConfig();
        this.storageType = config.getString("storage.type", "yaml").toLowerCase();
        this.useConnectionPool = config.getBoolean("storage.mysql.useConnectionPool", true);
        
        if (storageType.equals("mysql")) {
            try {
                initMySQL();
            } catch (Exception e) {
                logger.severe("无法连接到 MySQL 数据库，回退到 YAML 存储: " + e.getMessage());
                storageType = "yaml";
                initYAML();
            }
        } else if (storageType.equals("sqlite")) {
            try {
                initSQLite();
            } catch (Exception e) {
                logger.severe("无法连接到 SQLite 数据库，回退到 YAML 存储: " + e.getMessage());
                storageType = "yaml";
                initYAML();
            }
        } else {
            initYAML();
        }
        
        startBatchSaveTask();
        startCleanupTask();
        startExpiredDataCleanupTask();
        
        logger.info("数据管理器已初始化，玩家数据将在登录时加载");
    }
    
    private void initYAML() {
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
        if (plugin.isDebugMode()) {
            logger.info("使用 YAML 存储模式");
        }
    }
    
    private void initSQLite() throws Exception {
        Class.forName("org.sqlite.JDBC");
        this.dbPrefix = sanitizeDbPrefix(plugin.getConfigManager().getConfig().getString("storage.prefix", "yiyunafkpond_"));
        sqliteFile = new File(plugin.getDataFolder(), "data.db");
        String url = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("PRAGMA journal_mode=WAL");
            stmt.executeUpdate("PRAGMA busy_timeout=5000");
            stmt.executeUpdate("PRAGMA synchronous=NORMAL");
        }
        createTables(connection);
        if (plugin.isDebugMode()) {
            logger.info("成功连接到 SQLite 数据库");
        }
    }
    
    private void initMySQL() throws Exception {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        this.dbPrefix = sanitizeDbPrefix(config.getString("storage.prefix", "yiyunafkpond_"));
        
        Class.forName("com.mysql.cj.jdbc.Driver");
        
        String host = config.getString("storage.mysql.host", "localhost");
        int port = config.getInt("storage.mysql.port", 3306);
        String database = config.getString("storage.mysql.database", "yiyunafkpond");
        String username = config.getString("storage.mysql.username", "root");
        String password = config.getString("storage.mysql.password", "password");
        boolean useSSL = config.getBoolean("storage.mysql.useSSL", false);
        
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database 
                + "?useSSL=" + useSSL 
                + "&serverTimezone=Asia/Shanghai"
                + "&useLegacyDatetimeCode=false"
                + "&useJDBCCompliantTimezoneShift=true"
                + "&characterEncoding=utf8mb4"
                + "&allowPublicKeyRetrieval=true";
        
        boolean hikariAvailable = isHikariCPAvailable();
        
        if (useConnectionPool && hikariAvailable) {
            try {
                ClassLoader classLoader = LibraryLoader.getHikariClassLoader();
                if (classLoader == null) {
                    classLoader = getClass().getClassLoader();
                }
                
                Class<?> hikariConfigClass = Class.forName("com.zaxxer.hikari.HikariConfig", true, classLoader);
                Class<?> hikariDataSourceClass = Class.forName("com.zaxxer.hikari.HikariDataSource", true, classLoader);
                
                Object hikariConfig = hikariConfigClass.getDeclaredConstructor().newInstance();
                
                Method setJdbcUrl = hikariConfigClass.getMethod("setJdbcUrl", String.class);
                Method setUsername = hikariConfigClass.getMethod("setUsername", String.class);
                Method setPassword = hikariConfigClass.getMethod("setPassword", String.class);
                Method setPoolName = hikariConfigClass.getMethod("setPoolName", String.class);
                Method setMaximumPoolSize = hikariConfigClass.getMethod("setMaximumPoolSize", int.class);
                Method setMinimumIdle = hikariConfigClass.getMethod("setMinimumIdle", int.class);
                Method setConnectionTimeout = hikariConfigClass.getMethod("setConnectionTimeout", long.class);
                Method setIdleTimeout = hikariConfigClass.getMethod("setIdleTimeout", long.class);
                Method setMaxLifetime = hikariConfigClass.getMethod("setMaxLifetime", long.class);
                
                setJdbcUrl.invoke(hikariConfig, jdbcUrl);
                setUsername.invoke(hikariConfig, username);
                setPassword.invoke(hikariConfig, password);
                setPoolName.invoke(hikariConfig, "YiyunAFKpond-Pool");
                setMaximumPoolSize.invoke(hikariConfig, config.getInt("storage.mysql.pool-size", 10));
                setMinimumIdle.invoke(hikariConfig, config.getInt("storage.mysql.pool-min-idle", 2));
                setConnectionTimeout.invoke(hikariConfig, config.getLong("storage.mysql.connection-timeout", 30000L));
                setIdleTimeout.invoke(hikariConfig, config.getLong("storage.mysql.idle-timeout", 600000L));
                setMaxLifetime.invoke(hikariConfig, config.getLong("storage.mysql.max-lifetime", 1800000L));
                
                Constructor<?> dataSourceConstructor = hikariDataSourceClass.getConstructor(hikariConfigClass);
                this.dataSource = dataSourceConstructor.newInstance(hikariConfig);
                
                if (plugin.isDebugMode()) {
                    logger.info("成功初始化 MySQL 数据库（使用 HikariCP 连接池）");
                }
            } catch (Exception e) {
                logger.warning("HikariCP 反射加载失败，回退到传统连接: " + e.getMessage());
                connection = DriverManager.getConnection(jdbcUrl, username, password);
            }
        } else {
            if (useConnectionPool && !hikariAvailable) {
                logger.warning("HikariCP 不可用，使用传统连接方式");
            }
            connection = DriverManager.getConnection(jdbcUrl, username, password);
        }
        
        Connection initConn = null;
        boolean initIsPooled = dataSource != null;
        try {
            initConn = getConnection();
            createTables(initConn);
        } finally {
            if (initIsPooled && initConn != null) {
                try { initConn.close(); } catch (SQLException ignored) {}
            }
        }
    }
    
    private Connection getConnection() throws SQLException {
        if (dataSource != null) {
            try {
                Method getConnection = dataSource.getClass().getMethod("getConnection");
                return (Connection) getConnection.invoke(dataSource);
            } catch (Exception e) {
                throw new SQLException("无法从连接池获取连接: " + e.getMessage());
            }
        }
        if (connection != null) {
            try {
                if (!connection.isClosed() && connection.isValid(5)) {
                    return connection;
                }
            } catch (SQLException ignored) {
            }
        }
        reconnect();
        if (connection != null) {
            return connection;
        }
        throw new SQLException("数据库连接不可用");
    }
    
    private void createTables(Connection conn) throws SQLException {
        String onUpdateTimestamp = storageType.equals("mysql") ? " ON UPDATE CURRENT_TIMESTAMP" : "";
        String moneyType = storageType.equals("mysql") ? "DECIMAL(12,2)" : "REAL";
        
        try (Statement stmt = conn.createStatement()) {
            if (storageType.equals("sqlite")) {
                stmt.executeUpdate("PRAGMA foreign_keys=ON");
            }
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `" + dbPrefix + "player` ("
                    + "`uuid` CHAR(36) NOT NULL PRIMARY KEY, "
                    + "`name` VARCHAR(16) NOT NULL, "
                    + "`total_afk_time` BIGINT NOT NULL DEFAULT 0, "
                    + "`total_xp_gained` BIGINT NOT NULL DEFAULT 0, "
                    + "`total_money_gained` " + moneyType + " NOT NULL DEFAULT 0.0, "
                    + "`total_point_gained` INT NOT NULL DEFAULT 0, "
                    + "`last_reset` DATE NOT NULL, "
                    + "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "`updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" + onUpdateTimestamp
                    + ")");
            
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `" + dbPrefix + "player_daily` ("
                    + "`uuid` CHAR(36) NOT NULL, "
                    + "`pond_id` VARCHAR(64) NOT NULL, "
                    + "`daily_date` DATE NOT NULL, "
                    + "`daily_exp` BIGINT NOT NULL DEFAULT 0, "
                    + "`daily_money` " + moneyType + " NOT NULL DEFAULT 0.0, "
                    + "`daily_point` INT NOT NULL DEFAULT 0, "
                    + "`afk_time` BIGINT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (`uuid`, `pond_id`, `daily_date`), "
                    + "FOREIGN KEY (`uuid`) REFERENCES `" + dbPrefix + "player`(`uuid`) ON DELETE CASCADE"
                    + ")");
            
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `" + dbPrefix + "player_pond_stats` ("
                    + "`uuid` CHAR(36) NOT NULL, "
                    + "`pond_id` VARCHAR(64) NOT NULL, "
                    + "`total_afk_time` BIGINT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (`uuid`, `pond_id`), "
                    + "FOREIGN KEY (`uuid`) REFERENCES `" + dbPrefix + "player`(`uuid`) ON DELETE CASCADE"
                    + ")");
            
            createIndexes(conn);
        }
    }
    
    private void createIndexes(Connection conn) throws SQLException {
        if (storageType.equals("mysql")) {
            String checkIndex = "SELECT COUNT(*) FROM information_schema.statistics "
                    + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?";
            createIndexIfNotExists(conn, checkIndex, dbPrefix + "player", dbPrefix + "idx_player_name", "name");
            createIndexIfNotExists(conn, checkIndex, dbPrefix + "player_daily", dbPrefix + "idx_daily_date", "daily_date");
            createIndexIfNotExists(conn, checkIndex, dbPrefix + "player_daily", dbPrefix + "idx_daily_pond", "pond_id");
            createIndexIfNotExists(conn, checkIndex, dbPrefix + "player_pond_stats", dbPrefix + "idx_stats_pond", "pond_id");
        } else {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `" + dbPrefix + "idx_player_name` ON `" + dbPrefix + "player` (`name`)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `" + dbPrefix + "idx_daily_date` ON `" + dbPrefix + "player_daily` (`daily_date`)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `" + dbPrefix + "idx_daily_pond` ON `" + dbPrefix + "player_daily` (`pond_id`)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `" + dbPrefix + "idx_stats_pond` ON `" + dbPrefix + "player_pond_stats` (`pond_id`)");
            }
        }
    }
    
    private void createIndexIfNotExists(Connection conn, String checkSql, String tableName, String indexName, String column) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setString(1, tableName);
            pstmt.setString(2, indexName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("CREATE INDEX `" + indexName + "` ON `" + tableName + "` (`" + column + "`)");
                    }
                }
            }
        }
    }
    
    private PlayerData loadSinglePlayerFromYAML(UUID uuid, String playerName) {
        File file = new File(dataFolder, uuid.toString() + ".yml");
        if (!file.exists()) return null;
        
        try {
            FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(file);
            return loadPlayerDataFromConfig(uuid, playerConfig);
        } catch (Exception e) {
            logger.severe("无法加载玩家数据文件 " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }
    
    private PlayerData loadPlayerDataFromConfig(UUID uuid, FileConfiguration config) {
        String name = config.getString("name", "Unknown");
        PlayerData playerData = new PlayerData(uuid, name);
        
        playerData.setTotalAfkTime(config.getLong("totalAfkTime", 0));
        playerData.setTotalXpGained(config.getLong("totalXpGained", 0));
        playerData.setTotalMoneyGained(config.getDouble("totalMoneyGained", 0.0));
        playerData.setTotalPointGained(config.getInt("totalPointGained", 0));
        playerData.setLastRewardTime(config.getLong("lastRewardTime", 0));
        playerData.setAfk(config.getBoolean("isAfk", false));
        playerData.setCurrentPondId(config.getString("currentPondId"));
        
        playerData.setTodayExp(config.getLong("todayExp", 0));
        playerData.setTodayMoney(config.getDouble("todayMoney", 0.0));
        playerData.setTodayPoint((int) config.getDouble("todayPoint", 0.0));
        
        if (config.contains("pondTodayExp")) {
            Map<String, Object> map = config.getConfigurationSection("pondTodayExp").getValues(false);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                playerData.setPoolTodayExp(entry.getKey(), Long.parseLong(entry.getValue().toString()));
            }
        }
        if (config.contains("pondTodayMoney")) {
            Map<String, Object> map = config.getConfigurationSection("pondTodayMoney").getValues(false);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                playerData.setPoolTodayMoney(entry.getKey(), Double.parseDouble(entry.getValue().toString()));
            }
        }
        if (config.contains("pondTodayPoint")) {
            Map<String, Object> map = config.getConfigurationSection("pondTodayPoint").getValues(false);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                playerData.setPoolTodayPoint(entry.getKey(), (int) Double.parseDouble(entry.getValue().toString()));
            }
        }
        if (config.contains("pondTodayAfkTime")) {
            Map<String, Object> map = config.getConfigurationSection("pondTodayAfkTime").getValues(false);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                playerData.setPoolTodayAfkTime(entry.getKey(), Long.parseLong(entry.getValue().toString()));
            }
        }
        if (config.contains("lastReset")) {
            playerData.setLastReset(new java.util.Date(config.getLong("lastReset", System.currentTimeMillis())));
        }
        if (config.contains("pondAfkTimes")) {
            Map<String, Object> map = config.getConfigurationSection("pondAfkTimes").getValues(false);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                playerData.setPondAfkTime(entry.getKey(), Long.parseLong(entry.getValue().toString()));
            }
        }
        
        playerData.checkAndResetDailyData();
        return playerData;
    }
    
    public void loadAllPlayerData() {
        if (storageType.equals("mysql") || storageType.equals("sqlite")) {
            loadAllFromDatabase();
        } else {
            loadAllFromYAML();
        }
    }
    
    private void loadAllFromDatabase() {
        executeWithConnectionVoid(conn -> {
            Map<UUID, PlayerData> loaded = new HashMap<>();
            
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT uuid, name, total_afk_time, total_xp_gained, total_money_gained, total_point_gained, last_reset "
                    + "FROM `" + dbPrefix + "player`");
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    PlayerData pd = new PlayerData(uuid, rs.getString("name"));
                    pd.setTotalAfkTime(rs.getLong("total_afk_time"));
                    pd.setTotalXpGained(rs.getLong("total_xp_gained"));
                    pd.setTotalMoneyGained(rs.getDouble("total_money_gained"));
                    pd.setTotalPointGained(rs.getInt("total_point_gained"));
                    java.sql.Date lastReset = rs.getDate("last_reset");
                    if (lastReset != null) {
                        pd.setLastReset(new java.util.Date(lastReset.getTime()));
                    }
                    loaded.put(uuid, pd);
                }
            }
            
            if (!loaded.isEmpty()) {
                java.sql.Date today = java.sql.Date.valueOf(LocalDate.now());
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT uuid, pond_id, daily_exp, daily_money, daily_point, afk_time "
                        + "FROM `" + dbPrefix + "player_daily` WHERE daily_date = ?")) {
                    pstmt.setDate(1, today);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            PlayerData pd = loaded.get(uuid);
                            if (pd == null) continue;
                            String pondId = rs.getString("pond_id");
                            long exp = rs.getLong("daily_exp");
                            double money = rs.getDouble("daily_money");
                            int point = rs.getInt("daily_point");
                            long afkTime = rs.getLong("afk_time");
                            pd.setTodayExp(pd.getTodayExp() + exp);
                            pd.setTodayMoney(pd.getTodayMoney() + money);
                            pd.setTodayPoint(pd.getTodayPoint() + point);
                            if (exp > 0) pd.setPoolTodayExp(pondId, exp);
                            if (money > 0.0) pd.setPoolTodayMoney(pondId, money);
                            if (point > 0) pd.setPoolTodayPoint(pondId, point);
                            if (afkTime > 0) pd.setPoolTodayAfkTime(pondId, afkTime);
                        }
                    }
                }
                
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT uuid, pond_id, total_afk_time FROM `" + dbPrefix + "player_pond_stats`");
                     ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        PlayerData pd = loaded.get(uuid);
                        if (pd == null) continue;
                        String pondId = rs.getString("pond_id");
                        long afkTime = rs.getLong("total_afk_time");
                        if (afkTime > 0) pd.setPondAfkTime(pondId, afkTime);
                    }
                }
            }
            
            for (Map.Entry<UUID, PlayerData> entry : loaded.entrySet()) {
                playerDataMap.putIfAbsent(entry.getKey(), entry.getValue());
            }
            logger.info("从数据库加载了 " + loaded.size() + " 个玩家数据");
            return null;
        });
    }
    
    private void loadAllFromYAML() {
        if (!dataFolder.exists()) return;
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) return;
        
        int loadedCount = 0;
        for (File file : files) {
            try {
                String uuidStr = file.getName().substring(0, file.getName().length() - 4);
                UUID uuid = UUID.fromString(uuidStr);
                FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(file);
                PlayerData playerData = loadPlayerDataFromConfig(uuid, playerConfig);
                if (playerData != null) {
                    playerDataMap.put(uuid, playerData);
                    loadedCount++;
                }
            } catch (Exception e) {
                logger.severe("加载YAML文件 " + file.getName() + " 失败: " + e.getMessage());
            }
        }
        logger.info("从YAML加载了 " + loadedCount + " 个玩家数据");
    }
    
    public void saveAllPlayerData() {
        saveAllPlayerData(Collections.emptySet());
    }
    
    private void saveAllPlayerData(Set<UUID> exclude) {
        if (storageType.equals("mysql") || storageType.equals("sqlite")) {
            saveToDatabase(exclude);
        } else {
            saveToYAML(exclude);
        }
    }
    
    private void saveToYAML(Set<UUID> exclude) {
        for (PlayerData pd : new ArrayList<>(playerDataMap.values())) {
            if (!exclude.contains(pd.getUuid())) {
                savePlayerDataToYAML(pd);
            }
        }
    }
    
    private void savePlayerDataToYAML(PlayerData pd) {
        File file = new File(dataFolder, pd.getUuid().toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        config.set("name", pd.getName());
        config.set("totalAfkTime", pd.getTotalAfkTime());
        config.set("totalXpGained", pd.getTotalXpGained());
        config.set("totalMoneyGained", pd.getTotalMoneyGained());
        config.set("totalPointGained", pd.getTotalPointGained());
        config.set("lastRewardTime", pd.getLastRewardTime());
        config.set("isAfk", pd.isAfk());
        config.set("currentPondId", pd.getCurrentPondId());
        config.set("todayExp", pd.getTodayExp());
        config.set("todayMoney", pd.getTodayMoney());
        config.set("todayPoint", pd.getTodayPoint());
        config.set("pondTodayExp", pd.getPoolTodayExp());
        config.set("pondTodayMoney", pd.getPoolTodayMoney());
        config.set("pondTodayPoint", pd.getPoolTodayPoint());
        config.set("pondTodayAfkTime", pd.getPoolTodayAfkTime());
        config.set("lastReset", pd.getLastReset().getTime());
        config.set("pondAfkTimes", pd.getPondAfkTimes());
        try {
            config.save(file);
        } catch (IOException e) {
            logger.severe("无法保存玩家数据文件 " + file.getName() + ": " + e.getMessage());
        }
    }
    
    private void saveToDatabase(Set<UUID> exclude) {
        if (playerDataMap.isEmpty()) return;
        
        for (PlayerData pd : new ArrayList<>(playerDataMap.values())) {
            if (exclude.contains(pd.getUuid())) continue;
            try {
                saveSinglePlayerToDatabase(pd);
            } catch (Exception e) {
                logger.severe("保存玩家 " + pd.getName() + " 数据失败: " + e.getMessage());
            }
        }
    }
    
    private void saveSinglePlayerToDatabase(PlayerData data) {
        executeWithConnectionVoid(conn -> {
            savePlayerBase(conn, data);
            savePlayerDaily(conn, data);
            savePlayerPondStats(conn, data);
            return null;
        });
    }
    
    private void savePlayerBase(Connection conn, PlayerData data) throws SQLException {
        String mysqlUpsert = "INSERT INTO `" + dbPrefix + "player` (uuid, name, total_afk_time, total_xp_gained, total_money_gained, total_point_gained, last_reset) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) AS new "
                + "ON DUPLICATE KEY UPDATE name=new.name, total_afk_time=new.total_afk_time, "
                + "total_xp_gained=new.total_xp_gained, total_money_gained=new.total_money_gained, "
                + "total_point_gained=new.total_point_gained, last_reset=new.last_reset";
        String sqliteUpsert = "INSERT INTO `" + dbPrefix + "player` (uuid, name, total_afk_time, total_xp_gained, total_money_gained, total_point_gained, last_reset) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, total_afk_time=excluded.total_afk_time, "
                + "total_xp_gained=excluded.total_xp_gained, total_money_gained=excluded.total_money_gained, "
                + "total_point_gained=excluded.total_point_gained, last_reset=excluded.last_reset";
        
        try (PreparedStatement pstmt = conn.prepareStatement(storageType.equals("sqlite") ? sqliteUpsert : mysqlUpsert)) {
            pstmt.setString(1, data.getUuid().toString());
            pstmt.setString(2, data.getName());
            pstmt.setLong(3, data.getTotalAfkTime());
            pstmt.setLong(4, data.getTotalXpGained());
            pstmt.setDouble(5, data.getTotalMoneyGained());
            pstmt.setInt(6, data.getTotalPointGained());
            pstmt.setDate(7, java.sql.Date.valueOf(
                    data.getLastReset().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()));
            pstmt.executeUpdate();
        }
    }
    
    private void savePlayerDaily(Connection conn, PlayerData data) throws SQLException {
        java.sql.Date today = java.sql.Date.valueOf(LocalDate.now());
        
        String mysqlUpsert = "INSERT INTO `" + dbPrefix + "player_daily` (uuid, pond_id, daily_date, daily_exp, daily_money, daily_point, afk_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) AS new "
                + "ON DUPLICATE KEY UPDATE daily_exp=new.daily_exp, daily_money=new.daily_money, "
                + "daily_point=new.daily_point, afk_time=new.afk_time";
        String sqliteUpsert = "INSERT INTO `" + dbPrefix + "player_daily` (uuid, pond_id, daily_date, daily_exp, daily_money, daily_point, afk_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(uuid, pond_id, daily_date) DO UPDATE SET daily_exp=excluded.daily_exp, daily_money=excluded.daily_money, "
                + "daily_point=excluded.daily_point, afk_time=excluded.afk_time";
        
        try (PreparedStatement pstmt = conn.prepareStatement(storageType.equals("sqlite") ? sqliteUpsert : mysqlUpsert)) {
            Set<String> allPondIds = new HashSet<>();
            allPondIds.addAll(data.getPoolTodayExp().keySet());
            allPondIds.addAll(data.getPoolTodayMoney().keySet());
            allPondIds.addAll(data.getPoolTodayPoint().keySet());
            allPondIds.addAll(data.getPoolTodayAfkTime().keySet());
            
            for (String pondId : allPondIds) {
                pstmt.setString(1, data.getUuid().toString());
                pstmt.setString(2, pondId);
                pstmt.setDate(3, today);
                pstmt.setLong(4, data.getDailyExpByPool(pondId));
                pstmt.setDouble(5, data.getDailyMoneyByPool(pondId));
                pstmt.setInt(6, data.getDailyPointByPool(pondId));
                pstmt.setLong(7, data.getDailyAfkTimeByPool(pondId));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    
    private void savePlayerPondStats(Connection conn, PlayerData data) throws SQLException {
        String mysqlUpsert = "INSERT INTO `" + dbPrefix + "player_pond_stats` (uuid, pond_id, total_afk_time) "
                + "VALUES (?, ?, ?) AS new "
                + "ON DUPLICATE KEY UPDATE total_afk_time=new.total_afk_time";
        String sqliteUpsert = "INSERT INTO `" + dbPrefix + "player_pond_stats` (uuid, pond_id, total_afk_time) "
                + "VALUES (?, ?, ?) "
                + "ON CONFLICT(uuid, pond_id) DO UPDATE SET total_afk_time=excluded.total_afk_time";
        
        try (PreparedStatement pstmt = conn.prepareStatement(storageType.equals("sqlite") ? sqliteUpsert : mysqlUpsert)) {
            for (Map.Entry<String, Long> entry : data.getPondAfkTimes().entrySet()) {
                pstmt.setString(1, data.getUuid().toString());
                pstmt.setString(2, entry.getKey());
                pstmt.setLong(3, entry.getValue());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    
    private void reconnect() {
        logger.info("尝试重新连接数据库...");
        try {
            closeDataSource();
            if (connection != null && !connection.isClosed()) {
                try { connection.close(); } catch (SQLException ignored) {}
                connection = null;
            }
            if (storageType.equals("mysql")) initMySQL();
            else if (storageType.equals("sqlite")) initSQLite();
            logger.info("数据库重新连接成功");
        } catch (Exception e) {
            logger.severe("数据库重新连接失败: " + e.getMessage());
        }
    }
    
    private void closeDataSource() {
        if (dataSource != null) {
            try {
                Method isClosedMethod = dataSource.getClass().getMethod("isClosed");
                Method closeMethod = dataSource.getClass().getMethod("close");
                if (!(boolean) isClosedMethod.invoke(dataSource)) {
                    closeMethod.invoke(dataSource);
                }
                dataSource = null;
            } catch (Exception e) {
                logger.warning("关闭旧 HikariCP 连接池失败: " + e.getMessage());
                dataSource = null;
            }
        }
    }
    
    private PlayerData loadPlayerDataFromDatabase(UUID uuid) {
        return executeWithConnection(conn -> {
            PlayerData pd = null;
            
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT uuid, name, total_afk_time, total_xp_gained, total_money_gained, total_point_gained, last_reset "
                    + "FROM `" + dbPrefix + "player` WHERE uuid = ?")) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        pd = new PlayerData(uuid, rs.getString("name"));
                        pd.setTotalAfkTime(rs.getLong("total_afk_time"));
                        pd.setTotalXpGained(rs.getLong("total_xp_gained"));
                        pd.setTotalMoneyGained(rs.getDouble("total_money_gained"));
                        pd.setTotalPointGained(rs.getInt("total_point_gained"));
                        java.sql.Date lastReset = rs.getDate("last_reset");
                        if (lastReset != null) pd.setLastReset(new java.util.Date(lastReset.getTime()));
                    }
                }
            }
            
            if (pd == null) return null;
            
            java.sql.Date today = java.sql.Date.valueOf(LocalDate.now());
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT pond_id, daily_exp, daily_money, daily_point, afk_time "
                    + "FROM `" + dbPrefix + "player_daily` WHERE uuid = ? AND daily_date = ?")) {
                pstmt.setString(1, uuid.toString());
                pstmt.setDate(2, today);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String pondId = rs.getString("pond_id");
                        long exp = rs.getLong("daily_exp");
                        double money = rs.getDouble("daily_money");
                        int point = rs.getInt("daily_point");
                        long afkTime = rs.getLong("afk_time");
                        pd.setTodayExp(pd.getTodayExp() + exp);
                        pd.setTodayMoney(pd.getTodayMoney() + money);
                        pd.setTodayPoint(pd.getTodayPoint() + point);
                        if (exp > 0) pd.setPoolTodayExp(pondId, exp);
                        if (money > 0.0) pd.setPoolTodayMoney(pondId, money);
                        if (point > 0) pd.setPoolTodayPoint(pondId, point);
                        if (afkTime > 0) pd.setPoolTodayAfkTime(pondId, afkTime);
                    }
                }
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT pond_id, total_afk_time FROM `" + dbPrefix + "player_pond_stats` WHERE uuid = ?")) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String pondId = rs.getString("pond_id");
                        long afkTime = rs.getLong("total_afk_time");
                        if (afkTime > 0) pd.setPondAfkTime(pondId, afkTime);
                    }
                }
            }
            
            return pd;
        });
    }
    
    public PlayerData getOrCreatePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData pd = playerDataMap.computeIfAbsent(uuid, id -> {
            PlayerData loaded = loadPlayerDataDirect(id, player.getName());
            if (loaded != null) return loaded;
            return new PlayerData(id, player.getName());
        });
        
        if (!pd.getName().equals(player.getName())) {
            pd.setName(player.getName());
            queuePlayerDataSave(pd);
        }
        pd.checkAndResetDailyData();
        updatePlayerActivity(uuid);
        return pd;
    }
    
    public PlayerData getPlayerDataIfLoaded(UUID uuid) {
        PlayerData pd = playerDataMap.get(uuid);
        if (pd != null) {
            pd.checkAndResetDailyData();
            updatePlayerActivity(uuid);
        }
        return pd;
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        PlayerData pd = playerDataMap.computeIfAbsent(uuid, id -> {
            PlayerData loaded = loadPlayerDataDirect(id, null);
            if (loaded != null) return loaded;
            return new PlayerData(id, "Unknown");
        });
        pd.checkAndResetDailyData();
        updatePlayerActivity(uuid);
        return pd;
    }
    
    private PlayerData loadPlayerDataDirect(UUID uuid, String playerName) {
        if (storageType.equals("mysql") || storageType.equals("sqlite")) {
            return loadPlayerDataFromDatabase(uuid);
        } else {
            return loadSinglePlayerFromYAML(uuid, playerName);
        }
    }
    
    public void savePlayerData(PlayerData pd) {
        if (storageType.equals("mysql") || storageType.equals("sqlite")) {
            saveSinglePlayerToDatabase(pd);
        } else {
            savePlayerDataToYAML(pd);
        }
    }
    
    public void removePlayerData(UUID uuid) {
        PlayerData pd = playerDataMap.remove(uuid);
        if (pd != null) savePlayerData(pd);
        lastActivityTimes.remove(uuid);
        pendingSaveQueue.remove(uuid);
    }
    
    public Collection<PlayerData> getAllPlayerData() {
        return new ArrayList<>(playerDataMap.values());
    }
    
    public int getPendingSaveCount() {
        return pendingSaveQueue.size();
    }
    
    private void startBatchSaveTask() {
        FoliaSchedulerAdapter adapter = plugin.getSchedulerManager().getAdapter();
        batchSaveTask = adapter.runAsyncRepeating(() -> {
            if (pendingSaveQueue.isEmpty()) return;
            
            Map<UUID, PlayerData> toSave = new HashMap<>(pendingSaveQueue);
            pendingSaveQueue.clear();
            
            if (storageType.equals("mysql") || storageType.equals("sqlite")) {
                Connection conn = null;
                boolean isPooled = dataSource != null;
                try {
                    conn = getConnection();
                    int savedCount = 0;
                    for (PlayerData pd : toSave.values()) {
                        try {
                            savePlayerBase(conn, pd);
                            savePlayerDaily(conn, pd);
                            savePlayerPondStats(conn, pd);
                            savedCount++;
                        } catch (Exception e) {
                            logger.warning("批量保存玩家 " + pd.getName() + " 数据失败: " + e.getMessage());
                        }
                    }
                    if (plugin.isDebugMode() && savedCount > 0) {
                        logger.info("批量保存了 " + savedCount + " 个玩家数据");
                    }
                } catch (SQLException e) {
                    logger.warning("批量保存获取连接失败: " + e.getMessage());
                } finally {
                    if (isPooled && conn != null) {
                        try { conn.close(); } catch (SQLException ignored) {}
                    }
                }
            } else {
                int savedCount = 0;
                for (PlayerData pd : toSave.values()) {
                    try {
                        savePlayerDataToYAML(pd);
                        savedCount++;
                    } catch (Exception e) {
                        logger.warning("批量保存玩家 " + pd.getName() + " 数据失败: " + e.getMessage());
                    }
                }
                if (plugin.isDebugMode() && savedCount > 0) {
                    logger.info("批量保存了 " + savedCount + " 个玩家数据");
                }
            }
        }, BATCH_SAVE_DELAY_TICKS, BATCH_SAVE_INTERVAL_TICKS);
    }
    
    public void queuePlayerDataSave(PlayerData pd) {
        pendingSaveQueue.put(pd.getUuid(), pd);
        updatePlayerActivity(pd.getUuid());
    }
    
    private void startCleanupTask() {
        FoliaSchedulerAdapter adapter = plugin.getSchedulerManager().getAdapter();
        cleanupTask = adapter.runAsyncRepeating(this::cleanupInactivePlayerData,
                Constants.CLEANUP_INTERVAL_SECONDS * Constants.TICKS_PER_SECOND,
                Constants.CLEANUP_INTERVAL_SECONDS * Constants.TICKS_PER_SECOND);
    }
    
    private void startExpiredDataCleanupTask() {
        if (!storageType.equals("mysql") && !storageType.equals("sqlite")) return;
        
        FoliaSchedulerAdapter adapter = plugin.getSchedulerManager().getAdapter();
        expiredDataCleanupTask = adapter.runAsyncRepeating(this::cleanupExpiredDailyData,
                EXPIRED_CLEANUP_INTERVAL_TICKS, EXPIRED_CLEANUP_INTERVAL_TICKS);
    }
    
    private void cleanupExpiredDailyData() {
        LocalDate cutoffDate = LocalDate.now().minusDays(DAILY_DATA_RETENTION_DAYS);
        java.sql.Date cutoffSqlDate = java.sql.Date.valueOf(cutoffDate);
        
        executeWithConnectionVoid(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM `" + dbPrefix + "player_daily` WHERE daily_date < ?")) {
                pstmt.setDate(1, cutoffSqlDate);
                int deleted = pstmt.executeUpdate();
                if (deleted > 0) {
                    logger.info("已清理 " + deleted + " 条超过 " + DAILY_DATA_RETENTION_DAYS + " 天的过期每日数据（截止日期: " + cutoffDate + "）");
                }
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM `" + dbPrefix + "player_pond_stats` WHERE uuid NOT IN "
                    + "(SELECT uuid FROM `" + dbPrefix + "player`)")) {
                int deleted = pstmt.executeUpdate();
                if (deleted > 0) {
                    logger.info("已清理 " + deleted + " 条孤立的池统计数据");
                }
            }
            return null;
        });
    }
    
    private void cleanupInactivePlayerData() {
        long currentTime = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();
        
        for (Map.Entry<UUID, Long> entry : lastActivityTimes.entrySet()) {
            if (currentTime - entry.getValue() > Constants.INACTIVE_THRESHOLD_MILLISECONDS) {
                toRemove.add(entry.getKey());
            }
        }
        
        if (!toRemove.isEmpty()) {
            int removedCount = 0;
            for (UUID uuid : toRemove) {
                PlayerData pd = playerDataMap.remove(uuid);
                if (pd != null) {
                    savePlayerData(pd);
                    removedCount++;
                }
                lastActivityTimes.remove(uuid);
            }
            logger.info("清理了 " + removedCount + " 个不活跃玩家的数据");
        }
    }
    
    private void updatePlayerActivity(UUID uuid) {
        lastActivityTimes.put(uuid, System.currentTimeMillis());
    }
    
    @FunctionalInterface
    private interface SqlOperation<T> {
        T execute(Connection conn) throws SQLException;
    }
    
    private <T> T executeWithConnection(SqlOperation<T> operation) {
        Connection conn = null;
        boolean isPooled = dataSource != null;
        try {
            conn = getConnection();
            return operation.execute(conn);
        } catch (SQLException e) {
            logger.severe("数据库操作失败: " + e.getMessage());
            return null;
        } finally {
            if (isPooled && conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
    
    private void executeWithConnectionVoid(SqlOperation<Void> operation) {
        executeWithConnection(operation);
    }

    private boolean isHikariCPAvailable() {
        return LibraryLoader.isHikariCPAvailable();
    }

    private String sanitizeDbPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return "yiyunafkpond_";
        String sanitized = prefix.replaceAll("[^a-zA-Z0-9_]", "");
        if (sanitized.isEmpty()) {
            logger.warning("数据库前缀包含非法字符，已重置为默认值: yiyunafkpond_");
            return "yiyunafkpond_";
        }
        if (!sanitized.endsWith("_")) sanitized += "_";
        if (!sanitized.equals(prefix)) {
            logger.warning("数据库前缀已从 '" + prefix + "' 修正为 '" + sanitized + "'");
        }
        return sanitized;
    }

    public void shutdown() {
        if (batchSaveTask != null) { batchSaveTask.cancel(); batchSaveTask = null; }
        if (cleanupTask != null) { cleanupTask.cancel(); cleanupTask = null; }
        if (expiredDataCleanupTask != null) { expiredDataCleanupTask.cancel(); expiredDataCleanupTask = null; }
        
        Set<UUID> savedUuids = new HashSet<>();
        if (!pendingSaveQueue.isEmpty()) {
            logger.info("正在保存待保存队列中的 " + pendingSaveQueue.size() + " 个玩家数据...");
            if (storageType.equals("mysql") || storageType.equals("sqlite")) {
                Connection conn = null;
                boolean isPooled = dataSource != null;
                try {
                    conn = getConnection();
                    for (PlayerData pd : pendingSaveQueue.values()) {
                        try {
                            savePlayerBase(conn, pd);
                            savePlayerDaily(conn, pd);
                            savePlayerPondStats(conn, pd);
                            savedUuids.add(pd.getUuid());
                        } catch (Exception e) {
                            logger.warning("保存玩家 " + pd.getName() + " 数据失败: " + e.getMessage());
                        }
                    }
                } catch (SQLException e) {
                    logger.severe("获取连接失败: " + e.getMessage());
                } finally {
                    if (isPooled && conn != null) {
                        try { conn.close(); } catch (SQLException ignored) {}
                    }
                }
            } else {
                for (PlayerData pd : pendingSaveQueue.values()) {
                    try {
                        savePlayerDataToYAML(pd);
                        savedUuids.add(pd.getUuid());
                    } catch (Exception e) {
                        logger.warning("保存玩家 " + pd.getName() + " 数据失败: " + e.getMessage());
                    }
                }
            }
            pendingSaveQueue.clear();
        }
        
        saveAllPlayerData(savedUuids);
        
        closeDataSource();
        
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.info("数据库连接已关闭");
                }
            } catch (SQLException e) {
                logger.severe("关闭数据库连接失败: " + e.getMessage());
            }
        }
    }
}
