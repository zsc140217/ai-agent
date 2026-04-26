# Embedding 模型对比详解

## 核心问题：为什么不同模型的 Embedding 效果不同？

Embedding 模型的本质是**将文本映射到向量空间**，但不同模型在以下维度有显著差异：

---

## 1. 模型架构差异

### 1.1 基于 Transformer 的模型

#### BERT 系列（双向编码器）

**代表模型**：BERT、RoBERTa、ALBERT

**架构特点**：
```
输入：[CLS] 北京 出差 住宿 标准 [SEP]
       ↓
    Transformer Encoder（双向注意力）
       ↓
输出：[CLS向量] 作为句子表示
```

**优点**：
- ✅ 双向上下文：同时看到前后文
- ✅ 预训练任务（MLM）适合理解语义

**缺点**：
- ❌ [CLS] 向量不是专门为检索优化的
- ❌ 需要微调才能用于检索任务

#### Sentence-BERT（孪生网络）

**代表模型**：Sentence-BERT、SimCSE、bge-large-zh

**架构特点**：
```
句子A: "北京出差住宿标准"  →  BERT  →  向量A
句子B: "北京差旅酒店政策"  →  BERT  →  向量B
                                    ↓
                        计算余弦相似度
                                    ↓
                        对比学习损失函数
```

**训练方式**：
- 正样本对：语义相似的句子（如同义改写）
- 负样本对：语义不同的句子
- 目标：拉近正样本，推远负样本

**优点**：
- ✅ 专门为检索优化
- ✅ 向量可以直接用于相似度计算
- ✅ 推理速度快（无需交互）

**缺点**：
- ❌ 依赖训练数据质量
- ❌ 对否定词仍不够敏感

#### GPT 系列（单向解码器）

**代表模型**：text-embedding-ada-002（OpenAI）

**架构特点**：
```
输入：北京 出差 住宿 标准
      ↓
   Transformer Decoder（单向注意力）
      ↓
输出：最后一个 token 的向量
```

**优点**：
- ✅ 训练数据规模大
- ✅ 泛化能力强

**缺点**：
- ❌ 单向上下文（只看前文）
- ❌ 黑盒模型，无法微调

---

## 2. 训练数据差异

### 2.1 通用语料 vs 领域语料

| 模型 | 训练数据 | 适用场景 |
|------|---------|---------|
| text-embedding-v3 | 通用网页、书籍、代码 | 通用检索 |
| bge-large-zh | 中文百科、新闻、问答 | 中文 RAG |
| PubMedBERT | 医学论文 | 医疗领域 |
| FinBERT | 金融报告 | 金融领域 |

**示例**：

```python
# 通用模型
query = "心肌梗死的症状"
doc1 = "心肌梗死是一种心脏病"  # 相似度 0.75
doc2 = "MI 的临床表现包括..."   # 相似度 0.60（不认识缩写）

# 医疗领域模型（PubMedBERT）
query = "心肌梗死的症状"
doc1 = "心肌梗死是一种心脏病"  # 相似度 0.75
doc2 = "MI 的临床表现包括..."   # 相似度 0.85（认识 MI = Myocardial Infarction）
```

### 2.2 中文 vs 英文 vs 多语言

| 模型 | 语言 | 词表大小 | 中文 token 效率 |
|------|------|---------|---------------|
| BERT-base | 英文 | 30K | 低（1个汉字=2-3 tokens）|
| BERT-base-chinese | 中文 | 21K | 高（1个汉字=1 token）|
| XLM-RoBERTa | 100种语言 | 250K | 中 |
| bge-large-zh | 中文 | 21K | 高 |

**为什么中文模型更好？**

```
英文 BERT 处理中文：
"北京出差" → [北, ##京, 出, ##差] （4 tokens，语义割裂）

中文 BERT 处理中文：
"北京出差" → [北京, 出差] （2 tokens，保留词义）
```

---

## 3. 训练目标差异

### 3.1 对比学习（Contrastive Learning）

**代表模型**：SimCSE、bge-large-zh、gte-large-zh

