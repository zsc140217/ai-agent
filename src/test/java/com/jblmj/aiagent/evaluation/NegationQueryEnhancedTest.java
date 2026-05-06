package com.jblmj.aiagent.evaluation;

import com.jblmj.aiagent.rag.NegationQueryHandler;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * 否定查询处理增强测试
 *
 * 测试目标：
 * 1. 验证否定查询检测的准确性
 * 2. 测试否定查询处理的效果
 * 3. 覆盖多种否定模式（直接否定、否定疑问、否定判断）
 *
 * 面试价值：
 * - 展示对RAG检索问题的深刻理解（向量检索对否定词不敏感）
 * - 体现对NLP细节的关注（语义保留）
 * - 证明你理解如何解决实际业务问题
 *
 * @author jblmj
 */
@SpringBootTest
@Slf4j
public class NegationQueryEnhancedTest {

    @Resource
    private NegationQueryHandler negationHandler;

    @Data
    static class NegationTestCase {
        private String query;
        private NegationQueryHandler.NegationType expectedType;
        private List<String> expectedKeywords;
        private String description;

        public NegationTestCase(String query, NegationQueryHandler.NegationType expectedType,
                                List<String> expectedKeywords, String description) {
            this.query = query;
            this.expectedType = expectedType;
            this.expectedKeywords = expectedKeywords;
            this.description = description;
        }
    }

    @Data
    static class TestResult {
        private int totalCases;
        private int passedCases;
        private int failedCases;
        private double accuracy;
        private List<String> failedDetails = new ArrayList<>();
    }

    /**
     * 主测试方法
     */
    @Test
    public void runNegationQueryTest() {
        log.info("========== 开始否定查询处理增强测试 ==========");

        // 1. 准备测试用例
        List<NegationTestCase> testCases = prepareTestCases();
        log.info("测试用例数量: {}", testCases.size());

        // 2. 执行测试
        TestResult result = executeTests(testCases);

        // 3. 输出结果
        printResult(result);
    }

    /**
     * 准备测试用例（覆盖多种否定模式）
     */
    private List<NegationTestCase> prepareTestCases() {
        List<NegationTestCase> cases = new ArrayList<>();

        // 类型1：否定疑问句（不是...吗、不能...吗）
        cases.add(new NegationTestCase(
                "北京出差不能住五星级酒店吗",
                NegationQueryHandler.NegationType.QUESTION,
                List.of("北京", "住宿", "五星级", "是否允许"),
                "否定疑问句-不能...吗"
        ));

        cases.add(new NegationTestCase(
                "去二线城市不是500元住宿标准吗",
                NegationQueryHandler.NegationType.QUESTION,
                List.of("二线城市", "住宿", "标准"),
                "否定疑问句-不是...吗"
        ));

        cases.add(new NegationTestCase(
                "上海出差住宿标准不是350元吗",
                NegationQueryHandler.NegationType.QUESTION,
                List.of("上海", "住宿", "标准"),
                "否定疑问句-数值纠正"
        ));

        cases.add(new NegationTestCase(
                "打车报销后还不能领交通补助吗",
                NegationQueryHandler.NegationType.QUESTION,
                List.of("打车", "交通补助", "是否允许"),
                "否定疑问句-复杂条件"
        ));

        // 类型2：直接否定（不能、不可以、禁止）
        cases.add(new NegationTestCase(
                "出差不能坐商务舱对吗",
                NegationQueryHandler.NegationType.DIRECT,
                List.of("出差", "商务舱", "是否允许"),
                "直接否定-不能"
        ));

        cases.add(new NegationTestCase(
                "员工不可以住五星级酒店",
                NegationQueryHandler.NegationType.DIRECT,
                List.of("员工", "五星级", "是否允许"),
                "直接否定-不可以"
        ));

        cases.add(new NegationTestCase(
                "公司禁止报销出租车费用",
                NegationQueryHandler.NegationType.DIRECT,
                List.of("出租车", "费用", "是否允许"),
                "直接否定-禁止"
        ));

        cases.add(new NegationTestCase(
                "差旅不允许超标准住宿",
                NegationQueryHandler.NegationType.DIRECT,
                List.of("差旅", "住宿", "标准"),
                "直接否定-不允许"
        ));

        // 类型3：否定判断（不是、不对）
        cases.add(new NegationTestCase(
                "这个标准不对吧",
                NegationQueryHandler.NegationType.JUDGMENT,
                List.of("标准"),
                "否定判断-不对"
        ));

        cases.add(new NegationTestCase(
                "报销金额不正确",
                NegationQueryHandler.NegationType.JUDGMENT,
                List.of("报销", "金额"),
                "否定判断-不正确"
        ));

        // 类型4：缺失否定（没有、无）
        cases.add(new NegationTestCase(
                "这个城市没有住宿标准吗",
                NegationQueryHandler.NegationType.ABSENCE,
                List.of("城市", "住宿", "标准"),
                "缺失否定-没有"
        ));

        cases.add(new NegationTestCase(
                "无交通补助",
                NegationQueryHandler.NegationType.ABSENCE,
                List.of("交通补助"),
                "缺失否定-无"
        ));

        // 类型5：非否定查询（对照组）
        cases.add(new NegationTestCase(
                "北京出差住宿标准",
                NegationQueryHandler.NegationType.NONE,
                List.of("北京", "住宿", "标准"),
                "非否定查询-正常查询"
        ));

        cases.add(new NegationTestCase(
                "去上海出差可以住什么酒店",
                NegationQueryHandler.NegationType.NONE,
                List.of("上海", "出差", "酒店"),
                "非否定查询-肯定疑问"
        ));

        return cases;
    }

