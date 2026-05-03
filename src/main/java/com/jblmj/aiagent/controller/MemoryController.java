package com.jblmj.aiagent.controller;

import com.jblmj.aiagent.chatmemory.LongTermMemoryManager;
import com.jblmj.aiagent.chatmemory.MemoryService;
import com.jblmj.aiagent.chatmemory.WorkingMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 记忆系统管理接口
 *
 * 面试演示要点：
 * 1. 展示三层记忆的查询接口
 * 2. 演示用户画像的积累过程
 * 3. 展示GDPR合规的数据删除功能
 */
@RestController
@RequestMapping("/api/memory")
@Slf4j
public class MemoryController {

    @Autowired
    private MemoryService memoryService;

    /**
     * 获取工作记忆（当前会话的上下文）
     */
    @GetMapping("/working/{conversationId}")
    public WorkingMemory getWorkingMemory(@PathVariable String conversationId) {
        return memoryService.getWorkingMemory(conversationId);
    }

    /**
     * 获取用户画像（长期记忆）
     */
    @GetMapping("/profile/{userId}")
    public LongTermMemoryManager.UserProfile getUserProfile(@PathVariable String userId) {
        return memoryService.getUserProfile(userId);
    }

    /**
     * 清空会话记忆
     */
    @DeleteMapping("/conversation/{conversationId}")
    public String clearConversation(@PathVariable String conversationId) {
        memoryService.clearConversation(conversationId);
        return "Conversation memory cleared for: " + conversationId;
    }

    /**
     * 删除用户数据（GDPR合规）
     */
    @DeleteMapping("/user/{userId}")
    public String deleteUserData(@PathVariable String userId) {
        memoryService.deleteUserData(userId);
        return "User data deleted for: " + userId;
    }

    /**
     * 手动触发学习（从工作记忆更新长期记忆）
     */
    @PostMapping("/learn")
    public String learnFromConversation(
            @RequestParam String userId,
            @RequestParam String conversationId) {
        memoryService.learnFromConversation(userId, conversationId);
        return "Learning completed for userId=" + userId + ", conversationId=" + conversationId;
    }

    /**
     * 获取记忆系统统计信息
     */
    @GetMapping("/stats")
    public MemoryService.MemoryStats getStats() {
        return memoryService.getStats();
    }

    /**
     * 清理过期会话
     */
    @PostMapping("/cleanup")
    public String cleanupExpiredSessions() {
        memoryService.cleanupExpiredSessions();
        return "Expired sessions cleaned up";
    }
}
