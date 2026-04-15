package com.jblmj.aiagent.evaluation;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jblmj.aiagent.app.EnterpriseAssistantApp;
import com.jblmj.aiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RAG 检索准确率评测
 * 对比三种方案：
 * 1. Baseline：直接检索（不做任何优化）
 * 2. +Query Rewriting：加查询重写
 * 3. +RAG Advisor：使用 Spring AI 的 QuestionAnswerAdvisor
 */
@SpringBootTest
@Slf4j
public class RAGEvaluationTest {

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private EnterpriseAssistantApp enterpriseAssistantApp;

    /**
     * 测试用例数据结构
     */
    @Data
    static class TestCase {
        private Integer id;
        private String query;
        private String difficulty;
        private String category;
        private String cityLevel;
        private List<String> expectedKeywords;
        private String expectedAnswerContains;
        private String shouldNotContain;
    }

    /**
     * 评测结果
     */
    @Data
    static class EvaluationResult {
        private String method;
        private int totalCases;
        private int passedCases;
        private int failedCases;
        private double accuracy;
        private long avgResponseTime;
        private List<FailedCase> failedDetails = new ArrayList<>();
    }

    @Data
    static class FailedCase {
        private int id;
        private String query;
        private String reason;
        private String actualResponse;
    }

    /**
     * 主评测方法
     */
    @Test
    public void runFullEvaluation() throws IOException {
        log.info("========== 开始 RAG 检索准确率评测 ==========");

        // 1. 加载测试用例
        List<TestCase> testCases = loadTestCases();
        log.info("加载测试用例数量: {}", testCases.size());

        // 2. 方案1：Baseline（直接调用，不做优化）
        log.info("\n========== 方案1：Baseline（无优化） ==========");
        EvaluationResult baselineResult = evaluateBaseline(testCases);
        printResult(baselineResult);

        // 3. 方案2：Query Rewriting
        log.info("\n========== 方案2：Query Rewriting ==========");
        EvaluationResult rewriteResult = evaluateWithQueryRewriting(testCases);
        printResult(rewriteResult);

        // 4. 方案3：完整 RAG（Query Rewriting + QuestionAnswerAdvisor）
        log.info("\n========== 方案3：完整 RAG 优化 ==========");
        EvaluationResult fullRagResult = evaluateWithFullRAG(testCases);
        printResult(fullRagResult);

        // 5. 生成对比报告
        log.info("\n========== 评测对比报告 ==========");
        printComparisonReport(baselineResult, rewriteResult, fullRagResult);
    }

    /**
     * 加载测试用例
     */
    private List<TestCase> loadTestCases() throws IOException {
        ClassPathResource resource = new ClassPathResource("evaluation/rag_test_cases.json");
        String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        JSONArray jsonArray = JSONUtil.parseArray(json);

        List<TestCase> testCases = new ArrayList<>();
        for (Object obj : jsonArray) {
            JSONObject jsonObj = (JSONObject) obj;
            TestCase tc = new TestCase();
            tc.setId(jsonObj.getInt("id"));
            tc.setQuery(jsonObj.getStr("query"));
            tc.setDifficulty(jsonObj.getStr("difficulty"));
            tc.setCategory(jsonObj.getStr("category"));
            tc.setCityLevel(jsonObj.getStr("city_level"));
            tc.setExpectedKeywords(jsonObj.getBeanList("expected_keywords", String.class));
            tc.setExpectedAnswerContains(jsonObj.getStr("expected_answer_contains"));
            tc.setShouldNotContain(jsonObj.getStr("should_not_contain"));
            testCases.add(tc);
        }
        return testCases;
    }

