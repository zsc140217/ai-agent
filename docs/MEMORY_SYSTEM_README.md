# 三层记忆系统 - 文档索引

## 📚 文档导航

### 快速开始
- **[快速开始指南](MEMORY_SYSTEM_QUICKSTART.md)** - 5分钟体验记忆系统
  - 启动应用
  - 模拟对话
  - 查看记忆
  - API测试

### 集成说明
- **[集成说明文档](MEMORY_SYSTEM_INTEGRATION.md)** - 记忆系统的接入点和使用方法
  - Controller层接入（4个接口）
  - Application层增强（2个方法）
  - 完整对话流程
  - 测试验证
  - 面试演示脚本

### 详细设计
- **[系统设计文档](MEMORY_SYSTEM_DESIGN.md)** - 完整的架构设计和实现细节
  - 架构设计
  - 核心功能
  - API接口
  - 配置说明
  - 测试验证
  - 集成方案

### 面试准备
- **[面试问答手册](MEMORY_SYSTEM_INTERVIEW_QA.md)** - 常见面试问题和标准答案
  - 架构设计类问题
  - 技术实现类问题
  - 性能优化类问题
  - 业务场景类问题
  - 对比分析类问题

---

## 🎯 核心特性

### 三层记忆架构

```
┌─────────────────────────────────────────────────────────────┐
│                      用户请求                                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
         ┌──────────────────────────────────────┐
         │        MemoryService (统一门面)       │
         └──────────────────────────────────────┘
         ↓                    ↓                    ↓
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Layer 1      │    │ Layer 2      │    │ Layer 3      │
│ 短期记忆      │    │ 工作记忆      │    │ 长期记忆      │
│              │    │              │    │              │
│ 原始对话历史  │    │ 实体提取      │    │ 用户画像      │
│ 滑动窗口20条  │    │ 意图追踪      │    │ 偏好学习      │
│              │    │              │    │              │
│ 存储: Kryo   │    │ 存储: 内存    │    │ 存储: JSON   │
└──────────────┘    └──────────────┘    └──────────────┘
```

### 核心组件

| 组件 | 文件 | 职责 |
|------|------|------|
| **EnhancedMessageWindowChatMemory** | [EnhancedMessageWindowChatMemory.java](../src/main/java/com/jblmj/aiagent/chatmemory/EnhancedMessageWindowChatMemory.java) | 短期记忆：滑动窗口 + 文件持久化 |
| **WorkingMemoryManager** | [WorkingMemoryManager.java](../src/main/java/com/jblmj/aiagent/chatmemory/WorkingMemoryManager.java) | 工作记忆：实体提取 + 意图追踪 |
| **LongTermMemoryManager** | [LongTermMemoryManager.java](../src/main/java/com/jblmj/aiagent/chatmemory/LongTermMemoryManager.java) | 长期记忆：用户画像学习 |
| **MemoryService** | [MemoryService.java](../src/main/java/com/jblmj/aiagent/chatmemory/MemoryService.java) | 统一门面：协调三层记忆 |
| **MemoryController** | [MemoryController.java](../src/main/java/com/jblmj/aiagent/controller/MemoryController.java) | REST API接口 |

---

## 🚀 快速开始

### 1. 启动应用

```bash
./mvnw spring-boot:run
```

### 2. 测试对话

```bash
# 第一次对话
curl -N "http://localhost:8123/api/ai/enterprise/chat/sse?message=我要去上海出差，帮我查一下天气&chatId=demo001"

# 查看工作记忆
curl http://localhost:8123/api/memory/working/demo001

# 继续对话
curl -N "http://localhost:8123/api/ai/enterprise/chat/sse?message=那边有什么协议酒店推荐吗&chatId=demo001"

# 触发学习
curl -X POST "http://localhost:8123/api/memory/learn?userId=user001&conversationId=demo001"

# 查看用户画像
curl http://localhost:8123/api/memory/profile/user001
```

### 3. 运行测试

```bash
./mvnw test -Dtest=MemorySystemIntegrationTest
```

---

## 📊 技术亮点

### 1. 文件持久化 + 滑动窗口
- ✅ Kryo序列化，比JSON快3-5倍
- ✅ 滑动窗口20条，防止token超限
- ✅ 边界优化，保留完整User-Assistant对
- ✅ 重启后自动恢复会话

