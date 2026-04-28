#!/bin/bash

# Ollama 部署脚本 - Windows版本
# 用于快速部署 bge-reranker-v2-m3 重排序模型

echo "=========================================="
echo "Ollama 重排序模型部署脚本"
echo "=========================================="

# 检查 Ollama 是否已安装
echo ""
echo "[步骤1] 检查 Ollama 安装状态..."
if ! command -v ollama &> /dev/null
then
    echo "❌ Ollama 未安装"
    echo ""
    echo "请按照以下步骤安装 Ollama："
    echo "1. 访问: https://ollama.com/download/windows"
    echo "2. 下载并安装 Ollama for Windows"
    echo "3. 安装完成后重新运行此脚本"
    echo ""
    exit 1
else
    echo "✅ Ollama 已安装"
    ollama --version
fi

# 检查 Ollama 服务是否运行
echo ""
echo "[步骤2] 检查 Ollama 服务状态..."
if curl -s http://localhost:11434 > /dev/null 2>&1
then
    echo "✅ Ollama 服务正在运行"
else
    echo "❌ Ollama 服务未运行"
    echo ""
    echo "请启动 Ollama 服务："
    echo "- Windows: Ollama 应该会自动启动，检查系统托盘"
    echo "- 或手动运行: ollama serve"
    echo ""
    exit 1
fi

# 拉取 bge-reranker-v2-m3 模型
echo ""
echo "[步骤3] 拉取 bge-reranker-v2-m3 模型..."
echo "注意: 首次下载约 1.2GB，需要几分钟时间"
echo ""

# 检查模型是否已存在
if ollama list | grep -q "bge-reranker-v2-m3"
then
    echo "✅ 模型已存在，跳过下载"
else
    echo "开始下载模型..."
    ollama pull bge-reranker-v2-m3

    if [ $? -eq 0 ]; then
        echo "✅ 模型下载成功"
    else
        echo "❌ 模型下载失败"
        echo ""
        echo "可能的原因："
        echo "1. 网络连接问题"
        echo "2. Ollama 官方仓库中没有此模型"
        echo ""
        echo "备选方案："
        echo "1. 使用 bge-reranker-base (更小的模型)"
        echo "   ollama pull bge-reranker-base"
        echo ""
        echo "2. 使用通用 embedding 模型做重排"
        echo "   ollama pull nomic-embed-text"
        echo ""
        exit 1
    fi
fi

# 验证模型
echo ""
echo "[步骤4] 验证模型..."
ollama list

# 测试模型
echo ""
echo "[步骤5] 测试模型..."
echo "发送测试请求..."

curl -s http://localhost:11434/api/embeddings -d '{
  "model": "bge-reranker-v2-m3",
  "prompt": "query: 北京住宿标准 document: 北京一类城市住宿标准500元"
}' > /tmp/ollama_test.json

if [ $? -eq 0 ]; then
    echo "✅ 模型测试成功"
    echo ""
    echo "响应示例:"
    cat /tmp/ollama_test.json | head -n 5
    echo "..."
else
    echo "❌ 模型测试失败"
    exit 1
fi

# 完成
echo ""
echo "=========================================="
echo "✅ Ollama 重排序模型部署完成！"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 启动 Spring Boot 应用: ./mvnw spring-boot:run"
echo "2. 运行重排序测试: ./mvnw test -Dtest=RerankerTest"
echo ""
echo "配置信息："
echo "- Ollama 地址: http://localhost:11434"
echo "- 重排序模型: bge-reranker-v2-m3"
echo "- 配置文件: src/main/resources/application.yml"
echo ""
