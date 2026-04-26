# 华为AI应用工程师一面终极准备（2天冲刺）

## 面试形式（基于真题）

### ✅ 确定会考
1. **手撕代码**：6道高频题（华为原题）
2. **项目深挖**：STAR法则，带数据
3. **Transformer基础**：Self-Attention、KV Cache、显存计算
4. **RAG原理**：查询改写、混合检索、向量召回
5. **Agent与工具调用**：ReAct、Function Call
6. **大模型微调**：LoRA、DPO、RLHF（简单了解）

### ❌ 不会深问
- 深度学习训练细节
- 复杂的数学推导
- Python/Java八股（可能简单问）

---

## 2天时间规划

### Day 1（今天）：项目优化 + 手撕代码 + Transformer基础

#### 上午（3小时）：ReAct框架优化
- [ ] 实现Observation环节
- [ ] 实现Reflection环节
- [ ] 补充测试用例
- [ ] 更新README

#### 下午（4小时）：手撕代码（重点🔥）
**必练6题**：
1. [ ] 除自身以外数组的乘积
2. [ ] 岛屿数量（DFS版本）
3. [ ] 数组第K大元素
4. [ ] ±K操作求最小极差（华为原题）
5. [ ] 最长无重复子串
6. [ ] 合并两个有序链表

**练习方式**：
- 白板写代码（不看IDE）
- 边写边讲解思路
- 分析时间/空间复杂度

#### 晚上（3小时）：Transformer基础 + 项目话术

**Transformer（1.5小时）**：
- [ ] Self-Attention公式和代码
- [ ] 7B模型BF16显存计算
- [ ] KV Cache原理和作用

**项目话术（1.5小时）**：
- [ ] 自我介绍（2分钟，背熟）
- [ ] 项目介绍（3分钟，STAR法则）
- [ ] 核心问题答案（5个）

---

### Day 2（明天）：RAG + Agent + Mock面试

#### 上午（4小时）：RAG + Agent + 微调

**RAG（2小时）**：
- [ ] RAG完整原理
- [ ] 查询改写方法
- [ ] 混合检索流程
- [ ] 向量召回代码示例
- [ ] RAG幻觉解决方法

**Agent（1小时）**：
- [ ] ReAct框架
- [ ] Function Call流程
- [ ] 工具调用失败优化

**微调（1小时，简单了解）**：
- [ ] LoRA原理
- [ ] DPO原理
- [ ] RLHF流程

#### 下午（4小时）：Mock面试 + 最后冲刺

**Mock面试（2小时）**：
- [ ] 自我介绍
- [ ] 项目介绍
- [ ] 手撕代码2题
- [ ] 技术问题10个
- [ ] 反问环节

**最后冲刺（2小时）**：
- [ ] 复习核心数据
- [ ] 复习手撕代码
- [ ] 复习项目话术

---

## 1. 自我介绍（2分钟，背熟）

"面试官好，我是张书铖，四川大学计算机专业，研究方向是大模型应用开发、RAG、Agent。

**技术栈**：熟练Java/Python，掌握Transformer、RAG、Function Call、LoRA微调、模型量化、KV Cache。

**项目经验**：
1. **企业差旅AI Agent**：RAG准确率40%→80%，工具调用率0%→100%
2. **增强版ReAct框架**：完整的Thought→Action→Observation→Reflection循环
3. **实习项目**：K8s部署、并发去重、性能优化

了解华为昇腾、MindSpore生态，希望在AI应用工程化方向深耕，非常希望加入华为。"

---

## 2. 手撕代码（6道必练）

### 2.1 除自身以外数组的乘积

**题目**：返回每个元素除自身外的乘积，不使用除法。

```java
public int[] productExceptSelf(int[] nums) {
    int n = nums.length;
    int[] res = new int[n];
    
    // 左乘积
    int left = 1;
    for (int i = 0; i < n; i++) {
        res[i] = left;
        left *= nums[i];
    }
    
    // 右乘积
    int right = 1;
    for (int i = n - 1; i >= 0; i--) {
        res[i] *= right;
        right *= nums[i];
    }
    
    return res;
}

// 测试
// 输入：[1,2,3,4]
// 输出：[24,12,8,6]
```

