# Agent Skill 架构设计与实现 - 面试指南（修正版）

## 一、核心定义：什么是 Skill？

### 标准定义

**Skill = 面向用户任务的功能单元**

- ✅ 一个任务对应一个 Skill
- ✅ 用户可以直接理解"这个 Skill 是干什么的"
- ✅ Skill 内部可以调用多个 Service 和 Tool

### 正确示例

```
✅ WeatherQuerySkill - 查询天气
✅ TravelPlanningSkill - 规划差旅行程
✅ EmailSendSkill - 发送邮件
✅ CalendarCreateSkill - 创建日历事件
✅ CustomerVisitSkill - 规划客户拜访
```

### 错误示例

```
❌ ComplexityAssessmentSkill - 这是 Service，不是 Skill
❌ TaskDecompositionSkill - 这是 Service，不是 Skill
❌ ParallelExecutionSkill - 这是 Service，不是 Skill
❌ ResultIntegrationSkill - 这是 Service，不是 Skill
```

**关键点：**
- Skill 是用户会说的任务（"我要查天气"、"帮我规划行程"）
- Service 是框架层的能力（用户不会说"我要用复杂度评估"）

---

## 二、面试官可能的问题

### Q1: "你了解 Agent Skill 吗？你的项目是怎么用的？"

**标准回答（2分钟）：**

> "我在项目中实现了一个 Skill 架构，**Skill 是面向用户任务的功能单元**。
> 
> **我的 Skill 定义：**
> 
> 一个任务对应一个 Skill，比如：
> - WeatherQuerySkill：查询天气
> - TravelPlanningSkill：规划差旅行程
> 
> 每个 Skill 内部会调用多个 Service 和 Tool 来完成任务。
> 
> **架构设计：**
> 
> 我采用了三层架构：
> 
> 1. **Skill 层**：面向用户任务（查天气、规划行程）
> 2. **Service 层**：框架能力（复杂度评估、任务分解）
> 3. **Tool 层**：原子能力（API 调用、数据库查询）
> 
> **举个例子：**
> 
> TravelPlanningSkill 内部的实现：
> 
> ```java
> @SkillComponent(name = "travel_planning")
> public class TravelPlanningSkill implements Skill {
>     // 注入 Service（不是 Skill）
>     @Resource
>     private ComplexityAssessor complexityAssessor;
>     @Resource
>     private TaskDecomposer taskDecomposer;
>     @Resource
>     private WeatherQueryTool weatherQueryTool;
>     
>     public String execute(String query, String chatId) {
>         // 1. 调用 Service：评估复杂度
>         QueryComplexity complexity = complexityAssessor.assess(query);
>         
>         // 2. 根据复杂度选择策略
>         if (complexity == COMPLEX) {
>             // 调用 Service：任务分解
>             List<SubTask> tasks = taskDecomposer.decompose(query);
>             // 并行执行
>             // 整合结果
>         }
>         
>         return result;
>     }
> }
> ```
> 
> **技术实现：**
> 
> 使用 @SkillComponent 注解自动注册，SkillRegistry 根据查询选择合适的 Skill。
> 
> **核心价值：**
> 
> 新增业务场景只需新建一个 Skill 类，复用现有的 Service 和 Tool，扩展成本降低 90%。"

---

### Q2: "Skill 和 Tool 有什么区别？"

**标准回答（1分钟）：**

> "这是一个很好的问题。
> 
> **Tool（工具）：**
> - 原子能力，单一职责
> - 例如：查天气 API、发送邮件 API、查询数据库
> - 不包含业务逻辑
> 
> **Skill（技能）：**
> - 面向用户的任务
> - 例如：差旅规划 = 查天气 + 查路线 + 查酒店 + 整合结果
> - 包含业务逻辑，是多个 Tool 和 Service 的组合
> 
> **关系：**
> ```
> Skill（技能）
> ├── 调用多个 Service（框架能力）
> ├── 调用多个 Tool（原子能力）
> └── 包含业务逻辑（编排）
> ```
> 
> **类比：**
> - Tool = 螺丝刀、扳手（单一工具）
> - Skill = 修理汽车（组合多个工具 + 专业知识）"

