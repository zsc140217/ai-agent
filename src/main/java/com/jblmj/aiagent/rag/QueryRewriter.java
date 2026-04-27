package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 查询重写器
 * 优化策略：
 * 1. 口语化/多语言查询：字典替换（不调用LLM）
 * 2. 否定查询：保留原样（LLM自己能理解）
 * 3. 复杂查询：才调用LLM改写
 */
@Component
@Slf4j
public class QueryRewriter {

    private final QueryTransformer queryTransformer;

    // 口语化表达字典
    private static final Map<String, String> COLLOQUIAL_MAP = Map.of(
            "魔都", "上海",
            "帝都", "北京",
            "BJ", "北京",
            "SH", "上海",
            "Shanghai", "上海",
            "Beijing", "北京",
            "GZ", "广州",
            "SZ", "深圳"
    );

    // 否定词模式（用于检测，但不改写）
    private static final Pattern NEGATION_PATTERN = Pattern.compile(
            ".*(不是|不能|不可以|没有|不允许|禁止|不得|不要).*"
    );

    public QueryRewriter(ChatModel dashscopeChatModel) {
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);
        // 创建查询重写转换器（仅在复杂查询时使用）
        queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }

    /**
     * 执行查询重写（优化版）
     *
     * @param prompt 原始查询
     * @return 重写后的查询
     */
    public String doQueryRewrite(String prompt) {
        // Step 1: 字典替换口语化表达（不调用LLM）
        String normalized = normalizeColloquial(prompt);

        // Step 2: 检测否定查询（保留原样，LLM自己能理解）
        if (NEGATION_PATTERN.matcher(normalized).matches()) {
            log.debug("检测到否定查询，保留原样: {}", normalized);
            return normalized;
        }

        // Step 3: 检测复杂查询（才调用LLM改写）
        if (isComplexQuery(normalized)) {
            log.debug("检测到复杂查询，使用LLM改写: {}", normalized);
            return rewriteWithLLM(normalized);
        }

        // Step 4: 简单查询直接返回
        log.debug("简单查询，直接返回: {}", normalized);
        return normalized;
    }

    /**
     * 标准化口语化表达（字典替换）
     */
    private String normalizeColloquial(String query) {
        String result = query;
        for (Map.Entry<String, String> entry : COLLOQUIAL_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        if (!result.equals(query)) {
            log.debug("口语化标准化: {} -> {}", query, result);
        }
        return result;
    }

    /**
     * 判断是否为复杂查询
     */
    private boolean isComplexQuery(String query) {
        // 多意图查询："去杭州拜访客户，住宿标准和客户地址"
        // 对比查询："北京和上海的住宿标准哪个高"
        boolean hasMultipleIntents = query.contains("，") && query.split("，").length > 1;
        boolean hasComparison = query.contains("和") && (query.contains("哪个") || query.contains("对比"));

        return hasMultipleIntents || hasComparison;
    }

    /**
     * 使用LLM改写查询（仅复杂查询）
     */
    private String rewriteWithLLM(String query) {
        Query q = new Query(query);
        Query transformed = queryTransformer.transform(q);
        log.debug("LLM改写: {} -> {}", query, transformed.text());
        return transformed.text();
    }
}
