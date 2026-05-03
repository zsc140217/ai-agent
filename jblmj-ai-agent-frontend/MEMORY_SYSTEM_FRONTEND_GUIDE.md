# 前端记忆系统使用指南

## 🎯 更新内容

### 1. 新增页面
- **增强版企业助手**: `/enterprise-assistant-enhanced`
  - 三层记忆系统可视化
  - 执行模式切换（快速模式 vs 思考模式）
  - 实时查看工作记忆和用户画像
  - 手动触发学习功能

### 2. 更新的文件

| 文件 | 修改内容 |
|------|----------|
| `src/api/index.js` | 新增记忆系统API（5个接口） |
| `src/views/EnterpriseAssistantEnhanced.vue` | 新增增强版页面（完整功能） |
| `src/router/index.js` | 新增路由配置 |
| `src/views/Home.vue` | 首页新增增强版入口 |

---

## 🚀 如何启动

### 1. 启动后端（IDEA）
```bash
# 在IDEA中运行
Run -> Run 'YuAiAgentApplication'

# 或使用Maven
./mvnw spring-boot:run
```

**确认后端启动成功**：访问 http://localhost:8123/api/health

### 2. 启动前端（VSCode）
```bash
# 进入前端目录
cd jblmj-ai-agent-frontend

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev
```

**访问地址**：http://localhost:5173

---

## 📱 功能演示

### 场景1：测试记忆系统

#### 步骤1：进入增强版页面
1. 打开浏览器访问 http://localhost:5173
2. 点击 **"企业出差管家（增强版）"** 卡片（带🎯标记）
3. 进入增强版聊天界面

#### 步骤2：第一次对话
在输入框输入：
```
我要去上海出差，帮我查一下天气
```

**系统自动**：
- 提取实体：`cities=["上海"]`, `currentDestination="上海"`
- 识别意图：`currentIntent="查询天气"`

#### 步骤3：查看工作记忆
点击 **"📝 查看工作记忆"** 按钮

**预期显示**：
```json
{
  "conversationId": "trip_xxx",
  "cities": ["上海"],
  "currentDestination": "上海",
  "currentIntent": "查询天气",
  "intentHistory": ["查询天气"],
  "lastUpdateTime": 1735660800000
}
```

#### 步骤4：继续对话（测试上下文理解）
在输入框输入：
```
那边有什么酒店
```

**系统理解**：
- "那边" → "上海"（从工作记忆获取）
- 更新意图：`currentIntent="查询酒店"`

#### 步骤5：再次查看工作记忆
点击 **"📝 查看工作记忆"** 按钮

**预期显示**：
```json
{
  "conversationId": "trip_xxx",
  "cities": ["上海"],
  "currentDestination": "上海",
  "currentIntent": "查询酒店",
  "intentHistory": ["查询天气", "查询酒店"]
}
```

#### 步骤6：触发学习
点击 **"🎓 触发学习"** 按钮

**系统自动**：
- 从工作记忆提取信息
- 更新用户画像：`cityVisitCount["上海"] = 1`
- 保存行程摘要

#### 步骤7：查看用户画像
点击 **"👤 查看用户画像"** 按钮

**预期显示**：
```json
{
  "userId": "user_xxx",
  "cityVisitCount": {
    "上海": 1
  },
  "tripSummaries": [
    {
      "destination": "上海",
      "timestamp": 1735660800000,
      "intents": ["查询天气", "查询酒店"]
    }
  ],
  "createdAt": 1735574400000,
  "updatedAt": 1735660800000
}
```

---

### 场景2：测试执行模式切换

#### 快速模式（默认）
- **特点**：复杂度评估 + 工具路由
- **速度**：5-10秒
- **适用**：常规查询

**测试命令**：
```
查询北京天气
```

#### 思考模式
1. 点击 **"🧠 思考模式"** 按钮
2. 输入复杂查询：
```
帮我规划明天去杭州的行程，包括天气、酒店和路线
```

- **特点**：ReAct循环推理
- **速度**：15-30秒
- **适用**：复杂任务

---

## 🎨 界面说明

### 控制面板

```
┌─────────────────────────────────────────────────────┐
│ 执行模式：                                           │
│  [🚀 快速模式]  [🧠 思考模式]                        │
│                                                      │
│ 记忆功能：                                           │
│  [📝 查看工作记忆] [👤 查看用户画像]                 │
│  [🎓 触发学习] [🗑️ 清空记忆]                        │
└─────────────────────────────────────────────────────┘
```

### 记忆面板（右侧）

```
┌─────────────────────────────┐
│ 工作记忆（当前会话）    [✕] │
├─────────────────────────────┤
│ {                           │
│   "conversationId": "...",  │
│   "cities": ["上海"],       │
│   "currentDestination": ... │
│ }                           │
└─────────────────────────────┘
```

