package com.jblmj.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 AI 的文档元信息增强器（为文档补充元信息）
 */
@Component
@Slf4j
public class MyKeywordEnricher {

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 批量增强文档，严格控制并发避免限流（120 QPM）
     */
    public List<Document> enrichDocuments(List<Document> documents) {
        KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(dashscopeChatModel, 5);

        // 120 QPM 限制，每分钟最多 120 次请求
        // 安全起见，每批只处理 1 个文档，每次间隔 1 秒（相当于 60 QPM）
        List<Document> enrichedDocuments = new ArrayList<>();

        log.info("开始处理 {} 个文档，预计耗时 {} 秒", documents.size(), documents.size());

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            log.info("正在处理第 {}/{} 个文档", i + 1, documents.size());

            try {
                List<Document> enrichedBatch = keywordMetadataEnricher.apply(List.of(doc));
                enrichedDocuments.addAll(enrichedBatch);

                // 每次请求后延迟 1 秒，确保不超过 60 QPM
                if (i < documents.size() - 1) {
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (Exception e) {
                log.error("文档增强失败，使用原始文档: {}", e.getMessage());
                enrichedDocuments.add(doc);
            }
        }

        log.info("文档处理完成，成功增强 {} 个文档", enrichedDocuments.size());
        return enrichedDocuments;
    }
}
