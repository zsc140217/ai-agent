package com.jblmj.aiagent.agent;

import com.jblmj.aiagent.model.ReActStep;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 增强版 ReAct Agent 测试
 * 验证完整的 Thought → Action → Observation → Reflection 循环
 */
@SpringBootTest
@Slf4j
public class EnhancedReActAgentTest {

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
     * 测试1：简单天气查询
     * 验证基础的 ReAct 循环
     */
    @Test
    public void testSimpleWeatherQuery() {
        log.info("========== 测试1：简单天气查询 ==========");

        String userPrompt = "查询北京的天气";
        String result = jblmjManus.run(userPrompt);

        // 验证执行结果
        assertNotNull(result, "执行结果不应为空");
        log.info("执行结果：\n{}", result);

        // 验证执行轨迹
        List<ReActStep> trace = jblmjManus.getExecutionTrace();
        assertFalse(trace.isEmpty(), "执行轨迹不应为空");

        // 验证每个步骤都包含完整的 ReAct 循环
        for (ReActStep step : trace) {
            log.info("步骤 {}：", step.getStepNumber());
            log.info("  Thought: {}", step.getThought());
            log.info("  Action: {}", step.getAction());
            log.info("  Observation: {}", step.getObservation());
            log.info("  Reflection: {}", step.getReflection());

            assertNotNull(step.getThought(), "思考内容不应为空");
            assertNotNull(step.getAction(), "行动内容不应为空");
            assertNotNull(step.getObservation(), "观察内容不应为空");
            assertNotNull(step.getReflection(), "反思内容不应为空");
        }

        // 输出完整轨迹
        log.info(jblmjManus.getExecutionTraceFormatted());
    }

    /**
     * 测试2：复杂任务（多步骤）
     * 验证多轮 ReAct 循环和策略调整
     */
    @Test
    public void testComplexTask() {
        log.info("========== 测试2：复杂任务（多步骤） ==========");

        String userPrompt = "查询北京和上海的天气，并比较哪个城市更适合旅游";
        String result = jblmjManus.run(userPrompt);

        // 验证执行结果
        assertNotNull(result, "执行结果不应为空");
        log.info("执行结果：\n{}", result);

        // 验证执行轨迹
        List<ReActStep> trace = jblmjManus.getExecutionTrace();
        assertTrue(trace.size() >= 2, "复杂任务应该有多个步骤");

        // 验证是否有观察和反思
        boolean hasObservation = trace.stream()
                .anyMatch(step -> step.getObservation() != null && step.getObservation().contains("观察到"));
        assertTrue(hasObservation, "应该包含观察结果");

        boolean hasReflection = trace.stream()
                .anyMatch(step -> step.getReflection() != null && !step.getReflection().isEmpty());
        assertTrue(hasReflection, "应该包含反思内容");

        // 输出完整轨迹
        log.info(jblmjManus.getExecutionTraceFormatted());
    }

    /**
     * 测试3：工具调用失败场景
     * 验证错误处理和策略调整
     */
    @Test
    public void testToolCallFailure() {
        log.info("========== 测试3：工具调用失败场景 ==========");

        // 使用一个不存在的城市名，触发工具调用失败
        String userPrompt = "查询不存在城市的天气";
        String result = jblmjManus.run(userPrompt);

        // 验证执行结果
        assertNotNull(result, "执行结果不应为空");
        log.info("执行结果：\n{}", result);

        // 验证执行轨迹
        List<ReActStep> trace = jblmjManus.getExecutionTrace();
        assertFalse(trace.isEmpty(), "执行轨迹不应为空");

        // 检查是否有反思提到失败或调整策略
        boolean hasFailureReflection = trace.stream()
                .anyMatch(step -> step.getReflection() != null &&
                        (step.getReflection().contains("失败") || step.getReflection().contains("调整策略")));

        log.info("是否检测到失败并反思：{}", hasFailureReflection);

        // 输出完整轨迹
        log.info(jblmjManus.getExecutionTraceFormatted());
    }

    /**
     * 测试4：验证执行轨迹的完整性
     */
    @Test
    public void testExecutionTraceCompleteness() {
        log.info("========== 测试4：验证执行轨迹的完整性 ==========");

        String userPrompt = "查询深圳的天气";
        jblmjManus.run(userPrompt);

        List<ReActStep> trace = jblmjManus.getExecutionTrace();

        // 验证每个步骤的时间戳和耗时
        for (ReActStep step : trace) {
            assertTrue(step.getTimestamp() > 0, "时间戳应该大于0");
            assertTrue(step.getDuration() >= 0, "耗时应该大于等于0");

            log.info("步骤 {} - 耗时: {}ms", step.getStepNumber(), step.getDuration());
        }

        // 验证步骤编号的连续性
        for (int i = 0; i < trace.size(); i++) {
            assertEquals(i + 1, trace.get(i).getStepNumber(),
                    "步骤编号应该连续");
        }
    }

    /**
     * 测试5：验证 ReAct 循环的格式化输出
     */
    @Test
    public void testFormattedOutput() {
        log.info("========== 测试5：验证 ReAct 循环的格式化输出 ==========");

        String userPrompt = "查询广州的天气";
        String result = jblmjManus.run(userPrompt);

        // 验证输出包含 ReAct 循环的标记
        assertTrue(result.contains("Step"), "输出应包含步骤标记");

        // 验证格式化的执行轨迹
        String formattedTrace = jblmjManus.getExecutionTraceFormatted();
        assertNotNull(formattedTrace, "格式化轨迹不应为空");

        assertTrue(formattedTrace.contains("💭 Thought"), "应包含思考标记");
        assertTrue(formattedTrace.contains("🔧 Action"), "应包含行动标记");
        assertTrue(formattedTrace.contains("👁️ Observation"), "应包含观察标记");
        assertTrue(formattedTrace.contains("🤔 Reflection"), "应包含反思标记");

        log.info("格式化输出：\n{}", formattedTrace);
    }
}
