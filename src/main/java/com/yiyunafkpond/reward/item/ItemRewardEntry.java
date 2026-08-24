package com.yiyunafkpond.reward.item;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.regex.Pattern;

public final class ItemRewardEntry {
    public static final int MAX_AMOUNT_PER_ROLL = 2304;
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    public enum SourceType {
        MINECRAFT,
        CAPTURED
    }

    private final String id;
    private final SourceType sourceType;
    private boolean enabled;
    private double weight;
    private int minAmount;
    private int maxAmount;
    private ItemStack template;

    public ItemRewardEntry(String id, SourceType sourceType, boolean enabled, double weight,
                           int minAmount, int maxAmount, ItemStack template) {
        Objects.requireNonNull(id, "id");
        if (!isValidId(id)) {
            throw new IllegalArgumentException("id must contain only letters, numbers, underscores, or hyphens (1-64 characters)");
        }
        this.id = id;
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
        this.enabled = enabled;
        setWeight(weight);
        setAmountRange(minAmount, maxAmount);
        setTemplate(template);
    }

    public String getId() { return id; }
    public SourceType getSourceType() { return sourceType; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public double getWeight() { return weight; }
    public int getMinAmount() { return minAmount; }
    public int getMaxAmount() { return maxAmount; }

    public static boolean isValidId(String id) {
        return id != null && ID_PATTERN.matcher(id).matches();
    }

    public void setWeight(double weight) {
        if (!Double.isFinite(weight) || weight <= 0.0) {
            throw new IllegalArgumentException("weight must be a positive finite number");
        }
        this.weight = weight;
    }

    public void setAmountRange(int minAmount, int maxAmount) {
        if (minAmount < 1 || maxAmount < minAmount || maxAmount > MAX_AMOUNT_PER_ROLL) {
            throw new IllegalArgumentException("amount range must satisfy 1 <= min <= max <= "
                    + MAX_AMOUNT_PER_ROLL);
        }
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public ItemStack getTemplate() {
        return template.clone();
    }

    public void setTemplate(ItemStack template) {
        if (template == null || template.getType().isAir()) {
            throw new IllegalArgumentException("item template must not be empty");
        }
        this.template = template.clone();
        this.template.setAmount(1);
    }
}
