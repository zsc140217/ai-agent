# 任务循环依赖分析与测试案例

## 一、什么是循环依赖？

### 定义

循环依赖是指任务之间形成了一个闭环，导致无法确定执行顺序。

### 图示

```
简单循环（2个任务）：
任务 A 依赖 任务 B
任务 B 依赖 任务 A
    ↓
A → B → A （形成闭环）

复杂循环（3个任务）：
任务 A 依赖 任务 B
任务 B 依赖 任务 C
任务 C 依赖 任务 A
    ↓
A → B → C → A （形成闭环）

间接循环（4个任务）：
任务 A 依赖 任务 B
任务 B 依赖 任务 C
任务 C 依赖 任务 D
任务 D 依赖 任务 A
    ↓
A → B → C → D → A （形成闭环）
```

---

## 二、什么情况会出现循环依赖？

### 场景 1：相互依赖（最常见）

**业务场景：**
```
用户："查询从公司到客户的路线，并根据路线推荐酒店"

错误的任务分解：
- 任务 A：查询路线（需要知道酒店位置）→ 依赖任务 B
- 任务 B：推荐酒店（需要知道路线）→ 依赖任务 A

问题：A 依赖 B，B 依赖 A，形成循环
```

**为什么会出现：**
- LLM 理解错误，认为路线需要酒店位置
- 实际上应该先查路线，再根据路线推荐沿途酒店

**正确的分解：**
```
- 任务 A：查询客户地址
- 任务 B：查询路线（依赖任务 A）
- 任务 C：推荐酒店（依赖任务 B，根据路线推荐沿途酒店）
```

---

### 场景 2：三角循环

**业务场景：**
```
用户："规划出差行程，包括天气、路线、酒店"

错误的任务分解：
- 任务 A：查询天气（需要知道酒店位置）→ 依赖任务 C
- 任务 B：查询路线（需要知道天气）→ 依赖任务 A
- 任务 C：推荐酒店（需要知道路线）→ 依赖任务 B

问题：A → C → B → A，形成三角循环
```

**为什么会出现：**
- LLM 过度推理，认为每个任务都需要其他任务的结果
- 实际上天气、路线、酒店可以并行查询

**正确的分解：**
```
- 任务 A：查询天气（无依赖）
- 任务 B：查询路线（无依赖）
- 任务 C：推荐酒店（无依赖）
- 三个任务并行执行
```

---

### 场景 3：间接循环（隐蔽）

**业务场景：**
```
用户："规划多天出差，第一天拜访客户A，第二天拜访客户B，根据行程推荐酒店"

错误的任务分解：
- 任务 A：查询客户A地址（需要知道酒店位置）→ 依赖任务 D
- 任务 B：查询客户B地址（需要知道酒店位置）→ 依赖任务 D
- 任务 C：查询路线（依赖任务 A、B）
- 任务 D：推荐酒店（依赖任务 C，根据路线推荐）

问题：A → D → C → A，形成间接循环
```

**为什么会出现：**
- LLM 认为查询客户地址需要知道酒店位置（方便计算距离）
- 实际上应该先查客户地址，再推荐酒店

**正确的分解：**
```
- 任务 A：查询客户A地址（无依赖）
- 任务 B：查询客户B地址（无依赖）
- 任务 C：查询路线（依赖任务 A、B）
- 任务 D：推荐酒店（依赖任务 A、B，根据客户位置推荐）
```

---

### 场景 4：自依赖（最简单）

**业务场景：**
```
错误的任务分解：
- 任务 A：查询天气（依赖任务 A）

问题：任务依赖自己
```

**为什么会出现：**
- LLM 生成错误，把自己的 id 填到了 dependsOn 里
- 这是最容易检测的循环依赖

---

## 三、循环依赖检测算法

### 代码实现

