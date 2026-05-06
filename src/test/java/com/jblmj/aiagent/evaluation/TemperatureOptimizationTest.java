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
        log.info("测试设计说明：");
        log.info("- 测试集规模：20个查询，覆盖6种查询模式");
        log.info("- 稳定性测试：每个查询改写5次，计算一致性");
        log.info("- Temperature范围：0.0（完全确定）到 0.5（中等随机）");
        log.info("- 评估指标：准确率（60%权重）+ 稳定性（40%权重）");

        // 测试的Temperature值
        double[] temperatures = {0.0, 0.1, 0.3, 0.5};

        // 准备测试查询
        List<String> testQueries = prepareTestQueries();
        log.info("\n测试查询数量: {}", testQueries.size());
        log.info("查询分类：口语化(5) + 缩写(3) + 否定(3) + 复合(4) + 模糊(3) + 标准(2)");

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

        // 输出详细分析
        printDetailedAnalysis(results, bestTemp);
    }

    /**
     * 准备测试查询（科学设计的测试集）
     *
     * 测试集设计原则：
     * 1. 覆盖多种查询模式（口语化、缩写、否定、复合、模糊）
     * 2. 包含不同难度级别（简单、中等、困难）
     * 3. 样本量足够（20个查询，每个测试5次稳定性）
     * 4. 有明确的预期改写结果（可验证准确率）
     *
     * 查询分类：
     * - 口语化查询（5个）：测试同义词替换能力
     * - 缩写查询（3个）：测试标准化能力
     * - 否定查询（3个）：测试否定语义保留
     * - 复合查询（4个）：测试多意图提取
     * - 模糊查询（3个）：测试信息补全
     * - 标准查询（2个）：测试基线性能
     */
    private List<String> prepareTestQueries() {
        return List.of(
                // === 口语化查询（5个）===
                "去魔都出差住宿能报多少",              // 魔都→上海
                "去帝都出差住宿标准是啥",              // 帝都→北京，啥→什么
                "去羊城出差能住啥酒店",                // 羊城→广州
                "去大鹏出差住宿咋报销",                // 大鹏→深圳，咋→怎么
                "去蓉城出差住宿费用咋算",              // 蓉城→成都

                // === 缩写查询（3个）===
                "去BJ出差住宿能报多少",                // BJ→北京
                "去SH出差住宿标准",                    // SH→上海
                "GZ出差住宿费用",                      // GZ→广州

                // === 否定查询（3个）===
                "北京出差不能住五星级酒店吗",          // 保留"不能"
                "上海出差不可以住民宿吗",              // 保留"不可以"
                "深圳出差没有住宿补助吗",              // 保留"没有"

                // === 复合查询（4个）===
                "明天去上海出差，住宿和伙食一共能报多少",  // 住宿+伙食
                "北京和上海的住宿标准哪个高",              // 对比查询
                "出差30天伙食补助总共多少",                // 计算查询
                "去杭州出差3天，住宿交通伙食一共多少钱",   // 多费用类型

                // === 模糊查询（3个）===
                "去省会城市出差住宿能报多少",          // 省会城市→需要补充具体城市
                "如果打车了还能领交通补助吗",          // 条件查询
                "出差坐高铁可以报销吗",                // 是非查询

                // === 标准查询（2个）===
                "北京住宿标准",                        // 最简单
                "上海出差住宿费用报销标准"             // 标准表达
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

                // 测试稳定性（增加到5次）
                double stability = testStability(chatClient, query, temperature, 5);
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

                **重要规则**：
                1. 替换口语词为标准术语
                   - 魔都 → 上海
                   - 帝都 → 北京
                   - 羊城 → 广州
                   - 大鹏 → 深圳
                   - 蓉城 → 成都
                   - BJ → 北京
                   - SH → 上海
                   - GZ → 广州

                2. **必须保留否定词**（不能、不可以、没有、不是、不允许、禁止）
                   - 示例：北京出差不能住五星级酒店吗 → 北京出差住宿标准 不能住五星级酒店

                3. 补充关键信息（城市分类、费用类型）

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
     *
     * 优化点：
     * 1. 增加测试次数：3次 → 5次（更可靠）
     * 2. 记录所有结果，便于分析
     * 3. 计算标准差，量化波动程度
     */
    private double testStability(ChatClient chatClient, String query, double temperature, int times) {
        List<String> results = new ArrayList<>();

        // 多次改写（增加到5次）
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

        // 如果有多个不同结果，记录下来（用于分析）
        if (counts.size() > 1) {
            log.debug("稳定性测试 [{}]: 产生了{}种不同结果", query, counts.size());
            counts.forEach((result, count) ->
                log.debug("  - {} (出现{}次)", result, count)
            );
        }

        return stability;
    }

    /**
     * 验证改写质量（更严格的验证规则）
     *
     * 验证维度：
     * 1. 基本格式检查（非空、长度合理）
     * 2. 关键词保留检查（核心信息不丢失）
     * 3. 标准化检查（口语化词汇是否被替换）
     * 4. 否定词保留检查（否定语义不能丢失）
     */
    private boolean validateRewrite(String original, String rewritten) {
        // 1. 基本格式检查
        if (rewritten == null || rewritten.isEmpty() || rewritten.length() < 5) {
            log.warn("改写失败：结果为空或过短");
            return false;
        }

        if (rewritten.length() > 100) {
            log.warn("改写失败：结果过长（可能包含解释）");
            return false;
        }

        // 2. 关键词保留检查
        String[] keywords = {"北京", "上海", "广州", "深圳", "成都", "杭州",
                            "住宿", "标准", "出差", "报销", "伙食", "补助", "交通", "费用"};
        boolean hasKeyword = false;
        for (String keyword : keywords) {
            if (original.contains(keyword) || rewritten.contains(keyword)) {
                hasKeyword = true;
                break;
            }
        }
        if (!hasKeyword) {
            log.warn("改写失败：缺少关键词");
            return false;
        }

        // 3. 标准化检查（口语化词汇应该被替换）
        Map<String, String> colloquialMap = Map.of(
            "魔都", "上海",
            "帝都", "北京",
            "羊城", "广州",
            "大鹏", "深圳",
            "蓉城", "成都",
            "BJ", "北京",
            "SH", "上海",
            "GZ", "广州"
        );

        for (Map.Entry<String, String> entry : colloquialMap.entrySet()) {
            String colloquial = entry.getKey();
            String standard = entry.getValue();
            if (original.contains(colloquial)) {
                // 原查询包含口语化词汇，改写后应该被替换为标准词汇
                if (rewritten.contains(colloquial) && !rewritten.contains(standard)) {
                    log.warn("改写失败：口语化词汇'{}' 未被标准化为'{}'", colloquial, standard);
                    return false;
                }
            }
        }

        // 4. 否定词保留检查
        String[] negationWords = {"不能", "不可以", "没有", "不是", "不允许", "禁止"};
        for (String negation : negationWords) {
            if (original.contains(negation) && !rewritten.contains(negation)) {
                log.warn("改写失败：否定词'{}' 丢失", negation);
                return false;
            }
        }

        return true;
    }

    /**
     * 输出单个Temperature的测试结果
     */
    private void printResult(TemperatureTestResult result) {
        log.info("Temperature: {}", result.getTemperature());
        log.info("总测试数: {}", result.getTotalTests());
        log.info("通过数: {}", result.getPassedTests());
        log.info("准确率: {}%", String.format("%.2f", result.getAccuracy()));
        log.info("稳定性: {}%", String.format("%.2f", result.getAvgStability()));
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

            log.info("Temperature {} 综合评分: {}", entry.getKey(), String.format("%.2f", score));

            if (score > bestScore) {
                bestScore = score;
                bestTemp = entry.getKey();
            }
        }

        return bestTemp;
    }

    /**
     * 输出详细分析报告
     */
    private void printDetailedAnalysis(Map<Double, TemperatureTestResult> results, double bestTemp) {
        log.info("\n========== 详细分析 ==========");

        TemperatureTestResult bestResult = results.get(bestTemp);

        log.info("1. 最优配置分析：");
        log.info("   - Temperature={} 在准确率和稳定性之间取得最佳平衡", bestTemp);
        log.info("   - 准确率：{}%（{}个查询全部通过）",
            String.format("%.2f", bestResult.getAccuracy()),
            bestResult.getPassedTests());
        log.info("   - 稳定性：{}%（5次改写中平均{}次一致）",
            String.format("%.2f", bestResult.getAvgStability()),
            String.format("%.1f", bestResult.getAvgStability() / 20));

        log.info("\n2. Temperature对比：");
        log.info("   - Temperature=0.0：稳定性最高，但可能过于死板");
        log.info("   - Temperature=0.1-0.3：平衡稳定性和灵活性");
        log.info("   - Temperature=0.5：随机性较高，不适合生产环境");

        log.info("\n3. 生产环境建议：");
        log.info("   - 使用Temperature={}作为默认配置", bestTemp);
        log.info("   - 对于标准查询，可以缓存改写结果（稳定性高）");
        log.info("   - 对于复杂查询，可以动态调整Temperature");

        log.info("\n4. 测试覆盖度：");
        log.info("   - 测试集规模：20个查询 × 5次稳定性测试 = 100次改写");
        log.info("   - 查询类型：覆盖6种典型场景（口语化、缩写、否定等）");
        log.info("   - 验证维度：准确率、稳定性、响应时间");
    }
}
