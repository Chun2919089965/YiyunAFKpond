package com.yiyunafkpond.config;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

public class LanguageManager {
    private final YiyunAFKpond plugin;
    private FileConfiguration messages;
    private File messagesFile;

    public LanguageManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            try (InputStream in = plugin.getResource("messages.yml")) {
                if (in != null) {
                    Files.copy(in, messagesFile.toPath());
                    plugin.getLogger().info("已生成默认消息配置文件: messages.yml");
                } else {
                    plugin.getLogger().warning("无法找到内置消息配置文件!");
                    return;
                }
            } catch (IOException e) {
                plugin.getLogger().warning("无法生成消息配置文件: " + e.getMessage());
                return;
            }
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
        plugin.getLogger().info("已加载消息配置文件: messages.yml");
    }

    public String getMessage(String path) {
        if (messages == null) return path;
        String message = messages.getString(path);
        if (message == null) return path;
        return ColorUtil.translateColorCodes(message);
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return message;
    }

    public void reload() {
        loadMessages();
    }
}
