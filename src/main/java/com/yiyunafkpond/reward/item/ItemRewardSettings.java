package com.yiyunafkpond.reward.item;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ItemRewardSettings {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_ROLLS_PER_CYCLE = 100;

    public enum OverflowPolicy {
        SKIP,
        DROP;

        public static OverflowPolicy parse(String value) {
            if (value == null) return SKIP;
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return SKIP;
            }
        }
    }

    private boolean enabled;
    private long intervalSeconds = 60L;
    private int rolls = 1;
    private double chance = 100.0;
    private int maxSuccessfulRollsDaily;
    private OverflowPolicy overflowPolicy = OverflowPolicy.SKIP;
    private final Map<String, ItemRewardEntry> entries = new LinkedHashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getIntervalSeconds() { return intervalSeconds; }
    public int getRolls() { return rolls; }
    public double getChance() { return chance; }
    public int getMaxSuccessfulRollsDaily() { return maxSuccessfulRollsDaily; }
    public OverflowPolicy getOverflowPolicy() { return overflowPolicy; }

    public void setIntervalSeconds(long intervalSeconds) {
        if (intervalSeconds <= 0L) throw new IllegalArgumentException("interval must be greater than zero");
        this.intervalSeconds = intervalSeconds;
    }

    public void setRolls(int rolls) {
        if (rolls < 1 || rolls > MAX_ROLLS_PER_CYCLE) {
            throw new IllegalArgumentException("rolls must be between 1 and " + MAX_ROLLS_PER_CYCLE);
        }
        this.rolls = rolls;
    }

    public void setChance(double chance) {
        if (!Double.isFinite(chance) || chance < 0.0 || chance > 100.0) {
            throw new IllegalArgumentException("chance must be between 0 and 100");
        }
        this.chance = chance;
    }

    public void setMaxSuccessfulRollsDaily(int maxSuccessfulRollsDaily) {
        if (maxSuccessfulRollsDaily < 0) throw new IllegalArgumentException("max daily rolls cannot be negative");
        this.maxSuccessfulRollsDaily = maxSuccessfulRollsDaily;
    }

    public void setOverflowPolicy(OverflowPolicy overflowPolicy) {
        this.overflowPolicy = overflowPolicy == null ? OverflowPolicy.SKIP : overflowPolicy;
    }

    public synchronized void putEntry(ItemRewardEntry entry) {
        entries.put(entry.getId().toLowerCase(Locale.ROOT), entry);
    }

    public synchronized ItemRewardEntry getEntry(String id) {
        return id == null ? null : entries.get(id.toLowerCase(Locale.ROOT));
    }

    public synchronized ItemRewardEntry removeEntry(String id) {
        return id == null ? null : entries.remove(id.toLowerCase(Locale.ROOT));
    }

    public synchronized Collection<ItemRewardEntry> getEntries() {
        return List.copyOf(entries.values());
    }

    public synchronized boolean hasEnabledEntries() {
        return entries.values().stream().anyMatch(entry -> entry.isEnabled() && entry.getWeight() > 0.0);
    }
}
