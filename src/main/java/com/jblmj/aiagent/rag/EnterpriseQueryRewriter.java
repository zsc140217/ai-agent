package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 企业级查询重写器
 *
 * 核心策略：
 * 1. Few-shot Learning：提供改写示例，教LLM如何改写
 * 2. 领域知识注入：企业术语、同义词库
 * 3. 结构化改写：提取关键信息（地点、金额、时间等）
 * 4. 改写质量保证：检测改写是否成功
 */
@Component
@Slf4j
public class EnterpriseQueryRewriter {

    private final ChatClient chatClient;

    // 企业领域知识库
    private static final String DOMAIN_KNOWLEDGE = """
        【企业差旅领域术语】
        - 城市分类：一类城市（北京、上海、深圳、广州、杭州、成都）、二类城市（省会城市）
        - 住宿标准：每晚住宿费用上限
        - 交通标准：高铁二等座、飞机经济舱
        - 补贴标准：伙食补助、市内交通补助

        【同义词映射】
        - 魔都 = 上海
        - 帝都 = 北京
        - 住宿 = 酒店 = 宾馆
        - 报销 = 费用 = 标准
        - 出差 = 差旅 = 外勤
        """;

    // Few-shot 改写示例
    private static final String FEW_SHOT_EXAMPLES = """
        【改写示例 - 学习这些模式】

        示例1：口语化 → 标准化
        原始："去魔都出差住宿能报多少"
        改写："上海一类城市出差住宿费用报销标准"
        原因：替换口语词（魔都→上海）+ 补充关键词（一类城市、费用报销标准）

        示例2：简略 → 完整
        原始："北京住宿标准"
        改写："北京一类城市出差住宿费用标准"
        原因：补充上下文（一类城市、出差、费用）

        示例3：否定疑问 → 明确查询
        原始："北京不能住五星级酒店吗"
        改写："北京出差住宿标准 五星级酒店是否允许"
        原因：保留否定语义 + 转换为明确的"是否允许"查询

        示例4：多意图 → 拆分关键词
        原始："去杭州拜访客户，住宿标准和客户地址"
        改写："杭州出差住宿标准 杭州客户信息地址"
        原因：拆分多个意图，保留所有关键词

        示例5：对比查询 → 保留对比结构
        原始："北京和上海的住宿标准哪个高"
        改写："北京上海一类城市住宿标准对比"
        原因：保留对比意图，补充分类信息

        示例6：数值计算 → 明确计算意图
        原始："出差30天伙食补助总共多少"
        改写："出差伙食补助标准 30天总计金额"
        原因：明确计算意图，保留数值信息
        """;

    public EnterpriseQueryRewriter(ChatModel dashscopeChatModel) {
        this.chatClient = ChatClient.builder(dashscopeChatModel).build();
    }

    /**
     * 执行查询重写
     *
     * @param originalQuery 原始查询
     * @return 改写后的查询
     */
    public String rewrite(String originalQuery) {
        log.debug("开始查询重写，原始查询: {}", originalQuery);

        // 构建改写Prompt
        String rewritePrompt = buildRewritePrompt(originalQuery);

        try {
            // 调用LLM改写
            String rewrittenQuery = chatClient.prompt()
                    .user(rewritePrompt)
                    .call()
                    .content();

            // 清理改写结果（去除多余的解释）
            rewrittenQuery = cleanRewriteResult(rewrittenQuery);

            // 验证改写质量
            if (isValidRewrite(originalQuery, rewrittenQuery)) {
                log.info("查询重写成功: {} -> {}", originalQuery, rewrittenQuery);
                return rewrittenQuery;
            } else {
                log.warn("改写质量不佳，使用原始查询: {}", originalQuery);
                return originalQuery;
            }

        } catch (Exception e) {
            log.error("查询重写失败，使用原始查询: {}", e.getMessage());
            return originalQuery;
        }
    }

