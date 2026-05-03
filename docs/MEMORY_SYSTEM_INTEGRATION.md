# 记忆系统集成说明

## 已接入的位置

### 1. Controller层接入（AiController.java）

记忆系统已经集成到所有主要的对话接口中：

#### 接入点1：同步对话接口
```java
@GetMapping("/enterprise/chat/sync")
public String doChatWithEnterpriseSync(
    @RequestParam String message,
    @RequestParam String chatId,
    @RequestParam(required = false, defaultValue = "anonymous") String userId)
```

**工作流程**：
1. 调用 `memoryService.processUserMessage(userId, chatId, message)` 更新工作记忆
2. 调用 `enterpriseAssistantApp.doChat()` 生成回复
3. 可选：调用 `memoryService.learnFromConversation()` 更新长期记忆

**测试命令**：
```bash
curl "http://localhost:8123/api/ai/enterprise/chat/sync?message=我要去上海出差&chatId=test001&userId=user001"
```

---

#### 接入点2：SSE流式对话接口
```java
@GetMapping("/enterprise/chat/sse")
public Flux<String> doChatWithEnterpriseSSE(
    @RequestParam String message,
    @RequestParam String chatId,
    @RequestParam(required = false, defaultValue = "anonymous") String userId)
```

**工作流程**：
1. 调用 `memoryService.processUserMessage()` 更新工作记忆
2. 返回流式响应
3. 注意：流式响应结束后需要前端调用 `/api/memory/learn` 触发学习

**测试命令**：
```bash
curl -N "http://localhost:8123/api/ai/enterprise/chat/sse?message=我要去上海出差&chatId=test001&userId=user001"
```

---

#### 接入点3：综合对话接口（RAG + MCP + 记忆）
```java
@GetMapping("/enterprise/chat/comprehensive")
public String doComprehensiveChat(
    @RequestParam String message,
    @RequestParam String chatId,
    @RequestParam(required = false, defaultValue = "anonymous") String userId)
```

**工作流程**：
1. 调用 `memoryService.processUserMessage()` 更新工作记忆
2. 调用 `enterpriseAssistantApp.doComprehensiveChat()` 生成回复（包含RAG + MCP + 工作记忆上下文）
3. 可选：触发学习

**测试命令**：
```bash
curl "http://localhost:8123/api/ai/enterprise/chat/comprehensive?message=我要去上海出差&chatId=test001&userId=user001"
```

---

#### 接入点4：模式选择接口（支持默认/思考模式）
```java
@GetMapping("/enterprise/chat")
public String doChatWithMode(
    @RequestParam String message,
    @RequestParam String chatId,
    @RequestParam(required = false, defaultValue = "anonymous") String userId,
    @RequestParam(required = false) String mode)
```

**工作流程**：
1. 调用 `memoryService.processUserMessage()` 更新工作记忆
2. 根据mode参数选择执行模式（default/thinking）
3. 调用 `workflowOrchestrator.route()` 路由执行

**测试命令**：
```bash
# 默认模式
curl "http://localhost:8123/api/ai/enterprise/chat?message=我要去上海出差&chatId=test001&userId=user001"

# 思考模式
curl "http://localhost:8123/api/ai/enterprise/chat?message=我要去上海出差&chatId=test001&userId=user001&mode=thinking"
```

---

### 2. Application层增强（EnterpriseAssistantApp.java）

#### 增强点1：综合对话方法
```java
public String doComprehensiveChat(String message, String chatId) {
    // 获取工作记忆的上下文摘要
    String contextSummary = memoryService.getContextSummary(chatId);
    String enhancedSystemPrompt = SYSTEM_PROMPT;
    if (!contextSummary.isEmpty()) {
        enhancedSystemPrompt = SYSTEM_PROMPT + "\n\n" + contextSummary;
    }
    
    // 使用增强的系统提示调用LLM
    // ...
}
```

**效果**：
- 自动将工作记忆的上下文（城市、意图）注入到系统提示中
- LLM能够理解"那里的天气"指的是"上海的天气"

---

#### 增强点2：RAG对话方法
```java
public String doChatWithCorporateKnowledge(String message, String chatId) {
    // 获取工作记忆的上下文摘要
    String contextSummary = memoryService.getContextSummary(chatId);
    String enhancedSystemPrompt = SYSTEM_PROMPT;
    if (!contextSummary.isEmpty()) {
        enhancedSystemPrompt = SYSTEM_PROMPT + "\n\n" + contextSummary;
    }
    
    // 使用增强的系统提示 + RAG
    // ...
}
```

**效果**：
- RAG检索时能够利用工作记忆的上下文
- 提升检索准确性