**训练目标**：
```python
# 正样本对（语义相似）
anchor = "北京出差住宿标准"
positive = "北京差旅酒店政策"

# 负样本对（语义不同）
negative1 = "上海美食推荐"
negative2 = "深圳天气预报"

# 损失函数
loss = -log(
    exp(sim(anchor, positive) / τ) / 
    (exp(sim(anchor, positive) / τ) + Σ exp(sim(anchor, negative_i) / τ))
)
```

**效果**：
- ✅ 同义句向量距离近
- ✅ 不同主题向量距离远
- ✅ 检索精度高

### 3.2 掩码语言模型（Masked Language Model）

**代表模型**：BERT、RoBERTa

**训练目标**：
```
输入：北京 [MASK] 住宿 标准
预测：[MASK] = 出差
```

**效果**：
- ✅ 理解上下文
- ❌ 不是专门为检索优化

### 3.3 下一句预测（Next Sentence Prediction）

**代表模型**：BERT（已被淘汰）

**训练目标**：
```
句子A：北京出差住宿标准是500元
句子B：上海的标准也是500元
预测：B 是否是 A 的下一句？
```

**效果**：
- ❌ 任务与检索关系不大
- ❌ RoBERTa 证明去掉这个任务效果更好

---

## 4. 向量维度差异

| 模型 | 向量维度 | 存储成本 | 检索速度 | 精度 |
|------|---------|---------|---------|------|
| BERT-base | 768 | 中 | 快 | 中 |
| BERT-large | 1024 | 高 | 慢 | 高 |
| text-embedding-v3 | 1536 | 高 | 慢 | 高 |
| bge-small-zh | 512 | 低 | 很快 | 中 |
| bge-large-zh | 1024 | 高 | 慢 | 高 |

**权衡**：
- 维度越高 → 表达能力越强 → 精度越高 → 成本越高
- 维度越低 → 检索越快 → 存储越少 → 精度越低

**实际案例**：

```python
# 100万文档，1536维向量
存储成本 = 1,000,000 × 1536 × 4 bytes = 6.1 GB

# 100万文档，512维向量
存储成本 = 1,000,000 × 512 × 4 bytes = 2.0 GB

# 检索速度（FAISS，单机）
1536维：~50ms
512维：~15ms
```

---

## 5. 特殊能力差异

### 5.1 长文本处理

| 模型 | 最大长度 | 超长处理 |
|------|---------|---------|
| BERT-base | 512 tokens | 截断 |
| Longformer | 4096 tokens | 滑动窗口注意力 |
| e5-mistral | 32768 tokens | 分块 + 池化 |
| text-embedding-v3 | 8192 tokens | 截断 |

**示例**：

```python
# 短文本模型（BERT-base）
doc = "北京出差住宿标准..." (5000 tokens)
embedding = model.encode(doc[:512])  # 截断，丢失后半部分

# 长文本模型（e5-mistral）
doc = "北京出差住宿标准..." (5000 tokens)
embedding = model.encode(doc)  # 完整编码
```

### 5.2 多语言能力

| 模型 | 支持语言 | 跨语言检索 |
|------|---------|-----------|
| BERT-base-chinese | 中文 | ❌ |
| XLM-RoBERTa | 100种 | ✅ |
| mBERT | 104种 | ✅ |
| text-embedding-v3 | 多语言 | ✅ |

**跨语言检索示例**：

```python
# 单语言模型
query_zh = "北京出差住宿标准"
doc_en = "Beijing business travel accommodation policy"
similarity = 0.3  # 低（无法跨语言）

# 多语言模型（XLM-RoBERTa）
query_zh = "北京出差住宿标准"
doc_en = "Beijing business travel accommodation policy"
similarity = 0.75  # 高（理解跨语言语义）
```

### 5.3 否定词敏感度