**时间复杂度**：O(n)  
**空间复杂度**：O(1)（不算输出数组）

---

### 2.2 岛屿数量（DFS版本）

**题目**：1表示陆地，0表示水域，求岛屿数量。

```java
public int numIslands(char[][] grid) {
    if (grid == null || grid.length == 0) return 0;
    
    int m = grid.length, n = grid[0].length;
    int count = 0;
    
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (grid[i][j] == '1') {
                count++;
                dfs(grid, i, j);
            }
        }
    }
    
    return count;
}

private void dfs(char[][] grid, int i, int j) {
    int m = grid.length, n = grid[0].length;
    
    if (i < 0 || i >= m || j < 0 || j >= n || grid[i][j] == '0') {
        return;
    }
    
    grid[i][j] = '0'; // 标记为已访问
    
    dfs(grid, i + 1, j);
    dfs(grid, i - 1, j);
    dfs(grid, i, j + 1);
    dfs(grid, i, j - 1);
}
```

**时间复杂度**：O(m×n)  
**空间复杂度**：O(m×n)（递归栈）

---

### 2.3 数组第K大元素

**题目**：找到数组中第K大的元素。

```java
import java.util.PriorityQueue;

public int findKthLargest(int[] nums, int k) {
    PriorityQueue<Integer> heap = new PriorityQueue<>();
    
    for (int num : nums) {
        heap.offer(num);
        if (heap.size() > k) {
            heap.poll();
        }
    }
    
    return heap.peek();
}

// 测试
// 输入：[3,2,1,5,6,4], k=2
// 输出：5
```

**时间复杂度**：O(n log k)  
**空间复杂度**：O(k)

---

### 2.4 ±K操作求最小极差（华为原题）

**题目**：数组每个元素可以+K或-K，求最小极差。

```java
import java.util.Arrays;

public int smallestRangeII(int[] nums, int k) {
    Arrays.sort(nums);
    int n = nums.length;
    int res = nums[n - 1] - nums[0];
    
    for (int i = 0; i < n - 1; i++) {
        int max = Math.max(nums[i] + k, nums[n - 1] - k);
        int min = Math.min(nums[0] + k, nums[i + 1] - k);
        res = Math.min(res, max - min);
    }
    
    return res;
}

// 测试
// 输入：[1,3,6], k=3
// 输出：3
```

**时间复杂度**：O(n log n)  
**空间复杂度**：O(1)

---

### 2.5 最长无重复子串

**题目**：找到最长无重复字符的子串长度。

```java
import java.util.HashMap;

public int lengthOfLongestSubstring(String s) {
    Map<Character, Integer> map = new HashMap<>();
    int left = 0, maxLen = 0;
    
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        
        if (map.containsKey(c)) {
            left = Math.max(left, map.get(c) + 1);
        }
        
        map.put(c, right);
        maxLen = Math.max(maxLen, right - left + 1);
    }
    
    return maxLen;
}

// 测试
// 输入："abcabcbb"
// 输出：3（"abc"）
```

**时间复杂度**：O(n)  
**空间复杂度**：O(min(n, m))，m为字符集大小

---

### 2.6 合并两个有序链表

**题目**：合并两个升序链表。

```java
public ListNode mergeTwoLists(ListNode l1, ListNode l2) {
    ListNode dummy = new ListNode(0);
    ListNode curr = dummy;
    
    while (l1 != null && l2 != null) {
        if (l1.val < l2.val) {
            curr.next = l1;
            l1 = l1.next;
        } else {
            curr.next = l2;
            l2 = l2.next;
        }
        curr = curr.next;
    }
    
    curr.next = (l1 != null) ? l1 : l2;
    
    return dummy.next;
}
```

**时间复杂度**：O(m + n)  
**空间复杂度**：O(1)

---

## 3. Transformer & 大模型基础

### 3.1 Transformer整体结构

**Encoder**：
- N×(多头自注意力 + FFN + 残差 + LayerNorm)

**Decoder**：
- N×(掩码自注意力 + 交叉注意力 + FFN + 残差 + LayerNorm)

**输入**：Embedding + 位置编码  
**输出**：Linear + Softmax

