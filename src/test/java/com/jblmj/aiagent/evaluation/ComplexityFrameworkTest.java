package com.jblmj.aiagent.evaluation;

import com.jblmj.aiagent.app.WorkflowOrchestrator;
import com.jblmj.aiagent.model.QueryComplexity;
import com.jblmj.aiagent.service.ComplexityAssessor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 复杂度评估框架完整测试
 *
 * 测试目标：
 * 1. 验证复杂度评估的准确性
 * 2. 验证不同复杂度的路由策略
 * 3. 验证端到端的查询处理流程
 *
 * 测试用例设计：
 * - SIMPLE: 5 条（单一意图，单次工具调用）
 * - MEDIUM: 5 条（单一意图，多次工具调用）
 * - COMPLEX: 5 条（多意图，需要任务分解）
 *
 * @author jblmj
 */
@Slf4j
@SpringBootTest
public class ComplexityFrameworkTest {

    @Resource
    private ComplexityAssessor complexityAssessor;

    @Resource
    private WorkflowOrchestrator workflowOrchestrator;

    /**
     * 测试用例
     */
    static class TestCase {
        String query;
        QueryComplexity expectedComplexity;
        String description;

        TestCase(String query, QueryComplexity expectedComplexity, String description) {
            this.query = query;
            this.expectedComplexity = expectedComplexity;
            this.description = description;
        }
    }

    @Test
    public void testComplexityAssessment() {
        log.info("========================================");
        log.info("开始测试：复杂度评估准确性");
        log.info("========================================");

        List<TestCase> testCases = createTestCases();

        int correct = 0;
        int total = testCases.size();

        for (int i = 0; i < testCases.size(); i++) {
            TestCase testCase = testCases.get(i);
            log.info("\n【测试用例 {}】", i + 1);
            log.info("查询: {}", testCase.query);
            log.info("描述: {}", testCase.description);
            log.info("期望复杂度: {}", testCase.expectedComplexity);

            QueryComplexity actualComplexity = complexityAssessor.assess(testCase.query);
            log.info("实际复杂度: {}", actualComplexity);

            boolean isCorrect = actualComplexity == testCase.expectedComplexity;
            log.info("判断结果: {}", isCorrect ? "✓ 正确" : "✗ 错误");

            if (isCorrect) {
                correct++;
            }
        }

        log.info("\n========================================");
        log.info("复杂度评估准确率: {}/{} = {}", correct, total,
                String.format("%.1f%%", (correct * 100.0 / total)));
        log.info("========================================");
    }

    @Test
    public void testEndToEndWorkflow() {
        log.info("========================================");
        log.info("开始测试：端到端工作流");
        log.info("========================================");

        // 选择 3 个代表性用例
        String[] queries = {
                "北京今天天气怎么样",                           // SIMPLE
                "上海和广州哪个天气更好",                       // MEDIUM
                "我要去深圳拜访客户，帮我查天气和推荐酒店"      // COMPLEX
        };

        for (int i = 0; i < queries.length; i++) {
            String query = queries[i];
            log.info("\n【端到端测试 {}】", i + 1);
            log.info("查询: {}", query);

            try {
                long startTime = System.currentTimeMillis();
                String response = workflowOrchestrator.route(query, "test-chat-" + i);
                long endTime = System.currentTimeMillis();

                log.info("响应: {}", response);
                log.info("耗时: {} ms", endTime - startTime);
                log.info("状态: ✓ 成功");

            } catch (Exception e) {
                log.error("状态: ✗ 失败", e);
            }
        }

        log.info("\n========================================");
        log.info("端到端测试完成");
        log.info("========================================");
    }

    /**
     * 创建测试用例
     */
    private List<TestCase> createTestCases() {
        List<TestCase> cases = new ArrayList<>();

        // ========== SIMPLE 用例（5 条）==========
        cases.add(new TestCase(
                "北京今天天气怎么样",
                QueryComplexity.SIMPLE,
                "单一意图：查询天气"
        ));

        cases.add(new TestCase(
                "杭州需要带伞吗",
                QueryComplexity.SIMPLE,
                "单一意图：查询天气（隐含）"
        ));

        cases.add(new TestCase(
                "深圳现在温度多少",
                QueryComplexity.SIMPLE,
                "单一意图：查询温度"
        ));

        cases.add(new TestCase(
                "阿里巴巴的客户地址在哪里",
                QueryComplexity.SIMPLE,
                "单一意图：查询客户信息"
        ));

        cases.add(new TestCase(
                "住宿补贴标准是多少",
                QueryComplexity.SIMPLE,
                "单一意图：查询政策"
        ));

        // ========== MEDIUM 用例（5 条）==========
        cases.add(new TestCase(
                "上海和广州哪个天气更好",
                QueryComplexity.MEDIUM,
                "单一意图，多次工具调用：对比两个城市天气"
        ));

        cases.add(new TestCase(
                "北京、上海、深圳三个城市的天气对比",
                QueryComplexity.MEDIUM,
                "单一意图，多次工具调用：对比三个城市天气"
        ));

        cases.add(new TestCase(
                "查询阿里巴巴和腾讯的客户信息",
                QueryComplexity.MEDIUM,
                "单一意图，多次查询：查询两个客户"
        ));

        cases.add(new TestCase(
                "一级城市和二级城市的住宿补贴分别是多少",
                QueryComplexity.MEDIUM,
                "单一意图，多次查询：对比两种补贴标准"
        ));

        cases.add(new TestCase(
                "杭州和成都的交通补贴对比",
                QueryComplexity.MEDIUM,
                "单一意图，多次查询：对比两个城市的补贴"
        ));

        // ========== COMPLEX 用例（5 条）==========
        cases.add(new TestCase(
                "我要去深圳拜访客户，帮我查天气和推荐酒店",
                QueryComplexity.COMPLEX,
                "多意图：查天气 + 查客户 + 推荐酒店"
        ));

        cases.add(new TestCase(
                "帮我规划去杭州的出差，包括天气、路线和住宿安排",
                QueryComplexity.COMPLEX,
                "多意图：查天气 + 查路线 + 查住宿政策"
        ));

        cases.add(new TestCase(
                "我要拜访阿里巴巴，查一下天气、客户地址和附近酒店",
                QueryComplexity.COMPLEX,
                "多意图：查天气 + 查客户 + 推荐酒店"
        ));

        cases.add(new TestCase(
                "规划一次北京到上海的出差，包括交通方式、住宿补贴和天气情况",
                QueryComplexity.COMPLEX,
                "多意图：查交通 + 查补贴 + 查天气"
        ));

        cases.add(new TestCase(
                "我要去广州和深圳两个城市出差，帮我对比天气、查询客户信息和推荐酒店",
                QueryComplexity.COMPLEX,
                "多意图 + 多城市：查天气 + 查客户 + 推荐酒店"
        ));

        return cases;
    }
}