---

### Q3: "你的 Skill 架构和标准的企业级 AI 一致吗？"

**标准回答（1分钟）：**

> "是的，我的 Skill 架构符合企业级标准。
> 
> **符合标准的部分：**
> 
> 1. **Skill 定义**：一个任务一个 Skill，面向用户任务
> 2. **三层架构**：Skill 层 / Service 层 / Tool 层
> 3. **自动注册**：使用注解自动扫描和注册
> 4. **降级策略**：Skill 失败自动降级到传统流程
> 
> **参考的框架：**
> 
> 我参考了 Microsoft Semantic Kernel 和 LangChain 的设计：
> - Semantic Kernel：Plugin（插件）= 我的 Skill
> - LangChain：Agent Toolkit = 我的 Skill 集合
> 
> **核心一致性：**
> 
> 我的 Skill 定义和主流框架完全一致：
> - ✅ 面向用户任务
> - ✅ 一个任务一个 Skill
> - ✅ 内部调用 Service 和 Tool
> 
> 这保证了我的架构可以无缝对接企业级 AI 系统。"

---

### Q4: "你的 Skill 架构是怎么实现的？"

**标准回答（2分钟）：**

> "我使用了**基于注解的自动注册机制**。
> 
> **1. 定义 Skill 接口**
> 
> ```java
> public interface Skill {
>     String getName();
>     String getDescription();
>     boolean canHandle(String query);
>     String execute(String query, String chatId);
> }
> ```
> 
> **2. 使用注解标记 Skill**
> 
> ```java
> @SkillComponent(
>     name = "weather_query",
>     description = "查询天气信息",
>     keywords = {"天气", "温度"}
> )
> public class WeatherQuerySkill implements Skill {
>     // 实现
> }
> ```
> 
> **3. 自动扫描注册**
> 
> ```java
> @Component
> public class SkillRegistry implements ApplicationContextAware {
>     public void setApplicationContext(ApplicationContext context) {
>         // 扫描所有 @SkillComponent 注解的类
>         Map<String, Object> beans = context.getBeansWithAnnotation(SkillComponent.class);
>         for (Object bean : beans.values()) {
>             if (bean instanceof Skill) {
>                 register((Skill) bean);
>             }
>         }
>     }
> }
> ```
> 
> **4. 智能路由**
> 
> ```java
> public String route(String query, String chatId) {
>     // 1. 选择 Skill
>     Skill skill = skillRegistry.selectSkill(query);
>     if (skill != null) {
>         return skill.execute(query, chatId);
>     }
>     
>     // 2. 降级到传统流程
>     return routeByComplexity(query, chatId);
> }
> ```
> 
> **技术亮点：**
> - 参考 Spring 的设计思想
> - 支持优先级排序
> - 支持关键词快速匹配
> - 支持降级策略"

---

## 三、核心架构图（面试时画在白板上）

```
┌─────────────────────────────────────────┐
│      WorkflowOrchestrator（编排层）      │
│  - 选择合适的 Skill                      │
│  - 降级策略                              │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│         Skill Layer（任务层）            │
│  - WeatherQuerySkill（查天气）          │
│  - TravelPlanningSkill（规划行程）      │
│  - CustomerVisitSkill（客户拜访）       │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│       Service Layer（服务层）            │
│  - ComplexityAssessor（复杂度评估）     │
│  - TaskDecomposer（任务分解）           │
│  - TaskExecutor（任务执行）             │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│         Tool Layer（工具层）             │
│  - WeatherQueryTool（天气 API）         │
│  - WebScrapingTool（网页抓取）          │
│  - PDFGenerationTool（PDF 生成）        │
└─────────────────────────────────────────┘
```

**关键点：**
- Skill = 用户任务（查天气、规划行程）
- Service = 框架能力（复杂度评估、任务分解）
- Tool = 原子能力（API 调用）

---

## 四、代码演示（面试时展示）

### 1. Skill 接口定义

```java
public interface Skill {
    String getName();
    String getDescription();
    boolean canHandle(String query);
    String execute(String query, String chatId);
}
```

