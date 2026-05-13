# OpenClaw面试指南

## 一、OpenClaw是什么？

OpenClaw是一个**AI Agent编排平台**，通过Plugin架构和MCP协议，让AI模型能够调用外部工具和服务。

**核心价值**：
- 统一的Agent管理（Gateway架构）
- 标准化的工具调用（Plugin + MCP）
- 多渠道支持（Telegram、WhatsApp、Web控制台）

---

## 二、我的OpenClaw使用经验

### 1. 环境配置

**安装方式**：
```bash
npm install -g openclaw
```

**配置文件位置**：`~/.openclaw/openclaw.json`

**Gateway启动**：
```bash
openclaw gateway start
# 访问控制面板：http://localhost:18789/
# Token: openclaw2026
```

### 2. 已启用的Plugins（7个）

| Plugin | 功能 | 使用场景 |
|--------|------|---------|
| llm-task | LLM任务编排 | 核心功能 |
| memory-core | 记忆管理 | 上下文保持 |
| duckduckgo | 网络搜索 | 实时信息查询 |
| firecrawl | 网页爬取 | 内容提取 |
| exa | 语义搜索 | 深度搜索 |
| file-transfer | 文件传输 | 跨节点文件操作 |
| document-extract | 文档提取 | PDF/Word解析 |

**配置方式**：
```json
{
  "plugins": {
    "bundledDiscovery": "allowlist",
    "allow": ["llm-task", "duckduckgo", "firecrawl", ...],
    "entries": {
      "duckduckgo": { "enabled": true },
      ...
    }
  }
}
```

### 3. MCP Server集成

**MCP vs Plugin的区别**：

| 维度 | Plugin | MCP Server |
|------|--------|-----------|
| 定义 | OpenClaw生态的扩展 | 标准化的上下文协议 |
| 安装 | 内置或npm安装 | 独立进程，通过配置连接 |
| 适用场景 | OpenClaw特定功能 | 跨平台工具集成 |
| 示例 | duckduckgo插件 | 高德地图MCP、天气MCP |

**MCP配置示例**（在项目中）：
```json
{
  "mcpServers": {
    "amap-maps": {
      "command": "npx",
      "args": ["-y", "@amap/amap-maps-mcp-server"],
      "env": {
        "AMAP_API_KEY": "your-key"
      }
    }
  }
}
```

---

## 三、面试回答模板

### Q1: 你用过OpenClaw吗？怎么使用的？

> "用过。我在企业差旅AI Agent项目中使用了OpenClaw作为Agent编排平台。
> 
> **配置了7个Plugins**：包括llm-task（核心任务编排）、duckduckgo（网络搜索）、firecrawl（网页爬取）、file-transfer（文件操作）等。
> 
> **集成了MCP Server**：配置了高德地图MCP用于地理信息查询，在差旅规划场景中调用。
> 
> **使用方式**：
> 1. 通过Gateway启动服务（端口18789）
> 2. 在Web控制面板或命令行中与Agent交互
> 3. Agent根据任务自动调用相应的Plugin或MCP Server
> 
> 我还研究了OpenClaw的Plugin架构，理解了如何通过allowlist控制插件启用，以及如何通过配置文件管理多个MCP Server。"

### Q2: Skill的用法是什么？

> "OpenClaw 2026版本采用的是**Plugin架构**，不是独立的Skill系统。但我在Spring AI项目中实现了自定义Skill系统。
> 
> **我的Skill实现**：
> 1. **定义**：通过`@SkillComponent`注解标注，实现`Skill`接口
> 2. **注册**：启动时通过`SkillRegistry`自动扫描注册
> 3. **调用**：通过关键词匹配（`canHandle()`）或显式调用
> 
> **已实现的Skill**：
> - `WeatherQuerySkill`：天气查询
> - `TravelPlanningSkill`：差旅规划
> - `ReActTravelPlanningSkill`：ReAct模式差旅规划
> 
> **Skill vs Service vs Tool的区别**：
> - Skill：用户任务层（一个任务 = 一个Skill）
> - Service：框架能力层（ComplexityAssessor、TaskDecomposer）
> - Tool：原子操作层（API调用、CLI命令）
> 
> 这种分层设计让系统更易维护和扩展。"

### Q3: OpenClaw和MCP的关系？

> "OpenClaw是**MCP协议的实现者和编排者**。
> 
> **关系**：
> - MCP（Model Context Protocol）是Anthropic提出的标准化上下文协议
> - OpenClaw通过MCP协议连接外部工具和服务
> - OpenClaw的Plugin可以封装MCP Server，提供统一的调用接口
> 
> **实际应用**：
> 在我的项目中，高德地图MCP Server提供地理信息查询能力，OpenClaw通过MCP协议调用它，实现差旅路线规划功能。
> 
> **优势**：
> 1. 标准化：MCP是通用协议，不绑定特定平台
> 2. 解耦：MCP Server独立进程，故障隔离
> 3. 可组合：多个MCP Server可以协同工作"

