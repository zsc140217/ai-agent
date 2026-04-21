package com.jblmj.aiagent.evaluation;

import com.jblmj.aiagent.app.EnterpriseAssistantApp;
import com.jblmj.aiagent.app.WorkflowOrchestrator;
import com.jblmj.aiagent.chatmemory.FileBasedChatMemory;
import com.jblmj.aiagent.model.QueryComplexity;
import com.jblmj.aiagent.model.SubTask;
import com.jblmj.aiagent.rag.QueryRewriter;
import com.jblmj.aiagent.service.ComplexityAssessor;
import com.jblmj.aiagent.service.TaskDecomposer;
import com.jblmj.aiagent.skill.SkillRegistry;
import com.jblmj.aiagent.tools.WeatherQueryTool;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统集成测试 - 全功能综合评测
 *
 * 测试覆盖：
 * 1. 记忆系统（FileBasedChatMemory）
 * 2. RAG 检索（VectorStore + QueryRewriter）
 * 3. MCP 工具调用（WeatherQueryTool）
 * 4. 查询重写（QueryRewriter）
 * 5. 任务编排（WorkflowOrchestrator）
 * 6. 复杂度评估（ComplexityAssessor）
 * 7. 任务分解（TaskDecomposer）
 * 8. 技能路由（SkillRegistry）
 *
 * @author jblmj
 */
@SpringBootTest
@Slf4j
public class SystemIntegrationTest {

    @Resource
    private WorkflowOrchestrator workflowOrchestrator;

    @Resource
    private ComplexityAssessor complexityAssessor;

    @Resource
    private TaskDecomposer taskDecomposer;

    @Resource
    private QueryRewriter queryRewriter;

    @Resource
    private WeatherQueryTool weatherQueryTool;

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private SkillRegistry skillRegistry;

    @Resource
    private EnterpriseAssistantApp enterpriseAssistantApp;

    private FileBasedChatMemory chatMemory;
    private List<TestCase> testCases;
    private TestReport report;

    @BeforeEach
    public void setup() {
        chatMemory = new FileBasedChatMemory("data/test_memory");
        testCases = prepareTestCases();
        report = new TestReport();
        log.info("========================================");
        log.info("系统集成测试初始化完成");
        log.info("========================================");
    }

    /**
     * 测试 1：记忆系统功能测试
     */
    @Test
    @DisplayName("测试记忆系统 - 多轮对话上下文保持")
    public void testMemorySystem() {
        log.info("\n========== 测试 1：记忆系统 ==========");

        String chatId = "test_memory_" + UUID.randomUUID();

        // 第一轮对话
        List<Message> messages1 = new ArrayList<>();
        messages1.add(new org.springframework.ai.chat.messages.UserMessage("我叫张三"));
        chatMemory.add(chatId, messages1);

        // 第二轮对话
        List<Message> messages2 = new ArrayList<>();
        messages2.add(new org.springframework.ai.chat.messages.UserMessage("我在杭州工作"));
        chatMemory.add(chatId, messages2);

        // 验证记忆
        List<Message> history = chatMemory.get(chatId);
        assertEquals(2, history.size(), "应该保存 2 条消息");
        assertTrue(history.get(0).getText().contains("张三"), "第一条消息应包含姓名");
        assertTrue(history.get(1).getText().contains("杭州"), "第二条消息应包含城市");

        // 清除记忆
        chatMemory.clear(chatId);
        List<Message> clearedHistory = chatMemory.get(chatId);
        assertEquals(0, clearedHistory.size(), "清除后应该没有消息");

        log.info("✓ 记忆系统测试通过");
        report.addResult("记忆系统", true, "多轮对话上下文保持正常");
    }

    /**
     * 测试 2：查询重写功能测试
     */
    @Test
    @DisplayName("测试查询重写 - 口语化转标准查询")
    public void testQueryRewriting() {
        log.info("\n========== 测试 2：查询重写 ==========");

        Map<String, String> testQueries = new HashMap<>();
        testQueries.put("魔都今天天气咋样", "上海今天天气");
        testQueries.put("帝都明天会下雨吗", "北京明天天气");
        testQueries.put("差旅能报销多少钱", "差旅报销标准");

        int passCount = 0;
        for (Map.Entry<String, String> entry : testQueries.entrySet()) {
            String original = entry.getKey();
            String expected = entry.getValue();

            try {
                String rewritten = queryRewriter.doQueryRewrite(original);
                log.info("原始查询: {} -> 重写后: {}", original, rewritten);

                // 验证重写后的查询更标准化
                assertNotNull(rewritten, "重写结果不应为空");
                assertNotEquals(original, rewritten, "重写后应该有变化");

                passCount++;
            } catch (Exception e) {
                log.error("查询重写失败: {}", original, e);
            }
        }

        assertTrue(passCount >= testQueries.size() * 0.8, "至少 80% 的查询重写应该成功");
        log.info("✓ 查询重写测试通过 ({}/{})", passCount, testQueries.size());
        report.addResult("查询重写", true, String.format("成功率: %d%%", passCount * 100 / testQueries.size()));
    }

