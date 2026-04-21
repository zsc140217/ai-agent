package com.jblmj.aiagent.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 子任务模型（支持任务依赖和并行执行）
 *
 * @author jblmj
 */
@Data
public class SubTask {
    /**
     * 任务 ID（用于依赖关系）
     */
    private int id;

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
     * 依赖的任务 ID 列表（必须等这些任务完成后才能执行）
     */
    private List<Integer> dependsOn = new ArrayList<>();

    /**
     * 任务优先级（数字越小优先级越高）
     */
    private int priority = 0;

    /**
     * 执行结果
     */
    private String result;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 是否可以并行执行（没有依赖关系的任务可以并行）
     */
    public boolean canExecuteNow(List<SubTask> completedTasks) {
        if (dependsOn == null || dependsOn.isEmpty()) {
            return true;
        }

        // 检查所有依赖的任务是否都已完成
        for (int depId : dependsOn) {
            boolean depCompleted = completedTasks.stream()
                    .anyMatch(t -> t.getId() == depId && t.isSuccess());
            if (!depCompleted) {
                return false;
            }
        }

        return true;
    }
}
