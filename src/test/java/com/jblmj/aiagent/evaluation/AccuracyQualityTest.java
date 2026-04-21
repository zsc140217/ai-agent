package com.jblmj.aiagent.evaluation;

import com.jblmj.aiagent.app.EnterpriseAssistantApp;
import com.jblmj.aiagent.app.WorkflowOrchestrator;
import com.jblmj.aiagent.chatmemory.FileBasedChatMemory;
import com.jblmj.aiagent.rag.QueryRewriter;
import com.jblmj.aiagent.service.ComplexityAssessor;
import com.jblmj.aiagent.service.TaskDecomposer;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据准确性和质量评测
 *
 * 测试维度：
 * 1. 记忆准确性 - 上下文是否正确保存和召回
 * 2. RAG 相关性 - 检索结果是否与查询相关
 * 3. 查询重写质量 - 重写后是否更标准化
 * 4. 任务分解合理性 - 子任务是否合理且完整
 * 5. 响应完整性 - 回答是否完整覆盖问题
 * 6. 一致性测试 - 相同查询是否得到一致结果
 *
 * @author jblmj
 */
@SpringBootTest
@Slf4j
public class AccuracyQualityTest {

    @Resource
    private WorkflowOrchestrator workflowOrchestrator;

    @Resource
    private ComplexityAssessor complexityAssessor;

    @Resource
    private TaskDecomposer taskDecomposer;

    @Resource
    private QueryRewriter queryRewriter;

    @Resource
    private EnterpriseAssistantApp enterpriseAssistantApp;

    private FileBasedChatMemory chatMemory;

    @BeforeEach
    public void setup() {
        chatMemory = new FileBasedChatMemory("data/test_accuracy");
        log.info("========================================");
        log.info("数据准确性和质量评测初始化");
        log.info("========================================");
    }

    /**
     * 测试 1：记忆准确性测试
     */
    @Test
    @DisplayName("记忆准确性 - 上下文保持和召回")
    public void testMemoryAccuracy() {
        log.info("\n========== 测试 1：记忆准确性 ==========");

        String chatId = "accuracy_test_" + UUID.randomUUID();

        // 场景：多轮对话，验证上下文保持
        List<ConversationTurn> conversation = Arrays.asList(
            new ConversationTurn("我叫张三", null),
            new ConversationTurn("我在阿里巴巴工作", null),
            new ConversationTurn("我明天要去北京出差", null)
        );

        // 保存对话历史
        for (ConversationTurn turn : conversation) {
            List<Message> messages = new ArrayList<>();
            messages.add(new UserMessage(turn.getUserInput()));
            chatMemory.add(chatId, messages);
        }

        // 验证记忆召回
        List<Message> history = chatMemory.get(chatId);

        assertEquals(conversation.size(), history.size(), "应该保存所有对话轮次");

        // 验证内容准确性
        assertTrue(history.get(0).getText().contains("张三"), "应该记住用户姓名");
        assertTrue(history.get(1).getText().contains("阿里巴巴"), "应该记住公司名");
        assertTrue(history.get(2).getText().contains("北京"), "应该记住出差地点");

        log.info("✓ 记忆准确性测试通过 - 所有上下文正确保存");

        // 测试记忆清除
        chatMemory.clear(chatId);
        List<Message> clearedHistory = chatMemory.get(chatId);
        assertEquals(0, clearedHistory.size(), "清除后应该没有历史记录");

        log.info("✓ 记忆清除功能正常");
    }

