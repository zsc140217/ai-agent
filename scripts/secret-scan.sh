#!/bin/bash
# 敏感信息扫描脚本

echo "🔍 扫描敏感信息..."

# 检查是否有API密钥泄露
if git diff --cached | grep -iE "(api[_-]?key|secret|password|token|dashscope|qweather)" | grep -v "YOUR_"; then
    echo "❌ 检测到可能的API密钥泄露！"
    echo "请检查以下内容："
    git diff --cached | grep -iE "(api[_-]?key|secret|password|token)" | head -5
    exit 1
fi

# 检查application.yml中的真实密钥
if git diff --cached --name-only | grep -q "application.yml"; then
    if git diff --cached src/main/resources/application.yml | grep -E "api-key: [^Y]" | grep -v "YOUR_"; then
        echo "❌ application.yml中检测到真实API密钥！"
        exit 1
    fi
fi

echo "✅ 未检测到敏感信息泄露"
exit 0
