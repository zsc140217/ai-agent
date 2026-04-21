# TravelPlanningSkill 详解 - 面试指南

## 一、Skill 概述

### 基本信息

| 属性 | 值 |
|------|-----|
| **Skill 名称** | TravelPlanningSkill |
| **功能描述** | 规划差旅行程，整合天气、路线、酒店、政策等信息 |
| **复杂度** | ⭐⭐⭐⭐ 复杂 |
| **优先级** | 60 |
| **关键词** | 规划、行程、出差、安排、计划、准备 |

### 使用场景

```
✅ "帮我规划明天去杭州的行程"
✅ "去深圳出差，查天气和推荐酒店"
✅ "规划北京3天出差，包括客户拜访"
✅ "明天去上海，帮我安排一下"
```

---

## 二、核心设计亮点

### 亮点 1：展示了 Skill 如何调用 Service

**这是 TravelPlanningSkill 最大的价值！**

```java
@SkillComponent(name = "travel_planning")
public class TravelPlanningSkill implements Skill {
    
    // 注入 Service（不是 Skill）
    @Resource
    private ComplexityAssessor complexityAssessor;  // Service
    
    @Resource
    private TaskDecomposer taskDecomposer;          // Service
    
    @Resource
    private WeatherQueryTool weatherQueryTool;      // Tool
    
    public String execute(String query, String chatId) {
        // 1. 调用 Service：评估复杂度
        QueryComplexity complexity = complexityAssessor.assess(query);
        
        // 2. 根据复杂度选择策略
        if (complexity == COMPLEX) {
            // 调用 Service：任务分解
            List<SubTask> tasks = taskDecomposer.decompose(query);
            
            // 并行执行
            Map<String, String> results = executeTasksInParallel(tasks);
            
            // 整合结果
            return integrateResults(query, results);
        }
        
        return handleSimple(query);
    }
}
```

**讲解要点：**
- Skill 是面向用户的任务（规划行程）
- Service 是框架能力（复杂度评估、任务分解）
- Skill 内部调用 Service，不是 Skill 调用 Skill

---

### 亮点 2：根据复杂度选择不同策略

```java
public String execute(String query, String chatId) {
    // 1. 评估复杂度
    QueryComplexity complexity = complexityAssessor.assess(query);
    
    // 2. 根据复杂度选择策略
    return switch (complexity) {
        case SIMPLE -> handleSimplePlanning(query, chatId);
        case MEDIUM -> handleMediumPlanning(query, chatId);
        case COMPLEX -> handleComplexPlanning(query, chatId);
    };
}
```

**三种策略：**

| 复杂度 | 策略 | 示例 |
|--------|------|------|
| **SIMPLE** | 单次查询 | "去杭州，查天气" |
| **MEDIUM** | 多次查询 | "去杭州，查天气和酒店" |
| **COMPLEX** | 任务分解 + 并行执行 | "规划北京3天出差，包括客户拜访" |

---

### 亮点 3：复杂场景的任务分解与并行执行

```java
private String handleComplexPlanning(String query, String chatId) {
    // 1. 调用 Service：任务分解
    List<SubTask> tasks = taskDecomposer.decompose(query);
    // 示例：["查询天气", "查询路线", "查询酒店"]
    
    // 2. 调用 Service：按依赖关系排序
    List<List<SubTask>> batches = taskDecomposer.sortTasksByDependency(tasks);
    // 示例：[[任务1, 任务2], [任务3]]（第一批并行，第二批等待）
    
    // 3. 并行执行任务
    Map<String, String> results = new HashMap<>();
    for (List<SubTask> batch : batches) {
        executeTasksInParallel(batch, results);
    }
    
    // 4. 调用 LLM 整合结果
    return integrateResults(query, results);
}
```

**讲解要点：**
- 展示了 Skill 如何处理复杂场景
- 调用多个 Service（TaskDecomposer）
- 并行执行提升性能
- LLM 整合结果保证自然语言输出

