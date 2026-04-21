# 项目实现的 Skill 列表及经典举例

## 一、项目中实现的 Skill

### 已实现的 Skill（2个）

| Skill 名称 | 功能描述 | 使用场景 | 复杂度 |
|-----------|---------|---------|--------|
| **WeatherQuerySkill** | 查询天气信息 | "北京今天天气怎么样"<br>"上海和广州天气对比" | ⭐⭐ |
| **TravelPlanningSkill** | 规划差旅行程 | "帮我规划明天去杭州的行程"<br>"去深圳出差，查天气和推荐酒店" | ⭐⭐⭐⭐ |

---

## 二、最经典的举例：WeatherQuerySkill

### 为什么选 WeatherQuerySkill 作为经典举例？

1. ✅ **简单易懂**：天气查询是最常见的场景，面试官容易理解
2. ✅ **功能完整**：支持单城市查询和多城市对比，展示了 Skill 的灵活性
3. ✅ **代码简洁**：核心逻辑清晰，适合面试时快速讲解
4. ✅ **实际可用**：真实调用了和风天气 API，不是玩具项目

---

## 三、面试时的经典举例（完整版）

### 场景设定

> "我以 WeatherQuerySkill 为例，这是一个查询天气信息的 Skill。"

---

### 1. Skill 定义

```java
@SkillComponent(
    name = "weather_query",
    description = "查询天气信息，支持单城市查询和多城市对比",
    layer = SkillLayer.BUSINESS,
    keywords = {"天气", "温度", "下雨", "带伞"},
    priority = 50
)
public class WeatherQuerySkill implements Skill {
    
    @Resource
    private WeatherQueryTool weatherQueryTool;  // 注入 Tool
    
    // 实现 Skill 接口的方法
}
```

**讲解要点：**
- 使用 @SkillComponent 注解，Spring 启动时自动注册
- keywords 用于快速匹配（包含"天气"关键词就能匹配）
- 注入 WeatherQueryTool（Tool 层），不是直接调用 API

---

### 2. 执行流程

```java
public String execute(String query, String chatId) {
    // 1. 提取城市列表
    List<String> cities = extractCities(query);
    
    // 2. 根据城市数量选择策略
    if (cities.size() == 1) {
        // 单城市查询
        return handleSingleCityQuery(cities.get(0));
    } else {
        // 多城市对比
        return handleMultiCityComparison(cities);
    }
}
```

**讲解要点：**
- 自动提取城市名称（支持 20 个主要城市）
- 根据城市数量选择不同策略（单城市 vs 多城市）
- 展示了 Skill 的智能性

---

### 3. 单城市查询示例

**用户输入：** "北京今天天气怎么样"

**执行流程：**
```
1. SkillRegistry 选择 WeatherQuerySkill（匹配关键词"天气"）
2. WeatherQuerySkill.execute() 被调用
3. 提取城市：["北京"]
4. 调用 WeatherQueryTool.queryWeather("北京")
5. 返回结果：
   "北京今天天气：晴，温度 15-25℃，空气质量良好，
    适合出差。建议穿轻薄外套。"
```

**代码：**
```java
private String handleSingleCityQuery(String city) {
    try {
        // 调用 Tool 层
        String result = weatherQueryTool.queryWeather(city);
        return result;
    } catch (Exception e) {
        return "抱歉，" + city + " 的天气查询失败";
    }
}
```

---

### 4. 多城市对比示例

**用户输入：** "上海和广州天气对比"

**执行流程：**
```
1. SkillRegistry 选择 WeatherQuerySkill
2. 提取城市：["上海", "广州"]
3. 并行调用 WeatherQueryTool.queryWeather("上海")
           WeatherQueryTool.queryWeather("广州")
4. 整合结果：
   "以下是各城市的天气对比：
   
   【上海】
   天气：多云，温度 18-26℃
   
   【广州】
   天气：晴，温度 22-30℃
   
   综合建议：广州天气更适宜出差。"
```

**代码：**
```java
private String handleMultiCityComparison(List<String> cities) {
    StringBuilder result = new StringBuilder();
    result.append("以下是各城市的天气对比：\n\n");
    
    for (String city : cities) {
        String weatherInfo = weatherQueryTool.queryWeather(city);
        result.append("【").append(city).append("】\n");
        result.append(weatherInfo).append("\n\n");
    }
    
    result.append("综合建议：根据以上天气信息，您可以选择天气更适宜的城市进行出差。");
    return result.toString();
}
```

---

## 四、面试时的完整讲解（3分钟）

### 开场（30秒）

> "我以 WeatherQuerySkill 为例，这是一个查询天气信息的 Skill。它支持单城市查询和多城市对比，是一个典型的面向用户任务的 Skill。"

---

### 展开（2分钟）

