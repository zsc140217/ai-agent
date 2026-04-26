package com.jblmj.aiagent.model;

import lombok.Data;

/**
 * ReAct 执行步骤
 * 记录完整的 Thought → Action → Observation → Reflection 循环
 */
@Data
public class ReActStep {

    /**
     * 步骤编号
     */
    private int stepNumber;

    /**
     * 思考内容：分析当前状态，决定下一步行动
     */
    private String thought;

    /**
     * 执行的动作：具体执行的操作（工具调用、API请求等）
     */
    private String action;

    /**
     * 观察到的结果：执行后的输出和关键信息
     */
    private String observation;

    /**
     * 反思内容：根据观察结果判断是否需要调整策略
     */
    private String reflection;

    /**
     * 结构化观察结果
     */
    private ObservationResult observationResult;

    /**
     * 结构化反思结果
     */
    private ReflectionResult reflectionResult;

    /**
     * 错误信息（如果有）
     */
    private String error;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 执行耗时（毫秒）
     */
    private long duration;

    /**
     * 格式化输出
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Step ").append(stepNumber).append(" ===\n");

        if (thought != null) {
            sb.append("💭 Thought: ").append(thought).append("\n");
        }

        if (action != null) {
            sb.append("🔧 Action: ").append(action).append("\n");
        }

        if (observation != null) {
            sb.append("👁️ Observation: ").append(observation).append("\n");
        }

        // 输出结构化观察结果
        if (observationResult != null) {
            sb.append(observationResult.format()).append("\n");
        }

        if (reflection != null) {
            sb.append("🤔 Reflection: ").append(reflection).append("\n");
        }

        // 输出结构化反思结果
        if (reflectionResult != null) {
            sb.append(reflectionResult.format()).append("\n");
        }

        if (error != null) {
            sb.append("❌ Error: ").append(error).append("\n");
        }

        if (duration > 0) {
            sb.append("⏱️ Duration: ").append(duration).append("ms\n");
        }

        return sb.toString();
    }
}
