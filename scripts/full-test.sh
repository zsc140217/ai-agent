#!/bin/bash
# 完整测试套件 (用于CI/CD或手动运行)

echo "🧪 运行完整测试套件..."

# 运行所有测试
./mvnw test

# 生成测试报告
echo "📊 测试报告: target/surefire-reports/"

# 统计测试结果
TOTAL=$(find target/surefire-reports -name "TEST-*.xml" 2>/dev/null | wc -l)
echo "✅ 完成 $TOTAL 个测试类"
