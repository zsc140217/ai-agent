# AI Agent 系统评测文档

## 概述

本评测体系全面测试 AI Agent 系统的各项核心功能，包括记忆、RAG、MCP、查询重写、任务编排、复杂度评估等模块。

## 评测架构

```
evaluation/
├── SystemIntegrationTest.java      # 系统集成测试（9个测试）
├── PerformanceStressTest.java      # 性能压测测试（7个测试）
├── AccuracyQualityTest.java        # 准确性质量测试（7个测试）
├── RAGEvaluationTest.java          # RAG检索评测
├── McpEvaluationTest.java          # MCP工具评测
├── ComplexityFrameworkTest.java    # 复杂度评估评测
├── WorkflowOrchestratorTest.java   # 工作流编排评测
├── WeatherToolEvaluationTest.java  # 天气工具评测
└── EvaluationTestSuite.java        # 测试套件运行器
```

## 测试覆盖

### 1. SystemIntegrationTest - 系统集成测试

**测试目标**: 验证各模块集成后的功能完整性

| 测试项 | 测试内容 | 通过标准 |
|--------|---------|---------|
| 记忆系统 | 多轮对话上下文保持 | 正确保存和召回所有消息 |
| 查询重写 | 口语化转标准查询 | 成功率 ≥ 80% |
| 复杂度评估 | 分类准确性 | 准确率 ≥ 80% |
| 任务分解 | 复杂查询拆解 | 合理分解为子任务 |
| MCP工具 | 天气查询 | 成功率 ≥ 80% |
| 技能路由 | Skill选择 | 正确路由到对应Skill |
| RAG检索 | 向量相似度搜索 | 成功率 ≥ 60% |
| 端到端工作流 | 完整流程 | 通过率 ≥ 70% |
| 性能基准 | 响应时间 | 所有操作 < 10s |

**运行方式**:
```bash
mvn test -Dtest=SystemIntegrationTest
```

### 2. PerformanceStressTest - 性能压测测试

**测试目标**: 验证系统在高负载下的性能表现

| 测试项 | 测试内容 | 通过标准 |
|--------|---------|---------|
| 并发查询压测 | 10用户×5请求 | 成功率 ≥ 80% |
| 复杂度评估性能 | 批量处理 | 每个查询 < 5s |
| 任务分解并行度 | 并行执行效率 | 分解 < 10s，排序 < 1s |
| 内存使用 | 大量查询后内存增长 | 增长 < 500MB |
| 稳定性测试 | 50次连续查询 | 成功率 ≥ 95% |
| 峰值负载 | 20并发突发流量 | 成功率 ≥ 80% |
| 错误恢复 | 异常输入处理 | 优雅处理，不崩溃 |

**关键指标**:
- QPS (每秒查询数)
- 响应时间分布 (P50, P95, P99)
- 内存使用情况
- 并发处理能力

**运行方式**:
```bash
mvn test -Dtest=PerformanceStressTest
```

### 3. AccuracyQualityTest - 准确性质量测试

**测试目标**: 验证系统输出的准确性和质量

| 测试项 | 测试内容 | 通过标准 |
|--------|---------|---------|
| 记忆准确性 | 上下文保持和召回 | 100%准确 |
| 查询重写质量 | 标准化和语义保持 | 质量 ≥ 70% |
| 任务分解合理性 | 完整性和逻辑性 | 合理性 ≥ 70% |
| 响应完整性 | 回答是否完整 | 完整性 ≥ 60% |
| 一致性测试 | 相同查询结果稳定性 | 100%一致 |
| 边界情况 | 极端输入处理 | 处理率 ≥ 80% |
| 语义理解 | 同义表达识别 | 一致性 ≥ 60% |

**运行方式**:
```bash
mvn test -Dtest=AccuracyQualityTest
```

### 4. 已有评测测试

#### RAGEvaluationTest
- 对比三种方案：Baseline、+Query Rewriting、+RAG Advisor
- 评估检索准确率和相关性

#### McpEvaluationTest
- 测试 MCP 工具调用
- 验证工具集成和响应

#### ComplexityFrameworkTest
- 测试复杂度评估框架
- 验证 SIMPLE/MEDIUM/COMPLEX 分类

#### WorkflowOrchestratorTest
- 测试工作流编排
- 验证任务路由和执行

## 快速开始

### 运行所有评测

```bash
# 运行完整测试套件
mvn test -Dtest=EvaluationTestSuite

# 或者运行所有evaluation包下的测试
mvn test -Dtest="com.jblmj.aiagent.evaluation.*"
```

### 运行单个测试类

