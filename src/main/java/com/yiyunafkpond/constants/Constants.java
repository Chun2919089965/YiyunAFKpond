package com.yiyunafkpond.constants;

public class Constants {
    
    public static final long MILLISECONDS_PER_SECOND = 1000L;
    public static final long TICKS_PER_SECOND = 20L;
    
    public static final double MIN_REWARD_AMOUNT = 0.01;
    
    public static final int CLEANUP_INTERVAL_SECONDS = 3600;
    public static final long INACTIVE_THRESHOLD_MILLISECONDS = 86400000L;
    
    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
