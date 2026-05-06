package com.jblmj.aiagent.evaluation;

import com.jblmj.aiagent.rag.EnterpriseQueryRewriter;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询改写效果评测
 *
 * 评测目标：
 * 1. 验证Few-shot示例扩展后的改写质量
 * 2. 测试各种场景的改写效果（口语化、否定、对比、数值计算等）
 * 3. 评估关键词保留率和改写稳定性
 *
 * 面试价值：
 * - 展示如何评估Prompt工程效果
 * - 体现数据驱动的优化思路
 * - 证明对RAG检索质量的关注
 */
@SpringBootTest
@Slf4j
public class QueryRewriteEvaluationTest {

    @Resource
    private EnterpriseQueryRewriter enterpriseQueryRewriter;

    @Data
    static class RewriteTestCase {
        private String category;
        private String originalQuery;
        private List<String> expectedKeywords;
        private List<String> shouldNotContain;

        public RewriteTestCase(String category, String originalQuery,
                               List<String> expectedKeywords, List<String> shouldNotContain) {
            this.category = category;
            this.originalQuery = originalQuery;
            this.expectedKeywords = expectedKeywords;
            this.shouldNotContain = shouldNotContain;
        }
    }

    @Data
    static class RewriteResult {
        private int totalCases;
        private int passedCases;
        private int failedCases;
        private double accuracy;
        private List<String> failedDetails = new ArrayList<>();
    }

    /**
     * 主评测方法
     */
    @Test
    public void runQueryRewriteEvaluation() {
        log.info("========== 开始查询改写效果评测 ==========");

        // 1. 准备测试用例
        List<RewriteTestCase> testCases = prepareTestCases();
        log.info("测试用例数量: {}", testCases.size());

        // 2. 执行改写评测
        RewriteResult result = evaluateRewrite(testCases);

        // 3. 输出评测结果
        printResult(result);
    }

    /**
     * 准备测试用例（覆盖15种场景）
     */
    private List<RewriteTestCase> prepareTestCases() {
        List<RewriteTestCase> cases = new ArrayList<>();

        // 场景1：口语化查询
        cases.add(new RewriteTestCase(
            "口语化",
            "去魔都出差住宿能报多少",
            List.of("上海", "一类城市", "住宿", "标准"),
            List.of("魔都")
        ));

        // 场景2：城市缩写
        cases.add(new RewriteTestCase(
            "缩写识别",
            "去BJ出差住宿能报多少",
            List.of("北京", "一类城市", "住宿", "标准"),
            List.of("BJ")
        ));

        // 场景3：多语言混合
        cases.add(new RewriteTestCase(
            "多语言",
            "去Shanghai出差住宿标准",
            List.of("上海", "一类城市", "住宿", "标准"),
            List.of("Shanghai")
        ));

        // 场景4：否定查询
        cases.add(new RewriteTestCase(
            "否定查询",
            "北京出差不能住五星级酒店吗",
            List.of("北京", "住宿", "五星级", "不能"),
            List.of()
        ));

        // 场景5：省会城市映射
        cases.add(new RewriteTestCase(
            "省会映射",
            "去省会城市出差住宿能报多少",
            List.of("二类城市", "住宿", "标准"),
            List.of()
        ));

        // 场景6：对比查询
        cases.add(new RewriteTestCase(
            "对比查询",
            "北京和上海的住宿标准哪个高",
            List.of("北京", "上海", "住宿", "标准", "对比"),
            List.of()
        ));

        // 场景7：数值计算
        cases.add(new RewriteTestCase(
            "数值计算",
            "出差30天伙食补助总共多少",
            List.of("伙食", "补助", "30天", "总计"),
            List.of()
        ));

        // 场景8：综合查询
        cases.add(new RewriteTestCase(
            "综合查询",
            "明天去上海出差，住宿和伙食一共能报多少",
            List.of("上海", "住宿", "伙食", "总计"),
            List.of()
        ));

        // 场景9：条件查询
        cases.add(new RewriteTestCase(
            "条件查询",
            "如果打车了还能领交通补助吗",
            List.of("打车", "交通补助", "关系"),
            List.of()
        ));

        // 场景10：交通方式
        cases.add(new RewriteTestCase(
            "交通查询",
            "出差坐高铁可以报销吗",
            List.of("高铁", "交通", "报销", "标准"),
            List.of()
        ));

        // 场景11：客户信息
        cases.add(new RewriteTestCase(
            "客户查询",
            "去杭州拜访某某科技公司的联系人是谁",
            List.of("杭州", "某某科技", "客户", "联系人"),
            List.of()
        ));

        // 场景12：多意图
        cases.add(new RewriteTestCase(
            "多意图",
            "去杭州拜访客户，住宿标准和客户地址",
            List.of("杭州", "住宿", "标准", "客户", "地址"),
            List.of()
        ));

        // 场景13：简略查询
        cases.add(new RewriteTestCase(
            "简略查询",
            "北京住宿标准",
            List.of("北京", "一类城市", "住宿", "标准"),
            List.of()
        ));

        // 场景14：补贴查询
        cases.add(new RewriteTestCase(
            "补贴查询",
            "出差每天伙食补助多少",
            List.of("伙食", "补助", "标准", "每日"),
            List.of()
        ));

        // 场景15：飞机条件
        cases.add(new RewriteTestCase(
            "条件查询",
            "什么情况下可以坐飞机",
            List.of("飞机", "交通", "条件", "标准"),
            List.of()
        ));

        return cases;
    }

