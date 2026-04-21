# Skill 路由与匹配策略 - 企业级方案

## 一、当前方案的问题

### 问题 1：关键词匹配不准确

**当前实现：**
```java
public boolean canHandle(String query) {
    String[] keywords = {"天气", "温度", "下雨"};
    for (String keyword : keywords) {
        if (query.contains(keyword)) {
            return true;
        }
    }
    return false;
}
```

**问题场景：**
```
用户："今天天气不错，帮我规划一下行程"
❌ 错误匹配：WeatherQuerySkill（因为包含"天气"）
✅ 正确匹配：TravelPlanningSkill（用户真实意图是规划行程）

用户："北京温度适合穿什么衣服"
✅ 正确匹配：WeatherQuerySkill
但如果有 ClothingRecommendationSkill，应该匹配它

用户："查一下天气预报，然后帮我订酒店"
❌ 问题：包含多个意图，单个 Skill 无法处理
```

**核心问题：**
- 关键词匹配只看"是否包含"，不理解语义
- 无法处理多意图查询
- 无法处理歧义（"天气"可能是主要意图，也可能是次要信息）

---

## 二、企业级 Skill 路由方案

### 方案对比

| 方案 | 准确率 | 延迟 | 成本 | 适用场景 |
|------|--------|------|------|---------|
| **关键词匹配** | 60% | < 1ms | 免费 | 简单场景、原型验证 |
| **语义匹配（Embedding）** | 85% | 50ms | 低 | 中等规模、生产环境 |
| **LLM 路由** | 95% | 1-2s | 高 | 复杂场景、高准确率要求 |
| **混合路由** | 90% | 100ms | 中 | **企业级推荐** ⭐ |

---

## 三、企业级方案：混合路由

### 架构设计

```
用户查询
    ↓
┌─────────────────────────────────────┐
│  第一层：关键词快速过滤（< 1ms）      │
│  - 过滤明显不相关的 Skill            │
│  - 减少后续计算量                    │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│  第二层：语义匹配（50ms）            │
│  - 计算查询与 Skill 描述的相似度     │
│  - 使用 Embedding 向量相似度         │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│  第三层：LLM 确认（1-2s，可选）      │
│  - 只在相似度接近时调用              │
│  - 让 LLM 最终决策                   │
└─────────────────────────────────────┘
    ↓
选择最合适的 Skill
```

---

## 四、方案 1：语义匹配（Embedding）

### 核心思想

使用 Embedding 计算查询与 Skill 描述的语义相似度。

### 实现代码

```java
@Component
public class SemanticSkillMatcher {
    
    @Resource
    private EmbeddingModel embeddingModel;
    
    // 缓存 Skill 描述的 Embedding
    private Map<String, float[]> skillEmbeddings = new HashMap<>();
    
    /**
     * 初始化：预计算所有 Skill 描述的 Embedding
     */
    @PostConstruct
    public void init() {
        List<Skill> skills = skillRegistry.getAllSkills();
        for (Skill skill : skills) {
            String description = skill.getDescription();
            float[] embedding = embeddingModel.embed(description);
            skillEmbeddings.put(skill.getName(), embedding);
        }
    }
    
    /**
     * 语义匹配：计算查询与 Skill 的相似度
     */
    public Skill matchSkill(String query) {
        // 1. 计算查询的 Embedding
        float[] queryEmbedding = embeddingModel.embed(query);
        
        // 2. 计算与每个 Skill 的相似度
        Map<String, Double> similarities = new HashMap<>();
        for (Map.Entry<String, float[]> entry : skillEmbeddings.entrySet()) {
            double similarity = cosineSimilarity(queryEmbedding, entry.getValue());
            similarities.put(entry.getKey(), similarity);
        }
        
        // 3. 选择相似度最高的 Skill
        String bestSkillName = similarities.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        // 4. 如果相似度太低，返回 null
        if (similarities.get(bestSkillName) < 0.7) {
            return null;  // 没有合适的 Skill
        }
        
        return skillRegistry.getSkill(bestSkillName);
    }
    
    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

### 效果对比

```
用户："今天天气不错，帮我规划一下行程"

关键词匹配：
- WeatherQuerySkill: 匹配（包含"天气"）❌ 错误
- TravelPlanningSkill: 匹配（包含"规划"）✅ 正确

语义匹配：
- WeatherQuerySkill: 相似度 0.45（低）
- TravelPlanningSkill: 相似度 0.82（高）✅ 正确选择
```

---

## 五、方案 2：LLM 路由

### 核心思想

让 LLM 根据所有 Skill 的描述，选择最合适的 Skill。

### 实现代码

```java
@Component
public class LLMSkillRouter {
    
    @Resource
    private ChatModel chatModel;
    
    @Resource
    private SkillRegistry skillRegistry;
    