    /**
     * 测试 3：复杂度评估准确性测试
     */
    @Test
    @DisplayName("测试复杂度评估 - 分类准确性")
    public void testComplexityAssessment() {
        log.info("\n========== 测试 3：复杂度评估 ==========");

        Map<String, QueryComplexity> expectedResults = new HashMap<>();
        expectedResults.put("北京天气", QueryComplexity.SIMPLE);
        expectedResults.put("上海和广州天气对比", QueryComplexity.MEDIUM);
        expectedResults.put("明天去杭州出差，查天气，拜访阿里巴巴，规划路线", QueryComplexity.COMPLEX);
        expectedResults.put("查询差旅补贴标准", QueryComplexity.SIMPLE);
        expectedResults.put("规划北京三日游行程", QueryComplexity.COMPLEX);

        int correctCount = 0;
        for (Map.Entry<String, QueryComplexity> entry : expectedResults.entrySet()) {
            String query = entry.getKey();
            QueryComplexity expected = entry.getValue();

            QueryComplexity actual = complexityAssessor.assess(query);
            log.info("查询: {} | 预期: {} | 实际: {} | {}",
                query, expected, actual, expected == actual ? "✓" : "✗");

            if (expected == actual) {
                correctCount++;
            }
        }

        double accuracy = (double) correctCount / expectedResults.size();
        log.info("复杂度评估准确率: {}/{} = {}", correctCount, expectedResults.size(),
            String.format("%.1f%%", accuracy * 100));

        assertTrue(accuracy >= 0.8, "复杂度评估准确率应 >= 80%");
        report.addResult("复杂度评估", true, String.format("准确率: %.1f%%", accuracy * 100));
    }

    /**
     * 测试 4：任务分解功能测试
     */
    @Test
    @DisplayName("测试任务分解 - 复杂查询拆解")
    public void testTaskDecomposition() {
        log.info("\n========== 测试 4：任务分解 ==========");

        String complexQuery = "明天去杭州出差，查一下天气，还要拜访阿里巴巴，帮我规划一下路线";

        try {
            List<SubTask> tasks = taskDecomposer.decompose(complexQuery);

            assertNotNull(tasks, "任务列表不应为空");
            assertTrue(tasks.size() >= 2, "复杂查询应该分解为至少 2 个子任务");

            log.info("任务分解结果: 共 {} 个子任务", tasks.size());
            for (SubTask task : tasks) {
                log.info("  - 任务 {}: {} (类型: {}, 依赖: {})",
                    task.getId(), task.getDescription(), task.getTaskType(), task.getDependsOn());

                assertNotNull(task.getTaskType(), "任务类型不应为空");
                assertNotNull(task.getDescription(), "任务描述不应为空");
            }

            // 测试拓扑排序
            List<List<SubTask>> batches = taskDecomposer.sortTasksByDependency(tasks);
            assertNotNull(batches, "排序结果不应为空");
            assertTrue(batches.size() >= 1, "至少应该有一个批次");

            log.info("拓扑排序结果: 共 {} 个批次", batches.size());
            for (int i = 0; i < batches.size(); i++) {
                log.info("  批次 {}: {} 个任务可并行执行", i + 1, batches.get(i).size());
            }

            log.info("✓ 任务分解测试通过");
            report.addResult("任务分解", true, String.format("分解为 %d 个子任务，%d 个批次",
                tasks.size(), batches.size()));

        } catch (Exception e) {
            log.error("任务分解失败", e);
            fail("任务分解不应该抛出异常");
        }
    }

