package com.jblmj.aiagent.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 反思结果
 * 包含失败分析、自动重试、策略调整等能力
 */
@Data
public class ReflectionResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 策略调整建议
     */
    private String strategyAdjustment;

    /**
     * 是否需要重试
     */
    private boolean shouldRetry;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 备用工具推荐
     */
    private String alternativeTool;

    /**
     * 进度检查结果
     */
    private String progressCheck;

    /**
     * 学到的经验
     */
    private String experienceLearned;

    /**
     * 是否需要调整策略
     */
    private boolean needStrategyAdjustment;

    /**
     * 策略调整建议列表
     */
    private List<String> strategyAdjustments = new ArrayList<>();

    /**
     * 失败原因分析
     */
    private String failureAnalysis;

    /**
     * 是否达到目标
     */
    private boolean goalAchieved;

    /**
     * 目标完成度（0-1）
     */
    private double goalProgress = 0.0;

    /**
     * 下一步行动计划
     */
    private String nextActionPlan;

    /**
     * 是否应该终止
     */
    private boolean shouldTerminate;

    /**
     * 终止原因
     */
    private String terminationReason;

    /**
     * 添加策略调整建议
     */
    public void addStrategyAdjustment(String adjustment) {
        this.strategyAdjustments.add(adjustment);
    }

    /**
     * 判断是否可以重试
     */
    public boolean canRetry() {
        return shouldRetry && retryCount < maxRetries;
    }

    /**
     * 增加重试计数
     */
    public void incrementRetry() {
        this.retryCount++;
    }

    /**
     * 格式化输出
     */
    public String format() {
        StringBuilder sb = new StringBuilder();

        if (success) {
            sb.append("执行成功");
        } else {
            sb.append("执行失败");
            if (failureReason != null) {
                sb.append(" - ").append(failureReason);
            }
        }

        if (shouldRetry) {
            sb.append(" | 重试: ").append(retryCount).append("/").append(maxRetries);
        }

        if (strategyAdjustment != null && !strategyAdjustment.isEmpty()) {
            sb.append(" | 策略: ").append(strategyAdjustment);
        }

        if (alternativeTool != null && !alternativeTool.isEmpty()) {
            sb.append(" | 备用工具: ").append(alternativeTool);
        }

        if (progressCheck != null && !progressCheck.isEmpty()) {
            sb.append(" | 进度: ").append(progressCheck);
        }

        if (experienceLearned != null && !experienceLearned.isEmpty()) {
            sb.append(" | 经验: ").append(experienceLearned);
        }

        return sb.toString();
    }
}
