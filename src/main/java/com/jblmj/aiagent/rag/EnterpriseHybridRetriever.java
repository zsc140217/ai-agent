package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 企业级混合检索器 - 三路召回
 *
 * 架构：
 * 1. BM25检索（稀疏检索）- 精确匹配
 * 2. Dense检索-原始查询（稠密检索）- 语义匹配
 * 3. Dense检索-改写查询（稠密检索）- 标准化语义匹配
 * 4. RRF融合（倒数排名融合）
 *
 * 企业级特性：
 * - 多路召回容错：任一路失败不影响整体
 * - 性能监控：记录每路召回耗时
 * - 可配置权重：支持调整各路召回的权重
 * - 去重策略：相同文档只保留一份
 */
@Component
@Slf4j
public class EnterpriseHybridRetriever {

    private final VectorStore vectorStore;
    private final BM25Retriever bm25Retriever;
    private final EnterpriseQueryRewriter queryRewriter;

    // RRF参数
    private static final int RRF_K = 60;

    // 各路召回权重（用于加权RRF）
    private static final double BM25_WEIGHT = 1.0;
    private static final double DENSE_ORIGINAL_WEIGHT = 1.0;
    private static final double DENSE_REWRITTEN_WEIGHT = 1.0;

    public EnterpriseHybridRetriever(VectorStore loveAppVectorStore,
                                     BM25Retriever bm25Retriever,
                                     EnterpriseQueryRewriter queryRewriter) {
        this.vectorStore = loveAppVectorStore;
        this.bm25Retriever = bm25Retriever;
        this.queryRewriter = queryRewriter;
    }

    /**
     * 三路召回 + RRF融合
     *
     * @param originalQuery 原始查询
     * @param topK 返回Top-K结果
     * @return 融合后的文档列表
     */
    public List<Document> retrieve(String originalQuery, int topK) {
        log.info("========== 开始三路召回 ==========");
        log.info("原始查询: {}", originalQuery);
        log.info("目标Top-K: {}", topK);

        long startTime = System.currentTimeMillis();

        // Step 1: 查询改写
        long rewriteStart = System.currentTimeMillis();
        String rewrittenQuery = queryRewriter.rewrite(originalQuery);
        long rewriteCost = System.currentTimeMillis() - rewriteStart;
        log.info("查询改写完成，耗时: {}ms, 改写后: {}", rewriteCost, rewrittenQuery);

        // Step 2: 三路召回（召回2倍数量，后续融合）
        int retrieveSize = topK * 2;

        // 路径1：BM25检索
        List<Document> bm25Results = retrieveWithMetrics(
                "BM25",
                () -> bm25Retriever.search(originalQuery, retrieveSize)
        );

        // 路径2：Dense检索-原始查询
        List<Document> denseOriginalResults = retrieveWithMetrics(
                "Dense-Original",
                () -> vectorStore.similaritySearch(originalQuery)
        );

        // 路径3：Dense检索-改写查询
        List<Document> denseRewrittenResults = retrieveWithMetrics(
                "Dense-Rewritten",
                () -> vectorStore.similaritySearch(rewrittenQuery)
        );

        // Step 3: RRF融合
        long fusionStart = System.currentTimeMillis();
        List<Document> fusedResults = fuseWithWeightedRRF(
                bm25Results,
                denseOriginalResults,
                denseRewrittenResults,
                topK
        );
        long fusionCost = System.currentTimeMillis() - fusionStart;

        long totalCost = System.currentTimeMillis() - startTime;

        log.info("========== 三路召回完成 ==========");
        log.info("融合耗时: {}ms", fusionCost);
        log.info("总耗时: {}ms", totalCost);
        log.info("最终返回: {} 个文档", fusedResults.size());

        return fusedResults;
    }

    /**
     * 带性能监控的检索
     */
    private List<Document> retrieveWithMetrics(String pathName, RetrieveFunction function) {
        long start = System.currentTimeMillis();
        List<Document> results;

        try {
            results = function.retrieve();
            long cost = System.currentTimeMillis() - start;
            log.info("路径[{}] 召回完成，耗时: {}ms, 召回数量: {}", pathName, cost, results.size());
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("路径[{}] 召回失败，耗时: {}ms, 错误: {}", pathName, cost, e.getMessage());
            results = Collections.emptyList();
        }

        return results;
    }