    /**
     * 测试 2：查询重写质量评测
     */
    @Test
    @DisplayName("查询重写质量 - 标准化和语义保持")
    public void testQueryRewritingQuality() {
        log.info("\n========== 测试 2：查询重写质量 ==========");

        List<QueryRewriteCase> testCases = Arrays.asList(
            new QueryRewriteCase(
                "魔都今天天气咋样",
                Arrays.asList("上海", "天气"),
                "应该将'魔都'转换为'上海'"
            ),
            new QueryRewriteCase(
                "帝都明天会不会下雨",
                Arrays.asList("北京", "天气", "下雨"),
                "应该将'帝都'转换为'北京'"
            ),
            new QueryRewriteCase(
                "差旅能报销多少钱啊",
                Arrays.asList("差旅", "报销", "标准"),
                "应该去除口语化表达"
            ),
            new QueryRewriteCase(
                "出差住宿有啥要求",
                Arrays.asList("出差", "住宿", "标准", "要求"),
                "应该标准化表达"
            )
        );

        int passCount = 0;
        for (QueryRewriteCase testCase : testCases) {
            try {
                String rewritten = queryRewriter.doQueryRewrite(testCase.getOriginalQuery());

                log.info("\n原始查询: {}", testCase.getOriginalQuery());
                log.info("重写结果: {}", rewritten);
                log.info("期望包含: {}", testCase.getExpectedKeywords());

                // 验证重写质量
                assertNotNull(rewritten, "重写结果不应为空");
                assertFalse(rewritten.trim().isEmpty(), "重写结果不应为空字符串");

                // 验证关键词保留（至少保留 60% 的关键词）
                long matchedKeywords = testCase.getExpectedKeywords().stream()
                    .filter(rewritten::contains)
                    .count();

                double keywordRetention = (double) matchedKeywords / testCase.getExpectedKeywords().size();
                log.info("关键词保留率: {}% ({}/{})",
                    String.format("%.1f", keywordRetention * 100),
                    matchedKeywords,
                    testCase.getExpectedKeywords().size());

                if (keywordRetention >= 0.6) {
                    log.info("✓ {}", testCase.getExpectation());
                    passCount++;
                } else {
                    log.warn("✗ 关键词保留率不足");
                }

            } catch (Exception e) {
                log.error("查询重写失败: {}", testCase.getOriginalQuery(), e);
            }
        }

        double passRate = (double) passCount / testCases.size();
        log.info("\n查询重写质量评测结果: {}/{} ({}%)",
            passCount, testCases.size(), String.format("%.1f", passRate * 100));

        assertTrue(passRate >= 0.7, "查询重写质量应 >= 70%");
    }

    /**
     * 测试 3：任务分解合理性评测
     */
    @Test
    @DisplayName("任务分解合理性 - 完整性和逻辑性")
    public void testTaskDecompositionQuality() {
        log.info("\n========== 测试 3：任务分解合理性 ==========");

        List<TaskDecompositionCase> testCases = Arrays.asList(
            new TaskDecompositionCase(
                "明天去杭州出差，查天气，拜访阿里巴巴",
                3,
                Arrays.asList("WEATHER", "CUSTOMER", "ROUTE")
            ),
            new TaskDecompositionCase(
                "去北京出差，住宿标准是多少，推荐协议酒店",
                2,
                Arrays.asList("POLICY", "HOTEL")
            ),
            new TaskDecompositionCase(
                "查询上海天气",
                1,
                Arrays.asList("WEATHER")
            )
        );

        int passCount = 0;
        for (TaskDecompositionCase testCase : testCases) {
            try {
                var tasks = taskDecomposer.decompose(testCase.getQuery());

                log.info("\n查询: {}", testCase.getQuery());
                log.info("分解为 {} 个子任务 (期望: {} 个)", tasks.size(), testCase.getExpectedTaskCount());

                for (var task : tasks) {
                    log.info("  - 任务 {}: {} (类型: {})",
                        task.getId(), task.getDescription(), task.getTaskType());
                }

                // 验证任务数量合理性（允许 ±1 的误差）
                boolean countReasonable = Math.abs(tasks.size() - testCase.getExpectedTaskCount()) <= 1;

                // 验证任务类型覆盖
                Set<String> taskTypes = new HashSet<>();
                for (var task : tasks) {
                    taskTypes.add(task.getTaskType());
                }

                long coveredTypes = testCase.getExpectedTypes().stream()
                    .filter(type -> taskTypes.stream().anyMatch(t -> t.toUpperCase().contains(type.toUpperCase())))
                    .count();

                double typeCoverage = (double) coveredTypes / testCase.getExpectedTypes().size();
                log.info("任务类型覆盖率: {}% ({}/{})",
                    String.format("%.1f", typeCoverage * 100),
                    coveredTypes,
                    testCase.getExpectedTypes().size());

                if (countReasonable && typeCoverage >= 0.8) {
                    log.info("✓ 任务分解合理");
                    passCount++;
                } else {
                    log.warn("✗ 任务分解不够合理");
                }

            } catch (Exception e) {
                log.error("任务分解失败: {}", testCase.getQuery(), e);
            }
        }

        double passRate = (double) passCount / testCases.size();
        log.info("\n任务分解合理性评测结果: {}/{} ({}%)",
            passCount, testCases.size(), String.format("%.1f", passRate * 100));

        assertTrue(passRate >= 0.7, "任务分解合理性应 >= 70%");
    }