---

### 3.2 Self-Attention（公式 + 代码）

**公式**：
```
Attention(Q, K, V) = softmax(QK^T / √d_k) × V
```

**Java代码示例**（伪代码）：
```java
public double[][] selfAttention(double[][] Q, double[][] K, double[][] V) {
    int d_k = Q[0].length;
    
    // 1. 计算QK^T
    double[][] scores = matmul(Q, transpose(K));
    
    // 2. 缩放
    for (int i = 0; i < scores.length; i++) {
        for (int j = 0; j < scores[0].length; j++) {
            scores[i][j] /= Math.sqrt(d_k);
        }
    }
    
    // 3. Softmax
    scores = softmax(scores);
    
    // 4. 乘以V
    return matmul(scores, V);
}
```

**优势**：
- 并行计算
- 捕捉长距离依赖
- 可解释性强

---

### 3.3 7B模型BF16显存计算

**公式**：
```
参数量：7B = 7×10^9
BF16：2字节/参数
权重显存 = 7e9 × 2 = 14GB
推理总显存 ≈ 14GB + KV Cache + 激活 ≈ 20~28GB
```

**回答话术**：
"7B模型用BF16存储，每个参数2字节，权重占14GB。推理时还需要KV Cache和激活值，总显存约20-28GB。"

---

### 3.4 KV Cache原理

**原理**：
- 推理时缓存每一层的K、V矩阵
- 下一个token只需计算最新一步，不用重复计算历史token
- 速度提升巨大

**显存计算**：
```
显存 = 层数 × 头数 × 序列长度 × 头维度 × 2(K和V) × 2(BF16)
```

**回答话术**：
"KV Cache缓存每层的K、V矩阵，避免重复计算。速度提升明显，但显存会随序列长度线性增长。"

---

## 4. RAG检索增强生成

### 4.1 RAG完整原理

**流程**：
```
用户问题 → 向量检索 → 召回相关文本 → 拼入Prompt → LLM生成
```

**解决的问题**：
- 幻觉
- 知识过时
- 长文本处理

**回答话术**：
"RAG通过向量检索召回相关文档，拼入Prompt让LLM生成答案。解决了幻觉、知识过时、长文本问题。"

---

### 4.2 查询改写（Query Rewriting）

**方法**：
1. **纠错**：拼写错误、语法错误
2. **扩写**：补充关键词
3. **语义归一**：口语化→标准化
4. **多轮上下文整合**：合并历史对话

**我的实现**：
"用LLM将口语化查询改写为结构化查询。比如'去魔都出差'改写为'上海市一类城市住宿标准'，准确率提升20%。"

---

### 4.3 混合检索

**流程**：
1. **稀疏检索**：BM25（关键词匹配）
2. **稠密检索**：向量检索（语义相似度）
3. **结果融合**：RRF（Reciprocal Rank Fusion）
4. **重排序**：CrossEncoder/Reranker

**回答话术**：
"混合检索结合BM25和向量检索，BM25捕捉关键词，向量检索捕捉语义。融合后用Reranker重排，召回率更高。"

---

### 4.4 向量召回流程（代码示例）

```java
// 伪代码示例
public List<String> vectorSearch(String query, List<String> docs) {
    // 1. 文档分块
    List<String> chunks = splitDocs(docs);
    
    // 2. 向量化
    float[][] embeddings = model.encode(chunks);
    
    // 3. 建库（FAISS）
    Index index = new IndexFlatL2(embeddings[0].length);
    index.add(embeddings);
    
    // 4. 查询向量化
    float[] queryEmb = model.encode(query);
    
    // 5. 检索Top-K
    SearchResult result = index.search(queryEmb, k=5);
    
    // 6. 返回文档
    return result.getDocuments();
}
```

---

### 4.5 RAG幻觉解决方法

1. **更精细分块**：保证语义完整性
2. **多路召回 + 重排**：提升召回准确率
3. **事实校验**：对生成结果进行验证
4. **引用标注**：标注答案来源
5. **限制生成依据**：Prompt中强调"仅根据检索内容回答"