    /**
     * 执行测试
     */
    private TestResult executeTests(List<NegationTestCase> testCases) {
        TestResult result = new TestResult();
        result.setTotalCases(testCases.size());

        int passed = 0;
        int failed = 0;

        for (NegationTestCase testCase : testCases) {
            log.info("\n========== 测试：{} ==========", testCase.getDescription());
            log.info("查询: {}", testCase.getQuery());
            log.info("期望类型: {}", testCase.getExpectedType());

            try {
                // 1. 检测否定类型
                NegationQueryHandler.NegationType detectedType = negationHandler.detectNegationType(testCase.getQuery());
                log.info("检测类型: {}", detectedType);

                // 2. 处理否定查询
                String handled = negationHandler.handleNegationQuery(testCase.getQuery());
                log.info("处理结果: {}", handled);

                // 3. 验证结果
                boolean typeMatch = detectedType == testCase.getExpectedType();
                boolean keywordsMatch = validateKeywords(handled, testCase.getExpectedKeywords());

                if (typeMatch && keywordsMatch) {
                    passed++;
                    log.info("✓ 测试通过");
                } else {
                    failed++;
                    String failMsg = String.format("[%s] %s -> 类型匹配:%s, 关键词匹配:%s",
                            testCase.getDescription(), testCase.getQuery(), typeMatch, keywordsMatch);
                    result.getFailedDetails().add(failMsg);
                    log.warn("✗ 测试失败: {}", failMsg);
                }

            } catch (Exception e) {
                failed++;
                String failMsg = String.format("[%s] %s -> 异常: %s",
                        testCase.getDescription(), testCase.getQuery(), e.getMessage());
                result.getFailedDetails().add(failMsg);
                log.error("✗ 测试失败: {}", failMsg);
            }
        }

        result.setPassedCases(passed);
        result.setFailedCases(failed);
        result.setAccuracy((double) passed / testCases.size() * 100);

        return result;
    }

    /**
     * 验证关键词
     */
    private boolean validateKeywords(String handled, List<String> expectedKeywords) {
        int matchCount = 0;
        for (String keyword : expectedKeywords) {
            if (handled.contains(keyword)) {
                matchCount++;
            }
        }

        // 至少匹配50%的关键词
        double matchRate = (double) matchCount / expectedKeywords.size();
        return matchRate >= 0.5;
    }

    /**
     * 输出测试结果
     */
    private void printResult(TestResult result) {
        log.info("\n========== 否定查询处理测试结果 ==========");
        log.info("总测试用例: {}", result.getTotalCases());
        log.info("通过用例: {}", result.getPassedCases());
        log.info("失败用例: {}", result.getFailedCases());
        log.info("准确率: {:.2f}%", result.getAccuracy());

        if (!result.getFailedDetails().isEmpty()) {
            log.info("\n失败详情:");
            for (String detail : result.getFailedDetails()) {
                log.info("  - {}", detail);
            }
        }

        log.info("\n========== 测试完成 ==========");
    }
}