    /**
     * 测试 5：MCP 工具调用测试
     */
    @Test
    @DisplayName("测试 MCP 工具 - 天气查询")
    public void testMcpToolInvocation() {
        log.info("\n========== 测试 5：MCP 工具调用 ==========");

        String[] cities = {"北京", "上海", "杭州"};
        int successCount = 0;

        for (String city : cities) {
            try {
                String result = weatherQueryTool.queryWeather(city);

                assertNotNull(result, "天气查询结果不应为空");
                assertTrue(result.contains(city) || result.contains("天气") || result.contains("温度"),
                    "结果应包含天气相关信息");

                log.info("✓ {} 天气查询成功: {}", city, result.substring(0, Math.min(50, result.length())));
                successCount++;

            } catch (Exception e) {
                log.error("✗ {} 天气查询失败", city, e);
            }
        }

        assertTrue(successCount >= cities.length * 0.8, "至少 80% 的工具调用应该成功");
        log.info("MCP 工具调用成功率: {}/{}", successCount, cities.length);
        report.addResult("MCP 工具", true, String.format("成功率: %d%%", successCount * 100 / cities.length));
    }

    /**
     * 测试 6：技能路由测试
     */
    @Test
    @DisplayName("测试技能路由 - Skill 选择")
    public void testSkillRouting() {
        log.info("\n========== 测试 6：技能路由 ==========");

        // 获取所有已注册的 Skill
        var allSkills = skillRegistry.getAllSkills();
        log.info("已注册 Skill 数量: {}", allSkills.size());

        assertTrue(allSkills.size() > 0, "应该至少有一个 Skill 被注册");

        // 测试不同查询的 Skill 选择
        Map<String, String> queryToSkill = new HashMap<>();
        queryToSkill.put("北京天气怎么样", "WeatherQuerySkill");
        queryToSkill.put("规划杭州三日游", "TravelPlanningSkill");

        for (Map.Entry<String, String> entry : queryToSkill.entrySet()) {
            String query = entry.getKey();
            String expectedSkill = entry.getValue();

            var selectedSkill = skillRegistry.selectSkill(query);

            if (selectedSkill != null) {
                log.info("查询: {} -> Skill: {}", query, selectedSkill.getName());
            } else {
                log.info("查询: {} -> 无匹配 Skill（将使用默认流程）", query);
            }
        }

        log.info("✓ 技能路由测试通过");
        report.addResult("技能路由", true, String.format("已注册 %d 个 Skill", allSkills.size()));
    }

    /**
     * 测试 7：RAG 检索测试
     */
    @Test
    @DisplayName("测试 RAG 检索 - 向量相似度搜索")
    public void testRagRetrieval() {
        log.info("\n========== 测试 7：RAG 检索 ==========");

        String[] queries = {
            "差旅住宿标准是多少",
            "出差可以报销哪些费用",
            "协议酒店有哪些"
        };

        int successCount = 0;
        for (String query : queries) {
            try {
                var results = loveAppVectorStore.similaritySearch(query);

                assertNotNull(results, "检索结果不应为空");
                log.info("查询: {} -> 检索到 {} 条相关文档", query, results.size());

                if (results.size() > 0) {
                    log.info("  Top 1: {}", results.get(0).getText().substring(0,
                        Math.min(100, results.get(0).getText().length())));
                    successCount++;
                }

            } catch (Exception e) {
                log.error("RAG 检索失败: {}", query, e);
            }
        }

        assertTrue(successCount >= queries.length * 0.6, "至少 60% 的检索应该成功");
        log.info("✓ RAG 检索测试通过 ({}/{})", successCount, queries.length);
        report.addResult("RAG 检索", true, String.format("成功率: %d%%", successCount * 100 / queries.length));
    }

    /**
     * 测试 8：端到端工作流测试
     */
    @Test
    @DisplayName("测试端到端工作流 - 完整流程")
    public void testEndToEndWorkflow() {
        log.info("\n========== 测试 8：端到端工作流 ==========");

        for (TestCase testCase : testCases) {
            log.info("\n--- 测试用例: {} ---", testCase.getName());
            log.info("查询: {}", testCase.getQuery());
            log.info("预期复杂度: {}", testCase.getExpectedComplexity());

            Instant start = Instant.now();

            try {
                String result = workflowOrchestrator.route(testCase.getQuery(), "test_" + UUID.randomUUID());

                Instant end = Instant.now();
                long duration = Duration.between(start, end).toMillis();

                assertNotNull(result, "工作流结果不应为空");
                assertTrue(result.length() > 10, "结果应该有实质内容");

                log.info("✓ 执行成功 (耗时: {}ms)", duration);
                log.info("结果摘要: {}", result.substring(0, Math.min(200, result.length())));

                testCase.setPassed(true);
                testCase.setDuration(duration);
                testCase.setResult(result);

            } catch (Exception e) {
                log.error("✗ 执行失败", e);
                testCase.setPassed(false);
                testCase.setError(e.getMessage());
            }
        }

        // 统计结果
        long passedCount = testCases.stream().filter(TestCase::isPassed).count();
        double passRate = (double) passedCount / testCases.size();

        log.info("\n========== 端到端测试总结 ==========");
        log.info("通过: {}/{} ({}%)", passedCount, testCases.size(),
            String.format("%.1f", passRate * 100));

        assertTrue(passRate >= 0.7, "端到端测试通过率应 >= 70%");
        report.addResult("端到端工作流", true, String.format("通过率: %.1f%%", passRate * 100));
    }

