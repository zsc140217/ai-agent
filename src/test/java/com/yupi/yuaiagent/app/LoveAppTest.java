package com.yupi.yuaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class EnterpriseAssistantAppTest {

    @Resource
    private EnterpriseAssistantApp enterpriseAssistantApp;

    /**
     * 测试基础多轮对话与记忆功能
     */
    @Test
    void testChatWithMemory() {
        String chatId = UUID.randomUUID().toString();

        // 第一轮：自我介绍
        String message = "你好，我是销售部的王小二，我下周要去杭州出差拜访张总。";
        String answer = enterpriseAssistantApp.doChat(message, chatId);
        System.out.println("AI 回复 1: " + answer);

        // 第二轮：测试多轮记忆，不重复说明目的地和身份
        message = "帮我回忆一下，我下周要去哪里？见谁？";
        answer = enterpriseAssistantApp.doChat(message, chatId);
        System.out.println("AI 回复 2: " + answer);

        Assertions.assertTrue(answer.contains("杭州") && answer.contains("张总"), "记忆功能校验失败");
    }

    /**
     * 测试结构化行程简报生成 (TripPlan)
     */
    @Test
    void testDoChatWithTripPlan() {
        String chatId = UUID.randomUUID().toString();
        String message = "我要去北京参加为期三天的技术研讨会，预算大概在 3000 元左右，帮我列个简单的行程建议。";

        EnterpriseAssistantApp.TripPlan plan = enterpriseAssistantApp.doChatWithTripPlan(message, chatId);

        System.out.println("生成的行程方案: " + plan);
        Assertions.assertNotNull(plan);
        Assertions.assertNotNull(plan.destination());
        Assertions.assertNotNull(plan.hotelRecommendation());
    }

    /**
     * 测试 RAG 企业知识库查询
     * 场景：查询杭州的出差补贴和客户张总的地址
     */
    @Test
    void testDoChatWithCorporateKnowledge() {
        String chatId = UUID.randomUUID().toString();

        // 假设知识库 MD 文件里有杭州的补贴标准
        String message = "杭州的出差标准是多少？顺便帮我查查客户张总的公司地址。";
        String answer = enterpriseAssistantApp.doChatWithCorporateKnowledge(message, chatId);

        System.out.println("知识库查询结果: " + answer);
        Assertions.assertNotNull(answer);
    }

    /**
     * 测试综合调度：RAG 查制度 + MCP 查地图
     * 场景：查询客户周边的协议酒店及路况
     */
    @Test
    void testDoComprehensiveChat() {
        String chatId = UUID.randomUUID().toString();

        // 此用例会同时触发 RAG 检索客户地址，并调用 MCP 高德地图工具
        String message = "帮我查查杭州客户张总的公司地址，并在 5 公里内推荐一家公司协议酒店，看看现在过去堵不堵车。";

        String answer = enterpriseAssistantApp.doComprehensiveChat(message, chatId);

        System.out.println("综合调度结果: " + answer);
        Assertions.assertNotNull(answer);
    }

    /**
     * 测试通用工具调用（联网搜索、生成 PDF 等）
     */
    @Test
    void testDoChatWithTools() {
        String chatId = UUID.randomUUID().toString();

        // 1. 测试联网搜索：查询杭州最近的天气
        String msg1 = "帮我搜一下下周杭州的天气怎么样，适合带什么衣服？";
        String ans1 = enterpriseAssistantApp.doChat(msg1, chatId); // 确保注入了 searchTool
        System.out.println("工具调用-搜索: " + ans1);

        // 2. 测试 PDF 生成：生成报销须知
        String msg2 = "根据刚才聊的杭州出差计划，帮我生成一份出差申请书 PDF。";
        // 这里需要确保你已经在 allTools 中注册了对应的 PDF 生成工具
        String ans2 = enterpriseAssistantApp.doChat(msg2, chatId);
        System.out.println("工具调用-PDF: " + ans2);

        Assertions.assertNotNull(ans1);
    }
}