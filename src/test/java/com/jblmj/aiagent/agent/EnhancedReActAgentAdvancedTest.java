package com.jblmj.aiagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 增强版 ReAct Agent 测试
 * 验证智能观察、自动重试、策略调整、经验积累等功能
 */
@SpringBootTest
@Slf4j
public class EnhancedReActAgentAdvancedTest {

    @Autowired
    private JblmjManus jblmjManus;

    /**
     * 测试1：智能观察 - 信息提炼
     */
    @Test
    public void testIntelligentObservation() {
        log.info("========== 测试1：智能观察 - 信息提炼 ==========");

        jblmjManus.reset();
        jblmjManus.setTaskGoal("查询杭州天气并分析");

        String result = jblmjManus.run("查询杭州的天气");

        log.info("执行结果：\n{}", result);
        log.info("执行轨迹：\n{}", jblmjManus.getExecutionTraceFormatted());

        // 验证执行轨迹
        assertFalse(jblmjManus.getExecutionTrace().isEmpty(), "应该有执行轨迹");

        // 验证观察结果包含结构化信息
        jblmjManus.getExecutionTrace().forEach(step -> {
            if (step.getObservationResult() != null) {
                log.info("观察结果：{}", step.getObservationResult().format());
                assertNotNull(step.getObservationResult().getSummary(), "应该有观察摘要");
            }
        });
    }

    /**
     * 测试2：反思机制 - 失败分析
     */
    @Test
    public void testReflectionWithFailureAnalysis() {
        log.info("========== 测试2：反思机制 - 失败分析 ==========");

        jblmjManus.reset();
        jblmjManus.setTaskGoal("测试失败处理");

        // 模拟一个可能失败的查询
        String result = jblmjManus.run("查询一个不存在的城市的天气");

        log.info("执行结果：\n{}", result);
        log.info("执行轨迹：\n{}", jblmjManus.getExecutionTraceFormatted());

        // 验证反思结果
        jblmjManus.getExecutionTrace().forEach(step -> {
            if (step.getReflectionResult() != null) {
                log.info("反思结果：{}", step.getReflectionResult().format());

                // 如果失败，应该有失败原因分析
                if (!step.getReflectionResult().isSuccess()) {
                    assertNotNull(step.getReflectionResult().getFailureReason(),
                            "失败时应该有失败原因分析");
                    log.info("失败原因：{}", step.getReflectionResult().getFailureReason());
                }
            }
        });
    }

    /**
     * 测试3：重试机制
     */
    @Test
    public void testRetryMechanism() {
        log.info("========== 测试3：重试机制 ==========");

        jblmjManus.reset();

        // 执行一个任务
        String result = jblmjManus.run("查询北京天气");

        log.info("执行结果：\n{}", result);

        // 检查重试计数器
        log.info("重试计数器：{}", jblmjManus.getRetryCounters());

        // 验证重试计数器已初始化
        assertNotNull(jblmjManus.getRetryCounters(), "重试计数器应该存在");
    }

    /**
     * 测试4：经验积累
     */
    @Test
    public void testExperienceLearning() {
        log.info("========== 测试4：经验积累 ==========");

        jblmjManus.reset();

        // 执行多个任务，积累经验
        jblmjManus.run("查询上海天气");
        jblmjManus.reset();
        jblmjManus.run("查询广州天气");

        // 检查经验库
        log.info("经验库：{}", jblmjManus.getExperienceLibrary());

        // 验证经验库不为空
        assertNotNull(jblmjManus.getExperienceLibrary(), "经验库应该存在");
    }

    /**
     * 测试5：目标追踪和进度管理
     */
    @Test
    public void testGoalTrackingAndProgress() {
        log.info("========== 测试5：目标追踪和进度管理 ==========");

        jblmjManus.reset();
        jblmjManus.setTaskGoal("规划深圳出差行程");

        String result = jblmjManus.run("规划去深圳出差，查询天气和推荐酒店");

        log.info("执行结果：\n{}", result);

        // 计算进度
        double progress = jblmjManus.calculateProgress();
        log.info("任务进度：{}%", progress * 100);

        // 验证进度
        assertTrue(progress >= 0.0 && progress <= 1.0, "进度应该在0-1之间");

        // 检查已完成的子目标
        log.info("已完成的子目标：{}", jblmjManus.getCompletedSubGoals());
    }

    /**
     * 测试6：完整的 ReAct 循环验证
     */
    @Test
    public void testCompleteReActCycle() {
        log.info("========== 测试6：完整的 ReAct 循环验证 ==========");

        jblmjManus.reset();
        jblmjManus.setTaskGoal("完整的差旅规划");

        String result = jblmjManus.run("我要去杭州出差3天，帮我规划一下");

        log.info("执行结果：\n{}", result);
        log.info("执行轨迹：\n{}", jblmjManus.getExecutionTraceFormatted());

        // 验证每个步骤都有完整的四阶段
        jblmjManus.getExecutionTrace().forEach(step -> {
            log.info("\n=== 验证 Step {} ===", step.getStepNumber());

            // 验证 Thought
            assertNotNull(step.getThought(), "应该有思考内容");
            log.info("✅ Thought: {}", step.getThought().substring(0, Math.min(50, step.getThought().length())));

            // 验证 Action
            assertNotNull(step.getAction(), "应该有行动内容");
            log.info("✅ Action: {}", step.getAction().substring(0, Math.min(50, step.getAction().length())));

            // 验证 Observation
            assertNotNull(step.getObservation(), "应该有观察内容");
            log.info("✅ Observation: {}", step.getObservation().substring(0, Math.min(50, step.getObservation().length())));

            // 验证 Reflection
            assertNotNull(step.getReflection(), "应该有反思内容");
            log.info("✅ Reflection: {}", step.getReflection().substring(0, Math.min(50, step.getReflection().length())));

            // 验证结构化结果
            if (step.getObservationResult() != null) {
                log.info("📊 观察结果：{}", step.getObservationResult().format());
            }

            if (step.getReflectionResult() != null) {
                log.info("🤔 反思结果：{}", step.getReflectionResult().format());
            }
        });

        // 验证执行轨迹不为空
        assertFalse(jblmjManus.getExecutionTrace().isEmpty(), "应该有执行轨迹");

        // 验证任务完成
        log.info("\n========== 任务总结 ==========");
        log.info("总步骤数：{}", jblmjManus.getExecutionTrace().size());
        log.info("任务进度：{}%", jblmjManus.calculateProgress() * 100);
        log.info("经验库大小：{}", jblmjManus.getExperienceLibrary().size());
    }

    /**
     * 测试7：策略调整验证
     */
    @Test
    public void testStrategyAdjustment() {
        log.info("========== 测试7：策略调整验证 ==========");

        jblmjManus.reset();

        String result = jblmjManus.run("查询天气并推荐酒店");

        log.info("执行结果：\n{}", result);

        // 检查是否有策略调整
        jblmjManus.getExecutionTrace().forEach(step -> {
            if (step.getReflectionResult() != null) {
                String strategyAdjustment = step.getReflectionResult().getStrategyAdjustment();
                if (strategyAdjustment != null && !strategyAdjustment.isEmpty()) {
                    log.info("策略调整：{}", strategyAdjustment);
                }
            }
        });
    }
}
