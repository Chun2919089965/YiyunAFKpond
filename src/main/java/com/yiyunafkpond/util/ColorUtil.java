package com.yiyunafkpond.util;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern AMPERSAND_COLOR_PATTERN = Pattern.compile("&[0-9a-fA-Fk-oK-OrR]");
    private static final String COLOR_CHARS = "0123456789abcdefklmnor";

    private static final String[] LEGACY_TO_MINI = {
        "&0", "<black>", "&1", "<dark_blue>", "&2", "<dark_green>", "&3", "<dark_aqua>",
        "&4", "<dark_red>", "&5", "<dark_purple>", "&6", "<gold>", "&7", "<gray>",
        "&8", "<dark_gray>", "&9", "<blue>", "&a", "<green>", "&b", "<aqua>",
        "&c", "<red>", "&d", "<light_purple>", "&e", "<yellow>", "&f", "<white>",
        "&k", "<obfuscated>", "&l", "<bold>", "&m", "<strikethrough>", "&n", "<underline>",
        "&o", "<italic>", "&r", "<reset>"
    };

    private ColorUtil() {
    }

    public static String translateColorCodes(String message) {
        if (message == null || message.isEmpty()) return message;
        return translateAmpersandToSection(translateHexColors(message));
    }

    private static String translateAmpersandToSection(String message) {
        if (message == null) return null;
        char[] chars = message.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && COLOR_CHARS.indexOf(Character.toLowerCase(chars[i + 1])) > -1) {
                chars[i] = '\u00a7';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    public static String translateHexColors(String message) {
        if (message == null || message.isEmpty()) return message;
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder(message.length() + 32);
        while (matcher.find()) {
            StringBuilder replacement = new StringBuilder("\u00a7x");
            for (char c : matcher.group(1).toCharArray()) {
                replacement.append('\u00a7').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String hexToMiniMessage(String message) {
        if (message == null || message.isEmpty()) return message;
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<color:#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String legacyToMiniMessage(String message) {
        if (message == null || message.isEmpty()) return message;
        String result = message;
        for (int i = 0; i < LEGACY_TO_MINI.length; i += 2) {
            result = result.replace(LEGACY_TO_MINI[i], LEGACY_TO_MINI[i + 1]);
        }
        return result;
    }

    private static String toMiniMessage(String message) {
        if (message == null || message.isEmpty()) return message;
        String result = hexToMiniMessage(message);
        if (AMPERSAND_COLOR_PATTERN.matcher(result).find()) {
            result = legacyToMiniMessage(result);
        }
        return result;
    }

    public static Component parseToComponent(String message) {
        if (message == null || message.isEmpty()) return Component.empty();

        String miniMsg = toMiniMessage(message);
        try {
            return MINI_MESSAGE.deserialize(miniMsg);
        } catch (Exception e) {
            try {
                return LEGACY_AMPERSAND.deserialize(translateColorCodes(message));
            } catch (Exception e2) {
                return Component.text(message);
            }
        }
    }

    public static String parseToString(String message) {
        if (message == null || message.isEmpty()) return message;

        try {
            Component component = parseToComponent(message);
            return LEGACY_SECTION.serialize(component);
        } catch (Exception e) {
            return translateColorCodes(message);
        }
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        if (sender instanceof Player player) {
            player.sendMessage(parseToComponent(message));
        } else {
            sender.sendMessage(parseToString(message));
        }
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;
        player.sendActionBar(parseToComponent(message));
    }

    public static Component buildBossBarTitle(String message) {
        if (message == null || message.isEmpty()) return Component.empty();
        return parseToComponent(message);
    }

    public static BossBar.Color getBossBarColorByProgress(double progress) {
        if (progress >= 0.8) return BossBar.Color.RED;
        if (progress >= 0.5) return BossBar.Color.YELLOW;
        if (progress >= 0.25) return BossBar.Color.GREEN;
        return BossBar.Color.BLUE;
    }

    public static String replacePlaceholders(String message, String... pairs) {
        if (message == null || message.isEmpty()) return message;
        String result = message;
        for (int i = 0; i < pairs.length - 1; i += 2) {
            result = result.replace(pairs[i], pairs[i + 1] != null ? pairs[i + 1] : "");
        }
        return result;
    }

    public static String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return message;
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}
