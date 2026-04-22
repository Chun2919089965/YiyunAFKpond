package com.yiyunafkpond.pond.selection;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SelectionManager {
    private final YiyunAFKpond plugin;
    private final Map<UUID, PondSelection> playerSelections;
    private final Material selectionToolMaterial = Material.BLAZE_ROD;
    private final String selectionToolName = "&#87CEEB[挂机池] 选区工具";
    private final List<String> selectionToolLore = List.of(
            "&#ADD8E6左键设置第一点，右键设置第二点",
            "&#ADD8E6跨世界选择将自动重置",
            "&#6CA6CD唯一标识符: &#B0E0E6YIYUNAFKPOND_SELECTION_TOOL"
    );
    private final int selectionToolCustomModelData = 1000001;
    private final Component expectedDisplayName;
    private final List<Component> expectedLore;
    private final Component uniqueIdentifierComponent;

    public SelectionManager(YiyunAFKpond plugin) {
        this.plugin = plugin;
        this.playerSelections = new HashMap<>();
        this.expectedDisplayName = ColorUtil.parseToComponent(selectionToolName);
        this.expectedLore = new ArrayList<>();
        for (String line : selectionToolLore) {
            expectedLore.add(ColorUtil.parseToComponent(line));
        }
        this.uniqueIdentifierComponent = ColorUtil.parseToComponent("&#6CA6CD唯一标识符: &#B0E0E6YIYUNAFKPOND_SELECTION_TOOL");
    }

    public void giveSelectionTool(Player player) {
        ItemStack tool = new ItemStack(selectionToolMaterial);
        ItemMeta meta = tool.getItemMeta();
        meta.displayName(expectedDisplayName);
        meta.lore(expectedLore);
        meta.setCustomModelData(selectionToolCustomModelData);
        tool.setItemMeta(meta);
        player.getInventory().addItem(tool);
        plugin.sendPlayerMessage(player, "&#87CEEB已获得挂机池选区工具！");
        plugin.sendPlayerMessage(player, "&#B0E0E6左键点击设置第一点，右键点击设置第二点");
        plugin.sendPlayerMessage(player, "&#ADD8E6使用 /yiyunafkpond create <池ID> <名称> 创建挂机池");
    }

    public boolean isSelectionTool(ItemStack item) {
        if (item == null || item.getType() != selectionToolMaterial) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!Objects.equals(expectedDisplayName, meta.displayName())) return false;
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return false;
        if (!lore.contains(uniqueIdentifierComponent)) return false;
        return meta.hasCustomModelData() && meta.getCustomModelData() == selectionToolCustomModelData;
    }

    public void setFirstPoint(Player player, Location location) {
        PondSelection sel = playerSelections.computeIfAbsent(player.getUniqueId(), k -> new PondSelection());
        sel.setFirstPoint(location);
        plugin.sendPlayerMessage(player, "&#87CEEB已设置第一点：&#B0E0E6" + formatLocation(location));
    }

    public void setSecondPoint(Player player, Location location) {
        PondSelection sel = playerSelections.computeIfAbsent(player.getUniqueId(), k -> new PondSelection());
        sel.setSecondPoint(location);
        plugin.sendPlayerMessage(player, "&#87CEEB已设置第二点：&#B0E0E6" + formatLocation(location));
        if (sel.isComplete()) {
            plugin.sendPlayerMessage(player, "&#87CEEB选区完成！");
            plugin.sendPlayerMessage(player, "&#ADD8E6区域大小：&#B0E0E6" + sel.getSize() + " &#ADD8E6方块");
            plugin.sendPlayerMessage(player, "&#ADD8E6使用 /yiyunafkpond create <池ID> <名称> 创建挂机池");
        }
    }

    public PondSelection getSelection(Player player) {
        return playerSelections.get(player.getUniqueId());
    }

    public void clearSelection(Player player) {
        playerSelections.remove(player.getUniqueId());
        plugin.sendPlayerMessage(player, "&#87CEEB已清除选区");
    }

    private String formatLocation(Location loc) {
        return "X:" + loc.getBlockX() + ", Y:" + loc.getBlockY() + ", Z:" + loc.getBlockZ();
    }

    public Material getSelectionToolMaterial() {
        return selectionToolMaterial;
    }

    public String getSelectionToolName() {
        return selectionToolName;
    }
}