    /**
     * 构建改写Prompt（核心）
     */
    private String buildRewritePrompt(String originalQuery) {
        return String.format("""
                你是企业差旅政策查询系统的查询重写专家。
                你的任务是将用户的口语化查询，改写为更适合向量检索的标准化表达。

                %s

                %s

                【改写规则】
                1. 替换口语词为标准术语（参考同义词映射）
                2. 补充关键信息（城市分类、费用类型、标准等）
                3. 保留原始语义，不要改变用户意图
                4. 保留否定词、数值、时间等关键信息
                5. 如果是多意图查询，保留所有关键词
                6. 改写后的查询应该是陈述句，不要带疑问词

                【用户查询】
                %s

                【改写要求】
                - 只返回改写后的查询，不要解释
                - 改写后的查询应该在20-30字之间
                - 如果原始查询已经很标准，可以只做微调

                改写后的查询：
                """,
                DOMAIN_KNOWLEDGE,
                FEW_SHOT_EXAMPLES,
                originalQuery
        );
    }

    /**
     * 清理改写结果
     */
    private String cleanRewriteResult(String rewritten) {
        if (rewritten == null) {
            return "";
        }

        // 去除常见的解释性前缀
        rewritten = rewritten.replaceAll("^(改写后的查询：|改写：|查询：)", "");

        // 去除引号
        rewritten = rewritten.replaceAll("[\"\"'']", "");

        // 去除首尾空白
        rewritten = rewritten.trim();

        return rewritten;
    }

    /**
     * 验证改写质量
     */
    private boolean isValidRewrite(String original, String rewritten) {
        // 基本检查
        if (rewritten == null || rewritten.isEmpty()) {
            return false;
        }

        // 长度检查（改写后不应该太短或太长）
        if (rewritten.length() < 5 || rewritten.length() > 100) {
            return false;
        }

        // 相似度检查（改写后不应该和原始查询完全一样）
        if (rewritten.equals(original)) {
            log.debug("改写结果与原始查询相同，可能改写失败");
            // 但这不一定是错误，可能原始查询已经很标准
            return true;
        }

        // 关键词保留检查（改写后应该保留原始查询的核心关键词）
        // 提取原始查询的关键词（简单实现：去除停用词）
        String[] originalKeywords = extractKeywords(original);
        String[] rewrittenKeywords = extractKeywords(rewritten);

        // 至少保留50%的关键词
        int matchCount = 0;
        for (String keyword : originalKeywords) {
            for (String rewrittenKeyword : rewrittenKeywords) {
                if (rewrittenKeyword.contains(keyword) || keyword.contains(rewrittenKeyword)) {
                    matchCount++;
                    break;
                }
            }
        }

        double retainRate = (double) matchCount / originalKeywords.length;
        if (retainRate < 0.3) {
            log.warn("改写后关键词保留率过低: {}, 原始: {}, 改写: {}",
                    retainRate, original, rewritten);
            return false;
        }

        return true;
    }

    /**
     * 提取关键词（改进版：按词提取，而不是按字）
     */
    private String[] extractKeywords(String query) {
        // 去除停用词
        String[] stopWords = {"的", "了", "吗", "呢", "吧", "啊", "是", "在", "有", "和", "与", "能", "多少"};
        String cleaned = query;
        for (String stopWord : stopWords) {
            cleaned = cleaned.replace(stopWord, " ");
        }

        // 简单分词：提取2-4字的词组
        List<String> keywords = new ArrayList<>();

        // 提取2字词
        for (int i = 0; i < cleaned.length() - 1; i++) {
            String word = cleaned.substring(i, i + 2);
            if (!word.trim().isEmpty() && !word.matches(".*\\s.*")) {
                keywords.add(word);
            }
        }

        // 提取3字词
        for (int i = 0; i < cleaned.length() - 2; i++) {
            String word = cleaned.substring(i, i + 3);
            if (!word.trim().isEmpty() && !word.matches(".*\\s.*")) {
                keywords.add(word);
            }
        }

        // 去重
        return keywords.stream().distinct().toArray(String[]::new);
    }
}
