package com.jblmj.aiagent.chatmemory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 对话记忆配置
 * 面试要点：
 * 1. 为什么用文件而不是数据库？实习项目快速验证，避免引入MySQL依赖
 * 2. 为什么用Kryo序列化？比JSON快3-5倍，且Spring AI的Message对象复杂
 * 3. 滑动窗口大小如何确定？20轮≈10次对话往返，覆盖一次完整出差规划流程
 */
@Configuration
public class EnhancedChatMemoryConfig {

    @Value("${chat.memory.storage.path:./data/chat-history}")
    private String storagePath;

    @Value("${chat.memory.window.size:20}")
    private int windowSize;

    /**
     * 底层持久化存储（文件）
     */
    @Bean
    public ChatMemory fileBasedChatMemory() {
        return new FileBasedChatMemory(storagePath);
    }

    /**
     * 增强版滑动窗口记忆（主要使用）
     */
    @Bean
    @Primary
    public ChatMemory enhancedChatMemory(ChatMemory fileBasedChatMemory) {
        return new EnhancedMessageWindowChatMemory(fileBasedChatMemory, windowSize);
    }

    /**
     * 滑动窗口配置Bean
     */
    @Bean
    public ChatMemoryWindowConfig chatMemoryWindowConfig() {
        return new ChatMemoryWindowConfig(windowSize);
    }

    public record ChatMemoryWindowConfig(int maxMessages) {}
}
