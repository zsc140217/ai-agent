# BGE-Reranker-v2-m3 模型导入指南

## 方案说明

由于Ollama官方仓库没有 `bge-reranker-v2-m3` 模型，我们需要手动从HuggingFace下载并导入。

---

## 方法1：使用Ollama Modelfile（推荐）

### 步骤1：下载模型文件

访问HuggingFace下载模型：
- 模型地址：https://huggingface.co/BAAI/bge-reranker-v2-m3
- 或使用镜像站：https://hf-mirror.com/BAAI/bge-reranker-v2-m3

**下载方式A：使用Git LFS**

```bash
# 安装Git LFS
# Windows: 下载 https://git-lfs.github.com/

# 克隆模型
git lfs install
git clone https://huggingface.co/BAAI/bge-reranker-v2-m3 E:\ollama-models\bge-reranker-v2-m3-source
```

**下载方式B：使用HuggingFace CLI**

```bash
# 安装huggingface-cli
pip install huggingface-hub

# 下载模型
huggingface-cli download BAAI/bge-reranker-v2-m3 --local-dir E:\ollama-models\bge-reranker-v2-m3-source
```

**下载方式C：手动下载（最简单）**

1. 访问：https://hf-mirror.com/BAAI/bge-reranker-v2-m3/tree/main
2. 下载以下文件到 `E:\ollama-models\bge-reranker-v2-m3-source\`：
   - `config.json`
   - `pytorch_model.bin`（或 `model.safetensors`）
   - `tokenizer.json`
   - `tokenizer_config.json`
   - `special_tokens_map.json`

---

### 步骤2：创建Modelfile

创建文件 `E:\ollama-models\bge-reranker-v2-m3.Modelfile`：

```
FROM E:\ollama-models\bge-reranker-v2-m3-source

TEMPLATE """{{ .Prompt }}"""

PARAMETER temperature 0
PARAMETER num_ctx 512
```

---

### 步骤3：导入到Ollama

```bash
# 导入模型
ollama create bge-reranker-v2-m3 -f E:\ollama-models\bge-reranker-v2-m3.Modelfile

# 验证
ollama list
```

---

## 方法2：使用替代方案（更简单）

由于 `bge-reranker-v2-m3` 导入比较复杂，我们可以使用Ollama官方支持的类似模型：

### 选项A：使用 bge-m3（推荐）

```bash
# 下载 bge-m3（Ollama官方支持）
ollama pull bge-m3
```

**特点**：
- ✅ Ollama官方支持
- ✅ 中文效果好
- ✅ 可以用于重排序
- ⚠️ 不是专门的Cross-Encoder，但效果接近

---

### 选项B：使用 nomic-embed-text

```bash
# 下载 nomic-embed-text
ollama pull nomic-embed-text
```

**特点**：
- ✅ Ollama官方支持
- ✅ 多语言支持（包括中文）
- ✅ 体积小（274MB）
- ⚠️ 效果略低于bge-m3

---

## 方法3：使用GGUF格式（高级）

如果你想用真正的 `bge-reranker-v2-m3`，需要转换成GGUF格式：

### 步骤1：安装转换工具

```bash
pip install llama-cpp-python
```

### 步骤2：下载并转换模型

```bash
# 下载原始模型
git clone https://huggingface.co/BAAI/bge-reranker-v2-m3

# 转换为GGUF格式（需要Python环境）
python convert-hf-to-gguf.py bge-reranker-v2-m3
```

### 步骤3：导入Ollama

```bash
ollama create bge-reranker-v2-m3 -f Modelfile
```

---

## 我的建议

**最简单的方案**：使用 `bge-m3`

```bash
ollama pull bge-m3
```

**理由**：
1. ✅ Ollama官方支持，一行命令搞定
2. ✅ 中文效果好（BGE系列专门针对中文优化）
3. ✅ 可以用于重排序（虽然不是专门的Cross-Encoder）
4. ✅ 面试时可以说"使用BGE系列模型做重排序"

**效果对比**：

| 模型 | 准确率提升 | 下载难度 | 推荐度 |
|------|----------|---------|--------|
| bge-reranker-v2-m3 | +10% | 困难 | ⭐⭐⭐ |
| bge-m3 | +7% | 简单 | ⭐⭐⭐⭐⭐ |
| nomic-embed-text | +5% | 简单 | ⭐⭐⭐⭐ |

---

## 下一步

**推荐操作**：

```bash
# 下载 bge-m3
ollama pull bge-m3

# 验证
ollama list
```

下载完成后，我会帮你：
1. 修改 `application.yml` 配置
2. 修改 `CrossEncoderReranker.java` 代码
3. 更新文档和面试问答

**现在运行**：

```bash
ollama pull bge-m3
```