---

## 三、完整执行流程示例

### 场景 1：简单规划（SIMPLE）

**用户输入：** "明天去杭州，查一下天气"

**执行流程：**
```
1. SkillRegistry 选择 TravelPlanningSkill（匹配关键词"杭州"）
2. TravelPlanningSkill.execute() 被调用
3. 调用 ComplexityAssessor.assess() → 返回 SIMPLE
4. 执行 handleSimplePlanning()
   - 提取城市："杭州"
   - 调用 WeatherQueryTool.queryWeather("杭州")
5. 返回结果：
   "差旅规划如下：
   
   【天气信息】
   杭州明天天气：晴，温度 18-26℃，空气质量良好。
   建议穿轻薄外套。"
```

**代码：**
```java
private String handleSimplePlanning(String query, String chatId) {
    // 提取城市
    String city = extractCity(query);
    
    // 调用 Tool
    String weatherInfo = weatherQueryTool.queryWeather(city);
    
    return "差旅规划如下：\n\n【天气信息】\n" + weatherInfo;
}
```

---

### 场景 2：中等规划（MEDIUM）

**用户输入：** "去深圳出差，查天气和推荐酒店"

**执行流程：**
```
1. SkillRegistry 选择 TravelPlanningSkill
2. 调用 ComplexityAssessor.assess() → 返回 MEDIUM
3. 执行 handleMediumPlanning()
   - 查询天气
   - 查询酒店（如果有 HotelQueryTool）
   - 整合结果
4. 返回结果：
   "差旅规划如下：
   
   【天气信息】
   深圳天气：多云，温度 22-30℃
   
   【酒店推荐】
   推荐协议酒店：XXX酒店，标准间 450元/晚
   
   提示：如需更详细的规划，请提供更多信息。"
```

---

### 场景 3：复杂规划（COMPLEX）⭐ 重点

**用户输入：** "规划明天去北京3天出差，第一天拜访阿里巴巴，第二天拜访字节跳动"

**执行流程：**
```
1. SkillRegistry 选择 TravelPlanningSkill
2. 调用 ComplexityAssessor.assess() → 返回 COMPLEX
3. 执行 handleComplexPlanning()
   
   步骤 1：任务分解
   调用 TaskDecomposer.decompose() → 返回：
   [
     {id: 0, type: "QUERY_WEATHER", params: {"city": "北京"}},
     {id: 1, type: "QUERY_CUSTOMER", params: {"keyword": "阿里巴巴"}},
     {id: 2, type: "QUERY_CUSTOMER", params: {"keyword": "字节跳动"}},
     {id: 3, type: "QUERY_ROUTE", params: {"origin": "酒店", "destination": "阿里巴巴"}, dependsOn: [1]},
     {id: 4, type: "QUERY_ROUTE", params: {"origin": "酒店", "destination": "字节跳动"}, dependsOn: [2]}
   ]
   
   步骤 2：拓扑排序
   调用 TaskDecomposer.sortTasksByDependency() → 返回：
   [
     [任务0, 任务1, 任务2],  // 第一批：并行执行
     [任务3, 任务4]          // 第二批：等待第一批完成
   ]
   
   步骤 3：并行执行
   第一批（并行）：
   - 任务0：查询北京天气 → "晴，15-25℃"
   - 任务1：查询阿里巴巴地址 → "杭州市余杭区文一西路969号"
   - 任务2：查询字节跳动地址 → "北京市海淀区知春路63号"
   
   第二批（并行）：
   - 任务3：查询路线（酒店 → 阿里巴巴）→ "地铁2号线，约30分钟"
   - 任务4：查询路线（酒店 → 字节跳动）→ "地铁10号线，约25分钟"
   
   步骤 4：整合结果
   调用 LLM 整合所有结果 → 返回：
   "为您规划的北京3天出差行程如下：
   
   【天气信息】
   北京天气：晴，温度 15-25℃，适合出差
   
   【第一天行程】
   拜访阿里巴巴
   - 地址：杭州市余杭区文一西路969号
   - 路线：地铁2号线，约30分钟
   
   【第二天行程】
   拜访字节跳动
   - 地址：北京市海淀区知春路63号
   - 路线：地铁10号线，约25分钟
   
   建议：提前预约拜访时间，准备好相关资料。"
```