| 模型 | 否定词处理 | 对比学习 | 逻辑推理 |
|------|-----------|---------|---------|
| BERT-base | ❌ 不敏感 | ❌ | ❌ |
| Sentence-BERT | ❌ 不敏感 | ✅ | ❌ |
| bge-reranker | ✅ 较敏感 | ✅ | ✅ |
| NLI-BERT | ✅ 敏感 | ✅ | ✅ |

**NLI-BERT**（Natural Language Inference）：

```python
# 训练数据：蕴含关系
前提："北京出差住宿标准是500元"
假设1："北京可以住500元的酒店"  → 蕴含（Entailment）
假设2："北京不能住500元的酒店" → 矛盾（Contradiction）

# 效果
query = "北京不能住五星级酒店吗"
doc1 = "北京住宿标准：四星及以下"  # 相似度 0.85 ✓
doc2 = "北京可以住五星级酒店"      # 相似度 0.20 ✓（识别矛盾）
```

---

## 6. 主流 Embedding 模型对比

### 6.1 国际主流模型

| 模型 | 提供方 | 维度 | 语言 | 最大长度 | 特点 |
|------|-------|------|------|---------|------|
| text-embedding-ada-002 | OpenAI | 1536 | 多语言 | 8191 | 泛化能力强，黑盒 |
| text-embedding-3-small | OpenAI | 1536 | 多语言 | 8191 | 更快，更便宜 |
| text-embedding-3-large | OpenAI | 3072 | 多语言 | 8191 | 精度最高 |
| e5-mistral-7b-instruct | Microsoft | 4096 | 多语言 | 32768 | 长文本，开源 |
| gte-large | Alibaba DAMO | 1024 | 多语言 | 512 | 开源，中文优化 |

### 6.2 国内主流模型

| 模型 | 提供方 | 维度 | 语言 | 最大长度 | 特点 |
|------|-------|------|------|---------|------|
| text-embedding-v3 | 阿里云 DashScope | 1536 | 中文为主 | 2048 | 云服务，中文优化 |
| bge-large-zh | 智源研究院 | 1024 | 中文 | 512 | 开源，检索精度高 |
| bge-reranker-large | 智源研究院 | - | 中文 | 512 | 重排序专用 |
| m3e-base | Moka | 768 | 中文 | 512 | 开源，轻量 |
| gte-large-zh | 阿里 DAMO | 1024 | 中文 | 512 | 开源，通用 |
| text2vec-large-chinese | shibing624 | 1024 | 中文 | 128 | 开源，简单 |

### 6.3 领域专用模型

| 模型 | 领域 | 基座 | 特点 |
|------|------|------|------|
| PubMedBERT | 医疗 | BERT | 医学术语理解 |
| FinBERT | 金融 | BERT | 金融术语理解 |
| SciBERT | 科研 | BERT | 科学论文检索 |
| LegalBERT | 法律 | BERT | 法律文书检索 |
| CodeBERT | 代码 | BERT | 代码语义检索 |

---

## 7. 如何选择 Embedding 模型？

### 决策树

```
是否需要跨语言检索？
├─ 是 → XLM-RoBERTa / text-embedding-v3
└─ 否 → 继续

是否是中文场景？
├─ 是 → 继续
└─ 否 → text-embedding-ada-002

是否有特定领域？
├─ 是 → 领域模型（PubMedBERT / FinBERT）
└─ 否 → 继续

是否需要本地部署？
├─ 是 → bge-large-zh / m3e-base
└─ 否 → text-embedding-v3（云服务）

是否需要处理长文本（>512 tokens）？
├─ 是 → e5-mistral / text-embedding-v3
└─ 否 → bge-large-zh

是否需要重排序？
├─ 是 → bge-reranker-large
└─ 否 → bge-large-zh
```

### 本项目的选择

**当前配置**：DashScope text-embedding-v3

**选择理由**：
1. ✅ 中文优化（理解"魔都"→"上海"）
2. ✅ 云服务（无需部署，快速落地）
3. ✅ 较长上下文（2048 tokens）
4. ✅ 成本可控（0.0005元/千tokens）

**局限性**：
1. ❌ 对否定词不够敏感（需要查询改写）
2. ❌ 黑盒模型（无法微调）
3. ❌ 数据上传到云端（隐私问题）