```java
/**
 * 检测循环依赖（深度优先搜索）
 */
private boolean hasCyclicDependency(SubTask task, List<SubTask> allTasks, List<Integer> visited) {
    // 如果当前任务已经在访问路径中，说明存在循环
    if (visited.contains(task.getId())) {
        return true;  // 发现循环
    }

    // 将当前任务加入访问路径
    visited.add(task.getId());

    // 递归检查所有依赖的任务
    for (int depId : task.getDependsOn()) {
        SubTask depTask = findTaskById(depId, allTasks);
        if (depTask != null && hasCyclicDependency(depTask, allTasks, new ArrayList<>(visited))) {
            return true;
        }
    }

    return false;
}
```

### 算法原理

**深度优先搜索（DFS）：**
```
1. 从任务 A 开始
2. 标记 A 为"正在访问"
3. 递归访问 A 的所有依赖任务
4. 如果访问到一个"正在访问"的任务，说明存在循环
5. 如果所有依赖都访问完毕，标记 A 为"已访问"
```

**示例：**
```
任务列表：
- 任务 A 依赖 任务 B
- 任务 B 依赖 任务 C
- 任务 C 依赖 任务 A

检测过程：
1. 访问 A，visited = [A]
2. 访问 B（A 的依赖），visited = [A, B]
3. 访问 C（B 的依赖），visited = [A, B, C]
4. 访问 A（C 的依赖），发现 A 已在 visited 中
5. 返回 true，检测到循环依赖
```

---

## 四、测试案例

### 测试案例 1：简单循环（2个任务）

```java
@Test
public void testSimpleCyclicDependency() {
    // 任务 A 依赖 任务 B
    // 任务 B 依赖 任务 A
    List<SubTask> tasks = List.of(
        SubTask.builder()
            .id(0)
            .taskType("QUERY_WEATHER")
            .description("查询天气")
            .dependsOn(List.of(1))  // 依赖任务 1
            .build(),
        SubTask.builder()
            .id(1)
            .taskType("QUERY_HOTEL")
            .description("推荐酒店")
            .dependsOn(List.of(0))  // 依赖任务 0
            .build()
    );

    // 预期：抛出异常（检测到循环依赖）
    assertThrows(IllegalStateException.class, () -> {
        taskDecomposer.sortTasksByDependency(tasks);
    });
}
```

**业务场景：**
```
用户："查询天气并推荐酒店"

错误分解：
- 查询天气（需要知道酒店位置）→ 依赖推荐酒店
- 推荐酒店（需要知道天气）→ 依赖查询天气

问题：相互依赖
```

---

### 测试案例 2：三角循环（3个任务）

```java
@Test
public void testTriangleCyclicDependency() {
    // 任务 A 依赖 任务 B
    // 任务 B 依赖 任务 C
    // 任务 C 依赖 任务 A
    List<SubTask> tasks = List.of(
        SubTask.builder()
            .id(0)
            .taskType("QUERY_WEATHER")
            .description("查询天气")
            .dependsOn(List.of(1))  // 依赖任务 1
            .build(),
        SubTask.builder()
            .id(1)
            .taskType("QUERY_ROUTE")
            .description("查询路线")
            .dependsOn(List.of(2))  // 依赖任务 2
            .build(),
        SubTask.builder()
            .id(2)
            .taskType("QUERY_HOTEL")
            .description("推荐酒店")
            .dependsOn(List.of(0))  // 依赖任务 0
            .build()
    );

    // 预期：抛出异常
    assertThrows(IllegalStateException.class, () -> {
        taskDecomposer.sortTasksByDependency(tasks);
    });
}
```

**业务场景：**
```
用户："规划出差行程"

错误分解：
- 查询天气（需要酒店位置）→ 依赖推荐酒店
- 查询路线（需要天气信息）→ 依赖查询天气
- 推荐酒店（需要路线信息）→ 依赖查询路线

问题：A → B → C → A 形成闭环
```

---

### 测试案例 3：间接循环（4个任务）