    /**
     * 测试 4：响应完整性评测
     */
    @Test
    @DisplayName("响应完整性 - 回答是否完整")
    public void testResponseCompleteness() {
        log.info("\n========== 测试 4：响应完整性 ==========");

        List<CompletenessCase> testCases = Arrays.asList(
            new CompletenessCase(
                "北京今天天气怎么样",
                Arrays.asList("北京", "天气", "温度"),
                "应该包含城市名和天气信息"
            ),
            new CompletenessCase(
                "差旅住宿标准是多少",
                Arrays.asList("住宿", "标准"),
                "应该包含住宿标准信息"
            )
        );

        int passCount = 0;
        for (CompletenessCase testCase : testCases) {
            try {
                String chatId = "completeness_" + UUID.randomUUID();
                String response = workflowOrchestrator.route(testCase.getQuery(), chatId);

                log.info("\n查询: {}", testCase.getQuery());
                log.info("响应长度: {} 字符", response.length());
                log.info("响应摘要: {}", response.substring(0, Math.min(150, response.length())));

                // 验证响应不为空
                assertNotNull(response, "响应不应为空");
                assertTrue(response.length() > 20, "响应应该有实质内容");

                // 验证关键信息覆盖
                long coveredKeywords = testCase.getRequiredKeywords().stream()
                    .filter(response::contains)
                    .count();

                double coverage = (double) coveredKeywords / testCase.getRequiredKeywords().size();
                log.info("关键信息覆盖率: {}% ({}/{})",
                    String.format("%.1f", coverage * 100),
                    coveredKeywords,
                    testCase.getRequiredKeywords().size());

                if (coverage >= 0.6) {
                    log.info("✓ {}", testCase.getExpectation());
                    passCount++;
                } else {
                    log.warn("✗ 响应不够完整");
                }

            } catch (Exception e) {
                log.error("查询执行失败: {}", testCase.getQuery(), e);
            }
        }

        double passRate = (double) passCount / testCases.size();
        log.info("\n响应完整性评测结果: {}/{} ({}%)",
            passCount, testCases.size(), String.format("%.1f", passRate * 100));

        assertTrue(passRate >= 0.6, "响应完整性应 >= 60%");
    }

    /**
     * 测试 5：一致性测试
     */
    @Test
    @DisplayName("一致性测试 - 相同查询的结果稳定性")
    public void testConsistency() {
        log.info("\n========== 测试 5：一致性测试 ==========");

        String[] queries = {
            "北京天气",
            "差旅住宿标准",
            "上海和广州天气对比"
        };

        for (String query : queries) {
            log.info("\n测试查询: {}", query);

            // 执行 3 次相同查询
            List<String> complexities = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                var complexity = complexityAssessor.assess(query);
                complexities.add(complexity.name());
                log.info("  第 {} 次: {}", i + 1, complexity);
            }

            // 验证结果一致性
            boolean consistent = complexities.stream().distinct().count() == 1;

            if (consistent) {
                log.info("✓ 结果一致");
            } else {
                log.warn("✗ 结果不一致: {}", complexities);
            }

            assertTrue(consistent, "相同查询应该得到一致的复杂度评估");
        }

