package com.jblmj.aiagent.chatmemory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作记忆管理器
 *
 * 面试要点：
 * 1. 为什么用ConcurrentHashMap？支持多用户并发访问
 * 2. 内存泄漏风险：需要定期清理过期会话（TTL机制）
 * 3. 实体提取方式：当前用规则匹配，可升级为NER模型
 *
 * 优化方向：
 * - 接入NER模型（如BERT-NER）提取实体
 - 使用Redis存储，支持分布式部署
 * - 添加TTL机制，自动清理30分钟无活动的会话
 */
@Component
@Slf4j
public class WorkingMemoryManager {

    private final Map<String, WorkingMemory> memoryStore = new ConcurrentHashMap<>();

    // 会话过期时间（30分钟）
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;

    /**
     * 获取或创建工作记忆
     */
    public WorkingMemory getOrCreate(String conversationId) {
        return memoryStore.computeIfAbsent(conversationId, WorkingMemory::new);
    }

    /**
     * 从用户消息中提取实体并更新工作记忆
     * 当前实现：基于规则的简单匹配
     * TODO: 升级为NER模型
     */
    public void extractAndUpdate(String conversationId, String userMessage) {
        WorkingMemory memory = getOrCreate(conversationId);

        // 1. 提取城市实体（简单规则：常见城市名）
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安", "南京", "重庆"};
        for (String city : cities) {
            if (userMessage.contains(city)) {
                memory.addCity(city);
                log.debug("Extracted city: {} from message: {}", city, userMessage);
            }
        }

        // 2. 提取意图（基于关键词）
        if (userMessage.contains("天气") || userMessage.contains("气温")) {
            memory.updateIntent("查询天气");
        } else if (userMessage.contains("酒店") || userMessage.contains("住宿")) {
            memory.updateIntent("查询酒店");
        } else if (userMessage.contains("路线") || userMessage.contains("距离") || userMessage.contains("怎么去")) {
            memory.updateIntent("规划路线");
        } else if (userMessage.contains("报销") || userMessage.contains("补贴") || userMessage.contains("标准")) {
            memory.updateIntent("查询政策");
        } else if (userMessage.contains("客户")) {
            memory.updateIntent("查询客户");
        }

        // 3. 提取客户名（简单规则：包含"公司"或"客户"的词）
        if (userMessage.contains("客户")) {
            // 简化处理：提取"XX公司"或"XX客户"
            String[] tokens = userMessage.split("[，。！？\\s]+");
            for (String token : tokens) {
                if (token.contains("公司") || token.contains("客户")) {
                    memory.addCustomer(token);
                    log.debug("Extracted customer: {} from message: {}", token, userMessage);
                }
            }
        }

        log.info("Updated working memory for conversationId={}: {}",
                 conversationId, memory.getContextSummary());
    }

    /**
     * 获取上下文摘要（用于增强prompt）
     */
    public String getContextSummary(String conversationId) {
        WorkingMemory memory = memoryStore.get(conversationId);
        return memory != null ? memory.getContextSummary() : "";
    }

    /**
     * 清理过期会话
     */
    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        memoryStore.entrySet().removeIf(entry -> {
            boolean expired = (now - entry.getValue().getLastUpdateTime()) > SESSION_TIMEOUT_MS;
            if (expired) {
                log.info("Cleaned up expired session: {}", entry.getKey());
            }
            return expired;
        });
    }

    /**
     * 清空指定会话的工作记忆
     */
    public void clear(String conversationId) {
        WorkingMemory memory = memoryStore.get(conversationId);
        if (memory != null) {
            memory.clear();
            log.info("Cleared working memory for conversationId={}", conversationId);
        }
    }

    /**
     * 获取当前活跃会话数
     */
    public int getActiveSessionCount() {
        return memoryStore.size();
    }
}
