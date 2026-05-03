package com.jblmj.aiagent.chatmemory;

import lombok.Data;
import java.util.*;

/**
 * 工作记忆：当前会话的关键信息提取
 *
 * 面试要点：
 * 1. 为什么需要工作记忆？短期记忆是原始对话，工作记忆是结构化信息
 * 2. 实体识别的作用：避免重复询问用户已提供的信息（如目的地城市）
 * 3. 意图追踪的作用：理解用户的任务流程（查天气 → 订酒店 → 规划路线）
 *
 * 设计思路：
 * - 每个conversationId对应一个WorkingMemory实例
 * - 每次对话后更新实体和意图
 * - 支持上下文补全（用户说"那里的天气"，自动补全为"上海的天气"）
 */
@Data
public class WorkingMemory {

    private String conversationId;

    // 实体信息
    private Set<String> cities = new HashSet<>();           // 提到的城市
    private Set<String> customers = new HashSet<>();        // 提到的客户
    private Set<String> hotels = new HashSet<>();           // 提到的酒店
    private String currentDestination;                      // 当前目的地（最近一次提到的城市）

    // 意图追踪
    private List<String> intentHistory = new ArrayList<>(); // 意图序列
    private String currentIntent;                           // 当前意图

    // 任务状态
    private Map<String, Boolean> taskStatus = new HashMap<>(); // 任务完成状态

    // 时间戳
    private long lastUpdateTime = System.currentTimeMillis();

    public WorkingMemory(String conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * 添加城市实体
     */
    public void addCity(String city) {
        cities.add(city);
        currentDestination = city; // 更新当前目的地
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 添加客户实体
     */
    public void addCustomer(String customer) {
        customers.add(customer);
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 添加酒店实体
     */
    public void addHotel(String hotel) {
        hotels.add(hotel);
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 更新意图
     */
    public void updateIntent(String intent) {
        if (!intent.equals(currentIntent)) {
            intentHistory.add(intent);
            currentIntent = intent;
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    /**
     * 标记任务完成
     */
    public void markTaskCompleted(String taskName) {
        taskStatus.put(taskName, true);
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 检查任务是否完成
     */
    public boolean isTaskCompleted(String taskName) {
        return taskStatus.getOrDefault(taskName, false);
    }

    /**
     * 获取上下文摘要（用于prompt增强）
     */
    public String getContextSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("【当前会话上下文】\n");

        if (currentDestination != null) {
            summary.append("目的地: ").append(currentDestination).append("\n");
        }

        if (!cities.isEmpty()) {
            summary.append("涉及城市: ").append(String.join(", ", cities)).append("\n");
        }

        if (!customers.isEmpty()) {
            summary.append("涉及客户: ").append(String.join(", ", customers)).append("\n");
        }

        if (currentIntent != null) {
            summary.append("当前意图: ").append(currentIntent).append("\n");
        }

        if (!taskStatus.isEmpty()) {
            summary.append("已完成任务: ");
            taskStatus.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .forEach(e -> summary.append(e.getKey()).append(" "));
            summary.append("\n");
        }

        return summary.toString();
    }

    /**
     * 清空工作记忆
     */
    public void clear() {
        cities.clear();
        customers.clear();
        hotels.clear();
        intentHistory.clear();
        taskStatus.clear();
        currentDestination = null;
        currentIntent = null;
        lastUpdateTime = System.currentTimeMillis();
    }
}