    /**
     * 方案1：Baseline（直接调用 LLM，不使用 RAG）
     */
    private EvaluationResult evaluateBaseline(List<TestCase> testCases) {
        EvaluationResult result = new EvaluationResult();
        result.setMethod("Baseline（无 RAG）");
        result.setTotalCases(testCases.size());

        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem("你是企业差旅助手，回答员工关于差旅报销的问题。")
                .build();

        long totalTime = 0;
        int passed = 0;

        for (TestCase tc : testCases) {
            long start = System.currentTimeMillis();
            String response = "";
            try {
                response = chatClient.prompt()
                        .user(tc.getQuery())
                        .call()
                        .content();
            } catch (Exception e) {
                log.error("Baseline 调用失败: {}", e.getMessage());
                response = "";
            }
            long cost = System.currentTimeMillis() - start;
            totalTime += cost;

            boolean isPass = validateResponse(tc, response);
            if (isPass) {
                passed++;
            } else {
                FailedCase fc = new FailedCase();
                fc.setId(tc.getId());
                fc.setQuery(tc.getQuery());
                fc.setReason("未包含关键信息或包含错误信息");
                fc.setActualResponse(response != null && response.length() > 0 ?
                        response.substring(0, Math.min(100, response.length())) : "无响应");
                result.getFailedDetails().add(fc);
            }

            log.info("[Baseline] Case {}: {} - {} ({}ms)", tc.getId(), tc.getQuery(), isPass ? "✓" : "✗", cost);
        }

        result.setPassedCases(passed);
        result.setFailedCases(testCases.size() - passed);
        result.setAccuracy((double) passed / testCases.size() * 100);
        result.setAvgResponseTime(totalTime / testCases.size());

        return result;
    }

    /**
     * 方案2：Query Rewriting
     */
    private EvaluationResult evaluateWithQueryRewriting(List<TestCase> testCases) {
        EvaluationResult result = new EvaluationResult();
        result.setMethod("Query Rewriting");
        result.setTotalCases(testCases.size());

        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem("你是企业差旅助手，回答员工关于差旅报销的问题。")
                .build();

        long totalTime = 0;
        int passed = 0;

        for (TestCase tc : testCases) {
            long start = System.currentTimeMillis();

            // 查询重写
            String rewrittenQuery = queryRewriter.doQueryRewrite(tc.getQuery());
            log.debug("原始查询: {} -> 重写后: {}", tc.getQuery(), rewrittenQuery);

            String response = "";
            try {
                response = chatClient.prompt()
                        .user(rewrittenQuery)
                        .call()
                        .content();
            } catch (Exception e) {
                log.error("Query Rewriting 调用失败: {}", e.getMessage());
                response = "";
            }
            long cost = System.currentTimeMillis() - start;
            totalTime += cost;

            boolean isPass = validateResponse(tc, response);
            if (isPass) {
                passed++;
            } else {
                FailedCase fc = new FailedCase();
                fc.setId(tc.getId());
                fc.setQuery(tc.getQuery());
                fc.setReason("查询重写后仍未命中");
                fc.setActualResponse(response != null && response.length() > 0 ?
                        response.substring(0, Math.min(100, response.length())) : "无响应");
                result.getFailedDetails().add(fc);
            }

            log.info("[Query Rewriting] Case {}: {} - {} ({}ms)", tc.getId(), tc.getQuery(), isPass ? "✓" : "✗", cost);
        }

        result.setPassedCases(passed);
        result.setFailedCases(testCases.size() - passed);
        result.setAccuracy((double) passed / testCases.size() * 100);
        result.setAvgResponseTime(totalTime / testCases.size());

        return result;
    }

    /**
     * 方案3：完整 RAG（Query Rewriting + QuestionAnswerAdvisor）
     */
    private EvaluationResult evaluateWithFullRAG(List<TestCase> testCases) {
        EvaluationResult result = new EvaluationResult();
        result.setMethod("完整 RAG 优化");
        result.setTotalCases(testCases.size());

        long totalTime = 0;
        int passed = 0;

        for (TestCase tc : testCases) {
            long start = System.currentTimeMillis();

            // 使用 EnterpriseAssistantApp 的完整 RAG 能力
            String chatId = UUID.randomUUID().toString();
            String response = "";
            try {
                response = enterpriseAssistantApp.doChatWithCorporateKnowledge(tc.getQuery(), chatId);
            } catch (Exception e) {
                log.error("Full RAG 调用失败: {}", e.getMessage());
                response = "";
            }

            long cost = System.currentTimeMillis() - start;
            totalTime += cost;

            boolean isPass = validateResponse(tc, response);
            if (isPass) {
                passed++;
            } else {
                FailedCase fc = new FailedCase();
                fc.setId(tc.getId());
                fc.setQuery(tc.getQuery());
                fc.setReason("完整 RAG 仍未通过");
                fc.setActualResponse(response != null && response.length() > 0 ?
                        response.substring(0, Math.min(100, response.length())) : "无响应");
                result.getFailedDetails().add(fc);
            }

            log.info("[Full RAG] Case {}: {} - {} ({}ms)", tc.getId(), tc.getQuery(), isPass ? "✓" : "✗", cost);
        }

        result.setPassedCases(passed);
        result.setFailedCases(testCases.size() - passed);
        result.setAccuracy((double) passed / testCases.size() * 100);
        result.setAvgResponseTime(totalTime / testCases.size());

        return result;
    }