---

## 🔧 API接口说明

### 1. 对话接口（SSE流式）
```javascript
// 快速模式（默认）
chatWithEnterpriseApp(message, chatId, userId)

// 参数说明：
// - message: 用户消息
// - chatId: 会话ID（自动生成）
// - userId: 用户ID（从localStorage获取）
```

### 2. 记忆系统接口

```javascript
// 获取工作记忆
getWorkingMemory(conversationId)

// 获取用户画像
getUserProfile(userId)

// 触发学习
triggerLearning(userId, conversationId)

// 清空会话记忆
clearConversation(conversationId)

// 获取系统统计
getMemoryStats()
```

---

## 📊 数据流图

```
用户输入
    ↓
【前端】发送消息 + userId + chatId
    ↓
【后端Controller】memoryService.processUserMessage()
    ↓
【工作记忆】提取实体和意图
    ↓
【LLM生成回复】（带上下文增强）
    ↓
【前端】显示回复
    ↓
【用户点击"触发学习"】
    ↓
【后端】memoryService.learnFromConversation()
    ↓
【长期记忆】更新用户画像
    ↓
【前端】显示成功提示
```

---

## 🐛 常见问题

### Q1: 点击"查看工作记忆"没有数据？
**A**: 需要先发送至少一条消息，系统才会提取实体和意图。

### Q2: 用户画像为空？
**A**: 需要先点击"触发学习"按钮，系统才会从工作记忆更新用户画像。

### Q3: 后端连接失败？
**A**: 检查后端是否启动（http://localhost:8123/api/health）

### Q4: 前端启动报错？
**A**: 
```bash
# 删除node_modules重新安装
rm -rf node_modules
npm install
npm run dev
```

### Q5: 如何清空测试数据？
**A**: 
1. 点击"🗑️ 清空记忆"按钮（清空当前会话）
2. 或删除后端数据文件：
   - `./data/chat-history/*.kryo`
   - `./data/user-profiles/*.json`

---

## 🎓 面试演示脚本

### 演示流程（5分钟）

**第1步：展示首页**
- 打开 http://localhost:5173
- 介绍三个卡片：普通版、增强版、超级智能体
- 点击"企业出差管家（增强版）"

**第2步：介绍界面**
- 指出控制面板：执行模式切换、记忆功能按钮
- 说明右侧记忆面板的作用

**第3步：第一次对话**
- 输入："我要去上海出差，帮我查一下天气"
- 等待回复
- 讲解："系统自动提取了'上海'和'查询天气'"

**第4步：查看工作记忆**
- 点击"📝 查看工作记忆"
- 展示JSON数据
- 讲解：`cities`, `currentDestination`, `currentIntent`

**第5步：测试上下文理解**
- 输入："那边有什么酒店"
- 等待回复
- 讲解："系统理解'那边'指的是'上海'"

**第6步：再次查看工作记忆**
- 点击"📝 查看工作记忆"
- 展示意图更新：`intentHistory: ["查询天气", "查询酒店"]`

**第7步：触发学习**
- 点击"🎓 触发学习"
- 等待成功提示

**第8步：查看用户画像**
- 点击"👤 查看用户画像"
- 展示：`cityVisitCount["上海"] = 1`
- 讲解："用户画像记录了常去城市和历史行程"

**第9步：切换执行模式**
- 点击"🧠 思考模式"
- 输入复杂查询
- 讲解："思考模式使用ReAct循环推理，适合复杂任务"

---

## 📝 技术要点

### 前端技术栈
- **框架**: Vue 3 + Composition API
- **路由**: Vue Router
- **HTTP**: Axios
- **SSE**: EventSource（原生API）

### 核心功能
1. **SSE流式对话**：实时显示AI回复
2. **记忆面板**：可视化工作记忆和用户画像
3. **模式切换**：快速模式 vs 思考模式
4. **状态提示**：Toast通知（成功/错误/信息）

### 数据持久化
- **用户ID**: 存储在 `localStorage`（key: `demo_user_id`）
- **会话ID**: 每次进入页面自动生成（格式: `trip_xxx`）

---

## 🚀 下一步优化

### 短期优化
- [ ] 添加加载动画（骨架屏）
- [ ] 优化移动端适配
- [ ] 添加快捷键支持（Ctrl+Enter发送）

### 中期优化
- [ ] 添加对话历史导出功能
- [ ] 支持多会话管理（会话列表）
- [ ] 添加语音输入功能

### 长期优化
- [ ] 集成WebSocket（替代SSE）
- [ ] 添加Markdown渲染
- [ ] 支持代码高亮

---

**文档版本**: v1.0  
**最后更新**: 2026-04-29  
**适用环境**: 开发环境（IDEA + VSCode）
