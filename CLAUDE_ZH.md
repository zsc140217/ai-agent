# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供在此代码库中工作的指导。

## 项目概述

基于 Spring AI 1.0 构建的企业差旅智能体平台，通过 RAG + MCP + ReAct 架构解决企业差旅规章问答与行程规划场景。系统通过复杂度评估框架解决了弱模型工具调用能力不足的问题，在所有国产大模型上实现 100% 工具调用率。

**技术栈**: Spring Boot 3.4, Spring AI 1.0, 阿里云百炼 (通义千问), Java 21, Maven

## 构建与运行命令

### 启动应用

```bash
# Windows（推荐）
./run-backend.bat

# 直接使用 Maven
./mvnw spring-boot:run

# 构建 JAR 包
./mvnw clean package
java -jar target/yu-ai-agent-0.0.1-SNAPSHOT.jar
```

后端运行在 `http://localhost:8123/api`

### 运行测试

```bash
# 运行所有测试
./mvnw test

# 运行特定测试类
./mvnw test -Dtest=RAGEvaluationTest

# 运行评测套件
./mvnw test -Dtest=EvaluationTestSuite

# 运行特定评测
./mvnw test -Dtest=AccuracyQualityTest        # RAG 准确率测试
./mvnw test -Dtest=ComplexityFrameworkTest    # 工作流编排测试
./mvnw test -Dtest=PerformanceStressTest      # 性能测试
```

### API 接口

```bash
# 健康检查
curl http://localhost:8123/api/health

# 同步对话
curl "http://localhost:8123/api/ai/enterprise/chat/sync?message=去上海出差住宿标准&chatId=test123"

# SSE 流式对话（推荐）
curl -N "http://localhost:8123/api/ai/enterprise/chat/sse?message=帮我规划明天去杭州的行程&chatId=test123"

# ReAct Agent 演示
curl -N "http://localhost:8123/api/ai/manus/chat?message=查询公司到虹桥机场的距离"

# Swagger 文档
open http://localhost:8123/api/swagger-ui.html
```

## 架构概览

### 核心组件

**WorkflowOrchestrator** ([WorkflowOrchestrator.java](src/main/java/com/jblmj/aiagent/app/WorkflowOrchestrator.java))
- 中央路由引擎，编排所有查询处理流程
- 路由策略：Skill 优先 → 复杂度评估降级
- 处理 SIMPLE（单次工具调用）、MEDIUM（多次调用）、COMPLEX（任务分解 + 并行执行）

**Skill 系统** ([src/main/java/com/jblmj/aiagent/skill/](src/main/java/com/jblmj/aiagent/skill/))
- 面向用户的任务单元（一个任务 = 一个 Skill）
- 通过 `@SkillComponent` 注解自动注册
- 当前 Skill：`WeatherQuerySkill`（天气查询）、`TravelPlanningSkill`（行程规划）
- Skill 内部调用 Service（框架能力）和 Tool（原子操作）

**ComplexityAssessor** ([ComplexityAssessor.java](src/main/java/com/jblmj/aiagent/service/ComplexityAssessor.java))
- 混合判断：80% 规则判断（快速），20% LLM 判断（准确）
- 将查询分类为 SIMPLE/MEDIUM/COMPLEX
- 通过预编排工作流实现 100% 工具调用率

**TaskDecomposer** ([TaskDecomposer.java](src/main/java/com/jblmj/aiagent/service/TaskDecomposer.java))
- 将复杂查询分解为结构化子任务（JSON 格式）
- 支持任务依赖关系和拓扑排序
- 通过 CompletableFuture 实现独立任务并行执行
- 包含循环依赖检测

**RAG 管道** ([src/main/java/com/jblmj/aiagent/rag/](src/main/java/com/jblmj/aiagent/rag/))
- Query Rewriting：将口语化查询转换为结构化搜索
- Metadata Enrichment：预标注文档的城市等级、费用类型等元数据
- 使用内存向量库 SimpleVectorStore（PgVector 支持已注释）
- 准确率达到 80%（相比基线提升 40%）

**EnterpriseAssistantApp** ([EnterpriseAssistantApp.java](src/main/java/com/jblmj/aiagent/app/EnterpriseAssistantApp.java))
- 主要的 RAG 对话应用
- 通过向量检索处理差旅政策查询
- 支持 SSE 流式响应

