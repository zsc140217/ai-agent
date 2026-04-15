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
     * 统计意图关键词数量（混合策略：关键词 + LLM 兜底）
     */
    private int countIntents(String query) {
        // 1. 先用关键词匹配（快速）
        int keywordCount = countIntentsByKeyword(query);

        // 2. 如果关键词匹配不到任何意图，用 LLM 兜底（准确）
        if (keywordCount == 0) {
            log.info("关键词匹配未找到意图，使用 LLM 兜底判断");
            int llmCount = countIntentsByLLM(query);
            log.info("LLM 判断意图数: {}", llmCount);
            return llmCount;
        }

        log.info("关键词匹配意图数: {}", keywordCount);
        return keywordCount;
    }

    /**
     * 通过关键词统计意图数量
     */
    private int countIntentsByKeyword(String query) {
        String[][] intentGroups = {
                {"天气", "温度", "下雨", "带伞", "穿什么", "气温", "热", "冷", "晴", "阴"},           // 天气意图
                {"客户", "公司", "地址", "联系", "拜访", "企业", "厂商"},            // 客户意图
                {"路线", "怎么去", "交通", "地铁", "打车", "距离", "多远", "导航"},          // 路线意图
                {"酒店", "住宿", "推荐", "协议酒店", "宾馆", "旅馆"},                // 酒店意图
                {"补贴", "报销", "标准", "伙食", "交通费", "费用", "能报多少"}           // 政策意图
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
     * 通过 LLM 统计意图数量（兜底方案）
     */
    private int countIntentsByLLM(String query) {
        String prompt = String.format("""
                分析以下查询包含几个意图，只回答数字（0、1、2、3...），不要解释。

                意图类型：
                1. 天气意图：查询天气、温度、是否下雨等
                2. 客户意图：查询客户公司地址、联系人等
                3. 路线意图：查询路线、距离、交通方式等
                4. 酒店意图：查询或推荐酒店、住宿等
                5. 政策意图：查询报销标准、补贴政策等

                示例：
                - "北京天气怎么样" → 1（只有天气意图）
                - "魔都今天气温如何" → 1（只有天气意图，魔都指上海）
                - "查北京天气并推荐酒店" → 2（天气 + 酒店）
                - "你好" → 0（没有明确意图）

                查询：%s

                意图数量：
                """, query);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 检查空值
            if (response == null || response.trim().isEmpty()) {
                log.warn("LLM 返回空内容，默认为 1");
                return 1;
            }

            response = response.trim();

            // 提取数字
            String numberStr = response.replaceAll("[^0-9]", "");
            if (numberStr.isEmpty()) {
                log.warn("LLM 返回无法解析的意图数: {}, 默认为 1", response);
                return 1;
            }

            int count = Integer.parseInt(numberStr);
            // 限制范围 0-5
            return Math.max(0, Math.min(count, 5));

        } catch (Exception e) {
            log.error("LLM 意图判断失败，默认返回 1", e);
            return 1;  // 降级：默认为单一意图
        }
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
     * 统计实体数量（城市、公司等）- 混合策略：关键词 + LLM 兜底
     */
    private int countEntities(String query) {
        // 1. 先用关键词匹配（快速）
        int keywordCount = countEntitiesByKeyword(query);

        // 2. 如果关键词匹配不到实体，但查询中可能包含实体（如"魔都"），用 LLM 兜底
        if (keywordCount == 0 && seemsToContainEntity(query)) {
            log.info("关键词未匹配到实体，但疑似包含实体，使用 LLM 兜底判断");
            int llmCount = countEntitiesByLLM(query);
            log.info("LLM 判断实体数: {}", llmCount);
            return llmCount;
        }

        return keywordCount;
    }

    /**
     * 通过关键词统计实体数量
     */
    private int countEntitiesByKeyword(String query) {
        String[] entities = {
                // 城市
                "北京", "上海", "广州", "深圳", "杭州", "成都", "西安", "南京", "武汉", "重庆",
                "天津", "苏州", "郑州", "长沙", "沈阳", "青岛", "无锡", "佛山", "宁波", "东莞",
                // 公司（示例）
                "阿里巴巴", "腾讯", "字节跳动", "华为", "百度", "京东", "美团", "拼多多", "小米", "网易"
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
     * 判断查询中是否疑似包含实体（用于触发 LLM 兜底）
     */
    private boolean seemsToContainEntity(String query) {
        // 如果包含"去"、"到"、"在"等介词，可能包含地点实体
        String[] locationPrepositions = {"去", "到", "在", "从", "往"};
        for (String prep : locationPrepositions) {
            if (query.contains(prep)) {
                return true;
            }
        }

        // 如果包含城市别名或口语化表达
        String[] cityNicknames = {"魔都", "帝都", "羊城", "蓉城", "鹏城"};
        for (String nickname : cityNicknames) {
            if (query.contains(nickname)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 通过 LLM 统计实体数量（兜底方案）
     */
    private int countEntitiesByLLM(String query) {
        String prompt = String.format("""
                分析以下查询包含几个地点或公司实体，只回答数字（0、1、2、3...），不要解释。

                实体类型：
                - 城市/地点：北京、上海、魔都（上海）、帝都（北京）等
                - 公司/企业：阿里巴巴、腾讯、华为等

                示例：
                - "北京天气怎么样" → 1（北京）
                - "魔都今天气温如何" → 1（魔都指上海）
                - "上海和广州天气对比" → 2（上海、广州）
                - "去帝都出差" → 1（帝都指北京）
                - "天气怎么样" → 0（没有地点）

                查询：%s

                实体数量：
                """, query);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 检查空值
            if (response == null || response.trim().isEmpty()) {
                log.warn("LLM 返回空内容，默认为 0");
                return 0;
            }

            response = response.trim();

            // 提取数字
            String numberStr = response.replaceAll("[^0-9]", "");
            if (numberStr.isEmpty()) {
                log.warn("LLM 返回无法解析的实体数: {}, 默认为 0", response);
                return 0;
            }

            int count = Integer.parseInt(numberStr);
            // 限制范围 0-10
            return Math.max(0, Math.min(count, 10));

        } catch (Exception e) {
            log.error("LLM 实体判断失败，默认返回 0", e);
            return 0;
        }
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
