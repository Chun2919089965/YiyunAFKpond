package com.yiyunafkpond.libs;

import com.yiyunafkpond.YiyunAFKpond;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class LibraryLoader {
    private final YiyunAFKpond plugin;
    private final File libsFolder;
    private static boolean hikariLoaded = false;
    private static URLClassLoader hikariClassLoader;
    
    public LibraryLoader(YiyunAFKpond plugin) {
        this.plugin = plugin;
        this.libsFolder = new File(plugin.getDataFolder(), "libs");
        if (!libsFolder.exists()) {
            libsFolder.mkdirs();
        }
    }
    
    public boolean loadHikariCP() {
        // 先检查是否已经加载
        if (isHikariCPAvailable()) {
            plugin.getLogger().info("HikariCP 已由服务器提供，跳过下载");
            hikariLoaded = true;
            return true;
        }
        
        // 检查是否已经通过此加载器加载
        if (hikariLoaded && hikariClassLoader != null) {
            return true;
        }
        
        plugin.getLogger().info("HikariCP 未找到，正在下载...");
        
        File hikariFile = new File(libsFolder, "HikariCP-5.1.0.jar");
        File slf4jFile = new File(libsFolder, "slf4j-api-2.0.16.jar");
        
        if (!hikariFile.exists()) {
            if (!downloadLibrary("https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar", hikariFile)) {
                return false;
            }
        }
        
        if (!slf4jFile.exists()) {
            if (!downloadLibrary("https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar", slf4jFile)) {
                return false;
            }
        }
        
        // 加载到类加载器
        try {
            URL[] urls = new URL[] {
                hikariFile.toURI().toURL(),
                slf4jFile.toURI().toURL()
            };
            
            ClassLoader parentClassLoader = plugin.getClass().getClassLoader();
            hikariClassLoader = new URLClassLoader(urls, parentClassLoader);
            
            ClassLoader originalContextLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(hikariClassLoader);
                
                Class<?> hikariConfigClass = Class.forName("com.zaxxer.hikari.HikariConfig", true, hikariClassLoader);
                Class<?> hikariDataSourceClass = Class.forName("com.zaxxer.hikari.HikariDataSource", true, hikariClassLoader);
                
                if (hikariConfigClass != null && hikariDataSourceClass != null) {
                    plugin.getLogger().info("HikariCP 加载成功！");
                    hikariLoaded = true;
                    return true;
                } else {
                    plugin.getLogger().severe("HikariCP 类加载失败");
                    return false;
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextLoader);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("HikariCP 加载失败: " + e.getMessage());
            return false;
        }
    }
    
    private boolean downloadLibrary(String url, File target) {
        plugin.getLogger().info("正在下载: " + url);
        
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("下载完成: " + target.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("下载失败: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean isHikariCPAvailable() {
        // 首先检查自定义类加载器
        if (hikariClassLoader != null) {
            try {
                Class.forName("com.zaxxer.hikari.HikariConfig", false, hikariClassLoader);
                return true;
            } catch (ClassNotFoundException e) {
                // 继续检查其他方式
            }
        }
        
        // 检查当前类加载器
        try {
            Class.forName("com.zaxxer.hikari.HikariConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public static boolean isHikariLoaded() {
        return hikariLoaded;
    }
    
    public static URLClassLoader getHikariClassLoader() {
        return hikariClassLoader;
    }
}
