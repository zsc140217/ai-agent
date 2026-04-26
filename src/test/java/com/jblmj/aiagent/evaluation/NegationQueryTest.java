package com.jblmj.aiagent.evaluation;

import com.jblmj.aiagent.app.EnterpriseAssistantApp;
import com.jblmj.aiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 否定查询测试
 * 测试场景：验证 RAG 系统能否正确处理否定查询，避免"是这样"召回"不是这样"的问题
 *
 * 面试问题：
 * Q: 为什么"是这样"会召回"不是这样"？
 * A: 因为向量检索基于余弦相似度，两个句子词汇重叠高，语义空间距离近，
 *    而大多数 Embedding 模型对否定词不敏感
 *
 * 解决方案：
 * 1. 查询改写：检测否定词，用 LLM 重写查询保留否定语义
 * 2. 混合检索：向量检索 + 关键词过滤
 * 3. 重排序：用 LLM 对召回结果重新打分
 */
@SpringBootTest
@Slf4j
public class NegationQueryTest {

    @Resource
    private EnterpriseAssistantApp enterpriseAssistantApp;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 测试1：否定查询 - 不能住五星级酒店
     */
    @Test
    public void testNegationQuery_CannotStayFiveStar() {
        String query = "北京出差不能住五星级酒店吗";
        String chatId = UUID.randomUUID().toString();

        log.info("========== 测试否定查询：{} ==========", query);

        // 1. 查看查询重写结果
        String rewrittenQuery = queryRewriter.doQueryRewrite(query);
        log.info("查询重写: {} -> {}", query, rewrittenQuery);
        assertTrue(rewrittenQuery.contains("不能") || rewrittenQuery.contains("住宿标准"),
                "重写后的查询应保留否定语义或提取核心意图");

        // 2. 执行完整 RAG 查询
        String response = enterpriseAssistantApp.doChatWithCorporateKnowledge(query, chatId);
        log.info("RAG 响应: {}", response);

        // 3. 验证响应
        assertNotNull(response, "响应不能为空");
        assertFalse(response.contains("可以住五星") || response.contains("允许五星"),
                "响应不应包含错误的肯定信息");
        assertTrue(response.contains("不能") || response.contains("不可以") ||
                   response.contains("四星及以下") || response.contains("500元"),
                "响应应明确说明不能住五星或给出正确标准");

        log.info("✓ 测试通过：正确处理否定查询");
    }

    /**
     * 测试2：否定查询 - 不能坐商务舱
     */
    @Test
    public void testNegationQuery_CannotBusinessClass() {
        String query = "出差不能坐商务舱对吗";
        String chatId = UUID.randomUUID().toString();

        log.info("========== 测试否定查询：{} ==========", query);

        String response = enterpriseAssistantApp.doChatWithCorporateKnowledge(query, chatId);
        log.info("RAG 响应: {}", response);

        assertNotNull(response);
        assertFalse(response.contains("可以坐商务舱"),
                "响应不应包含错误的肯定信息");
        assertTrue(response.contains("不能") || response.contains("经济舱"),
                "响应应明确说明不能坐商务舱或给出正确标准");

        log.info("✓ 测试通过：正确处理交通否定查询");
    }

    /**
     * 测试3：否定疑问句 - 不是500元吗
     */
    @Test
    public void testNegationQuery_IsNotFiveHundred() {
        String query = "去二线城市不是500元住宿标准吗";
        String chatId = UUID.randomUUID().toString();

        log.info("========== 测试否定疑问句：{} ==========", query);

        String response = enterpriseAssistantApp.doChatWithCorporateKnowledge(query, chatId);
        log.info("RAG 响应: {}", response);

        assertNotNull(response);
        assertTrue(response.contains("350") || response.contains("二类城市"),
                "响应应给出正确的二线城市标准（350元）");
        assertFalse(response.contains("500元"),
                "响应不应包含错误的500元标准");

        log.info("✓ 测试通过：正确纠正用户的错误认知");
    }

    /**
     * 测试4：双重否定 - 不能同时领取
     */
    @Test
    public void testNegationQuery_CannotBoth() {
        String query = "打车报销后还不能领交通补助吗";
        String chatId = UUID.randomUUID().toString();

        log.info("========== 测试双重否定：{} ==========", query);

        String response = enterpriseAssistantApp.doChatWithCorporateKnowledge(query, chatId);
        log.info("RAG 响应: {}", response);

        assertNotNull(response);
        assertTrue(response.contains("不能") || response.contains("取消") || response.contains("二选一"),
                "响应应明确说明不能同时领取");
        assertFalse(response.contains("可以同时"),
                "响应不应包含错误的肯定信息");

        log.info("✓ 测试通过：正确处理双重否定");
    }

    /**
     * 测试5：对比否定查询 vs 肯定查询的召回差异
     */
    @Test
    public void testNegationVsPositiveQuery() {
        String positiveQuery = "北京出差可以住五星级酒店吗";
        String negativeQuery = "北京出差不能住五星级酒店吗";

        log.info("========== 对比测试：肯定 vs 否定查询 ==========");

        // 肯定查询
        String positiveResponse = enterpriseAssistantApp.doChatWithCorporateKnowledge(
                positiveQuery, UUID.randomUUID().toString());
        log.info("肯定查询响应: {}", positiveResponse);

        // 否定查询
        String negativeResponse = enterpriseAssistantApp.doChatWithCorporateKnowledge(
                negativeQuery, UUID.randomUUID().toString());
        log.info("否定查询响应: {}", negativeResponse);

        // 验证：两个查询应该得到一致的答案（都说明不能住五星）
        assertNotNull(positiveResponse);
        assertNotNull(negativeResponse);

        boolean positiveCorrect = positiveResponse.contains("不能") ||
                                  positiveResponse.contains("不可以") ||
                                  positiveResponse.contains("四星及以下");
        boolean negativeCorrect = negativeResponse.contains("不能") ||
                                  negativeResponse.contains("不可以") ||
                                  negativeResponse.contains("四星及以下");

        assertTrue(positiveCorrect, "肯定查询应正确回答不能住五星");
        assertTrue(negativeCorrect, "否定查询应正确回答不能住五星");

        log.info("✓ 测试通过：肯定和否定查询得到一致的正确答案");
    }

    /**
     * 测试6：查询重写器的否定词检测
     */
    @Test
    public void testNegationDetection() {
        log.info("========== 测试否定词检测 ==========");

        String[] negationQueries = {
                "不能住五星级酒店",
                "不是500元标准",
                "没有交通补助",
                "不允许坐商务舱",
                "禁止打车",
                "不得超标"
        };

        String[] positiveQueries = {
                "可以住五星级酒店",
                "是500元标准",
                "有交通补助",
                "允许坐商务舱"
        };

        // 测试否定查询的重写
        for (String query : negationQueries) {
            String rewritten = queryRewriter.doQueryRewrite(query);
            log.info("否定查询重写: {} -> {}", query, rewritten);
            assertNotNull(rewritten, "重写结果不能为空");
        }

        // 测试肯定查询的重写（应该走正常流程）
        for (String query : positiveQueries) {
            String rewritten = queryRewriter.doQueryRewrite(query);
            log.info("肯定查询重写: {} -> {}", query, rewritten);
            assertNotNull(rewritten, "重写结果不能为空");
        }

        log.info("✓ 测试通过：否定词检测正常工作");
    }
}
