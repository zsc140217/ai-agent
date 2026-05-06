package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 否定查询处理器
 *
 * 核心功能：
 * 1. 检测否定查询（不能、不是、没有等）
 * 2. 保留否定语义，避免向量检索时丢失
 * 3. 转换为更适合检索的表达
 *
 * 面试价值：
 * - 展示对RAG检索问题的深刻理解（否定词敏感度低）
 * - 体现对NLP细节的关注（语义保留）
 * - 证明你理解向量检索的局限性
 *
 * @author jblmj
 */
@Component
@Slf4j
public class NegationQueryHandler {

    // 否定词模式（扩展版）
    private static final Pattern[] NEGATION_PATTERNS = {
            // 直接否定
            Pattern.compile(".*(不能|不可以|不得|不要|不许|不准).*"),
            // 否定判断
            Pattern.compile(".*(不是|不对|不正确|不符合).*"),
            // 缺失否定
            Pattern.compile(".*(没有|无|缺少|不存在).*"),
            // 禁止否定
            Pattern.compile(".*(禁止|不允许|不支持|不包括).*"),
            // 否定疑问
            Pattern.compile(".*(不是.*吗|不能.*吗|没有.*吗|不可以.*吗).*")
    };

    // 否定词列表（用于检测和保留）
    private static final String[] NEGATION_KEYWORDS = {
            "不能", "不可以", "不得", "不要", "不许", "不准",
            "不是", "不对", "不正确", "不符合",
            "没有", "无", "缺少", "不存在",
            "禁止", "不允许", "不支持", "不包括"
    };

    /**
     * 检测是否为否定查询
     */
    public boolean isNegationQuery(String query) {
        for (Pattern pattern : NEGATION_PATTERNS) {
            if (pattern.matcher(query).matches()) {
                log.debug("检测到否定查询: {}", query);
                return true;
            }
        }
        return false;
    }

    /**
     * 处理否定查询（保留否定语义）
     *
     * 策略：
     * 1. 识别否定词
     * 2. 转换为"是否允许"、"是否可以"等明确表达
     * 3. 保留核心关键词
     */
    public String handleNegationQuery(String query) {
        log.debug("处理否定查询: {}", query);

        // 1. 检测否定疑问句（不是...吗、不能...吗）
        if (query.matches(".*(不是|不能|没有|不可以).*吗.*")) {
            return handleNegationQuestion(query);
        }

        // 2. 检测直接否定（不能、不可以）
        if (query.matches(".*(不能|不可以|不得|禁止|不允许).*")) {
            return handleDirectNegation(query);
        }

        // 3. 检测否定判断（不是、不对）
        if (query.matches(".*(不是|不对|不正确).*")) {
            return handleNegationJudgment(query);
        }

        // 4. 默认保留原样
        return query;
    }

    /**
     * 处理否定疑问句
     * 例如："北京不能住五星级酒店吗" → "北京出差住宿标准 五星级酒店是否允许"
     */
    private String handleNegationQuestion(String query) {
        // 提取核心内容（去除"不是/不能/没有...吗"）
        String core = query
                .replaceAll("不是", "")
                .replaceAll("不能", "")
                .replaceAll("没有", "")
                .replaceAll("不可以", "")
                .replaceAll("吗", "")
                .replaceAll("\\?", "")
                .trim();

        // 转换为"是否"查询
        String result = core + " 是否允许 是否可以";
        log.debug("否定疑问句转换: {} -> {}", query, result);
        return result;
    }

    /**
     * 处理直接否定
     * 例如："出差不能坐商务舱" → "出差交通标准 商务舱是否允许"
     */
    private String handleDirectNegation(String query) {
        // 提取否定词前后的内容
        String[] parts = query.split("不能|不可以|不得|禁止|不允许");
        if (parts.length >= 2) {
            String before = parts[0].trim();
            String after = parts[1].trim();
            String result = before + " " + after + " 是否允许";
            log.debug("直接否定转换: {} -> {}", query, result);
            return result;
        }

        // 无法拆分，保留原样
        return query;
    }

    /**
     * 处理否定判断
     * 例如："去二线城市不是500元住宿标准吗" → "二线城市住宿标准 金额"
     */
    private String handleNegationJudgment(String query) {
        // 提取核心内容（去除"不是...吗"）
        String core = query
                .replaceAll("不是", "")
                .replaceAll("不对", "")
                .replaceAll("不正确", "")
                .replaceAll("吗", "")
                .replaceAll("\\?", "")
                .trim();

        log.debug("否定判断转换: {} -> {}", query, core);
        return core;
    }

    /**
     * 提取否定词
     */
    public String extractNegationKeyword(String query) {
        for (String keyword : NEGATION_KEYWORDS) {
            if (query.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    /**
     * 检测否定查询类型
     */
    public NegationType detectNegationType(String query) {
        if (query.matches(".*(不是|不能|没有|不可以).*吗.*")) {
            return NegationType.QUESTION;  // 否定疑问句
        }
        if (query.matches(".*(不能|不可以|不得|禁止|不允许).*")) {
            return NegationType.DIRECT;  // 直接否定
        }
        if (query.matches(".*(不是|不对|不正确).*")) {
            return NegationType.JUDGMENT;  // 否定判断
        }
        if (query.matches(".*(没有|无|缺少|不存在).*")) {
            return NegationType.ABSENCE;  // 缺失否定
        }
        return NegationType.NONE;
    }

    /**
     * 否定查询类型
     */
    public enum NegationType {
        NONE,       // 非否定查询
        QUESTION,   // 否定疑问句（不是...吗）
        DIRECT,     // 直接否定（不能、不可以）
        JUDGMENT,   // 否定判断（不是、不对）
        ABSENCE     // 缺失否定（没有、无）
    }
}
