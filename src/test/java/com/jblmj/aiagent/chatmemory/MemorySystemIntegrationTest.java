package com.jblmj.aiagent.chatmemory;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 三层记忆系统集成测试
 *
 * 面试演示场景：
 * 1. 用户第一次咨询上海出差
 * 2. 系统记录城市、意图等信息
 * 3. 用户第二次咨询上海出差时，系统能识别"常去城市"并提供个性化服务
 */
@SpringBootTest
@Slf4j
public class MemorySystemIntegrationTest {

    @Autowired
    private MemoryService memoryService;

    @Test
    public void testThreeLayerMemorySystem() {
        String userId = "test_user_001";
        String conversationId = "conv_001";

        // ========== 场景1：用户第一次咨询上海出差 ==========
        log.info("========== 场景1：用户第一次咨询上海出差 ==========");

        // 模拟用户输入
        String message1 = "我要去上海出差，帮我查一下天气";
        memoryService.processUserMessage(userId, conversationId, message1);

        // 验证工作记忆
        WorkingMemory workingMemory = memoryService.getWorkingMemory(conversationId);
        assertTrue(workingMemory.getCities().contains("上海"), "工作记忆应该包含城市'上海'");
        assertEquals("上海", workingMemory.getCurrentDestination(), "当前目的地应该是上海");
        assertEquals("查询天气", workingMemory.getCurrentIntent(), "当前意图应该是查询天气");

        log.info("工作记忆: {}", workingMemory.getContextSummary());

        // ========== 场景2：用户继续询问酒店 ==========
        log.info("========== 场景2：用户继续询问酒店 ==========");

        String message2 = "那边有什么协议酒店推荐吗";
        memoryService.processUserMessage(userId, conversationId, message2);

        workingMemory = memoryService.getWorkingMemory(conversationId);
        assertEquals("查询酒店", workingMemory.getCurrentIntent(), "意图应该更新为查询酒店");
        assertTrue(workingMemory.getIntentHistory().contains("查询天气"), "意图历史应该包含之前的查询天气");
        assertTrue(workingMemory.getIntentHistory().contains("查询酒店"), "意图历史应该包含当前的查询酒店");

        log.info("工作记忆: {}", workingMemory.getContextSummary());

        // ========== 场景3：会话结束，学习用户偏好 ==========
        log.info("========== 场景3：会话结束，学习用户偏好 ==========");

        memoryService.learnFromConversation(userId, conversationId);

        // 验证长期记忆
        LongTermMemoryManager.UserProfile profile = memoryService.getUserProfile(userId);
        assertEquals(1, profile.getCityVisitCount().get("上海"), "上海的访问次数应该是1");
        assertEquals(1, profile.getTripSummaries().size(), "应该有1条行程摘要");

        log.info("用户画像: 常去城市={}", profile.getCityVisitCount());
        log.info("行程摘要数量: {}", profile.getTripSummaries().size());

        // ========== 场景4：用户第二次咨询上海出差（个性化推荐） ==========
        log.info("========== 场景4：用户第二次咨询上海出差（个性化推荐） ==========");

        String conversationId2 = "conv_002";
        String message3 = "我又要去上海了";
        memoryService.processUserMessage(userId, conversationId2, message3);

        // 生成个性化提示
        String enhancedPrompt = memoryService.buildEnhancedPrompt(userId, conversationId2, "上海");
        log.info("增强Prompt: {}", enhancedPrompt);

        assertTrue(enhancedPrompt.contains("上海") || enhancedPrompt.contains("之前"),
                   "增强Prompt应该包含个性化信息");

        // 再次学习
        memoryService.learnFromConversation(userId, conversationId2);

        // 验证访问次数增加
        profile = memoryService.getUserProfile(userId);
        assertEquals(2, profile.getCityVisitCount().get("上海"), "上海的访问次数应该增加到2");

        log.info("更新后的用户画像: 常去城市={}", profile.getCityVisitCount());

        // ========== 场景5：清理测试数据 ==========
        log.info("========== 场景5：清理测试数据 ==========");

        memoryService.clearConversation(conversationId);
        memoryService.clearConversation(conversationId2);
        memoryService.deleteUserData(userId);

        log.info("测试完成，数据已清理");
    }

    @Test
    public void testMultiCityExtraction() {
        String userId = "test_user_002";
        String conversationId = "conv_multi_city";

        // 测试多城市提取
        String message = "我要对比一下上海和杭州的天气，看看去哪个城市出差比较好";
        memoryService.processUserMessage(userId, conversationId, message);

        WorkingMemory workingMemory = memoryService.getWorkingMemory(conversationId);
        assertTrue(workingMemory.getCities().contains("上海"), "应该提取到上海");
        assertTrue(workingMemory.getCities().contains("杭州"), "应该提取到杭州");
        assertEquals("查询天气", workingMemory.getCurrentIntent(), "意图应该是查询天气");

        log.info("多城市提取测试: {}", workingMemory.getContextSummary());

        // 清理
        memoryService.clearConversation(conversationId);
        memoryService.deleteUserData(userId);
    }

    @Test
    public void testIntentTracking() {
        String userId = "test_user_003";
        String conversationId = "conv_intent";

        // 测试意图追踪
        memoryService.processUserMessage(userId, conversationId, "北京天气怎么样");
        memoryService.processUserMessage(userId, conversationId, "那边有什么酒店");
        memoryService.processUserMessage(userId, conversationId, "从公司到客户那里怎么走");

        WorkingMemory workingMemory = memoryService.getWorkingMemory(conversationId);
        assertEquals(3, workingMemory.getIntentHistory().size(), "应该有3个意图");
        assertTrue(workingMemory.getIntentHistory().contains("查询天气"));
        assertTrue(workingMemory.getIntentHistory().contains("查询酒店"));
        assertTrue(workingMemory.getIntentHistory().contains("规划路线"));

        log.info("意图追踪测试: {}", workingMemory.getIntentHistory());

        // 清理
        memoryService.clearConversation(conversationId);
        memoryService.deleteUserData(userId);
    }

    @Test
    public void testSessionCleanup() {
        // 测试会话清理
        int initialCount = memoryService.getStats().getActiveSessionCount();

        // 创建多个会话
        for (int i = 0; i < 5; i++) {
            String conversationId = "conv_cleanup_" + i;
            memoryService.processUserMessage("user_cleanup", conversationId, "测试消息");
        }

        int afterCreateCount = memoryService.getStats().getActiveSessionCount();
        assertTrue(afterCreateCount >= initialCount + 5, "活跃会话数应该增加");

        log.info("创建会话后活跃数: {}", afterCreateCount);

        // 清理过期会话（注意：这些会话不会立即过期，因为刚创建）
        memoryService.cleanupExpiredSessions();

        int afterCleanupCount = memoryService.getStats().getActiveSessionCount();
        log.info("清理后活跃数: {}", afterCleanupCount);

        // 手动清理测试会话
        for (int i = 0; i < 5; i++) {
            String conversationId = "conv_cleanup_" + i;
            memoryService.clearConversation(conversationId);
        }
        memoryService.deleteUserData("user_cleanup");
    }
}