**未来优化方向**：

```yaml
# 阶段1：快速验证（当前）
model: text-embedding-v3
deployment: 云服务

# 阶段2：精度优化
model: bge-large-zh
deployment: 本地部署
optimization: 在企业差旅数据上微调

# 阶段3：重排序
retrieval: bge-large-zh（召回）
reranking: bge-reranker-large（精排）
```

---

## 8. 实际性能对比

### 8.1 检索精度对比（MTEB 中文榜单）

| 模型 | Retrieval | Reranking | STS | 平均 |
|------|-----------|-----------|-----|------|
| bge-large-zh-v1.5 | 70.46 | - | 65.74 | 64.53 |
| gte-large-zh | 68.52 | - | 66.12 | 63.25 |
| text2vec-large-chinese | 63.18 | - | 61.90 | 59.08 |
| m3e-large | 66.03 | - | 63.99 | 61.64 |

### 8.2 推理速度对比（单机 CPU）

| 模型 | 参数量 | 向量维度 | 编码速度 | 检索速度 |
|------|-------|---------|---------|---------|
| bge-small-zh | 102M | 512 | 1200 句/秒 | 15ms |
| bge-base-zh | 110M | 768 | 800 句/秒 | 25ms |
| bge-large-zh | 326M | 1024 | 300 句/秒 | 50ms |
| text-embedding-v3 | - | 1536 | API 延迟 | 100-200ms |

### 8.3 成本对比

| 模型 | 部署方式 | 硬件成本 | API 成本 | 维护成本 |
|------|---------|---------|---------|---------|
| text-embedding-v3 | 云服务 | 0 | 0.0005元/千tokens | 0 |
| bge-large-zh | 本地 CPU | 0 | 0 | 低 |
| bge-large-zh | 本地 GPU | 5000元（T4） | 0 | 中 |

**100万次查询成本对比**：

```python
# 云服务（text-embedding-v3）
平均查询长度 = 20 tokens
成本 = 1,000,000 × 20 / 1000 × 0.0005 = 10元

# 本地部署（bge-large-zh）
硬件成本 = 5000元（一次性）
电费 = 0.6元/度 × 8小时/天 × 30天 × 0.3度/小时 = 43元/月
人力成本 = 1000元/月（运维）
```

---

## 9. 模型微调

### 9.1 为什么要微调？

**通用模型的局限**：

```python
# 通用模型
query = "去魔都出差住宿标准"
doc1 = "上海差旅酒店政策"  # 相似度 0.65（不认识"魔都"）
doc2 = "北京住宿标准"      # 相似度 0.70（错误召回）

# 微调后的模型
query = "去魔都出差住宿标准"
doc1 = "上海差旅酒店政策"  # 相似度 0.85 ✓
doc2 = "北京住宿标准"      # 相似度 0.60
```

### 9.2 微调数据准备

**对比学习数据格式**：

```json
[
  {
    "query": "去魔都出差住宿标准",
    "positive": "上海差旅酒店政策：一类城市500元/晚",
    "negative": "北京住宿标准：一类城市500元/晚"
  },
  {
    "query": "北京出差不能住五星级酒店吗",
    "positive": "北京住宿标准：四星及以下，500元/晚",
    "negative": "北京可以住五星级酒店"
  }
]
```

**数据来源**：
1. 用户查询日志 + 点击数据
2. 人工标注（正负样本对）
3. LLM 生成（同义改写）

### 9.3 微调方法

**方法1：全量微调**

```python
from sentence_transformers import SentenceTransformer, InputExample, losses
from torch.utils.data import DataLoader

# 加载预训练模型
model = SentenceTransformer('BAAI/bge-large-zh')

# 准备训练数据
train_examples = [
    InputExample(texts=['去魔都出差', '上海差旅'], label=1.0),
    InputExample(texts=['去魔都出差', '北京住宿'], label=0.0),
]

# 训练
train_dataloader = DataLoader(train_examples, shuffle=True, batch_size=16)
train_loss = losses.CosineSimilarityLoss(model)
model.fit(train_objectives=[(train_dataloader, train_loss)], epochs=3)
```

