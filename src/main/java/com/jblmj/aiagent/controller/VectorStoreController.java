package com.jblmj.aiagent.controller;

import com.jblmj.aiagent.rag.LoveAppDocumentLoader;
import com.jblmj.aiagent.rag.MyKeywordEnricher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量数据库管理接口
 *
 * 功能：
 * 1. 查看向量数据库状态
 * 2. 手动刷新向量数据库（重新加载文档）
 * 3. 清空向量数据库
 */
@RestController
@RequestMapping("/vectorstore")
@Tag(name = "向量数据库管理", description = "管理向量数据库的接口")
@Slf4j
public class VectorStoreController {

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    private static final String VECTOR_STORE_FILE = "data/vectorstore.json";

    /**
     * 查看向量数据库状态
     */
    @GetMapping("/status")
    @Operation(summary = "查看向量数据库状态")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        File vectorStoreFile = new File(VECTOR_STORE_FILE);
        status.put("fileExists", vectorStoreFile.exists());
        status.put("filePath", vectorStoreFile.getAbsolutePath());

        if (vectorStoreFile.exists()) {
            status.put("fileSize", vectorStoreFile.length() + " bytes");
            status.put("lastModified", new java.util.Date(vectorStoreFile.lastModified()));
        }

        // 统计文档数量（如果是 SimpleVectorStore）
        if (loveAppVectorStore instanceof SimpleVectorStore) {
            SimpleVectorStore simpleVectorStore = (SimpleVectorStore) loveAppVectorStore;
            // SimpleVectorStore 没有直接获取文档数量的方法，可以通过查询来估算
            status.put("type", "SimpleVectorStore (内存向量库)");
        }

        return status;
    }

    /**
     * 手动刷新向量数据库（重新加载文档）
     */
    @PostMapping("/refresh")
    @Operation(summary = "手动刷新向量数据库", description = "删除本地文件，重新加载文档并入库")
    public Map<String, Object> refresh() {
        log.info("开始手动刷新向量数据库...");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 删除本地文件
            File vectorStoreFile = new File(VECTOR_STORE_FILE);
            if (vectorStoreFile.exists()) {
                boolean deleted = vectorStoreFile.delete();
                log.info("删除本地文件: {}, 结果: {}", VECTOR_STORE_FILE, deleted);
            }

            // 2. 重新加载文档
            log.info("重新加载文档...");
            List<Document> documentList = loveAppDocumentLoader.loadMarkdowns();
            log.info("加载完成，共 {} 个文档", documentList.size());

            // 3. 补充关键词元信息
            log.info("补充关键词元信息...");
            List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documentList);
            log.info("补充完成，共 {} 个文档", enrichedDocuments.size());

            // 4. 清空旧数据并重新入库
            if (loveAppVectorStore instanceof SimpleVectorStore) {
                SimpleVectorStore simpleVectorStore = (SimpleVectorStore) loveAppVectorStore;
                // SimpleVectorStore 没有 clear 方法，需要重新创建
                log.warn("SimpleVectorStore 不支持清空操作，请重启应用以加载新数据");
            }

            // 5. 入库
            log.info("向量化并入库...");
            loveAppVectorStore.add(enrichedDocuments);

            // 6. 保存到本地文件
            if (loveAppVectorStore instanceof SimpleVectorStore) {
                SimpleVectorStore simpleVectorStore = (SimpleVectorStore) loveAppVectorStore;
                vectorStoreFile.getParentFile().mkdirs();
                simpleVectorStore.save(vectorStoreFile);
                log.info("保存到本地文件: {}", VECTOR_STORE_FILE);
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("向量数据库刷新完成，耗时: {} ms", totalTime);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "向量数据库刷新完成");
            result.put("documentCount", enrichedDocuments.size());
            result.put("totalTime", totalTime + " ms");
            return result;

        } catch (Exception e) {
            log.error("向量数据库刷新失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "刷新失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 删除本地向量数据库文件
     */
    @DeleteMapping("/file")
    @Operation(summary = "删除本地向量数据库文件", description = "删除后需要重启应用才能重新生成")
    public Map<String, Object> deleteFile() {
        File vectorStoreFile = new File(VECTOR_STORE_FILE);

        if (!vectorStoreFile.exists()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "文件不存在: " + VECTOR_STORE_FILE);
            return result;
        }

        boolean deleted = vectorStoreFile.delete();
        log.info("删除本地文件: {}, 结果: {}", VECTOR_STORE_FILE, deleted);

        Map<String, Object> result = new HashMap<>();
        result.put("success", deleted);
        result.put("message", deleted ? "删除成功，请重启应用以重新生成" : "删除失败");
        return result;
    }
}