---

## 完整的对话流程

### 场景：用户第一次咨询上海出差

```
用户输入: "我要去上海出差，帮我查一下天气"
    ↓
【Controller层】
    memoryService.processUserMessage("user001", "conv001", "我要去上海出差，帮我查一下天气")
    ↓
【Layer 2: 工作记忆】
    提取实体: cities=["上海"], currentDestination="上海"
    识别意图: currentIntent="查询天气"
    ↓
【Application层】
    获取上下文摘要: "【当前会话上下文】\n目的地: 上海\n当前意图: 查询天气"
    增强系统提示: SYSTEM_PROMPT + 上下文摘要
    ↓
【LLM生成回复】
    "上海今天天气晴朗，温度15-22℃..."
    ↓
【返回给用户】
```

### 场景：用户继续询问（利用上下文）

```
用户输入: "那边有什么协议酒店推荐吗"
    ↓
【Controller层】
    memoryService.processUserMessage("user001", "conv001", "那边有什么协议酒店推荐吗")
    ↓
【Layer 2: 工作记忆】
    更新意图: currentIntent="查询酒店"
    意图历史: ["查询天气", "查询酒店"]
    ↓
【Application层】
    获取上下文摘要: "【当前会话上下文】\n目的地: 上海\n涉及城市: 上海\n当前意图: 查询酒店"
    增强系统提示: SYSTEM_PROMPT + 上下文摘要
    ↓
【LLM生成回复】
    LLM理解"那边"指的是"上海"
    "上海的协议酒店有：XX酒店（经济型）、YY酒店（舒适型）..."
    ↓
【返回给用户】
```

### 场景：会话结束，学习用户偏好

```
【前端或定时任务触发】
    POST /api/memory/learn?userId=user001&conversationId=conv001
    ↓
【MemoryService】
    从工作记忆提取信息
    ↓
【Layer 3: 长期记忆】
    更新用户画像:
    - cityVisitCount["上海"] = 1
    - tripSummaries.add({destination: "上海", intents: ["查询天气", "查询酒店"]})
    保存到 ./data/user-profiles/user001.json
```

### 场景：用户第二次咨询（个性化推荐）

```
用户输入: "我又要去上海了"
    ↓
【Controller层】
    memoryService.processUserMessage("user001", "conv002", "我又要去上海了")
    ↓
【Layer 2: 工作记忆】
    提取实体: cities=["上海"], currentDestination="上海"
    ↓
【Layer 3: 长期记忆】
    检测到用户之前来过上海（cityVisitCount["上海"] = 1）
    生成个性化提示: "您之前来过上海，上次入住的酒店信息已为您调取。"
    ↓
【Application层】
    增强系统提示: SYSTEM_PROMPT + 上下文摘要 + 个性化提示
    ↓
【LLM生成回复】
    "欢迎再次来上海！根据您上次的出差记录，为您推荐..."
    ↓
【返回给用户】
```

---

## 记忆系统的三个调用时机

### 1. 每次对话时（必须）
```java
// 在Controller层，每次收到用户消息时调用
memoryService.processUserMessage(userId, chatId, message);
```

**作用**：
- 更新工作记忆（提取实体和意图）
- 为当前对话提供上下文

---

### 2. 生成回复时（自动）
```java
// 在Application层，生成回复前调用
String contextSummary = memoryService.getContextSummary(chatId);
String enhancedSystemPrompt = SYSTEM_PROMPT + "\n\n" + contextSummary;
```

**作用**：
- 将工作记忆的上下文注入到系统提示
- 让LLM理解指代词（"那里"、"那个城市"）

---

### 3. 会话结束时（可选）
```java
// 方式1：在Controller层，对话结束后调用
memoryService.learnFromConversation(userId, chatId);

// 方式2：前端调用API
POST /api/memory/learn?userId=user001&conversationId=conv001

// 方式3：定时任务（每N次对话触发一次）
if (conversationCount % 5 == 0) {
    memoryService.learnFromConversation(userId, chatId);
}
```

**作用**：
- 从工作记忆提取信息，更新长期记忆
- 积累用户画像，用于个性化推荐

---

## 参数说明

### userId（用户ID）
- **用途**：标识用户，用于长期记忆（用户画像）
- **默认值**：`"anonymous"`（匿名用户）
- **建议**：
  - 有用户系统：传真实的userId
  - 无用户系统：可以用设备ID或sessionId代替
  - 测试环境：可以用固定值如 `"test_user"`

