# 三层记忆系统设计文档

## 一、系统架构

### 1.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      用户请求                                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    MemoryService                             │
│                   (统一记忆门面)                              │
└─────────────────────────────────────────────────────────────┘
         ↓                    ↓                    ↓
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Layer 1      │    │ Layer 2      │    │ Layer 3      │
│ 短期记忆      │    │ 工作记忆      │    │ 长期记忆      │
│              │    │              │    │              │
│ 原始对话历史  │    │ 实体提取      │    │ 用户画像      │
│ 滑动窗口      │    │ 意图追踪      │    │ 偏好学习      │
│              │    │              │    │              │
│ 存储: 文件    │    │ 存储: 内存    │    │ 存储: JSON   │
└──────────────┘    └──────────────┘    └──────────────┘
```

### 1.2 核心组件

| 组件 | 职责 | 存储方式 | 生命周期 |
|------|------|----------|----------|
| **EnhancedMessageWindowChatMemory** | 短期记忆：存储原始对话历史 | Kryo序列化文件 | 持久化，滑动窗口20条 |
| **WorkingMemoryManager** | 工作记忆：提取实体和意图 | ConcurrentHashMap | 内存，30分钟TTL |
| **LongTermMemoryManager** | 长期记忆：学习用户偏好 | JSON文件 | 持久化，永久保存 |
| **MemoryService** | 统一门面：协调三层记忆 | - | 单例Bean |

---

## 二、核心功能

### 2.1 短期记忆（Layer 1）

**功能**：存储原始对话历史，支持上下文理解

**关键特性**：
- ✅ 文件持久化（Kryo序列化，比JSON快3-5倍）
- ✅ 滑动窗口机制（保留最近20条消息，防止token超限）
- ✅ 会话恢复（重启后可恢复历史对话）
- ✅ 边界优化（确保保留完整的User-Assistant对）

**代码示例**：
```java
// 自动注入增强记忆
@Autowired
private ChatMemory enhancedChatMemory;

// 使用方式（Spring AI自动调用）
chatClient.prompt()
    .user(message)
    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
    .call();
```

**文件存储路径**：
```
./data/chat-history/
├── conv_001.kryo
├── conv_002.kryo
└── conv_003.kryo
```

---

### 2.2 工作记忆（Layer 2）

**功能**：从对话中提取结构化信息，支持任务追踪

**提取内容**：
- 🏙️ **城市实体**：北京、上海、杭州等
- 👤 **客户实体**：XX公司、XX客户
- 🏨 **酒店实体**：协议酒店名称
- 🎯 **意图序列**：查天气 → 订酒店 → 规划路线

**代码示例**：
```java
// 处理用户消息，自动提取实体
memoryService.processUserMessage(userId, conversationId, "我要去上海出差");

// 获取工作记忆
WorkingMemory memory = memoryService.getWorkingMemory(conversationId);
System.out.println(memory.getCurrentDestination()); // 输出: 上海
System.out.println(memory.getCurrentIntent());      // 输出: 查询天气
```

**上下文摘要示例**：
```
【当前会话上下文】
目的地: 上海
涉及城市: 上海, 杭州
当前意图: 查询酒店
已完成任务: 查询天气
```

---

### 2.3 长期记忆（Layer 3）

**功能**：学习用户偏好，提供个性化推荐

**学习内容**：
- 📊 **常去城市统计**：{"上海": 5, "北京": 3, "杭州": 2}
- 🏨 **偏好酒店档次**：经济型/舒适型/豪华型
- 📝 **历史行程摘要**：最近20次出差记录

**代码示例**：
```java
// 会话结束时学习
memoryService.learnFromConversation(userId, conversationId);

// 获取用户画像
UserProfile profile = memoryService.getUserProfile(userId);
System.out.println(profile.getCityVisitCount()); // {"上海": 5}