**我的实现**：
"通过Query Rewriting + Metadata Enrichment + Token-based Splitting，准确率从40%提升到80%。"

---

## 5. Agent与工具调用

### 5.1 Agent框架（ReAct）

**流程**：
```
Thought（思考） → Action（行动） → Observation（观察） → Thought → ...
```

**我的增强版**：
- 完整的Thought→Action→Observation→Reflection循环
- 分层记忆：短期/工作/长期记忆
- 细粒度状态机：PLANNING/THINKING/ACTING/OBSERVING/REFLECTING
- 错误处理：重试、降级、回滚

**回答话术**：
"我实现了增强版ReAct，加入了Observation和Reflection环节。Observation提取关键信息，Reflection判断是否调整策略。比如查天气失败→观察到'城市不存在'→反思'需要纠正'→调用地理编码→重新查询。"

---

### 5.2 Function Call完整流程

**流程**：
1. **定义工具**：name、parameters、description
2. **LLM判断**：是否需要调用工具
3. **解析参数**：从LLM输出中提取参数
4. **执行工具**：调用实际函数
5. **返回结果**：将结果返回给LLM
6. **LLM生成**：基于工具结果生成最终答案

**我的优化**：
"设计了复杂度评估框架，根据查询复杂度选择策略（SIMPLE/MEDIUM/COMPLEX）。不完全依赖LLM决策，用代码控制工具调用，工具调用率从0%提升到100%。"

---

### 5.3 工具调用失败优化方案

1. **重试机制**：临时错误（超时、网络）重试3次
2. **降级策略**：工具失败→降级为纯LLM回答
3. **错误分类**：区分临时错误和永久错误
4. **参数校验**：调用前校验参数合法性
5. **超时控制**：设置合理的超时时间

**我的实现**：
"实现了智能重试和降级。临时错误（超时）重试，永久错误（参数错误）降级。根据错误类型选择不同策略。"

---

## 6. 大模型微调与对齐（简单了解）

### 6.1 LoRA原理

**原理**：
- 冻结原模型权重
- 只训练低秩矩阵A、B
- W' = W + AB（A和B的秩很小）

**优势**：
- 显存小（只训练1-2%参数）
- 速度快
- 不修改原权重

**回答话术**：
"LoRA冻结原模型，只训练低秩矩阵。显存小、速度快，适合资源受限场景。"

---

### 6.2 DPO原理

**原理**：
- 不需要奖励模型
- 用chosen/rejected配对直接训练
- 优化目标：提高chosen概率，降低rejected概率

**优势**：
- 稳定
- 好落地
- 不需要RL

**回答话术**：
"DPO不需要奖励模型，用配对数据直接训练。比RLHF更稳定，更容易落地。"

---

### 6.3 RLHF流程

**流程**：
1. **SFT**：有监督微调
2. **训练RM**：奖励模型
3. **PPO**：强化学习对齐

**回答话术**：
"RLHF分三步：SFT微调、训练奖励模型、PPO强化学习。但训练复杂，现在更多用DPO。"

---

## 7. 项目深挖（STAR法则）

### 项目介绍模板（3分钟）

"我做的是企业差旅AI Agent平台。

**背景（Situation）**：
员工查询差旅政策需要翻阅几十页手册，效率低、容易出错。

**任务（Task）**：
搭建AI Agent系统，支持差旅政策查询和行程规划。

**行动（Action）**：
1. **RAG优化**：
   - Query Rewriting：口语化→结构化
   - Metadata Enrichment：文档预标注
   - 准确率40%→80%

2. **复杂度评估框架**：
   - 根据复杂度选策略（SIMPLE/MEDIUM/COMPLEX）
   - 工具调用率0%→100%

3. **增强版ReAct**：
   - Thought→Action→Observation→Reflection循环
   - 支持规划、反思、错误处理

**结果（Result）**：
- RAG准确率：80%（+40%）
- 工具调用率：100%（+100%）
- 平均延迟：7.5s
- 测试用例：110+

**技术栈**：Spring AI、通义千问、向量检索、MCP协议"

---

### 核心问题回答

**Q1：RAG准确率从40%到80%是怎么做到的？**

