# RAG 面试速查卡

## 核心问题速答

### Q1: 为什么"是这样"会召回"不是这样"？

**30秒版本**：
向量检索基于余弦相似度，"不能住五星"和"可以住五星"词汇重叠75%，语义空间距离近（相似度0.85+），而 Embedding 模型对否定词不敏感。

**解决方案**：查询改写（检测否定词） + LLM 理解 + 重排序

---

### Q2: 为什么要用模型来 Embedding？

**30秒版本**：
传统方法（TF-IDF、Word2Vec）无法理解同义词、丢失上下文、无法迁移知识。深度学习模型（BERT、Sentence-BERT）通过上下文感知、预训练知识迁移、任务适配，检索精度提升20-40%。

**本项目选择**：DashScope text-embedding-v3（中文优化 + 云服务 + 快速落地）

---

### Q3: 不同 Embedding 模型有什么区别？

**5个维度**：
1. **架构**：BERT（双向）vs GPT（单向）vs Sentence-BERT（孪生网络）
2. **训练数据**：通用语料 vs 领域语料 vs 中文语料
3. **训练目标**：对比学习 vs 掩码语言模型 vs NLI
4. **向量维度**：512（快）vs 1024（平衡）vs 1536（精度高）
5. **特殊能力**：长文本 vs 跨语言 vs 否定词敏感

**主流模型**：
- 国际：text-embedding-ada-002（OpenAI）、e5-mistral（Microsoft）
- 国内：text-embedding-v3（阿里云）、bge-large-zh（智源）、gte-large-zh（阿里）

---

## 项目亮点速记

### RAG 优化成果

| 指标 | Baseline | 优化后 | 提升 |
|------|---------|--------|------|
| 准确率 | 40% | 80% | +100% |
| 工具调用率 | 0% | 100% | +100% |
| 平均延迟 | - | 7.5s | - |

### 核心技术栈

```
RAG Pipeline:
├─ 查询改写（QueryRewriter）
│  └─ 否定词检测 + LLM 重写
├─ 向量检索（SimpleVectorStore）
│  └─ DashScope text-embedding-v3
├─ 元数据增强（MyKeywordEnricher）
│  └─ 城市等级 + 费用类型
└─ LLM 生成（QuestionAnswerAdvisor）
   └─ Qwen-Plus + 上下文注入
```

### 测试覆盖

- **RAG 评测**：30 个测试用例（含 5 个否定查询）
- **否定查询专项**：6 个测试方法
- **复杂度评估**：5 个天气查询测试
- **性能压测**：延迟 + 吞吐量基准

---

## 技术细节速查

### 否定查询处理流程

```
用户查询："北京出差不能住五星级酒店吗"
    ↓
检测否定词（正则匹配）
    ↓
LLM 重写："北京出差住宿标准 不能住五星级酒店"
    ↓
向量检索（Top-5）
    ↓
LLM 生成（理解否定语义）
    ↓
响应："北京住宿标准为四星及以下，500元/晚，不能住五星级酒店"
```

### 查询改写示例

| 原始查询 | 重写后 | 目的 |
|---------|--------|------|
| 去魔都出差 | 去上海出差 | 口语化 → 标准化 |
| 住宿能报多少 | 住宿费用报销标准 | 提取核心意图 |
| 不能住五星吗 | 住宿标准 不能住五星 | 保留否定语义 |

### 元数据增强示例

```markdown
# 原始文档
一类城市（北京、上海）住宿标准：500元/晚

# 增强后
一类城市（北京、上海）住宿标准：500元/晚
[元数据]
- 城市等级：一类城市
- 费用类型：住宿费用
- 金额：500元
- 城市：北京,上海
```

---

## 常见追问及应对

### Q: 如果召回的文档都是错的怎么办？

**A**: 三层防御：
1. **查询改写**：提高召回精度（40% → 80%）
2. **元数据过滤**：城市等级、费用类型精确匹配
3. **重排序**：用 LLM 对召回结果重新打分（未实现，可作为优化方向）

### Q: 为什么不用 PgVector？

**A**: 
- 当前用 SimpleVectorStore（内存）快速验证
- PgVector 支持已准备（pom.xml 已配置，注释掉）
- 生产环境建议切换到 PgVector（持久化 + 分布式）

### Q: 如何评估 RAG 效果？

**A**: 三个维度：
1. **准确率**：25 个标准测试用例，人工标注预期答案
2. **召回率**：检查关键信息是否被召回
3. **延迟**：平均响应时间 7.5s（可接受）

### Q: 如果要支持 100 万文档怎么办？

**A**: 
1. **向量数据库**：切换到 PgVector / Milvus / Weaviate
2. **索引优化**：HNSW / IVF 索引
3. **分片策略**：按城市 / 费用类型分片
4. **缓存**：热点查询结果缓存（Redis）

### Q: 为什么不用 LangChain？

**A**: 
- 本项目用 Spring AI（Java 生态）
- Spring AI 提供类似能力：ChatClient、QuestionAnswerAdvisor、VectorStore
- 更适合 Spring Boot 项目集成

### Q: 如何处理知识更新？

**A**: 
1. **增量更新**：新增文档 → Embedding → 写入向量库
2. **全量重建**：定期重建索引（凌晨执行）
3. **版本管理**：文档版本号 + 时间戳

---

## 代码位置速查