        log.info("\n✓ 一致性测试通过");
    }

    /**
     * 测试 6：边界情况测试
     */
    @Test
    @DisplayName("边界情况测试 - 极端输入处理")
    public void testEdgeCases() {
        log.info("\n========== 测试 6：边界情况测试 ==========");

        Map<String, String> edgeCases = new HashMap<>();
        edgeCases.put("空查询", "");
        edgeCases.put("单字查询", "天");
        edgeCases.put("超长查询", "明天".repeat(100));
        edgeCases.put("纯数字", "123456");
        edgeCases.put("纯符号", "!@#$%^&*()");
        edgeCases.put("混合字符", "abc123!@#中文");

        int handledCount = 0;
        for (Map.Entry<String, String> entry : edgeCases.entrySet()) {
            String caseName = entry.getKey();
            String query = entry.getValue();

            log.info("\n测试: {}", caseName);
            log.info("输入: {}", query.length() > 50 ? query.substring(0, 50) + "..." : query);

            try {
                var complexity = complexityAssessor.assess(query);
                log.info("✓ 正常处理，复杂度: {}", complexity);
                handledCount++;
            } catch (Exception e) {
                log.warn("✗ 处理异常: {}", e.getMessage());
            }
        }

        double handleRate = (double) handledCount / edgeCases.size();
        log.info("\n边界情况处理率: {}/{} ({}%)",
            handledCount, edgeCases.size(), String.format("%.1f", handleRate * 100));

        assertTrue(handleRate >= 0.8, "边界情况处理率应 >= 80%");
    }

    /**
     * 测试 7：语义理解准确性
     */
    @Test
    @DisplayName("语义理解准确性 - 同义表达识别")
    public void testSemanticUnderstanding() {
        log.info("\n========== 测试 7：语义理解准确性 ==========");

        // 同义表达组
        List<List<String>> synonymGroups = Arrays.asList(
            Arrays.asList("北京天气", "帝都天气", "北京市天气情况"),
            Arrays.asList("上海天气", "魔都天气", "上海市天气"),
            Arrays.asList("差旅补贴", "出差补贴", "差旅费用标准")
        );

        int consistentGroups = 0;
        for (List<String> group : synonymGroups) {
            log.info("\n同义表达组: {}", group);

            Set<String> complexities = new HashSet<>();
            for (String query : group) {
                var complexity = complexityAssessor.assess(query);
                complexities.add(complexity.name());
                log.info("  {} -> {}", query, complexity);
            }

            boolean consistent = complexities.size() == 1;
            if (consistent) {
                log.info("✓ 同义表达识别一致");
                consistentGroups++;
            } else {
                log.warn("✗ 同义表达识别不一致: {}", complexities);
            }
        }

        double consistency = (double) consistentGroups / synonymGroups.size();
        log.info("\n语义理解一致性: {}/{} ({}%)",
            consistentGroups, synonymGroups.size(), String.format("%.1f", consistency * 100));

        assertTrue(consistency >= 0.6, "语义理解一致性应 >= 60%");
    }

    // ========== 测试数据结构 ==========

    @Data
    static class ConversationTurn {
        private String userInput;
        private String expectedContext;

        public ConversationTurn(String userInput, String expectedContext) {
            this.userInput = userInput;
            this.expectedContext = expectedContext;
        }
    }

    @Data
    static class QueryRewriteCase {
        private String originalQuery;
        private List<String> expectedKeywords;
        private String expectation;

        public QueryRewriteCase(String originalQuery, List<String> expectedKeywords, String expectation) {
            this.originalQuery = originalQuery;
            this.expectedKeywords = expectedKeywords;
            this.expectation = expectation;
        }
    }

    @Data
    static class TaskDecompositionCase {
        private String query;
        private int expectedTaskCount;
        private List<String> expectedTypes;

        public TaskDecompositionCase(String query, int expectedTaskCount, List<String> expectedTypes) {
            this.query = query;
            this.expectedTaskCount = expectedTaskCount;
            this.expectedTypes = expectedTypes;
        }
    }

    @Data
    static class CompletenessCase {
        private String query;
        private List<String> requiredKeywords;
        private String expectation;

        public CompletenessCase(String query, List<String> requiredKeywords, String expectation) {
            this.query = query;
            this.requiredKeywords = requiredKeywords;
            this.expectation = expectation;
        }
    }
}
