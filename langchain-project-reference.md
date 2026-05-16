# 在 LangChain 项目中添加的内容

## 位置：README.md 的 "相关文档" 章节之前

---

## 🔗 相关项目

### Spring AI 版本实现
本项目有一个对应的 **Spring AI (Java) 版本**，实现了相同的核心功能，可用于框架对比学习：

**仓库地址**：[jblmj-ai-agent](https://github.com/zsc140217/jblmj-ai-agent-master)

**核心差异对比**：

| 维度 | LangChain (本项目) | Spring AI 版本 |
|------|-------------------|---------------|
| **语言** | Python 3.10+ | Java 21 |
| **架构模式** | Chain 模式（流水线） | Advisor 模式（洋葱架构） |
| **类型安全** | 弱类型（运行时检查） | 强类型（编译时检查） |
| **可观测性** | LangSmith 自动追踪 ⭐ | 日志 + 手动埋点 |
| **学习曲线** | 平缓（函数式编程） | 陡峭（需理解 Spring 生态） |
| **适用场景** | 快速原型、研究实验 | 企业级应用、长期维护 |
| **Skill 系统** | ⏳ 待实现 | ✅ 已实现（注解自动注册） |
| **三层记忆系统** | ✅ 已实现 | ✅ 已实现 |
| **混合检索** | ✅ BM25+Dense+RRF | ✅ BM25+Dense+重排序 |

**LangChain 版本的独特优势（本项目）**：
- **LangSmith 可观测性** ⭐⭐⭐⭐⭐：零代码侵入，自动追踪所有调用链，可视化树状结构，5分钟定位问题
- **开发速度快**：函数式编程风格，代码量约为 Spring AI 版本的 60%
- **生态丰富**：原生支持 100+ 工具和集成
- **学习曲线平缓**：适合快速上手和实验

**Spring AI 版本的独特优势**：
- **企业级稳定性**：强类型、编译时检查、完善的异常处理
- **Spring 生态集成**：无缝对接 Spring Boot、Spring Security、Spring Cloud
- **Skill 架构**：标准化的任务定义和自动注册机制
- **高并发性能**：JVM 多线程优势

**学习建议**：
- 如果你是 **Python 开发者**或需要 **快速验证想法**，推荐本项目（LangChain）
- 如果你是 **Java 开发者**或需要 **企业级应用**，推荐 Spring AI 版本
- **最佳实践**：两个版本都学习，深入理解不同框架的设计哲学

**详细对比文档**：
- [Spring AI vs LangChain 完整对比](https://github.com/zsc140217/jblmj-ai-agent-master/blob/main/docs/SPRING_AI_VS_LANGCHAIN.md)

---

## 📚 相关文档

### 核心文档
- [Spring AI vs LangChain对比](docs/SPRING_AI_VS_LANGCHAIN.md)
- [实现指南](docs/IMPLEMENTATION_GUIDE.md)
- [Spring AI深度分析](docs/SPRING_AI_ANALYSIS.md)
- [三层记忆系统设计](docs/MEMORY_SYSTEM.md)
- [API文档](docs/API_DOCS.md)
- [项目总结](PROJECT_SUMMARY.md)

### 面试准备文档
- [Spring AI vs LangChain面试指南](docs/SPRING_AI_VS_LANGCHAIN_INTERVIEW_GUIDE.md)
- [面试速查卡](docs/INTERVIEW_CHEAT_SHEET.md)
- [LangSmith实战指南](docs/LANGSMITH_PRACTICAL_GUIDE.md)
- [LangSmith快速开始](LANGSMITH_QUICKSTART.md)

### 相关资源
- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [LangChain 官方文档](https://python.langchain.com/)
- [LangSmith 可观测性平台](https://smith.langchain.com/)
- [本项目 LangChain 版本](https://github.com/zsc140217/langchain-business-trip-management)
- [本项目 Spring AI 版本](https://github.com/zsc140217/jblmj-ai-agent-master)

---
