# 文档索引

本目录包含项目的所有技术文档，已清理重复和过时内容。

## 📚 文档分类

### RAG 检索系统（6个）

| 文档 | 说明 | 用途 |
|------|------|------|
| [RAG_INDEXING_RETRIEVAL_COMPLETE_GUIDE.md](RAG_INDEXING_RETRIEVAL_COMPLETE_GUIDE.md) | **RAG 完整技术指南** | 数据库选型、Chunk策略、索引优化、检索器实现、性能优化 |
| [THREE_WAY_RETRIEVAL_GUIDE.md](THREE_WAY_RETRIEVAL_GUIDE.md) | 三路召回指南 | BM25 + Dense + Rewritten 召回架构 |
| [ENTERPRISE_QUERY_REWRITE.md](ENTERPRISE_QUERY_REWRITE.md) | 查询改写指南 | 否定词处理、同义词扩展、实体识别 |
| [RERANKER_INTERVIEW_QA.md](RERANKER_INTERVIEW_QA.md) | 重排序面试问答 | 交叉编码重排序原理、性能优化、面试要点 |
| [RAG_INTERVIEW_QA.md](RAG_INTERVIEW_QA.md) | RAG 面试问答 | 常见面试问题和回答模板 |
| [RAG_INTERVIEW_CHEATSHEET.md](RAG_INTERVIEW_CHEATSHEET.md) | RAG 快速参考 | 核心概念速查表 |

### 系统架构（4个）

| 文档 | 说明 | 用途 |
|------|------|------|
| [COMPLEXITY_FRAMEWORK_REPORT.md](COMPLEXITY_FRAMEWORK_REPORT.md) | 复杂度框架报告 | 查询复杂度评估、工作流编排 |
| [WORKFLOW_ORCHESTRATION_REPORT.md](WORKFLOW_ORCHESTRATION_REPORT.md) | 工作流编排报告 | Skill-first 路由策略 |
| [TASK_DECOMPOSER_OPTIMIZATION.md](TASK_DECOMPOSER_OPTIMIZATION.md) | 任务分解优化 | 复杂查询分解、依赖管理 |
| [REACT_VS_COMPLEXITY_SYSTEM.md](REACT_VS_COMPLEXITY_SYSTEM.md) | ReAct vs 复杂度系统 | 两种架构对比分析 |

### 测试相关（3个）

| 文档 | 说明 | 用途 |
|------|------|------|
| [TEST_GUIDE.md](TEST_GUIDE.md) | 测试指南 | 如何运行各类测试 |
| [RETRIEVAL_STRATEGY_TEST_GUIDE.md](RETRIEVAL_STRATEGY_TEST_GUIDE.md) | 检索策略测试 | 对比不同检索策略效果 |
| [TEST_RESULTS_TEMPLATE.md](TEST_RESULTS_TEMPLATE.md) | 测试结果模板 | 标准化测试报告格式 |

### 技术选型（2个）

| 文档 | 说明 | 用途 |
|------|------|------|
| [EMBEDDING_MODELS_COMPARISON.md](EMBEDDING_MODELS_COMPARISON.md) | Embedding 模型对比 | text-embedding-v1/v2/v3 性能对比 |
| [MCP_TEST_GUIDE.md](MCP_TEST_GUIDE.md) | MCP 测试指南 | Model Context Protocol 集成测试 |

### 业务功能（1个）

| 文档 | 说明 | 用途 |
|------|------|------|
| [TRAVEL_PLANNING_SKILL_GUIDE.md](TRAVEL_PLANNING_SKILL_GUIDE.md) | 差旅规划技能 | TravelPlanningSkill 实现指南 |

### 问题分析（1个）

| 文档 | 说明 | 用途 |
|------|------|------|
| [FAILURE_CASE_ANALYSIS.md](FAILURE_CASE_ANALYSIS.md) | 失败案例分析 | 典型问题和解决方案 |

### 面试准备（1个）

| 文档 | 说明 | 用途 |
|------|------|------|
| [HUAWEI_INTERVIEW_FINAL.md](HUAWEI_INTERVIEW_FINAL.md) | 华为面试最终版 | 面试准备材料 |

### 快速开始（1个）

| 文档 | 说明 | 用途 |
|------|------|------|
| [QUICK_START.md](QUICK_START.md) | 快速开始 | 项目启动和基本使用 |

---

## 🎯 推荐阅读路径

### 新手入门
1. [QUICK_START.md](QUICK_START.md) - 快速启动项目
2. [TEST_GUIDE.md](TEST_GUIDE.md) - 运行测试验证
3. [RAG_INDEXING_RETRIEVAL_COMPLETE_GUIDE.md](RAG_INDEXING_RETRIEVAL_COMPLETE_GUIDE.md) - 理解核心架构

### 深入理解
1. [THREE_WAY_RETRIEVAL_GUIDE.md](THREE_WAY_RETRIEVAL_GUIDE.md) - 三路召回原理
2. [RERANKER_INTERVIEW_QA.md](RERANKER_INTERVIEW_QA.md) - 重排序实现
3. [COMPLEXITY_FRAMEWORK_REPORT.md](COMPLEXITY_FRAMEWORK_REPORT.md) - 复杂度框架

### 面试准备
1. [RAG_INTERVIEW_CHEATSHEET.md](RAG_INTERVIEW_CHEATSHEET.md) - 快速复习
2. [RAG_INTERVIEW_QA.md](RAG_INTERVIEW_QA.md) - 常见问题
3. [HUAWEI_INTERVIEW_FINAL.md](HUAWEI_INTERVIEW_FINAL.md) - 面试实战

---

## 📊 文档统计

- **总文档数**: 19 个
- **RAG 相关**: 6 个
- **系统架构**: 4 个
- **测试相关**: 3 个
- **其他**: 6 个

---

## 🔄 最近更新

- 2026-04-29: 创建 `RAG_INDEXING_RETRIEVAL_COMPLETE_GUIDE.md`（最新完整指南）
- 2026-04-29: 清理重复文档，从 40 个精简到 19 个
- 2026-04-27: 更新 `RERANKER_INTERVIEW_QA.md`
- 2026-04-27: 更新 `THREE_WAY_RETRIEVAL_GUIDE.md`

---

## 📝 文档维护规范

1. **避免重复**: 同一主题只保留一个最新、最完整的文档
2. **及时更新**: 代码变更后同步更新相关文档
3. **清晰命名**: 文件名要能准确反映内容
4. **分类管理**: 按功能模块组织文档
5. **定期清理**: 每月检查并删除过时文档
