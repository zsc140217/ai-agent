package com.jblmj.aiagent.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 观察结果
 * 包含信息提炼、异常检测、多步推理等能力
 */
@Data
public class ObservationResult {

    /**
     * 原始结果
     */
    private String rawResult;

    /**
     * 观察摘要
     */
    private String summary;

    /**
     * 提炼后的关键信息
     */
    private String keyInfo;

    /**
     * 异常信息
     */
    private String anomalies = "";

    /**
     * 推理过程
     */
    private String reasoning;

    /**
     * 下一步建议
     */
    private String nextStepSuggestion;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    /**
     * 错误类型
     */
    private ErrorType errorType;

    /**
     * 推理结果列表
     */
    private List<String> inferences = new ArrayList<>();

    /**
     * 建议的下一步行动
     */
    private String suggestedNextAction;

    /**
     * 置信度（0-1）
     */
    private double confidence = 1.0;

    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        NONE,           // 无错误
        TIMEOUT,        // 超时
        NETWORK,        // 网络错误
        INVALID_PARAM,  // 参数错误
        NOT_FOUND,      // 资源未找到
        PERMISSION,     // 权限错误
        UNKNOWN         // 未知错误
    }

    /**
     * 添加推理结果
     */
    public void addInference(String inference) {
        this.inferences.add(inference);
    }

    /**
     * 格式化输出
     */
    public String format() {
        StringBuilder sb = new StringBuilder();

        if (summary != null) {
            sb.append(summary);
        }

        if (keyInfo != null && !keyInfo.isEmpty()) {
            sb.append(" | 关键信息: ").append(keyInfo);
        }

        if (anomalies != null && !anomalies.isEmpty()) {
            sb.append(" | 异常: ").append(anomalies);
        }

        if (reasoning != null && !reasoning.isEmpty()) {
            sb.append(" | 推理: ").append(reasoning);
        }

        if (nextStepSuggestion != null && !nextStepSuggestion.isEmpty()) {
            sb.append(" | 建议: ").append(nextStepSuggestion);
        }

        return sb.toString();
    }
}
