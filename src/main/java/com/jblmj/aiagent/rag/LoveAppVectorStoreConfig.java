package com.jblmj.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;

/**
 * 恋爱大师向量数据库配置（支持本地持久化）
 *
 * 优化：
 * 1. 首次启动：加载文档 → 切分 → 入库 → 保存到本地文件
 * 2. 后续启动：直接从本地文件加载，跳过切分过程
 * 3. 如果文档有更新：删除本地文件，重新生成
 */
@Configuration
@Slf4j
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    // 向量数据库持久化文件路径
    private static final String VECTOR_STORE_FILE = "data/vectorstore.json";

    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        File vectorStoreFile = new File(VECTOR_STORE_FILE);
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();

        // 如果本地文件存在，直接加载
        if (vectorStoreFile.exists()) {
            log.info("检测到本地向量数据库文件，直接加载: {}", VECTOR_STORE_FILE);
            long startTime = System.currentTimeMillis();
            simpleVectorStore.load(vectorStoreFile);
            long loadTime = System.currentTimeMillis() - startTime;
            log.info("向量数据库加载完成，耗时: {} ms", loadTime);
        } else {
            // 首次启动，需要加载文档并入库
            log.info("本地向量数据库文件不存在，开始初始化...");
            long startTime = System.currentTimeMillis();

            // 1. 加载文档
            log.info("步骤 1/3: 加载文档...");
            List<Document> documentList = loveAppDocumentLoader.loadMarkdowns();
            log.info("加载完成，共 {} 个文档", documentList.size());

            // 2. 自动补充关键词元信息
            log.info("步骤 2/3: 补充关键词元信息...");
            List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documentList);
            log.info("补充完成，共 {} 个文档", enrichedDocuments.size());

            // 3. 入库
            log.info("步骤 3/3: 向量化并入库（可能需要几分钟）...");
            simpleVectorStore.add(enrichedDocuments);
            long initTime = System.currentTimeMillis() - startTime;
            log.info("向量数据库初始化完成，耗时: {} ms", initTime);

            // 4. 保存到本地文件
            log.info("保存向量数据库到本地文件: {}", VECTOR_STORE_FILE);
            // 确保目录存在
            vectorStoreFile.getParentFile().mkdirs();
            simpleVectorStore.save(vectorStoreFile);
            log.info("保存完成！下次启动将直接加载，无需重新初始化");
        }

        return simpleVectorStore;
    }
}