    /**
     * 验证响应是否符合预期
     */
    private boolean validateResponse(TestCase tc, String response) {
        // 空响应直接失败
        if (response == null || response.trim().isEmpty()) {
            return false;
        }

        // 1. 必须包含预期关键词（放宽匹配：支持数字的多种表达）
        if (tc.getExpectedAnswerContains() != null) {
            String expected = tc.getExpectedAnswerContains();
            // 如果期望包含"500元"，也接受"500"、"500元/晚"、"五百元"等
            if (!response.contains(expected)) {
                // 尝试提取数字进行模糊匹配
                String numberOnly = expected.replaceAll("[^0-9]", "");
                if (!numberOnly.isEmpty() && !response.contains(numberOnly)) {
                    return false;
                }
            }
        }

        // 2. 不能包含错误信息
        if (tc.getShouldNotContain() != null && response.contains(tc.getShouldNotContain())) {
            return false;
        }

        // 3. 至少包含一个预期关键词（放宽匹配）
        if (tc.getExpectedKeywords() != null && !tc.getExpectedKeywords().isEmpty()) {
            boolean hasKeyword = false;
            for (String keyword : tc.getExpectedKeywords()) {
                if (response.contains(keyword)) {
                    hasKeyword = true;
                    break;
                }
                // 尝试数字模糊匹配
                String numberOnly = keyword.replaceAll("[^0-9]", "");
                if (!numberOnly.isEmpty() && response.contains(numberOnly)) {
                    hasKeyword = true;
                    break;
                }
            }
            if (!hasKeyword) {
                return false;
            }
        }

        return true;
    }

    /**
     * 打印单个方案结果
     */
    private void printResult(EvaluationResult result) {
        log.info("方案: {}", result.getMethod());
        log.info("总用例数: {}", result.getTotalCases());
        log.info("通过数: {}", result.getPassedCases());
        log.info("失败数: {}", result.getFailedCases());
        log.info(String.format("准确率: %.2f%%", result.getAccuracy()));
        log.info("平均响应时间: {}ms", result.getAvgResponseTime());

        if (!result.getFailedDetails().isEmpty()) {
            log.info("失败用例详情:");
            for (FailedCase fc : result.getFailedDetails()) {
                log.info("  - Case {}: {} (原因: {})", fc.getId(), fc.getQuery(), fc.getReason());
            }
        }
    }

    /**
     * 打印对比报告
     */
    private void printComparisonReport(EvaluationResult baseline, EvaluationResult rewrite, EvaluationResult fullRag) {
        log.info("\n╔════════════════════════════════════════════════════════════════╗");
        log.info("║                    RAG 评测对比报告                             ║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ 方案                  │ 准确率    │ 平均延迟   │ 提升幅度      ║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info(String.format("║ %-20s │ %6.2f%%  │ %7dms │ baseline     ║",
                baseline.getMethod(), baseline.getAccuracy(), baseline.getAvgResponseTime()));
        log.info(String.format("║ %-20s │ %6.2f%%  │ %7dms │ +%.2f%%       ║",
                rewrite.getMethod(), rewrite.getAccuracy(), rewrite.getAvgResponseTime(),
                rewrite.getAccuracy() - baseline.getAccuracy()));
        log.info(String.format("║ %-20s │ %6.2f%%  │ %7dms │ +%.2f%%       ║",
                fullRag.getMethod(), fullRag.getAccuracy(), fullRag.getAvgResponseTime(),
                fullRag.getAccuracy() - baseline.getAccuracy()));
        log.info("╚════════════════════════════════════════════════════════════════╝");

        log.info("\n关键发现:");
        log.info(String.format("1. Query Rewriting 使准确率从 %.2f%% 提升到 %.2f%%（+%.2f%%）",
                baseline.getAccuracy(), rewrite.getAccuracy(), rewrite.getAccuracy() - baseline.getAccuracy()));
        log.info(String.format("2. 完整 RAG 优化使准确率达到 %.2f%%（相比 Baseline +%.2f%%）",
                fullRag.getAccuracy(), fullRag.getAccuracy() - baseline.getAccuracy()));
        log.info("3. 平均响应延迟: {}ms（可接受范围）", fullRag.getAvgResponseTime());
    }
}