// 生成个性化提示
String hint = memoryService.buildEnhancedPrompt(userId, conversationId, "上海");
// 输出: "您之前来过上海，上次入住的酒店信息已为您调取。"
```

**存储格式**（JSON）：
```json
{
  "userId": "user_001",
  "cityVisitCount": {
    "上海": 5,
    "北京": 3
  },
  "preferredHotelLevel": "舒适型",
  "tripSummaries": [
    {
      "destination": "上海",
      "timestamp": 1735660800000,
      "intents": ["查询天气", "查询酒店", "规划路线"]
    }
  ],
  "createdAt": 1735574400000,
  "updatedAt": 1735660800000
}
```

---

## 三、API接口

### 3.1 记忆管理接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/memory/working/{conversationId}` | GET | 获取工作记忆 |
| `/api/memory/profile/{userId}` | GET | 获取用户画像 |
| `/api/memory/conversation/{conversationId}` | DELETE | 清空会话记忆 |
| `/api/memory/user/{userId}` | DELETE | 删除用户数据（GDPR） |
| `/api/memory/learn` | POST | 手动触发学习 |
| `/api/memory/stats` | GET | 获取系统统计 |
| `/api/memory/cleanup` | POST | 清理过期会话 |

### 3.2 使用示例

```bash
# 1. 查看工作记忆
curl http://localhost:8123/api/memory/working/conv_001

# 2. 查看用户画像
curl http://localhost:8123/api/memory/profile/user_001

# 3. 手动触发学习
curl -X POST "http://localhost:8123/api/memory/learn?userId=user_001&conversationId=conv_001"

# 4. 获取系统统计
curl http://localhost:8123/api/memory/stats

# 5. 清理过期会话
curl -X POST http://localhost:8123/api/memory/cleanup
```

---

## 四、配置说明

### 4.1 application.yml配置

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

### 4.2 配置参数说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `chat.memory.storage.path` | `./data/chat-history` | 短期记忆文件存储目录 |
| `chat.memory.window.size` | `20` | 滑动窗口大小（消息条数） |
| `chat.memory.longterm.path` | `./data/user-profiles` | 用户画像JSON存储目录 |

---

## 五、测试验证

### 5.1 运行测试

```bash
# 运行记忆系统集成测试
./mvnw test -Dtest=MemorySystemIntegrationTest

# 测试场景：
# 1. 用户第一次咨询上海出差
# 2. 系统记录城市、意图等信息
# 3. 用户第二次咨询时，系统识别"常去城市"并提供个性化服务
```

### 5.2 测试覆盖

- ✅ 三层记忆协同工作流程
- ✅ 多城市实体提取
- ✅ 意图追踪和序列记录
- ✅ 用户画像积累
- ✅ 个性化推荐生成
- ✅ 会话清理和过期管理

---

## 六、面试要点

### 6.1 为什么需要三层记忆？

| 层级 | 问题 | 解决方案 |
|------|------|----------|
| **短期记忆** | 如何保留上下文？ | 滑动窗口 + 文件持久化 |
| **工作记忆** | 如何理解用户意图？ | 实体提取 + 意图追踪 |
| **长期记忆** | 如何个性化推荐？ | 用户画像 + 偏好学习 |

### 6.2 技术选型理由

**Q: 为什么短期记忆用Kryo而不是JSON？**
- A: Kryo序列化速度比JSON快3-5倍，且Spring AI的Message对象结构复杂，JSON序列化会丢失类型信息

**Q: 为什么工作记忆用内存而不是数据库？**
- A: 工作记忆是临时状态，读写频繁，内存性能更好。30分钟TTL自动清理，避免内存泄漏

**Q: 为什么长期记忆用JSON文件而不是数据库？**
- A: 实习项目快速验证，避免引入MySQL依赖。生产环境可升级为Redis/PostgreSQL

### 6.3 性能优化

| 优化点 | 方案 | 效果 |
|--------|------|------|
| **短期记忆** | 滑动窗口裁剪 | 防止token超限（20轮≈4000 tokens） |
| **工作记忆** | ConcurrentHashMap | 支持多用户并发访问 |
| **长期记忆** | 异步更新 | 不阻塞主流程（可用CompletableFuture） |
| **会话清理** | 定时任务 | 30分钟TTL自动清理过期会话 |

