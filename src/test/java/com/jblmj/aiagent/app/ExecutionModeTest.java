package com.jblmj.aiagent.app;

import com.jblmj.aiagent.model.ExecutionMode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 执行模式切换测试
 * 验证用户可以主动选择 DEFAULT（快速）或 THINKING（详细）模式
 */
@SpringBootTest
@Slf4j
public class ExecutionModeTest {

    @Autowired
    private WorkflowOrchestrator workflowOrchestrator;

    /**
     * 测试1：默认模式（快速）
     * 使用复杂度评估 + 并行执行
     */
    @Test
    public void testDefaultMode() {
        log.info("========== 测试1：默认模式（快速） ==========");

        String query = "规划去杭州出差，查询天气";
        long startTime = System.currentTimeMillis();

        String result = workflowOrchestrator.route(query, "test-default", ExecutionMode.DEFAULT);

        long duration = System.currentTimeMillis() - startTime;

        log.info("执行结果：\n{}", result);
        log.info("执行耗时：{} ms", duration);

        assertNotNull(result, "结果不应为空");
        assertTrue(duration < 15000, "默认模式应该在15秒内完成");
    }

    /**
     * 测试2：思考模式（详细）
     * 使用 ReAct 循环，有完整的执行轨迹
     */
    @Test
    public void testThinkingMode() {
        log.info("========== 测试2：思考模式（详细） ==========");

        String query = "规划去杭州出差，查询天气";
        long startTime = System.currentTimeMillis();

        String result = workflowOrchestrator.route(query, "test-thinking", ExecutionMode.THINKING);

        long duration = System.currentTimeMillis() - startTime;

        log.info("执行结果：\n{}", result);
        log.info("执行耗时：{} ms", duration);

        assertNotNull(result, "结果不应为空");
        // 思考模式应该包含执行轨迹信息
        assertTrue(result.contains("执行步骤") || result.contains("Thought") || result.contains("💭"),
                "思考模式应该包含执行轨迹");
    }

    /**
     * 测试3：对比两种模式的性能
     */
    @Test
    public void testModeComparison() {
        log.info("========== 测试3：对比两种模式的性能 ==========");

        String query = "规划去深圳出差，查询天气";

        // 测试默认模式
        long defaultStart = System.currentTimeMillis();
        String defaultResult = workflowOrchestrator.route(query, "test-default-compare", ExecutionMode.DEFAULT);
        long defaultDuration = System.currentTimeMillis() - defaultStart;

        log.info("默认模式耗时：{} ms", defaultDuration);
        log.info("默认模式结果长度：{} 字符", defaultResult.length());

        // 等待一下，避免 API 限流
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 测试思考模式
        long thinkingStart = System.currentTimeMillis();
        String thinkingResult = workflowOrchestrator.route(query, "test-thinking-compare", ExecutionMode.THINKING);
        long thinkingDuration = System.currentTimeMillis() - thinkingStart;

        log.info("思考模式耗时：{} ms", thinkingDuration);
        log.info("思考模式结果长度：{} 字符", thinkingResult.length());

        // 对比分析
        log.info("========== 性能对比 ==========");
        log.info("默认模式：{} ms", defaultDuration);
        log.info("思考模式：{} ms", thinkingDuration);
        log.info("性能差异：{} ms ({}%)",
                thinkingDuration - defaultDuration,
                (thinkingDuration - defaultDuration) * 100 / defaultDuration);

        // 验证：思考模式应该更慢，但提供更多信息
        assertTrue(thinkingResult.length() > defaultResult.length(),
                "思考模式应该提供更详细的信息");
    }

    /**
     * 测试4：ExecutionMode.fromString() 解析
     */
    @Test
    public void testExecutionModeFromString() {
        log.info("========== 测试4：ExecutionMode 解析 ==========");

        // 测试各种输入
        assertEquals(ExecutionMode.DEFAULT, ExecutionMode.fromString("default"));
        assertEquals(ExecutionMode.DEFAULT, ExecutionMode.fromString("DEFAULT"));
        assertEquals(ExecutionMode.DEFAULT, ExecutionMode.fromString("默认"));
        assertEquals(ExecutionMode.DEFAULT, ExecutionMode.fromString("快速"));
        assertEquals(ExecutionMode.DEFAULT, ExecutionMode.fromString(null));
        assertEquals(ExecutionMode.DEFAULT, ExecutionMode.fromString(""));

        assertEquals(ExecutionMode.THINKING, ExecutionMode.fromString("thinking"));
        assertEquals(ExecutionMode.THINKING, ExecutionMode.fromString("THINKING"));
        assertEquals(ExecutionMode.THINKING, ExecutionMode.fromString("思考"));
        assertEquals(ExecutionMode.THINKING, ExecutionMode.fromString("详细"));

        log.info("ExecutionMode 解析测试通过");
    }

    /**
     * 测试5：不传模式参数（应该使用默认模式）
     */
    @Test
    public void testNoModeParameter() {
        log.info("========== 测试5：不传模式参数 ==========");

        String query = "规划去广州出差，查询天气";
        long startTime = System.currentTimeMillis();

        // 使用旧接口（不传模式）
        String result = workflowOrchestrator.route(query, "test-no-mode");

        long duration = System.currentTimeMillis() - startTime;

        log.info("执行结果：\n{}", result);
        log.info("执行耗时：{} ms", duration);

        assertNotNull(result, "结果不应为空");
        assertTrue(duration < 15000, "默认应该使用快速模式");
    }
}
