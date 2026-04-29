package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 恋爱大师应用文档加载器
 * 单例模式：文档只加载一次，多处复用
 */
@Component
@Slf4j
public class LoveAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    // 缓存加载的文档，避免重复读取磁盘
    private List<Document> cachedDocuments;

    public LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载多篇 Markdown 文档（带缓存）
     * @return
     */
    public List<Document> loadMarkdowns() {
        if (cachedDocuments != null) {
            log.info("使用缓存的文档，共 {} 个", cachedDocuments.size());
            return cachedDocuments;
        }

        List<Document> allDocuments = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            log.info("开始加载 {} 个 Markdown 文件", resources.length);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                // 提取文档倒数第 3 和第 2 个字作为标签
                String status = filename.substring(filename.length() - 6, filename.length() - 4);
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .withAdditionalMetadata("status", status)
                        .build();
                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(markdownDocumentReader.get());
            }
            cachedDocuments = allDocuments;
            log.info("文档加载完成，共 {} 个文档片段", allDocuments.size());
        } catch (IOException e) {
           log.error("Markdown 文档加载失败", e);
        }
        return allDocuments;
    }
}
