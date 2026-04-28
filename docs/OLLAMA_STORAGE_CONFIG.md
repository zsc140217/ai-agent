# Ollama 模型存储路径配置指南

## 问题：C盘空间不足，如何将模型下载到E盘？

Ollama默认将模型存储在C盘，如果C盘空间不足，可以通过设置环境变量将模型存储到其他盘。

---

## 方法1：永久设置（推荐）

### Windows系统

**步骤1：设置系统环境变量**

1. 右键"此电脑" → "属性"
2. 点击"高级系统设置"
3. 点击"环境变量"
4. 在"系统变量"区域点击"新建"
5. 输入：
   - 变量名：`OLLAMA_MODELS`
   - 变量值：`E:\ollama-models`（你想存放的路径）
6. 点击"确定"保存所有窗口

**步骤2：重启Ollama服务**

方式1：通过任务管理器
- 打开任务管理器（Ctrl+Shift+Esc）
- 找到"Ollama"进程
- 右键 → "结束任务"
- Ollama会自动重启（或手动启动）

方式2：通过命令行
```bash
# 结束Ollama进程
taskkill /F /IM ollama.exe

# 重新启动Ollama
ollama serve
```

**步骤3：验证配置**

```bash
# 下载模型（会自动下载到E盘）
ollama pull bge-reranker-v2-m3

# 检查文件位置
dir E:\ollama-models
```

你应该能在 `E:\ollama-models` 看到下载的模型文件。

---

## 方法2：临时设置（不推荐）

如果不想修改系统环境变量，可以每次启动Ollama时临时指定：

```bash
# 在CMD中运行
set OLLAMA_MODELS=E:\ollama-models
ollama serve
```

**缺点**：
- 每次启动都要手动设置
- 关闭CMD窗口后失效

---

## 方法3：使用部署脚本（最简单）

我已经修改了部署脚本，自动设置路径到E盘：

```bash
cd scripts
setup-ollama.bat
```

脚本会自动：
1. 设置 `OLLAMA_MODELS=E:\ollama-models`
2. 下载模型到E盘
3. 提示你设置永久环境变量

---

## 常见问题

### Q1: 如何查看当前模型存储路径？

**Windows**：
```bash
echo %OLLAMA_MODELS%
```

如果显示空白，说明使用默认路径（C盘）。

**默认路径**：
- Windows: `C:\Users\<用户名>\.ollama\models`
- Linux: `~/.ollama/models`
- Mac: `~/.ollama/models`

---

### Q2: 已经下载到C盘了，如何迁移到E盘？

**步骤1：设置新路径**
```bash
# 设置环境变量
set OLLAMA_MODELS=E:\ollama-models
```

**步骤2：复制已有模型**
```bash
# 复制C盘的模型到E盘
xcopy "C:\Users\<用户名>\.ollama\models" "E:\ollama-models" /E /I /H
```

**步骤3：删除C盘旧文件（可选）**
```bash
# 确认E盘模型可用后，删除C盘文件
rmdir /S /Q "C:\Users\<用户名>\.ollama\models"
```

**步骤4：重启Ollama**
```bash
taskkill /F /IM ollama.exe
ollama serve
```

---

### Q3: 设置后还是下载到C盘？

**可能原因**：

1. **环境变量未生效**
   - 解决：重启Ollama服务
   - 解决：重启电脑

2. **设置了用户变量而不是系统变量**
   - 解决：在"系统变量"区域设置，不是"用户变量"

3. **路径包含中文或特殊字符**
   - 解决：使用纯英文路径，如 `E:\ollama-models`

**验证方法**：
```bash
# 查看环境变量
echo %OLLAMA_MODELS%

# 应该输出：E:\ollama-models
```

---

### Q4: 模型大小是多少？

| 模型 | 大小 | 用途 |
|------|------|------|
| bge-reranker-v2-m3 | 1.2GB | 重排序（推荐） |
| bge-reranker-base | 400MB | 重排序（备选） |
| nomic-embed-text | 274MB | 通用embedding |

**建议预留空间**：2GB（包括模型和缓存）

---

### Q5: 可以把模型放在移动硬盘或网络盘吗？

**可以，但不推荐**：

- **移动硬盘**：可以，但速度慢，影响重排序性能
- **网络盘**：不推荐，网络延迟会严重影响性能
- **固态硬盘**：推荐，速度快

**最佳实践**：
- 开发环境：本地固态硬盘（E盘）
- 生产环境：服务器本地磁盘

---

## 完整配置示例

### 1. 设置环境变量

```
变量名: OLLAMA_MODELS
变量值: E:\ollama-models
```

### 2. 重启Ollama

```bash
taskkill /F /IM ollama.exe
ollama serve
```

### 3. 下载模型

```bash
ollama pull bge-reranker-v2-m3
```

### 4. 验证

```bash
# 查看模型列表
ollama list

# 查看文件位置
dir E:\ollama-models
```

### 5. 测试

```bash
# 测试模型
curl http://localhost:11434/api/embeddings -d "{\"model\": \"bge-reranker-v2-m3\", \"prompt\": \"test\"}"
```

---

## 总结

**推荐方案**：设置系统环境变量 `OLLAMA_MODELS=E:\ollama-models`

**优点**：
- 永久生效
- 所有用户共享
- 不需要每次手动设置

**步骤**：
1. 设置环境变量
2. 重启Ollama
3. 运行部署脚本：`scripts\setup-ollama.bat`

**预期结果**：
- 模型存储在 `E:\ollama-models`
- C盘空间不受影响
- 重排序功能正常工作
