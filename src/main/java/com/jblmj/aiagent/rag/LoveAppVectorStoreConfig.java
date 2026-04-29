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
 *
 * 企业级增强：
 * 4. 同时初始化BM25索引（用于混合检索）
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

    @Resource
    private BM25Retriever bm25Retriever;

    // 向量数据库持久化文件路径
    private static final String VECTOR_STORE_FILE = "data/vectorstore.json";
    // 文档缓存文件路径（避免重复调用LLM）
    private static final String DOCUMENTS_CACHE_FILE = "data/documents-cache.ser";

    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        File vectorStoreFile = new File(VECTOR_STORE_FILE);
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();

        // 用于存储文档列表（BM25索引需要）
        List<Document> documents = null;

        // 如果本地文件存在，直接加载
        if (vectorStoreFile.exists()) {
            log.info("检测到本地向量数据库文件，直接加载: {}", VECTOR_STORE_FILE);
            long startTime = System.currentTimeMillis();
            simpleVectorStore.load(vectorStoreFile);
            long loadTime = System.currentTimeMillis() - startTime;
            log.info("向量数据库加载完成，耗时: {} ms", loadTime);

            // 尝试从缓存加载文档（避免重新调用LLM）
            File docsCacheFile = new File(DOCUMENTS_CACHE_FILE);
            if (docsCacheFile.exists()) {
                log.info("检测到文档缓存文件，直接加载: {}", DOCUMENTS_CACHE_FILE);
                try {
                    documents = loadDocumentsFromCache(docsCacheFile);
                    log.info("文档缓存加载完成，共 {} 个文档", documents.size());
                } catch (Exception e) {
                    log.warn("文档缓存加载失败，将重新加载: {}", e.getMessage());
                    documents = null;
                }
            }

            // 如果缓存加载失败，重新加载文档（但跳过LLM增强）
            if (documents == null) {
                log.info("加载文档用于BM25索引（跳过LLM增强以节省时间）...");
                documents = loveAppDocumentLoader.loadMarkdowns();
                log.info("文档加载完成，共 {} 个文档", documents.size());

                // 保存到缓存
                try {
                    saveDocumentsToCache(documents, docsCacheFile);
                    log.info("文档已缓存到: {}", DOCUMENTS_CACHE_FILE);
                } catch (Exception e) {
                    log.warn("文档缓存保存失败: {}", e.getMessage());
                }
            }
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

            // 保存文档列表
            documents = enrichedDocuments;

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

            // 5. 保存文档缓存
            try {
                File docsCacheFile = new File(DOCUMENTS_CACHE_FILE);
                saveDocumentsToCache(documents, docsCacheFile);
                log.info("文档已缓存到: {}", DOCUMENTS_CACHE_FILE);
            } catch (Exception e) {
                log.warn("文档缓存保存失败: {}", e.getMessage());
            }
        }

        // 6. 初始化BM25索引（企业级增强）
        if (documents != null && !documents.isEmpty()) {
            log.info("========== 初始化BM25索引 ==========");
            // BM25Retriever内部已经实现了持久化，会自动加载或构建索引
            bm25Retriever.buildIndex(documents);
            log.info("========== BM25索引初始化完成 ==========");
        } else {
            log.warn("文档列表为空，跳过BM25索引初始化");
        }

        return simpleVectorStore;
    }

    /**
     * 保存文档到缓存
     */
    private void saveDocumentsToCache(List<Document> documents, File cacheFile) throws Exception {
        cacheFile.getParentFile().mkdirs();
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(
                new java.io.FileOutputStream(cacheFile))) {
            oos.writeObject(new java.util.ArrayList<>(documents));
        }
    }

    /**
     * 从缓存加载文档
     */
    @SuppressWarnings("unchecked")
    private List<Document> loadDocumentsFromCache(File cacheFile) throws Exception {
        try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
                new java.io.FileInputStream(cacheFile))) {
            return (List<Document>) ois.readObject();
        }
    }
}