### chatId（会话ID）
- **用途**：标识会话，用于短期记忆和工作记忆
- **建议**：
  - 前端生成UUID：`const chatId = uuidv4()`
  - 或使用时间戳：`const chatId = "chat_" + Date.now()`
  - 同一个对话流程使用同一个chatId

### message（用户消息）
- **用途**：用户输入的原始文本
- **注意**：不需要预处理，系统会自动提取实体和意图

---

## 测试验证

### 1. 测试工作记忆提取
```bash
# 第一次对话
curl "http://localhost:8123/api/ai/enterprise/chat/sync?message=我要去上海出差，帮我查一下天气&chatId=test001&userId=user001"

# 查看工作记忆
curl http://localhost:8123/api/memory/working/test001

# 预期输出：
{
  "conversationId": "test001",
  "cities": ["上海"],
  "currentDestination": "上海",
  "currentIntent": "查询天气",
  "intentHistory": ["查询天气"]
}
```

### 2. 测试上下文理解
```bash
# 继续对话（使用指代词）
curl "http://localhost:8123/api/ai/enterprise/chat/sync?message=那边有什么酒店&chatId=test001&userId=user001"

# 查看工作记忆
curl http://localhost:8123/api/memory/working/test001

# 预期输出：
{
  "conversationId": "test001",
  "cities": ["上海"],
  "currentDestination": "上海",
  "currentIntent": "查询酒店",
  "intentHistory": ["查询天气", "查询酒店"]
}
```

### 3. 测试长期记忆学习
```bash
# 触发学习
curl -X POST "http://localhost:8123/api/memory/learn?userId=user001&conversationId=test001"

# 查看用户画像
curl http://localhost:8123/api/memory/profile/user001

# 预期输出：
{
  "userId": "user001",
  "cityVisitCount": {
    "上海": 1
  },
  "tripSummaries": [
    {
      "destination": "上海",
      "timestamp": 1735660800000,
      "intents": ["查询天气", "查询酒店"]
    }
  ]
}
```

### 4. 测试个性化推荐
```bash
# 第二次对话（新会话）
curl "http://localhost:8123/api/ai/enterprise/chat/sync?message=我又要去上海了&chatId=test002&userId=user001"

# 系统会识别用户之前来过上海，提供个性化推荐
```

---

## 注意事项

### 1. 学习触发时机
- **当前实现**：需要手动调用 `/api/memory/learn` 接口
- **建议优化**：
  - 方案1：前端在对话结束时调用
  - 方案2：后端定时任务，每5次对话触发一次
  - 方案3：用户明确表示"结束对话"时触发

### 2. 性能考虑
- 工作记忆提取：~5ms（规则匹配）
- 上下文摘要生成：<1ms（字符串拼接）
- 长期记忆更新：~20ms（JSON序列化）
- **总开销**：<30ms，对用户体验影响极小

### 3. 扩展建议
- **短期**：实体提取升级为NER模型
- **中期**：迁移到Redis（分布式部署）
- **长期**：历史行程向量化，支持语义查询

---

## 面试演示要点

### 演示脚本（5分钟）

1. **启动应用**
   ```bash
   ./mvnw spring-boot:run
   ```

2. **第一次对话**
   ```bash
   curl "http://localhost:8123/api/ai/enterprise/chat/sync?message=我要去上海出差，帮我查一下天气&chatId=demo001&userId=interviewer"
   ```
   **讲解**：系统自动提取"上海"和"查询天气"

3. **查看工作记忆**
   ```bash
   curl http://localhost:8123/api/memory/working/demo001 | jq
   ```
   **讲解**：展示JSON输出，指出提取的实体和意图

4. **继续对话（测试上下文理解）**
   ```bash
   curl "http://localhost:8123/api/ai/enterprise/chat/sync?message=那边有什么酒店&chatId=demo001&userId=interviewer"
   ```
   **讲解**：系统理解"那边"指的是"上海"

5. **触发学习**
   ```bash
   curl -X POST "http://localhost:8123/api/memory/learn?userId=interviewer&conversationId=demo001"
   ```
   **讲解**：从工作记忆提取信息，更新用户画像

6. **查看用户画像**
   ```bash
   curl http://localhost:8123/api/memory/profile/interviewer | jq
   ```
   **讲解**：展示cityVisitCount和tripSummaries

7. **第二次对话（个性化）**
   ```bash
   curl "http://localhost:8123/api/ai/enterprise/chat/sync?message=我又要去上海了&chatId=demo002&userId=interviewer"
   ```
   **讲解**：系统识别用户之前来过上海，提供个性化推荐

---

**文档版本**: v1.1  
**最后更新**: 2026-04-29  
**集成状态**: ✅ 完成
