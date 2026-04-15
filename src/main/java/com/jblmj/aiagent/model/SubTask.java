package com.jblmj.aiagent.model;

import lombok.Data;

/**
 * 子任务模型
 *
 * @author jblmj
 */
@Data
public class SubTask {
    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务参数（JSON 格式）
     */
    private String parameters;

    /**
     * 执行结果
     */
    private String result;

    /**
     * 是否成功
     */
    private boolean success;
}
