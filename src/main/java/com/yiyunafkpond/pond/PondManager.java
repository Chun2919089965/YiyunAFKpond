package com.yiyunafkpond.pond;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class PondManager {
    private final YiyunAFKpond plugin;
    private final Map<String, Pond> ponds; // 按ID存储池
    private final Map<String, List<Pond>> pondsByWorld; // 按世界名称存储池，提高查询性能
    private final File pondsFile;
    private FileConfiguration pondsConfig;
    
    public PondManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        this.ponds = new ConcurrentHashMap<>();
        this.pondsByWorld = new ConcurrentHashMap<>();
        this.pondsFile = new File(plugin.getDataFolder(), "ponds.yml");
        
        // 创建 ponds.yml 文件（如果不存在）
        if (!pondsFile.exists()) {
            try (InputStream in = plugin.getResource("ponds.yml")) {
                if (in != null) {
                    Files.copy(in, pondsFile.toPath());
                    plugin.getLogger().info("已从资源文件夹复制 ponds.yml 文件");
                } else {
                    pondsFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe(String.format("无法创建 ponds.yml 文件: %s", e.getMessage()));
            }
        }
        
        this.pondsConfig = YamlConfiguration.loadConfiguration(pondsFile);
    }
    
    // 加载所有池
    public void loadPonds() {
        ponds.clear();
        pondsByWorld.clear();
        
        // 重新加载配置文件
        this.pondsConfig = YamlConfiguration.loadConfiguration(pondsFile);
        
        if (pondsConfig.contains("ponds")) {
            ConfigurationSection pondsSection = pondsConfig.getConfigurationSection("ponds");
            
            for (String pondId : pondsSection.getKeys(false)) {
                ConfigurationSection pondSection = pondsSection.getConfigurationSection(pondId);
                Pond pond = loadPondFromConfig(pondId, pondSection);
                if (pond != null) {
                    ponds.put(pondId, pond);
                    addPondToWorldMap(pond);
                }
            }
        }
        
        plugin.getLogger().info(String.format("已加载 %d 个挂机池!", ponds.size()));
    }
    
    // 将池添加到按世界分组的map中
    private void addPondToWorldMap(Pond pond) {
        if (pond.getWorld() == null) return;
        String worldName = pond.getWorld().getName();
        pondsByWorld.computeIfAbsent(worldName, k -> Collections.synchronizedList(new ArrayList<>())).add(pond);
    }
    
    private void removePondFromWorldMap(Pond pond) {
        if (pond.getWorld() == null) return;
        String worldName = pond.getWorld().getName();
        List<Pond> worldPonds = pondsByWorld.get(worldName);
        if (worldPonds != null) {
            worldPonds.remove(pond);
            // 如果该世界没有池了，移除该世界的条目
            if (worldPonds.isEmpty()) {
                pondsByWorld.remove(worldName);
            }
        }
    }
    
    // 重载所有池配置
    public void reloadPonds() {
        loadPonds();
    }
    
    // 从配置加载池
    private Pond loadPondFromConfig(String pondId, ConfigurationSection pondSection) {
        try {
            String name = pondSection.getString("name", pondId);
            String worldName = pondSection.getString("world");
            
            // 检查世界名称是否为null
            if (worldName == null || worldName.isEmpty()) {
                plugin.getLogger().warning(String.format("池 %s 的世界名称为空，跳过该池", pondId));
                return null;
            }
            
            // 尝试获取世界（现在世界应该已经全部加载完成）
            World world = plugin.getServer().getWorld(worldName);
            
            // 如果世界仍然不存在，跳过该池
            if (world == null) {
                plugin.getLogger().warning(String.format("无法找到世界: %s，跳过池 %s", worldName, pondId));
                return null;
            }
            
            // 加载最小点和最大点
            double minX = pondSection.getDouble("minPoint.x");
            double minY = pondSection.getDouble("minPoint.y");
            double minZ = pondSection.getDouble("minPoint.z");
            Location minPoint = new Location(world, minX, minY, minZ);
            
            double maxX = pondSection.getDouble("maxPoint.x");
            double maxY = pondSection.getDouble("maxPoint.y");
            double maxZ = pondSection.getDouble("maxPoint.z");
            Location maxPoint = new Location(world, maxX, maxY, maxZ);
            
            // 创建池
            Pond pond = new Pond(pondId, name, world, minPoint, maxPoint);
            
            // 加载其他属性
            pond.setEnabled(pondSection.getBoolean("enabled", true));
            pond.setXpRate(pondSection.getDouble("xpRate", 1.0));
            pond.setMoneyRate(pondSection.getDouble("moneyRate", 1.0));
            
            // 加载奖励开关
            pond.setExpEnabled(pondSection.getBoolean("expEnabled", true));
            pond.setMoneyEnabled(pondSection.getBoolean("moneyEnabled", true));
            pond.setPointEnabled(pondSection.getBoolean("pointEnabled", false));
            
            // 加载经验相关属性
            pond.setExpDistributionMode(pondSection.getString("expDistributionMode", "interval"));
            pond.setExpInterval(pondSection.getLong("expInterval", 5));
            pond.setExpRewardMode(pondSection.getString("expRewardMode", "random"));
            pond.setExpRandomMin(pondSection.getLong("expRandomMin", 5));
            pond.setExpRandomMax(pondSection.getLong("expRandomMax", 20));
            pond.setExpFixedAmount(pondSection.getLong("expFixedAmount", 10));
            pond.setExpMaxDaily(pondSection.getLong("expMaxDaily", 0));
            pond.setExpApplyMending(pondSection.getBoolean("expApplyMending", true));
            
            // 加载金币相关属性
            pond.setMoneyRewardMode(pondSection.getString("moneyRewardMode", "random"));
            // 直接使用配置文件中的元数值
            double moneyRandomMin = pondSection.getDouble("moneyRandomMin", 5.0);
            double moneyRandomMax = pondSection.getDouble("moneyRandomMax", 15.0);
            double moneyFixedAmount = pondSection.getDouble("moneyFixedAmount", 10.0);
            double moneyMaxDaily = pondSection.getDouble("moneyMaxDaily", 0.0);
            pond.setMoneyRandomMin(moneyRandomMin);
            pond.setMoneyRandomMax(moneyRandomMax);
            pond.setMoneyFixedAmount(moneyFixedAmount);
            pond.setMoneyInterval(pondSection.getLong("moneyInterval", 30));
            pond.setMoneyMaxDaily(moneyMaxDaily);
            
            // 加载点券相关属性
            pond.setPointRewardMode(pondSection.getString("pointRewardMode", "random"));
            pond.setPointRandomMin(pondSection.getDouble("pointRandomMin", 1.0));
            pond.setPointRandomMax(pondSection.getDouble("pointRandomMax", 5.0));
            pond.setPointFixedAmount(pondSection.getDouble("pointFixedAmount", 3.0));
            pond.setPointInterval(pondSection.getLong("pointInterval", 60));
            pond.setPointMaxDaily(pondSection.getDouble("pointMaxDaily", 0.0));
            pond.setPointRate(pondSection.getDouble("pointRate", 1.0));
            pond.setPointRewardMessage(pondSection.getString("pointRewardMessage", pond.getPointRewardMessage()));
            pond.setPointLimitMessage(pondSection.getString("pointLimitMessage", pond.getPointLimitMessage()));
            
            // 加载命令相关属性
            if (pondSection.contains("commands")) {
                List<String> commands = pondSection.getStringList("commands");
                pond.setCommands(commands);
            }
            pond.setCommandInterval(pondSection.getLong("commandInterval", 120));
            
            // 加载权限相关属性
            pond.setRequiredPermission(pondSection.getString("requiredPermission", null));
            
            // 加载消息相关属性
            pond.setEnterMessage(pondSection.getString("enterMessage", pond.getEnterMessage()));
            pond.setLeaveMessage(pondSection.getString("leaveMessage", pond.getLeaveMessage()));
            pond.setExpRewardMessage(pondSection.getString("expRewardMessage", pond.getExpRewardMessage()));
            pond.setMoneyRewardMessage(pondSection.getString("moneyRewardMessage", pond.getMoneyRewardMessage()));
            pond.setExpLimitMessage(pondSection.getString("expLimitMessage", pond.getExpLimitMessage()));
            pond.setMoneyLimitMessage(pondSection.getString("moneyLimitMessage", pond.getMoneyLimitMessage()));
            
            // 加载自定义奖励
            if (pondSection.contains("customRewards")) {
                ConfigurationSection customRewardsSection = pondSection.getConfigurationSection("customRewards");
                for (String rewardType : customRewardsSection.getKeys(false)) {
                    double amount = customRewardsSection.getDouble(rewardType);
                    pond.addCustomReward(rewardType, amount);
                }
            }
            
            return pond;
        } catch (Exception e) {
            plugin.getLogger().severe(String.format("无法加载池 %s: %s", pondId, e.getMessage()));
            return null;
        }
    }
    
    // 保存所有池（同步）
    public void savePonds() {
        savePonds(false);
    }
    
    // 保存所有池（异步）
    public void savePonds(boolean async) {
        if (async) {
            plugin.getSchedulerManager().runAsync(this::performSave);
        } else {
            performSave();
        }
    }
    
    // 执行实际的保存操作
    private void performSave() {
        // 检查ponds集合是否为空，避免保存空配置
        if (ponds.isEmpty()) {
            plugin.getLogger().warning("没有挂机池可保存，跳过保存操作");
            return;
        }
        
        // 创建临时配置文件，确保保存失败时不会影响原文件
        FileConfiguration tempConfig = new YamlConfiguration();
        ConfigurationSection pondsSection = tempConfig.createSection("ponds");
        
        try {
            // 保存所有池塘到临时配置
            for (Pond pond : ponds.values()) {
                savePondToConfig(pond, pondsSection);
            }
            
            // 保存到文件
            tempConfig.save(pondsFile);
            
            // 保存成功后，更新内存中的配置
            this.pondsConfig = tempConfig;
            
            // 只在调试模式下输出保存信息，减少控制台开销
            if (plugin.isDebugMode()) {
                plugin.getLogger().info(String.format("已保存 %d 个挂机池!", ponds.size()));
            }
        } catch (IOException e) {
            plugin.getLogger().severe(String.format("无法保存 ponds.yml 文件: %s", e.getMessage()));
            // 保存失败时，保持原有的配置不变
        } catch (Exception e) {
            plugin.getLogger().severe(String.format("保存挂机池时发生异常: %s", e.getMessage()));
            // 捕获所有异常，避免保存过程中出现问题导致配置丢失
        }
    }
    
    // 保存池到配置
    private void savePondToConfig(Pond pond, ConfigurationSection pondsSection) {
        ConfigurationSection pondSection = pondsSection.createSection(pond.getId());
        
        pondSection.set("name", pond.getName());
        pondSection.set("world", pond.getWorld() != null ? pond.getWorld().getName() : pond.getWorldName());
        
        // 保存最小点和最大点
        pondSection.set("minPoint.x", pond.getMinPoint().getX());
        pondSection.set("minPoint.y", pond.getMinPoint().getY());
        pondSection.set("minPoint.z", pond.getMinPoint().getZ());
        
        pondSection.set("maxPoint.x", pond.getMaxPoint().getX());
        pondSection.set("maxPoint.y", pond.getMaxPoint().getY());
        pondSection.set("maxPoint.z", pond.getMaxPoint().getZ());
        
        // 保存其他属性
        pondSection.set("enabled", pond.isEnabled());
        pondSection.set("xpRate", pond.getXpRate());
        pondSection.set("moneyRate", pond.getMoneyRate());
        
        // 保存奖励开关
        pondSection.set("expEnabled", pond.isExpEnabled());
        pondSection.set("moneyEnabled", pond.isMoneyEnabled());
        pondSection.set("pointEnabled", pond.isPointEnabled());
        
        // 保存经验相关属性
        pondSection.set("expDistributionMode", pond.getExpDistributionMode());
        pondSection.set("expInterval", pond.getExpInterval());
        pondSection.set("expRewardMode", pond.getExpRewardMode());
        pondSection.set("expRandomMin", pond.getExpRandomMin());
        pondSection.set("expRandomMax", pond.getExpRandomMax());
        pondSection.set("expFixedAmount", pond.getExpFixedAmount());
        pondSection.set("expMaxDaily", pond.getExpMaxDaily());
        pondSection.set("expApplyMending", pond.isExpApplyMending());
        
        // 保存金币相关属性
        pondSection.set("moneyRewardMode", pond.getMoneyRewardMode());
        // 直接保存元数值
        pondSection.set("moneyRandomMin", pond.getMoneyRandomMin());
        pondSection.set("moneyRandomMax", pond.getMoneyRandomMax());
        pondSection.set("moneyFixedAmount", pond.getMoneyFixedAmount());
        pondSection.set("moneyInterval", pond.getMoneyInterval());
        pondSection.set("moneyMaxDaily", pond.getMoneyMaxDaily());
        pondSection.set("moneyRate", pond.getMoneyRate());
        pondSection.set("moneyRewardMessage", pond.getMoneyRewardMessage());
        pondSection.set("moneyLimitMessage", pond.getMoneyLimitMessage());
        
        // 保存点券相关属性
        pondSection.set("pointRewardMode", pond.getPointRewardMode());
        pondSection.set("pointRandomMin", pond.getPointRandomMin());
        pondSection.set("pointRandomMax", pond.getPointRandomMax());
        pondSection.set("pointFixedAmount", pond.getPointFixedAmount());
        pondSection.set("pointInterval", pond.getPointInterval());
        pondSection.set("pointMaxDaily", pond.getPointMaxDaily());
        pondSection.set("pointRate", pond.getPointRate());
        pondSection.set("pointRewardMessage", pond.getPointRewardMessage());
        pondSection.set("pointLimitMessage", pond.getPointLimitMessage());
        
        // 保存命令相关属性
        pondSection.set("commands", pond.getCommands());
        pondSection.set("commandInterval", pond.getCommandInterval());
        
        // 保存权限相关属性
        pondSection.set("requiredPermission", pond.getRequiredPermission());
        
        // 保存消息相关属性
        pondSection.set("enterMessage", pond.getEnterMessage());
        pondSection.set("leaveMessage", pond.getLeaveMessage());
        pondSection.set("expRewardMessage", pond.getExpRewardMessage());
        pondSection.set("moneyRewardMessage", pond.getMoneyRewardMessage());
        pondSection.set("expLimitMessage", pond.getExpLimitMessage());
        pondSection.set("moneyLimitMessage", pond.getMoneyLimitMessage());
        
        // 保存自定义奖励
        if (!pond.getCustomRewards().isEmpty()) {
            ConfigurationSection customRewardsSection = pondSection.createSection("customRewards");
            for (Map.Entry<String, Double> entry : pond.getCustomRewards().entrySet()) {
                customRewardsSection.set(entry.getKey(), entry.getValue());
            }
        }
    }
    
    // 创建池
    public Pond createPond(String id, String name, World world, Location minPoint, Location maxPoint) {
        if (ponds.containsKey(id)) {
            return null; // 池已存在
        }
        
        // 验证坐标有效性
        if (world == null || minPoint == null || maxPoint == null) {
            return null;
        }
        
        if (!minPoint.getWorld().equals(world) || !maxPoint.getWorld().equals(world)) {
            return null;
        }
        
        // 确保minPoint是真正的最小点，maxPoint是真正的最大点
        double minX = Math.min(minPoint.getX(), maxPoint.getX());
        double minY = Math.min(minPoint.getY(), maxPoint.getY());
        double minZ = Math.min(minPoint.getZ(), maxPoint.getZ());
        double maxX = Math.max(minPoint.getX(), maxPoint.getX());
        double maxY = Math.max(minPoint.getY(), maxPoint.getY());
        double maxZ = Math.max(minPoint.getZ(), maxPoint.getZ());
        
        // 验证坐标范围
        if (minX == maxX && minY == maxY && minZ == maxZ) {
            return null; // 坐标点相同，无法形成有效区域
        }
        
        Location adjustedMinPoint = new Location(world, minX, minY, minZ);
        Location adjustedMaxPoint = new Location(world, maxX, maxY, maxZ);
        
        Pond pond = new Pond(id, name, world, adjustedMinPoint, adjustedMaxPoint);
        ponds.put(id, pond);
        addPondToWorldMap(pond);
        return pond;
    }
    
    // 创建池（API用，接受世界名称）
    public Pond createPond(String id, String name, String worldName, 
                          double minX, double minY, double minZ,
                          double maxX, double maxY, double maxZ) {
        if (ponds.containsKey(id)) {
            return null; // 池已存在
        }
        
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }
        
        // 确保minPoint是真正的最小点，maxPoint是真正的最大点
        double adjustedMinX = Math.min(minX, maxX);
        double adjustedMinY = Math.min(minY, maxY);
        double adjustedMinZ = Math.min(minZ, maxZ);
        double adjustedMaxX = Math.max(minX, maxX);
        double adjustedMaxY = Math.max(minY, maxY);
        double adjustedMaxZ = Math.max(minZ, maxZ);
        
        Location minPoint = new Location(world, adjustedMinX, adjustedMinY, adjustedMinZ);
        Location maxPoint = new Location(world, adjustedMaxX, adjustedMaxY, adjustedMaxZ);
        
        Pond pond = new Pond(id, name, world, minPoint, maxPoint);
        ponds.put(id, pond);
        addPondToWorldMap(pond);
        return pond;
    }
    
    // 删除池
    public void removePond(String id) {
        Pond pond = ponds.remove(id);
        if (pond != null) {
            removePondFromWorldMap(pond);
        }
    }
    
    // 删除池（API用，与ChlAFKpond兼容）
    public boolean deletePond(String id) {
        Pond pond = ponds.remove(id);
        if (pond != null) {
            removePondFromWorldMap(pond);
            return true;
        }
        return false;
    }
    
    // 获取池
    public Pond getPond(String id) {
        return ponds.get(id);
    }
    
    // 获取所有池
    public List<Pond> getPonds() {
        return new ArrayList<>(ponds.values());
    }
    
    // 获取所有池（兼容ChlAFKpond）
    public Collection<Pond> getAllPonds() {
        return ponds.values();
    }
    
    // 根据位置获取池
    public Pond getPondByLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        
        // 获取位置所在世界的名称
        String worldName = location.getWorld().getName();
        
        // 只遍历该世界的池，提高性能
        List<Pond> worldPonds = pondsByWorld.get(worldName);
        if (worldPonds != null) {
            for (Pond pond : worldPonds) {
                if (pond.isEnabled() && pond.isInPond(location)) {
                    return pond;
                }
            }
        }
        return null;
    }
    
    // 获取所有池的ID
    public List<String> getPondIds() {
        return new ArrayList<>(ponds.keySet());
    }
    
    // 检查池是否存在
    public boolean pondExists(String id) {
        return ponds.containsKey(id);
    }
    
    // 获取在指定池中的玩家列表
    public List<Player> getPlayersInPond(Pond pond) {
        List<Player> players = new ArrayList<>();
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
            
            if (playerData != null && playerData.isAfk() && playerData.getCurrentPondId() != null && playerData.getCurrentPondId().equals(pond.getId())) {
                players.add(player);
            }
        }
        
        return players;
    }
}