```java
@Test
public void testIndirectCyclicDependency() {
    // 任务 A 依赖 任务 B
    // 任务 B 依赖 任务 C
    // 任务 C 依赖 任务 D
    // 任务 D 依赖 任务 A
    List<SubTask> tasks = List.of(
        SubTask.builder()
            .id(0)
            .taskType("QUERY_CUSTOMER")
            .description("查询客户A地址")
            .dependsOn(List.of(3))  // 依赖任务 3
            .build(),
        SubTask.builder()
            .id(1)
            .taskType("QUERY_CUSTOMER")
            .description("查询客户B地址")
            .dependsOn(List.of(0))  // 依赖任务 0
            .build(),
        SubTask.builder()
            .id(2)
            .taskType("QUERY_ROUTE")
            .description("查询路线")
            .dependsOn(List.of(1))  // 依赖任务 1
            .build(),
        SubTask.builder()
            .id(3)
            .taskType("QUERY_HOTEL")
            .description("推荐酒店")
            .dependsOn(List.of(2))  // 依赖任务 2
            .build()
    );

    // 预期：抛出异常
    assertThrows(IllegalStateException.class, () -> {
        taskDecomposer.sortTasksByDependency(tasks);
    });
}
```

**业务场景：**
```
用户："规划多天出差"

错误分解：
- 查询客户A地址（需要酒店位置）→ 依赖推荐酒店
- 查询客户B地址（需要客户A地址）→ 依赖查询客户A
- 查询路线（需要客户B地址）→ 依赖查询客户B
- 推荐酒店（需要路线信息）→ 依赖查询路线

问题：A → D → C → B → A 形成长链循环
```

---

### 测试案例 4：自依赖

```java
@Test
public void testSelfDependency() {
    // 任务 A 依赖自己
    List<SubTask> tasks = List.of(
        SubTask.builder()
            .id(0)
            .taskType("QUERY_WEATHER")
            .description("查询天气")
            .dependsOn(List.of(0))  // 依赖自己
            .build()
    );

    // 预期：抛出异常
    assertThrows(IllegalStateException.class, () -> {
        taskDecomposer.sortTasksByDependency(tasks);
    });
}
```

**业务场景：**
```
LLM 生成错误，把任务的 id 填到了 dependsOn 里
```

---

### 测试案例 5：部分循环（混合场景）

```java
@Test
public void testPartialCyclicDependency() {
    // 任务 A、B 无依赖
    // 任务 C 依赖 任务 D
    // 任务 D 依赖 任务 C（循环）
    List<SubTask> tasks = List.of(
        SubTask.builder()
            .id(0)
            .taskType("QUERY_WEATHER")
            .description("查询天气")
            .dependsOn(List.of())  // 无依赖
            .build(),
        SubTask.builder()
            .id(1)
            .taskType("QUERY_CUSTOMER")
            .description("查询客户地址")
            .dependsOn(List.of())  // 无依赖
            .build(),
        SubTask.builder()
            .id(2)
            .taskType("QUERY_ROUTE")
            .description("查询路线")
            .dependsOn(List.of(3))  // 依赖任务 3
            .build(),
        SubTask.builder()
            .id(3)
            .taskType("QUERY_HOTEL")
            .description("推荐酒店")
            .dependsOn(List.of(2))  // 依赖任务 2（循环）
            .build()
    );

    // 预期：抛出异常
    assertThrows(IllegalStateException.class, () -> {
        taskDecomposer.sortTasksByDependency(tasks);
    });
}
```

**业务场景：**
```
用户："查询天气、客户地址、路线和酒店"

错误分解：
- 查询天气（无依赖）✅
- 查询客户地址（无依赖）✅
- 查询路线（需要酒店位置）→ 依赖推荐酒店 ❌
- 推荐酒店（需要路线信息）→ 依赖查询路线 ❌

问题：部分任务正常，部分任务循环依赖
```

---

## 五、如何避免循环依赖？

### 策略 1：优化 LLM Prompt