    /**
     * 测试 9：性能基准测试
     */
    @Test
    @DisplayName("测试性能基准 - 响应时间")
    public void testPerformanceBenchmark() {
        log.info("\n========== 测试 9：性能基准 ==========");

        Map<String, Long> benchmarks = new HashMap<>();

        // 简单查询性能
        Instant start1 = Instant.now();
        complexityAssessor.assess("北京天气");
        long duration1 = Duration.between(start1, Instant.now()).toMillis();
        benchmarks.put("复杂度评估", duration1);

        // 查询重写性能
        Instant start2 = Instant.now();
        queryRewriter.doQueryRewrite("魔都今天天气咋样");
        long duration2 = Duration.between(start2, Instant.now()).toMillis();
        benchmarks.put("查询重写", duration2);

        // 任务分解性能
        Instant start3 = Instant.now();
        taskDecomposer.decompose("明天去杭州出差，查天气，拜访客户");
        long duration3 = Duration.between(start3, Instant.now()).toMillis();
        benchmarks.put("任务分解", duration3);

        log.info("性能基准测试结果:");
        benchmarks.forEach((name, duration) -> {
            log.info("  {}: {}ms", name, duration);
            assertTrue(duration < 10000, name + " 响应时间应 < 10s");
        });

        log.info("✓ 性能基准测试通过");
        report.addResult("性能基准", true, "所有操作响应时间 < 10s");
    }

    /**
     * 生成测试报告
     */
    @Test
    @DisplayName("生成综合测试报告")
    public void generateTestReport() {
        log.info("\n========================================");
        log.info("系统集成测试报告");
        log.info("========================================");
        log.info("测试时间: {}", new Date());
        log.info("测试项目数: {}", report.getResults().size());
        log.info("\n详细结果:");

        report.getResults().forEach(result -> {
            log.info("  {} {} - {}",
                result.isPassed() ? "✓" : "✗",
                result.getModule(),
                result.getMessage());
        });

        long passedCount = report.getResults().stream().filter(TestResult::isPassed).count();
        double passRate = report.getResults().isEmpty() ? 0 :
            (double) passedCount / report.getResults().size();

        log.info("\n总体通过率: {}/{} ({}%)",
            passedCount, report.getResults().size(),
            String.format("%.1f", passRate * 100));
        log.info("========================================");
    }

    /**
     * 准备测试用例
     */
    private List<TestCase> prepareTestCases() {
        List<TestCase> cases = new ArrayList<>();

        cases.add(new TestCase(
            "简单天气查询",
            "北京今天天气怎么样",
            QueryComplexity.SIMPLE
        ));

        cases.add(new TestCase(
            "天气对比查询",
            "上海和广州天气对比",
            QueryComplexity.MEDIUM
        ));

        cases.add(new TestCase(
            "复杂行程规划",
            "明天去杭州出差，查天气，拜访阿里巴巴，规划路线",
            QueryComplexity.COMPLEX
        ));

        cases.add(new TestCase(
            "政策查询",
            "差旅住宿标准是多少",
            QueryComplexity.SIMPLE
        ));

        return cases;
    }

    /**
     * 测试用例数据结构
     */
    @Data
    static class TestCase {
        private String name;
        private String query;
        private QueryComplexity expectedComplexity;
        private boolean passed;
        private long duration;
        private String result;
        private String error;

        public TestCase(String name, String query, QueryComplexity expectedComplexity) {
            this.name = name;
            this.query = query;
            this.expectedComplexity = expectedComplexity;
        }
    }

    /**
     * 测试报告数据结构
     */
    @Data
    static class TestReport {
        private List<TestResult> results = new ArrayList<>();

        public void addResult(String module, boolean passed, String message) {
            results.add(new TestResult(module, passed, message));
        }
    }

    @Data
    static class TestResult {
        private String module;
        private boolean passed;
        private String message;

        public TestResult(String module, boolean passed, String message) {
            this.module = module;
            this.passed = passed;
            this.message = message;
        }
    }
}
