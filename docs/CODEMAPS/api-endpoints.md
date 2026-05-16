# API端点地图

## 健康检查
- `GET /api/health` - 健康检查端点

## 聊天接口

### 同步聊天
- `GET /api/ai/enterprise/chat/sync`
  - 参数: `message` (查询内容), `chatId` (会话ID)
  - 返回: JSON格式的完整响应
  - 用途: 简单查询，等待完整结果

### SSE流式聊天（推荐）
- `GET /api/ai/enterprise/chat/sse`
  - 参数: `message` (查询内容), `chatId` (会话ID)
  - 返回: Server-Sent Events流
  - 用途: 实时流式响应，用户体验更好

### ReAct Agent
- `GET /api/ai/manus/chat`
  - 参数: `message` (查询内容)
  - 返回: SSE流式响应
  - 用途: 工具调用场景（如查询距离、天气）

## 记忆系统API（Phase 2新增）

### 工作记忆
- `GET /api/memory/working/{chatId}`
  - 返回: 当前会话的工作记忆（实体+意图）
  - 用途: 调试记忆系统

### 用户画像
- `GET /api/memory/profile/{userId}`
  - 返回: 用户长期偏好和历史行为
  - 用途: 个性化推荐

### 触发学习
- `POST /api/memory/learn`
  - 参数: `userId`, `conversationId`
  - 返回: 学习结果
  - 用途: 手动触发长期记忆学习

## 监控接口（Phase 2新增）

### JVM实时状态
- `GET /api/monitor/jvm/status`
  - 返回: 当前JVM状态（堆内存、GC、线程数）
  - 用途: 实时监控

### JVM历史指标
- `GET /api/monitor/jvm/metrics`
  - 返回: 最近1小时的JVM指标时间序列
  - 用途: 性能分析、趋势观察

### 触发GC（测试用）
- `POST /api/monitor/jvm/gc`
  - 返回: GC执行结果
  - 用途: 测试环境手动触发垃圾回收

## Swagger文档
- `GET /api/swagger-ui.html` - 交互式API文档

## 使用示例

### 基础查询
```bash
# 差旅政策查询
curl "http://localhost:8123/api/ai/enterprise/chat/sse?message=去上海出差住宿标准&chatId=test123"

# 行程规划
curl "http://localhost:8123/api/ai/enterprise/chat/sse?message=帮我规划明天去杭州的行程&chatId=test123"
```

### 工具调用
```bash
# 查询距离（ReAct Agent）
curl "http://localhost:8123/api/ai/manus/chat?message=查询公司到虹桥机场的距离"

# 天气查询
curl "http://localhost:8123/api/ai/manus/chat?message=北京今天天气怎么样"
```

### 记忆系统
```bash
# 查看工作记忆
curl "http://localhost:8123/api/memory/working/test123"

# 查看用户画像
curl "http://localhost:8123/api/memory/profile/user001"

# 触发学习
curl -X POST "http://localhost:8123/api/memory/learn?userId=user001&conversationId=test123"
```

### 性能监控
```bash
# 查看JVM状态
curl "http://localhost:8123/api/monitor/jvm/status"

# 查看历史指标
curl "http://localhost:8123/api/monitor/jvm/metrics"
```

## 响应格式

### 同步响应
```json
{
  "answer": "根据公司差旅政策...",
  "sources": ["TravelPolicy.md"],
  "confidence": 0.85
}
```

### SSE流式响应
```
data: {"type":"token","content":"根据"}
data: {"type":"token","content":"公司"}
data: {"type":"token","content":"差旅"}
data: {"type":"done"}
```

### JVM状态响应
```json
{
  "timestamp": "2026-05-16T23:30:00",
  "heapUsed": 512,
  "heapMax": 2048,
  "gcCount": 15,
  "threadCount": 42
}
```
