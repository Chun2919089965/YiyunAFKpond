package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.pond.Pond;
import com.yiyunafkpond.reward.item.ItemRewardEntry;
import com.yiyunafkpond.reward.item.ItemRewardSettings;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public final class ItemCommand implements SubCommand {
    private final YiyunAFKpond plugin;

    public ItemCommand(YiyunAFKpond plugin) {
        this.plugin = plugin;
    }

    @Override public String getName() { return "item"; }
    @Override public String getDescription() { return "管理挂机池物品奖励"; }
    @Override public String getUsage() { return "item <add|remove|list|test> ..."; }
    @Override public String getPermission() { return "yiyunafkpond.admin.item"; }
    @Override public boolean isPlayerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendUsage(sender);
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        Pond pond = plugin.getPondManager().getPond(args[2].toLowerCase(Locale.ROOT));
        if (pond == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD找不到挂机池: &#87CEEB" + args[2]);
            return true;
        }

        return switch (action) {
            case "add" -> add(sender, args, pond);
            case "remove" -> remove(sender, args, pond);
            case "list" -> list(sender, pond);
            case "test" -> test(sender, args, pond);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean add(CommandSender sender, String[] args, Pond pond) {
        if (!(sender instanceof Player player)) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD添加捕获物品时必须由玩家执行。");
            return true;
        }
        if (args.length < 5) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD用法: /yafk item add <池ID> <奖励ID> <权重> [最小数量] [最大数量]");
            return true;
        }

        String entryId = args[3];
        if (!ItemRewardEntry.isValidId(entryId)) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD奖励ID只能包含字母、数字、下划线和连字符，最长64个字符。");
            return true;
        }

        try {
            double weight = Double.parseDouble(args[4]);
            int minAmount = args.length >= 6 ? Integer.parseInt(args[5]) : 1;
            int maxAmount = args.length >= 7 ? Integer.parseInt(args[6]) : minAmount;
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            ItemRewardEntry entry = new ItemRewardEntry(entryId, ItemRewardEntry.SourceType.CAPTURED,
                    true, weight, minAmount, maxAmount, heldItem);
            pond.getItemRewardSettings().putEntry(entry);
            saveAndRestart(pond);
            plugin.sendPlayerMessage(sender, "&#87CEEB已捕获主手物品并保存奖励 &#B0E0E6" + entryId
                    + " &#87CEEB(权重 " + weight + ", 数量 " + minAmount + "-" + maxAmount + ")");
        } catch (NumberFormatException e) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD权重和数量必须是有效数字。");
        } catch (IllegalArgumentException e) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD无法添加物品奖励: " + e.getMessage());
        }
        return true;
    }

    private boolean remove(CommandSender sender, String[] args, Pond pond) {
        if (args.length < 4) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD用法: /yafk item remove <池ID> <奖励ID>");
            return true;
        }
        ItemRewardEntry removed = pond.getItemRewardSettings().removeEntry(args[3]);
        if (removed == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD物品奖励不存在: &#87CEEB" + args[3]);
            return true;
        }
        saveAndRestart(pond);
        plugin.sendPlayerMessage(sender, "&#87CEEB已删除物品奖励: &#B0E0E6" + removed.getId());
        return true;
    }

    private boolean list(CommandSender sender, Pond pond) {
        ItemRewardSettings settings = pond.getItemRewardSettings();
        plugin.sendPlayerMessage(sender, "&#87CEEB===== " + pond.getName() + " 物品奖励 =====");
        plugin.sendPlayerMessage(sender, "&#ADD8E6状态: " + settings.isEnabled()
                + " | 周期: " + settings.getIntervalSeconds() + "秒 | 抽取: " + settings.getRolls()
                + " | 概率: " + settings.getChance() + "% | 溢出: " + settings.getOverflowPolicy().name().toLowerCase(Locale.ROOT));
        if (settings.getEntries().isEmpty()) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD没有配置物品奖励条目。");
            return true;
        }
        for (ItemRewardEntry entry : settings.getEntries()) {
            plugin.sendPlayerMessage(sender, "&#B0E0E6" + entry.getId() + " &#ADD8E6- "
                    + entry.getTemplate().getType().name() + " x" + entry.getMinAmount() + "-" + entry.getMaxAmount()
                    + " | weight=" + entry.getWeight() + " | enabled=" + entry.isEnabled()
                    + " | source=" + entry.getSourceType().name().toLowerCase(Locale.ROOT));
        }
        return true;
    }

    private boolean test(CommandSender sender, String[] args, Pond pond) {
        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayerExact(args[3]);
        } else {
            target = sender instanceof Player player ? player : null;
        }
        if (target == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD请指定一个在线玩家: /yafk item test <池ID> <玩家>");
            return true;
        }
        plugin.getSchedulerManager().getAdapter().runAtEntity(target, () -> {
            boolean granted = plugin.getRewardManager().getItemRewardService().grantTestReward(target, pond);
            if (granted) {
                sendTestResult(sender, "&#87CEEB已向 &#B0E0E6" + target.getName() + " &#87CEEB发放一次测试物品奖励。");
            } else {
                sendTestResult(sender, "&#6CA6CD测试奖励未发放，请检查目标玩家权限、条目配置和背包空间。");
            }
        });
        return true;
    }

    private void sendTestResult(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            plugin.getSchedulerManager().getAdapter().runAtEntity(player,
                    () -> plugin.sendPlayerMessage(player, message));
        } else {
            plugin.sendPlayerMessage(sender, message);
        }
    }

    private void saveAndRestart(Pond pond) {
        plugin.getPondManager().savePonds();
        if (pond.isEnabled()) plugin.getRewardManager().startPoolRewardTasks(pond);
    }

    private void sendUsage(CommandSender sender) {
        plugin.sendPlayerMessage(sender, "&#87CEEB/yafk item add <池ID> <奖励ID> <权重> [最小数量] [最大数量]");
        plugin.sendPlayerMessage(sender, "&#87CEEB/yafk item remove <池ID> <奖励ID>");
        plugin.sendPlayerMessage(sender, "&#87CEEB/yafk item list <池ID>");
        plugin.sendPlayerMessage(sender, "&#87CEEB/yafk item test <池ID> [玩家]");
    }
}
