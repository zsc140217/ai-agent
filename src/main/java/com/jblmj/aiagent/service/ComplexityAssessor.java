package com.jblmj.aiagent.service;

import com.jblmj.aiagent.model.QueryComplexity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

/**
 * 查询复杂度评估器
 *
 * 核心功能：
 * 1. 判断用户查询的复杂度（SIMPLE / MEDIUM / COMPLEX）
 * 2. 采用混合判断策略：规则判断（快速）+ LLM 判断（准确）
 * 3. 80% 的查询用规则判断，20% 的查询用 LLM 判断
 *
 * 面试价值：
 * - 展示对 Agent 架构的深刻理解（不同复杂度用不同策略）
 * - 体现工程化思维（性能 vs 准确性的权衡）
 * - 证明你理解如何优化 LLM 调用成本
 *
 * @author jblmj
 */
@Service
@Slf4j
public class ComplexityAssessor {

    @Resource
    @Qualifier("dashscopeChatModel")
    private ChatModel chatModel;

    private ChatClient chatClient;

    @PostConstruct
    public void init() {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 评估查询复杂度（混合策略）
     *
     * @param query 用户查询
     * @return 复杂度等级
     */
    public QueryComplexity assess(String query) {
        // 1. 快速筛选：长度 < 10 字 → 大概率是 SIMPLE
        if (query.length() < 10) {
            log.info("快速判断（长度 < 10）: SIMPLE");
            return QueryComplexity.SIMPLE;
        }

        // 2. 规则判断
        QueryComplexity ruleResult = assessByRule(query);

        // 3. 如果规则判断为 COMPLEX，用 LLM 二次确认（避免误判）
        if (ruleResult == QueryComplexity.COMPLEX) {
            QueryComplexity llmResult = assessByLLM(query);
            log.info("规则判断: {}, LLM 判断: {}, 最终结果: {}", ruleResult, llmResult, llmResult);
            return llmResult;
        }

        log.info("规则判断: {}", ruleResult);
        return ruleResult;
    }

    /**
     * 规则判断（基于关键词统计）
     */
    private QueryComplexity assessByRule(String query) {
        // 1. 统计各类关键词数量
        int intentCount = countIntents(query);
        int actionCount = countActions(query);
        int entityCount = countEntities(query);

        log.info("意图数: {}, 动词数: {}, 实体数: {}", intentCount, actionCount, entityCount);

        // 2. 判断是否为复杂规划场景
        if (isComplexPlanning(query)) {
            log.info("检测到规划类关键词 → COMPLEX");
            return QueryComplexity.COMPLEX;
        }

        // 3. 判断是否为多意图查询
        if (hasMultipleIntents(query)) {
            log.info("检测到多意图（包含连接词） → COMPLEX");
            return QueryComplexity.COMPLEX;
        }

        // 4. 根据意图数和实体数判断
        if (intentCount >= 2) {
            log.info("意图数 >= 2 → COMPLEX");
            return QueryComplexity.COMPLEX;
        }

        if (intentCount == 1 && entityCount >= 2) {
            log.info("单一意图，多实体 → MEDIUM");
            return QueryComplexity.MEDIUM;
        }

        // 5. 默认为 SIMPLE
        return QueryComplexity.SIMPLE;
    }

    /**
     * LLM 判断（准确但慢）
     */
    private QueryComplexity assessByLLM(String query) {
        String prompt = String.format("""
                判断以下查询的复杂度，只回答 SIMPLE / MEDIUM / COMPLEX，不要解释。

                规则：
                - SIMPLE: 单一意图，单次工具调用（如"北京天气"）
                - MEDIUM: 单一意图，多次工具调用（如"上海和广州天气对比"）
                - COMPLEX: 多意图，需要任务分解（如"规划杭州行程"）

                查询：%s

                复杂度：
                """, query);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content()
                    .trim()
                    .toUpperCase();

            // 解析响应
            if (response.contains("SIMPLE")) {
                return QueryComplexity.SIMPLE;
            } else if (response.contains("MEDIUM")) {
                return QueryComplexity.MEDIUM;
            } else if (response.contains("COMPLEX")) {
                return QueryComplexity.COMPLEX;
            } else {
                log.warn("LLM 返回了无法识别的复杂度: {}, 默认为 SIMPLE", response);
                return QueryComplexity.SIMPLE;
            }
        } catch (Exception e) {
            log.error("LLM 判断失败，降级为规则判断", e);
            return QueryComplexity.SIMPLE;
        }
    }

    /**
     * 统计意图关键词数量
     */
    private int countIntents(String query) {
        String[][] intentGroups = {
                {"天气", "温度", "下雨", "带伞", "穿什么"},           // 天气意图
                {"客户", "公司", "地址", "联系", "拜访"},            // 客户意图
                {"路线", "怎么去", "交通", "地铁", "打车"},          // 路线意图
                {"酒店", "住宿", "推荐", "协议酒店"},                // 酒店意图
                {"补贴", "报销", "标准", "伙食", "交通费"}           // 政策意图
        };

        int count = 0;
        for (String[] group : intentGroups) {
            for (String keyword : group) {
                if (query.contains(keyword)) {
                    count++;
                    break;  // 每组只计数一次
                }
            }
        }

        return count;
    }

    /**
     * 统计动词数量
     */
    private int countActions(String query) {
        String[] actionKeywords = {
                "查", "查询", "查一下",
                "规划", "安排", "计划",
                "推荐", "建议", "帮我",
                "对比", "比较"
        };

        int count = 0;
        for (String keyword : actionKeywords) {
            if (query.contains(keyword)) {
                count++;
            }
        }

        return count;
    }

    /**
     * 统计实体数量（城市、公司等）
     */
    private int countEntities(String query) {
        String[] entities = {
                // 城市
                "北京", "上海", "广州", "深圳", "杭州", "成都", "西安", "南京", "武汉", "重庆",
                // 公司（示例）
                "阿里巴巴", "腾讯", "字节跳动", "华为", "百度"
        };

        int count = 0;
        for (String entity : entities) {
            if (query.contains(entity)) {
                count++;
            }
        }

        return count;
    }

    /**
     * 判断是否为复杂规划场景
     */
    private boolean isComplexPlanning(String query) {
        String[] planningKeywords = {
                "规划", "安排", "计划", "准备",
                "行程", "方案", "攻略"
        };

        for (String keyword : planningKeywords) {
            if (query.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否包含多个意图（通过连接词判断）
     */
    private boolean hasMultipleIntents(String query) {
        String[] conjunctions = {"并", "和", "还有", "以及", "同时"};

        for (String conj : conjunctions) {
            if (query.contains(conj)) {
                // 进一步验证：连接词前后是否都有意图关键词
                String[] parts = query.split(conj);
                if (parts.length >= 2) {
                    boolean part1HasIntent = hasIntentKeyword(parts[0]);
                    boolean part2HasIntent = hasIntentKeyword(parts[1]);

                    if (part1HasIntent && part2HasIntent) {
                        return true;  // 确认是多意图
                    }
                }
            }
        }

        return false;
    }

    /**
     * 判断文本中是否包含意图关键词
     */
    private boolean hasIntentKeyword(String text) {
        String[] intentKeywords = {
                "天气", "温度", "下雨", "客户", "公司", "地址",
                "路线", "交通", "酒店", "住宿", "补贴", "报销"
        };

        for (String keyword : intentKeywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}
