package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 查询重写器
 * 增强功能：处理否定查询，避免"是这样"召回"不是这样"的问题
 */
@Component
@Slf4j
public class QueryRewriter {

    private final QueryTransformer queryTransformer;
    private final ChatClient chatClient;

    // 否定词模式
    private static final Pattern NEGATION_PATTERN = Pattern.compile(
            ".*(不是|不能|不可以|没有|不允许|禁止|不得|不要).*"
    );

    public QueryRewriter(ChatModel dashscopeChatModel) {
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);
        this.chatClient = builder.build();
        // 创建查询重写转换器
        queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }

    /**
     * 执行查询重写
     *
     * @param prompt
     * @return
     */
    public String doQueryRewrite(String prompt) {
        // 检测是否包含否定词
        if (NEGATION_PATTERN.matcher(prompt).matches()) {
            log.debug("检测到否定查询: {}", prompt);
            return rewriteNegationQuery(prompt);
        }

        Query query = new Query(prompt);
        // 执行查询重写
        Query transformedQuery = queryTransformer.transform(query);
        // 输出重写后的查询
        return transformedQuery.text();
    }

    /**
     * 处理否定查询的特殊重写逻辑
     * 策略：将否定查询转换为肯定查询 + 标记，后续通过 LLM 理解否定语义
     *
     * @param prompt 原始查询
     * @return 重写后的查询
     */
    private String rewriteNegationQuery(String prompt) {
        String rewritePrompt = String.format("""
                用户查询包含否定词，请将其改写为更适合检索的形式。

                改写规则：
                1. 保留否定语义，不要转换为肯定句
                2. 提取核心查询意图
                3. 添加相关关键词以提高召回

                示例：
                - "北京出差不能住五星级酒店吗" → "北京出差住宿标准 不能住五星级酒店"
                - "出差不能坐商务舱对吗" → "出差交通标准 不能坐商务舱"
                - "去二线城市不是500元住宿标准吗" → "二线城市住宿标准 不是500元"

                用户查询：%s

                只返回改写后的查询，不要解释。
                """, prompt);

        try {
            String rewritten = chatClient.prompt()
                    .user(rewritePrompt)
                    .call()
                    .content();
            if (rewritten != null && !rewritten.trim().isEmpty()) {
                log.debug("否定查询重写: {} -> {}", prompt, rewritten);
                return rewritten;
            }
            return prompt;
        } catch (Exception e) {
            log.error("否定查询重写失败，使用原始查询: {}", e.getMessage());
            return prompt;
        }
    }
}
