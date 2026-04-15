package com.jblmj.aiagent.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jblmj.aiagent.app.EnterpriseAssistantApp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.io.InputStream;
import java.util.*;

/**
 * MCP 工具调用评测
 *
 * 测试目标：
 * 1. 验证 MCP 地图工具是否正确调用
 * 2. 统计工具调用成功率
 * 3. 测量平均响应延迟
 * 4. 识别失败模式
 *
 * 面试价值：
 * - 证明你真的接入了 MCP，不是纸上谈兵
 * - 展示对工具调用准确性的量化评估
 * - 体现工程化测试思维
 */
@Slf4j
@SpringBootTest
public class McpEvaluationTest {

    @Resource
    private EnterpriseAssistantApp enterpriseAssistantApp;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 主测试方法：运行所有 MCP 测试用例
     */
    @Test
    public void testMcpToolCalling() throws Exception {
        log.info("========================================");
        log.info("开始 MCP 工具调用评测");
        log.info("========================================");

        // 1. 加载测试用例
        List<McpTestCase> testCases = loadTestCases();
        log.info("加载了 {} 条测试用例", testCases.size());

        // 2. 运行测试
        List<McpTestResult> results = new ArrayList<>();
        for (McpTestCase testCase : testCases) {
            McpTestResult result = runSingleTest(testCase);
            results.add(result);

            // 打印单条结果
            log.info("用例 {}: {} | 耗时: {}ms | 工具调用: {} | 内容验证: {}",
                    testCase.getId(),
                    result.isSuccess() ? "✓ 通过" : "✗ 失败",
                    result.getLatencyMs(),
                    result.isToolCalled() ? "是" : "否",
                    result.isContentValid() ? "通过" : "失败");
        }

        // 3. 生成评测报告
        generateReport(results, testCases);
    }

