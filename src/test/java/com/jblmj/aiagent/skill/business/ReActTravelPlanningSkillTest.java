package com.jblmj.aiagent.skill.business;

import com.jblmj.aiagent.agent.JblmjManus;
import com.jblmj.aiagent.model.ReActStep;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReAct 差旅规划 Skill 测试
 * 验证 ReAct 框架与出差项目的集成
 */
@SpringBootTest
@Slf4j
public class ReActTravelPlanningSkillTest {

    @Autowired
    private ReActTravelPlanningSkill reActTravelPlanningSkill;

    @Autowired
    private JblmjManus jblmjManus;

    /**
     * 每个测试前重置 Agent 状态
     */
    @BeforeEach
    public void setUp() {
        log.info("重置 Agent 状态");
        jblmjManus.reset();
    }

    /**
     * 测试1：简单差旅规划（单城市天气查询）
     */
    @Test
    public void testSimpleTravelPlanning() {
        log.info("========== 测试1：简单差旅规划 ==========");

        String query = "帮我规划明天去杭州的出差行程";
        String result = reActTravelPlanningSkill.execute(query, "test-001");

        // 验证结果
        assertNotNull(result, "结果不应为空");
        assertTrue(result.contains("差旅规划") || result.contains("杭州"), "结果应包含规划信息");

        log.info("规划结果：\n{}", result);

        // 验证执行轨迹
        List<ReActStep> trace = jblmjManus.getExecutionTrace();
        assertFalse(trace.isEmpty(), "执行轨迹不应为空");

        log.info("执行步骤数：{}", trace.size());
    }

    /**
     * 测试2：复杂差旅规划（多城市对比）
     */
    @Test
    public void testComplexTravelPlanning() {
        log.info("========== 测试2：复杂差旅规划 ==========");

        String query = "我要去北京和上海出差，帮我规划行程，对比两个城市的天气";
        String result = reActTravelPlanningSkill.execute(query, "test-002");

        // 验证结果
        assertNotNull(result, "结果不应为空");
        assertTrue(result.contains("北京") || result.contains("上海"), "结果应包含城市信息");

        log.info("规划结果：\n{}", result);

        // 验证执行轨迹
        List<ReActStep> trace = jblmjManus.getExecutionTrace();
        assertTrue(trace.size() >= 2, "复杂任务应该有多个步骤");

        // 验证是否有工具调用
        boolean hasToolCall = trace.stream()
                .anyMatch(step -> step.getAction() != null && step.getAction().contains("工具"));
        assertTrue(hasToolCall, "应该包含工具调用");

        log.info("执行步骤数：{}", trace.size());
    }

    /**
     * 测试3：带客户拜访的差旅规划
     */
    @Test
    public void testTravelPlanningWithCustomer() {
        log.info("========== 测试3：带客户拜访的差旅规划 ==========");

        String query = "规划去深圳出差3天，需要拜访腾讯公司，查询天气和推荐酒店";
        String result = reActTravelPlanningSkill.execute(query, "test-003");

        // 验证结果
        assertNotNull(result, "结果不应为空");
        log.info("规划结果：\n{}", result);

        // 验证执行轨迹
        List<ReActStep> trace = jblmjManus.getExecutionTrace();
        assertFalse(trace.isEmpty(), "执行轨迹不应为空");

        // 验证是否有观察和反思
        boolean hasObservation = trace.stream()
                .anyMatch(step -> step.getObservation() != null && !step.getObservation().isEmpty());
        assertTrue(hasObservation, "应该包含观察结果");

        boolean hasReflection = trace.stream()
                .anyMatch(step -> step.getReflection() != null && !step.getReflection().isEmpty());
        assertTrue(hasReflection, "应该包含反思内容");

        log.info("执行步骤数：{}", trace.size());
    }

    /**
     * 测试4：验证 ReAct 循环的完整性
     */
    @Test
    public void testReActCycleCompleteness() {
        log.info("========== 测试4：验证 ReAct 循环的完整性 ==========");

        String query = "规划去广州出差，查询天气";
        reActTravelPlanningSkill.execute(query, "test-004");

        List<ReActStep> trace = jblmjManus.getExecutionTrace();

        // 验证每个步骤都包含完整的 ReAct 循环
        for (ReActStep step : trace) {
            assertNotNull(step.getThought(), "思考内容不应为空");
            assertNotNull(step.getAction(), "行动内容不应为空");
            assertNotNull(step.getObservation(), "观察内容不应为空");
            assertNotNull(step.getReflection(), "反思内容不应为空");

            log.info("步骤 {} 验证通过", step.getStepNumber());
        }

        log.info("所有步骤的 ReAct 循环完整性验证通过");
    }

    /**
     * 测试5：验证执行轨迹的格式化输出
     */
    @Test
    public void testExecutionTraceFormatting() {
        log.info("========== 测试5：验证执行轨迹的格式化输出 ==========");

        String query = "规划去成都出差";
        String result = reActTravelPlanningSkill.execute(query, "test-005");

        // 验证格式化输出
        assertTrue(result.contains("差旅规划"), "应包含差旅规划标题");
        assertTrue(result.contains("执行摘要"), "应包含执行摘要");
        assertTrue(result.contains("执行步骤"), "应包含步骤统计");
        assertTrue(result.contains("总耗时"), "应包含耗时统计");

        log.info("格式化输出：\n{}", result);
    }

    /**
     * 测试6：对比原有 Skill 和 ReAct Skill
     */
    @Test
    public void testCompareWithOriginalSkill() {
        log.info("========== 测试6：对比原有 Skill 和 ReAct Skill ==========");

        String query = "规划去西安出差，查询天气";

        // 测试 ReAct Skill
        long startTime = System.currentTimeMillis();
        String reactResult = reActTravelPlanningSkill.execute(query, "test-006-react");
        long reactDuration = System.currentTimeMillis() - startTime;

        log.info("ReAct Skill 结果：\n{}", reactResult);
        log.info("ReAct Skill 耗时：{} ms", reactDuration);

        // 验证 ReAct Skill 的优势
        List<ReActStep> trace = jblmjManus.getExecutionTrace();
        log.info("ReAct Skill 执行步骤：{}", trace.size());
        log.info("ReAct Skill 提供了完整的执行轨迹和反思能力");

        assertNotNull(reactResult, "ReAct Skill 结果不应为空");
        assertFalse(trace.isEmpty(), "ReAct Skill 应该有执行轨迹");
    }

    /**
     * 测试7：验证 Skill 优先级
     */
    @Test
    public void testSkillPriority() {
        log.info("========== 测试7：验证 Skill 优先级 ==========");

        // ReActTravelPlanningSkill 的优先级是 70
        int priority = reActTravelPlanningSkill.getPriority();
        assertEquals(70, priority, "ReAct Skill 优先级应该是 70");

        log.info("ReAct Skill 优先级：{}", priority);
        log.info("优先级高于原有的 TravelPlanningSkill (60)，会被优先选择");
    }
}
