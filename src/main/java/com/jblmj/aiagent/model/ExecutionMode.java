package com.jblmj.aiagent.model;

/**
 * 执行模式枚举
 *
 * 用户可以主动选择不同的执行模式：
 * - DEFAULT: 默认模式，使用复杂度评估 + 并行执行（快速）
 * - THINKING: 思考模式，使用 ReAct 循环（完整轨迹，但较慢）
 */
public enum ExecutionMode {

    /**
     * 默认模式（快速）
     * - 使用复杂度评估系统
     * - 预先分解任务
     * - 并行执行
     * - 响应时间：5-10秒
     * - 适合：日常使用、快速查询
     */
    DEFAULT("默认模式", "快速响应，适合日常使用"),

    /**
     * 思考模式（详细）
     * - 使用 ReAct 循环
     * - 完整的 Thought → Action → Observation → Reflection
     * - 串行执行，但有完整轨迹
     * - 响应时间：15-30秒
     * - 适合：复杂任务、需要详细过程
     */
    THINKING("思考模式", "完整推理过程，适合复杂任务");

    private final String displayName;
    private final String description;

    ExecutionMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 从字符串解析执行模式
     */
    public static ExecutionMode fromString(String mode) {
        if (mode == null) {
            return DEFAULT;
        }

        return switch (mode.toLowerCase()) {
            case "thinking", "think", "思考", "详细" -> THINKING;
            case "default", "normal", "默认", "快速" -> DEFAULT;
            default -> DEFAULT;
        };
    }
}