### 6.4 扩展方向

1. **实体提取升级**：规则匹配 → NER模型（BERT-NER）
2. **意图分类升级**：关键词匹配 → Few-shot Prompt / 分类模型
3. **存储升级**：文件 → Redis（分布式部署）
4. **向量化历史**：行程摘要向量化存入VectorStore，支持"上次去杭州住的哪家酒店"
5. **GDPR合规**：数据加密、访问日志、用户删除权

---

## 七、与现有系统集成

### 7.1 修改EnterpriseAssistantApp

```java
@Component
public class EnterpriseAssistantApp {
    
    @Autowired
    private MemoryService memoryService;
    
    public String doChatWithMemory(String message, String chatId, String userId) {
        // 1. 处理用户消息（更新工作记忆）
        memoryService.processUserMessage(userId, chatId, message);
        
        // 2. 生成增强prompt（包含上下文和个性化提示）
        WorkingMemory memory = memoryService.getWorkingMemory(chatId);
        String enhancedPrompt = memoryService.buildEnhancedPrompt(
            userId, chatId, memory.getCurrentDestination()
        );
        
        // 3. 调用LLM（带上增强prompt）
        ChatResponse response = chatClient.prompt()
            .system(SYSTEM_PROMPT + "\n" + enhancedPrompt)
            .user(message)
            .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
            .call()
            .chatResponse();
        
        return response.getResult().getOutput().getText();
    }
}
```

### 7.2 添加定时清理任务

```java
@Component
public class MemoryCleanupScheduler {
    
    @Autowired
    private MemoryService memoryService;
    
    @Scheduled(fixedRate = 1800000) // 每30分钟执行一次
    public void cleanupExpiredSessions() {
        memoryService.cleanupExpiredSessions();
    }
}
```

---

## 八、常见问题

### Q1: 滑动窗口大小如何确定？
**A**: 20轮≈10次对话往返，覆盖一次完整出差规划流程。可根据业务场景调整：
- 简单问答：10-15轮
- 复杂任务：20-30轮
- 注意：Qwen-Max上下文128k，但实际20轮已足够

### Q2: 如何防止内存泄漏？
**A**: 三重保障：
1. 工作记忆：30分钟TTL自动清理
2. 定时任务：每30分钟扫描过期会话
3. 手动清理：提供API接口清空会话

### Q3: 如何保护用户隐私？
**A**: GDPR合规措施：
1. 提供删除接口：`DELETE /api/memory/user/{userId}`
2. 敏感信息脱敏：不存储身份证、手机号等
3. 访问控制：生产环境需加权限校验

### Q4: 如何升级为分布式部署？
**A**: 三步走：
1. 短期记忆：文件 → Redis（使用Spring Data Redis）
2. 工作记忆：ConcurrentHashMap → Redis（TTL自动过期）
3. 长期记忆：JSON文件 → PostgreSQL（结构化查询）

---

## 九、总结

### 9.1 核心价值

✅ **提升用户体验**：记住上下文，避免重复询问  
✅ **个性化推荐**：基于历史偏好，提供定制化服务  
✅ **任务追踪**：理解意图序列，支持多轮对话  
✅ **工程化实践**：文件持久化、滑动窗口、TTL清理  

### 9.2 面试亮点

1. **架构设计**：三层记忆分层清晰，职责明确
2. **技术选型**：Kryo序列化、ConcurrentHashMap、JSON存储，有理有据
3. **性能优化**：滑动窗口、TTL清理、异步更新
4. **工程实践**：GDPR合规、定时任务、API接口完善
5. **扩展性**：预留NER模型、Redis存储、向量化历史等升级路径

### 9.3 演示建议

1. **启动应用**：展示记忆系统自动加载
2. **模拟对话**：演示实体提取和意图追踪
3. **查看API**：展示工作记忆和用户画像
4. **第二次对话**：演示个性化推荐效果
5. **讲解代码**：重点讲三层记忆协同工作流程

---

**文档版本**: v1.0  
**最后更新**: 2026-04-29  
**作者**: AI Agent Team