    /**
     * 执行改写评测
     */
    private RewriteResult evaluateRewrite(List<RewriteTestCase> testCases) {
        RewriteResult result = new RewriteResult();
        result.setTotalCases(testCases.size());

        int passed = 0;
        int failed = 0;

        for (RewriteTestCase testCase : testCases) {
            log.info("\n========== 测试场景：{} ==========", testCase.getCategory());
            log.info("原始查询: {}", testCase.getOriginalQuery());

            try {
                // 执行改写
                String rewritten = enterpriseQueryRewriter.rewrite(testCase.getOriginalQuery());
                log.info("改写结果: {}", rewritten);

                // 验证改写质量
                boolean isValid = validateRewrite(testCase, rewritten);

                if (isValid) {
                    passed++;
                    log.info("✓ 测试通过");
                } else {
                    failed++;
                    String failMsg = String.format("[%s] %s -> %s (缺少关键词或包含不应有的词)",
                        testCase.getCategory(), testCase.getOriginalQuery(), rewritten);
                    result.getFailedDetails().add(failMsg);
                    log.warn("✗ 测试失败: {}", failMsg);
                }

            } catch (Exception e) {
                failed++;
                String failMsg = String.format("[%s] %s -> 改写异常: %s",
                    testCase.getCategory(), testCase.getOriginalQuery(), e.getMessage());
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
     * 验证改写质量
     */
    private boolean validateRewrite(RewriteTestCase testCase, String rewritten) {
        // 1. 检查期望关键词
        int matchedKeywords = 0;
        for (String keyword : testCase.getExpectedKeywords()) {
            if (rewritten.contains(keyword)) {
                matchedKeywords++;
            } else {
                log.debug("缺少关键词: {}", keyword);
            }
        }

        // 至少匹配70%的关键词
        double keywordMatchRate = (double) matchedKeywords / testCase.getExpectedKeywords().size();
        if (keywordMatchRate < 0.7) {
            log.warn("关键词匹配率过低: {}/{} = {}",
                matchedKeywords, testCase.getExpectedKeywords().size(), keywordMatchRate);
            return false;
        }

        // 2. 检查不应包含的词
        for (String forbidden : testCase.getShouldNotContain()) {
            if (rewritten.contains(forbidden)) {
                log.warn("包含不应有的词: {}", forbidden);
                return false;
            }
        }

        // 3. 基本质量检查
        if (rewritten.isEmpty() || rewritten.length() < 5) {
            log.warn("改写结果过短");
            return false;
        }

        if (rewritten.length() > 100) {
            log.warn("改写结果过长");
            return false;
        }

        return true;
    }

    /**
     * 输出评测结果
     */
    private void printResult(RewriteResult result) {
        log.info("\n========== 查询改写评测结果 ==========");
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

        log.info("\n========== 评测完成 ==========");
    }
}
