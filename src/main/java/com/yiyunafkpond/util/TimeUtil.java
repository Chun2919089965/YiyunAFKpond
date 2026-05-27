package com.yiyunafkpond.util;

import com.yiyunafkpond.constants.Constants;

public final class TimeUtil {

    private TimeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String formatDuration(long milliseconds) {
        if (milliseconds <= 0) return "0秒";
        long seconds = milliseconds / Constants.MILLISECONDS_PER_SECOND;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天");
        if (hours > 0) sb.append(hours).append("小时");
        if (minutes > 0) sb.append(minutes).append("分");
        if (secs > 0) sb.append(secs).append("秒");
        return sb.toString();
    }
}