    /**
     * 加权RRF融合（Weighted Reciprocal Rank Fusion）
     *
     * 公式：score(doc) = Σ weight_i / (k + rank_i)
     * 其中：
     * - weight_i: 第i路召回的权重
     * - rank_i: 文档在第i路召回中的排名
     * - k: 平滑因子（60）
     */
    private List<Document> fuseWithWeightedRRF(List<Document> bm25Results,
                                               List<Document> denseOriginalResults,
                                               List<Document> denseRewrittenResults,
                                               int topK) {
        log.debug("开始RRF融合，输入文档数: BM25={}, Dense-Original={}, Dense-Rewritten={}",
                bm25Results.size(), denseOriginalResults.size(), denseRewrittenResults.size());

        // 计算每个文档的加权RRF分数
        Map<String, RRFScore> scoreMap = new HashMap<>();

        // 处理BM25结果
        for (int i = 0; i < bm25Results.size(); i++) {
            Document doc = bm25Results.get(i);
            String docId = getDocumentId(doc);

            scoreMap.putIfAbsent(docId, new RRFScore(doc));
            scoreMap.get(docId).addRank(i + 1, "BM25", BM25_WEIGHT);
        }

        // 处理Dense-Original结果
        for (int i = 0; i < denseOriginalResults.size(); i++) {
            Document doc = denseOriginalResults.get(i);
            String docId = getDocumentId(doc);

            scoreMap.putIfAbsent(docId, new RRFScore(doc));
            scoreMap.get(docId).addRank(i + 1, "Dense-Original", DENSE_ORIGINAL_WEIGHT);
        }

        // 处理Dense-Rewritten结果
        for (int i = 0; i < denseRewrittenResults.size(); i++) {
            Document doc = denseRewrittenResults.get(i);
            String docId = getDocumentId(doc);

            scoreMap.putIfAbsent(docId, new RRFScore(doc));
            scoreMap.get(docId).addRank(i + 1, "Dense-Rewritten", DENSE_REWRITTEN_WEIGHT);
        }

        // 按RRF分数排序
        List<Document> fusedResults = scoreMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .map(RRFScore::getDocument)
                .collect(Collectors.toList());

        // 打印融合详情（Top-5）
        if (log.isInfoEnabled()) {
            log.info("RRF融合详情（Top-5）:");
            scoreMap.values().stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(5)
                    .forEach(score -> {
                        String content = score.getDocument().getText();
                        String preview = content.substring(0, Math.min(50, content.length()));
                        log.info("  [分数: {:.4f}] {} | 来源: {}",
                                String.format("%.4f", score.getScore()), preview, score.getSources());
                    });
        }

        return fusedResults;
    }

    /**
     * 获取文档唯一标识
     */
    private String getDocumentId(Document doc) {
        if (doc.getId() != null && !doc.getId().isEmpty()) {
            return doc.getId();
        }
        return String.valueOf(doc.getText().hashCode());
    }

    /**
     * 检索函数接口
     */
    @FunctionalInterface
    private interface RetrieveFunction {
        List<Document> retrieve();
    }

    /**
     * 加权RRF分数计算器
     */
    private static class RRFScore {
        private final Document document;
        private double score = 0.0;
        private final List<String> sources = new ArrayList<>();

        public RRFScore(Document document) {
            this.document = document;
        }

        /**
         * 添加排名（加权）
         *
         * @param rank 排名（从1开始）
         * @param source 来源路径
         * @param weight 权重
         */
        public void addRank(int rank, String source, double weight) {
            // 加权RRF公式：weight / (k + rank)
            double contribution = weight / (RRF_K + rank);
            score += contribution;
            sources.add(String.format("%s:rank%d(%.4f)", source, rank, contribution));
        }

        public Document getDocument() {
            return document;
        }

        public double getScore() {
            return score;
        }

        public String getSources() {
            return String.join(", ", sources);
        }
    }

    /**
     * 获取检索统计信息（用于监控）
     */
    public RetrievalStats getStats() {
        // TODO: 实现统计信息收集
        return new RetrievalStats();
    }

    /**
     * 检索统计信息
     */
    public static class RetrievalStats {
        public long totalQueries = 0;
        public long bm25Failures = 0;
        public long denseFailures = 0;
        public double avgLatency = 0.0;

        // TODO: 添加更多统计指标
    }
}