```bash
# 系统集成测试
mvn test -Dtest=SystemIntegrationTest

# 性能压测
mvn test -Dtest=PerformanceStressTest

# 准确性测试
mvn test -Dtest=AccuracyQualityTest
```

### 运行单个测试方法

```bash
# 只运行记忆系统测试
mvn test -Dtest=SystemIntegrationTest#testMemorySystem

# 只运行并发压测
mvn test -Dtest=PerformanceStressTest#testConcurrentQueries
```

## 测试数据准备

### 1. 向量数据库准备

确保 RAG 向量库已加载数据：
```bash
# 访问向量库管理接口
curl http://localhost:8080/vector/load
```

### 2. 测试数据目录

测试会自动创建以下目录：
```
data/
├── test_memory/          # 记忆系统测试数据
├── test_accuracy/        # 准确性测试数据
└── evaluation/           # 评测结果数据
```

## 评测报告

### 查看测试报告

测试完成后，查看生成的报告：

```bash
# Maven Surefire 报告
target/surefire-reports/

# 控制台输出包含详细的测试日志
```

### 报告内容

每个测试都会输出：
- ✓ 通过的测试项
- ✗ 失败的测试项
- 详细的性能指标
- 准确率统计
- 错误信息和堆栈

### 示例输出

```
========== 测试 1：记忆系统 ==========
✓ 记忆准确性测试通过 - 所有上下文正确保存
✓ 记忆清除功能正常

========== 测试 2：查询重写 ==========
原始查询: 魔都今天天气咋样 -> 重写后: 上海今天天气情况
✓ 查询重写测试通过 (4/4)

========================================
系统集成测试报告
========================================
测试时间: 2026-04-19
测试项目数: 9
总体通过率: 9/9 (100.0%)
========================================
```

## 性能基准

### 预期性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 复杂度评估 | < 2s | 单次评估响应时间 |
| 查询重写 | < 3s | 单次重写响应时间 |
| 任务分解 | < 10s | 复杂查询分解时间 |
| 并发QPS | > 5 | 10并发下的吞吐量 |
| P95响应时间 | < 5s | 95%请求响应时间 |
| 内存增长 | < 500MB | 100次查询后内存增长 |

### 压测配置

可以通过修改测试类中的常量调整压测参数：

```java
// PerformanceStressTest.java
private static final int CONCURRENT_USERS = 10;      // 并发用户数
private static final int REQUESTS_PER_USER = 5;      // 每用户请求数
```

## 故障排查

### 常见问题

1. **测试超时**
   - 检查网络连接
   - 确认 LLM 服务可用
   - 增加超时时间配置

2. **向量检索失败**
   - 确认向量库已初始化
   - 检查数据是否已加载
   - 验证向量库配置

3. **MCP工具调用失败**
   - 检查 MCP 服务状态
   - 验证 API Key 配置
   - 查看工具注册情况

4. **内存溢出**
   - 增加 JVM 堆内存: `-Xmx2g`
   - 减少并发数量
   - 检查内存泄漏

### 调试模式

启用详细日志：

```bash
# 设置日志级别为 DEBUG
mvn test -Dtest=SystemIntegrationTest -Dlogging.level.com.jblmj.aiagent=DEBUG
```

## 持续集成

### CI/CD 集成

在 CI 流程中运行评测：

```yaml
# .github/workflows/test.yml
- name: Run Evaluation Tests
  run: mvn test -Dtest=EvaluationTestSuite
  
- name: Upload Test Reports
  uses: actions/upload-artifact@v2
  with:
    name: test-reports
    path: target/surefire-reports/
```

## 扩展测试

### 添加新测试用例

1. 在对应的测试类中添加测试方法
2. 使用 `@Test` 和 `@DisplayName` 注解
3. 遵循现有的测试结构和命名规范

示例：

```java
@Test
@DisplayName("测试新功能 - 功能描述")
public void testNewFeature() {
    log.info("\n========== 测试新功能 ==========");
    
    // 测试逻辑
    
    assertTrue(condition, "验证条件");
    log.info("✓ 测试通过");
}
```

### 添加新测试类

1. 创建新的测试类继承测试基类
2. 添加 `@SpringBootTest` 和 `@Slf4j` 注解
3. 在 `EvaluationTestSuite` 中注册新测试类

## 最佳实践

1. **测试隔离**: 每个测试使用独立的 chatId 和数据
2. **清理资源**: 测试后清理临时数据
3. **日志记录**: 详细记录测试过程和结果
4. **断言明确**: 使用清晰的断言消息
5. **性能监控**: 记录关键操作的耗时

## 联系方式

如有问题或建议，请联系：
- 作者: jblmj
- 项目: AI Agent System
- 日期: 2026-04-19
