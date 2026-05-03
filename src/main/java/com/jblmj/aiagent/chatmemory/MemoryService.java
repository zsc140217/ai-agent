package com.jblmj.aiagent.chatmemory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 三层记忆系统统一门面
 *
 * 面试要点：
 * 1. 为什么需要三层记忆？
 *    - 短期记忆：原始对话历史，用于上下文理解
 *    - 工作记忆：结构化信息提取，用于任务追踪
 *    - 长期记忆：用户偏好学习，用于个性化推荐
 *
 * 2. 三层记忆的协同工作流程：
 *    用户输入 → 短期记忆存储原始对话
 *            → 工作记忆提取实体和意图
 *            → 长期记忆学习用户偏好
 *            → 生成增强prompt（包含上下文摘要+个性化提示）
 *
 * 3. 性能优化：
 *    - 短期记忆：文件持久化，滑动窗口防止token超限
 *    - 工作记忆：内存存储，30分钟TTL自动清理
 *    - 长期记忆：JSON文件，异步更新不阻塞主流程
 */
@Service
@Slf4j
public class MemoryService {

    @Autowired
    private ChatMemory enhancedChatMemory; // Layer 1: 短期记忆

    @Autowired
    private WorkingMemoryManager workingMemoryManager; // Layer 2: 工作记忆

    @Autowired
    private LongTermMemoryManager longTermMemoryManager; // Layer 3: 长期记忆

    /**
     * 处理用户消息（更新三层记忆）
     *
     * @param userId 用户ID（用于长期记忆）
     * @param conversationId 会话ID（用于短期记忆和工作记忆）
     * @param userMessage 用户消息
     */
    public void processUserMessage(String userId, String conversationId, String userMessage) {
        // Layer 2: 从消息中提取实体和意图，更新工作记忆
        workingMemoryManager.extractAndUpdate(conversationId, userMessage);

        log.debug("Processed user message for userId={}, conversationId={}", userId, conversationId);
    }

    /**
     * 生成增强的系统提示（包含上下文信息）
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param currentCity 当前讨论的城市（可选）
     * @return 增强的系统提示
     */
    public String buildEnhancedPrompt(String userId, String conversationId, String currentCity) {
        StringBuilder enhancedPrompt = new StringBuilder();

        // 1. 添加工作记忆的上下文摘要
        String contextSummary = workingMemoryManager.getContextSummary(conversationId);
        if (!contextSummary.isEmpty()) {
            enhancedPrompt.append(contextSummary).append("\n");
        }

        // 2. 添加长期记忆的个性化提示
        if (currentCity != null && !currentCity.isEmpty()) {
            String personalizedHint = longTermMemoryManager.getPersonalizedHint(userId, currentCity);
            if (!personalizedHint.isEmpty()) {
                enhancedPrompt.append("【个性化提示】\n").append(personalizedHint).append("\n");
            }
        }

        return enhancedPrompt.toString();
    }

    /**
     * 会话结束时的学习流程
     * 从工作记忆中提取信息，更新长期记忆
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     */
    public void learnFromConversation(String userId, String conversationId) {
        WorkingMemory workingMemory = workingMemoryManager.getOrCreate(conversationId);
        longTermMemoryManager.learnFromConversation(userId, conversationId, workingMemory);
        log.info("Learned from conversation: userId={}, conversationId={}", userId, conversationId);
    }

    /**
     * 清空指定会话的记忆
     *
     * @param conversationId 会话ID
     */
    public void clearConversation(String conversationId) {
        // 清空短期记忆
        enhancedChatMemory.clear(conversationId);

        // 清空工作记忆
        workingMemoryManager.clear(conversationId);

        log.info("Cleared all memory for conversationId={}", conversationId);
    }

    /**
     * 删除用户的所有数据（GDPR合规）
     *
     * @param userId 用户ID
     */
    public void deleteUserData(String userId) {
        longTermMemoryManager.deleteUserData(userId);
        log.info("Deleted all data for userId={}", userId);
    }

    /**
     * 获取工作记忆的上下文摘要（用于增强prompt）
     *
     * @param conversationId 会话ID
     * @return 上下文摘要字符串
     */
    public String getContextSummary(String conversationId) {
        return workingMemoryManager.getContextSummary(conversationId);
    }

    /**
     * 获取工作记忆（用于调试和监控）
     */
    public WorkingMemory getWorkingMemory(String conversationId) {
        return workingMemoryManager.getOrCreate(conversationId);
    }

    /**
     * 获取用户画像（用于调试和监控）
     */
    public LongTermMemoryManager.UserProfile getUserProfile(String userId) {
        return longTermMemoryManager.getUserProfile(userId);
    }

    /**
     * 定期清理过期会话（建议通过定时任务调用）
     */
    public void cleanupExpiredSessions() {
        workingMemoryManager.cleanupExpiredSessions();
        log.info("Cleaned up expired sessions");
    }

    /**
     * 获取系统统计信息
     */
    public MemoryStats getStats() {
        MemoryStats stats = new MemoryStats();
        stats.setActiveSessionCount(workingMemoryManager.getActiveSessionCount());
        return stats;
    }

    /**
     * 记忆系统统计信息
     */
    public static class MemoryStats {
        private int activeSessionCount;

        public int getActiveSessionCount() {
            return activeSessionCount;
        }

        public void setActiveSessionCount(int activeSessionCount) {
            this.activeSessionCount = activeSessionCount;
        }
    }
}