### 分层架构

```
Skill 层（用户任务）
  ↓ 调用
Service 层（框架能力：ComplexityAssessor、TaskDecomposer）
  ↓ 调用
Tool 层（原子操作：WeatherQueryTool、CLI 工具、MCP 客户端）
```

**重要说明**：ComplexityAssessor 和 TaskDecomposer 是 Service，不是 Skill。Skill 是面向用户的任务，如"查询天气"或"规划行程"。

## 配置说明

### 必需的 API Key

编辑 [src/main/resources/application.yml](src/main/resources/application.yml)：

```yaml
spring:
  ai:
    dashscope:
      api-key: YOUR_DASHSCOPE_API_KEY  # 从 https://dashscope.aliyun.com/ 获取

qweather:
  api-key: YOUR_QWEATHER_API_KEY      # 从 https://dev.qweather.com/ 获取
```

### 模型配置

默认模型：`qwen-plus-2025-07-28`

修改模型，更新 `application.yml`：
```yaml
spring:
  ai:
    dashscope:
      chat:
        options:
          model: qwen-plus-2025-07-28  # 或 qwen-turbo、qwen-max
```

### 向量存储

当前使用内存向量库 `SimpleVectorStore`。PgVector 支持已在 [pom.xml](pom.xml) 和 [application.yml](src/main/resources/application.yml) 中注释。

启用 PgVector：
1. 在 `pom.xml` 中取消注释 PostgreSQL 依赖
2. 在 `application.yml` 中取消注释数据源配置
3. 在 `LoveAppVectorStoreConfig.java` 中切换向量存储 Bean

## 开发指南

### 添加新 Skill

1. 创建实现 `Skill` 接口的类
2. 使用 `@SkillComponent(name, description, keywords)` 注解
3. 实现 `canHandle()` 进行关键词匹配
4. 实现 `execute()` 编写业务逻辑
5. Skill 在启动时通过 `SkillRegistry` 自动注册

示例：
```java
@SkillComponent(
    name = "hotel_booking",
    description = "预订差旅酒店",
    keywords = {"酒店", "预订", "住宿"}
)
public class HotelBookingSkill implements Skill {
    public String execute(String query, String chatId) {
        // 调用 Service 和 Tool
    }
}
```

### 添加新 Tool

Tool 是原子操作（API 调用、CLI 命令、数据库查询）：

1. 创建带 `@Component` 注解的类
2. 实现单一用途的方法
3. Tool 由 Skill 或 Service 调用

示例：[WeatherQueryTool.java](src/main/java/com/jblmj/aiagent/tools/WeatherQueryTool.java)

### 使用 RAG 文档

文档位于 [src/main/resources/document/](src/main/resources/document/)：
- `TravelPolicy.md` - 企业差旅政策
- `CustomerList.md` - 客户信息
- `PreferredHotels.md` - 酒店推荐
- `Transportation_Guide.md` - 交通指南

更新 RAG 知识库：
1. 修改 `document/` 目录中的 Markdown 文件
2. 重启应用（向量库在启动时重建）
3. 使用相关查询测试

### 任务分解

在 `TaskDecomposer` 中添加新任务类型：

1. 在 `buildDecomposePrompt()` 的提示词中添加任务类型
2. 在 `executeSubTask()` 的 switch 语句中添加处理逻辑
3. 确保任务参数可 JSON 序列化
4. 使用复杂多意图查询测试

### 复杂度评估

调整 `ComplexityAssessor` 中的复杂度阈值：

- SIMPLE：单一意图，单次工具调用（如"北京天气"）
- MEDIUM：单一意图，多次工具调用（如"上海vs广州天气对比"）
- COMPLEX：多意图需要分解（如"去深圳出差，查天气和推荐酒店"）

修改 `assessByRule()` 方法中的关键词计数逻辑。

## 测试策略

### 评测测试

位于 [src/test/java/com/jblmj/aiagent/evaluation/](src/test/java/com/jblmj/aiagent/evaluation/)：

- `RAGEvaluationTest` - 25 条差旅政策问答测试用例
- `ComplexityFrameworkTest` - 5 条天气查询测试用例，验证工具调用
- `PerformanceStressTest` - 延迟和吞吐量基准测试
- `SystemIntegrationTest` - 端到端集成测试

