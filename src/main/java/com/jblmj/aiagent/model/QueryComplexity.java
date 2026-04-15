package com.jblmj.aiagent.model;

/**
 * 查询复杂度枚举
 *
 * @author jblmj
 */
public enum QueryComplexity {
    /**
     * 简单查询：单一意图，单次工具调用
     * 例如："北京天气"
     */
    SIMPLE,

    /**
     * 中等复杂：单一意图，多次工具调用
     * 例如："上海和广州哪个天气更好"
     */
    MEDIUM,

    /**
     * 高度复杂：多意图，需要任务分解
     * 例如："我要去杭州拜访客户，帮我规划行程"
     */
    COMPLEX
}
