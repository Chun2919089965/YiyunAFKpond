package com.yiyunafkpond.commands;

import com.yiyunafkpond.YiyunAFKpond;
import com.yiyunafkpond.pond.Pond;
import org.bukkit.command.CommandSender;

import java.util.*;

public class SetCommand implements SubCommand {
    private final YiyunAFKpond plugin;

    private static final String HELP_HEADER = "&#87CEEB可用属性:";
    private static final List<String> HELP_LINES = List.of(
            "&#ADD8E6  经验: expInterval, expRewardMode, expRandomMin, expRandomMax, expFixedAmount, expMaxDaily, expApplyMending, xpRate",
            "&#ADD8E6  金币: moneyRewardMode, moneyRandomMin, moneyRandomMax, moneyFixedAmount, moneyInterval, moneyMaxDaily, moneyRate",
            "&#ADD8E6  点券: pointRewardMode, pointRandomMin, pointRandomMax, pointFixedAmount, pointInterval, pointMaxDaily, pointRate",
            "&#ADD8E6  其他: requiredPermission, enabled, enterMessage, leaveMessage",
            "&#ADD8E6  消息: expRewardMessage, moneyRewardMessage, pointRewardMessage, expLimitMessage, moneyLimitMessage, pointLimitMessage"
    );

    private static final Set<String> RESTART_PROPERTIES = Set.of(
            "expinterval", "moneyinterval", "pointinterval", "enabled",
            "moneyrandommin", "moneyrandommax", "moneyfixedamount", "moneyrewardmode",
            "pointrandommin", "pointrandommax", "pointfixedamount", "pointrewardmode",
            "exprandommin", "exprandommax", "expfixedamount", "exprewardmode"
    );

    private interface PropertySetter {
        String apply(Pond pond, String value) throws IllegalArgumentException;
    }

    private final Map<String, PropertySetter> propertySetters;

    public SetCommand(YiyunAFKpond plugin) {
        this.plugin = plugin;
        propertySetters = createPropertySetters();
    }

    private Map<String, PropertySetter> createPropertySetters() {
        Map<String, PropertySetter> map = new LinkedHashMap<>();

        map.put("expdistributionmode", (pond, v) -> setStringVal(pond::getExpDistributionMode, pond::setExpDistributionMode, v));
        map.put("expinterval", (pond, v) -> setLongVal(pond::getExpInterval, pond::setExpInterval, v, 1, "时间间隔必须大于0秒！"));
        map.put("exprewardmode", (pond, v) -> setRewardModeVal(pond::getExpRewardMode, pond::setExpRewardMode, v, "经验"));
        map.put("exprandommin", (pond, v) -> setLongVal(pond::getExpRandomMin, pond::setExpRandomMin, v, 0, "经验随机最小值不能为负数！"));
        map.put("exprandommax", (pond, v) -> setLongVal(pond::getExpRandomMax, pond::setExpRandomMax, v, 0, "经验随机最大值不能为负数！"));
        map.put("expfixedamount", (pond, v) -> setLongVal(pond::getExpFixedAmount, pond::setExpFixedAmount, v, 0, "经验固定奖励值不能为负数！"));
        map.put("expmaxdaily", (pond, v) -> setLongVal(pond::getExpMaxDaily, pond::setExpMaxDaily, v, 0, "每日经验上限不能为负数！"));
        map.put("expapplymending", (pond, v) -> setBoolVal(pond::isExpApplyMending, pond::setExpApplyMending, v));
        map.put("xprate", (pond, v) -> setDoubleVal(pond::getXpRate, pond::setXpRate, v, 0, "经验倍率不能为负数！"));

        map.put("moneyrewardmode", (pond, v) -> setRewardModeVal(pond::getMoneyRewardMode, pond::setMoneyRewardMode, v, "金币"));
        map.put("moneyrandommin", (pond, v) -> setDoubleVal(pond::getMoneyRandomMin, pond::setMoneyRandomMin, v, 0, "金币随机最小值不能为负数！"));
        map.put("moneyrandommax", (pond, v) -> setDoubleVal(pond::getMoneyRandomMax, pond::setMoneyRandomMax, v, 0, "金币随机最大值不能为负数！"));
        map.put("moneyfixedamount", (pond, v) -> setDoubleVal(pond::getMoneyFixedAmount, pond::setMoneyFixedAmount, v, 0, "金币固定奖励值不能为负数！"));
        map.put("moneyinterval", (pond, v) -> setLongVal(pond::getMoneyInterval, pond::setMoneyInterval, v, 1, "时间间隔必须大于0秒！"));
        map.put("moneymaxdaily", (pond, v) -> setDoubleVal(pond::getMoneyMaxDaily, pond::setMoneyMaxDaily, v, 0, "每日金币上限不能为负数！"));
        map.put("moneyrate", (pond, v) -> setDoubleVal(pond::getMoneyRate, pond::setMoneyRate, v, 0, "金币倍率不能为负数！"));

        map.put("pointrewardmode", (pond, v) -> setRewardModeVal(pond::getPointRewardMode, pond::setPointRewardMode, v, "点券"));
        map.put("pointrandommin", (pond, v) -> setDoubleVal(pond::getPointRandomMin, pond::setPointRandomMin, v, 0, "点券随机最小值不能为负数！"));
        map.put("pointrandommax", (pond, v) -> setDoubleVal(pond::getPointRandomMax, pond::setPointRandomMax, v, 0, "点券随机最大值不能为负数！"));
        map.put("pointfixedamount", (pond, v) -> setDoubleVal(pond::getPointFixedAmount, pond::setPointFixedAmount, v, 0, "点券固定奖励值不能为负数！"));
        map.put("pointinterval", (pond, v) -> setLongVal(pond::getPointInterval, pond::setPointInterval, v, 1, "时间间隔必须大于0秒！"));
        map.put("pointmaxdaily", (pond, v) -> setDoubleVal(pond::getPointMaxDaily, pond::setPointMaxDaily, v, 0, "每日点券上限不能为负数！"));
        map.put("pointrate", (pond, v) -> setDoubleVal(pond::getPointRate, pond::setPointRate, v, 0, "点券倍率不能为负数！"));

        map.put("requiredpermission", (pond, v) -> {
            String old = pond.getRequiredPermission() != null ? pond.getRequiredPermission() : "null";
            pond.setRequiredPermission(v.equalsIgnoreCase("null") ? null : v);
            return old;
        });
        map.put("enabled", (pond, v) -> setBoolVal(pond::isEnabled, pond::setEnabled, v));
        map.put("entermessage", (pond, v) -> setStringVal(pond::getEnterMessage, pond::setEnterMessage, v));
        map.put("leavemessage", (pond, v) -> setStringVal(pond::getLeaveMessage, pond::setLeaveMessage, v));
        map.put("exprewardmessage", (pond, v) -> setStringVal(pond::getExpRewardMessage, pond::setExpRewardMessage, v));
        map.put("moneyrewardmessage", (pond, v) -> setStringVal(pond::getMoneyRewardMessage, pond::setMoneyRewardMessage, v));
        map.put("pointrewardmessage", (pond, v) -> setStringVal(pond::getPointRewardMessage, pond::setPointRewardMessage, v));
        map.put("explimitmessage", (pond, v) -> setStringVal(pond::getExpLimitMessage, pond::setExpLimitMessage, v));
        map.put("moneylimitmessage", (pond, v) -> setStringVal(pond::getMoneyLimitMessage, pond::setMoneyLimitMessage, v));
        map.put("pointlimitmessage", (pond, v) -> setStringVal(pond::getPointLimitMessage, pond::setPointLimitMessage, v));

        return map;
    }