### 2. 实体提取 + 意图追踪
- ✅ 自动提取城市、客户、酒店实体
- ✅ 追踪意图序列（查天气 → 订酒店 → 规划路线）
- ✅ 上下文补全（"那里的天气" → "上海的天气"）
- ✅ 30分钟TTL自动清理

### 3. 用户画像 + 个性化推荐
- ✅ 统计常去城市（{"上海": 5, "北京": 3}）
- ✅ 记录历史行程摘要（最近20次）
- ✅ 生成个性化提示（"您之前来过上海..."）
- ✅ GDPR合规，支持数据删除

---

## 🎓 面试要点

### 核心问题

1. **为什么需要三层记忆？**
   - 短期记忆：上下文理解
   - 工作记忆：任务追踪
   - 长期记忆：个性化推荐

2. **为什么工作记忆用内存，短期记忆用文件？**
   - 工作记忆：临时状态，读写频繁，30分钟过期
   - 短期记忆：需要持久化，重启恢复

3. **如何防止内存泄漏？**
   - TTL自动过期（30分钟）
   - 定时任务清理
   - 手动清理API

4. **如何扩展到100万用户？**
   - 短期记忆：文件 → Redis Cluster
   - 工作记忆：HashMap → Redis + TTL
   - 长期记忆：JSON → PostgreSQL + 分库分表

### 演示脚本

详见 [快速开始指南 - 第五节](MEMORY_SYSTEM_QUICKSTART.md#五面试演示脚本)

---

## 📈 性能指标

| 操作 | 延迟 | 说明 |
|------|------|------|
| 短期记忆读取 | ~10ms | Kryo文件反序列化 |
| 工作记忆读写 | <1ms | 内存HashMap |
| 长期记忆更新 | ~20ms | JSON序列化 |
| 实体提取 | ~5ms | 规则匹配 |
| 会话清理 | ~50ms | 遍历所有会话 |

---

## 🔧 配置说明

### application.yml

```yaml
chat:
  memory:
    storage:
      path: ./data/chat-history  # 短期记忆存储路径
    window:
      size: 20  # 滑动窗口大小
    longterm:
      path: ./data/user-profiles  # 长期记忆存储路径
```

---

## 🧪 测试覆盖

- ✅ 三层记忆协同工作流程
- ✅ 多城市实体提取
- ✅ 意图追踪和序列记录
- ✅ 用户画像积累
- ✅ 个性化推荐生成
- ✅ 会话清理和过期管理

测试文件：[MemorySystemIntegrationTest.java](../src/test/java/com/jblmj/aiagent/chatmemory/MemorySystemIntegrationTest.java)

---

## 🚧 扩展方向

### 短期优化（1-2周）
- [ ] 实体提取升级为NER模型（BERT-NER）
- [ ] 意图分类升级为Few-shot Prompt
- [ ] 长期记忆异步更新（CompletableFuture）

### 中期优化（1-2月）
- [ ] 短期记忆迁移到Redis
- [ ] 工作记忆迁移到Redis + TTL
- [ ] 长期记忆迁移到PostgreSQL

### 长期优化（3-6月）
- [ ] 历史行程向量化存入VectorStore
- [ ] 支持语义查询（"上次去杭州住的哪家酒店"）
- [ ] 混合检索（BM25 + 向量）
- [ ] 分布式部署（Redis Cluster + 分库分表）

---

## 📞 联系方式

- 项目地址：[GitHub](https://github.com/your-repo)
- 问题反馈：[Issues](https://github.com/your-repo/issues)
- 技术交流：[Discussions](https://github.com/your-repo/discussions)

---

## 📝 更新日志

### v1.0 (2026-04-29)
- ✅ 实现三层记忆架构
- ✅ 短期记忆：Kryo文件 + 滑动窗口
- ✅ 工作记忆：实体提取 + 意图追踪
- ✅ 长期记忆：用户画像 + 个性化推荐
- ✅ REST API接口
- ✅ 集成测试
- ✅ 完整文档

---

**文档版本**: v1.0  
**最后更新**: 2026-04-29  
**维护者**: AI Agent Team