### 2. Skill 实现示例

```java
@SkillComponent(
    name = "travel_planning",
    description = "规划差旅行程",
    keywords = {"规划", "行程", "出差"}
)
public class TravelPlanningSkill implements Skill {
    
    // 注入 Service（不是 Skill）
    @Resource
    private ComplexityAssessor complexityAssessor;
    
    @Resource
    private TaskDecomposer taskDecomposer;
    
    @Resource
    private WeatherQueryTool weatherQueryTool;
    
    public String execute(String query, String chatId) {
        // 1. 调用 Service：评估复杂度
        QueryComplexity complexity = complexityAssessor.assess(query);
        
        // 2. 根据复杂度处理
        if (complexity == COMPLEX) {
            // 调用 Service：任务分解
            List<SubTask> tasks = taskDecomposer.decompose(query);
            
            // 并行执行任务
            Map<String, String> results = executeTasksInParallel(tasks);
            
            // 整合结果
            return integrateResults(query, results);
        }
        
        // 简单场景直接处理
        return handleSimple(query);
    }
}
```

---

## 五、面试时的一句话总结

> **"我实现了一个标准的 Skill 架构：Skill 是面向用户任务的功能单元，一个任务对应一个 Skill。每个 Skill 内部调用多个 Service（框架能力）和 Tool（原子能力）来完成任务。这符合 Microsoft Semantic Kernel 和 LangChain 的标准定义。"**

---

## 六、可能的追问及回答

### Q: "为什么不把复杂度评估也做成 Skill？"

**A:** "因为复杂度评估不是用户任务，而是框架层的能力。

用户不会说'我要用复杂度评估 Skill'，但用户会说'我要查天气'或'帮我规划行程'。

Skill 应该是面向用户的，用户可以直接理解的任务。复杂度评估、任务分解这些是框架内部的能力，应该是 Service，而不是 Skill。

这也是 Semantic Kernel 和 LangChain 的设计原则：Plugin/Tool 都是面向用户任务的。"

### Q: "如果新增一个任务，怎么扩展？"

**A:** "非常简单，只需要新建一个 Skill 类：

```java
@SkillComponent(
    name = "customer_visit",
    description = "规划客户拜访",
    keywords = {"拜访", "客户"}
)
public class CustomerVisitSkill implements Skill {
    @Resource
    private ComplexityAssessor complexityAssessor;
    @Resource
    private TaskDecomposer taskDecomposer;
    
    public String execute(String query, String chatId) {
        // 复用现有的 Service 和 Tool
        // 30-50 行代码完成
    }
}
```

Spring 启动时会自动扫描并注册，不需要修改任何其他代码。扩展成本降低 90%。"

### Q: "Skill 执行失败怎么办？"

**A:** "我设计了降级策略：

```java
public String route(String query, String chatId) {
    // 1. 尝试使用 Skill
    Skill skill = skillRegistry.selectSkill(query);
    if (skill != null) {
        try {
            return skill.execute(query, chatId);
        } catch (Exception e) {
            log.error("Skill 执行失败，降级");
        }
    }
    
    // 2. 降级到传统复杂度评估流程
    return routeByComplexity(query, chatId);
}
```

这保证了系统的鲁棒性，Skill 失败不会导致整个系统不可用。"

---

## 七、总结

**面试时的关键点：**

1. ✅ 强调 Skill 是面向用户任务的
2. ✅ 强调一个任务一个 Skill
3. ✅ 强调 Service 和 Skill 的区别
4. ✅ 展示代码实现（注解 + 自动注册）
5. ✅ 说明符合标准（Semantic Kernel / LangChain）

**一句话总结：**

> "Skill 是面向用户任务的功能单元，一个任务对应一个 Skill。每个 Skill 内部调用多个 Service 和 Tool 来完成任务。这符合企业级 AI 的标准定义。"

**这样回答，面试官会认为你：**
1. 理解标准的 Skill 定义
2. 有清晰的架构设计思维
3. 了解主流框架（Semantic Kernel / LangChain）
4. 有工程实践能力（降级策略、鲁棒性）

**完美！** 🎉