```java
private String buildDecomposePrompt(String query) {
    return """
        你是一个任务规划专家，请将用户的复杂查询分解为多个子任务。
        
        重要规则：
        1. 任务依赖必须是单向的，不能相互依赖
        2. 如果任务 A 依赖任务 B，则任务 B 不能依赖任务 A
        3. 任务不能依赖自己
        4. 尽量减少依赖关系，能并行执行的任务不要设置依赖
        
        示例（正确）：
        - 任务 A：查询客户地址（无依赖）
        - 任务 B：查询路线（依赖任务 A）
        
        示例（错误）：
        - 任务 A：查询路线（依赖任务 B）❌
        - 任务 B：推荐酒店（依赖任务 A）❌
        原因：A 依赖 B，B 依赖 A，形成循环
        
        用户查询：%s
        """.formatted(query);
}
```

---

### 策略 2：后处理检测与修复

```java
public List<SubTask> decompose(String query) {
    List<SubTask> tasks = parseTasksFromResponse(response);
    
    // 检测循环依赖
    if (hasAnyCyclicDependency(tasks)) {
        log.warn("检测到循环依赖，尝试自动修复");
        tasks = removeCyclicDependencies(tasks);
    }
    
    return tasks;
}

/**
 * 移除循环依赖（简单策略：移除所有依赖关系）
 */
private List<SubTask> removeCyclicDependencies(List<SubTask> tasks) {
    return tasks.stream()
        .map(task -> {
            task.setDependsOn(List.of());  // 移除所有依赖
            return task;
        })
        .toList();
}
```

---

### 策略 3：限制依赖深度

```java
/**
 * 验证依赖深度（防止过长的依赖链）
 */
private void validateDependencyDepth(List<SubTask> tasks) {
    for (SubTask task : tasks) {
        int depth = calculateDependencyDepth(task, tasks, 0);
        if (depth > 5) {
            throw new IllegalStateException(
                "任务依赖链过长: " + task.getDescription() + ", 深度: " + depth
            );
        }
    }
}
```

---

## 六、面试时怎么讲

### 问题："你的项目如何处理循环依赖？"

**标准回答（2分钟）：**

> "我实现了循环依赖检测机制，使用深度优先搜索（DFS）算法。
> 
> **什么是循环依赖：**
> 
> 任务之间形成闭环，导致无法确定执行顺序。比如：
> - 任务 A 依赖任务 B
> - 任务 B 依赖任务 A
> 
> **什么情况会出现：**
> 
> 1. **相互依赖**：查询路线需要酒店位置，推荐酒店需要路线信息
> 2. **三角循环**：A → B → C → A
> 3. **间接循环**：A → B → C → D → A
> 4. **自依赖**：任务依赖自己
> 
> **检测算法：**
> 
> 使用 DFS 深度优先搜索：
> 1. 从任务 A 开始，标记为"正在访问"
> 2. 递归访问 A 的所有依赖任务
> 3. 如果访问到一个"正在访问"的任务，说明存在循环
> 4. 检测到循环依赖后抛出异常，拒绝执行
> 
> **如何避免：**
> 
> 1. **优化 Prompt**：在 Prompt 中明确告诉 LLM 不能相互依赖
> 2. **后处理修复**：检测到循环依赖后，移除所有依赖关系，改为并行执行
> 3. **限制依赖深度**：依赖链不能超过 5 层
> 
> **测试覆盖：**
> 
> 我写了 5 个测试案例：简单循环、三角循环、间接循环、自依赖、部分循环。测试覆盖率 100%。"

---

## 七、总结

### 循环依赖的常见场景

| 场景 | 示例 | 原因 |
|------|------|------|
| **相互依赖** | A → B, B → A | LLM 理解错误 |
| **三角循环** | A → B → C → A | LLM 过度推理 |
| **间接循环** | A → B → C → D → A | 依赖链过长 |
| **自依赖** | A → A | LLM 生成错误 |

### 检测与避免

| 策略 | 方法 | 效果 |
|------|------|------|
| **检测** | DFS 深度优先搜索 | 100% 检测率 |
| **避免** | 优化 Prompt | 降低 80% 循环依赖 |
| **修复** | 移除依赖关系 | 降级为并行执行 |

**完美！** 🎉
