package com.yiyunafkpond.config;

import com.yiyunafkpond.YiyunAFKpond;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.ClosedWatchServiceException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigManager {
    private final YiyunAFKpond plugin;
    private FileConfiguration config;
    private File configFile;
    
    // 配置热加载相关
    private WatchService watchService;
    private AtomicBoolean isWatching = new AtomicBoolean(false);
    private Thread watchThread;
    private volatile long lastReloadTime = 0;
    private static final long RELOAD_COOLDOWN = 1000; // 重载冷却时间（毫秒）
    
    public ConfigManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        loadConfig();
        startConfigWatcher();
    }
    
    // 加载配置文件
    public void loadConfig() {
        // 如果配置文件不存在，从资源文件夹复制
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建配置文件: " + e.getMessage());
            }
        }
        
        // 加载配置
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 只在调试模式下输出配置加载信息
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("配置文件已加载!");
        }
    }
    
    // 保存配置文件
    public void saveConfig() {
        try {
            config.save(configFile);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("配置文件已保存!");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存配置文件: " + e.getMessage());
        }
    }
    
    // 获取配置
    public FileConfiguration getConfig() {
        return config;
    }
    
    // 重载配置
    public void reloadConfig() {
        loadConfig();
    }
    
    // 获取配置值，带默认值
    public <T> T getValue(String path, T defaultValue) {
        if (config.contains(path)) {
            return (T) config.get(path);
        }
        return defaultValue;
    }
    
    // 设置配置值
    public void setValue(String path, Object value) {
        config.set(path, value);
    }
    
    // 启动配置文件监听
    private void startConfigWatcher() {
        if (isWatching.get()) {
            return;
        }
        
        try {
            // 初始化WatchService
            watchService = FileSystems.getDefault().newWatchService();
            
            // 获取配置文件所在目录
            Path configDir = configFile.getParentFile().toPath();
            
            // 注册监听事件：修改、创建
            configDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
            
            isWatching.set(true);
            
            // 启动监听线程
            watchThread = new Thread(this::watchConfigChanges, "YiyunAFKpond-ConfigWatcher");
            watchThread.setDaemon(true); // 设置为守护线程，随插件关闭而终止
            watchThread.start();
            
            // 只在调试模式下输出配置热加载信息
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("配置文件热加载已启动!");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法启动配置文件监听: " + e.getMessage());
            isWatching.set(false);
        }
    }
    
    // 停止配置文件监听
    public void stopConfigWatcher() {
        if (!isWatching.get()) {
            return;
        }
        
        isWatching.set(false);
        
        if (watchService != null) {
            try {
                watchService.close();
                watchService = null; // 置空，防止重复关闭和资源泄漏
            } catch (IOException e) {
                plugin.getLogger().severe("无法关闭WatchService: " + e.getMessage());
            }
        }
        
        if (watchThread != null && watchThread.isAlive()) {
            watchThread.interrupt();
            try {
                // 等待线程终止，最多1秒
                watchThread.join(1000);
            } catch (InterruptedException e) {
                plugin.getLogger().warning("等待配置文件监听线程终止时被中断: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
            watchThread = null; // 置空，防止重复中断和资源泄漏
        }
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("配置文件热加载已停止!");
        }
    }
    
    // 监听配置文件变化
    private void watchConfigChanges() {
        while (isWatching.get()) {
            WatchKey key;
            try {
                // 等待事件，超时100ms，便于检查isWatching状态
                key = watchService.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                // WatchService 已被关闭，正常退出循环
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("配置文件监听服务已关闭");
                }
                break;
            } catch (Exception e) {
                // 捕获其他异常，避免线程崩溃
                plugin.getLogger().warning("配置文件监听异常: " + e.getMessage());
                break;
            }
            
            if (key == null) {
                continue;
            }
            
            // 处理事件
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                
                // 忽略OVERFLOW事件
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                
                // 获取文件名
                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();
                
                // 检查是否是配置文件
                if (fileName.toString().equals(configFile.getName())) {
                    // 检查冷却时间，避免短时间内多次重载
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastReloadTime < RELOAD_COOLDOWN) {
                        continue;
                    }
                    lastReloadTime = currentTime;
                    
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("检测到配置文件变化，正在重载...");
                    }
                    
                    plugin.getSchedulerManager().getAdapter().runSync(() -> {
                        reloadConfig();
                        plugin.onConfigReloaded();
                    });
                }
            }
            
            // 重置WatchKey
            boolean valid = key.reset();
            if (!valid) {
                // 监听目录可能已被删除或不可访问
                break;
            }
        }
        
        isWatching.set(false);
    }
}