---

## 四、实战Demo准备

### Demo 1：展示OpenClaw控制面板（2分钟）

1. 启动Gateway：`openclaw gateway start`
2. 打开浏览器：http://localhost:18789/
3. 输入Token：`openclaw2026`
4. 展示已启用的7个Plugins
5. 在控制面板中发送测试消息

### Demo 2：展示配置文件（1分钟）

打开 `~/.openclaw/openclaw.json`，讲解：
- `plugins.allow`：插件白名单
- `plugins.entries`：插件启用状态
- `gateway.port`：服务端口配置
- `agents.defaults.model`：默认模型配置

### Demo 3：展示MCP配置（1分钟）

打开项目中的 `src/main/resources/mcp-servers.json`，讲解：
- 高德地图MCP的配置
- command和args的作用
- 环境变量传递

---

## 五、可能的追问和回答

### Q: OpenClaw相比LangChain的优势？

> "两者定位不同：
> - **LangChain**：Python生态，偏向快速原型开发，链式调用
> - **OpenClaw**：跨语言，偏向生产级部署，Gateway架构
> 
> **OpenClaw优势**：
> 1. Gateway架构：统一的Agent管理和监控
> 2. 多渠道支持：Telegram、WhatsApp、Web控制台
> 3. MCP标准化：工具调用更规范
> 4. 企业级特性：权限控制、日志审计、故障隔离
> 
> 我的项目用Spring AI（类似LangChain的Java版），但如果要部署到生产环境，会考虑用OpenClaw做编排层。"

### Q: 如何调试OpenClaw的Plugin？

> "三种方式：
> 1. **日志查看**：`openclaw logs` 或查看 `~/.openclaw/logs/`
> 2. **控制面板**：实时查看Agent的工具调用过程
> 3. **命令行测试**：`openclaw agent --session-id test --message "测试消息"`
> 
> 我在配置duckduckgo插件时遇到网络问题，通过查看日志发现是防火墙限制，最终通过配置代理解决。"

### Q: 你会开发自定义Plugin吗？

> "理解原理，但还没实际开发过OpenClaw的Plugin。
> 
> **我的理解**：
> 1. Plugin是Node.js模块，导出特定接口
> 2. 需要实现工具描述（tool schema）和执行逻辑
> 3. 通过npm发布或本地加载
> 
> **我的经验**：
> 在Spring AI项目中开发了自定义Tool（类似Plugin），实现了天气查询、文件操作等功能。开发思路是相通的：定义接口 → 实现逻辑 → 注册到框架。
> 
> 如果需要，我可以快速学习OpenClaw的Plugin开发规范。"

---

## 六、加分项：对OpenClaw的理解

### 架构设计亮点

1. **Gateway模式**：集中式管理，便于监控和扩展
2. **Plugin热插拔**：通过配置启用/禁用，无需重启
3. **MCP标准化**：拥抱开放标准，生态互通
4. **多渠道统一**：一套Agent逻辑，多个入口（Web、Telegram、WhatsApp）

### 适用场景

- **企业级AI应用**：需要权限控制、审计日志
- **多渠道客服**：统一的Bot逻辑，多平台部署
- **复杂工作流**：需要编排多个工具和服务
- **团队协作**：多人共享Agent，统一管理

### 与我的项目结合

我的项目是**应用层**（Spring AI + 自定义Skill），OpenClaw是**编排层**（Gateway + Plugin）。

**结合方式**：
1. 把我的Skill封装成OpenClaw Plugin
2. 通过MCP协议暴露我的服务
3. 用OpenClaw Gateway统一管理多个Agent

这样既保留了Spring AI的开发效率，又获得了OpenClaw的生产级能力。

---

## 七、下一步学习计划

**短期（本周）**：
- [ ] 深入研究OpenClaw的Plugin开发文档
- [ ] 尝试开发一个简单的自定义Plugin
- [ ] 配置3-5个常用MCP Server

**中期（下周）**：
- [ ] 把项目中的Skill迁移到OpenClaw
- [ ] 实现一个复杂的多Plugin协同场景
- [ ] 准备5分钟的项目演示视频

**长期（持续）**：
- [ ] 关注OpenClaw社区动态
- [ ] 贡献代码或文档
- [ ] 在生产环境中实践

---

## 八、参考资源

- OpenClaw官方文档：https://docs.openclaw.ai/
- MCP协议文档：https://modelcontextprotocol.io/
- 我的项目GitHub：[链接]
- OpenClaw配置文件：`~/.openclaw/openclaw.json`