**方法2：LoRA 微调**（推荐）

```python
from peft import LoraConfig, get_peft_model

# LoRA 配置
lora_config = LoraConfig(
    r=8,  # 低秩矩阵的秩
    lora_alpha=32,
    target_modules=["query", "value"],
    lora_dropout=0.1,
)

# 应用 LoRA
model = get_peft_model(base_model, lora_config)

# 只训练 LoRA 参数（1% 的参数量）
trainable_params = sum(p.numel() for p in model.parameters() if p.requires_grad)
# bge-large-zh: 326M → LoRA: 3.2M
```

---

## 10. 面试回答模板

### 问题：不同 Embedding 模型有什么区别？

**回答**：

"Embedding 模型的差异主要体现在 5 个维度：

**1. 模型架构**：
- BERT 系列用双向 Transformer，理解上下文
- Sentence-BERT 用孪生网络 + 对比学习，专门为检索优化
- GPT 系列用单向 Transformer，泛化能力强但只看前文

**2. 训练数据**：
- 通用模型（text-embedding-v3）在网页、书籍上训练，适合通用场景
- 领域模型（PubMedBERT）在医学论文上训练，理解专业术语
- 中文模型（bge-large-zh）在中文语料上训练，token 效率高

**3. 训练目标**：
- 对比学习（SimCSE）：拉近同义句，推远不同句，检索精度高
- 掩码语言模型（BERT）：预测被遮盖的词，理解上下文
- NLI 训练（bge-reranker）：学习蕴含关系，对否定词敏感

**4. 向量维度**：
- 高维（1536）：表达能力强，精度高，但存储和检索成本高
- 低维（512）：检索快，存储少，但精度略低
- 需要根据业务场景权衡

**5. 特殊能力**：
- 长文本处理：e5-mistral 支持 32K tokens
- 跨语言检索：XLM-RoBERTa 支持 100 种语言
- 否定词敏感：bge-reranker 用 NLI 训练，能识别矛盾

我们项目用的是 DashScope 的 text-embedding-v3，主要考虑：
- 中文优化，理解口语化查询
- 云服务，快速落地
- 1536 维，精度较高

但也有局限：
- 对否定词不够敏感（通过查询改写解决）
- 黑盒模型，无法微调

如果要进一步优化，可以考虑：
- 本地部署 bge-large-zh，在企业差旅数据上微调
- 用 bge-reranker 做重排序，提升精度
- 根据查询长度动态选择模型（短查询用 small，长查询用 large）"

---

## 相关资源

### 开源模型

- **bge 系列**：https://github.com/FlagOpen/FlagEmbedding
- **gte 系列**：https://huggingface.co/thenlper/gte-large-zh
- **m3e 系列**：https://huggingface.co/moka-ai/m3e-base
- **text2vec**：https://github.com/shibing624/text2vec

### 评测榜单

- **MTEB 中文榜单**：https://huggingface.co/spaces/mteb/leaderboard
- **C-MTEB**：https://github.com/FlagOpen/FlagEmbedding/tree/master/C_MTEB

### 论文

- **Sentence-BERT**：https://arxiv.org/abs/1908.10084
- **SimCSE**：https://arxiv.org/abs/2104.08821
- **BGE**：https://arxiv.org/abs/2309.07597
- **E5**：https://arxiv.org/abs/2212.03533

### 本项目相关代码

- **配置文件**：[application.yml](../src/main/resources/application.yml)
- **查询改写**：[QueryRewriter.java](../src/main/java/com/jblmj/aiagent/rag/QueryRewriter.java)
- **RAG 应用**：[EnterpriseAssistantApp.java](../src/main/java/com/jblmj/aiagent/app/EnterpriseAssistantApp.java)
- **评测测试**：[RAGEvaluationTest.java](../src/test/java/com/jblmj/aiagent/evaluation/RAGEvaluationTest.java)
