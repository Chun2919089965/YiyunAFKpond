package com.yiyunafkpond.util;

/**
 * 插件统一消息颜色常量。
 * 避免硬编码颜色码散落各处，方便全局调整主题色。
 */
public final class MessageColor {
    // 主色调（浅蓝渐变系）
    public static final String SKY_BLUE    = "&#87CEEB"; // 天空蓝 - 主要信息
    public static final String POWDER_BLUE = "&#B0E0E6"; // 粉蓝   - 强调/名称
    public static final String LIGHT_BLUE  = "&#ADD8E6"; // 浅蓝   - 次要信息
    public static final String DEEP_BLUE   = "&#6CA6CD"; // 深天蓝 - 错误/禁止

    // 状态
    public static final String SUCCESS = SKY_BLUE;
    public static final String INFO    = POWDER_BLUE;
    public static final String MUTED   = LIGHT_BLUE;
    public static final String ERROR   = DEEP_BLUE;

    private MessageColor() {
        throw new UnsupportedOperationException("Utility class");
    }
}
