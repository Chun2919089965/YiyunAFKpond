package com.yiyunafkpond.pond;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class Pond {
    private String id;                  // 池唯一标识
    private String name;                // 池名称
    private World world;                // 所属世界
    private Location minPoint;          // 最小点
    private Location maxPoint;          // 最大点
    private boolean enabled;            // 是否启用
    private double xpRate;              // 经验倍率
    private double moneyRate;           // 金币倍率
    private double pointRate;           // 点券倍率
    
    // 奖励开关
    private boolean expEnabled;         // 是否启用经验奖励
    private boolean moneyEnabled;       // 是否启用金币奖励
    private boolean pointEnabled;       // 是否启用点券奖励
    
    private Map<String, Double> customRewards;  // 自定义奖励
    
    // 经验相关
    private String expDistributionMode; // 经验分布模式：continuous 或 interval
    private long expInterval;           // 经验发放间隔（秒）
    private String expRewardMode;       // 经验奖励模式：random 或 fixed
    private long expRandomMin;          // 经验随机奖励最小值
    private long expRandomMax;          // 经验随机奖励最大值
    private long expFixedAmount;        // 经验固定奖励值
    private long expMaxDaily;           // 每日经验上限
    private boolean expApplyMending;    // 经验是否触发经验修补
    
    // 金币相关
    private String moneyRewardMode;     // 金币奖励模式：random 或 fixed
    private double moneyRandomMin;      // 金币随机奖励最小值（元）
    private double moneyRandomMax;      // 金币随机奖励最大值（元）
    private double moneyFixedAmount;    // 金币固定奖励值（元）
    private long moneyInterval;         // 金币发放间隔（秒）
    private double moneyMaxDaily;         // 每日金币上限（元）
    
    // 点券相关
    private String pointRewardMode;     // 点券奖励模式：random 或 fixed
    private double pointRandomMin;      // 点券随机奖励最小值
    private double pointRandomMax;      // 点券随机奖励最大值
    private double pointFixedAmount;    // 点券固定奖励值
    private long pointInterval;         // 点券发放间隔（秒）
    private double pointMaxDaily;         // 每日点券上限
    
    // 消息相关
    private String enterMessage;        // 进入池消息
    private String leaveMessage;        // 离开池消息
    private String expRewardMessage;    // 经验奖励消息
    private String moneyRewardMessage;  // 金币奖励消息
    private String pointRewardMessage;  // 点券奖励消息
    private String expLimitMessage;     // 经验上限消息
    private String moneyLimitMessage;   // 金币上限消息
    private String pointLimitMessage;   // 点券上限消息
    
    // 命令相关
    private List<String> commands;      // 命令列表
    private long commandInterval;       // 命令执行间隔（秒）
    
    // 权限相关
    private String requiredPermission;  // 进入池所需权限
    
    // 构造方法
    public Pond(String id, String name, World world, Location minPoint, Location maxPoint) {
        this.id = id;
        this.name = name;
        this.world = world;
        this.worldName = (world != null) ? world.getName() : null;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.enabled = true;
        this.xpRate = 1.0;
        this.moneyRate = 1.0;
        this.pointRate = 1.0;
        this.customRewards = new ConcurrentHashMap<>();
        
        // 奖励开关默认值
        this.expEnabled = true;
        this.moneyEnabled = true;
        this.pointEnabled = false;  // 点券默认关闭
        
        // 经验默认值
        this.expDistributionMode = "interval";
        this.expInterval = 5;
        this.expRewardMode = "random";
        this.expRandomMin = 5;
        this.expRandomMax = 20;
        this.expFixedAmount = 10;
        this.expMaxDaily = 0;
        this.expApplyMending = true;
        
        // 金币默认值
        this.moneyRewardMode = "random";
        this.moneyRandomMin = 5.0; // 5元
        this.moneyRandomMax = 15.0; // 15元
        this.moneyFixedAmount = 10.0; // 10元
        this.moneyInterval = 30;
        this.moneyMaxDaily = 6666.0; // 6666元
        
        // 点券默认值（默认关闭）
        this.pointRewardMode = "random";
        this.pointRandomMin = 1.0; // 1点券
        this.pointRandomMax = 5.0; // 5点券
        this.pointFixedAmount = 3.0; // 3点券
        this.pointInterval = 0; // 默认关闭点券功能
        this.pointMaxDaily = 0; // 默认关闭点券功能
        
        // 消息默认值
        this.enterMessage = "&#87CEEB(✧ω✧) 欢迎来到 &#B0E0E6" + name + " &#87CEEB挂机池~(～￣▽￣)～";
        this.leaveMessage = "&#87CEEB(｡･ω･｡)ﾉ 感谢使用 &#B0E0E6" + name + " &#87CEEB挂机池，期待下次光临~";
        this.expRewardMessage = "&#87CEEB(✧ω✧) 你的挂机池正在奖励你 &#B0E0E6{xp_amount} &#87CEEB点经验~(～￣▽￣)～";
        this.moneyRewardMessage = "&#87CEEB(๑•̀ㅂ•́)و✧ 你的挂机池正在奖励你 &#B0E0E6{money_amount} &#87CEEB金币~(～￣▽￣)～";
        this.pointRewardMessage = "&#87CEEB(๑•̀ㅂ•́)و✧ 你的挂机池正在奖励你 &#B0E0E6{point_amount} &#87CEEB点券~(～￣▽￣)～";
        this.expLimitMessage = "&#87CEEB(｡･ω･｡) 你今天在 &#B0E0E6" + name + " &#87CEEB挂机池的经验已达挂机池上限~";
        this.moneyLimitMessage = "&#87CEEB(｡･ω･｡) 你今天在 &#B0E0E6" + name + " &#87CEEB挂机池的金币已达挂机池上限~";
        this.pointLimitMessage = "&#87CEEB(｡･ω･｡) 你今天在 &#B0E0E6" + name + " &#87CEEB挂机池的点券已达挂机池上限~";
        
        // 命令默认值
        this.commands = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.commandInterval = 120;
        
        // 权限默认值
        this.requiredPermission = null;
        
        // 初始化缓存的min和max点
        updateCachedPoints();
    }
    
    // 缓存的方块坐标，避免每次isInPond调用都重新计算
    private int cachedMinBlockX;
    private int cachedMinBlockY;
    private int cachedMinBlockZ;
    private int cachedMaxBlockX;
    private int cachedMaxBlockY;
    private int cachedMaxBlockZ;
    private String worldName; // 存储世界名称，即使世界不存在
    
    // 初始化缓存
    private void updateCachedPoints() {
        if (minPoint == null || maxPoint == null) {
            return;
        }
        
        // 计算真实的最小和最大坐标
        double realMinX = Math.min(minPoint.getX(), maxPoint.getX());
        double realMinY = Math.min(minPoint.getY(), maxPoint.getY());
        double realMinZ = Math.min(minPoint.getZ(), maxPoint.getZ());
        double realMaxX = Math.max(minPoint.getX(), maxPoint.getX());
        double realMaxY = Math.max(minPoint.getY(), maxPoint.getY());
        double realMaxZ = Math.max(minPoint.getZ(), maxPoint.getZ());
        
        // 转换为方块坐标并缓存
        this.cachedMinBlockX = (int) Math.floor(realMinX);
        this.cachedMinBlockY = (int) Math.floor(realMinY);
        this.cachedMinBlockZ = (int) Math.floor(realMinZ);
        this.cachedMaxBlockX = (int) Math.ceil(realMaxX) - 1;
        this.cachedMaxBlockY = (int) Math.ceil(realMaxY) - 1;
        this.cachedMaxBlockZ = (int) Math.ceil(realMaxZ) - 1;
    }
    
    // 检查位置是否在池内
    public boolean isInPond(Location location) {
        // 快速检查：位置和世界是否有效
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        // 快速检查：池塘世界是否可用
        if (this.world == null || this.worldName == null) {
            return false;
        }
        
        // 快速检查：世界名称是否匹配
        if (!location.getWorld().getName().equals(this.worldName)) {
            return false;
        }
        
        // 使用方块坐标进行检测
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        // 直接使用缓存的坐标进行比较，避免重复计算
        return x >= cachedMinBlockX && x <= cachedMaxBlockX &&
               y >= cachedMinBlockY && y <= cachedMaxBlockY &&
               z >= cachedMinBlockZ && z <= cachedMaxBlockZ;
    }
    
    // 更新池塘的世界引用
    public void updateWorld(World world) {
        this.world = world;
    }
    
    // Getter 和 Setter 方法
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public World getWorld() {
        return world;
    }
    
    public void setWorld(World world) {
        this.world = world;
        this.worldName = (world != null) ? world.getName() : null;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public Location getMinPoint() {
        return minPoint;
    }
    
    public void setMinPoint(Location minPoint) {
        this.minPoint = minPoint;
        // 更新缓存的min和max点
        updateCachedPoints();
    }
    
    public Location getMaxPoint() {
        return maxPoint;
    }
    
    public void setMaxPoint(Location maxPoint) {
        this.maxPoint = maxPoint;
        // 更新缓存的min和max点
        updateCachedPoints();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public double getXpRate() {
        return xpRate;
    }
    
    public void setXpRate(double xpRate) {
        this.xpRate = xpRate;
    }
    
    public double getMoneyRate() {
        return moneyRate;
    }
    
    public void setMoneyRate(double moneyRate) {
        this.moneyRate = moneyRate;
    }
    
    public Map<String, Double> getCustomRewards() {
        return customRewards;
    }
    
    public void setCustomRewards(Map<String, Double> customRewards) {
        this.customRewards = customRewards;
    }
    
    public void addCustomReward(String type, double amount) {
        this.customRewards.put(type, amount);
    }
    
    public void removeCustomReward(String type) {
        this.customRewards.remove(type);
    }
    
    public String getExpDistributionMode() {
        return expDistributionMode;
    }
    
    public void setExpDistributionMode(String expDistributionMode) {
        this.expDistributionMode = expDistributionMode;
    }
    
    public long getExpInterval() {
        return expInterval;
    }
    
    public void setExpInterval(long expInterval) {
        this.expInterval = expInterval;
    }
    
    public String getExpRewardMode() {
        return expRewardMode;
    }
    
    public void setExpRewardMode(String expRewardMode) {
        this.expRewardMode = expRewardMode;
    }
    
    public long getExpRandomMin() {
        return expRandomMin;
    }
    
    public void setExpRandomMin(long expRandomMin) {
        this.expRandomMin = expRandomMin;
    }
    
    public long getExpRandomMax() {
        return expRandomMax;
    }
    
    public void setExpRandomMax(long expRandomMax) {
        this.expRandomMax = expRandomMax;
    }
    
    public long getExpFixedAmount() {
        return expFixedAmount;
    }
    
    public void setExpFixedAmount(long expFixedAmount) {
        this.expFixedAmount = expFixedAmount;
    }
    
    public long getExpMaxDaily() {
        return expMaxDaily;
    }
    
    public void setExpMaxDaily(long expMaxDaily) {
        this.expMaxDaily = expMaxDaily;
    }
    
    public boolean isExpApplyMending() {
        return expApplyMending;
    }
    
    public void setExpApplyMending(boolean expApplyMending) {
        this.expApplyMending = expApplyMending;
    }
    
    public String getMoneyRewardMode() {
        return moneyRewardMode;
    }
    
    public void setMoneyRewardMode(String moneyRewardMode) {
        this.moneyRewardMode = moneyRewardMode;
    }
    
    public double getMoneyRandomMin() {
        return moneyRandomMin;
    }
    
    public void setMoneyRandomMin(double moneyRandomMin) {
        this.moneyRandomMin = moneyRandomMin;
    }
    
    public double getMoneyRandomMax() {
        return moneyRandomMax;
    }
    
    public void setMoneyRandomMax(double moneyRandomMax) {
        this.moneyRandomMax = moneyRandomMax;
    }
    
    public double getMoneyFixedAmount() {
        return moneyFixedAmount;
    }
    
    public void setMoneyFixedAmount(double moneyFixedAmount) {
        this.moneyFixedAmount = moneyFixedAmount;
    }
    
    public long getMoneyInterval() {
        return moneyInterval;
    }
    
    public void setMoneyInterval(long moneyInterval) {
        this.moneyInterval = moneyInterval;
    }
    
    public double getMoneyMaxDaily() {
        return moneyMaxDaily;
    }
    
    public void setMoneyMaxDaily(double moneyMaxDaily) {
        this.moneyMaxDaily = moneyMaxDaily;
    }
    
    public double getPointRate() {
        return pointRate;
    }
    
    public void setPointRate(double pointRate) {
        this.pointRate = pointRate;
    }
    
    public String getPointRewardMode() {
        return pointRewardMode;
    }
    
    public void setPointRewardMode(String pointRewardMode) {
        this.pointRewardMode = pointRewardMode;
    }
    
    public double getPointRandomMin() {
        return pointRandomMin;
    }
    
    public void setPointRandomMin(double pointRandomMin) {
        this.pointRandomMin = pointRandomMin;
    }
    
    public double getPointRandomMax() {
        return pointRandomMax;
    }
    
    public void setPointRandomMax(double pointRandomMax) {
        this.pointRandomMax = pointRandomMax;
    }
    
    public double getPointFixedAmount() {
        return pointFixedAmount;
    }
    
    public void setPointFixedAmount(double pointFixedAmount) {
        this.pointFixedAmount = pointFixedAmount;
    }
    
    public long getPointInterval() {
        return pointInterval;
    }
    
    public void setPointInterval(long pointInterval) {
        this.pointInterval = pointInterval;
    }
    
    public double getPointMaxDaily() {
        return pointMaxDaily;
    }
    
    public void setPointMaxDaily(double pointMaxDaily) {
        this.pointMaxDaily = pointMaxDaily;
    }
    
    public String getPointRewardMessage() {
        return pointRewardMessage;
    }
    
    public void setPointRewardMessage(String pointRewardMessage) {
        this.pointRewardMessage = pointRewardMessage;
    }
    
    public String getPointLimitMessage() {
        return pointLimitMessage;
    }
    
    public void setPointLimitMessage(String pointLimitMessage) {
        this.pointLimitMessage = pointLimitMessage;
    }
    
    // 奖励开关的getter和setter
    public boolean isExpEnabled() {
        return expEnabled;
    }
    
    public void setExpEnabled(boolean expEnabled) {
        this.expEnabled = expEnabled;
    }
    
    public boolean isMoneyEnabled() {
        return moneyEnabled;
    }
    
    public void setMoneyEnabled(boolean moneyEnabled) {
        this.moneyEnabled = moneyEnabled;
    }
    
    public boolean isPointEnabled() {
        return pointEnabled;
    }
    
    public void setPointEnabled(boolean pointEnabled) {
        this.pointEnabled = pointEnabled;
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
    
    public void addCommand(String command) {
        this.commands.add(command);
    }
    
    public void removeCommand(String command) {
        this.commands.remove(command);
    }
    
    public long getCommandInterval() {
        return commandInterval;
    }
    
    public void setCommandInterval(long commandInterval) {
        this.commandInterval = commandInterval;
    }
    
    public String getRequiredPermission() {
        return requiredPermission;
    }
    
    public void setRequiredPermission(String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }
    
    // 消息相关的getter和setter方法
    public String getEnterMessage() {
        return enterMessage;
    }
    
    public void setEnterMessage(String enterMessage) {
        this.enterMessage = enterMessage;
    }
    
    public String getLeaveMessage() {
        return leaveMessage;
    }
    
    public void setLeaveMessage(String leaveMessage) {
        this.leaveMessage = leaveMessage;
    }
    
    public String getExpRewardMessage() {
        return expRewardMessage;
    }
    
    public void setExpRewardMessage(String expRewardMessage) {
        this.expRewardMessage = expRewardMessage;
    }
    
    public String getMoneyRewardMessage() {
        return moneyRewardMessage;
    }
    
    public void setMoneyRewardMessage(String moneyRewardMessage) {
        this.moneyRewardMessage = moneyRewardMessage;
    }
    
    public String getExpLimitMessage() {
        return expLimitMessage;
    }
    
    public void setExpLimitMessage(String expLimitMessage) {
        this.expLimitMessage = expLimitMessage;
    }
    
    public String getMoneyLimitMessage() {
        return moneyLimitMessage;
    }
    
    public void setMoneyLimitMessage(String moneyLimitMessage) {
        this.moneyLimitMessage = moneyLimitMessage;
    }
    
    // 获取池的大小
    public int getSize() {
        if (minPoint == null || maxPoint == null) {
            return 0;
        }
        
        int width = (int) (maxPoint.getX() - minPoint.getX() + 1);
        int height = (int) (maxPoint.getY() - minPoint.getY() + 1);
        int depth = (int) (maxPoint.getZ() - minPoint.getZ() + 1);
        
        // 确保所有维度都是正数
        width = Math.max(1, width);
        height = Math.max(1, height);
        depth = Math.max(1, depth);
        
        return width * height * depth;
    }
    
    // 获取最小X坐标
    public int getMinX() {
        return minPoint != null ? (int) minPoint.getX() : 0;
    }

    public int getMaxX() {
        return maxPoint != null ? (int) maxPoint.getX() : 0;
    }

    public int getMinY() {
        return minPoint != null ? (int) minPoint.getY() : 0;
    }

    public int getMaxY() {
        return maxPoint != null ? (int) maxPoint.getY() : 0;
    }

    public int getMinZ() {
        return minPoint != null ? (int) minPoint.getZ() : 0;
    }

    public int getMaxZ() {
        return maxPoint != null ? (int) maxPoint.getZ() : 0;
    }
    
    // 获取中心位置
    public Location getCenterLocation() {
        if (world == null || minPoint == null || maxPoint == null) return null;
        double centerX = (minPoint.getX() + maxPoint.getX()) / 2.0;
        double centerY = (minPoint.getY() + maxPoint.getY()) / 2.0;
        double centerZ = (minPoint.getZ() + maxPoint.getZ()) / 2.0;
        return new Location(world, centerX, centerY, centerZ);
    }
    
    @Override
    public String toString() {
        return String.format("Pond{id='%s', name='%s', world='%s'}", id, name, worldName);
    }
}