| 功能 | 文件 | 行数 |
|------|------|------|
| 查询改写 | [QueryRewriter.java](../src/main/java/com/jblmj/aiagent/rag/QueryRewriter.java) | 40-90 |
| 否定词检测 | QueryRewriter.java | 25-27 |
| 元数据增强 | [MyKeywordEnricher.java](../src/main/java/com/jblmj/aiagent/rag/MyKeywordEnricher.java) | 全文 |
| RAG 应用 | [EnterpriseAssistantApp.java](../src/main/java/com/jblmj/aiagent/app/EnterpriseAssistantApp.java) | 全文 |
| 否定查询测试 | [NegationQueryTest.java](../src/test/java/com/jblmj/aiagent/evaluation/NegationQueryTest.java) | 全文 |
| RAG 评测 | [RAGEvaluationTest.java](../src/test/java/com/jblmj/aiagent/evaluation/RAGEvaluationTest.java) | 全文 |
| 测试用例 | [rag_test_cases.json](../src/test/resources/evaluation/rag_test_cases.json) | 1-252 |

---

## 演示命令速查

### 启动应用

```bash
# Windows
./run-backend.bat

# Maven
./mvnw spring-boot:run
```

### 运行测试

```bash
# 否定查询测试
./mvnw test -Dtest=NegationQueryTest

# RAG 完整评测
./mvnw test -Dtest=RAGEvaluationTest

# 所有评测
./mvnw test -Dtest=EvaluationTestSuite
```

### API 测试

```bash
# 健康检查
curl http://localhost:8123/api/health

# RAG 查询（同步）
curl "http://localhost:8123/api/ai/enterprise/chat/sync?message=北京出差不能住五星级酒店吗&chatId=test123"

# RAG 查询（SSE 流式）
curl -N "http://localhost:8123/api/ai/enterprise/chat/sse?message=北京出差住宿标准&chatId=test123"
```

---

## 数据示例速查

### 测试用例示例

```json
{
  "id": 26,
  "query": "北京出差不能住五星级酒店吗",
  "difficulty": "hard",
  "category": "否定查询",
  "city_level": "一类城市",
  "expected_keywords": ["不能", "不可以", "四星及以下", "500元"],
  "expected_answer_contains": "不能",
  "should_not_contain": ["可以住五星", "允许五星"]
}
```

### 知识库文档示例

```markdown
# TravelPolicy.md

## 住宿费用标准

### 一类城市
- 城市：北京、上海、广州、深圳、杭州、成都
- 标准：500元/晚
- 要求：四星级及以下酒店

### 二类城市
- 城市：省会城市及计划单列市
- 标准：350元/晚
- 要求：三星级及以下酒店
```

---

## 优化方向速查

### 短期优化（1-2周）

1. ✅ 否定查询处理（已完成）
2. ⏳ 重排序模块（bge-reranker）
3. ⏳ 查询缓存（Redis）
4. ⏳ 切换到 PgVector

### 中期优化（1-2月）

1. ⏳ 模型微调（bge-large-zh + 企业数据）
2. ⏳ 混合检索（向量 + BM25）
3. ⏳ 多路召回（不同模型融合）
4. ⏳ A/B 测试框架

### 长期优化（3-6月）

1. ⏳ 知识图谱集成
2. ⏳ 多模态检索（文档 + 图片）
3. ⏳ 个性化推荐
4. ⏳ 实时学习（用户反馈）

---

## 面试话术模板

### 开场介绍

"我做的是企业差旅 AI 助手，基于 RAG 架构，解决员工差旅政策查询和行程规划问题。核心亮点是通过复杂度评估框架，解决了弱模型工具调用不稳定的问题，实现了 100% 的工具调用成功率。同时通过查询改写和元数据增强，将 RAG 准确率从 40% 提升到 80%。"

### 技术深度展示

"在 RAG 优化上，我遇到了一个有意思的问题：用户查询'不能住五星级酒店吗'，系统会错误召回'可以住五星级酒店'的文档。这是因为向量检索基于余弦相似度，两个句子词汇重叠高，而 Embedding 模型对否定词不敏感。

我的解决方案是：
1. 在查询改写阶段检测否定词
2. 用 LLM 重写查询，保留否定语义
3. 依赖 LLM 生成阶段理解否定逻辑

这个方案在 30 个测试用例中验证有效，包括 5 个专门的否定查询测试。"

### 结尾升华

"这个项目让我深刻理解了 RAG 不是简单的'检索+生成'，而是需要在查询理解、文档召回、结果排序每个环节做优化。未来我还想尝试重排序、模型微调、混合检索等方向，进一步提升效果。"

---

## 关键数字记忆

- **80%**：RAG 准确率（相比 Baseline 提升 100%）
- **100%**：工具调用成功率（弱模型场景）
- **7.5s**：平均响应延迟
- **30**：测试用例数量（含 5 个否定查询）
- **500元**：一类城市住宿标准
- **350元**：二类城市住宿标准
- **1536**：text-embedding-v3 向量维度
- **0.85**：否定句对的相似度（问题根源）

---

## 最后检查清单

面试前 5 分钟：

- [ ] 能说出 RAG 的 3 个核心问题（召回、排序、生成）
- [ ] 能解释否定查询问题的根本原因
- [ ] 能说出 3 种 Embedding 模型的差异
- [ ] 能说出项目的 2 个核心指标（80% 准确率、100% 工具调用率）
- [ ] 能说出 2 个优化方向（重排序、模型微调）
- [ ] 记住关键代码位置（QueryRewriter、NegationQueryTest）
- [ ] 准备好演示命令（启动应用、运行测试）

---

**祝面试顺利！🚀**