    /**
     * 运行单条测试用例
     */
    private McpTestResult runSingleTest(McpTestCase testCase) {
        McpTestResult result = new McpTestResult();
        result.setTestCaseId(testCase.getId());
        result.setQuery(testCase.getQuery());

        try {
            long startTime = System.currentTimeMillis();

            // 调用综合接口（包含 MCP 工具）
            String response = enterpriseAssistantApp.doComprehensiveChat(
                    testCase.getQuery(),
                    "mcp_test_" + testCase.getId()
            );

            long endTime = System.currentTimeMillis();
            result.setLatencyMs(endTime - startTime);
            result.setResponse(response);

            // 验证工具调用
            boolean toolCalled = detectToolCall(response, testCase);
            result.setToolCalled(toolCalled);

            // 验证内容质量
            boolean contentValid = validateContent(response, testCase);
            result.setContentValid(contentValid);

            // 判断是否成功
            boolean expectedToolCall = testCase.isExpectedToolCall();
            boolean success = (toolCalled == expectedToolCall) && contentValid;
            result.setSuccess(success);

            if (!success) {
                result.setFailureReason(buildFailureReason(testCase, toolCalled, contentValid));
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setFailureReason("异常: " + e.getMessage());
            log.error("测试用例 {} 执行失败", testCase.getId(), e);
        }

        return result;
    }

    /**
     * 检测是否调用了地图工具
     *
     * 判断依据：
     * 1. 响应中包含地图特征词（距离、公里、分钟、路线等）
     * 2. 响应中包含具体数字+单位组合
     */
    private boolean detectToolCall(String response, McpTestCase testCase) {
        if (response == null || response.isEmpty()) {
            return false;
        }

        String lowerResponse = response.toLowerCase();

        // 地图工具特征词
        String[] mapKeywords = {"距离", "公里", "km", "分钟", "路线", "导航", "地铁", "打车"};

        int matchCount = 0;
        for (String keyword : mapKeywords) {
            if (lowerResponse.contains(keyword.toLowerCase())) {
                matchCount++;
            }
        }

        // 至少匹配 2 个特征词才认为调用了工具
        return matchCount >= 2;
    }

    /**
     * 验证响应内容质量
     */
    private boolean validateContent(String response, McpTestCase testCase) {
        if (response == null || response.isEmpty()) {
            return false;
        }

        String lowerResponse = response.toLowerCase();

        // 检查必须包含的关键词
        int keywordMatchCount = 0;
        for (String keyword : testCase.getExpectedKeywords()) {
            if (lowerResponse.contains(keyword.toLowerCase())) {
                keywordMatchCount++;
            }
        }

        // 至少匹配 50% 的关键词
        double matchRate = (double) keywordMatchCount / testCase.getExpectedKeywords().size();
        return matchRate >= 0.5;
    }

    /**
     * 构建失败原因
     */
    private String buildFailureReason(McpTestCase testCase, boolean toolCalled, boolean contentValid) {
        List<String> reasons = new ArrayList<>();

        if (testCase.isExpectedToolCall() && !toolCalled) {
            reasons.add("应该调用工具但未调用");
        } else if (!testCase.isExpectedToolCall() && toolCalled) {
            reasons.add("不应该调用工具但调用了");
        }

        if (!contentValid) {
            reasons.add("内容验证失败");
        }

        return String.join("; ", reasons);
    }

    /**
     * 生成评测报告
     */
    private void generateReport(List<McpTestResult> results, List<McpTestCase> testCases) {
        log.info("\n========================================");
        log.info("MCP 工具调用评测报告");
        log.info("========================================");

        // 1. 总体统计
        int totalCases = results.size();
        long successCount = results.stream().filter(McpTestResult::isSuccess).count();
        double successRate = (double) successCount / totalCases * 100;

        long avgLatency = (long) results.stream()
                .mapToLong(McpTestResult::getLatencyMs)
                .average()
                .orElse(0);

        log.info("总用例数: {}", totalCases);
        log.info("成功数: {}", successCount);
        log.info("成功率: {:.2f}%", successRate);
        log.info("平均延迟: {}ms", avgLatency);

        // 2. 工具调用统计
        long shouldCallCount = testCases.stream().filter(McpTestCase::isExpectedToolCall).count();
        long actualCallCount = results.stream().filter(McpTestResult::isToolCalled).count();
        long correctCallCount = results.stream()
                .filter(r -> {
                    McpTestCase tc = findTestCase(testCases, r.getTestCaseId());
                    return tc != null && (r.isToolCalled() == tc.isExpectedToolCall());
                })
                .count();

        log.info("\n工具调用统计:");
        log.info("  应该调用工具的用例: {}", shouldCallCount);
        log.info("  实际调用工具的用例: {}", actualCallCount);
        log.info("  工具调用正确率: {:.2f}%", (double) correctCallCount / totalCases * 100);

        // 3. 按难度统计
        Map<String, List<McpTestResult>> byDifficulty = new HashMap<>();
        for (McpTestResult result : results) {
            McpTestCase tc = findTestCase(testCases, result.getTestCaseId());
            if (tc != null) {
                byDifficulty.computeIfAbsent(tc.getDifficulty(), k -> new ArrayList<>()).add(result);
            }
        }

        log.info("\n按难度统计:");
        for (Map.Entry<String, List<McpTestResult>> entry : byDifficulty.entrySet()) {
            long success = entry.getValue().stream().filter(McpTestResult::isSuccess).count();
            double rate = (double) success / entry.getValue().size() * 100;
            log.info("  {}: {:.2f}% ({}/{})",
                    entry.getKey(), rate, success, entry.getValue().size());
        }

        // 4. 失败用例详情
        List<McpTestResult> failures = results.stream()
                .filter(r -> !r.isSuccess())
                .toList();

        if (!failures.isEmpty()) {
            log.info("\n失败用例详情:");
            for (McpTestResult failure : failures) {
                log.info("  用例 {}: {}", failure.getTestCaseId(), failure.getFailureReason());
                log.info("    查询: {}", failure.getQuery());
                log.info("    响应片段: {}", truncate(failure.getResponse(), 100));
            }
        }

        log.info("\n========================================");
        log.info("评测完成");
        log.info("========================================");
    }

    /**
     * 加载测试用例
     */
    private List<McpTestCase> loadTestCases() throws Exception {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("evaluation/mcp_test_cases.json");

        if (is == null) {
            throw new RuntimeException("找不到测试用例文件: evaluation/mcp_test_cases.json");
        }

        JsonNode root = objectMapper.readTree(is);
        List<McpTestCase> testCases = new ArrayList<>();

        for (JsonNode node : root) {
            McpTestCase testCase = new McpTestCase();
            testCase.setId(node.get("id").asText());
            testCase.setQuery(node.get("query").asText());
            testCase.setDifficulty(node.get("difficulty").asText());
            testCase.setCategory(node.get("category").asText());
            testCase.setExpectedToolCall(node.get("expected_tool_call").asBoolean());
            testCase.setDescription(node.get("description").asText());

            List<String> keywords = new ArrayList<>();
            node.get("expected_keywords").forEach(k -> keywords.add(k.asText()));
            testCase.setExpectedKeywords(keywords);

            testCases.add(testCase);
        }

        return testCases;
    }

    private McpTestCase findTestCase(List<McpTestCase> testCases, String id) {
        return testCases.stream()
                .filter(tc -> tc.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    // ========== 数据类 ==========

    static class McpTestCase {
        private String id;
        private String query;
        private String difficulty;
        private String category;
        private boolean expectedToolCall;
        private List<String> expectedKeywords;
        private String description;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public boolean isExpectedToolCall() { return expectedToolCall; }
        public void setExpectedToolCall(boolean expectedToolCall) { this.expectedToolCall = expectedToolCall; }
        public List<String> getExpectedKeywords() { return expectedKeywords; }
        public void setExpectedKeywords(List<String> expectedKeywords) { this.expectedKeywords = expectedKeywords; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    static class McpTestResult {
        private String testCaseId;
        private String query;
        private String response;
        private long latencyMs;
        private boolean toolCalled;
        private boolean contentValid;
        private boolean success;
        private String failureReason;

        // Getters and Setters
        public String getTestCaseId() { return testCaseId; }
        public void setTestCaseId(String testCaseId) { this.testCaseId = testCaseId; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        public long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
        public boolean isToolCalled() { return toolCalled; }
        public void setToolCalled(boolean toolCalled) { this.toolCalled = toolCalled; }
        public boolean isContentValid() { return contentValid; }
        public void setContentValid(boolean contentValid) { this.contentValid = contentValid; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    }
}