    /**
     * LLM 路由：让 LLM 选择最合适的 Skill
     */
    public Skill routeByLLM(String query) {
        // 1. 构建 Prompt
        String prompt = buildRoutingPrompt(query);
        
        // 2. 调用 LLM
        String response = chatModel.call(prompt);
        
        // 3. 解析 LLM 返回的 Skill 名称
        String skillName = parseSkillName(response);
        
        // 4. 返回 Skill
        return skillRegistry.getSkill(skillName);
    }
    
    /**
     * 构建路由 Prompt
     */
    private String buildRoutingPrompt(String query) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个智能路由器，根据用户查询选择最合适的 Skill。\n\n");
        prompt.append("可用的 Skill：\n");
        
        // 列出所有 Skill 的描述
        List<Skill> skills = skillRegistry.getAllSkills();
        for (Skill skill : skills) {
            prompt.append(String.format("- %s: %s\n", 
                skill.getName(), skill.getDescription()));
        }
        
        prompt.append("\n用户查询：").append(query).append("\n\n");
        prompt.append("请选择最合适的 Skill，只返回 Skill 名称（如 weather_query）。\n");
        prompt.append("如果没有合适的 Skill，返回 none。");
        
        return prompt.toString();
    }
    
    /**
     * 解析 LLM 返回的 Skill 名称
     */
    private String parseSkillName(String response) {
        // 简单解析，提取 Skill 名称
        response = response.trim().toLowerCase();
        if (response.equals("none")) {
            return null;
        }
        return response;
    }
}
```

### Prompt 示例

```
你是一个智能路由器，根据用户查询选择最合适的 Skill。

可用的 Skill：
- weather_query: 查询天气信息，支持单城市查询和多城市对比
- travel_planning: 规划差旅行程，整合天气、路线、酒店、政策等信息

用户查询：今天天气不错，帮我规划一下行程

请选择最合适的 Skill，只返回 Skill 名称（如 weather_query）。
如果没有合适的 Skill，返回 none。
```

**LLM 返回：** `travel_planning`

---

## 六、方案 3：混合路由（推荐）⭐

### 核心思想

结合关键词、语义匹配、LLM 路由的优势，分层决策。

### 实现代码

```java
@Component
public class HybridSkillRouter {
    
    @Resource
    private SkillRegistry skillRegistry;
    
    @Resource
    private SemanticSkillMatcher semanticMatcher;
    
    @Resource
    private LLMSkillRouter llmRouter;
    
    /**
     * 混合路由：分层决策
     */
    public Skill route(String query) {
        log.info("[HybridRouter] 开始路由: {}", query);
        
        // 第一层：关键词快速过滤
        List<Skill> candidates = filterByKeywords(query);
        log.info("[HybridRouter] 关键词过滤后剩余 {} 个候选", candidates.size());
        
        if (candidates.isEmpty()) {
            // 没有候选，直接返回 null
            return null;
        }
        
        if (candidates.size() == 1) {
            // 只有一个候选，直接返回
            return candidates.get(0);
        }
        
        // 第二层：语义匹配
        Skill bestSkill = semanticMatcher.matchSkill(query, candidates);
        double bestSimilarity = semanticMatcher.getSimilarity(query, bestSkill);
        log.info("[HybridRouter] 语义匹配最佳: {} (相似度: {})", 
            bestSkill.getName(), bestSimilarity);
        
        // 如果相似度很高（> 0.85），直接返回
        if (bestSimilarity > 0.85) {
            return bestSkill;
        }
        
        // 如果相似度中等（0.7-0.85），且候选数量 <= 3，用 LLM 确认
        if (bestSimilarity > 0.7 && candidates.size() <= 3) {
            log.info("[HybridRouter] 相似度中等，使用 LLM 确认");
            return llmRouter.routeByLLM(query, candidates);
        }
        
        // 相似度太低，返回 null
        return null;
    }
    
    /**
     * 第一层：关键词快速过滤
     */
    private List<Skill> filterByKeywords(String query) {
        return skillRegistry.getAllSkills().stream()
                .filter(skill -> skill.canHandle(query))
                .collect(Collectors.toList());
    }
}
```

### 决策流程

```
用户查询："今天天气不错，帮我规划一下行程"
    ↓
第一层：关键词过滤
- WeatherQuerySkill: 匹配（包含"天气"）
- TravelPlanningSkill: 匹配（包含"规划"）
候选：2 个
    ↓
第二层：语义匹配
- WeatherQuerySkill: 相似度 0.45
- TravelPlanningSkill: 相似度 0.82
最佳：TravelPlanningSkill (0.82)
    ↓
判断：0.82 < 0.85，且候选数 = 2，使用 LLM 确认
    ↓
第三层：LLM 确认
LLM 返回：travel_planning
    ↓
