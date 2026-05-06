package com.jblmj.aiagent.evaluation;

import com.jblmj.aiagent.rag.EnterpriseQueryRewriter;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Temperature参数调优测试
 *
 * 测试目标：
 * 1. 对比不同Temperature（0.0/0.1/0.3/0.5）对改写质量的影响
 * 2. 评估改写稳定性（相同查询多次改写的一致性）
 * 3. 选择最优Temperature配置
 *
 * 面试价值：
 * - 展示对LLM参数调优的理解
 * - 体现数据驱动的优化思路
 * - 证明对生产环境稳定性的关注
 *
 * @author jblmj
 */
@SpringBootTest
@Slf4j
public class TemperatureOptimizationTest {

    @Resource
    private ChatModel dashscopeChatModel;

    @Data
    static class TemperatureTestResult {
        private double temperature;
        private int totalTests;
        private int passedTests;
        private double accuracy;
        private double avgStability;  // 稳定性（多次改写的一致性）
        private long avgResponseTime;
        private List<String> failedCases = new ArrayList<>();
    }

    /**
     * 主测试方法：对比不同Temperature
     */
    @Test
    public void runTemperatureOptimization() {
        log.info("========== 开始Temperature参数调优测试 ==========");

        // 测试的Temperature值
        double[] temperatures = {0.0, 0.1, 0.3, 0.5};

        // 准备测试查询
        List<String> testQueries = prepareTestQueries();
        log.info("测试查询数量: {}", testQueries.size());

        // 对每个Temperature进行测试
        Map<Double, TemperatureTestResult> results = new HashMap<>();
        for (double temp : temperatures) {
            log.info("\n========== 测试Temperature={} ==========", temp);
            TemperatureTestResult result = testTemperature(temp, testQueries);
            results.put(temp, result);
            printResult(result);
        }

        // 生成对比报告
        log.info("\n========== Temperature对比报告 ==========");
        printComparisonReport(results);

        // 推荐最优Temperature
        double bestTemp = recommendBestTemperature(results);
        log.info("\n========== 推荐配置 ==========");
        log.info("最优Temperature: {}", bestTemp);
        log.info("原因: 综合考虑准确率和稳定性");
    }

    /**
     * 准备测试查询（选择代表性查询）
     */
    private List<String> prepareTestQueries() {
        return List.of(
                "去魔都出差住宿能报多少",
                "去BJ出差住宿能报多少",
                "北京出差不能住五星级酒店吗",
                "去省会城市出差住宿能报多少",
                "北京和上海的住宿标准哪个高",
                "出差30天伙食补助总共多少",
                "明天去上海出差，住宿和伙食一共能报多少",
                "如果打车了还能领交通补助吗",
                "出差坐高铁可以报销吗",
                "北京住宿标准"
        );
    }

    /**
     * 测试指定Temperature
     */
    private TemperatureTestResult testTemperature(double temperature, List<String> queries) {
        TemperatureTestResult result = new TemperatureTestResult();
        result.setTemperature(temperature);
        result.setTotalTests(queries.size());

        int passed = 0;
        long totalTime = 0;
        double totalStability = 0;

        ChatClient chatClient = ChatClient.builder(dashscopeChatModel).build();

        for (String query : queries) {
            log.debug("测试查询: {}", query);

            try {
                // 测试改写质量
                long startTime = System.currentTimeMillis();
                String rewritten = rewriteWithTemperature(chatClient, query, temperature);
                long endTime = System.currentTimeMillis();
                totalTime += (endTime - startTime);

                // 测试稳定性（多次改写的一致性）
                double stability = testStability(chatClient, query, temperature, 3);
                totalStability += stability;

                // 验证改写质量
                boolean isValid = validateRewrite(query, rewritten);
                if (isValid) {
                    passed++;
                    log.debug("✓ 改写成功: {} -> {}", query, rewritten);
                } else {
                    result.getFailedCases().add(query);
                    log.debug("✗ 改写失败: {} -> {}", query, rewritten);
                }

            } catch (Exception e) {
                result.getFailedCases().add(query + " (异常: " + e.getMessage() + ")");
                log.error("改写异常: {}", e.getMessage());
            }
        }

        result.setPassedTests(passed);
        result.setAccuracy((double) passed / queries.size() * 100);
        result.setAvgStability(totalStability / queries.size() * 100);
        result.setAvgResponseTime(totalTime / queries.size());

        return result;
    }

