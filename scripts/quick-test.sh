#!/bin/bash
# 快速测试套件 (用于pre-commit)

echo "🧪 运行快速测试套件..."

# 只运行核心测试 (不包括性能测试)
./mvnw test -Dtest=RAGEvaluationTest,ComplexityFrameworkTest -q

if [ $? -eq 0 ]; then
    echo "✅ 快速测试通过"
    exit 0
else
    echo "❌ 测试失败，请修复后再提交"
    exit 1
fi