最终选择：TravelPlanningSkill ✅
```

### 性能对比

| 场景 | 关键词 | 语义匹配 | LLM | 混合路由 |
|------|--------|---------|-----|---------|
| 简单查询（"北京天气"） | ✅ 1ms | ✅ 50ms | ✅ 1.5s | ✅ 1ms（第一层） |
| 歧义查询（"天气不错，规划行程"） | ❌ 错误 | ✅ 50ms | ✅ 1.5s | ✅ 1.6s（三层） |
| 复杂查询（多意图） | ❌ 错误 | ⚠️ 中等 | ✅ 1.5s | ✅ 1.6s（三层） |

**结论：混合路由兼顾准确率和性能**

---

## 七、防止用错 Skill 的策略

### 策略 1：置信度阈值

```java
public Skill route(String query) {
    Skill skill = semanticMatcher.matchSkill(query);
    double confidence = semanticMatcher.getConfidence();
    
    // 置信度太低，拒绝执行
    if (confidence < 0.7) {
        log.warn("置信度太低: {}, 拒绝执行", confidence);
        return null;  // 降级到传统流程
    }
    
    return skill;
}
```

### 策略 2：多候选确认

```java
public Skill route(String query) {
    List<Skill> topSkills = semanticMatcher.getTopN(query, 3);
    
    // 如果前两名相似度接近，用 LLM 确认
    if (topSkills.size() >= 2) {
        double diff = topSkills.get(0).similarity - topSkills.get(1).similarity;
        if (diff < 0.1) {
            log.info("前两名相似度接近，使用 LLM 确认");
            return llmRouter.routeByLLM(query, topSkills);
        }
    }
    
    return topSkills.get(0).skill;
}
```

### 策略 3：用户确认（交互式）

```java
public Skill route(String query) {
    List<Skill> candidates = semanticMatcher.getTopN(query, 3);
    
    // 如果有多个候选，让用户选择
    if (candidates.size() > 1) {
        return askUserToSelect(candidates);
    }
    
    return candidates.get(0);
}

private Skill askUserToSelect(List<Skill> candidates) {
    System.out.println("我找到了多个可能的功能，请选择：");
    for (int i = 0; i < candidates.size(); i++) {
        System.out.println((i + 1) + ". " + candidates.get(i).getDescription());
    }
    // 等待用户输入
}
```

---

## 八、面试时怎么讲

### 问题："你的关键词匹配不准确，企业级方案是什么？"

**标准回答（2分钟）：**

> "你说得对，关键词匹配确实不够准确。企业级方案通常使用**混合路由**。
> 
> **问题场景：**
> 
> 用户问'今天天气不错，帮我规划一下行程'，关键词匹配会错误地选择 WeatherQuerySkill（因为包含'天气'），但用户真实意图是规划行程。
> 
> **企业级方案：混合路由**
> 
> 分三层决策：
> 
> 1. **第一层：关键词快速过滤**（< 1ms）
>    - 过滤明显不相关的 Skill
>    - 减少后续计算量
> 
> 2. **第二层：语义匹配**（50ms）
>    - 使用 Embedding 计算查询与 Skill 描述的相似度
>    - 选择相似度最高的 Skill
> 
> 3. **第三层：LLM 确认**（1-2s，可选）
>    - 只在相似度接近时调用
>    - 让 LLM 最终决策
> 
> **效果：**
> 
> - 80% 的查询在第一层就能正确匹配（< 1ms）
> - 15% 的查询需要第二层语义匹配（50ms）
> - 5% 的查询需要第三层 LLM 确认（1-2s）
> - 平均延迟 < 100ms，准确率 90%
> 
> **防止用错 Skill 的策略：**
> 
> 1. 置信度阈值：相似度 < 0.7 拒绝执行
> 2. 多候选确认：前两名相似度接近时用 LLM 确认
> 3. 降级策略：Skill 路由失败降级到传统流程
> 
> **我的实现：**
> 
> 目前使用关键词匹配（原型验证阶段），下一步计划实现语义匹配。代码已经预留了接口，可以无缝升级。"

---

## 九、总结

### 方案对比

| 方案 | 准确率 | 延迟 | 成本 | 推荐场景 |
|------|--------|------|------|---------|
| 关键词匹配 | 60% | < 1ms | 免费 | 原型验证 |
| 语义匹配 | 85% | 50ms | 低 | 生产环境 |
| LLM 路由 | 95% | 1-2s | 高 | 高准确率要求 |
| **混合路由** | **90%** | **100ms** | **中** | **企业级推荐** ⭐ |

### 实现优先级

1. **P0（当前）**：关键词匹配 ✅
2. **P1（下一步）**：语义匹配（Embedding）
3. **P2（优化）**：混合路由（关键词 + 语义 + LLM）

### 面试加分项

> "我目前使用关键词匹配，但我知道这不够准确。企业级方案应该使用混合路由：关键词快速过滤 + 语义匹配 + LLM 确认。我的代码已经预留了接口，可以无缝升级到语义匹配。"

**这样回答，面试官会认为你：**
1. ✅ 有问题意识（知道关键词匹配的局限）
2. ✅ 了解企业级方案（语义匹配、混合路由）
3. ✅ 有架构设计能力（预留接口、可扩展）
4. ✅ 有工程思维（分层决策、降级策略）

**完美！** 🎉