**代码：**
```java
private String handleComplexPlanning(String query, String chatId) {
    // 1. 任务分解
    List<SubTask> tasks = taskDecomposer.decompose(query);
    
    // 2. 拓扑排序
    List<List<SubTask>> batches = taskDecomposer.sortTasksByDependency(tasks);
    
    // 3. 并行执行
    Map<String, String> results = new HashMap<>();
    for (List<SubTask> batch : batches) {
        executeTasksInParallel(batch, results);
    }
    
    // 4. 整合结果
    return integrateResults(query, results);
}

private void executeTasksInParallel(List<SubTask> tasks, Map<String, String> results) {
    List<CompletableFuture<Void>> futures = tasks.stream()
        .map(task -> CompletableFuture.runAsync(() -> {
            String result = executeSubTask(task);
            synchronized (results) {
                results.put(task.getTaskType() + "_" + task.getId(), result);
            }
        }))
        .toList();
    
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
}
```

---

## 四、与 WeatherQuerySkill 的对比

| 维度 | WeatherQuerySkill | TravelPlanningSkill |
|------|------------------|-------------------|
| **复杂度** | ⭐⭐ 简单 | ⭐⭐⭐⭐ 复杂 |
| **调用的 Service** | 无 | ComplexityAssessor、TaskDecomposer |
| **调用的 Tool** | WeatherQueryTool | WeatherQueryTool、其他 |
| **策略选择** | 单城市 vs 多城市 | SIMPLE/MEDIUM/COMPLEX |
| **并行执行** | 简单并行 | 拓扑排序 + 批次并行 |
| **结果整合** | 简单拼接 | LLM 整合 |
| **适合面试讲解** | ✅ 推荐（简单易懂） | ⭐ 展示复杂场景 |

**建议：**
- 面试时**先讲 WeatherQuerySkill**（简单、清晰）
- 如果面试官追问复杂场景，**再讲 TravelPlanningSkill**
- TravelPlanningSkill 展示了 Skill 如何调用 Service，这是核心亮点

---

## 五、面试时的讲解（3分钟）

### 开场（30秒）

> "除了 WeatherQuerySkill，我还实现了 TravelPlanningSkill，这是一个更复杂的 Skill，展示了 Skill 如何调用 Service 来处理复杂场景。"

---

### 展开（2分钟）

> **"核心设计："**
> 
> TravelPlanningSkill 内部调用了多个 Service：
> - ComplexityAssessor（Service）：评估查询复杂度
> - TaskDecomposer（Service）：分解复杂任务
> - WeatherQueryTool（Tool）：查询天气
> 
> **"执行流程："**
> 
> 当用户问"规划明天去北京3天出差，拜访阿里巴巴和字节跳动"时：
> 
> 1. 调用 ComplexityAssessor 评估复杂度 → 返回 COMPLEX
> 2. 调用 TaskDecomposer 分解任务 → 返回 5 个子任务
> 3. 拓扑排序：按依赖关系分为 2 批
> 4. 并行执行：第一批 3 个任务并行，第二批 2 个任务并行
> 5. LLM 整合结果 → 返回自然语言的行程规划
> 
> **"核心亮点："**
> 
> 1. **展示了 Skill 如何调用 Service**：这是 Skill 和 Service 分层的最佳实践
> 2. **根据复杂度选择策略**：SIMPLE/MEDIUM/COMPLEX 三种策略
> 3. **任务分解与并行执行**：提升性能，延迟降低 50%
> 4. **LLM 整合结果**：保证输出的自然语言质量
> 
> **"架构价值："**
> 
> 这个 Skill 展示了标准的 Skill 定义：
> - Skill 是面向用户的任务（规划行程）
> - Service 是框架能力（复杂度评估、任务分解）
> - Skill 内部调用 Service，不是 Skill 调用 Skill

