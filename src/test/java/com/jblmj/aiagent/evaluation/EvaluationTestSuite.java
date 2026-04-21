package com.jblmj.aiagent.evaluation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 系统评测测试套件
 *
 * 运行所有评测测试，生成完整的系统评估报告
 *
 * 使用方式：
 * 1. 命令行运行所有评测: mvn test -Dtest="com.jblmj.aiagent.evaluation.*"
 * 2. 运行单个测试: mvn test -Dtest=SystemIntegrationTest
 * 3. IDE 运行: 右键点击测试类 -> Run
 *
 * 测试覆盖：
 * - SystemIntegrationTest: 系统集成测试（9个测试）
 * - PerformanceStressTest: 性能压测（7个测试）
 * - AccuracyQualityTest: 准确性质量测试（7个测试）
 * - 已有的评测: RAG、MCP、复杂度框架、工作流编排
 *
 * @author jblmj
 */
@SpringBootTest
@Slf4j
public class EvaluationTestSuite {

    /**
     * 测试套件说明
     */
    @Test
    @DisplayName("评测套件说明")
    public void testSuiteInfo() {
        log.info("\n========================================");
        log.info("AI Agent 系统全面评测套件");
        log.info("========================================");
        log.info("\n包含以下测试类:");
        log.info("1. SystemIntegrationTest - 系统集成测试（9个测试）");
        log.info("   - 记忆系统、查询重写、复杂度评估、任务分解");
        log.info("   - MCP工具、技能路由、RAG检索、端到端工作流");
        log.info("");
        log.info("2. PerformanceStressTest - 性能压测（7个测试）");
        log.info("   - 并发查询、性能基准、并行执行效率");
        log.info("   - 内存使用、稳定性、峰值负载、错误恢复");
        log.info("");
        log.info("3. AccuracyQualityTest - 准确性质量测试（7个测试）");
        log.info("   - 记忆准确性、查询重写质量、任务分解合理性");
        log.info("   - 响应完整性、一致性、边界情况、语义理解");
        log.info("");
        log.info("4. 已有评测测试");
        log.info("   - RAGEvaluationTest: RAG检索评测");
        log.info("   - McpEvaluationTest: MCP工具评测");
        log.info("   - ComplexityFrameworkTest: 复杂度评估评测");
        log.info("   - WorkflowOrchestratorTest: 工作流编排评测");
        log.info("   - WeatherToolEvaluationTest: 天气工具评测");
        log.info("\n运行方式:");
        log.info("  mvn test -Dtest=\"com.jblmj.aiagent.evaluation.*\"");
        log.info("========================================");
    }
}
