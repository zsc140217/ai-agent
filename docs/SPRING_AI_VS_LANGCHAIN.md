# Spring AI vs LangChain 框架对比

本文档对比分析了同一项目的两个实现版本：
- **Spring AI 版本**（本项目）：[jblmj-ai-agent](https://github.com/zsc140217/jblmj-ai-agent-master)
- **LangChain 版本**：[langchain-business-trip-management](https://github.com/zsc140217/langchain-business-trip-management)

---

## 核心架构对比

### 1. 设计模式

| 维度 | Spring AI | LangChain |
|------|-----------|-----------|
| **核心模式** | Advisor 模式（洋葱架构） | Chain 模式（流水线） |
| **组件组织** | 面向对象（类、接口、继承） | 函数式（装饰器、管道） |
| **数据流** | Request → Advisor1 → Advisor2 → ... → Response | Input → Component1 → Component2 → ... → Output |
| **扩展方式** | 实现接口 + Spring Bean 注册 | 装饰器 + 函数组合 |

**Spring AI 示例**：
```java
// Advisor 洋葱架构
ChatClient.builder(chatModel)
    .defaultAdvisors(
        new MessageChatMemoryAdvisor(chatMemory),
        new QuestionAnswerAdvisor(vectorStore),
        new PromptChatMemoryAdvisor(chatMemory)
    )
    .build();
```

**LangChain 示例**：
```python
# Chain 流水线
chain = (
    {"context": retriever, "question": RunnablePassthrough()}
    | prompt
    | llm
    | StrOutputParser()
)
```

---

### 2. 类型系统

| 维度 | Spring AI | LangChain |
|------|-----------|-----------|
| **类型安全** | 强类型（编译时检查） | 弱类型（运行时检查） |
| **IDE 支持** | 完整的代码补全和重构 | 有限的类型提示 |
| **错误发现** | 编译期 | 运行期 |
| **学习曲线** | 陡峭（需理解泛型、接口） | 平缓（动态类型） |

**Spring AI 示例**：
```java
// 编译时类型检查
ChatResponse response = chatClient.prompt()
    .user("查询天气")
    .call()
    .chatResponse();  // 类型安全

String content = response.getResult().getOutput().getContent();
```

**LangChain 示例**：
```python
# 运行时类型检查
response = chain.invoke("查询天气")  # 返回类型不确定
content = response  # 可能是 str、dict、AIMessage 等
```

---

### 3. 可观测性 ⭐ 核心差异

| 维度 | Spring AI | LangChain |
|------|-----------|-----------|
| **追踪方式** | 手动日志 + 埋点 | LangSmith 自动追踪 |
| **调用链可视化** | ❌ 无 | ✅ 树状结构展示 |
| **历史记录** | ❌ 需自建 | ✅ 永久保存 |
| **性能分析** | ❌ 手动统计 | ✅ 自动生成火焰图 |
| **成本监控** | ❌ 手动计算 Token | ✅ 自动统计成本 |
| **问题定位时间** | 半天（加日志→部署→复现） | 5分钟（点击查看历史） |

**Spring AI 调试流程**：
```java
// 需要手动添加日志
logger.info("开始查询重写");
String rewrittenQuery = queryRewriter.rewrite(query);
logger.info("查询重写结果: {}", rewrittenQuery);

logger.info("开始向量检索");
List<Document> docs = vectorStore.similaritySearch(rewrittenQuery);
logger.info("检索到 {} 个文档", docs.size());

// 需要重新部署才能看到日志
```

**LangChain 调试流程**：
```python
# 仅需 3 行配置，自动追踪所有调用
# .env 文件
LANGCHAIN_TRACING_V2=true
LANGCHAIN_API_KEY=你的API Key
LANGCHAIN_PROJECT=travel-agent

# 访问 https://smith.langchain.com/ 查看可视化调用链
# 无需修改代码，无需重新部署
```

**LangSmith 核心优势**：
- ✅ **零代码侵入**：3行环境变量配置，自动追踪所有 LangChain 调用
- ✅ **可视化调用链**：树状结构展示 RAG 流程（检索→Prompt→LLM→解析）
- ✅ **快速定位问题**：用户反馈"回答不准确" → 点击那次调用 → 发现检索器返回错误文档 → 5分钟定位
- ✅ **性能优化**：发现 Prompt 构建耗时长 → 优化后快 24%
- ✅ **成本控制**：监控 Token 消耗 → 优化后成本降低 50%

---

### 4. RAG 实现对比

#### 查询重写

**Spring AI**：
```java
@Component
public class QueryRewriter {
    private final ChatClient chatClient;
    
    public String rewrite(String query) {
        String prompt = """
            将以下口语化查询改写为结构化查询：
            原始查询：%s
            改写后的查询：
            """.formatted(query);
        
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }
}
```

**LangChain**：
```python
from langchain.prompts import PromptTemplate
from langchain_core.output_parsers import StrOutputParser

query_rewriter = (
    PromptTemplate.from_template(
        "将以下口语化查询改写为结构化查询：\n原始查询：{query}\n改写后的查询："
    )
    | llm
    | StrOutputParser()
)

rewritten = query_rewriter.invoke({"query": "去上海出差住宿"})
```

#### 混合检索

**Spring AI**：
```java
@Component
public class EnterpriseHybridRetriever {
    private final BM25Retriever bm25Retriever;
    private final VectorStore vectorStore;
    private final SimpleReranker reranker;
    
    public List<Document> retrieve(String query) {
        // 1. BM25 检索
        List<Document> bm25Docs = bm25Retriever.retrieve(query, 50);
        
        // 2. Dense 检索（原始查询）
        List<Document> denseDocs1 = vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(4)
        );
        
        // 3. Dense 检索（改写查询）
        String rewritten = queryRewriter.rewrite(query);
        List<Document> denseDocs2 = vectorStore.similaritySearch(
            SearchRequest.query(rewritten).withTopK(4)
        );
        
        // 4. RRF 融合
        List<Document> merged = rrfFusion(bm25Docs, denseDocs1, denseDocs2);
        
        // 5. 重排序
        return reranker.rerank(query, merged, 5);
    }
}
```

**LangChain**：
```python
from langchain.retrievers import EnsembleRetriever
from langchain_community.retrievers import BM25Retriever

# 1. BM25 检索器
bm25_retriever = BM25Retriever.from_documents(documents)
bm25_retriever.k = 50

# 2. Dense 检索器（原始查询）
dense_retriever_1 = vectorstore.as_retriever(search_kwargs={"k": 4})

# 3. Dense 检索器（改写查询）
dense_retriever_2 = (
    query_rewriter 
    | vectorstore.as_retriever(search_kwargs={"k": 4})
)

# 4. RRF 融合（EnsembleRetriever 内置 RRF）
ensemble_retriever = EnsembleRetriever(
    retrievers=[bm25_retriever, dense_retriever_1, dense_retriever_2],
    weights=[0.4, 0.3, 0.3]
)

# 5. 重排序（可选）
docs = ensemble_retriever.invoke(query)
reranked_docs = reranker.rerank(query, docs, top_k=5)
```

**对比总结**：
- **Spring AI**：需要手动实现 RRF 融合逻辑，代码量更多
- **LangChain**：`EnsembleRetriever` 内置 RRF，代码量更少
- **类型安全**：Spring AI 编译时检查，LangChain 运行时检查

---

### 5. 工具调用对比

#### 工具定义

**Spring AI**：
```java
@Component
public class WeatherQueryTool implements Function<WeatherRequest, String> {
    @Override
    public String apply(WeatherRequest request) {
        // 调用天气 API
        return queryWeatherApi(request.getCity());
    }
    
    @JsonClassDescription("查询指定城市的天气信息")
    public record WeatherRequest(
        @JsonProperty(required = true, value = "city")
        @JsonPropertyDescription("城市名称，如：北京、上海")
        String city
    ) {}
}
```

**LangChain**：
```python
from langchain.tools import tool

@tool
def query_weather(city: str) -> str:
    """查询指定城市的天气信息
    
    Args:
        city: 城市名称，如：北京、上海
    """
    # 调用天气 API
    return query_weather_api(city)
```

**对比总结**：
- **Spring AI**：需要定义 Record 类，类型安全但代码量多
- **LangChain**：使用装饰器，代码简洁但类型检查弱

#### 工具调用

**Spring AI**：
```java
// 注册工具
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultFunctions("weatherQueryTool")
    .build();

// 调用（LLM 自主决策是否调用工具）
String response = chatClient.prompt()
    .user("北京今天天气怎么样")
    .call()
    .content();
```

**LangChain**：
```python
# 创建 Agent
agent = create_react_agent(llm, [query_weather], prompt)
agent_executor = AgentExecutor(agent=agent, tools=[query_weather])

# 调用（LLM 自主决策是否调用工具）
response = agent_executor.invoke({"input": "北京今天天气怎么样"})
```

---

### 6. 三层记忆系统对比

#### 架构设计

**Spring AI**：
```java
@Service
public class MemoryService {
    private final ChatMemoryManager chatMemory;        // Layer 1: 短期记忆
    private final WorkingMemoryManager workingMemory;  // Layer 2: 工作记忆
    private final LongTermMemoryManager longTermMemory; // Layer 3: 长期记忆
    
    public String buildEnhancedPrompt(String userId, String conversationId, String query) {
        // 1. 获取短期记忆（最近 20 条消息）
        List<Message> history = chatMemory.getHistory(conversationId, 20);
        
        // 2. 获取工作记忆（当前会话的实体和意图）
        WorkingMemory working = workingMemory.get(conversationId);
        
        // 3. 获取长期记忆（用户画像）
        UserProfile profile = longTermMemory.getProfile(userId);
        
        // 4. 融合三层记忆生成增强提示
        return buildPrompt(query, history, working, profile);
    }
}
```

**LangChain**：
```python
class MemoryService:
    def __init__(self):
        self.chat_memory = ChatMemoryManager()        # Layer 1
        self.working_memory = WorkingMemoryManager()  # Layer 2
        self.long_term_memory = LongTermMemoryManager()  # Layer 3
    
    def build_enhanced_prompt(self, user_id: str, conversation_id: str, query: str) -> str:
        # 1. 获取短期记忆
        history = self.chat_memory.get_history(conversation_id, limit=20)
        
        # 2. 获取工作记忆
        working = self.working_memory.get(conversation_id)
        
        # 3. 获取长期记忆
        profile = self.long_term_memory.get_profile(user_id)
        
        # 4. 融合三层记忆
        return self._build_prompt(query, history, working, profile)
```

**对比总结**：
- **架构相似度**：95%（两个版本的记忆系统设计几乎一致）
- **实现差异**：Spring AI 使用文件持久化，LangChain 同样使用文件持久化
- **类型安全**：Spring AI 有编译时检查，LangChain 依赖运行时检查

---

## 性能对比

### 开发效率

| 维度 | Spring AI | LangChain |
|------|-----------|-----------|
| **代码量** | ~4500 行 Java | ~2700 行 Python |
| **开发时间** | 2 周 | 1 周 |
| **学习曲线** | 陡峭（需理解 Spring 生态） | 平缓（函数式编程） |
| **调试效率** | 低（需加日志→部署→复现） | 高（LangSmith 5分钟定位） |

### 运行性能

| 维度 | Spring AI | LangChain |
|------|-----------|-----------|
| **启动时间** | 3-5s（Spring Boot） | <1s（Python） |
| **内存占用** | 200-300MB（JVM） | 50-100MB（Python） |
| **并发性能** | 高（多线程） | 中（GIL 限制） |
| **RAG 延迟** | 3.0s（Full RAG） | 2.8s（Full RAG） |

---

## 适用场景

### Spring AI 适合

✅ **企业级应用**：需要长期维护、多人协作、严格的类型检查  
✅ **Java 技术栈**：团队熟悉 Spring Boot、Spring Cloud  
✅ **高并发场景**：需要处理大量并发请求  
✅ **金融/医疗等严肃场景**：需要强类型保证和完善的异常处理  

### LangChain 适合

✅ **快速原型验证**：需要快速验证想法、迭代实验  
✅ **AI 研究**：需要频繁调整 Prompt、模型、检索策略  
✅ **Python 技术栈**：团队熟悉 Python、数据科学工具  
✅ **可观测性要求高**：需要 LangSmith 的自动追踪和可视化  

---

## 学习建议

### 如果你是初学者

1. **先学 LangChain**：
   - 代码简洁，快速上手
   - LangSmith 可视化帮助理解 RAG 流程
   - 社区资源丰富，问题容易解决

2. **再学 Spring AI**：
   - 理解企业级应用的设计模式
   - 掌握强类型系统的优势
   - 学习 Spring 生态的最佳实践

### 如果你是 Java 开发者

1. **直接学 Spring AI**：
   - 利用现有的 Java 知识
   - 无缝对接 Spring Boot 项目
   - 适合企业级应用开发

2. **参考 LangChain 版本**：
   - 学习 LangSmith 的可观测性思路
   - 借鉴函数式编程的简洁性
   - 对比两种框架的设计哲学

### 如果你是 Python 开发者

1. **先学 LangChain**：
   - 利用现有的 Python 知识
   - 快速构建 AI 应用原型
   - 使用 LangSmith 提升调试效率

2. **了解 Spring AI**：
   - 理解企业级应用的需求
   - 学习强类型系统的优势
   - 为未来转型 Java 做准备

---

## 总结

| 维度 | Spring AI | LangChain |
|------|-----------|-----------|
| **核心优势** | 企业级稳定性、类型安全 | 开发速度快、可观测性强 |
| **核心劣势** | 学习曲线陡峭、调试困难 | 类型检查弱、运行时错误多 |
| **最佳场景** | 企业级应用、长期维护 | 快速原型、AI 研究 |
| **推荐指数** | ⭐⭐⭐⭐（企业） | ⭐⭐⭐⭐⭐（研究） |

**核心观点**：
- 没有绝对的"更好"，只有"更适合"
- Spring AI 适合企业级应用，LangChain 适合快速原型
- **最佳实践**：两个版本都学习，深入理解不同框架的设计哲学

---

## 相关资源

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [LangChain 官方文档](https://python.langchain.com/)
- [LangSmith 可观测性平台](https://smith.langchain.com/)
- [本项目 Spring AI 版本](https://github.com/zsc140217/jblmj-ai-agent-master)
- [本项目 LangChain 版本](https://github.com/zsc140217/langchain-business-trip-management)
