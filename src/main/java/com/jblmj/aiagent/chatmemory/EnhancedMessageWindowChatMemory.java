package com.jblmj.aiagent.chatmemory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 增强版滑动窗口记忆
 * 核心改进：
 * 1. 底层使用FileBasedChatMemory持久化
 * 2. 滑动窗口逻辑：保留最近N条消息
 * 3. 自动清理过期消息（超过窗口大小）
 *
 * 面试要点：
 * - 为什么需要滑动窗口？防止token超限（Qwen-Max上下文128k，但实际对话20轮已足够）
 * - 如何处理窗口边界？保留完整的User-Assistant对（不能只保留User或只保留Assistant）
 * - 性能优化：每次add时检查窗口大小，超出则删除最旧的消息
 */
@Slf4j
public class EnhancedMessageWindowChatMemory implements ChatMemory {

    private final ChatMemory persistentMemory; // 底层持久化存储
    private final int maxMessages; // 滑动窗口大小

    public EnhancedMessageWindowChatMemory(ChatMemory persistentMemory, int maxMessages) {
        this.persistentMemory = persistentMemory;
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(@NonNull String conversationId, @NonNull List<Message> messages) {
        // 1. 获取当前会话的所有历史消息
        List<Message> existingMessages = persistentMemory.get(conversationId);

        // 2. 追加新消息
        existingMessages.addAll(messages);

        // 3. 滑动窗口裁剪：保留最近maxMessages条
        List<Message> windowedMessages = applyWindow(existingMessages);

        // 4. 持久化到文件
        persistentMemory.clear(conversationId); // 先清空
        persistentMemory.add(conversationId, windowedMessages);

        log.debug("ConversationId={}, Total messages={}, Windowed messages={}",
                  conversationId, existingMessages.size(), windowedMessages.size());
    }

    @Override
    @NonNull
    public List<Message> get(@NonNull String conversationId) {
        return persistentMemory.get(conversationId);
    }

    @Override
    public void clear(@NonNull String conversationId) {
        persistentMemory.clear(conversationId);
        log.info("Cleared conversation history for conversationId={}", conversationId);
    }

    /**
     * 滑动窗口逻辑：保留最近N条消息
     * 优化：确保保留完整的User-Assistant对
     */
    private List<Message> applyWindow(List<Message> messages) {
        if (messages.size() <= maxMessages) {
            return messages;
        }

        // 从尾部截取最近的maxMessages条
        int startIndex = messages.size() - maxMessages;
        List<Message> windowed = new ArrayList<>(messages.subList(startIndex, messages.size()));

        // 优化：如果第一条是Assistant消息，删除它（保证从User消息开始）
        if (!windowed.isEmpty() && isAssistantMessage(windowed.get(0))) {
            windowed.remove(0);
            log.debug("Removed orphan assistant message at window boundary");
        }

        return windowed;
    }

    /**
     * 判断是否为Assistant消息
     */
    private boolean isAssistantMessage(Message message) {
        return message.getMessageType().getValue().equalsIgnoreCase("assistant");
    }
}
