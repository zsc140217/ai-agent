# 记忆系统快速开始指南

## 一、5分钟快速体验

### 1. 启动应用

```bash
./mvnw spring-boot:run
```

### 2. 模拟第一次对话

```bash
# 用户第一次咨询上海出差
curl -N "http://localhost:8123/api/ai/enterprise/chat/sse?message=我要去上海出差，帮我查一下天气&chatId=demo001"
```

### 3. 查看工作记忆

```bash
# 查看系统提取的实体和意图
curl http://localhost:8123/api/memory/working/demo001
```

**预期输出**：
```json
{
  "conversationId": "demo001",
  "cities": ["上海"],
  "currentDestination": "上海",
  "currentIntent": "查询天气",
  "intentHistory": ["查询天气"]
}
```

### 4. 继续对话

```bash
# 用户继续询问酒店
curl -N "http://localhost:8123/api/ai/enterprise/chat/sse?message=那边有什么协议酒店推荐吗&chatId=demo001"
```

### 5. 再次查看工作记忆

```bash
curl http://localhost:8123/api/memory/working/demo001
```

**预期输出**：
```json
{
  "conversationId": "demo001",
  "cities": ["上海"],
  "currentDestination": "上海",
  "currentIntent": "查询酒店",
  "intentHistory": ["查询天气", "查询酒店"]
}
```

### 6. 触发学习（更新用户画像）

```bash
curl -X POST "http://localhost:8123/api/memory/learn?userId=user001&conversationId=demo001"
```

### 7. 查看用户画像

```bash
curl http://localhost:8123/api/memory/profile/user001
```

**预期输出**：
```json
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

### 8. 模拟第二次对话（个性化推荐）

```bash
# 用户第二次咨询上海出差
curl -N "http://localhost:8123/api/ai/enterprise/chat/sse?message=我又要去上海了&chatId=demo002"
```

**系统会识别**：用户之前来过上海，提供个性化推荐

---

## 二、运行测试验证

```bash
# 运行完整的记忆系统集成测试
./mvnw test -Dtest=MemorySystemIntegrationTest

# 测试覆盖：
# ✅ 三层记忆协同工作
# ✅ 实体提取（城市、客户）
# ✅ 意图追踪（查天气 → 订酒店 → 规划路线）
# ✅ 用户画像积累
# ✅ 个性化推荐生成
```

---

## 三、查看存储文件

### 短期记忆（对话历史）

```bash
# 查看生成的对话历史文件
ls -lh ./data/chat-history/

# 输出示例：
# demo001.kryo  (Kryo序列化的对话历史)
# demo002.kryo
```

### 长期记忆（用户画像）

```bash
# 查看用户画像JSON文件
cat ./data/user-profiles/user001.json

# 输出示例：
{
  "userId": "user001",
  "cityVisitCount": {
    "上海": 2,
    "北京": 1
  },
  "tripSummaries": [...]
}
```

---

## 四、常用API速查

| 功能 | 命令 |
|------|------|
| 查看工作记忆 | `curl http://localhost:8123/api/memory/working/{conversationId}` |
| 查看用户画像 | `curl http://localhost:8123/api/memory/profile/{userId}` |
| 触发学习 | `curl -X POST "http://localhost:8123/api/memory/learn?userId=X&conversationId=Y"` |
| 清空会话 | `curl -X DELETE http://localhost:8123/api/memory/conversation/{conversationId}` |
| 删除用户数据 | `curl -X DELETE http://localhost:8123/api/memory/user/{userId}` |
| 系统统计 | `curl http://localhost:8123/api/memory/stats` |
| 清理过期会话 | `curl -X POST http://localhost:8123/api/memory/cleanup` |

---

## 五、面试演示脚本

### 场景：用户多次咨询上海出差

**第1步：启动应用**
```bash
./mvnw spring-boot:run
```

**第2步：第一次对话**
```bash
curl -N "http://localhost:8123/api/ai/enterprise/chat/sse?message=我要去上海出差，帮我查一下天气&chatId=interview001"
```

**讲解要点**：
- 系统自动提取城市实体"上海"
- 识别意图为"查询天气"
- 存储到工作记忆

**第3步：展示工作记忆**
```bash
curl http://localhost:8123/api/memory/working/interview001 | jq
```

**讲解要点**：
- 展示JSON输出，指出提取的实体和意图
- 说明工作记忆是内存存储，30分钟TTL

**第4步：继续对话**
```bash
curl -N "http://localhost:8123/api/ai/enterprise/chat/sse?message=那边有什么协议酒店推荐吗&chatId=interview001"
```

**讲解要点**：
- 意图从"查询天气"更新为"查询酒店"
- 意图历史记录了完整序列

**第5步：触发学习**
```bash
curl -X POST "http://localhost:8123/api/memory/learn?userId=interviewer&conversationId=interview001"
```

**讲解要点**：
- 从工作记忆提取信息，更新长期记忆
- 用户画像记录"上海"访问次数+1

**第6步：展示用户画像**
```bash
curl http://localhost:8123/api/memory/profile/interviewer | jq
```

**讲解要点**：
- 展示cityVisitCount统计
- 展示tripSummaries历史记录

**第7步：第二次对话（个性化）**
```bash
curl -N "http://localhost:8123/api/ai/enterprise/chat/sse?message=我又要去上海了&chatId=interview002"
```

**讲解要点**：
- 系统识别用户之前来过上海
- 提供个性化提示："您之前来过上海..."

**第8步：再次查看用户画像**
```bash
curl -X POST "http://localhost:8123/api/memory/learn?userId=interviewer&conversationId=interview002"
curl http://localhost:8123/api/memory/profile/interviewer | jq
```

**讲解要点**：
- 上海访问次数从1增加到2
- 展示用户偏好的积累过程

**第9步：清理演示数据**
```bash
curl -X DELETE http://localhost:8123/api/memory/user/interviewer
```

**讲解要点**：
- GDPR合规，支持用户数据删除
- 生产环境需加权限校验

---

## 六、故障排查

### 问题1：启动报错"找不到data目录"

**解决方案**：
```bash
mkdir -p ./data/chat-history
mkdir -p ./data/user-profiles
```

### 问题2：工作记忆为空

**原因**：实体提取规则未匹配到关键词

**解决方案**：
- 检查消息是否包含城市名（北京、上海、杭州等）
- 检查消息是否包含意图关键词（天气、酒店、路线等）
- 查看日志：`grep "Extracted" logs/application.log`

### 问题3：用户画像未更新

**原因**：未调用学习接口

**解决方案**：
```bash
# 手动触发学习
curl -X POST "http://localhost:8123/api/memory/learn?userId=X&conversationId=Y"
```

### 问题4：内存占用过高

**原因**：工作记忆未清理

**解决方案**：
```bash
# 手动清理过期会话
curl -X POST http://localhost:8123/api/memory/cleanup

# 或配置定时任务（见文档第7.2节）
```

---

## 七、下一步

- 📖 阅读完整设计文档：[MEMORY_SYSTEM_DESIGN.md](MEMORY_SYSTEM_DESIGN.md)
- 🧪 运行完整测试：`./mvnw test -Dtest=MemorySystemIntegrationTest`
- 🔧 集成到现有系统：参考文档第7.1节
- 🚀 扩展升级：NER模型、Redis存储、向量化历史

---

**文档版本**: v1.0  
**最后更新**: 2026-04-29