"三层优化：
1. Query Rewriting：口语化→结构化，提升20%
2. Metadata Enrichment：文档预标注，提升召回率
3. Token-based Splitting：保证语义完整性

最终准确率40%→80%，在25个测试用例上验证。"

---

**Q2：工具调用率从0%到100%是怎么做到的？**

"核心创新是复杂度评估框架。

**问题**：弱模型注册多个工具时，调用率0%

**方案**：
- 规则判断（80%）：根据关键词快速判断SIMPLE/MEDIUM/COMPLEX
- LLM确认（20%）：对COMPLEX查询二次确认
- 预编排工作流：用代码控制工具调用

**效果**：调用率0%→100%，准确率100%，延迟<500ms"

---

**Q3：介绍一下你的ReAct框架**

"增强版ReAct，完整的Thought→Action→Observation→Reflection循环。

**核心特点**：
1. Observation：提取工具返回的关键信息
2. Reflection：根据结果判断是否调整策略
3. 分层记忆：短期/工作/长期记忆
4. 细粒度状态机：PLANNING/THINKING/ACTING/OBSERVING/REFLECTING
5. 错误处理：重试、降级、回滚

**举例**：查天气失败→观察到'城市不存在'→反思'需要纠正'→调用地理编码→重新查询"

---

**Q4：为什么不用LangChain？**

"三个原因：
1. 生态适配：Java技术栈，Spring AI与Spring Boot无缝集成
2. 可控性：LangChain是黑盒，我需要自定义每个环节
3. 稳定性：LangChain在弱模型上不稳定，我的框架工具调用率100%

实测：LangChain 20% vs 我的框架 100%"

---

**Q5：并发去重是怎么实现的？**

"Redis分布式锁 + 数据库唯一索引双重保障。

**技术选型**：Redis性能高（QPS 5000+）、部署简单

**实现**：Redisson的RedLock算法 + 看门狗机制

**性能**：QPS 5000+、延迟P95<50ms

**踩坑**：
1. 时钟漂移→改用看门狗自动续期
2. 网络分区→用RedLock多实例
3. 幂等性→业务层加唯一ID校验"

---

## 8. 反问环节（高分模板）

1. "请问团队当前的业务方向和技术栈是什么？"
   - 了解业务

2. "请问AI应用工程师主要负责哪些工作？"
   - 了解岗位

3. "请问和昇腾、MindSpore结合多吗？"
   - 展示对华为技术的了解

4. "请问新人的培养和成长路径是怎样的？"
   - 展示成长意愿

5. "请问下一阶段面试的重点是什么？"
   - 了解后续流程

---

## 9. 注意事项

### ✅ 要做的
1. **数据脱口而出**：准确率80%、调用率100%、延迟7.5s
2. **边写边讲**：手撕代码时解释思路
3. **STAR法则**：Situation、Task、Action、Result
4. **承认不足**：不会的直接说，但表示愿意学习
5. **保持自信**：你的项目有亮点

### ❌ 不要做的
1. **不要背答案**：理解原理，用自己的话
2. **不要夸大**：没做过的不要说做过
3. **不要沉默**：手撕代码卡住了也要说出思路
4. **不要紧张**：深呼吸，把面试官当同行

---

## 10. 时间规划

### Day 1（今天）
- **09:00-12:00**：ReAct框架优化（3小时）
- **14:00-18:00**：手撕代码6题（4小时）
- **19:00-22:00**：Transformer基础 + 项目话术（3小时）

### Day 2（明天）
- **09:00-13:00**：RAG + Agent + 微调（4小时）
- **14:00-18:00**：Mock面试 + 最后冲刺（4小时）
- **19:00-21:00**：复习核心数据，早点休息

### 面试当天
- **提前30分钟**：到达，调整状态
- **面试前10分钟**：深呼吸，回顾核心亮点
- **面试中**：自信、清晰、简洁
- **面试后**：记录问题，复盘

---

## 成功标准

**面试结束后，你应该能说**：
- "我清楚介绍了项目核心亮点"
- "我用数据证明了技术效果"
- "我手撕代码思路清晰"
- "我回答了大部分技术问题"
- "我给面试官留下了专业印象"

**加油！华为面试，你可以的！** 🚀
