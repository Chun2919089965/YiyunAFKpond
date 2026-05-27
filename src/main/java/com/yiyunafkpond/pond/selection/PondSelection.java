package com.yiyunafkpond.pond.selection;

import org.bukkit.Location;

public class PondSelection {
    private Location firstPoint;
    private Location secondPoint;
    
    public PondSelection() {
        this.firstPoint = null;
        this.secondPoint = null;
    }
    
    /**
     * 获取第一个点
     * @return 第一个点的位置
     */
    public Location getFirstPoint() {
        return firstPoint;
    }
    
    /**
     * 设置第一个点
     * @param firstPoint 第一个点的位置
     */
    public void setFirstPoint(Location firstPoint) {
        this.firstPoint = firstPoint;
    }
    
    /**
     * 获取第二个点
     * @return 第二个点的位置
     */
    public Location getSecondPoint() {
        return secondPoint;
    }
    
    /**
     * 设置第二个点
     * @param secondPoint 第二个点的位置
     */
    public void setSecondPoint(Location secondPoint) {
        this.secondPoint = secondPoint;
    }
    
    /**
     * 检查选区是否完整
     * @return 如果两个点都已设置且在同一世界，则返回true
     */
    public boolean isComplete() {
        if (firstPoint == null || secondPoint == null) {
            return false;
        }
        
        // 检查是否在同一世界
        return firstPoint.getWorld().equals(secondPoint.getWorld());
    }
    
    /**
     * 获取选区大小
     * @return 选区的方块数量
     */
    public int getSize() {
        if (!isComplete()) {
            return 0;
        }
        
        int minX = Math.min(firstPoint.getBlockX(), secondPoint.getBlockX());
        int maxX = Math.max(firstPoint.getBlockX(), secondPoint.getBlockX());
        int minY = Math.min(firstPoint.getBlockY(), secondPoint.getBlockY());
        int maxY = Math.max(firstPoint.getBlockY(), secondPoint.getBlockY());
        int minZ = Math.min(firstPoint.getBlockZ(), secondPoint.getBlockZ());
        int maxZ = Math.max(firstPoint.getBlockZ(), secondPoint.getBlockZ());
        
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
}