测试数据在 [src/test/resources/evaluation/](src/test/resources/evaluation/)

### 运行评测

```bash
# 完整评测套件
./mvnw test -Dtest=EvaluationTestSuite

# 单独评测
./mvnw test -Dtest=RAGEvaluationTest
./mvnw test -Dtest=ComplexityFrameworkTest
```

结果会记录以下指标：
- RAG 准确率
- 工具调用率（目标：100%）
- 复杂度评估准确率
- 平均响应延迟

## 关键设计决策

### 为什么 Skill 优先路由？

Skill 为常见任务提供稳定、可预测的行为。复杂度评估是不匹配任何 Skill 模式的查询的降级方案。这种混合方法在灵活性（LLM 决策）和可靠性（预编排工作流）之间取得平衡。

### 为什么混合复杂度评估？

基于规则的评估快速（<1ms）但准确性较低。基于 LLM 的评估准确但慢（1-2s）。混合方法对 80% 的情况使用规则，仅对 COMPLEX 查询使用 LLM 确认，实现 90% 准确率和 <500ms 延迟。

### 为什么需要任务依赖？

像"查询客户地址并规划路线"这样的复杂查询需要顺序执行（必须先获取地址再规划路线）。依赖系统支持拓扑排序和独立任务并行执行，同时尊重依赖关系。

### 为什么不用完全的 LLM 工具调用？

弱模型（通义千问、国产 LLM）在注册多个工具时工具调用可靠性差。复杂度框架通过使用代码控制的工作流而不是依赖 LLM 决策，实现 100% 工具调用率。

## 常见问题

### 应用无法启动

- 检查 JDK 版本：需要 JDK 21（或最低 17）
- 验证 `application.yml` 中的 API Key
- 如果使用 PgVector，确保 PostgreSQL 正在运行

### RAG 返回错误结果

- 检查查询是否需要重写（口语化 → 结构化）
- 验证 `document/*.md` 文件中的文档元数据
- 增加 RAG 检索的上下文窗口（默认：top 5 chunks）

### 工具未被调用

- 检查查询是否匹配 `canHandle()` 中的 Skill 关键词
- 验证复杂度评估分类是否正确
- 检查工具在 Spring 上下文中的注册

### 测试失败

- 确保配置了 API Key
- 检查外部 API 调用的网络连接（天气、LLM）
- 某些测试需要特定的模型响应 - 不同模型可能需要调整

## 项目结构

```
src/main/java/com/jblmj/aiagent/
├── app/                    # 主应用
│   ├── WorkflowOrchestrator.java    # 中央路由引擎
│   └── EnterpriseAssistantApp.java  # RAG 对话应用
├── skill/                  # Skill 系统
│   ├── Skill.java                   # Skill 接口
│   ├── SkillRegistry.java           # 自动注册
│   └── business/                    # 业务 Skill
├── service/                # 框架服务
│   ├── ComplexityAssessor.java      # 查询复杂度评估
│   └── TaskDecomposer.java          # 任务分解
├── tools/                  # 原子工具
│   └── WeatherQueryTool.java        # 天气 API 集成
├── rag/                    # RAG 管道
│   ├── QueryRewriter.java           # 查询重写
│   └── MyKeywordEnricher.java       # 元数据增强
├── agent/                  # Agent 实现
│   ├── ReActAgent.java              # ReAct 模式 Agent
│   └── JblmjManus.java              # 自定义 Agent
├── controller/             # REST 控制器
├── model/                  # 数据模型
└── config/                 # 配置

src/main/resources/
├── document/               # RAG 知识库（Markdown）
├── application.yml         # 主配置
└── mcp-servers.json        # MCP 服务器配置

src/test/java/com/jblmj/aiagent/
└── evaluation/             # 评测测试套件
```

## 性能特征

基于通义千问 Plus 模型的评测结果：

- RAG 准确率：80%（相比基线提升 40%）
- 工具调用率：100%（纯 LLM 工具调用为 0%）
- 平均延迟：7.5s（完整 RAG），9.4s（含工具调用）
- 瓶颈：LLM API 调用（占总延迟的 75%）

优化方向：
- Prompt 压缩
- 简单任务模型降级（qwen-turbo）
- 尽可能并行调用工具