    private static String setStringVal(java.util.function.Supplier<String> getter, java.util.function.Consumer<String> setter, String value) {
        String old = getter.get();
        setter.accept(value);
        return old;
    }

    private static String setBoolVal(java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter, String value) {
        String old = String.valueOf(getter.get());
        setter.accept(Boolean.parseBoolean(value));
        return old;
    }

    private static String setLongVal(java.util.function.Supplier<Long> getter, java.util.function.Consumer<Long> setter, String value, long min, String errorMsg) {
        long old = getter.get();
        long parsed = Long.parseLong(value);
        if (parsed < min) throw new IllegalArgumentException(errorMsg);
        setter.accept(parsed);
        return String.valueOf(old);
    }

    private static String setDoubleVal(java.util.function.Supplier<Double> getter, java.util.function.Consumer<Double> setter, String value, double min, String errorMsg) {
        double old = getter.get();
        double parsed = Double.parseDouble(value);
        if (parsed < min) throw new IllegalArgumentException(errorMsg);
        setter.accept(parsed);
        return String.valueOf(old);
    }

    private static String setRewardModeVal(java.util.function.Supplier<String> getter, java.util.function.Consumer<String> setter, String value, String type) {
        if (!value.equalsIgnoreCase("random") && !value.equalsIgnoreCase("fixed")) {
            throw new IllegalArgumentException(type + "奖励模式必须是 random 或 fixed！");
        }
        String old = getter.get();
        setter.accept(value);
        return old;
    }

    @Override public String getName() { return "set"; }
    @Override public String getDescription() { return "设置AFK池属性"; }
    @Override public String getUsage() { return "set <池ID> <属性> <值>"; }
    @Override public String getPermission() { return "yiyunafkpond.admin.edit"; }
    @Override public boolean isPlayerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD使用方法: /yap set <池ID> <属性> <值>");
            plugin.sendPlayerMessage(sender, HELP_HEADER);
            HELP_LINES.forEach(line -> plugin.sendPlayerMessage(sender, line));
            return true;
        }

        String poolId = args[1].toLowerCase();
        String property = args[2].toLowerCase();

        StringBuilder valueBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (i > 3) valueBuilder.append(" ");
            valueBuilder.append(args[i]);
        }
        String value = valueBuilder.toString();

        if (poolId.isEmpty() || poolId.contains("../") || poolId.contains("./")) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD无效的池ID！");
            return true;
        }

        Pond pond = plugin.getPondManager().getPond(poolId);
        if (pond == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD找不到ID为 &#87CEEB" + poolId + " &#6CA6CD的池！");
            return true;
        }

        PropertySetter setter = propertySetters.get(property);
        if (setter == null) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD未知属性: &#87CEEB" + property);
            plugin.sendPlayerMessage(sender, HELP_HEADER);
            HELP_LINES.forEach(line -> plugin.sendPlayerMessage(sender, line));
            return true;
        }

        String oldValue;
        try {
            oldValue = setter.apply(pond, value);
        } catch (NumberFormatException e) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD无效的数值: &#87CEEB" + value);
            return true;
        } catch (IllegalArgumentException e) {
            plugin.sendPlayerMessage(sender, "&#6CA6CD" + e.getMessage());
            return true;
        }

        plugin.getPondManager().savePonds(true);
        plugin.sendPlayerMessage(sender, "&#87CEEB成功设置池 &#B0E0E6" + pond.getName() + " &#87CEEB的属性 &#B0E0E6" + property + "&#87CEEB: &#6CA6CD" + oldValue + " &#ADD8E6→ &#87CEEB" + value);

        if (RESTART_PROPERTIES.contains(property)) {
            plugin.getRewardManager().startPoolRewardTasks(pond);
        }

        return true;
    }
}