    /**
     * 使用指定Temperature改写查询
     */
    private String rewriteWithTemperature(ChatClient chatClient, String query, double temperature) {
        String prompt = String.format("""
                将以下查询改写为更适合向量检索的标准化表达。

                规则：
                1. 替换口语词为标准术语（魔都→上海、BJ→北京）
                2. 补充关键信息（城市分类、费用类型）
                3. 保留原始语义
                4. 只返回改写后的查询，不要解释

                查询：%s

                改写后的查询：
                """, query);

        return chatClient.prompt()
                .user(prompt)
                .options(org.springframework.ai.chat.prompt.ChatOptions.builder()
                        .temperature(temperature)
                        .build())
                .call()
                .content()
                .trim();
    }

    /**
     * 测试稳定性（多次改写的一致性）
     */
    private double testStability(ChatClient chatClient, String query, double temperature, int times) {
        List<String> results = new ArrayList<>();

        // 多次改写
        for (int i = 0; i < times; i++) {
            String rewritten = rewriteWithTemperature(chatClient, query, temperature);
            results.add(rewritten);
        }

        // 计算一致性（相同结果的比例）
        Map<String, Integer> counts = new HashMap<>();
        for (String result : results) {
            counts.put(result, counts.getOrDefault(result, 0) + 1);
        }

        // 最高频次 / 总次数
        int maxCount = counts.values().stream().max(Integer::compareTo).orElse(0);
        double stability = (double) maxCount / times;

        log.debug("稳定性测试: {} -> {} (一致性: {:.2f}%)", query, results, stability * 100);
        return stability;
    }

    /**
     * 验证改写质量（简化版）
     */
    private boolean validateRewrite(String original, String rewritten) {
        // 基本检查
        if (rewritten == null || rewritten.isEmpty() || rewritten.length() < 5) {
            return false;
        }

        // 长度检查
        if (rewritten.length() > 100) {
            return false;
        }

        // 关键词保留检查（至少保留一些核心词）
        String[] keywords = {"北京", "上海", "住宿", "标准", "出差", "报销", "伙食", "补助", "交通"};
        boolean hasKeyword = false;
        for (String keyword : keywords) {
            if (original.contains(keyword) || rewritten.contains(keyword)) {
                hasKeyword = true;
                break;
            }
        }

        return hasKeyword;
    }

    /**
     * 输出单个Temperature的测试结果
     */
    private void printResult(TemperatureTestResult result) {
        log.info("Temperature: {}", result.getTemperature());
        log.info("总测试数: {}", result.getTotalTests());
        log.info("通过数: {}", result.getPassedTests());
        log.info("准确率: {:.2f}%", result.getAccuracy());
        log.info("稳定性: {:.2f}%", result.getAvgStability());
        log.info("平均响应时间: {}ms", result.getAvgResponseTime());

        if (!result.getFailedCases().isEmpty()) {
            log.info("失败用例: {}", result.getFailedCases());
        }
    }

    /**
     * 输出对比报告
     */
    private void printComparisonReport(Map<Double, TemperatureTestResult> results) {
        log.info("Temperature | 准确率 | 稳定性 | 响应时间");
        log.info("-----------|--------|--------|----------");

        for (double temp : new double[]{0.0, 0.1, 0.3, 0.5}) {
            TemperatureTestResult result = results.get(temp);
            log.info(String.format("%.1f        | %.2f%% | %.2f%% | %dms",
                    temp,
                    result.getAccuracy(),
                    result.getAvgStability(),
                    result.getAvgResponseTime()));
        }
    }

    /**
     * 推荐最优Temperature
     */
    private double recommendBestTemperature(Map<Double, TemperatureTestResult> results) {
        double bestTemp = 0.0;
        double bestScore = 0.0;

        for (Map.Entry<Double, TemperatureTestResult> entry : results.entrySet()) {
            TemperatureTestResult result = entry.getValue();

            // 综合评分：准确率 * 0.6 + 稳定性 * 0.4
            double score = result.getAccuracy() * 0.6 + result.getAvgStability() * 0.4;

            log.info("Temperature {} 综合评分: {:.2f}", entry.getKey(), score);

            if (score > bestScore) {
                bestScore = score;
                bestTemp = entry.getKey();
            }
        }

        return bestTemp;
    }
}
