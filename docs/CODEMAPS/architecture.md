# 架构地图

## 核心模块

### 1. 工作流编排层 (app/)
- `WorkflowOrchestrator.java` - 中央路由引擎，协调Skill和Service
- `EnterpriseAssistantApp.java` - RAG聊天应用主入口

### 2. Skill层 (skill/)
用户任务单元，一个任务对应一个Skill
- `Skill.java` - Skill接口定义
- `SkillRegistry.java` - 自动注册机制（@SkillComponent扫描）
- `business/WeatherQuerySkill.java` - 天气查询任务
- `business/TravelPlanningSkill.java` - 行程规划任务

### 3. Service层 (service/)
框架能力，被Skill调用
- `ComplexityAssessor.java` - 查询复杂度评估（SIMPLE/MEDIUM/COMPLEX）
- `TaskDecomposer.java` - 复杂任务分解与并行执行

### 4. Tool层 (tools/)
原子操作，被Service和Skill调用
- `WeatherQueryTool.java` - 和风天气API集成
- CLI工具、MCP客户端等

### 5. RAG层 (rag/)
检索增强生成管道
- `QueryRewriter.java` - 查询重写（口语化→结构化）
- `NegationQueryHandler.java` - 否定查询处理
- `MyKeywordEnricher.java` - 元数据增强（城市等级、费用类型）
- `HybridRetriever.java` - 三路召回（向量+BM25+元数据）

### 6. 记忆系统 (chatmemory/)
三层记忆架构（Phase 2新增）
- `ShortTermMemory.java` - 短期记忆（文件存储，滑动窗口20条）
- `WorkingMemory.java` - 工作记忆（实体提取+意图跟踪）
- `LongTermMemory.java` - 长期记忆（用户画像学习）
- `MemoryOrchestrator.java` - 记忆系统协调器

### 7. 监控层 (monitor/)
JVM性能监控（Phase 2新增）
- `JVMMetricsCollector.java` - JVM指标收集器（10秒间隔）
- `MonitorController.java` - 监控REST API

### 8. Agent层 (agent/)
- `ReActAgent.java` - ReAct模式Agent
- `JblmjManus.java` - 自定义Agent实现

## 数据流

```
用户查询 → WorkflowOrchestrator
  ↓
Skill匹配（关键词）
  ↓ (未匹配)
ComplexityAssessor（复杂度评估）
  ↓
SIMPLE → 单工具调用
MEDIUM → 多工具调用  
COMPLEX → TaskDecomposer → 并行执行
```

## RAG检索流程

```
用户查询
  ↓
QueryRewriter（查询重写）
  ↓
NegationQueryHandler（否定查询检测）
  ↓
HybridRetriever（三路召回）
  ├─ VectorStore（语义相似度）
  ├─ BM25（关键词匹配）
  └─ MetadataFilter（元数据过滤）
  ↓
Reranker（重排序）
  ↓
LLM生成答案
```

## 记忆系统流程

```
用户消息
  ↓
ShortTermMemory（保存对话历史）
  ↓
WorkingMemory（提取实体+意图）
  ↓
对话结束后
  ↓
LongTermMemory（学习用户偏好）
```

## 配置文件

- `application.yml` - 主配置（API密钥、模型配置、向量存储）
- `document/*.md` - RAG知识库（差旅政策、客户列表、酒店推荐）
- `mcp-servers.json` - MCP服务器配置
- `.claude/hooks.json` - Git hooks配置（Phase 2新增）
- `.claude/settings.local.json` - 权限配置（Phase 2新增）

## 关键设计决策

1. **Skill优先路由** - 常见任务走预定义Skill，保证稳定性
2. **混合复杂度评估** - 80%规则（快速）+ 20% LLM（准确）
3. **三路召回** - 向量+BM25+元数据，提升RAG准确率至80%
4. **任务依赖拓扑排序** - 支持复杂任务的并行执行
5. **文件存储记忆** - 避免数据库依赖，简化部署