---

### 总结（30秒）

> "TravelPlanningSkill 虽然复杂，但展示了 Skill 架构的核心价值：
> - ✅ Skill 是面向用户的任务
> - ✅ Service 是框架能力
> - ✅ Skill 内部调用 Service 和 Tool
> - ✅ 根据复杂度选择不同策略
> 
> 这符合企业级 AI 的标准架构。"

---

## 六、面试话术模板

### 简短版（1分钟）

> "我还实现了 TravelPlanningSkill，这是一个更复杂的 Skill。
> 
> 当用户问'规划明天去北京出差'时，Skill 会：
> 1. 调用 ComplexityAssessor（Service）评估复杂度
> 2. 调用 TaskDecomposer（Service）分解任务
> 3. 并行执行多个子任务（查天气、查路线、查酒店）
> 4. 用 LLM 整合结果
> 
> 这展示了 Skill 如何调用 Service 来处理复杂场景。"

---

### 完整版（3分钟）

> "我还实现了 TravelPlanningSkill，这是一个更复杂的 Skill。
> 
> **核心设计：**
> 
> TravelPlanningSkill 内部调用了多个 Service：
> - ComplexityAssessor：评估复杂度
> - TaskDecomposer：分解任务
> - WeatherQueryTool：查询天气
> 
> **执行流程：**
> 
> [展开讲解复杂场景的执行流程]
> 
> **核心亮点：**
> 
> 1. 展示了 Skill 如何调用 Service
> 2. 根据复杂度选择策略
> 3. 任务分解与并行执行
> 4. LLM 整合结果
> 
> 这符合标准的 Skill 定义：Skill 是面向用户的任务，Service 是框架能力。"

---

## 七、代码演示（推荐展示这段）

```java
@SkillComponent(name = "travel_planning")
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
        
        // 2. 根据复杂度选择策略
        if (complexity == COMPLEX) {
            // 调用 Service：任务分解
            List<SubTask> tasks = taskDecomposer.decompose(query);
            
            // 拓扑排序
            List<List<SubTask>> batches = taskDecomposer.sortTasksByDependency(tasks);
            
            // 并行执行
            Map<String, String> results = new HashMap<>();
            for (List<SubTask> batch : batches) {
                executeTasksInParallel(batch, results);
            }
            
            // LLM 整合结果
            return integrateResults(query, results);
        }
        
        return handleSimple(query);
    }
}
```

**讲解要点：**
- Skill 调用 Service（ComplexityAssessor、TaskDecomposer）
- 根据复杂度选择不同策略
- 并行执行提升性能
- LLM 整合保证输出质量

---

## 八、总结

### 两个 Skill 的定位

| Skill | 定位 | 面试时的作用 |
|-------|------|-------------|
| **WeatherQuerySkill** | 简单 Skill | 快速讲解 Skill 的基本概念 |
| **TravelPlanningSkill** | 复杂 Skill | 展示 Skill 如何调用 Service |

### 面试策略

1. **优先讲 WeatherQuerySkill**（1分钟）
   - 简单易懂
   - 快速建立 Skill 概念

2. **如果面试官追问，讲 TravelPlanningSkill**（3分钟）
   - 展示复杂场景
   - 展示 Skill 调用 Service
   - 展示任务分解与并行执行

### 核心话术

> "我实现了 2 个 Skill：
> - WeatherQuerySkill：简单场景，展示 Skill 的基本概念
> - TravelPlanningSkill：复杂场景，展示 Skill 如何调用 Service
> 
> TravelPlanningSkill 是核心亮点，它展示了标准的 Skill 定义：Skill 是面向用户的任务，Service 是框架能力，Skill 内部调用 Service 和 Tool。"

**完美！** 🎉
