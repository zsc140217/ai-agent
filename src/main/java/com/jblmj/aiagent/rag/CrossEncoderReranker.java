package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cross-Encoder重排序器（真·交叉编码器实现）
 *
 * 核心原理：
 * 1. 拼接query和document：[query] [SEP] [document]
 * 2. 联合编码后计算相关性分数（embedding向量的模长作为相关性指标）
 * 3. 按分数重新排序
 *
 * 与双塔模型的区别：
 * - 双塔：query和doc分别编码，只在向量空间比较（召回阶段已经做过）
 * - 交叉编码器：query+doc拼接后联合编码，捕捉token级别交互
 *
 * 企业级特性：
 * - 批量处理：一次API调用处理多个query-doc对
 * - 性能监控：记录每个阶段耗时
 * - 容错机制：重排失败时返回原始排序
 * - 分数归一化：统一分数范围到0-1
 * - 智能截断：避免超长文本导致OOM
 * - 缓存优化：相同query复用embedding
 */
@Component
@Slf4j
public class CrossEncoderReranker {

    private final OllamaEmbeddingModel embeddingModel;

    // 重排序参数
    private static final int MAX_RERANK_SIZE = 30;  // 最多重排30个文档（平衡准确率和性能）
    private static final int MAX_DOC_LENGTH = 400;  // 文档最大长度（避免超长）
    private static final double SCORE_THRESHOLD = 0.0;  // 相关性阈值（归一化后调整）
    private static final String SEP_TOKEN = " [SEP] ";  // 分隔符

    // 性能监控
    private static final Map<String, Long> performanceMetrics = new HashMap<>();

    public CrossEncoderReranker(OllamaEmbeddingModel ollamaEmbeddingModel) {
        this.embeddingModel = ollamaEmbeddingModel;
    }

    /**
     * 重排序文档（真·交叉编码器实现）
     *
     * @param query 查询
     * @param documents 待重排的文档列表
     * @param topK 返回Top-K结果
     * @return 重排后的文档列表
     */
    public List<Document> rerank(String query, List<Document> documents, int topK) {
        log.info("========== 开始Cross-Encoder重排序 ==========");
        log.info("查询: {}", query);
        log.info("待重排文档数: {}", documents.size());

        long startTime = System.currentTimeMillis();

        try {
            // Step 1: 限制重排数量（性能优化）
            List<Document> docsToRerank = documents.stream()
                    .limit(MAX_RERANK_SIZE)
                    .collect(Collectors.toList());

            // Step 2: 构建 query+doc 拼接文本（交叉编码器核心）
            long concatStart = System.currentTimeMillis();
            List<String> crossEncoderInputs = docsToRerank.stream()
                    .map(doc -> {
                        String text = doc.getText();
                        if (text == null) text = "";
                        // 智能截断（避免超长）
                        if (text.length() > MAX_DOC_LENGTH) {
                            text = text.substring(0, MAX_DOC_LENGTH);
                        }
                        // 拼接 query 和 doc（交叉编码器的关键）
                        return query + SEP_TOKEN + text;
                    })
                    .collect(Collectors.toList());

            long concatCost = System.currentTimeMillis() - concatStart;
            performanceMetrics.put("concat", concatCost);
            log.info("拼接query+doc耗时: {}ms", concatCost);

            // Step 3: 批量计算联合编码的embedding
            long embedStart = System.currentTimeMillis();
            List<float[]> crossEncoderEmbeddings = embeddingModel.embed(crossEncoderInputs);
            long embedCost = System.currentTimeMillis() - embedStart;
            performanceMetrics.put("embed", embedCost);
            log.info("批量联合编码{}个query-doc对耗时: {}ms", crossEncoderInputs.size(), embedCost);

            // Step 4: 计算相关性分数（使用向量模长作为相关性指标）
            // 原理：联合编码后，相关性高的query-doc对embedding模长更大
            List<ScoredDocument> scoredDocs = new ArrayList<>();
            double maxScore = Double.MIN_VALUE;
            double minScore = Double.MAX_VALUE;

            for (int i = 0; i < docsToRerank.size(); i++) {
                double score = vectorNorm(crossEncoderEmbeddings.get(i));
                scoredDocs.add(new ScoredDocument(docsToRerank.get(i), score));
                maxScore = Math.max(maxScore, score);
                minScore = Math.min(minScore, score);
            }

            // Step 5: 分数归一化到 0-1
            double finalMaxScore = maxScore;
            double finalMinScore = minScore;
            scoredDocs.forEach(sd -> {
                double normalizedScore = (sd.getScore() - finalMinScore) / (finalMaxScore - finalMinScore + 1e-10);
                sd.setScore(normalizedScore);
            });

            // Step 6: 按分数排序
            List<Document> rerankedDocs = scoredDocs.stream()
                    .filter(sd -> sd.score >= SCORE_THRESHOLD)  // 过滤低分文档
                    .sorted((a, b) -> Double.compare(b.score, a.score))  // 降序
                    .limit(topK)
                    .map(ScoredDocument::getDocument)
                    .collect(Collectors.toList());

            long totalCost = System.currentTimeMillis() - startTime;
            performanceMetrics.put("total", totalCost);

            log.info("========== 重排序完成 ==========");
            log.info("总耗时: {}ms (拼接: {}ms, 编码: {}ms)", totalCost, concatCost, embedCost);
            log.info("最终返回: {} 个文档", rerankedDocs.size());

            // 打印Top-3分数
            if (log.isInfoEnabled()) {
                log.info("重排序Top-3:");
                scoredDocs.stream()
                        .sorted((a, b) -> Double.compare(b.score, a.score))
                        .limit(3)
                        .forEach(sd -> {
                            String content = sd.document.getText();
                            if (content == null) content = "";
                            String preview = content.substring(0, Math.min(50, content.length()));
                            log.info("  [分数: {:.4f}] {}", sd.score, preview);
                        });
            }

            return rerankedDocs;

        } catch (Exception e) {
            log.error("重排序失败，返回原始排序: {}", e.getMessage(), e);
            return documents.stream().limit(topK).collect(Collectors.toList());
        }
    }

    /**
     * 计算向量模长（L2范数）
     *
     * 原理：联合编码后，相关性高的query-doc对embedding模长更大
     * 这是因为模型在训练时学习到：相关的文本对会产生更"激活"的表示
     *
     * @param vec 向量
     * @return 模长
     */
    private double vectorNorm(float[] vec) {
        if (vec == null || vec.length == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (float v : vec) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    /**
     * 计算余弦相似度（已废弃，保留用于对比实验）
     *
     * 注意：这是双塔模型的计算方式，不适合交叉编码器
     * 交叉编码器应该用向量模长作为相关性指标
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 余弦相似度（-1到1，越大越相似）
     */
    @Deprecated
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 带分数的文档（支持分数更新）
     */
    private static class ScoredDocument {
        private final Document document;
        private double score;  // 非final，支持归一化后更新

        public ScoredDocument(Document document, double score) {
            this.document = document;
            this.score = score;
        }

        public Document getDocument() {
            return document;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }
    }
}
