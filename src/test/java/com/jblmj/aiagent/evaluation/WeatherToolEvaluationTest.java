package com.jblmj.aiagent.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jblmj.aiagent.app.EnterpriseAssistantApp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 天气工具调用评测
 * 验证 Agent 是否能正确调用天气查询工具
 */
@SpringBootTest
@Slf4j
public class WeatherToolEvaluationTest {

    @Autowired
    private EnterpriseAssistantApp assistantApp;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testWeatherToolCalling() throws IOException {
        log.info("=== 开始天气工具调用评测 ===");

        // 加载测试用例
        List<WeatherTestCase> testCases = loadTestCases();
        log.info("加载了 {} 条测试用例", testCases.size());

        // 执行评测
        List<WeatherTestResult> results = new ArrayList<>();
        int totalCases = testCases.size();
        int passedCases = 0;
        long totalLatency = 0;

        for (WeatherTestCase testCase : testCases) {
            log.info("\n--- 测试用例 {} ---", testCase.id);
            log.info("查询: {}", testCase.query);

            long startTime = System.currentTimeMillis();
            final String[] responseHolder = {""};
            boolean success = false;
            String errorMessage = null;

            try {
                // 调用综合对话接口（包含工具调用能力）
                responseHolder[0] = assistantApp.doComprehensiveChat(testCase.query, "weather-test");
                long latency = System.currentTimeMillis() - startTime;
                totalLatency += latency;

                log.info("响应: {}", responseHolder[0]);
                log.info("延迟: {}ms", latency);

                // 验证响应
                boolean containsKeywords = testCase.expectedKeywords.stream()
                        .anyMatch(keyword -> responseHolder[0].contains(keyword));

                boolean noForbiddenWords = testCase.shouldNotContain.stream()
                        .noneMatch(responseHolder[0]::contains);

                success = containsKeywords && noForbiddenWords;

                if (success) {
                    passedCases++;
                    log.info("✓ 测试通过");
                } else {
                    log.warn("✗ 测试失败");
                    if (!containsKeywords) {
                        errorMessage = "响应中未包含预期关键词: " + testCase.expectedKeywords;
                    } else {
                        errorMessage = "响应中包含禁止词: " + testCase.shouldNotContain;
                    }
                }

                results.add(new WeatherTestResult(
                        testCase.id,
                        testCase.query,
                        responseHolder[0],
                        success,
                        latency,
                        errorMessage
                ));

            } catch (Exception e) {
                long latency = System.currentTimeMillis() - startTime;
                totalLatency += latency;
                log.error("✗ 测试异常: {}", e.getMessage(), e);
                results.add(new WeatherTestResult(
                        testCase.id,
                        testCase.query,
                        responseHolder[0],
                        false,
                        latency,
                        "异常: " + e.getMessage()
                ));
            }
        }

        // 输出统计结果
        log.info("\n=== 评测结果 ===");
        log.info("总用例数: {}", totalCases);
        log.info("通过数: {}", passedCases);
        log.info("失败数: {}", totalCases - passedCases);
        log.info("成功率: {}", String.format("%.1f%%", (passedCases * 100.0 / totalCases)));
        log.info("平均延迟: {}ms", totalLatency / totalCases);

        // 输出失败用例
        log.info("\n=== 失败用例详情 ===");
        results.stream()
                .filter(r -> !r.success)
                .forEach(r -> {
                    log.info("\n用例ID: {}", r.id);
                    log.info("查询: {}", r.query);
                    log.info("响应: {}", r.response);
                    log.info("失败原因: {}", r.errorMessage);
                });
    }

    private List<WeatherTestCase> loadTestCases() throws IOException {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("evaluation/weather_test_cases.json");
        if (inputStream == null) {
            throw new IOException("找不到测试用例文件: evaluation/weather_test_cases.json");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawCases = objectMapper.readValue(inputStream, List.class);
        List<WeatherTestCase> testCases = new ArrayList<>();

        for (Map<String, Object> rawCase : rawCases) {
            @SuppressWarnings("unchecked")
            List<String> expectedKeywords = (List<String>) rawCase.get("expected_keywords");
            @SuppressWarnings("unchecked")
            List<String> shouldNotContain = (List<String>) rawCase.get("should_not_contain");

            testCases.add(new WeatherTestCase(
                    (String) rawCase.get("id"),
                    (String) rawCase.get("query"),
                    (String) rawCase.get("difficulty"),
                    (Boolean) rawCase.get("expected_tool_call"),
                    expectedKeywords,
                    shouldNotContain
            ));
        }

        return testCases;
    }

    record WeatherTestCase(
            String id,
            String query,
            String difficulty,
            boolean expectedToolCall,
            List<String> expectedKeywords,
            List<String> shouldNotContain
    ) {
    }

    record WeatherTestResult(
            String id,
            String query,
            String response,
            boolean success,
            long latency,
            String errorMessage
    ) {
    }
}
