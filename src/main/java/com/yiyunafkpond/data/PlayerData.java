package com.yiyunafkpond.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {
    private final UUID uuid;
    private String name;
    private long totalAfkTime;
    private long totalXpGained;
    private double totalMoneyGained;
    private int totalPointGained;
    private final Map<String, Long> pondAfkTimes;
    private long lastRewardTime;
    private boolean isAfk;
    private String currentPondId;

    private long todayExp;
    private double todayMoney;
    private int todayPoint;
    private final Map<String, Long> pondTodayExp;
    private final Map<String, Double> pondTodayMoney;
    private final Map<String, Integer> pondTodayPoint;
    private final Map<String, Long> pondTodayAfkTime;
    private Date lastReset;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.totalAfkTime = 0;
        this.totalXpGained = 0;
        this.totalMoneyGained = 0.0;
        this.totalPointGained = 0;
        this.pondAfkTimes = new ConcurrentHashMap<>();
        this.lastRewardTime = System.currentTimeMillis();
        this.isAfk = false;
        this.currentPondId = null;

        this.todayExp = 0;
        this.todayMoney = 0.0;
        this.todayPoint = 0;
        this.pondTodayExp = new ConcurrentHashMap<>();
        this.pondTodayMoney = new ConcurrentHashMap<>();
        this.pondTodayPoint = new ConcurrentHashMap<>();
        this.pondTodayAfkTime = new ConcurrentHashMap<>();
        this.lastReset = new Date();
    }

    public UUID getUuid() { return uuid; }
    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

    public synchronized long getTotalAfkTime() { return totalAfkTime; }
    public synchronized void setTotalAfkTime(long totalAfkTime) { this.totalAfkTime = totalAfkTime; }
    public synchronized void addAfkTime(long time) { if (time > 0) this.totalAfkTime += time; }

    public synchronized long getTotalXpGained() { return totalXpGained; }
    public synchronized void setTotalXpGained(long totalXpGained) { this.totalXpGained = totalXpGained; }
    public synchronized void addXpGained(long xp) { this.totalXpGained += xp; }

    public synchronized double getTotalMoneyGained() { return totalMoneyGained; }
    public synchronized void setTotalMoneyGained(double totalMoneyGained) { this.totalMoneyGained = totalMoneyGained; }
    public synchronized void addMoneyGained(double money) { this.totalMoneyGained = roundMoney(this.totalMoneyGained + money); }

    public synchronized int getTotalPointGained() { return totalPointGained; }
    public synchronized void setTotalPointGained(int totalPointGained) { this.totalPointGained = totalPointGained; }
    public synchronized void addPointGained(int point) { this.totalPointGained += point; }

    public Map<String, Long> getPondAfkTimes() { return Map.copyOf(pondAfkTimes); }
    public synchronized void setPondAfkTimes(Map<String, Long> pondAfkTimes) {
        this.pondAfkTimes.clear();
        if (pondAfkTimes != null) this.pondAfkTimes.putAll(pondAfkTimes);
    }
    public synchronized void addPondAfkTime(String pondId, long time) {
        this.pondAfkTimes.merge(pondId, time, Long::sum);
        this.pondTodayAfkTime.merge(pondId, time, Long::sum);
    }
    public synchronized void setPondAfkTime(String pondId, long time) { this.pondAfkTimes.put(pondId, time); }
    public synchronized void removePondAfkTime(String pondId) {
        this.pondAfkTimes.remove(pondId);
        this.pondTodayAfkTime.remove(pondId);
    }
    public synchronized void clearPondAfkTimes() { this.pondAfkTimes.clear(); this.pondTodayAfkTime.clear(); }

    public long getDailyAfkTimeByPool(String pondId) { return pondTodayAfkTime.getOrDefault(pondId, 0L); }
    public Map<String, Long> getPoolTodayAfkTime() { return Map.copyOf(pondTodayAfkTime); }
    public synchronized void setPoolTodayAfkTime(String pondId, long time) { this.pondTodayAfkTime.put(pondId, time); }
    public synchronized void removePoolTodayAfkTime(String pondId) { this.pondTodayAfkTime.remove(pondId); }
    public synchronized void clearPoolTodayAfkTime() { this.pondTodayAfkTime.clear(); }

    public synchronized long getLastRewardTime() { return lastRewardTime; }
    public synchronized void setLastRewardTime(long lastRewardTime) { this.lastRewardTime = lastRewardTime; }

    public synchronized boolean isAfk() { return isAfk; }
    public synchronized void setAfk(boolean afk) { isAfk = afk; }

    public synchronized String getCurrentPondId() { return currentPondId; }
    public synchronized void setCurrentPondId(String currentPondId) { this.currentPondId = currentPondId; }

    public synchronized long getTodayExp() { return todayExp; }
    public synchronized void setTodayExp(long todayExp) { this.todayExp = todayExp; }

    public synchronized double getTodayMoney() { return todayMoney; }
    public synchronized void setTodayMoney(double todayMoney) { this.todayMoney = todayMoney; }

    public synchronized int getTodayPoint() { return todayPoint; }
    public synchronized void setTodayPoint(int todayPoint) { this.todayPoint = todayPoint; }

    public synchronized void addTodayExp(String pondId, long exp) {
        this.todayExp += exp;
        this.pondTodayExp.merge(pondId, exp, Long::sum);
    }

    public synchronized void addTodayMoney(String pondId, double money) {
        this.todayMoney = roundMoney(this.todayMoney + money);
        this.pondTodayMoney.merge(pondId, money, (a, b) -> roundMoney(a + b));
    }

    public synchronized void addTodayPoint(String pondId, int point) {
        this.todayPoint += point;
        this.pondTodayPoint.merge(pondId, point, Integer::sum);
    }

    public long getDailyExpByPool(String pondId) { return pondTodayExp.getOrDefault(pondId, 0L); }
    public double getDailyMoneyByPool(String pondId) { return pondTodayMoney.getOrDefault(pondId, 0.0); }
    public int getDailyPointByPool(String pondId) { return pondTodayPoint.getOrDefault(pondId, 0); }
    public Map<String, Long> getPoolTodayExp() { return Map.copyOf(pondTodayExp); }
    public Map<String, Double> getPoolTodayMoney() { return Map.copyOf(pondTodayMoney); }
    public Map<String, Integer> getPoolTodayPoint() { return Map.copyOf(pondTodayPoint); }

    public synchronized void setPoolTodayExp(String pondId, long exp) { this.pondTodayExp.put(pondId, exp); }
    public synchronized void setPoolTodayMoney(String pondId, double money) { this.pondTodayMoney.put(pondId, money); }
    public synchronized void setPoolTodayPoint(String pondId, int point) { this.pondTodayPoint.put(pondId, point); }

    public synchronized void removePoolTodayExp(String pondId) { this.pondTodayExp.remove(pondId); }
    public synchronized void removePoolTodayMoney(String pondId) { this.pondTodayMoney.remove(pondId); }
    public synchronized void removePoolTodayPoint(String pondId) { this.pondTodayPoint.remove(pondId); }

    public synchronized void clearPoolTodayExp() { this.pondTodayExp.clear(); }
    public synchronized void clearPoolTodayMoney() { this.pondTodayMoney.clear(); }
    public synchronized void clearPoolTodayPoint() { this.pondTodayPoint.clear(); }

    public synchronized void setPoolTodayExp(Map<String, Long> map) { this.pondTodayExp.clear(); if (map != null) this.pondTodayExp.putAll(map); }
    public synchronized void setPoolTodayMoney(Map<String, Double> map) { this.pondTodayMoney.clear(); if (map != null) this.pondTodayMoney.putAll(map); }
    public synchronized void setPoolTodayPoint(Map<String, Integer> map) { this.pondTodayPoint.clear(); if (map != null) this.pondTodayPoint.putAll(map); }

    public synchronized Date getLastReset() { return lastReset; }
    public synchronized void setLastReset(Date lastReset) { this.lastReset = lastReset; }

    public synchronized void checkAndResetDailyData() {
        LocalDate lastResetDate = lastReset.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (!LocalDate.now().equals(lastResetDate)) {
            resetDailyData();
        }
    }

    public synchronized void resetDailyData() {
        this.todayExp = 0;
        this.todayMoney = 0.0;
        this.todayPoint = 0;
        this.pondTodayExp.clear();
        this.pondTodayMoney.clear();
        this.pondTodayPoint.clear();
        this.pondTodayAfkTime.clear();
        this.lastReset = new Date();
    }

    private static double roundMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