> **"Skill 定义："**
> 
> 使用 @SkillComponent 注解标记，包含 name、description、keywords 等元数据。Spring 启动时会自动扫描并注册到 SkillRegistry。
> 
> **"执行流程："**
> 
> 当用户问"北京今天天气怎么样"时：
> 
> 1. SkillRegistry 根据关键词"天气"匹配到 WeatherQuerySkill
> 2. 调用 execute() 方法
> 3. 自动提取城市名称"北京"
> 4. 调用 WeatherQueryTool（Tool 层）查询天气 API
> 5. 返回结果给用户
> 
> **"智能性体现："**
> 
> 如果用户问"上海和广州天气对比"，Skill 会自动识别出两个城市，并行查询两次天气 API，然后整合结果。这展示了 Skill 的灵活性。
> 
> **"架构设计："**
> 
> WeatherQuerySkill 是 Skill 层，它调用 WeatherQueryTool（Tool 层）。Tool 负责实际的 API 调用，Skill 负责业务逻辑（提取城市、选择策略、整合结果）。
> 
> 这种分层设计保证了职责清晰：
> - Skill：业务逻辑
> - Tool：技术实现

---

### 总结（30秒）

> "这个 Skill 虽然简单，但展示了 Skill 架构的核心特点：
> - ✅ 面向用户任务（查天气）
> - ✅ 自动注册和匹配
> - ✅ 灵活的策略选择（单城市 vs 多城市）
> - ✅ 清晰的分层设计（Skill → Tool）
> 
> 新增其他 Skill（如酒店推荐、路线规划）也是同样的模式。"

---

## 五、面试时的代码演示（推荐展示这段）

```java
// 1. Skill 定义（注解）
@SkillComponent(
    name = "weather_query",
    description = "查询天气信息",
    keywords = {"天气", "温度"}
)
public class WeatherQuerySkill implements Skill {
    
    @Resource
    private WeatherQueryTool weatherQueryTool;  // Tool 层
    
    // 2. 核心执行逻辑
    public String execute(String query, String chatId) {
        // 提取城市
        List<String> cities = extractCities(query);
        
        // 单城市 vs 多城市
        if (cities.size() == 1) {
            return weatherQueryTool.queryWeather(cities.get(0));
        } else {
            return compareWeather(cities);
        }
    }
    
    // 3. 匹配逻辑
    public boolean canHandle(String query) {
        return query.contains("天气") || query.contains("温度");
    }
}
```

**讲解要点：**
- 代码简洁（核心逻辑 < 20 行）
- 职责清晰（Skill 负责业务，Tool 负责技术）
- 易于扩展（新增城市只需修改 CITIES 列表）

---

## 六、与 TravelPlanningSkill 的对比

| 维度 | WeatherQuerySkill | TravelPlanningSkill |
|------|------------------|-------------------|
| **复杂度** | ⭐⭐ 简单 | ⭐⭐⭐⭐ 复杂 |
| **调用的 Service** | 无 | ComplexityAssessor、TaskDecomposer |
| **调用的 Tool** | WeatherQueryTool | WeatherQueryTool、其他 |
| **适合面试讲解** | ✅ 推荐（简单易懂） | ⚠️ 可选（展示复杂场景） |

**建议：**
- 面试时**优先讲 WeatherQuerySkill**（简单、清晰）
- 如果面试官追问复杂场景，再讲 TravelPlanningSkill

---

## 七、面试时的话术模板

### 简短版（1分钟）

> "我实现了 2 个 Skill：WeatherQuerySkill 和 TravelPlanningSkill。
> 
> 以 WeatherQuerySkill 为例，它支持单城市查询和多城市对比。当用户问'北京今天天气怎么样'时，Skill 会自动提取城市名称，调用 WeatherQueryTool 查询天气 API，返回结果。
> 
> 如果用户问'上海和广州天气对比'，Skill 会识别出两个城市，并行查询，然后整合结果。
> 
> 这展示了 Skill 的核心特点：面向用户任务、自动匹配、灵活策略。"

---

### 完整版（3分钟）

> "我实现了 2 个 Skill：
> 
> **1. WeatherQuerySkill（天气查询）**
> - 功能：单城市查询、多城市对比
> - 使用场景：'北京今天天气怎么样'、'上海和广州天气对比'
> - 技术实现：自动提取城市、调用 WeatherQueryTool、整合结果
> 
> **2. TravelPlanningSkill（差旅规划）**
> - 功能：根据复杂度选择策略，整合天气、路线、酒店等信息
> - 使用场景：'帮我规划明天去杭州的行程'
> - 技术实现：调用 ComplexityAssessor、TaskDecomposer 等 Service
> 
> **以 WeatherQuerySkill 为例：**
> 
> [展开讲解执行流程、代码实现、架构设计]
> 
> 这个 Skill 虽然简单，但展示了 Skill 架构的核心特点：面向用户任务、自动注册、灵活策略、清晰分层。"

---

## 八、总结

**面试时推荐的举例顺序：**

1. ✅ **首选：WeatherQuerySkill**
   - 简单易懂
   - 功能完整（单城市 + 多城市）
   - 代码简洁
   - 适合快速讲解

2. ⚠️ **备选：TravelPlanningSkill**
   - 展示复杂场景
   - 调用多个 Service
   - 适合深入讨论

**核心话术：**

> "我实现了 2 个 Skill。以 WeatherQuerySkill 为例，它支持单城市查询和多城市对比。当用户问'北京今天天气怎么样'时，Skill 会自动提取城市、调用 WeatherQueryTool、返回结果。这展示了 Skill 的核心特点：面向用户任务、自动匹配、灵活策略。"

**完美！** 🎉
