package com.yiyunafkpond.pond.selection;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    // ── 粒子渲染 ──
    private static final double PARTICLE_STEP = 0.3;
    private static final int RENDER_DURATION_TICKS = 200;   // 10 秒
    private static final int RENDER_INTERVAL_TICKS = 8;     // ~2.5 fps
    private static final int MAX_FRAMES = RENDER_DURATION_TICKS / RENDER_INTERVAL_TICKS; // 25
    private static final Color EDGE_COLOR = Color.fromRGB(135, 206, 235); // &#87CEEB 天空蓝
    private static final float PARTICLE_SIZE = 1.2f;

    private final Map<UUID, AtomicInteger> activeRenderFrames = new ConcurrentHashMap<>();

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

        if (sel.isComplete()) {
            renderSelection(player, sel);
            showCompleteMessage(player, sel);
        }
    }

    public void setSecondPoint(Player player, Location location) {
        PondSelection sel = playerSelections.computeIfAbsent(player.getUniqueId(), k -> new PondSelection());
        sel.setSecondPoint(location);
        plugin.sendPlayerMessage(player, "&#87CEEB已设置第二点：&#B0E0E6" + formatLocation(location));

        if (sel.isComplete()) {
            renderSelection(player, sel);
            showCompleteMessage(player, sel);
        }
    }

    private void showCompleteMessage(Player player, PondSelection sel) {
        plugin.sendPlayerMessage(player, "&#87CEEB选区完成！");
        plugin.sendPlayerMessage(player, "&#ADD8E6区域大小：&#B0E0E6" + sel.getSize() + " &#ADD8E6方块");
        plugin.sendPlayerMessage(player, "&#ADD8E6使用 /yiyunafkpond create <池ID> <名称> 创建挂机池");
    }

    public PondSelection getSelection(Player player) {
        return playerSelections.get(player.getUniqueId());
    }

    public void clearSelection(Player player) {
        playerSelections.remove(player.getUniqueId());
        cancelParticleRender(player);
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

    // ==================== 粒子渲染 ====================

    /**
     * 启动（或重启）选区粒子边框渲染。先取消旧渲染再启动新的。
     */
    private void renderSelection(Player player, PondSelection sel) {
        UUID playerId = player.getUniqueId();

        // 取消旧渲染
        cancelParticleRender(player);

        AtomicInteger frameCounter = new AtomicInteger(0);
        activeRenderFrames.put(playerId, frameCounter);

        scheduleNextFrame(player, sel, frameCounter);
    }

    /** 自调度帧循环，每次绘制粒子后延迟 RENDER_INTERVAL_TICKS 调度下一帧 */
    private void scheduleNextFrame(Player player, PondSelection sel, AtomicInteger frameCounter) {
        if (!player.isOnline()) {
            activeRenderFrames.remove(player.getUniqueId());
            return;
        }

        int frame = frameCounter.incrementAndGet();
        if (frame > MAX_FRAMES) {
            activeRenderFrames.remove(player.getUniqueId());
            return;
        }

        drawCuboidEdges(player, sel);

        plugin.getSchedulerManager().getAdapter().runAtEntityLater(player,
            () -> scheduleNextFrame(player, sel, frameCounter),
            RENDER_INTERVAL_TICKS);
    }

    /** 绘制选区立方体的 12 条棱边 */
    private void drawCuboidEdges(Player player, PondSelection sel) {
        Location min = sel.getMinPoint();
        Location max = sel.getMaxPoint();
        if (min == null || max == null) return;

        double minX = min.getX();
        double minY = min.getY();
        double minZ = min.getZ();
        double maxX = max.getX() + 1;  // +1 让粒子落在方块外边缘
        double maxY = max.getY() + 1;
        double maxZ = max.getZ() + 1;

        Particle.DustOptions dust = new Particle.DustOptions(EDGE_COLOR, PARTICLE_SIZE);

        // 底面 (Y = minY)
        drawParticleLine(player, dust, minX, minY, minZ, maxX, minY, minZ);
        drawParticleLine(player, dust, maxX, minY, minZ, maxX, minY, maxZ);
        drawParticleLine(player, dust, maxX, minY, maxZ, minX, minY, maxZ);
        drawParticleLine(player, dust, minX, minY, maxZ, minX, minY, minZ);

        // 顶面 (Y = maxY)
        drawParticleLine(player, dust, minX, maxY, minZ, maxX, maxY, minZ);
        drawParticleLine(player, dust, maxX, maxY, minZ, maxX, maxY, maxZ);
        drawParticleLine(player, dust, maxX, maxY, maxZ, minX, maxY, maxZ);
        drawParticleLine(player, dust, minX, maxY, maxZ, minX, maxY, minZ);

        // 四条垂直棱
        drawParticleLine(player, dust, minX, minY, minZ, minX, maxY, minZ);
        drawParticleLine(player, dust, maxX, minY, minZ, maxX, maxY, minZ);
        drawParticleLine(player, dust, maxX, minY, maxZ, maxX, maxY, maxZ);
        drawParticleLine(player, dust, minX, minY, maxZ, minX, maxY, maxZ);
    }

    /** 在两点间按 PARTICLE_STEP 间隔生成 REDSTONE 粒子 */
    private void drawParticleLine(Player player, Particle.DustOptions dust,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0) return;

        int steps = Math.max(1, (int) (length / PARTICLE_STEP));
        double stepX = dx / steps;
        double stepY = dy / steps;
        double stepZ = dz / steps;

        for (int i = 0; i <= steps; i++) {
            player.spawnParticle(Particle.REDSTONE,
                x1 + stepX * i, y1 + stepY * i, z1 + stepZ * i,
                1, 0, 0, 0, 0, dust);
        }
    }

    /** 取消并清理玩家的粒子渲染 */
    public void cancelParticleRender(Player player) {
        AtomicInteger prev = activeRenderFrames.remove(player.getUniqueId());
        if (prev != null) {
            prev.set(MAX_FRAMES + 1); // 向正在运行的帧循环发出停止信号
        }
    }
}
